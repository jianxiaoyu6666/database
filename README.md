# 火车售票管理系统 - 环境配置指南

## 必装环境

| 工具 | 版本要求 | 说明 |
|------|----------|------|
| JDK | **21** | 项目用 Spring Boot 4.0.7，最低要求 JDK 21 |
| MySQL | 8.0+ | 本地开发数据库 |
| Maven | 3.8+ | 构建工具（或用 IDEA 自带的） |
| IntelliJ IDEA | 2024+ | 推荐 IDE |

---

## 第一步：克隆项目

```bash
git clone https://github.com/wangxuyao1216/database.git
cd database
```

## 第二步：建数据库

打开 MySQL 命令行或 Navicat，执行以下两步：

```sql
-- 1. 先确保数据库存在
CREATE DATABASE IF NOT EXISTS train_ticket_db
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- 2. 导入建表脚本（在 IDEA 里打开 src/main/resources/static/01_schema.sql 全选执行）
```

> ⚠️ 脚本里有分区表（PARTITION BY RANGE），必须用 MySQL 8.0+  

## 第三步：改数据库配置

每个人本地 MySQL 密码不一样，改 `src/main/resources/application.yaml`：

```yaml
spring:
  datasource:
    password: 你的MySQL密码    # ← 改这里
    username: root             # ← 改这里（如果用其他用户）
    url: jdbc:mysql://localhost:3306/train_ticket_db
```

> ⚠️ **不要 commit 你自己的 application.yaml**。密码是敏感信息，git 提交前先 `git stash` 掉。

---

## 第四步：执行存储过程

`src/main/resources/static/用户查询.sql` 需要在 MySQL 里执行一次，创建 `sp_search_user` 存储过程。  
在 IDEA 里打开这个文件，连上数据库，全选执行即可。

---

## 第五步：运行测试

在 IDEA 里右键 `AESUtilTest` → Run，8 个测试全部通过说明加密模块正常。

然后运行 `TrainTicketManagementApplication` 启动项目。

---

## ⚠️ 注意事项

### 1. 配置文件不要提交
`application.yaml` 里有数据库密码，改完记得：
```bash
git update-index --assume-unchanged src/main/resources/application.yaml
```
这样 Git 会忽略你对这个文件的本地修改，不会误推。

### 2. AES 密钥保证一致
`AESUtil.java` 里 `SECRET_KEY = "***"` 和 `JwtUtil.java` 里 `key = "project"` 所有人必须一样，不然跨人没法验证 token、也没法解密数据。这两个不要动。

### 3. 分区表注意事项
`train_schedule`、`seat_inventory`、`ticket_order`、`audit_log` 这 4 张表用的是 RANGE 分区（按月），分区在 2025 年 12 月之后的数据会进 `p_future` 分区。如果要在更远的日期建数据，需要提前用 `ALTER TABLE ADD PARTITION` 加新分区。

### 4. 测试接口用 Apifox/Postman
启动后端口默认 8080，示例请求：

```
POST http://localhost:8080/register
{
  "username": "test",
  "password": "123456",
  "realName": "张三",
  "idNumber": "320102200001011234",
  "phone": "13800138000",
  "userType": "USER"
}
```

### 5. IDEA Lombok 插件
如果 IDEA 报 `@Data` 之类的注解找不到，去 Settings → Plugins 搜索 Lombok 安装。

---

## 日常协作流程

```bash
# 开工
git pull

# 写代码...

# 收工前
git stash              # 暂存 application.yaml 的改动
git add .
git commit -m "xxx"
git push
git stash pop          # 恢复你的本地配置
```
