# ============================================================
#  Order Platform · 把「安卓 App + 微信小程序」指向正式服务器
#
#  用法（在本地 Windows 执行）：
#    .\configure-clients.ps1 -ServerUrl http://1.2.3.4
#    .\configure-clients.ps1 -ServerUrl https://demo.example.com
#
#  做的事：
#    1. 改 applet-WeChat/miniprogram/config.ts 的 API_BASE
#    2. 用 -PorplBaseUrl 重新打安卓 APK（默认地址写死成正式地址）
#    3. 把新 APK 覆盖到 deploy/package 与 deploy/web/download
#
#  之后重新执行 deploy.ps1 上的 web 目录即可让下载页生效
# ============================================================
param(
    [Parameter(Mandatory = $true)][string]$ServerUrl
)

$ErrorActionPreference = "Stop"

$root      = Resolve-Path (Join-Path $PSScriptRoot "..\..\..")   # Desktop（anz / applet-WeChat 与 Project 同级）
$anzDir    = Join-Path $root "anz"
$mpDir     = Join-Path $root "applet-WeChat"
$deployDir = Resolve-Path (Join-Path $PSScriptRoot "..")

$url = $ServerUrl.Trim().TrimEnd("/")
if ($url -notmatch "^https?://") { $url = "http://$url" }
Write-Host "服务器地址：$url" -ForegroundColor Cyan

# ---------- 1. 微信小程序 ----------
$cfg = Join-Path $mpDir "miniprogram\config.ts"
if (-not (Test-Path $cfg)) { throw "找不到 $cfg" }
$content = Get-Content -LiteralPath $cfg -Raw -Encoding UTF8
$newLine = "export const API_BASE = '$url'"
if ($content -match "export const API_BASE = '[^']*'") {
    $content = $content -replace "export const API_BASE = '[^']*'", $newLine
} else {
    throw "config.ts 里没有找到 API_BASE 定义"
}
Set-Content -LiteralPath $cfg -Value $content -Encoding UTF8 -NoNewline
Write-Host "小程序 API_BASE 已更新：$url" -ForegroundColor Green

# ---------- 2. 安卓 APK ----------
$apiBase = "$url/api/"   # 经 nginx 反代，接口前缀是 /api/（直连后端 8080 才是 /admin/）
$javaHome = "D:\Jobs\App\android-studio\jbr"
if (-not (Test-Path (Join-Path $javaHome "bin\java.exe"))) { $javaHome = "D:\Jobs\App\java" }
if (-not (Test-Path (Join-Path $javaHome "bin\java.exe"))) { throw "找不到 JDK，请设置 JAVA_HOME" }
$env:JAVA_HOME = $javaHome

Write-Host "重新打包 APK（DEFAULT_BASE_URL = $apiBase）..." -ForegroundColor Cyan
Push-Location $anzDir
try {
    & .\gradlew.bat assembleDebug --no-daemon "-PorplBaseUrl=$apiBase"
    if ($LASTEXITCODE -ne 0) { throw "Gradle 打包失败" }
} finally {
    Pop-Location
}

$apk = Join-Path $anzDir "app\build\outputs\apk\debug\app-debug.apk"
if (-not (Test-Path $apk)) { throw "没有找到构建产物 $apk" }

foreach ($dst in @(
        (Join-Path $deployDir "package\orpl-admin.apk"),
        (Join-Path $deployDir "web\download\orpl-admin.apk"))) {
    $dir = Split-Path $dst -Parent
    New-Item -ItemType Directory -Force -Path $dir | Out-Null
    Copy-Item -LiteralPath $apk -Destination $dst -Force
}
Write-Host "APK 已更新（约 $([math]::Round((Get-Item $apk).Length / 1MB, 1)) MB）" -ForegroundColor Green

Write-Host ""
Write-Host "下一步：" -ForegroundColor Yellow
Write-Host "  1) .\deploy.ps1 -Server <IP> -User root        重新上传并部署"
Write-Host "  2) 微信开发者工具导入 applet-WeChat，上传体验版"

