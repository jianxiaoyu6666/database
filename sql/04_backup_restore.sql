-- ============================================
-- 备份恢复说明
-- 后端接口会在项目 backups/ 目录生成数据备份 SQL。
-- 备份内容按表输出 DELETE + INSERT，可用于课程验收演示。
-- ============================================

-- 1. 登录管理员后调用:
-- POST /api/admin/backups
-- body: {"tables":["user","station","train"],"includeDelete":true}

-- 2. 查看备份:
-- GET /api/admin/backups

-- 3. 恢复备份:
-- POST /api/admin/backups/{fileName}/restore

-- 4. 如果需要手工恢复，也可在 MySQL 中执行 backups/ 下生成的 .sql 文件。
