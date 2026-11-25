# Order Platform 部署手册（单机版）

一套后端同时服务 **管理端 Web**、**微信小程序顾客端**、**安卓管理 App**。
本目录是完整的可上传部署包，目标环境：**Ubuntu 22.04 / 24.04（1 核 2G 起）**。

---

## 一、目录结构

```text
deploy/
├── package/
│   ├── orpl-service.jar      # 后端可执行 jar（Spring Boot 4 / Java 25）
│   └── orpl-admin.apk        # 安卓管理端安装包
├── web/
│   ├── portal/index.html     # 打开 / 看到的项目门户
│   ├── admin/                # 管理端静态页（原生 HTML + ES Modules）
│   └── download/             # APK 下载目录
├── sql/                      # 建表 + 演示数据 + 两个增量迁移脚本
├── conf/
│   ├── nginx-orpl.conf       # nginx 站点配置
│   ├── orpl.service          # systemd 服务
│   ├── application-prod.yml  # 生产配置（敏感项走环境变量）
│   └── orpl.env.example      # 环境变量模板
└── scripts/
    ├── install.sh            # 服务器初始化（在服务器上跑）
    ├── deploy.ps1            # 本机一键上传 + 部署（在 Windows 上跑）
    └── configure-clients.ps1 # 把 App / 小程序指向正式地址并重新打包
```

服务器上的落地目录统一是 `/opt/orpl`：

```text
/opt/orpl
├── app/orpl-service.jar
├── conf/{application-prod.yml, orpl.env, nginx-orpl.conf, orpl.service}
├── sql/*.sql
├── web/{portal, admin, download}
├── logs/app.log
└── scripts/
```

---

## 二、部署（三步）

### 0. 服务器准备

- 系统：Ubuntu 22.04 / 24.04
- 配置：**2 核 1G 能跑**——脚本检测到内存小于 1.6G 会自动创建 1GB swap、把 MySQL `innodb_buffer_pool_size` 压到 64M、JVM 改成 `-Xmx256m`；**2 核 2G 更稳**，2C/4G 最舒服
- 云厂商**安全组放通 TCP 80**（要 HTTPS 再放 443）
- 记下公网 IP、SSH 端口、root 密码（或密钥）

### 1. 一键上传 + 安装（本地 Windows）

```powershell
cd C:\Users\lican\Desktop\Project\deploy\scripts

# 密钥登录
.\deploy.ps1 -Server 1.2.3.4 -User root -KeyFile C:\Users\你\.ssh\id_rsa

# 密码登录（会提示输入密码）
.\deploy.ps1 -Server 1.2.3.4 -User root
```

脚本做两件事：把 `deploy/` 整个上传到 `/opt/orpl`，然后执行 `install.sh`。

### 2. 安装脚本做了什么

```bash
sudo bash /opt/orpl/scripts/install.sh
```

1. `apt` 安装 nginx、MySQL 8、Redis、curl
2. 下载 **Temurin JDK 25** 到 `/usr/lib/jvm/temurin-25-jdk-amd64`
3. 创建运行用户 `orpl`，准备 `/opt/orpl/{app,conf,logs,web}`
4. 建库 `orpl`、建专用账号 `orpl@127.0.0.1`，导入 `01~04` 四个 SQL（库非空时自动跳过）
5. 给 Redis 设置密码，只监听 `127.0.0.1`
6. 配置 nginx（默认站点被移除，改由 `/etc/nginx/conf.d/orpl.conf` 接管）
7. 注册 `orpl.service` 并开机自启，轮询 `/health` 直到后端就绪

### 3. 验证

| 入口 | 地址 | 说明 |
|---|---|---|
| 项目门户 | `http://IP/` | 四端入口聚合页 |
| 管理端 | `http://IP/admin/` | `admin / 123456` |
| 安卓 APK | `http://IP/download/orpl-admin.apk` | 7 MB，直接安装 |
| 接口文档 | `http://IP/doc.html` | Knife4j，110 个接口 |
| 健康检查 | `http://IP/health` | 返回 `order-platform-ok` |

命令行自检：

```bash
curl http://127.0.0.1:8080/health
curl -i http://127.0.0.1/api/employee/login -H 'Content-Type: application/json' -d '{"username":"admin","password":"123456"}'
```

---

## 三、请求链路

