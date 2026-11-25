# 瑞吉外卖 · 管理端 Android App

order-platform（orpl）管理端的原生安卓客户端。视觉体系沿用 `nginx/html/admin` 的样式变量，接口与 `nginx/html/admin/js/api.js` 完全对齐（含 `admin/` 前缀、`zmlhsc` 令牌头）。

## 本次更新（对齐后端 2026-11-24 变更）

- 订单状态去掉「4 派送中」，`3 已接单` 可直接「完成」；列表/详情移除派送按钮，筛选改为 待付款/待接单/已接单/已完成/已取消。
- 新增「标记已支付 / 标记未支付 / 补记已支付」动作（`PUT /admin/order/payStatus`），订单卡片与详情页都会展示支付状态标签。
- 「我的 → 门店设置」新增外卖、堂食两个自动接单开关（`GET/PUT /admin/shop/autoAccept`）。
- 新增底部「堂食」Tab：桌台看板（开台/查看会话/关闭会话/编辑/删除/二维码）、就餐会话（筛选、金额、结账、关闭、转台、详情）、预约（确认/到店/取消）、堂食经营报表（营业额/会话数/客人数/人均/周转率 + 每日柱状图）。
- 工作台「全部订单」入口改为显示「待接单 x · 已接单 y」。

## 一、运行

**方式 A · Android Studio**：打开本目录（`C:/Users/lican/Desktop/anz`），等待 Sync 完成（`local.properties` 已指向本机 SDK `D:/Jobs/AppFile/android-studio`），然后 Run。

**方式 B · 命令行**：

```bash
export JAVA_HOME="D:/Jobs/App/android-studio/jbr"
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 正式环境：把默认服务器地址直接写进 APK（下载安装后不用手填「高级设置」）
./gradlew assembleDebug -PorplBaseUrl=http://<服务器IP>/admin/
```

> 工具链：**AGP 9.5.2 + Gradle 9.9.2 + JDK 25（Studio 自带 JBR）+ compileSdk 37 / targetSdk 34 / minSdk 24**。
> AGP 9 起 Kotlin 支持内置，`build.gradle.kts` 里**不要再加** `org.jetbrains.kotlin.android` 插件（加了会直接报错）。
> `distributionUrl` 指向腾讯云镜像（国内更快）；如需官方源改回 `https://services.gradle.org/distributions/gradle-9.9.2-bin.zip`。

1. 启动后端 `order-platform`（默认 `8080`），确认 `GET http://localhost:8080/health` 正常。
2. 运行 App，在登录页点「高级设置 · 服务器地址」填写接口地址：
   - 模拟器直连后端：`http://10.0.2.2:8080/admin/`（默认值，无需修改）
   - 模拟器走 nginx：`http://10.0.2.2/api/`
   - 真机（与电脑同一局域网）：`http://192.168.x.x:8080/admin/`
   - 只填 `192.168.1.8:8080` 也会自动补成 `.../admin/`
3. 用员工账号登录（与 Web 管理端同一套账号，密码走 MD5 校验）。

> 接口地址会被持久化，下次登录无需重复填写；也可以在「我的 → 服务器地址」里修改。

## 二、功能范围

