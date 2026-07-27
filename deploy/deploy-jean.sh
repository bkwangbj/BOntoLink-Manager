#!/usr/bin/env bash
# ============================================================
# BOntoLink Jean 服务器部署脚本
#
# Jean 是一台独立服务器，运行完整应用栈：
#   bontolink-admin.jar     管理后台  端口 8088  /bontolink
#   bontolink-ontology.jar  本体服务  端口 8089  /bontolink-ontology
#   frontend/dist           Vue SPA   Nginx 托管
#   PostgreSQL              共享数据库
#
# 两个服务是独立进程，通过 HTTP 通信。
# 未来可随时将 ontology 进程搬到另一台机器，无需改代码。
#
# 用法:
#   ./deploy-jean.sh build              # 本地构建(两个 jar + 前端)
#   ./deploy-jean.sh install            # 首次部署(全流程)
#   ./deploy-jean.sh upgrade            # 升级(构建 + 推送 + 重启)
#   ./deploy-jean.sh start              # 启动所有服务
#   ./deploy-jean.sh stop               # 停止所有服务
#   ./deploy-jean.sh restart            # 重启所有服务
#   ./deploy-jean.sh status             # 查看状态
#   ./deploy-jean.sh logs [service]     # 查看日志 (admin|ontology)
#   ./deploy-jean.sh init-db            # 初始化数据库
#   ./deploy-jean.sh nginx              # 生成 Nginx 配置
# ============================================================
set -euo pipefail

# ========== 配置区（按实际环境修改） ==========

# --- 目标服务器 ---
JEAN_HOST="${JEAN_HOST:-jean.beiktech.com}"
JEAN_USER="${JEAN_USER:-root}"
JEAN_PORT="${JEAN_PORT:-22}"

# --- Admin 服务 ---
ADMIN_NAME="bontolink-admin"
ADMIN_PORT=8088
ADMIN_CONTEXT="/bontolink"
ADMIN_JAR="bontolink-admin.jar"

# --- Ontology 服务 ---
ONT_NAME="bontolink-ontology"
ONT_PORT=8089
ONT_CONTEXT="/bontolink-ontology"
ONT_JAR="bontolink-ontology.jar"

# --- 远程目录 ---
BASE_DIR="/opt/bontolink"
ADMIN_DIR="${BASE_DIR}/${ADMIN_NAME}"
ONT_DIR="${BASE_DIR}/${ONT_NAME}"
FRONTEND_DIR="${BASE_DIR}/frontend"
LOG_DIR="/var/log/bontolink"
DATA_DIR="/data/bontolink"

# --- JDK ---
JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/jdk-21}"
JAVA_OPTS="${JAVA_OPTS:--Xms512m -Xmx2g -Djava.security.egd=file:/dev/./urandom}"

# --- 数据库（共享） ---
DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-bontolink}"
DB_SCHEMA="${DB_SCHEMA:-bonto_link_manager}"
DB_USER="${DB_USER:-bontolink}"
DB_PASS="${DB_PASS:-bontolink}"

# --- Redis（可选） ---
REDIS_HOST="${REDIS_HOST:-}"
REDIS_PORT="${REDIS_PORT:-6379}"
REDIS_PASSWORD="${REDIS_PASSWORD:-}"

# --- 本地路径 ---
LOCAL_BACKEND_DIR="$(cd "$(dirname "$0")/../backend" && pwd)"
LOCAL_FRONTEND_DIR="$(cd "$(dirname "$0")/../frontend" && pwd)"

