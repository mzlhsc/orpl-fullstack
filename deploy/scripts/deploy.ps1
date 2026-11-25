# ============================================================
#  Order Platform · 一键上传 + 部署（在本地 Windows 上执行）
#
#  用法：
#    1) 密钥登录（推荐）
#       .\deploy.ps1 -Server 1.2.3.4 -User root -KeyFile C:\Users\you\.ssh\id_rsa
#    2) 密码登录（会提示输入密码，逐个文件传输时需重复输入）
#       .\deploy.ps1 -Server 1.2.3.4 -User root
#    3) 只上传、不执行安装
#       .\deploy.ps1 -Server 1.2.3.4 -User root -SkipInstall
#
#  依赖：Windows 自带 OpenSSH（ssh / scp）
# ============================================================
param(
    [Parameter(Mandatory = $true)][string]$Server,
    [string]$User = "root",
    [int]$Port = 22,
    [string]$KeyFile = "",
    [string]$RemoteRoot = "/opt/orpl",
    [switch]$SkipInstall
)

$ErrorActionPreference = "Stop"
$PackageDir = Resolve-Path (Join-Path $PSScriptRoot "..")

$sshArgs = @("-p", "$Port", "-o", "StrictHostKeyChecking=accept-new", "-o", "ServerAliveInterval=30")
if ($KeyFile) { $sshArgs += @("-i", $KeyFile) }
$scpArgs = @($sshArgs)

function Invoke-Ssh([string]$Command) {
    & ssh @sshArgs "$User@$Server" $Command
    if ($LASTEXITCODE -ne 0) { throw "ssh 执行失败：$Command" }
}

Write-Host "[1/4] 检查服务器连通性 ..." -ForegroundColor Cyan
if (-not (Test-NetConnection -ComputerName $Server -Port $Port -WarningAction SilentlyContinue).TcpTestSucceeded) {
    throw "无法连接 $Server`:$Port，请检查 IP、端口、安全组和服务器是否开机"
}

Write-Host "[2/4] 创建远端目录 $RemoteRoot ..." -ForegroundColor Cyan
Invoke-Ssh "mkdir -p $RemoteRoot"

Write-Host "[3/4] 上传部署包（约 110MB，首次较慢）..." -ForegroundColor Cyan
foreach ($item in @("package", "sql", "conf", "web", "scripts")) {
    $local = Join-Path $PackageDir $item
    if (-not (Test-Path $local)) { Write-Host "  跳过（不存在）：$item" -ForegroundColor Yellow; continue }
    Write-Host "  -> $item"
    & scp @scpArgs -r $local "$User@${Server}:$RemoteRoot/"
    if ($LASTEXITCODE -ne 0) { throw "上传 $item 失败" }
}

Write-Host "[4/4] 执行服务器初始化 ..." -ForegroundColor Cyan
if ($SkipInstall) {
    Write-Host "已跳过安装。到服务器上执行：sudo bash $RemoteRoot/scripts/install.sh" -ForegroundColor Yellow
} else {
    Invoke-Ssh "chmod +x $RemoteRoot/scripts/install.sh && bash $RemoteRoot/scripts/install.sh"
}

Write-Host ""
Write-Host "完成。若安装脚本最后打印了访问地址，直接用浏览器打开即可。" -ForegroundColor Green
