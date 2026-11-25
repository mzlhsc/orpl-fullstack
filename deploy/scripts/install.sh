#!/usr/bin/env bash
# ============================================================
#  Order Platform · 服务器初始化脚本（Ubuntu 22.04 / 24.04 / Debian 12）
#
#  前提：已经执行 deploy.ps1 把 deploy/ 目录上传到 /opt/orpl
#  用法：sudo bash /opt/orpl/scripts/install.sh
#
#  做的事情：
#    1. 安装 nginx / MySQL 8 / Redis / 基础工具
#    2. 安装 Temurin JDK 25 到 /usr/lib/jvm/temurin-25-jdk-amd64
#    3. 创建 orpl 运行用户与目录 /opt/orpl
#    4. 创建数据库 orpl 与专用账号，导入建表 + 演示数据
#    5. 给 Redis 设置访问密码
#    6. 配置 nginx 站点（门户 / 管理端 / 接口反代 / APK 下载）
#    7. 注册 systemd 服务 orpl.service 并开机自启
#
#  可重复执行；没有 systemd 的容器环境会自动降级为后台进程启动
# ============================================================
set -euo pipefail

APP_ROOT=${APP_ROOT:-/opt/orpl}
JDK_DIR=/usr/lib/jvm/temurin-25-jdk-amd64
JDK_URL="https://api.adoptium.net/v3/binary/latest/25/ga/linux/x64/jdk/hotspot/normal/eclipse"
JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseSerialGC -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai"

# 内存自适应：1GiB 轻量服务器需要加 swap，并压低 MySQL / JVM 占用
MEM_MB=$(awk '/MemTotal/{print int($2/1024)}' /proc/meminfo 2>/dev/null || echo 2048)
LOW_MEM=0
[ "${MEM_MB:-2048}" -lt 1600 ] && LOW_MEM=1

DB_NAME=orpl
DB_USER=orpl
DB_PASS=${DB_PASS:-Orpl@2026}
REDIS_PASS=${REDIS_PASS:-Orpl@2026}
HTTP_PORT=${HTTP_PORT:-80}

GREEN='\033[32m'; RED='\033[31m'; YELLOW='\033[33m'; NC='\033[0m'
log(){  printf "${GREEN}[deploy]${NC} %s\n" "$*"; }
warn(){ printf "${YELLOW}[warn]${NC}  %s\n" "$*"; }
die(){  printf "${RED}[error]${NC} %s\n" "$*" >&2; exit 1; }

HAS_SYSTEMD=0
[ "$(ps -p 1 -o comm= 2>/dev/null || true)" = "systemd" ] && HAS_SYSTEMD=1

svc_start(){
  if [ "$HAS_SYSTEMD" -eq 1 ]; then
    systemctl enable --now "$1" >/dev/null 2>&1 || systemctl restart "$1" >/dev/null 2>&1 || true
  else
    service "$1" start >/dev/null 2>&1 || true
  fi
}
svc_restart(){
  if [ "$HAS_SYSTEMD" -eq 1 ]; then systemctl restart "$1" >/dev/null 2>&1 || true
  else service "$1" restart >/dev/null 2>&1 || true; fi
}

# ---------------- 0. 前置检查 ----------------
[ "$(id -u)" -eq 0 ] || die "请用 root 运行：sudo bash $0"
[ -f /etc/os-release ] || die "无法识别系统版本"
. /etc/os-release
case "${ID:-}" in
  ubuntu|debian) ;;
  *) die "本脚本按 Ubuntu/Debian 编写，当前系统是 ${PRETTY_NAME:-未知}。若是 CentOS/Alibaba Cloud Linux，请参考 README 手工安装 MySQL 8 / Redis / nginx / JDK 25 后再执行第 3~7 步。" ;;
esac

[ -f "$APP_ROOT/package/orpl-service.jar" ] || die "缺少 $APP_ROOT/package/orpl-service.jar，请先用 deploy.ps1 上传部署包"
[ -d "$APP_ROOT/web/admin" ]               || die "缺少 $APP_ROOT/web/admin，请先上传部署包"
[ -d "$APP_ROOT/sql" ]                     || die "缺少 $APP_ROOT/sql，请先上传部署包"