# ========== 颜色 ==========
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; CYAN='\033[0;36m'; NC='\033[0m'
info()  { echo -e "${GREEN}[INFO]${NC} $1"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
error() { echo -e "${RED}[ERROR]${NC} $1"; exit 1; }
step()  { echo -e "\n${CYAN}━━━ $1 ━━━${NC}"; }

# ========== 前置检查 ==========
precheck() {
    step "前置检查"
    command -v java >/dev/null 2>&1 || error "Java 未安装，请安装 JDK 21"
    command -v mvn >/dev/null 2>&1 || error "Maven 未安装"
    command -v node >/dev/null 2>&1 || error "Node.js 未安装"
    command -v npm >/dev/null 2>&1 || error "npm 未安装"
    command -v ssh >/dev/null 2>&1 || error "SSH 未安装"

    java -version 2>&1 | grep -q "21" || warn "Java 版本可能不是 21，当前: $(java -version 2>&1 | head -1)"
    info "前置检查通过"
}

# ========== 本地构建 ==========
do_build() {
    step "构建项目"

    # 1. 构建后端所有模块
    info "构建后端模块..."
    cd "${LOCAL_BACKEND_DIR}"
    ./mvnw -DskipTests clean package -q
    info "后端构建完成"

    # 2. 定位并复制 admin jar
    local admin_src="${LOCAL_BACKEND_DIR}/bontolink-admin/target/${ADMIN_JAR}"
    if [ ! -f "$admin_src" ]; then
        admin_src=$(ls "${LOCAL_BACKEND_DIR}/bontolink-admin/target/bontolink-admin-*.jar" 2>/dev/null | head -1)
    fi
    [ -n "$admin_src" ] && [ -f "$admin_src" ] || error "找不到 admin jar 构建产物"
    cp "$admin_src" "${BUILD_DIR}/${ADMIN_JAR}"
    info "Admin JAR: ${BUILD_DIR}/${ADMIN_JAR}"

    # 3. 定位并复制 ontology jar（带 classifier=exec）
    local ont_src="${LOCAL_BACKEND_DIR}/bontolink-ontology/target/bontolink-ontology-exec.jar"
    if [ ! -f "$ont_src" ]; then
        ont_src=$(ls "${LOCAL_BACKEND_DIR}/bontolink-ontology/target/bontolink-ontology-*-exec.jar" 2>/dev/null | head -1)
    fi
    # 如果没有 exec jar，用普通 jar
    if [ -z "$ont_src" ] || [ ! -f "$ont_src" ]; then
        ont_src=$(ls "${LOCAL_BACKEND_DIR}/bontolink-ontology/target/bontolink-ontology-*.jar" 2>/dev/null | grep -v -- '-exec' | grep -v 'original' | head -1)
    fi
    [ -n "$ont_src" ] && [ -f "$ont_src" ] || error "找不到 ontology jar 构建产物"
    cp "$ont_src" "${BUILD_DIR}/${ONT_JAR}"
    info "Ontology JAR: ${BUILD_DIR}/${ONT_JAR}"

    # 4. 构建前端
    info "构建前端..."
    cd "${LOCAL_FRONTEND_DIR}"
    npm install --silent 2>/dev/null || npm install
    npx vite build
    info "前端构建完成: ${LOCAL_FRONTEND_DIR}/dist"

    step "构建完成"
}

# ========== 推送文件到 Jean ==========
push_files() {
    step "推送文件到 Jean (${JEAN_HOST})"

    ssh -p "${JEAN_PORT}" "${JEAN_USER}@${JEAN_HOST}" \
        "mkdir -p ${ADMIN_DIR}/config ${ONT_DIR}/config ${FRONTEND_DIR} ${LOG_DIR}/admin ${LOG_DIR}/ontology ${DATA_DIR}"

    # Admin JAR
    info "推送 Admin JAR..."
    scp -P "${JEAN_PORT}" "${BUILD_DIR}/${ADMIN_JAR}" \
        "${JEAN_USER}@${JEAN_HOST}:${ADMIN_DIR}/${ADMIN_JAR}"

    # Ontology JAR
    info "推送 Ontology JAR..."
    scp -P "${JEAN_PORT}" "${BUILD_DIR}/${ONT_JAR}" \
        "${JEAN_USER}@${JEAN_HOST}:${ONT_DIR}/${ONT_JAR}"

    # 前端
    info "推送前端静态文件..."
    ssh -p "${JEAN_PORT}" "${JEAN_USER}@${JEAN_HOST}" "rm -rf ${FRONTEND_DIR}/dist"
    scp -P "${JEAN_PORT}" -r "${LOCAL_FRONTEND_DIR}/dist" \
        "${JEAN_USER}@${JEAN_HOST}:${FRONTEND_DIR}/dist"

    info "文件推送完成"
}

# ========== 生成 Admin 远程配置 ==========
generate_admin_config() {
    local config_path="${ADMIN_DIR}/config/application-remote.yml"

    local redis_block=""
    if [ -n "${REDIS_HOST}" ]; then
        redis_block=$(cat <<REDIS
  data:
    redis:
      host: ${REDIS_HOST}
      port: ${REDIS_PORT}
      password: ${REDIS_PASSWORD}
      timeout: 3000
      lettuce:
        pool:
          max-active: 16
          max-idle: 8
          min-idle: 4
REDIS
        )
    fi

    # 通过 heredoc 写入 SSH 命令，避免变量展开问题
    # 使用 base64 编码传输，避免特殊字符被 shell 二次解释
    local yaml_content
    yaml_content=$(cat <<YAML
server:
  port: ${ADMIN_PORT}
  servlet:
    context-path: ${ADMIN_CONTEXT}

spring:
  application:
    name: ${ADMIN_NAME}
  datasource:
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}?currentSchema=${DB_SCHEMA}
    username: ${DB_USER}
    password: ${DB_PASS}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      pool-name: BontoLinkAdminPool
      connection-test-query: SELECT 1
      idle-timeout: 300000
      connection-timeout: 30000
      max-lifetime: 1800000
      leak-detection-threshold: 0

${redis_block}

  flyway:
    enabled: true
    locations: classpath:db/migration/postgresql,classpath:db/migration/common
    baseline-on-migrate: true
    baseline-version: 0
    out-of-order: true

  jackson:
    default-property-inclusion: non_null
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: Asia/Shanghai

bontolink:
  cache:
    type: local

mybatis:
  configuration:
    map-underscore-to-camel-case: true
    default-fetch-size: 100
    default-statement-timeout: 30

logging:
  level:
    root: INFO
    com.beiktech.bontolink: INFO
  file:
    path: ${LOG_DIR}/admin
    name: ${LOG_DIR}/admin/admin.log
  logback:
    rollingpolicy:
      max-history: 14
      max-file-size: 100MB
YAML
    )

    echo "$yaml_content" | ssh -p "${JEAN_PORT}" "${JEAN_USER}@${JEAN_HOST}" \
        "cat > ${config_path}"
    info "Admin 配置已生成: ${config_path}"
}

