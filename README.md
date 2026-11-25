# order-platform（orpl）· 单仓全栈工程

点餐 + 堂食一体化餐饮管理平台，一套 Spring Boot 后端同时服务**管理端 Web**、**安卓管理 App**、**微信小程序顾客端**，配套 nginx 站点配置与 Ubuntu 单机部署包。

本仓库把原先分散在四个目录里的内容合并为一个仓库：

| 原目录 | 现在的位置 | 说明 |
| --- | --- | --- |
| `Project/order-platform` | `backend/` | Spring Boot 后端源码（Maven 多模块） |
| `Project/deploy` | `deploy/` | 整套部署包（静态产物、APK、SQL、nginx/systemd、脚本） |
| `applet-WeChat` | `miniprogram/` | 微信小程序顾客端 |
| `anz` | `android-app/` | 安卓管理端 App |
| `nginx-1.20.2` | `nginx/` | 本地开发用 nginx 配置（二进制未入库） |

## 四端一览

| 端 | 目录 | 技术形态 | 入口 / 运行方式 |
| --- | --- | --- | --- |
| 后端服务 | `backend/` | Spring Boot 4.0.9 · Java 25 · MySQL 8 · Redis · MyBatis-Plus · Knife4j | `http://127.0.0.1:8080`，文档 `/doc.html` |
| 管理端 Web | `web-admin/` | 原生 HTML + CSS + ES Modules，**无 npm、无构建步骤** | 由 nginx 托管（`http://<IP>/`），账号 `admin / 123456` |
| 项目门户 | `portal/` | 单文件静态页 | `http://<IP>/`，聚合四端入口 |
| 微信小程序 | `miniprogram/` | 微信原生 TypeScript，TypeScript 编译插件由开发者工具承担 | 开发者工具导入，AppID `wxc69c75f8ef2056d2` |
| 安卓管理端 | `android-app/` | Kotlin + ViewBinding + OkHttp，Gradle 9.9.2 / AGP 9.5.2 | `./gradlew assembleDebug`，包名 `com.mzlhsc.orpl.admin` |
| 部署包 | `deploy/` | shell / PowerShell 脚本 + 静态资源 + SQL | 详见 `deploy/README.md` |

请求链路：`浏览器 / 小程序 / App` → `nginx :80` → `/api/**` 反向代理 → `orpl.service :8080` → `MySQL :3306` / `Redis :6379`（后两者仅监听本机）。

## 目录结构

```text
orpl-fullstack/
├── backend/                # 后端源码
│   ├── orpl-common/        # 统一结果/异常、JWT、拦截器、自动配置、工具类
│   ├── orpl-pojo/          # 各领域 PO / DTO / VO
│   ├── orpl-service/       # controller / service / mapper / 启动类
│   ├── mysql/              # 建表 + 演示数据 + 增量迁移 SQL
│   └── docs/               # 接口文档、操作日志、数据库文档
├── web-admin/              # 管理端静态前端（可直接托管）
├── portal/                 # 四端入口门户页
├── miniprogram/            # 微信小程序（miniprogram/ 为源码根）
├── android-app/            # 安卓 App 源码 + preview/ 界面截图
├── deploy/
│   ├── package/            # orpl-admin.apk（后端 jar 见下方说明，未入库）
│   ├── web/                # admin / portal / download 三份静态产物
│   ├── sql/                # 01~04 部署用 SQL
│   ├── conf/               # nginx 站点、systemd 单元、生产配置、环境变量模板
│   └── scripts/            # install.sh / deploy.ps1 / configure-clients.ps1
├── nginx/conf/             # 本地开发 nginx 配置（nginx.conf 等）
└── README.md
```

## 打包产物与构建方式

| 产物 | 位置 | 是否需要构建 |
| --- | --- | --- |
| 管理端 Web 静态包 | `web-admin/` 与 `deploy/web/admin/` | **不需要**。原生 HTML + ES Modules，改完文件刷新即生效，本仓库里就是最终产物 |
| 项目门户 | `portal/` 与 `deploy/web/portal/` | 不需要，单文件静态页 |
| 安卓 APK | `deploy/package/orpl-admin.apk`、`deploy/web/download/orpl-admin.apk` | 已打包入库（8.1 MB，2026-11-25 11:20 构建，晚于全部源码改动）；需要重打时见下 |
| 微信小程序 | `miniprogram/` | TS 由微信开发者工具编译上传，无需本地 npm 构建 |
| 后端可执行 jar | **未入库**（见下方说明） | `mvn -DskipTests package` |