# ---------------- 1. 系统依赖 ----------------
log "安装系统依赖（nginx / MySQL 8 / Redis / curl / unzip）"
export DEBIAN_FRONTEND=noninteractive
apt-get update -qq
apt-get install -y -qq nginx mysql-server redis-server curl unzip ca-certificates >/dev/null
log "系统依赖安装完成"

# ---------------- 2. JDK 25 ----------------
if [ -x "$JDK_DIR/bin/java" ]; then
  log "JDK 已存在：$("$JDK_DIR/bin/java" -version 2>&1 | head -1)"
else
  log "下载 Temurin JDK 25（约 140MB，首次较慢）"
  mkdir -p "$JDK_DIR"
  tmp_jdk=/tmp/jdk25-$$.tar.gz
  curl -fL --retry 3 --connect-timeout 20 -o "$tmp_jdk" "$JDK_URL" \
    || die "JDK 下载失败，可手动下载后解压到 $JDK_DIR"
  tar -xzf "$tmp_jdk" -C "$JDK_DIR" --strip-components=1
  rm -f "$tmp_jdk"
  log "JDK 安装完成：$("$JDK_DIR/bin/java" -version 2>&1 | head -1)"
fi

# ---------------- 3. 运行用户与目录 ----------------
if ! id orpl >/dev/null 2>&1; then
  log "创建运行用户 orpl"
  useradd -r -m -d /opt/orpl -s /usr/sbin/nologin orpl
fi
mkdir -p "$APP_ROOT/app" "$APP_ROOT/logs" "$APP_ROOT/conf" "$APP_ROOT/web/download" "$APP_ROOT/web/uploads"
cp -f "$APP_ROOT/package/orpl-service.jar" "$APP_ROOT/app/orpl-service.jar"
if [ -f "$APP_ROOT/package/orpl-admin.apk" ]; then
  cp -f "$APP_ROOT/package/orpl-admin.apk" "$APP_ROOT/web/download/orpl-admin.apk"
fi

chmod 755 "$APP_ROOT" "$APP_ROOT/app" "$APP_ROOT/web" "$APP_ROOT/web/download" 2>/dev/null || true
[ -d "$APP_ROOT/web/admin" ]  && chmod -R a+rX "$APP_ROOT/web/admin"
[ -d "$APP_ROOT/web/portal" ] && chmod -R a+rX "$APP_ROOT/web/portal"
chown -R orpl:orpl "$APP_ROOT/app" "$APP_ROOT/logs" "$APP_ROOT/conf" "$APP_ROOT/web/uploads"
log "目录准备完成：$APP_ROOT"

# ---------------- 4. 环境变量文件 ----------------
if [ ! -f "$APP_ROOT/conf/orpl.env" ]; then
  log "生成 $APP_ROOT/conf/orpl.env"
  cat > "$APP_ROOT/conf/orpl.env" <<EOF
SERVER_PORT=8080
DB_USER=$DB_USER
DB_PASSWORD=$DB_PASS
REDIS_PASSWORD=$REDIS_PASS
WECHAT_APPID=
WECHAT_SECRET=
OSS_ENDPOINT=
OSS_BUCKET=
OSS_REGION=
OSS_ACCESS_KEY_ID=
OSS_ACCESS_KEY_SECRET=
KNIFE4J_ENABLE=true
EOF
  chmod 640 "$APP_ROOT/conf/orpl.env"
  chown orpl:orpl "$APP_ROOT/conf/orpl.env"
else
  warn "已存在 $APP_ROOT/conf/orpl.env，保留原配置不覆盖"
  DB_PASS=$(grep -E '^DB_PASSWORD=' "$APP_ROOT/conf/orpl.env" | cut -d= -f2- || true)
  REDIS_PASS=$(grep -E '^REDIS_PASSWORD=' "$APP_ROOT/conf/orpl.env" | cut -d= -f2- || true)
  DB_PASS=${DB_PASS:-Orpl@2026}
  REDIS_PASS=${REDIS_PASS:-Orpl@2026}
fi

