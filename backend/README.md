# order-platform

`order-platform` 是由 `D:\Jobs\Project\ordpf-mzlhsc` 微服务后端复制并改造得到的**单体 Spring Boot 工程**。改造过程不改动原项目，目标工程保留已有 `orpl-common`、`orpl-pojo`、`orpl-service` 三个 Maven 模块，不新增业务模块。

## 技术栈

| 组件 | 版本/说明 |
|---|---|
| Spring Boot | 4.0.9 |
| Java | 25 |
| MySQL | 8.0.34，库名 `orpl` |
| Redis | 3.2.100 客户端兼容，默认 `127.0.0.1:6379` |
| MyBatis-Plus | 3.5.18.1 |
| Redisson | 3.50.0，仅连 Redis，用于库存锁 |
| Knife4j | 5.8.2，`/doc.html` |
| 阿里云 OSS | 3.18.5，可选，配置为空时不影响启动 |

中间件只保留 **MySQL + Redis**；已移除 Nacos、Gateway、Sentinel、Seata、Feign 的运行依赖。

## 模块结构

```text
order-platform
├── orpl-common     # 统一结果/异常、JWT、UserContext、拦截器、自动配置、工具类
├── orpl-pojo       # 各领域 PO / DTO / VO
├── orpl-service    # controller / service / mapper / 领域查询服务 / 启动类
├── mysql
│   ├── 01-schema.sql
│   └── 02-demo-data.sql
├── docs
│   ├── 接口文档.md
│   ├── 操作日志.md
│   └── 数据库文档.md
└── pom.xml
```

启动类：

`orpl-service/src/main/java/com/mzlhsc/orpl/OrderPlatformApplication.java`

## 本地启动

### 1. 准备 MySQL

```sql
CREATE DATABASE IF NOT EXISTS orpl
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_general_ci;
```

```bash
mysql -uroot -proot orpl < mysql/01-schema.sql
mysql -uroot -proot orpl < mysql/02-demo-data.sql
```

### 2. 准备 Redis

确保 Redis 已启动，默认密码 `root`。如本机密码不同，修改 `orpl-service/src/main/resources/application.yml` 中 `spring.data.redis.password`。

### 3. 修改配置

`orpl-service/src/main/resources/application.yml` 默认配置：

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/orpl?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: root
  data:
    redis:
      host: 127.0.0.1
      port: 6379
      password: root
      database: 0
```

微信与 OSS 配置可通过环境变量注入：

```text
WECHAT_APPID / WECHAT_SECRET
OSS_ENDPOINT / OSS_BUCKET / OSS_REGION / OSS_ACCESS_KEY_ID / OSS_ACCESS_KEY_SECRET
```

未配置微信 appid/secret 时，开发环境把登录 code 当作 openid 使用，便于本地联调。

### 4. 编译打包

```powershell
cd C:\Users\lican\Desktop\Project\order-platform
mvn clean package -DskipTests
```

### 5. 启动

默认 8080：

```powershell
java -jar orpl-service\target\orpl-service-0.0.1-SNAPSHOT.jar
```

如果本机原微服务网关仍占用 8080，改用 8095：

```powershell
java -jar orpl-service\target\orpl-service-0.0.1-SNAPSHOT.jar --server.port=8095
```

### 6. 验证

```powershell
curl http://127.0.0.1:8095/health
# order-platform-ok
```

在线接口文档：

- Knife4j：`http://127.0.0.1:8095/doc.html`
- OpenAPI JSON：`http://127.0.0.1:8095/v3/api-docs`
- Swagger UI：`http://127.0.0.1:8095/swagger-ui/index.html`

## 认证方式

| 身份 | 请求头 | 登录接口 | 默认账号/开发 code |
|---|---|---|---|
| 管理端员工 | `zmlhsc` 或 `mzlhsc` | `POST /admin/employee/login` | `admin / 123456` |
| 小程序用户 | `authorization` | `POST /user/user/login` | `code=ordpf-mp-local-user` |

规则：

- 员工令牌只允许访问 `/admin/**`。
- 用户令牌只允许访问 `/user/**`、`/addressBook/**`、`/shopping-cart/**`。
- 登录、注册、商品浏览、扫码看桌台、接口文档、`/health` 为公开白名单。
- 单体不再保留 `/inner/**` 内部 HTTP 接口；跨模块统计查询直接调用各领域的 `XxxQueryService`，同一进程内完成。

## 从微服务到单体的关键改造

1. **唯一启动类**：只保留 `OrderPlatformApplication`，其他原 `*Application` 全部跳过。
2. **删除假 API 层**：原 `api/client` 的四个伪 Feign 接口、`LocalXxxClient` 适配器和 `/inner` 控制器已全部删除；跨模块调用改为直接注入领域 `Service` / `XxxQueryService`，DTO/VO 归入各自领域包。
3. **JWT 普通拦截器**：原网关 `AuthGlobalFilter` 改为 `TokenAuthInterceptor` + `WebMvcConfig`，校验后写 `UserContext`，不再信任客户端直接传 `empId` / `userId`。
4. **事务本地化**：移除 Seata `@GlobalTransactional`，下单使用本地 `@Transactional`。
5. **限流熔断降级**：移除 Sentinel 注解与规则配置，保留 Redisson 库存锁。
6. **自动配置收敛**：`CommonAutoConfiguration` 只注册异常处理、自动填充与 OpenAPI，避免重复 Bean。

## 已验证

- `mvn clean package -DskipTests` 构建成功。
- 8095 端口启动成功，`/health` 返回 `order-platform-ok`。
- 管理端：登录、分类分页、菜品分页、套餐分页、员工分页、桌台列表、店铺状态、工作台、营业额报表、订单条件查询。
- 用户端：登录、店铺状态、分类、菜品、套餐、桌台、地址簿、购物车、历史订单、订单详情、堂食开台/加菜/下单。
- 订单提交/取消链路、堂食开台/加菜/下单/结账链路已跑通。
- 身份隔离：匿名访问管理端 401；用户令牌访问管理端 401；员工令牌访问用户端 401。
- 删除 `api` 假 Feign 层后重新构建、启动 8095，购物车增删、普通下单/取消、堂食开台/加菜/下单/取消、工作台与四类报表全部通过；`/inner/**` 已无任何映射，访问返回 404。
- Knife4j `/doc.html`、`/v3/api-docs`、`/swagger-ui/index.html` 均可访问。

## 注意

- 原项目 `D:\Jobs\Project\ordpf-mzlhsc` 未被修改。
- 改造前目标工程备份位于 `C:\Users\lican\Desktop\Project\order-platform-backup-20261123-020537`。
- 当前 `orpl` 库包含迁移演示数据与联调测试数据；需要干净环境时重新执行 `mysql/01-schema.sql` + `mysql/02-demo-data.sql`。
- 生产环境请把 MySQL/Redis 密码、微信与 OSS 密钥改为环境变量或配置中心，不要使用代码中的开发默认值。
- 本工程当前只包含后端，未复制原项目的前端/小程序代码。