管理端 Web 与门户在 `web-admin/`、`portal/` 和 `deploy/web/` 下各有一份：前者便于单独查看/修改，后者保证 `deploy/` 自带的部署包是自包含的，改前端时请两份一起覆盖。

### 重新打包安卓 APK

```powershell
cd android-app
# local.properties 需指向本机 Android SDK（该文件不入库，自行创建）
# sdk.dir=D:/Jobs/AppFile/android-studio
$env:JAVA_HOME = "D:\Jobs\App\android-studio\jbr"
.\gradlew assembleDebug -PorplBaseUrl=http://<服务器IP>/api/
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

`-PorplBaseUrl` 会把默认后端地址写进 `BuildConfig.DEFAULT_BASE_URL`；经 nginx 部署时前缀是 `/api/`，直连 8080 时是 `http://<IP>:8080/admin/`。重打后复制到 `deploy/package/` 与 `deploy/web/download/` 两处。

### 重新打包后端 jar

```powershell
cd backend
mvn -DskipTests package
# 产物 orpl-service/target/orpl-service.jar（约 100.6 MB）
Copy-Item orpl-service\target\orpl-service.jar ..\deploy\package\
```

## 快速开始（本地）

1. **MySQL 8**：建库 `orpl`（utf8mb4），导入 `backend/mysql/01-schema.sql`、`02-demo-data.sql`。
2. **Redis**：默认 `127.0.0.1:6379`，密码 `root`，密码不同请改 `backend/orpl-service/src/main/resources/application.yml`。
3. **后端**：
   ```powershell
   cd backend
   mvn spring-boot:run
   ```
   起在 `8080`，`/health` 返回 `order-platform-ok`。
4. **管理端 Web**：把 `web-admin/` 放到 nginx 的 `html/admin`（或用 `deploy/conf/nginx-orpl.conf` 里的绝对路径写法），`/api/**` 反代到 `8080/admin/**`，浏览器访问 `http://127.0.0.1/`。
5. **小程序**：微信开发者工具导入 `miniprogram/`，关闭「校验合法域名」，`miniprogram/miniprogram/config.ts` 的 `API_BASE` 现指向线上演示服 `http://121.40.186.5`，本地联调改成 `http://127.0.0.1:8080`。
6. **安卓 App**：Android Studio 打开 `android-app/`，或命令行 `./gradlew assembleDebug`。

## 部署到服务器

完整步骤见 [`deploy/README.md`](deploy/README.md)：本地跑 `deploy/scripts/deploy.ps1` 上传 + `install.sh` 安装，落地目录 `/opt/orpl`，systemd 单元 `orpl.service`，nginx 站点 `conf/nginx-orpl.conf`。

线上演示环境：`http://121.40.186.5`（阿里云 ECS，Ubuntu 22.04，纯 IP 无域名，微信小程序仅能开发者工具 / 真机调试演示）。

## 未入库内容及原因

| 未入库 | 原因 | 如何获得 |
| --- | --- | --- |
| `deploy/package/orpl-service.jar` | 约 **100.63 MB**，超过 GitHub 单文件 100 MB 硬限制 | `mvn -DskipTests package` 后复制回 `deploy/package/` |
| `nginx.exe` 及 `logs/`、`temp/`、`contrib/`、`docs/` | 第三方 Windows 二进制与运行目录，无版本意义 | 用官方 nginx 1.20.2 发行包，配置取 `nginx/conf/` |
| `.git`、`target/`、`build/`、`.gradle/`、`.idea/`、`logs/`、`node_modules/`、`local.properties`、`uploads/` | 构建缓存、IDE 与本地环境相关 | 各项目 `mvn` / `gradle` / `npm` 自动重建 |
| 微信小程序 `preview-qr.png` | 预览二维码有效期短 | 开发者工具重新生成 |

## 文档

- 接口文档：`backend/docs/接口文档.md`（110 个接口，与 Knife4j `/doc.html` 对应）
- 操作日志：`backend/docs/操作日志.md`
- 数据库文档：`backend/docs/数据库文档.md`
- 部署手册：`deploy/README.md`
- 管理端说明：`web-admin/README.md`
- 小程序说明：`miniprogram/README.md`
- 安卓说明：`android-app/README.md`
- 本地 nginx 说明：`nginx/README.md`

## 安全提示

生产配置 `deploy/conf/application-prod.yml` 中的数据库密码、JWT 密钥、微信 AppID/Secret、OSS 密钥均通过环境变量注入，模板见 `deploy/conf/orpl.env.example`；不要把这些真实值提交到仓库。