# ---------------- 5. MySQL 初始化 ----------------
if [ "$LOW_MEM" -eq 1 ]; then
  if [ ! -f /swapfile ]; then
    log "内存约 ${MEM_MB}MB，创建 1GB swap，避免 MySQL 与 JVM 同时启动被 OOM Kill"
    fallocate -l 1G /swapfile 2>/dev/null || dd if=/dev/zero of=/swapfile bs=1M count=1024 status=none
    chmod 600 /swapfile
    mkswap /swapfile >/dev/null
    swapon /swapfile
    grep -q '^/swapfile ' /etc/fstab 2>/dev/null || echo '/swapfile none swap sw 0 0' >> /etc/fstab
  fi
  log "写入 MySQL 低内存配置（buffer pool 64M，关闭 performance_schema）"
  cat > /etc/mysql/mysql.conf.d/zz-orpl-lowmem.cnf <<'CNF'
[mysqld]
innodb_buffer_pool_size=64M
performance_schema=OFF
max_connections=60
table_open_cache=200
CNF
  svc_restart mysql
fi

log "启动 MySQL"
install -d -o mysql -g mysql -m 755 /var/run/mysqld 2>/dev/null || true
svc_start mysql
for i in $(seq 1 40); do
  mysqladmin ping >/dev/null 2>&1 && break
  sleep 1
done
mysqladmin ping >/dev/null 2>&1 || die "MySQL 未能启动，查看 /var/log/mysql/error.log"

