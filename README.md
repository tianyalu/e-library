# 电子图书馆借阅服务

图书借阅后端，Java 17 + Spring Boot 3.3。功能覆盖书目浏览检索、借书、还书、续借、
查看在借书籍和逾期罚金，另有一个库存对账的运维接口。

借书是一次资源转移：库存减一、借阅记录加一，和下单扣库存属于同一类问题。
实现上主要处理四件事：不超卖、幂等、事务边界、账实一致，下面逐条说明。

## 怎么跑起来

只装了 Docker 的话：

```bash
docker compose up --build -d
curl http://localhost:8081/actuator/health      # {"status":"UP"}
curl http://localhost:8080/api/v1/books         # 已灌入 11 本书
```

宿主端口是 `mysql:33066`、`redis:63790`（开发机上 3306 / 6379 被占用），容器内仍是标准端口。
`--build` 会在容器内从源码编译，需要能连 Docker Hub 和 Maven Central，首次约 5-10 分钟。

网络不方便时可以先本机打包，再让 compose 用现成 jar 启动：

```bash
mvn clean package -DskipTests
docker compose up -d --no-build
```

演示账号（密码均为 `password123`）：`librarian@elibrary.test` 为管理员，
`grace@ / alan@ / barbara@elibrary.test` 为普通会员。

Swagger：<http://localhost:8080/swagger-ui.html>，OpenAPI JSON：`/v3/api-docs`。

## 接口

| 方法 | 路径 | 谁能用 | 说明 |
|---|---|---|---|
| POST | `/api/v1/auth/token` | 所有人 | 账号换 JWT |
| GET | `/api/v1/books?q=&category=&author=&page=&size=` | 所有人 | 浏览检索，结果进 Redis 缓存 |
| GET | `/api/v1/books/{id}` | 所有人 | 详情，带实时可借数 |
| POST | `/api/v1/loans` | 会员 | 借书，可带 `Idempotency-Key` |
| POST | `/api/v1/loans/{id}/return` | 会员 | 还书 |
| POST | `/api/v1/loans/{id}/renew` | 会员 | 续借 |
| GET | `/api/v1/loans`、`/api/v1/loans/{id}` | 会员 | 查自己的借阅，别人的返回 404 |
| GET | `/api/v1/members/me/summary`、`/fines` | 会员 | 额度、欠费 |
| POST | `/api/v1/members/me/fines/{id}/settle` | 会员 | 缴费 |
| GET | `/api/v1/ops/reconciliation` | 管理员 | 库存与副本的核对报告 |

错误统一返回 RFC 7807 格式，带业务码和 `traceId`：

```json
{"type":"urn:elibrary:copy_not_available","title":"No copy of this book is currently available",
 "status":409,"detail":"No available copy for bookId=7 at the moment",
 "code":"COPY_NOT_AVAILABLE","traceId":"6f1c2e2a-...","timestamp":"2026-10-29T04:12:33.128Z"}
```

客户端的错误一律是 4xx：参数类型不对返回 400 `VALIDATION`，凭证错误返回 401
`INVALID_CREDENTIALS`。这两类都抛业务异常，由 `GlobalExceptionHandler` 显式处理，
不会被兜底分支包成 500。

## 设计取舍

### 1. 不超卖：一条带条件的更新

扣库存只有这一句：

```sql
UPDATE book SET available_copies = available_copies - 1
 WHERE id = ? AND available_copies > 0;
```

影响行数为 0 代表没抢到，返回 409。并发控制落在这条语句上，应用层不加锁。

另有三道约束配合：副本行 `LIMIT 1 FOR UPDATE` 挑出具体哪一册；
`UNIQUE(member_id, book_id, active_flag)` 防重复借阅（`active_flag` 是虚列，
已归还的行算出 NULL，历史记录不受影响）；`@Version` 防跨行更新冲突。

没有引入 Redis 分布式锁：锁过期、误删别人的锁、Redis 抖动都是额外的失败模式，
而这里的问题一条原子 SQL 就能表达完。前提是单主库，分库后扣减要改成分片内的原子操作。

### 2. 幂等：重试只产生一次副作用

借书和续借接受 `Idempotency-Key`。首次成功返回 201；同 Key 同载荷重放返回 200
和第一次的响应体（`loanId`、`dueAt` 都相同）；同 Key 换载荷返回 409
`IDEMPOTENCY_KEY_REUSED`。

并发重试由 `idempotency_record` 的唯一索引裁决，不是先查后写。还书天然幂等，
第二次调用返回 `alreadyReturned: true`。

### 3. 事务边界：五步一个事务，事务内不发网络请求

借书按顺序做五件事：规则校验 → 扣库存 → 锁副本行 → 写借阅记录 → 写发件箱。
五步在同一个事务里，任一步失败整体回滚。缓存失效事件在事务提交之后才发，
避免"缓存已删、事务回滚"的脏状态。

事件走 transactional outbox：事件行与业务数据同一事务落库，后台任务扫表投递。
代价是投递有延迟，默认每秒轮询一次。

### 4. 账实一致

`book.available_copies` 是查询用的快数，`book_copy.status` 是实物账。两个口径分开存，
用 `/api/v1/ops/reconciliation` 可以核对；借阅路径上发现账实不符时先回补库存再返回错误，
不让偏差继续累积。

库存不进 Redis：缓存只放书目这类读多写少的数据，能不能借必须在事务里判断。

## 验证情况

```bash
mvn clean verify      # 20 个测试
```

| 测试 | 说明 |
|---|---|
| `MoneyTest` | 舍入、精度、不同币种相加必须报错 |
| `LoanPolicyTest` | 到期日、续借起算点、"起步一天"计费 |
| `LoanTest` | 借阅状态机：重复归还、逾期标记、续借次数上限 |
| `BorrowConcurrencyTest` | 32 个线程抢 1 本，断言只有 1 个成功、其余全是"无库存"，且账实相符 |
| `LoanApiIntegrationTest` | HTTP 契约：令牌、幂等重放与冲突、幂等归还、401、公开浏览 |
| `ArchitectureGuardTest` | 扫源码检查 `domain` 不出现 Spring / JPA / Jackson 的 import（自己写的扫描，没引 ArchUnit） |

CI（`.github/workflows/ci.yml`）不使用 H2，而是起 MySQL 8 和 Redis 服务容器再跑
`mvn clean verify`（profile 为 `mysql`），表结构与迁移脚本的偏差在 CI 阶段就能暴露。

## 配置

业务参数在 `elibrary.*` 下，由 `LibraryProperties`（record + `@ConfigurationProperties`）
绑定。`application.yml` 里给了默认值，部署时用环境变量覆盖。

| 配置项 | 默认值 | 含义 |
|---|---|---|
| `elibrary.policy.loan-period-days` | 14 | 借阅天数 |
| `elibrary.policy.renewal-days` | 7 | 每次续借天数 |
| `elibrary.policy.max-renewals` | 1 | 续借次数上限 |
| `elibrary.policy.max-active-loans` | 5 | 同时可借数量 |
| `elibrary.policy.fine-per-day` | 0.50 | 每日罚金 |
| `elibrary.policy.currency` | CNY | 币种 |
| `elibrary.outbox.batch-size` / `delay-millis` | 100 / 1000 | 发件箱扫描批次与间隔（毫秒） |
| `elibrary.catalog.max-page-size` | 100 | 分页上限 |
| `security.jwt.secret` / `access-token-ttl-seconds` | 环境变量 / 3600 | 令牌 |