# ========== 生成 Ontology 远程配置 ==========
generate_ontology_config() {
    local config_path="${ONT_DIR}/config/application-ontology.yml"

    local yaml_content
    yaml_content=$(cat <<YAML
server:
  port: ${ONT_PORT}
  servlet:
    context-path: ${ONT_CONTEXT}

spring:
  application:
    name: ${ONT_NAME}
  datasource:
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}?currentSchema=${DB_SCHEMA}
    username: ${DB_USER}
    password: ${DB_PASS}
    hikari:
      maximum-pool-size: 10
      minimum-idle: 2
      connection-test-query: SELECT 1

  jackson:
    default-property-inclusion: non_null
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: Asia/Shanghai

bontolink:
  ontology:
    # Jena 开关
    jena-enabled: true
    storage-mode: tdb2
    tdb2-location: ${DATA_DIR}/tdb2
    reasoner-type: owl-micro
    sparql-enabled: true
    tdb2:
      bulkload: false
      sync-interval-ms: 5000
    # 向量库配置
    vector:
      enabled: true
      type: milvus
      dimension: 768
      milvus-host: 127.0.0.1
      milvus-port: 19530
      # milvus-home: "/opt/milvus"

mybatis:
  configuration:
    map-underscore-to-camel-case: true

logging:
  level:
    root: INFO
    com.beiktech.bontolink: INFO
    org.apache.jena: WARN
  file:
    path: ${LOG_DIR}/ontology
    name: ${LOG_DIR}/ontology/ontology.log
  logback:
    rollingpolicy:
      max-history: 14
      max-file-size: 100MB
YAML
    )

    echo "$yaml_content" | ssh -p "${JEAN_PORT}" "${JEAN_USER}@${JEAN_HOST}" \
        "cat > ${config_path}"
    info "Ontology 配置已生成: ${config_path}"
}

