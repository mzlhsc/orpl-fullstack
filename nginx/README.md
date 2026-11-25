# 本地开发用 nginx 配置

这里只保留 `nginx-1.20.2` 的**配置**（`conf/`），`nginx.exe` 及其 `logs/`、`temp/`、`contrib/`、`docs/`、`html/` 均未入库——二进制没有版本管理价值，官方发行包重新下载即可。

## 恢复一份可用的本地 nginx

1. 下载 Windows 版 nginx 1.20.2 并解压（与原先的 `C:\Users\lican\Desktop\nginx-1.20.2` 同级结构一致）。
2. 用本目录的 `conf/` 覆盖解压出来的 `conf/`。
3. 把本仓库的 `web-admin/` 整个复制为 `<nginx 目录>/html/admin`（`nginx.conf` 里用的是相对前缀路径 `root html/admin`，不需要改配置）。
4. 后端先起在 `8080`：`cd backend && mvn spring-boot:run`。
5. 校验并启动：

```powershell
cd <nginx 目录>
.\nginx.exe -t
Start-Process .\nginx.exe
```

浏览器打开 `http://127.0.0.1/`，账号 `admin / 123456`。

## conf/nginx.conf 的关键映射

| 路径 | 去向 |
| --- | --- |
| `/`、`/index.html` | 静态 `html/admin`（即 `web-admin/`），`try_files` 回落到 `index.html` 支持 hash 路由 |
| `/api/**` | 反代到 `http://sky_backend/admin/**`（后端原生路径没有 `/api` 前缀） |
| `/user/**` | 反代到 `http://sky_backend/user/**`，小程序顾客端接口 |
| `/health` | 后端健康检查，返回 `order-platform-ok` |
| `/doc.html`、`/v3/api-docs`、`/webjars/`、`/swagger-ui` | Knife4j / OpenAPI 文档 |
| `/ws/**` | 预留 WebSocket 反代 |

`upstream sky_backend` 指向 `127.0.0.1:8080`；后端换端口时只改这一处。

静态缓存在 `conf/nginx.conf` 里按类型区分：`js|css` 走 `no-cache`，避免改版后浏览器拿旧文件；图片字体缓存 7 天。

## 与服务器版配置的区别

线上用 `deploy/conf/nginx-orpl.conf`（绝对路径 `/opt/orpl/web/...`、监听 80、含 `/download/`、`/uploads/` 等完整映射）；本目录这份是 Windows 本机开发用，`root` 为相对路径，且保留了被注释掉的旧前端 `html/sky` 段落。
