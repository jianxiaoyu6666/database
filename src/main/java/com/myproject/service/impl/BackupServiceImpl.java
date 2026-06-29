package com.myproject.service.impl;

import com.myproject.entity.BackupInfo;
import com.myproject.entity.BackupRequest;
import com.myproject.service.BackupService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

@Service
public class BackupServiceImpl implements BackupService {
    private static final List<String> DEFAULT_TABLES = List.of(
            "user", "station", "train", "train_route", "carriage", "seat",
            "train_schedule", "seat_inventory", "ticket_order", "passenger",
            "audit_log", "refund_record"
    );
    private static final Set<String> ALLOWED_TABLES = Set.copyOf(DEFAULT_TABLES);
    private static final DateTimeFormatter FILE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final JdbcTemplate jdbcTemplate;
    private final Path backupDir;

    @Autowired
    public BackupServiceImpl(JdbcTemplate jdbcTemplate) {
        this(jdbcTemplate, Path.of("backups"));
    }

    BackupServiceImpl(JdbcTemplate jdbcTemplate, Path backupDir) {
        this.jdbcTemplate = jdbcTemplate;
        this.backupDir = backupDir.toAbsolutePath().normalize();
    }

    @Override
    public BackupInfo createBackup(BackupRequest request) {
        List<String> tables = normalizeTables(request == null ? null : request.getTables());
        boolean includeDelete = request == null || request.getIncludeDelete() == null || request.getIncludeDelete();
        String fileName = "train_ticket_backup_" + LocalDateTime.now().format(FILE_TIME_FORMAT) + ".sql";
        Path file = backupDir.resolve(fileName);

        try {
            Files.createDirectories(backupDir);
            StringBuilder sql = new StringBuilder();
            sql.append("-- train_ticket_db data backup\n");
            sql.append("-- created_at: ").append(LocalDateTime.now()).append("\n");
            sql.append("SET FOREIGN_KEY_CHECKS=0;\n\n");
            for (String table : tables) {
                appendTableBackup(sql, table, includeDelete);
            }
            sql.append("SET FOREIGN_KEY_CHECKS=1;\n");
            Files.writeString(file, sql.toString(), StandardCharsets.UTF_8);
            return toBackupInfo(file);
        } catch (IOException e) {
            throw new RuntimeException("备份文件写入失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<BackupInfo> listBackups() {
        if (!Files.exists(backupDir)) {
            return List.of();
        }
        try (var stream = Files.list(backupDir)) {
            return stream
                    .filter(path -> path.getFileName().toString().endsWith(".sql"))
                    .sorted((left, right) -> right.getFileName().toString().compareTo(left.getFileName().toString()))
                    .map(this::toBackupInfo)
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException("读取备份列表失败: " + e.getMessage(), e);
        }
    }

    @Override
    public int restore(String fileName) {
        if (!StringUtils.hasText(fileName) || fileName.contains("/") || fileName.contains("\\")) {
            throw new IllegalArgumentException("备份文件名不合法");
        }
        Path file = backupDir.resolve(fileName).normalize();
        if (!file.startsWith(backupDir) || !Files.exists(file)) {
            throw new IllegalArgumentException("备份文件不存在");
        }

        try {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            List<String> statements = splitSqlStatements(content);
            int executed = 0;
            for (String statement : statements) {
                if (StringUtils.hasText(statement)) {
                    jdbcTemplate.execute(statement);
                    executed++;
                }
            }
            return executed;
        } catch (IOException e) {
            throw new RuntimeException("读取备份文件失败: " + e.getMessage(), e);
        }
    }

    private void appendTableBackup(StringBuilder sql, String table, boolean includeDelete) {
        sql.append("-- table: ").append(table).append("\n");
        if (includeDelete) {
            sql.append("DELETE FROM `").append(table).append("`;\n");
        }

        jdbcTemplate.query("SELECT * FROM `" + table + "`", rs -> {
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            while (rs.next()) {
                sql.append("INSERT INTO `").append(table).append("` (");
                for (int i = 1; i <= columnCount; i++) {
                    if (i > 1) {
                        sql.append(", ");
                    }
                    sql.append("`").append(metaData.getColumnLabel(i)).append("`");
                }
                sql.append(") VALUES (");
                for (int i = 1; i <= columnCount; i++) {
                    if (i > 1) {
                        sql.append(", ");
                    }
                    sql.append(toSqlLiteral(rs.getObject(i)));
                }
                sql.append(");\n");
            }
            return null;
        });
        sql.append("\n");
    }

    private List<String> normalizeTables(List<String> tables) {
        if (tables == null || tables.isEmpty()) {
            return DEFAULT_TABLES;
        }
        List<String> normalized = new ArrayList<>();
        for (String table : tables) {
            if (!ALLOWED_TABLES.contains(table)) {
                throw new IllegalArgumentException("不允许备份未知表: " + table);
            }
            normalized.add(table);
        }
        return normalized;
    }

    private String toSqlLiteral(Object value) throws SQLException {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof Number || value instanceof BigDecimal) {
            return value.toString();
        }
        if (value instanceof Boolean bool) {
            return bool ? "1" : "0";
        }
        if (value instanceof byte[] bytes) {
            return "X'" + HexFormat.of().formatHex(bytes) + "'";
        }
        if (value instanceof java.sql.Date date) {
            return "'" + date.toLocalDate() + "'";
        }
        if (value instanceof LocalDate date) {
            return "'" + date + "'";
        }
        if (value instanceof Timestamp timestamp) {
            return "'" + timestamp.toLocalDateTime().withNano(0) + "'";
        }
        if (value instanceof LocalDateTime dateTime) {
            return "'" + dateTime.withNano(0) + "'";
        }
        if (value instanceof Time time) {
            return "'" + time.toLocalTime() + "'";
        }
        return "'" + value.toString().replace("\\", "\\\\").replace("'", "''") + "'";
    }

    private List<String> splitSqlStatements(String sql) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;

        for (int i = 0; i < sql.length(); i++) {
            char ch = sql.charAt(i);
            char next = i + 1 < sql.length() ? sql.charAt(i + 1) : '\0';
            if (!inSingleQuote && ch == '-' && next == '-') {
                while (i < sql.length() && sql.charAt(i) != '\n') {
                    i++;
                }
                continue;
            }
            if (ch == '\'' && (i == 0 || sql.charAt(i - 1) != '\\')) {
                inSingleQuote = !inSingleQuote;
            }
            if (ch == ';' && !inSingleQuote) {
                statements.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        if (StringUtils.hasText(current)) {
            statements.add(current.toString().trim());
        }
        return statements;
    }

    private BackupInfo toBackupInfo(Path path) {
        try {
            return new BackupInfo(
                    path.getFileName().toString(),
                    Files.size(path),
                    LocalDateTime.ofInstant(Files.getLastModifiedTime(path).toInstant(), java.time.ZoneId.systemDefault())
            );
        } catch (IOException e) {
            throw new RuntimeException("读取备份文件信息失败: " + e.getMessage(), e);
        }
    }
}