# ========== 注册 systemd 服务 ==========
setup_systemd_admin() {
    step "注册 Admin systemd 服务"

    local unit_content
    unit_content=$(cat <<UNIT
[Unit]
Description=BOntoLink Admin Service
After=network.target postgresql.service
Wants=postgresql.service

[Service]
Type=simple
User=root
WorkingDirectory=${ADMIN_DIR}

Environment="JAVA_HOME=${JAVA_HOME}"
ExecStart=${JAVA_HOME}/bin/java ${JAVA_OPTS} \\
    -Dspring.profiles.active=remote \\
    -jar ${ADMIN_DIR}/${ADMIN_JAR}

ExecStop=/bin/kill -15 \\\$MAINPID
SuccessExitStatus=143
Restart=always
RestartSec=10

StandardOutput=append:${LOG_DIR}/admin/stdout.log
StandardError=append:${LOG_DIR}/admin/stderr.log

[Install]
WantedBy=multi-user.target
UNIT
    )

    echo "$unit_content" | ssh -p "${JEAN_PORT}" "${JEAN_USER}@${JEAN_HOST}" \
        "cat > /etc/systemd/system/${ADMIN_NAME}.service"
    info "Admin systemd 服务已注册"
}

setup_systemd_ontology() {
    step "注册 Ontology systemd 服务"

    local unit_content
    unit_content=$(cat <<UNIT
[Unit]
Description=BOntoLink Ontology Service (Jena TDB2)
After=network.target postgresql.service
Wants=postgresql.service

[Service]
Type=simple
User=root
WorkingDirectory=${ONT_DIR}

Environment="JAVA_HOME=${JAVA_HOME}"
Environment="OPENAI_API_KEY=${OPENAI_API_KEY:-}"
ExecStart=${JAVA_HOME}/bin/java ${JAVA_OPTS} \\
    -Dspring.profiles.active=ontology \\
    -jar ${ONT_DIR}/${ONT_JAR}

ExecStop=/bin/kill -15 \\\$MAINPID
SuccessExitStatus=143
Restart=always
RestartSec=10

StandardOutput=append:${LOG_DIR}/ontology/stdout.log
StandardError=append:${LOG_DIR}/ontology/stderr.log

[Install]
WantedBy=multi-user.target
UNIT
    )

    echo "$unit_content" | ssh -p "${JEAN_PORT}" "${JEAN_USER}@${JEAN_HOST}" \
        "cat > /etc/systemd/system/${ONT_NAME}.service"
    info "Ontology systemd 服务已注册"

    ssh -p "${JEAN_PORT}" "${JEAN_USER}@${JEAN_HOST}" "systemctl daemon-reload"
}

