# Order Platform 管理端前端

管理端静态前端，直接由 `nginx-1.20.2` 托管，通过 `/api/**` 反向代理到后端 `/admin/**`。

## 访问方式

- 地址：`http://127.0.0.1/`
- 账号：`admin / 123456`（开发默认账号）
- 后端：`http://127.0.0.1:8080`（`orpl-service`）
- 接口文档：`http://127.0.0.1/doc.html`
- 健康检查：`http://127.0.0.1/health`

## 技术形态

- 原生 HTML + CSS + ES Modules，无 npm、无打包步骤，改完文件刷新即可生效。
- `js/app.js` 负责登录、hash 路由、侧边栏、店铺状态和账号操作。
- `js/api.js` 封装全部管理端接口，统一处理 `R<T>` 与裸对象两种返回结构。
- `js/charts.js` 为无依赖 Canvas 折线图/柱状图。
- `vendor/qrcode.min.js` 用于桌台二维码生成，已本地化，不依赖外网 CDN。

## 目录

```text
html/admin/
├── index.html            # 登录页 + 应用壳
├── css/app.css           # 设计系统与响应式样式
├── js/
│   ├── app.js            # 入口、路由、登录、店铺状态
│   ├── api.js            # 接口封装与 token 管理
│   ├── util.js           # 格式化/日期/分页/下载
│   ├── ui.js             # Toast/Modal/Drawer/分页/标签
│   ├── charts.js         # Canvas 图表
│   └── pages/            # 9 个业务页面模块
└── vendor/qrcode.min.js  # 本地二维码库
```

## 页面

| 路由 | 页面 | 主要能力 |
|---|---|---|
| `#/dashboard` | 工作台 | 今日经营指标、近 7 天营业额、门店状态、待接单快捷接单 |
| `#/orders` | 订单管理 | 状态筛选、接单/拒单/完成/取消、支付状态、接单设置、订单详情 |
| `#/dining` | 堂食管理 | 桌台、预约、就餐会话、堂食报表四个页签 |
| `#/dishes` | 菜品管理 | 菜品增改、图片、口味、库存、起售停售、批量删除 |
| `#/setmeals` | 套餐管理 | 套餐增改、套餐菜品与份数、起售停售 |
| `#/categories` | 分类管理 | 菜品/套餐分类、排序、启禁用、删除 |
| `#/employees` | 员工管理 | 员工增改、启禁用；密码仅支持当前账号修改 |
| `#/reports` | 数据统计 | 营业额、用户、订单、销量 Top10、导出报表 |
| `#/settings` | 店铺设置 | 营业状态、当前账号、修改密码、部署信息 |

## 订单状态与接单设置（2026-11-24）

- 订单管理页顶部新增“接单设置”卡片，提供**外卖自动接单**、**堂食自动接单**两个开关；开关读取/保存 `/api/shop/autoAccept`，设置保存在后端 Redis。
- 开关只影响新订单：外卖提交、堂食同桌加菜提交时，对应开关开启则订单直接进入 `3 已接单`，否则仍为 `2 待接单`。
- 订单状态删除 `4 派送中`；已接单外卖订单直接点“完成”，完成后支付状态自动置为已支付；堂食订单不显示“完成”，仍由整桌结账完成。
- 新增支付状态修改操作：
  - 未支付订单显示“标记已支付”，已完成未支付订单显示“补记已支付”；
  - 非完成、非取消订单的已支付订单可“标记未支付”；
  - 已完成订单只能保持已支付，后端会拒绝改回未支付。
- 订单统计卡只剩“待接单”“已接单/待处理”；状态筛选不再有派送中。

## 认证与接口约定

- 登录 `POST /api/employee/login`，返回裸 `EmployeeLoginVO`。
- token 存于 `localStorage`：`op_admin_token`；当前用户存于 `op_admin_user`。
- 所有请求自动带请求头 `zmlhsc: <token>`；401 时清除会话并回到登录页。
- `/api` 是 nginx 对外前缀，后端原生路径是 `/admin`；直连 8080 时不要带 `/api`。
- 返回结构兼容：
  - `employee/page`、`category/page`、`dish/page` 直接返回 `PageResult`；
  - `setmeal/page`、`order/conditionSearch` 返回 `R<PageResult>`；
  - 堂食、工作台、报表、店铺接口返回 `R<T>`；
  - `dish/stock/{dishId}` 直接返回数字；
  - 报表 `dateList/turnoverList` 等是逗号分隔字符串，前端已拆分。

## nginx 配置要点

`conf/nginx.conf` 中与本前端相关的配置：

```nginx
upstream sky_backend { server 127.0.0.1:8080; }

location / {
    root      html/admin;
    index     index.html;
    try_files $uri $uri/ /index.html;
}

location /api/ {
    proxy_pass http://sky_backend/admin/;
}

location = /health {
    proxy_pass http://sky_backend/health;
}
```

- `js/css` 使用 `Cache-Control: no-cache`，避免改版后浏览器继续用旧文件。
- 图片/字体缓存 7 天。
- 旧前端 `html/sky` 保持原样，未被覆盖；nginx 静态根已切换到 `html/admin`。

## 本地验收（2026-11-24）

- `nginx -t` 通过；`/`、`/css/app.css`、`/js/app.js`、`/vendor/qrcode.min.js` 均 200。
- 后端登录与 12 个核心接口经 nginx 联调通过。
- Playwright 真实浏览器验证 9 个页面路由、订单筛选/分页/单号搜索、订单详情抽屉、桌台二维码、菜品/套餐/员工/桌台/分类表单、堂食报表有数据/无数据两种状态。
- 临时分类“前端验收分类”完成新增 + 删除闭环，验收后已清理。

### 2026-11-24 订单页增量验收

- `node --check` 通过：`js/api.js`、`js/app.js`、`js/pages/orders.js`、`js/pages/dashboard.js`。
- `/v3/api-docs` 为 110 个 path operation；已无 `/admin/order/delivery/{id}`。
- 自动接单接口：默认 `false/false`；开启外卖后提交新外卖订单，返回 `status=3`、`payStatus=1`。
- 支付状态：已接单订单可从已支付改未支付；完成订单后自动回到已支付；已完成订单再改未支付返回 422。
- 接单设置卡片已按真实 `app.css` 做静态布局预览，两个开关、说明文案与状态标签显示正常。
- 管理端登录态页面截图未执行：浏览器管理员登录接管被跳过，未在登录状态下打开订单页。
