# Java Socket 聊天程序 — 一键编译并打包为 Windows 可执行程序
# 用法：在项目根目录执行  powershell -ExecutionPolicy Bypass -File build.ps1
# 可选参数：-Installer  生成 .exe 安装包（需 WiX 或 Inno Setup）；默认生成免安装目录

param(
    [switch]$Installer
)

$ErrorActionPreference = "Stop"
$Root = $PSScriptRoot
Set-Location $Root

Write-Host "=== [1/4] 编译 Java 源码 ===" -ForegroundColor Cyan
if (Test-Path "out") { Remove-Item -Recurse -Force "out" }
New-Item -ItemType Directory -Path "out", "dist" -Force | Out-Null

javac -encoding UTF-8 -d out `
    src/common/*.java `
    src/util/*.java `
    src/server/*.java `
    src/client/*.java `
    src/client/ui/*.java

if ($LASTEXITCODE -ne 0) { throw "编译失败" }
Write-Host "编译成功" -ForegroundColor Green

Write-Host "=== [2/4] 生成 JAR ===" -ForegroundColor Cyan
jar cfe dist/chat-server.jar server.ChatServer -C out .
jar cfe dist/chat-client.jar client.ChatClient -C out .
Write-Host "已生成 dist/chat-server.jar 与 dist/chat-client.jar" -ForegroundColor Green

Write-Host "=== [3/4] 使用 jpackage 打包 ===" -ForegroundColor Cyan
if (Test-Path "release") { Remove-Item -Recurse -Force "release" }

$packageType = if ($Installer) { "exe" } else { "app-image" }

# 服务端：保留控制台窗口
jpackage --input dist `
    --name ChatServer `
    --main-jar chat-server.jar `
    --main-class server.ChatServer `
    --type $packageType `
    --dest release `
    --java-options "-Dfile.encoding=UTF-8" `
    --win-console

if ($LASTEXITCODE -ne 0) { throw "服务端打包失败" }

# 客户端：图形界面，无控制台
jpackage --input dist `
    --name ChatClient `
    --main-jar chat-client.jar `
    --main-class client.ChatClient `
    --type $packageType `
    --dest release `
    --java-options "-Dfile.encoding=UTF-8"

if ($LASTEXITCODE -ne 0) { throw "客户端打包失败" }

Write-Host "=== [4/4] 生成分发压缩包 ===" -ForegroundColor Cyan
if (-not $Installer) {
    if (Test-Path "release\ChatServer-portable.zip") { Remove-Item -Force "release\ChatServer-portable.zip" }
    if (Test-Path "release\ChatClient-portable.zip") { Remove-Item -Force "release\ChatClient-portable.zip" }
    Compress-Archive -Path "release\ChatServer" -DestinationPath "release\ChatServer-portable.zip"
    Compress-Archive -Path "release\ChatClient" -DestinationPath "release\ChatClient-portable.zip"
}

Write-Host ""
Write-Host "=== 打包完成 ===" -ForegroundColor Green
Write-Host "输出目录：$Root\release" -ForegroundColor Yellow
if ($Installer) {
    Write-Host "  - release\ChatServer-*.exe  （服务端安装包）"
    Write-Host "  - release\ChatClient-*.exe  （客户端安装包）"
} else {
    Write-Host "  - release\ChatServer\ChatServer.exe"
    Write-Host "  - release\ChatClient\ChatClient.exe"
    Write-Host "  - release\ChatServer-portable.zip  （发给主机同学）"
    Write-Host "  - release\ChatClient-portable.zip  （发给其他参与者）"
    Write-Host ""
    Write-Host "对方解压 zip 后双击 exe 即可运行，无需安装 Java。" -ForegroundColor Green
}
