# EVOPS 基础工作区

印刷厂生产管理（第一批）：机台台账、印单全量记录、按印单号与日期组合检索。
付印数据交换（第二批）：与客户自助下单平台对接，XML 报文收发、按印单号归档留原文、失败重试。

技术栈：Java 8、Spring Boot 2.7、MyBatis-Plus、H2 本地文件数据库、Shiro、Thymeleaf。

## 启动

1. 执行 `mvn -q -DskipTests compile` 验证构建。
2. 执行 `mvn spring-boot:run` 启动应用；启动时自动执行 `src/main/resources/schema.sql`。
3. H2 数据文件默认写入工作区 `data/evops`。如需修改路径，编辑 `application.yml` 中 `jdbc:h2:file:` 连接串。

> 离线环境若拉不到 Maven Central，可在 `~/.m2/settings.xml` 配置阿里云镜像
> `https://maven.aliyun.com/repository/public`。

## 已交付接口（先满足建档与查询）

操作人通过请求头透传（鉴权后续接入）：`X-Operator-Id`、`X-Operator-Name`、可选 `X-Request-No`（不传则自动生成）。

机台台账：

- `POST   /api/machines`          机台建档（机台号唯一，重复拒绝并提示已存在）
- `GET    /api/machines`          条件分页查询（机台号/名称/类型/状态）
- `GET    /api/machines/{id}`     机台详情
- `DELETE /api/machines/{id}`     删除（只打 deleted 标记，不物理删除）

印单：

- `POST   /api/print-orders`      印单建档（同事务写 CREATE 正向记录）
- `GET    /api/print-orders`      按印单号 + 生产日期区间（+客户/状态）组合分页检索
- `GET    /api/print-orders/{id}` 印单详情
- `GET    /api/print-orders/{id}/records`  追溯链（建档/作废全量记录）
- `POST   /api/print-orders/{id}/void`     作废（写 VOID 反向记录并翻转状态）
- `DELETE /api/print-orders/{id}` 删除（只打 deleted 标记）

付印数据交换（对接客户自助下单平台）：

- `POST   /api/exchange/print-orders/{id}/send`  生成付印报文并发送（先归档留原文，再调平台）
- `POST   /api/exchange/messages/{id}/retry`     失败重试（重发归档原文，仅 PENDING/FAILED 可重试）
- `GET    /api/exchange/messages`         报文归档检索（按印单号 + 方向/状态，分页）
- `GET    /api/exchange/messages/{id}`    报文详情（含原文）
- `POST   /api/exchange/inbound`          接收平台来文（application/xml，解析校验后按印单号归档）

发送地址配置：`application.yml` 的 `exchange.gateway-url`（也可用环境变量 `EXCHANGE_GATEWAY_URL`），
指向对方接收接口，如 `http://<平台地址>/api/exchange/inbound`。未配置时发送直接失败并归档，配置后可重试。

## 关键规则

- 印单号、机台号加唯一约束；同一印单号重复建档直接拒绝并提示已存在。删除只打标记，号不释放。
- 作废不走删除：写一条 VOID 反向记录，以 `reverse_of_id` 指回原 CREATE 记录，保留完整追溯链。
- 审计字段随写落库：`request_no` 请求号、`create_by/operator_name` 操作人、`create_time/update_time` 时间、`version` 乐观锁版本。
- 用纸量按开数与克重估算：理论用纸 `CEIL(印数/开数)`；`waste_sheets` 损耗、`add_sheets` 加放按各自比率单列，合计 `total_sheets`，总重量 `paper_weight_kg`。
- 印单与追溯记录两张表在同一事务写入，要么全成要么全不动（见 `PrintOrderTransactionTest`）。
- 报文格式规范：日期 `yyyy-MM-dd`、时间 `yyyy-MM-dd HH:mm:ss`、金额定点 2 位小数、比率 4 位、重量 3 位；
  往返转换内容不丢失（见 `PrintOrderMessageCodecTest`）。
- 报文留原文：发出/接收的 XML 原文都落 `t_exchange_message.xml_content`，按印单号归档检索；
  重试按归档原文重发，不重新生成报文。
- 发送与归档分步提交：先归档 PENDING 再调平台，成功 SENT、失败 FAILED 记原因；FAILED/PENDING 可重试。
- 平台来文解析失败也留原文归档（印单号记 UNKNOWN），便于对账排查；同一报文ID重复推送直接拒绝。

## Schema 交付副本

工作区内 `docs/schema/evops.sql` 为交付副本，与 `src/main/resources/schema.sql` 保持一致
（运行环境根目录 `/docs` 不可写，故副本置于工作区内）。
