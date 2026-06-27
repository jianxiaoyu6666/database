# 🚂 火车售票管理系统 — 项目记忆

## 基本信息
- **项目名:** train-ticket-management（数据库课程设计）
- **技术栈:** Java 21 + Spring Boot 4.0.7 + MyBatis + MySQL 8.0+
- **项目路径:** `D:\JAVAabout\train_ticket_management`
- **GitHub:** https://github.com/wangxuyao1216/database
- **团队:** 4人协作，老大（liangzheng）是其中之一

## 项目目标
数据库课程设计，要求实现 5 项及以上高阶数据库技术（12项可选）。
答辩时需展示技术实际应用 + 验证有效性。

## 高阶技术实现进度

| 技术 | 状态 | Java代码 | 备注 |
|------|:--:|:--------:|------|
| 数据加密(AES-256-GCM) | ✅ | ✅ | 密码/身份证/手机号加密入库 |
| 存储过程 | ✅ | ✅ | sp_search_user，MyBatis CALLABLE调用 |
| 用户权限管理 | ✅ | ✅ | ADMIN/STAFF/USER 三级 + JWT |
| 索引优化 | ✅ | ✅ | 各表索引设计完成 |
| 全文检索 | ⚠️ | ❌ | station表有FULLTEXT索引，Java未调用 |
| 分区表 | ⚠️ | ❌ | 4表按月分区DDL完成，缺Java实体 |
| 审计日志 | ⚠️ | 🔄 | AOP+触发器架构，INSERT已完成，UPDATE/DELETE待做 |
| 触发器 | 🔄 | 🔄 | user表INSERT触发器已完成，待UPDATE/DELETE |
| 视图 | ❌ | ❌ | |
| 事务并发控制 | ❌ | ❌ | |
| 窗口函数 | ❌ | ❌ | |
| 备份恢复策略 | ❌ | ❌ | |
| 火车票核心业务 | ❌ | ❌ | 仅完成了用户系统 |

**当前得分预估: 3.5/5 项**

## 已修复的问题

### AES加密bug (2026-06-25)
- **问题:** KeyGenerator每次生成不同密钥，encrypt和decrypt用不同钥匙
- **修复:** 改为固定SecretKeySpec（SHA-256派生），加解密共用同一把钥匙
- **文件:** `AESUtil.java`
- **测试:** `AESUtilTest.java` — 8项测试全部通过

### 登录失败 (2026-06-25)
- **原因1:** 查询用 `id` 而非 `username`
- **原因2:** AES-GCM随机IV导致密文不同，无法直接 `WHERE password = 密文`
- **修复:** 按username查 → 解密库里密码 → 明文比对
- **涉及:** `UserMapper.java`, `UserServiceImpl.java`

### UserController修复 (2026-06-25)
- list: `@GetMapping` → `@PostMapping`（GET不带body）+ 补管理员权限校验
- delete: `||` → `&&`（逻辑修正）
- 两处 `"Admin"` → `"ADMIN"`（大小写匹配DB枚举值）

## 项目文件结构

```
src/main/java/com/myproject/
├── TrainTicketManagementApplication.java  ← 入口（含@ServletComponentScan）
├── annotation/
│   └── Log.java          ← 🆕 @Log注解，标记需审计方法
├── AOP/
│   └── LogAspect.java    ← 🆕 切面，SET @audit_operator
├── controller/
│   ├── loginController.java
│   ├── RegisterController.java
│   └── UserController.java
├── entity/
│   ├── User.java
│   ├── audit_log.java
│   ├── logInfo.java
│   ├── OperationType.java
│   └── Result.java
├── exception/
│   ├── AccountDataException.java
│   └── GlobalExceptionHandler.java
├── filter/
│   └── TokenFilter.java
├── mapper/
│   ├── UserMapper.java
│   └── LogMapper.java     ← 🆕 审计日志（SET变量+查询）
├── service/
│   ├── UserService.java
│   └── impl/UserServiceImpl.java
└── util/
    ├── AESUtil.java
    ├── CurreetHolder.java
    └── JwtUtil.java
```

## 数据库核心表

| 表 | 类型 | 说明 |
|----|------|------|
| user | 普通表 | 用户，AES加密密码/身份证/手机 |
| station | 普通表 | 车站，FULLTEXT索引 |
| train | 普通表 | 列车 |
| train_route | 普通表 | 线路（车次-站点关系） |
| train_schedule | 分区表 | 排班，按月RANGE分区 |
| carriage | 普通表 | 车厢 |
| seat | 普通表 | 座位 |
| seat_inventory | 分区表 | 座位库存（区间占用管理） |
| ticket_order | 分区表 | 订单（合并订单+明细） |
| passenger | 普通表 | 常用乘客 |
| audit_log | 分区表 | 审计日志 |
| refund_record | 普通表 | 退票记录 |

## 环境配置关键点

- JDK 21（Spring Boot 4.0.7 硬要求）
- MySQL 8.0+（分区表需要）
- 每人本地改 `application.yaml` 数据库密码，**不要提交**
- AES/JWT密钥不要改，全队保持一致
- Lombok 插件必须装
- 社区版 IDEA 完全够用，不花钱

## SQL文件结构（2026-06-28 重构）

```
sql/
├── schema/
│   └── 01_schema.sql
├── procedures/
│   └── 02_procedures.sql
├── triggers/
│   └── user_triggers.sql    ← INSERT已实现，UPDATE/DELETE待补
└── seed/                     ← 预留：测试数据
```

## 开发日志

### 2026-06-28 — 审计日志 AOP+触发器 双层架构 (老大)

**架构决策:**
- AOP负责传递操作人身份 → `CurreetHolder` → MySQL会话变量 `@audit_operator`
- 触发器负责捕获OLD/NEW数据快照，拼JSON写入 `audit_log`
- 两个高阶技术联动：审计日志 + 触发器，答辩时可做亮点

**已完成:**
- `@Log` 注解 (annotation/Log.java) — 标记需要审计的方法
- `LogAspect` 切面 (AOP/LogAspect.java) — `@Around` 拦截，SET会话变量
- `LogMapper` (mapper/LogMapper.java + XML) — SET变量 + 动态查询
- user表 INSERT 触发器 (sql/triggers/user_triggers.sql)
  - `AFTER INSERT` → 用 `NEW.xxx` 拼 JSON → 写入 audit_log
  - `operated_by` 用 `NEW.id`（注册即操作人本人）
- `UserServiceImpl.register()` 添加 `@Log(TableName="user", operationType=INSERT)`
- 测试类 `testTriggerInsert.java` 验证通过 ✅

**踩坑记录:**
- 测试类身份证号 `11111111111110000X` 年份段"11"不匹配正则 → 校验失败 → INSERT未执行 → 触发器不触发
- 触发器的 `DELIMITER` 拼写错误 `delimiter` → 必须是 `DELIMITER`
- Spring Boot Test 直接调 Service，不走 HTTP/TokenFilter → CurreetHolder 为 null

**待完成:**
- user表 UPDATE/DELETE 触发器
- `UserServiceImpl` update/setStatus/delete 添加 @Log 注解
- AuditLogController 查询接口

### 2026-06-25 — AES加密修复 (前人修)
- 见"已修复的问题"章节

## Git协作

- 仓库: wangxuyao1216/database
- README.md 已写配置指南
- 规则: 每人改不同模块文件，开工先pull，及时push