# ========== 配置 Nginx ==========
setup_nginx() {
    step "配置 Nginx"

    local nginx_content
    nginx_content=$(cat <<NGINX
# BOntoLink Jean 服务器 Nginx 配置
# 反向代理: /api/* → admin(8088), /api/ontology/* → ontology(8089)
# 前端 SPA: / → /opt/bontolink/frontend/dist

server {
    listen 80;
    server_name ${JEAN_HOST};

    # ===== 后端 API 路由 =====

    # 本体服务 API（优先匹配，放在 admin 之前）
    location /api/ontology/ {
        proxy_pass http://127.0.0.1:${ONT_PORT}${ONT_CONTEXT}/;
        proxy_set_header Host \\\$host;
        proxy_set_header X-Real-IP \\\$remote_addr;
        proxy_set_header X-Forwarded-For \\\$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \\\$scheme;
        proxy_connect_timeout 60s;
        proxy_read_timeout 120s;
        proxy_send_timeout 60s;
    }

    # 管理后台 API
    location /api/ {
        proxy_pass http://127.0.0.1:${ADMIN_PORT}${ADMIN_CONTEXT}/;
        proxy_set_header Host \\\$host;
        proxy_set_header X-Real-IP \\\$remote_addr;
        proxy_set_header X-Forwarded-For \\\$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \\\$scheme;
        proxy_connect_timeout 60s;
        proxy_read_timeout 60s;
        proxy_send_timeout 60s;
    }

    # ===== 前端 SPA =====
    location / {
        root ${FRONTEND_DIR}/dist;
        index index.html;
        try_files \\\$uri \\\$uri/ /index.html;

        location ~* \\.(js|css|png|jpg|jpeg|gif|ico|svg|woff2?|ttf|eot)$ {
            expires 30d;
            add_header Cache-Control "public, immutable";
        }
    }

    # 健康检查
    location /health {
        access_log off;
        return 200 "OK\\n";
        add_header Content-Type text/plain;
    }
}

# HTTPS（启用时取消注释并配置证书路径）
# server {
#     listen 443 ssl http2;
#     server_name ${JEAN_HOST};
#     ssl_certificate     /etc/nginx/ssl/${JEAN_HOST}.pem;
#     ssl_certificate_key /etc/nginx/ssl/${JEAN_HOST}.key;
#     ssl_protocols       TLSv1.2 TLSv1.3;
#     ssl_ciphers         HIGH:!aNULL:!MD5;
#     ...同上的 location 块...
# }
NGINX
    )

    echo "$nginx_content" | ssh -p "${JEAN_PORT}" "${JEAN_USER}@${JEAN_HOST}" \
        "cat > /etc/nginx/conf.d/bontolink.conf"

    ssh -p "${JEAN_PORT}" "${JEAN_USER}@${JEAN_HOST}" \
        "nginx -t 2>/dev/null && (systemctl reload nginx 2>/dev/null || nginx -s reload 2>/dev/null || systemctl restart nginx)" \
        && info "Nginx 配置已更新" || warn "Nginx 重载失败，请手动检查"
}

# ========== 服务生命周期 ==========
start_service() {
    local svc="$1"
    step "启动 ${svc}"
    ssh -p "${JEAN_PORT}" "${JEAN_USER}@${JEAN_HOST}" \
        "systemctl enable ${svc} 2>/dev/null; systemctl start ${svc}"
}

start_all() {
    start_service "${ADMIN_NAME}"
    start_service "${ONT_NAME}"

    # 等待就绪
    info "等待服务就绪..."
    local retries=30
    for i in $(seq 1 $retries); do
        local admin_ok=0 ont_ok=0
        curl -sf "http://${JEAN_HOST}/api/health" >/dev/null 2>&1 && admin_ok=1
        curl -sf "http://${JEAN_HOST}/api/ontology/health" >/dev/null 2>&1 && ont_ok=1
        if [ "$admin_ok" = "1" ] && [ "$ont_ok" = "1" ]; then
            info "所有服务已就绪! (耗时 ${i}s)"
            return 0
        fi
        sleep 1
    done

    warn "部分服务启动超时（${retries}s），请检查日志:"
    echo "  ssh ${JEAN_USER}@${JEAN_HOST} 'journalctl -u ${ADMIN_NAME} -f'"
    echo "  ssh ${JEAN_USER}@${JEAN_HOST} 'journalctl -u ${ONT_NAME} -f'"
}

stop_service() {
    local svc="$1"
    info "停止 ${svc}..."
    ssh -p "${JEAN_PORT}" "${JEAN_USER}@${JEAN_HOST}" \
        "systemctl stop ${svc} || true"
}

stop_all() {
    step "停止所有服务"
    stop_service "${ADMIN_NAME}"
    stop_service "${ONT_NAME}"
    info "所有服务已停止"
}

restart_all() {
    stop_all
    sleep 2
    start_all
}