| 模块 | 已实现 |
| --- | --- |
| 登录 | 员工登录、登录态持久化、401 自动回登录页 |
| 工作台 | 今日营业额/有效订单/完成率/客单价/新增用户、近 7 天营业额柱状图、待接单快捷接单、待接单↔已接单数量、商品总览、店铺营业状态切换 |
| 订单 | 状态筛选（待付款/待接单/已接单/已完成/已取消）、订单号/手机号搜索、分页上拉加载、接单、拒单（填原因）、完成、取消（填原因）、标记已支付/未支付、订单详情 |
| 堂食 | 桌台看板（按状态筛选、开台、查看会话、关闭会话、编辑/删除桌台、二维码内容复制与刷新）、就餐会话（状态筛选、金额汇总、结账、关闭会话、转台、并桌、会话详情含成员/共享购物车/关联订单）、预约（确认、到店、取消）、堂食经营报表（营业额、会话数、客人数、人均、桌台周转率、每日柱状图） |
| 商品 | 菜品：新增/编辑（图片上传·相册选图、分类、价格、库存、描述、口味做法、起售状态）、删除、起售/停售、库存设置、分类筛选与搜索；套餐：新增/编辑（选菜品+份数增减）、删除、起售/停售 |
| 分类 | 菜品/套餐分类切换、新增、编辑、删除、启用/禁用、名称搜索 |
| 员工 | 员工列表、姓名搜索、新增、编辑（姓名/账号/手机号/性别/身份证）、账号启用/禁用 |
| 数据统计 | 今日/近 7 天/近 30 天，营业额合计、订单总数、有效订单、完成率、新增用户、营业额趋势、订单趋势（总数/有效）、用户增长（总用户/每日新增）、销量 TOP10 |
| 我的 | 账号信息、门店营业状态、外卖/堂食自动接单开关、服务器地址、修改密码、退出登录 |

仍有 3 处与 Web 端存在差异（均为桌面端更顺手的操作）：

1. **批量删除**：菜品/套餐在 Web 是勾选后批量删除，App 目前是逐条「删除」（单条删除走同一接口 `DELETE /admin/dish?ids=`、`/admin/setmeal?ids=`）。
2. **报表导出 Excel**：`GET /admin/report/export` 下载文件，手机端不适合落 Excel，需要的话可以改成导出到公共目录或分享。
3. **菜品图片上传**依赖后端 OSS（`POST /admin/common/upload`），未配置 OSS 时只能用图片 URL；App 与 Web 行为一致。

> 订单状态已在 2026-11-24 调整：`1 待付款 / 2 待接单 / 3 已接单 / 5 已完成 / 6 已取消`，**「4 派送中」已删除**，App 里不再有派送动作，已接单可直接「完成」（完成后支付状态自动置为已支付）。

## 三、技术栈与结构

- Kotlin + AndroidX，ViewBinding + XML 布局（不使用 Compose，便于和现有静态前端对照维护）
- OkHttp + Gson + Coroutines，无 Retrofit（后端响应形态不统一，手工解包更稳）
- Material Components 1.12（Material 3 主题、MaterialSwitch、BottomNavigationView）
- Glide 加载菜品/套餐图片
- 自绘 `BarChartView`（不引图表库，首屏更快）

```
app/src/main/java/com/mzlhsc/orpl/admin/
├── AdminApp.kt                  Application，初始化本地存储
├── data/
│   ├── ApiClient.kt             HTTP 客户端：令牌头、R<T>/裸对象兼容、401 处理
│   ├── AdminApi.kt              所有管理端接口封装（路径与 web 端 api.js 一一对应）
│   ├── ApiException.kt          业务异常（code=401 表示登录失效）
│   ├── Prefs.kt                 登录态与服务器地址
│   └── model/Models.kt          后端 DTO/VO 的 Kotlin 映射
├── util/                        Fmt 格式化、Tags 状态标签、Ext 协程与视图扩展
├── widget/                      StatCardView 统计卡、BarChartView 柱状图
└── ui/
    ├── login/                   登录
    ├── main/                    底部导航容器 + 未接单角标
    ├── dashboard/               工作台
    ├── order/                   订单列表、适配器、订单详情
    ├── dining/                  堂食：桌台 / 会话 / 预约 / 报表 + 会话详情 + 弹窗
    ├── goods/                   商品（ViewPager2：菜品/套餐）
    ├── category/                分类管理
    ├── employee/                员工管理
    ├── report/                  数据统计
    ├── more/                    我的/设置
    └── common/                  Chips 筛选胶囊、Dialogs 原因/库存/确认弹窗
```

## 四、与后端对接的关键约定