log "创建数据库 / 账号 / 导入初始化 SQL"
mysql -uroot <<SQL
CREATE DATABASE IF NOT EXISTS \`${DB_NAME}\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE USER IF NOT EXISTS '${DB_USER}'@'127.0.0.1' IDENTIFIED BY '${DB_PASS}';
ALTER USER '${DB_USER}'@'127.0.0.1' IDENTIFIED BY '${DB_PASS}';
GRANT ALL PRIVILEGES ON \`${DB_NAME}\`.* TO '${DB_USER}'@'127.0.0.1';
FLUSH PRIVILEGES;
SQL

TABLE_COUNT=$(mysql -N -B -uroot -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='${DB_NAME}';")
if [ "${TABLE_COUNT:-0}" -eq 0 ]; then
  for f in 01-schema.sql 02-demo-data.sql 03-migrate-order-status.sql 04-migrate-dining-round.sql; do
    if [ -f "$APP_ROOT/sql/$f" ]; then
      log "导入 $f"
      mysql --default-character-set=utf8mb4 -uroot "$DB_NAME" < "$APP_ROOT/sql/$f"
    fi
  done
  log "数据库初始化完成（管理端默认账号 admin / 123456）"
else
  warn "库 ${DB_NAME} 已有 ${TABLE_COUNT} 张表，跳过初始化导入（如需重来请手动 DROP DATABASE orpl）"
fi

# ---------------- 6. Redis 密码 ----------------
log "配置 Redis 密码"
REDIS_CONF=/etc/redis/redis.conf
if [ -f "$REDIS_CONF" ]; then
  if grep -qE '^requirepass ' "$REDIS_CONF"; then
    sed -i "s|^requirepass .*|requirepass ${REDIS_PASS}|" "$REDIS_CONF"
  elif grep -qE '^# requirepass' "$REDIS_CONF"; then
    sed -i "s|^# requirepass .*|requirepass ${REDIS_PASS}|" "$REDIS_CONF"
  else
    echo "requirepass ${REDIS_PASS}" >> "$REDIS_CONF"
  fi
  sed -i 's|^bind .*|bind 127.0.0.1 ::1|' "$REDIS_CONF" || true
  if [ "$LOW_MEM" -eq 1 ]; then
    # 96MB 对演示足够，noeviction 避免令牌被静默淘汰
    sed -i '/^maxmemory /d' "$REDIS_CONF" || true
    { echo "maxmemory 96mb"; echo "maxmemory-policy noeviction"; } >> "$REDIS_CONF"
  fi
  svc_restart redis-server
  sleep 2
  # 兜底：旧实例没有被 systemd/service 接管时（容器或手工启动过 Redis），
  # 直接按配置文件重启，否则 requirepass 不生效、后端会报 AUTH 失败
  if ! redis-cli -a "$REDIS_PASS" --no-auth-warning ping 2>/dev/null | grep -q PONG; then
    warn "Redis 未按新配置重启，改为直接按配置文件启动"
    pkill -x redis-server 2>/dev/null || true
    sleep 1
    redis-server "$REDIS_CONF" --daemonize yes 2>/dev/null || true
    sleep 2
  fi
  if redis-cli -a "$REDIS_PASS" --no-auth-warning ping 2>/dev/null | grep -q PONG; then
    log "Redis 密码生效"
  else
    warn "Redis 密码校验失败，检查 $REDIS_CONF"
  fi
else
  warn "未找到 $REDIS_CONF，跳过 Redis 密码配置"
fi

# ---------------- 7. nginx ----------------
log "配置 nginx 站点"
cp -f "$APP_ROOT/conf/nginx-orpl.conf" /etc/nginx/conf.d/orpl.conf
[ -L /etc/nginx/sites-enabled/default ] && rm -f /etc/nginx/sites-enabled/default
nginx -t >/dev/null 2>&1 || die "nginx 配置校验失败：nginx -t"
svc_start nginx
(nginx -s reload >/dev/null 2>&1 || true)
log "nginx 已加载（端口 ${HTTP_PORT}）"

# ---------------- 8. 后端启动 ----------------
log "注册后端服务"
cp -f "$APP_ROOT/conf/orpl.service" /etc/systemd/system/orpl.service

if [ "$LOW_MEM" -eq 1 ]; then
  warn "内存 ${MEM_MB}MB：JVM 调整为 -Xmx256m / Metaspace 192m"
  JAVA_OPTS="-Xms128m -Xmx256m -XX:MaxMetaspaceSize=192m -XX:+UseSerialGC -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Shanghai"
  sed -i 's|-Xms256m -Xmx512m -XX:+UseSerialGC|-Xms128m -Xmx256m -XX:MaxMetaspaceSize=192m -XX:+UseSerialGC|' /etc/systemd/system/orpl.service
fi

if [ "$HAS_SYSTEMD" -eq 1 ]; then
  systemctl daemon-reload
  systemctl enable orpl >/dev/null 2>&1 || true
  systemctl restart orpl || die "后端启动失败：journalctl -u orpl -n 80 --no-pager"
else
  warn "当前环境没有 systemd（容器），降级为 nohup 后台启动；云服务器上会自动走 systemd"
  pkill -f 'orpl-service.jar' 2>/dev/null || true
  sleep 2
  set -a
  . "$APP_ROOT/conf/orpl.env"
  set +a
  su -s /bin/bash orpl -c "cd /opt/orpl && setsid nohup $JDK_DIR/bin/java $JAVA_OPTS -jar /opt/orpl/app/orpl-service.jar --spring.profiles.active=prod --spring.config.additional-location=file:/opt/orpl/conf/ >> /opt/orpl/logs/app.log 2>&1 < /dev/null &"
fi

log "等待后端健康检查"
ok=0
for i in $(seq 1 60); do
  # 必须是本后端返回的业务标识，避免 8080 被别的服务占用时误判为成功
  if curl -fsS "http://127.0.0.1:8080/health" 2>/dev/null | grep -q 'order-platform-ok'; then ok=1; break; fi
  sleep 2
done
if [ "$ok" -eq 1 ]; then
  log "后端 /health 正常：$(curl -s http://127.0.0.1:8080/health)"
else
  warn "60 秒内 /health 未就绪，可能仍在启动；稍后执行：systemctl status orpl --no-pager -l 或 tail -100 /opt/orpl/logs/app.log"
fi

# ---------------- 9. 汇总 ----------------
PUBLIC_IP=$(curl -s --max-time 8 https://ifconfig.me 2>/dev/null || true)
echo
echo "============================================================"
printf " 部署完成\n"
echo "============================================================"
echo "  管理端 Web    : http://${PUBLIC_IP:-服务器IP}/admin/   （admin / 123456）"
echo "  项目门户      : http://${PUBLIC_IP:-服务器IP}/"
echo "  安卓 APK      : http://${PUBLIC_IP:-服务器IP}/download/orpl-admin.apk"
echo "  接口文档      : http://${PUBLIC_IP:-服务器IP}/doc.html"
echo "  健康检查      : http://${PUBLIC_IP:-服务器IP}/health"
echo
echo "  后端日志      : journalctl -u orpl -f   或   tail -f /opt/orpl/logs/app.log"
echo "  配置文件      : /opt/orpl/conf/application-prod.yml   /opt/orpl/conf/orpl.env"
echo "  重载后端      : systemctl restart orpl"
echo
echo "  如果外网访问不了，检查云厂商安全组是否放通 TCP ${HTTP_PORT}"
echo "  本机防火墙：ufw allow ${HTTP_PORT}/tcp"
echo "============================================================"