```text
浏览器 / 小程序 / App
        │
        ▼
   nginx :80
   ├── /                    → /opt/orpl/web/portal
   ├── /admin/              → /opt/orpl/web/admin（静态）
   ├── /download/           → /opt/orpl/web/download（APK）
   ├── /api/**              → 127.0.0.1:8080/admin/**   （管理端，令牌头 zmlhsc）
   ├── /user/**             → 127.0.0.1:8080/user/**    （小程序，令牌头 authorization）
   ├── /shopping-cart/**    → 127.0.0.1:8080/shopping-cart/**
   ├── /addressBook/**      → 127.0.0.1:8080/addressBook/**
   ├── /doc.html            → Knife4j
   └── /health              → 后端健康检查
        │
        ▼
   orpl.service (Java 25) :8080  ──  MySQL 8 :3306（仅本机）
                                 └─  Redis  :6379（仅本机，带密码）
```

后端只监听 `127.0.0.1:8080`（`application-prod.yml` 里 `server.address`），外网无法直连 8080。

---

## 四、把客户端指向正式服务器

### 安卓 App

```powershell
.\configure-clients.ps1 -ServerUrl http://1.2.3.4
```

它会用 `-PorplBaseUrl=http://1.2.3.4/admin/` 重新打包 APK（`DEFAULT_BASE_URL` 写死进 BuildConfig），
并同时覆盖 `package/` 和 `web/download/` 下的 APK。之后重新上传 `web` 目录即可。

> App 登录页「高级设置 · 服务器地址」仍可手改，适合临时连别的后端。

### 微信小程序

1. `configure-clients.ps1 -ServerUrl https://你的域名` 会改掉 `miniprogram/config.ts` 的 `API_BASE`
2. 微信开发者工具导入 `C:\Users\lican\Desktop\applet-WeChat`，AppID `wxc69c75f8ef2056d2`
3. 开发阶段勾选「不校验合法域名、web-view、TLS 版本以及 HTTPS 证书」即可直连 `http://IP`
4. 正式发布必须：
   - 域名**已 ICP 备案**，站点启用 HTTPS
   - 微信公众平台 → 开发管理 → 服务器域名，把域名加到 **request 合法域名**
   - 上传代码 → 提交审核 → 发布

> 微信平台不接受 IP 作为服务器域名，所以「别人在微信里直接搜到并打开」必须走域名。
> 只做简历演示时，用开发者工具或「真机调试（不校验域名）」录屏也足够。

### 真实微信登录 / 图片上传（可选）

在 `/opt/orpl/conf/orpl.env` 里补：

```ini
WECHAT_APPID=wx...
WECHAT_SECRET=...
OSS_ENDPOINT=oss-cn-xxx.aliyuncs.com
OSS_BUCKET=...
OSS_REGION=...
OSS_ACCESS_KEY_ID=...
OSS_ACCESS_KEY_SECRET=...
```

然后 `systemctl restart orpl`。未配置时：小程序把登录 code 直接当 openid（开发模式），菜品图片只能填外链。

---

## 五、日常运维

```bash
systemctl status orpl --no-pager -l     # 后端状态
systemctl restart orpl                  # 重启后端
journalctl -u orpl -n 100 --no-pager    # 最近日志
tail -f /opt/orpl/logs/app.log          # 应用日志
nginx -t && systemctl reload nginx      # 改完 nginx 先校验再平滑重载
```

升级后端：

```powershell
# 本地重新 package 后
scp package\orpl-service.jar root@IP:/opt/orpl/app/
ssh root@IP "systemctl restart orpl"
```

数据库备份 / 恢复：

```bash
mysqldump -uroot orpl > /opt/orpl/backup-$(date +%F).sql
mysql -uroot orpl < /opt/orpl/backup-2026-11-24.sql
```

改数据库密码：改 `/opt/orpl/conf/orpl.env` 里的 `DB_PASSWORD`，同时在 MySQL 里 `ALTER USER`，再重启服务。

---

## 六、常见问题