1. **令牌头是 `zmlhsc`**，不是 `Authorization`（见 `TokenAuthInterceptor`）。
2. **响应形态两种**，`ApiClient` 统一处理：
   - `R<T>`：`{code,msg,data}`，`code != 200` 抛业务异常，401 落地清登录态；
   - 裸对象：登录 `EmployeeLoginVO`、员工/分类/菜品分页是直接返回数据，没有 `code` 包裹。
3. **分页字段是 `total / pages / list`**（`PageDTO`），不是 MyBatis-Plus 的 `records/current/size`；接口文档里早期的 `records` 描述已过时。
4. **写接口的位置参数差异**：分类删除是 `DELETE /admin/category?id=`，菜品批量删除是 `DELETE /admin/dish?ids=`，套餐删除是 `DELETE /admin/setmeal?ids=`。
5. 员工分页只支持 `name`，不支持 `status` 过滤（`EmployeePageQueryDTO` 没有该字段），所以员工页只做了姓名搜索 + 列表内启禁用。
6. 时间字段是 `LocalDateTime` 序列化的 `2026-11-22T18:00:00`，App 内按字符串解析为 `MM-dd HH:mm` 展示，避免 `java.time` 在 minSdk 24 上需要 desugaring。
7. **订单状态枚举（2026-11-24 起）**：`1 待付款 / 2 待接单 / 3 已接单 / 5 已完成 / 6 已取消`；`4 派送中`与 `PUT /admin/order/delivery/{id}` 已删除，历史 `status=4` 由 `mysql/03-migrate-order-status.sql` 迁移为 `3`。
8. **支付状态**：`PUT /admin/order/payStatus`，body `{id, payStatus}`，只接受 `0 未支付 / 1 已支付`；已完成订单不能改回未支付，已取消/已退款不能改支付状态。`PUT /admin/order/complete/{id}` 只允许 `3 已接单` 的外卖订单，完成后自动写 `payStatus=1`。
9. **自动接单**：`GET /admin/shop/autoAccept` → `{takeoutAutoAccept, diningAutoAccept}`；`PUT` 同一个地址，可只传一个字段，未传字段保持原值。开启后新订单直接落 `3 已接单`，只影响之后的新订单。
10. **堂食**：桌台状态 `0 空闲 / 1 就餐中 / 2 待结账`；会话状态 `1 就餐中 / 2 待结账 / 3 已结账 / 4 已关闭`；预约状态 `1 待确认 / 2 已确认 / 3 已到店 / 4 已取消 / 5 已过期`。
    - 关闭会话是 `POST /admin/dining/session/{id}/close?reason=`（旧的 `/cancel` 等价保留）：取消未完成订单、清空共享购物车、释放桌台，已支付/已完成订单保留，不写支付记录。
    - 结账是 `POST /admin/dining/session/checkout`，body `{sessionId, payMethod(1微信 2支付宝 3现金), settleUnpaid, remark}`。
    - 转台 `POST /admin/dining/session/{id}/transfer` 的 `targetTableId` 只能是空闲桌台，App 会先拉 `table/list?status=0` 再让选择。
    - `dining/report` 的时间参数是 `yyyy-MM-dd HH:mm:ss`，`daily` 里是 snake_case 字段（`day / revenue / session_count / guests`），模型用 `@SerializedName` 对齐。

## 五、设计规范（取自 `admin/css/app.css`）

| 变量 | 值 | 用途 |
| --- | --- | --- |
| brand | `#E8552D` | 主色、按钮、选中态、柱状图 |
| brand-strong | `#C9431F` | 强调文字、渐变深端 |
| brand-soft / line | `#FFF0EB` / `#FFD9CD` | 标签、浅色底 |
| ink / ink-2 / muted | `#1F2937` / `#374151` / `#6B7280` | 正文层级 |
| bg / card / line | `#F4F5F7` / `#FFFFFF` / `#E5E7EB` | 页面底、卡片、描边 |
| sidebar | `#18222D` | 深色渐变（营业额 hero、订单详情头部） |
| success / warn / danger / info | `#16803C` / `#B45309` / `#C62828` / `#0E7490` | 状态标签 |