service_status() {
    echo ""
    ssh -p "${JEAN_PORT}" "${JEAN_USER}@${JEAN_HOST}" "
        echo '━━━ BOntoLink Jean 服务状态 ━━━'
        echo ''

        for svc in ${ADMIN_NAME} ${ONT_NAME}; do
            if systemctl is-active --quiet \\\$svc 2>/dev/null; then
                printf '  %-30s %b运行中%b\\n' \"\\\$svc\" '${GREEN}' '${NC}'
            else
                printf '  %-30s %b未运行%b\\n' \"\\\$svc\" '${RED}' '${NC}'
            fi
        done

        echo ''
        echo '  Nginx:'
        systemctl is-active nginx >/dev/null 2>&1 \
            && echo '    ● 运行中' \
            || echo '    ○ 未运行'

        echo ''
        echo '  PostgreSQL:'
        systemctl is-active postgresql >/dev/null 2>&1 \
            && echo '    ● 运行中' \
            || echo '    ○ 未运行'

        echo ''
        echo '━━━ 资源使用 ━━━'
        ps aux | grep java | grep -v grep | awk '{printf \"  %-35s CPU: %s%%  MEM: %s%%\\n\", \\\$12, \\\$3, \\\$4}'
        echo ''
        df -h ${DATA_DIR} 2>/dev/null | tail -1 | awk '{print \"  \" \\\$6 \"  \" \\\$3 \" / \" \\\$4 \" (\" \\\$5 \")\"}'
    "
}

show_logs() {
    local svc="${1:-admin}"
    local tail_lines="${2:-50}"
    local unit_name

    case "$svc" in
        admin|${ADMIN_NAME}) unit_name="${ADMIN_NAME}" ;;
        ont|ontology|${ONT_NAME}) unit_name="${ONT_NAME}" ;;
        *) warn "未知服务: $svc，可选: admin / ontology"; return 1 ;;
    esac

    step "日志: ${unit_name} (最近 ${tail_lines} 行)"
    ssh -p "${JEAN_PORT}" "${JEAN_USER}@${JEAN_HOST}" \
        "journalctl -u ${unit_name} -n ${tail_lines} -f --no-pager" 2>/dev/null \
        || warn "无法获取日志，请手动执行: ssh ${JEAN_USER}@${JEAN_HOST} 'journalctl -u ${unit_name} -f'"
}

# ========== 初始化数据库 ==========
init_database() {
    step "初始化数据库"

    info "在 Jean 上创建数据库和 schema..."
    ssh -p "${JEAN_PORT}" "${JEAN_USER}@${JEAN_HOST}" "
        # 安装 PostgreSQL（如果未安装）
        command -v psql >/dev/null 2>&1 || { echo 'PostgreSQL 未安装，跳过数据库初始化'; exit 1; }

        su - postgres -c \"psql -tc \\\"SELECT 1 FROM pg_database WHERE datname='${DB_NAME}'\\\" | grep -q 1 || createdb ${DB_NAME}\"
        su - postgres -c \"psql -d ${DB_NAME} -c \\\"CREATE SCHEMA IF NOT EXISTS ${DB_SCHEMA};\\\"\"

        # 创建用户（幂等）
        su - postgres -c \"psql -d ${DB_NAME} -c \\\"
            DO \\\$\\\$ BEGIN
                IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = '${DB_USER}') THEN
                    CREATE USER ${DB_USER} WITH PASSWORD '${DB_PASS}';
                END IF;
            END \\\$\\\$;
        \\\"\"
        su - postgres -c \"psql -d ${DB_NAME} -c \\\"GRANT ALL ON SCHEMA ${DB_SCHEMA} TO ${DB_USER};\\\"\"
        su - postgres -c \"psql -d ${DB_NAME} -c \\\"ALTER DEFAULT PRIVILEGES IN SCHEMA ${DB_SCHEMA} GRANT ALL ON TABLES TO ${DB_USER};\\\"\"
        su - postgres -c \"psql -d ${DB_NAME} -c \\\"ALTER DEFAULT PRIVILEGES IN SCHEMA ${DB_SCHEMA} GRANT ALL ON SEQUENCES TO ${DB_USER};\\\"\"

        echo '数据库初始化完成'
    "

    info "数据库初始化完成（表由 Flyway 在 admin 首次启动时自动创建）"
}

