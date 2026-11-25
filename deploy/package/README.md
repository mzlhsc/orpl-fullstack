# deploy/package

部署包里最终要上传到服务器的产物就放在这里。

| 文件 | 是否入库 | 说明 |
| --- | --- | --- |
| `orpl-admin.apk` | 是（8.1 MB） | 安卓管理端安装包，2026-11-25 11:20 构建，可直接安装 |
| `orpl-service.jar` | **否** | 后端可执行 jar 约 100.63 MB，超过 GitHub 单文件 100 MB 硬限制，因此不入库 |

## 补上后端 jar

```powershell
cd backend
mvn -DskipTests package
Copy-Item orpl-service\target\orpl-service.jar ..\deploy\package\
```

`deploy/scripts/deploy.ps1` 与 `install.sh` 默认从这个目录取 `orpl-service.jar` 上传到 `/opt/orpl/app/`，缺这个文件部署会中断，先把上面两步做完。

需要发给别人或体积受限时，也可以只传 jar 到服务器（`scp`）而不走整包上传。