统一 14dp 圆角卡片 + 1dp 描边、无阴影；标签胶囊 12dp；按钮 10dp 圆角、44dp 高。明细见 `res/values/themes.xml`。

## 六、可能遇到的问题

- **登录提示网络异常**：确认后端已启动、手机/模拟器能访问该地址；真机不要填 `localhost`。
- **HTTP 明文被拦**：Manifest 已开 `usesCleartextTraffic="true"`；若公司网络强制 HTTPS，请改走 nginx + 证书。
- **接口返回 401**：令牌 12 小时过期，App 会自动清理登录态并跳回登录页。
- **图片不显示**：菜品图片是 OSS 地址，需要设备能访问外网；离线时保留占位底色。

## 七、构建与联调验证记录

- `./gradlew assembleDebug` 通过（Gradle 9.9.2 / AGP 9.5.2 / JDK 25 / compileSdk 37），无编译告警。
- 产物：`dist/orpl-admin-debug.apk`（约 8 MB，37 个 Kotlin 文件 / 106 个 XML），可直接 `adb install -r` 装到真机/模拟器。
- 启动 Activity 已确认：`com.mzlhsc.orpl.admin.ui.login.LoginActivity`。
- **已在模拟器 `Medium_Phone_API_37.0`（1080×2400）实机跑通**：安装 → 登录（admin/123456）→ 工作台、订单、堂食（桌台/会话/预约/报表）、我的 全部从真实后端取数渲染，无崩溃、无布局错位。

用真实后端（`admin/123456` 登录成功）逐条校验过接口，实际返回与 App 中的假设一致：

| 接口 | 实际返回 | App 处理 |
| --- | --- | --- |
| `POST /admin/employee/login` | 裸对象 `{id,name,token,userName}` | `LoginVO`，存 token 到本地 |
| `GET /admin/employee/page` | 裸 `{total,pages,list}` | `PageResult<Employee>` |
| `GET /admin/category/page` / `list` | 裸 `{total,pages,list}` / 裸数组 | 同上 |
| `GET /admin/dish/page` | 裸 `{total,pages,list}`，含 `stock`、`categoryName` | `PageResult<Dish>` |
| `GET /admin/setmeal/page` | `R<PageDTO>` | 解包后 `PageResult<Setmeal>` |
| `GET /admin/order/conditionSearch` | `R<PageDTO>`，含 `orderDishes`、`payStatus`、`orderType` | 列表直接展示菜品串 + 支付标签 |
| `GET /admin/order/statistics` | `{toBeConfirmed,confirmed}`（`deliveryInProgress` 已删除） | 底部导航角标 |
| `PUT /admin/order/payStatus` | `R<Void>`，body `{id,payStatus}` | 列表/详情「标记已支付/未支付」 |
| `GET /admin/workspace/*` | 营业额/订单/菜品/套餐概览 | 工作台 |
| `GET /admin/shop/status` | `R<Integer>` | 营业状态开关 |
| `GET`/`PUT /admin/shop/autoAccept` | `R<{takeoutAutoAccept,diningAutoAccept}>` | 我的页两个自动接单开关 |
| `GET /admin/dish/stock/{id}` | 裸整数 `97` | 库存弹窗默认值 |
| `GET /admin/report/*` | 逗号分隔字符串 | 拆分为图表与 TOP10 |
| `GET /admin/dining/table/list` | `R<[{id,name,seats,area,status,qrContent,currentSessionId,…}]>` | 桌台看板 |
| `GET /admin/dining/session/list` / `{id}` | `R<[{sessionNo,status,totalAmount,paidAmount,unpaidAmount,members,cartItems,orders,…}]>` | 会话列表与详情 |
| `GET /admin/dining/report` | `R<{sessionCount,totalRevenue,totalGuests,avgAmount,turnoverRate,tableCount,daily:[…]}>` | 堂食报表 + 每日柱状图 |
| 无令牌访问 | HTTP 401 + `{code:401,msg:"未登录"}` | 清登录态并回登录页 |

