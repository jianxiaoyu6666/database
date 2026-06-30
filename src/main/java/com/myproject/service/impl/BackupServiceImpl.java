package com.myproject.service.impl;

import com.myproject.service.BackupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BackupServiceImpl implements BackupService {

    @Autowired
    private DataSource dataSource;

    @Value("${spring.datasource.username}")
    private String dbUser;
    @Value("${spring.datasource.password}")
    private String dbPassword;

    /** MySQL bin 目录缓存，避免每次查询 */
    private volatile String mysqlBinDir;

    // 专用备份账号（只有只读权限）
    private static final String BACKUP_USER = "backup_operator";
    private static final String BACKUP_PASS = "123";

    private static final String DB_NAME = "train_ticket_db";
    private static final DateTimeFormatter TF = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /** backups 目录（项目根目录下） */
    private Path backupsDir() throws IOException {
        Path dir = Paths.get(System.getProperty("user.dir"), "backups");
        if (!Files.exists(dir)) Files.createDirectories(dir);
        return dir;
    }

    @Override
    public Map<String, Object> fullBackup() throws Exception {
        String timestamp = LocalDateTime.now().format(TF);
        String fileName = DB_NAME + "_" + timestamp + ".sql";

        Path outputFile = backupsDir().resolve(fileName);

        // mysqldump -u backup_operator -p123 --default-character-set=utf8mb4 --no-create-db --databases train_ticket_db --result-file=xxx.sql
        ProcessBuilder pb = new ProcessBuilder(
                mysqldumpPath(),
                "-u", BACKUP_USER,
                "-p" + BACKUP_PASS,
                "--default-character-set=utf8mb4",
                "--no-create-db", "--databases", DB_NAME,
                "--result-file=" + outputFile.toAbsolutePath()
        );
        pb.redirectErrorStream(true);

        Process p = pb.start();
        String output = readStream(p.getInputStream());
        int exitCode = p.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException("mysqldump 失败 (exit=" + exitCode + "): " + output);
        }

        long size = Files.size(outputFile);

        // 备份成功后写入审计日志（调用存储过程）
        try (Connection conn = dataSource.getConnection();
             CallableStatement cs = conn.prepareCall("{CALL sp_backup_database(?, ?)}")) {
            cs.setString(1, fileName);
            cs.setLong(2, size);
            cs.execute();
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("fileName", fileName);
        result.put("filePath", outputFile.toAbsolutePath().toString());
        result.put("fileSize", size);
        result.put("createdAt", LocalDateTime.now().toString());
        return result;
    }

    @Override
    public List<Map<String, Object>> listBackupFiles() throws Exception {
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(backupsDir(), "*.sql")) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (Path p : ds) {
                Map<String, Object> info = new LinkedHashMap<>();
                info.put("fileName", p.getFileName().toString());
                info.put("fileSize", Files.size(p));
                info.put("lastModified", Files.getLastModifiedTime(p).toString());
                list.add(info);
            }
            list.sort((a, b) -> b.get("lastModified").toString()
                    .compareTo(a.get("lastModified").toString()));
            return list;
        }
    }

    @Override
    public void restore(String fileName) throws Exception {
        Path backupFile = backupsDir().resolve(fileName);
        if (!Files.exists(backupFile)) {
            throw new RuntimeException("备份文件不存在: " + fileName);
        }

        // 恢复用 root 账号（restore_operator 会有 LOCK TABLES/REFERENCES/SUPER 等权限坑，课程项目直接用 root）
        // --default-character-set=utf8mb4 防止中文 COMMENT 乱码
        ProcessBuilder pb = new ProcessBuilder(
                mysqlPath(),
                "-u", dbUser,
                "-p" + dbPassword,
                "--default-character-set=utf8mb4",
                DB_NAME);
        pb.redirectErrorStream(true);
        Process p = pb.start();

        // 把备份文件内容喂给 mysql 的 stdin
        try (OutputStream os = p.getOutputStream();
             InputStream fis = Files.newInputStream(backupFile)) {
            fis.transferTo(os);
        }

        int exitCode = p.waitFor();
        String output = readStream(p.getInputStream());
        if (exitCode != 0) {
            throw new RuntimeException("恢复失败 (exit=" + exitCode + "): " + output);
        }
    }

    /** 通过 SHOW VARIABLES LIKE 'basedir' 找到 MySQL 安装目录下的 bin */
    private String findMysqlBin() throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW VARIABLES LIKE 'basedir'")) {
            if (rs.next()) {
                String basedir = rs.getString("Value");
                if (basedir != null) {
                    Path bin = Paths.get(basedir, "bin").toAbsolutePath();
                    if (Files.exists(bin)) return bin.toString();
                }
            }
        }
        return null;
    }

    private String mysqldumpPath() throws Exception {
        if (mysqlBinDir == null) {
            synchronized (this) {
                if (mysqlBinDir == null) mysqlBinDir = findMysqlBin();
            }
        }
        if (mysqlBinDir != null) {
            Path p = Paths.get(mysqlBinDir, "mysqldump" + (isWindows() ? ".exe" : ""));
            if (Files.exists(p)) return p.toString();
        }
        return "mysqldump";
    }

    private String mysqlPath() throws Exception {
        if (mysqlBinDir == null) {
            synchronized (this) {
                if (mysqlBinDir == null) mysqlBinDir = findMysqlBin();
            }
        }
        if (mysqlBinDir != null) {
            Path p = Paths.get(mysqlBinDir, "mysql" + (isWindows() ? ".exe" : ""));
            if (Files.exists(p)) return p.toString();
        }
        return "mysql";
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private String readStream(InputStream is) throws IOException {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(is))) {
            return r.lines().collect(Collectors.joining("\n"));
        }
    }
}
