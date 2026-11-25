# Order Platform 微信小程序顾客端

基于 `order-platform` 后端 `/user/**`、`/shopping-cart/**`、`/addressBook/**` 接口实现的微信原生 TypeScript 小程序。视觉延续管理端 `#e8552d / #1f2937 / #f7f4ef` 品牌色与圆角卡片风格。

## 打开与运行

1. 微信开发者工具导入 `C:\Users\lican\Desktop\applet-WeChat`，AppID 使用 `wxc69c75f8ef2056d2`。
2. 启动后端，默认地址 `http://127.0.0.1:8080`。
3. 开发者工具需关闭“校验合法域名、web-view、TLS 版本以及 HTTPS 证书”；项目已在 `project.config.json` 与 `project.private.config.json` 中设置 `urlCheck: false`。
4. 点击“编译”即可。开发阶段使用本地模拟器，不需要真机。

## 登录与接口

- 开发登录开关：`miniprogram/config.ts` 的 `USE_MOCK_LOGIN = true`。
- 固定开发 code：`ordpf-mp-local-user`，后端未配置微信 appid/secret 时会直接把 code 当 openid 使用。
- 登录接口：`POST /user/user/login`，body `{ code }`。
- 后续业务请求头：`authorization: <token>`。
- 生产环境需把 `API_BASE` 改为已备案 HTTPS 域名，并把 `USE_MOCK_LOGIN` 改为 `false`，同时在后端配置微信 appid/secret。

## 页面

订单状态为 `1待付款 → 2待接单 → 3已接单 → 5已完成`，取消为 `6`；已不再有“派送中”状态，管理端开启自动接单时新订单会直接进入 `3 已接单`。

| 页面 | 路由 |
|---|---|
| 点餐（tab） | `pages/menu/menu` |
| 订单（tab） | `pages/orders/orders` |
| 堂食（tab） | `pages/dining/dining` |
| 我的（tab） | `pages/profile/profile` |
| 购物车 | `pages/cart/cart` |
| 确认订单 | `pages/order-confirm/order-confirm` |
| 订单详情 | `pages/order-detail/order-detail` |
| 堂食会话 | `pages/dining-session/dining-session` |
| 预约桌台 | `pages/reservation/reservation` |
| 地址列表 / 编辑 | `pages/address/list`、`pages/address/edit` |

> 堂食页只提供“扫码识别桌台”入口：不展示桌台一览，不支持手动输入桌台码或点选桌台。空闲桌扫码后开台，已有会话的桌扫码后加入本桌。
>
> 管理端整桌结账或关闭会话后，会话关闭、桌台释放；小程序会话页会检测到已结账/已关闭，自动提示并退回堂食首页，不能继续加菜或结账。

## 校验

```powershell
cd C:\Users\lican\Desktop\applet-WeChat
npx -y -p typescript@5.6.3 tsc --noEmit -p tsconfig.json
```

微信开发者工具 `cli preview` 已通过，构建包约 495 KB。

## 发布前替换

- `config.ts` 中的 `API_BASE`、`SHOP_PHONE`。
- 正式微信登录：`USE_MOCK_LOGIN = false`，并配置后端微信参数。
- 演示图床图片 404 时，小程序会回退到 `assets/food` 本地图；上线前可替换为真实门店图。

## 部署到服务器

后端部署完成后，在 `C:\Users\lican\Desktop\Project\deploy\scripts` 执行：

```powershell
.\configure-clients.ps1 -ServerUrl http://<服务器IP>
```

会自动把 `miniprogram/config.ts` 的 `API_BASE` 改成该地址（正式发布用 `https://<已备案域名>`），并同步重打包安卓 APK。

小程序侧还要注意：

- 开发者工具/真机调试：勾选「不校验合法域名、web-view、TLS 版本以及 HTTPS 证书」，可直连 `http://IP`。
- 正式发布：域名必须已 ICP 备案并启用 HTTPS，并在微信公众平台把域名加入 **request 合法域名**；微信不接受 IP 作为服务器域名。
- 生产环境走 `USE_MOCK_LOGIN = false` 时，需要后端 `/opt/orpl/conf/orpl.env` 配好 `WECHAT_APPID` / `WECHAT_SECRET`。

### 当前演示配置（2026-11-25）

- `miniprogram/config.ts` 的 `API_BASE` 已指向 `http://121.40.186.5`（阿里云 ECS 上的点单系统后端）
- 用微信开发者工具「导入项目」选择 `C:\Users\lican\Desktop\applet-WeChat`，AppID `wxc69c75f8ef2056d2`，勾选「不校验合法域名」即可直接跑通登录、菜品、下单流程
- 手机真机演示用「真机调试」，同样不校验域名；正式发布仍需备案域名 + HTTPS

### 上传记录（2026-11-25）

- 开发版 1.0.0 已通过 CLI 上传（519 KB，AppID wxc69c75f8ef2056d2），在公众平台「版本管理」中选为体验版即可
- 体验版真机需在右上角菜单「打开调试」，否则会校验服务器域名而无法访问 `http://121.40.186.5`