> 说明：106 个 XML 全部合法，171 个 view id 与 ViewBinding 字段、drawable/style/string 引用全部对得上；
> 模拟器实拍截图见 `preview/` 目录下的 `shot-*.png`，静态设计稿见 `preview/design-preview.html`。真机首次运行如有异常，优先看 Logcat 里的 `ApiException` 信息。

### 本机环境备注（2026-11-24）

- Android Studio `D:/Jobs/App/android-studio`，SDK 在 `D:/Jobs/AppFile/android-studio`（API 37.0 + build-tools 36.0.0，本次补装了 platform 34 与 build-tools 34.0.0）。
- Studio 首次启动时因异常结束留下了一个无法释放的 AF_UNIX socket 锁（`…/Google/AndroidStudio2026.2.2/.port`），表现为启动即退出并报 `BindException: Address already in use`。
  解决办法：在 `%APPDATA%/Google/AndroidStudio2026.2.2/studio64.exe.vmoptions` 里加了
  `-Didea.system.path=C:/Users/lican/AppData/Local/Google/AndroidStudio2026.2.2-data`，让缓存走新目录。
  重启之后旧的 `…/AndroidStudio2026.2.2` 目录可以删掉，把这行删掉即可恢复默认缓存位置。

## 八、2026-11-24 晚 · 与 Web 管理端对齐的功能补齐

按「Web 端有的 App 也要有」逐页核对后补齐，并已在模拟器上逐项点验：

| 补齐项 | 说明 |
| --- | --- |
| 菜品新增/编辑 | 新增 `DishEditActivity`：图片（相册选图 → `POST /admin/common/upload` 或直接填 URL）、名称、分类、价格、库存、描述、**口味做法（可增删多行）**、起售状态；编辑时回填 `GET /admin/dish/{id}` 的口味 |
| 菜品删除 | 卡片「删除」→ 确认框；后端对起售中菜品会拒绝（提示「起售中的菜品不能删除」），需先停售 |
| 套餐新增/编辑 | 新增 `SetmealEditActivity`：图片、名称、套餐分类、价格、描述、起售状态、**菜品明细（多选菜品 + 份数 −/+ 调整 + 移除）** |
| 套餐删除 | 卡片「删除」→ 确认框 |
| 员工新增/编辑 | 工具栏「＋ 新增」/ 点员工卡片 → 姓名、账号、手机号、性别、身份证；新增默认密码 123456，编辑不改密码与状态 |
| 报表补图 | 新增「订单趋势」（每日有效订单 + 总数/有效对比说明）、「用户增长」（每日新增 + 总用户量）两张卡片，对应 Web 的 orders / users 图表 |
| 堂食并桌 | 会话详情底部新增「并桌」：把同桌其他「就餐中/待结账」会话并入当前会话（`POST /admin/dining/session/{targetId}/merge`） |
| 关闭会话免原因 | 会话列表、桌台看板、会话详情三处都不再弹原因输入，改为一次确认；`reason` 不传，后端自动记「管理员关闭堂食会话」 |

同时把列表页动作补齐：菜品条目底部为「编辑 / 库存 / 删除」，套餐为「编辑 / 删除」，商品页顶部新增「＋ 新增」按钮。

> 单条删除与 Web 的批量删除调的是同一个后端接口（`ids` 支持逗号分隔），如需多选批量删除可以再加长按多选。

### 2026-11-25 补丁

- 默认后端地址改为按「服务器是否经 nginx」区分：`http://<IP>/api/`（nginx，推荐）或 `http://<IP>:8080/admin/`（直连）
- `Prefs.normalize()` 现在只填主机时会按端口自动补前缀：8080 → `/admin/`，其余 → `/api/`
- 已重新打包并替换线上 APK；覆盖安装后如仍报 405，请卸载重装或手动把地址改成 `/api/`