| 现象 | 排查 |
|---|---|
| 浏览器打不开，SSH 正常 | 云厂商安全组是否放通 80；`ss -lntp \| grep :80` |
| 502 Bad Gateway | `systemctl status orpl`；多半是 MySQL/Redis 没起来或密码不一致 |
| 管理端能开但接口 401 | 令牌 12 小时过期，重新登录；或 nginx `/api/` 反代没生效 |
| 上传图片 413 | nginx `client_max_body_size` 已是 100m；确认改的是 `/etc/nginx/conf.d/orpl.conf` |
| 后端启动即退出 | `journalctl -u orpl -n 200`；常见为 JDK 路径、内存不足（`-Xmx512m` 可调小） |
| MySQL 安装后 root 登不上 | Ubuntu 下 root 默认 `auth_socket`，用 `sudo mysql` 进入 |
| APK 装上登录报网络异常 | App 内「高级设置」地址应是 `http://IP/admin/`，且手机能访问该 IP |
| 小程序真机报域名不合法 | 开发者工具勾选「不校验合法域名」，或改用已备案 HTTPS 域名 |

---

## 七、本部署包已验证项

- Ubuntu 22.04 + JDK 25 + MySQL 8.0.46 + Redis 6 + nginx 1.18 全链路启动
- 数据库 `01 → 04` 四个脚本顺序导入无报错
- `/health`、`/doc.html`、`/v3/api-docs` 经 nginx 访问正常
- 管理端登录 `admin/123456` 与订单/菜品/堂食接口连通
- APK / 门户 / 管理端静态资源经 nginx 正常返回

---

## 八、实际部署记录（2026-11-25）

| 项 | 值 |
| --- | --- |
| 服务器 | 阿里云 ECS `i-bp16aroypdq6r1ok7mkc`，cn-hangzhou-k，2 vCPU 2 GiB（ecs.e-c1m1.large），Ubuntu 22.04.5 LTS |
| 公网地址 | http://121.40.186.5 |
| 登录方式 | 实例仅 publickey，无密码入口；本次通过云助手 `RunCommand`（root）执行，效果等同 `deploy.ps1` 的安装步骤 |
| 安全组 | `sg-bp16aroypdq6r1obbxcw` 增开 TCP 80/80（0.0.0.0/0） |
| JDK | 华为云镜像 OpenJDK 25（官方 adoptium 源境内约 13 KB/s，不可用） |
| 低内存 | 1 GiB swap；MySQL `innodb_buffer_pool_size=64M` + `performance_schema=OFF`；JVM `-Xms128m -Xmx384m` |
| 部署路径 | `/opt/orpl/{app,conf,logs,sql,web,scripts}`，systemd 单元 `/etc/systemd/system/orpl.service` |
| 运行状态 | orpl.service active(running)，常驻约 316 MB；MySQL 18 张表；Redis `PONG` |

公网验收结果：`/`、`/admin/`、`/health`、`/doc.html`、`/v3/api-docs` 全部 200；`/download/orpl-admin.apk` 200 且 `Content-Length=7481398`；`admin/123456` 登录返回 token，带 `zmlhsc` 令牌查询员工/菜品分页正常。

注意：纯 IP 无域名、无 HTTPS，微信小程序只能在开发者工具或真机调试中演示，不能正式发布。

## 九、2026-11-25 补丁：安卓 APK 地址前缀

- 安卓默认地址由 `/admin/` 改为 `/api/`（`http://<IP>/api/`）：经 nginx 部署时接口前缀是 `/api/`，`/admin/` 只是静态页目录，`POST /admin/employee/login` 会 405
- 直连后端 8080 时仍是 `http://<IP>:8080/admin/`，两种写法都行，App 内「高级设置」可手改
- 服务器上的 `orpl-admin.apk` 已替换为新包（8,527,152 字节）

## 十、上传图片（2026-11-25 起支持本地存储）

- 默认 `STORAGE_TYPE=auto`：配了阿里云 OSS 就传 OSS，没配就存 `/opt/orpl/web/uploads`，由 nginx 的 `location /uploads/` 直接提供访问
- 接口 `POST /admin/common/upload`（外部经 nginx 是 `POST /api/common/upload`）返回**绝对地址**，形如 `http://<IP>/uploads/2026/11/<uuid>.png`
- 目录权限：`/opt/orpl/web/uploads` 属主 `orpl:orpl`，`install.sh` 会自动创建
- 切换 OSS：填好 `OSS_*` 五个变量后 `systemctl restart orpl` 即可，无需改代码
- 本地开发：不配 OSS 也能上传，文件在应用工作目录 `./uploads`，`http://127.0.0.1:8080/uploads/...` 可直接打开