# ========== 初始化目录 ==========
setup_dirs() {
    step "在 Jean 上创建目录结构"

    ssh -p "${JEAN_PORT}" "${JEAN_USER}@${JEAN_HOST}" \
        "mkdir -p ${ADMIN_DIR}/config ${ONT_DIR}/config ${FRONTEND_DIR} ${LOG_DIR}/admin ${LOG_DIR}/ontology ${DATA_DIR}/tdb2"

    info "目录结构已创建"
}

# ========== 首次部署 ==========
do_install() {
    precheck
    do_build
    setup_dirs
    push_files
    generate_admin_config
    generate_ontology_config
    setup_systemd_admin
    setup_systemd_ontology
    setup_nginx
    init_database
    start_all

    echo ""
    echo -e "${GREEN}╔════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║  BOntoLink 部署完成!                      ║${NC}"
    echo -e "${GREEN}╠════════════════════════════════════════════╣${NC}"
    echo -e "${GREEN}║  前端:        http://${JEAN_HOST}/        ${NC}"
    echo -e "${GREEN}║  Admin API:   http://${JEAN_HOST}/api/     ${NC}"
    echo -e "${GREEN}║  Ontology:    http://${JEAN_HOST}/api/ontology/health${NC}"
    echo -e "${GREEN}║  日志:        journalctl -u ${ADMIN_NAME} -f${NC}"
    echo -e "${GREEN}║               journalctl -u ${ONT_NAME} -f  ${NC}"
    echo -e "${GREEN}╚════════════════════════════════════════════╝${NC}"
}

# ========== 升级 ==========
do_upgrade() {
    step "升级部署"
    do_build
    push_files
    info "注意: 配置变更需手动更新 ${ADMIN_DIR}/config/ 和 ${ONT_DIR}/config/ 下的 yml"
    restart_all
    info "升级完成"
}

# ========== 主入口 ==========
main() {
    BUILD_DIR="$(cd "$(dirname "$0")" && pwd)/build"
    mkdir -p "${BUILD_DIR}"

    case "${1:-help}" in
        build)
            precheck
            do_build
            echo ""
            info "构建产物:"
            echo "  Admin JAR:     ${BUILD_DIR}/${ADMIN_JAR}"
            echo "  Ontology JAR:  ${BUILD_DIR}/${ONT_JAR}"
            echo "  Frontend dist: ${LOCAL_FRONTEND_DIR}/dist/"
            ;;
        install)    do_install ;;
        upgrade)    do_upgrade ;;
        start)      start_all ;;
        stop)       stop_all ;;
        restart)    restart_all ;;
        status)     service_status ;;
        logs)       show_logs "${2:-admin}" "${3:-50}" ;;
        init-db)    init_database ;;
        nginx)      setup_nginx ;;
        push)
            precheck
            push_files
            ;;
        config)
            generate_admin_config
            generate_ontology_config
            ;;
        *)
            echo "用法: $0 {build|install|upgrade|start|stop|restart|status|logs|init-db|nginx|push|config}"
            echo ""
            echo "  install   首次部署（构建 + 推送 + 配置 + 启动）"
            echo "  upgrade   升级（构建 + 推送 + 重启）"
            echo "  build     仅本地构建（两个 JAR + 前端）"
            echo "  push      仅推送文件"
            echo "  config    仅重新生成配置"
            echo "  nginx     仅更新 Nginx 配置"
            echo "  init-db   初始化 PostgreSQL 数据库"
            echo "  start/stop/restart   服务管理"
            echo "  status    查看运行状态"
            echo "  logs      [admin|ontology] 查看日志"
            echo ""
            echo "环境变量:"
            echo "  JEAN_HOST    目标地址 (默认 jean.beiktech.com)"
            echo "  DB_HOST/PORT/NAME/USER/PASS  数据库连接"
            echo "  REDIS_HOST/PORT/PASSWORD     Redis（可选）"
            echo ""
            echo "示例:"
            echo "  JEAN_HOST=192.168.1.100 DB_PASS=secret ./deploy-jean.sh install"
            echo "  ./deploy-jean.sh upgrade"
            echo "  ./deploy-jean.sh logs ontology"
            ;;
    esac
}

main "$@"
