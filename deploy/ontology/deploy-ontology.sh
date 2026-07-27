#!/usr/bin/env bash
# ============================================================
# BOntoLink Ontology Service 部署脚本
# 功能：构建、配置、启动 ontology 服务（嵌入式 Jena TDB2 + pgvector）
# 适用：远程 Linux 数据服务器
# 用法：
#   ./deploy-ontology.sh install        # 首次安装
#   ./deploy-ontology.sh upgrade        # 升级（只替换 jar）
#   ./deploy-ontology.sh start          # 启动服务
#   ./deploy-ontology.sh stop           # 停止服务
#   ./deploy-ontology.sh restart        # 重启
#   ./deploy-ontology.sh status         # 查看状态
#   ./deploy-ontology.sh logs           # 查看日志
#   ./deploy-ontology.sh rebuild --ns w_wtr   # 重建某个 namespace 索引
# ============================================================
set -euo pipefail

# ========== 配置区（按实际环境修改） ==========
APP_NAME="bontolink-ontology"
APP_PORT=8089
JAR_NAME="${APP_NAME}-1.0.0.jar"

INSTALL_DIR="/opt/${APP_NAME}"
DATA_DIR="/data/${APP_NAME}"
LOG_DIR="/var/log/${APP_NAME}"
JAR_PATH="${INSTALL_DIR}/${JAR_NAME}"
CONFIG_DIR="${INSTALL_DIR}/config"

# JDK 路径（项目要求 JDK 21）
JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/jdk-21}"
JAVA_OPTS="${JAVA_OPTS:--Xms512m -Xmx2g -Djava.security.egd=file:/dev/./urandom}"

# 数据库配置（从环境变量读取，避免硬编码）
DB_URL="${DB_URL:-jdbc:postgresql://127.0.0.1:5432/bontolink}"
DB_USER="${DB_USER:-bontolink}"
DB_PASS="${DB_PASS:-bontolink}"
REDIS_HOST="${REDIS_HOST:-127.0.0.1}"
REDIS_PORT="${REDIS_PORT:-6379}"

# 远程构建机地址（用于从构建机拉取 jar）
BUILD_SERVER="${BUILD_SERVER:-}"
BUILD_JAR_PATH="${BUILD_JAR_PATH:-/path/to/bontolink-ontology/target/${JAR_NAME}}"

# ========== 颜色 ==========
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'
info()  { echo -e "${GREEN}[INFO]${NC} $1"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
error() { echo -e "${RED}[ERROR]${NC} $1"; exit 1; }

# ========== 前置检查 ==========
precheck() {
    command -v java >/dev/null 2>&1 || error "Java 未安装，请先安装 JDK 21"
    java -version 2>&1 | grep -q "21" || warn "Java 版本可能不是 21，当前: $(java -version 2>&1 | head -1)"
    command -v psql >/dev/null 2>&1 || warn "psql 未安装，跳过 pgvector 检查"
    command -v redis-cli >/dev/null 2>&1 || warn "redis-cli 未安装，跳过 Redis 检查"
}

# ========== 创建目录结构 ==========
setup_dirs() {
    info "创建目录结构..."
    mkdir -p "${INSTALL_DIR}/config"
    mkdir -p "${DATA_DIR}/tdb2/namespaces"
    mkdir -p "${DATA_DIR}/logs"
    mkdir -p "${LOG_DIR}"
    chmod 755 "${DATA_DIR}/tdb2"
    info "目录结构就绪:"
    echo "  安装目录: ${INSTALL_DIR}"
    echo "  数据目录: ${DATA_DIR}/tdb2"
    echo "  日志目录: ${LOG_DIR}"
}

# ========== 生成配置文件 ==========
generate_config() {
    info "生成 application-remote.yml ..."
    cat > "${CONFIG_DIR}/application-remote.yml" <<YAML
server:
  port: ${APP_PORT}
  servlet:
    context-path: /bontolink-ontology

spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USER}
    password: ${DB_PASS}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 300000
      connection-timeout: 30000

  redis:
    host: ${REDIS_HOST}
    port: ${REDIS_PORT}
    timeout: 3000
    lettuce:
      pool:
        max-active: 16
        max-idle: 8
        min-idle: 4

  flyway:
    enabled: true
    locations: classpath:db/migration/postgresql,classpath:db/migration/common

bontolink:
  ontology:
    storage-mode: tdb2
    tdb2-location: ${DATA_DIR}/tdb2
    reasoner-type: owl-micro
    sparql-enabled: true

    tdb2:
      bulkload: false
      sync-interval-ms: 5000

    fusion:
      keyword-only-threshold: 0.9
      jena-timeout-ms: 1000
      vector-timeout-ms: 1000
      ask-timeout-ms: 5000

  embedding:
    provider: openai
    openai:
      api-key: \${OPENAI_API_KEY}
      model: text-embedding-ada-002

mybatis:
  configuration:
    map-underscore-to-camel-case: true

logging:
  level:
    com.beiktech.bontolink: INFO
    org.apache.jena: WARN
  file:
    path: ${LOG_DIR}
    name: ${LOG_DIR}/ontology.log
  logback:
    rollingpolicy:
      max-history: 7
      max-file-size: 100MB
YAML
    info "配置已生成: ${CONFIG_DIR}/application-remote.yml"
}

# ========== 获取 jar 包 ==========
fetch_jar() {
    if [ -n "${BUILD_SERVER}" ]; then
        info "从构建服务器拉取 ${JAR_NAME} ..."
        scp "${BUILD_SERVER}:${BUILD_JAR_PATH}" "${JAR_PATH}.tmp"
        mv "${JAR_PATH}.tmp" "${JAR_PATH}"
    elif [ -f "../backend/bontolink-ontology/target/${JAR_NAME}" ]; then
        info "从本地构建目录复制 ${JAR_NAME} ..."
        cp "../backend/bontolink-ontology/target/${JAR_NAME}" "${JAR_PATH}"
    else
        error "找不到 ${JAR_NAME}，请先构建：mvnw -DskipTests clean package -pl bontolink-ontology -am"
    fi
    chmod 644 "${JAR_PATH}"
    info "${JAR_NAME} 已就绪: ${JAR_PATH}"
}

# ========== 设置 systemd 服务 ==========
setup_service() {
    info "注册 systemd 服务..."
    cat > /etc/systemd/system/${APP_NAME}.service <<UNIT
[Unit]
Description=BOntoLink Ontology Service (Jena TDB2 + pgvector)
After=network.target postgresql.service redis.service
Wants=postgresql.service redis.service

[Service]
Type=simple
User=root
WorkingDirectory=${INSTALL_DIR}
Environment="JAVA_HOME=${JAVA_HOME}"
Environment="OPENAI_API_KEY=${OPENAI_API_KEY:-}"
ExecStart=${JAVA_HOME}/bin/java ${JAVA_OPTS} \
    -Dspring.profiles.active=remote \
    -Dspring.config.additional-location=${CONFIG_DIR}/application-remote.yml \
    -jar ${JAR_PATH}
ExecStop=/bin/kill -15 \$MAINPID
SuccessExitStatus=143
Restart=always
RestartSec=10
StandardOutput=append:${LOG_DIR}/stdout.log
StandardError=append:${LOG_DIR}/stderr.log

[Install]
WantedBy=multi-user.target
UNIT
    systemctl daemon-reload
    info "systemd 服务已注册: ${APP_NAME}"
}

# ========== 启动/停止/状态 ==========
start_service() {
    info "启动 ${APP_NAME} ..."
    systemctl enable ${APP_NAME} 2>/dev/null || true
    systemctl start ${APP_NAME}
    # 等待就绪
    local retries=30
    for i in $(seq 1 $retries); do
        if curl -sf "http://127.0.0.1:${APP_PORT}/bontolink-ontology/api/ontology/health" >/dev/null 2>&1; then
            info "服务已就绪! (耗时 ${i}s)"
            return 0
        fi
        sleep 1
    done
    warn "服务启动超时（${retries}s），请检查日志: journalctl -u ${APP_NAME} -f"
}

stop_service() {
    info "停止 ${APP_NAME} ..."
    systemctl stop ${APP_NAME} || true
    info "服务已停止"
}

service_status() {
    if systemctl is-active --quiet ${APP_NAME}; then
        echo -e "${GREEN}●${NC} ${APP_NAME} 运行中"
        curl -sf "http://127.0.0.1:${APP_PORT}/bontolink-ontology/api/ontology/health" 2>/dev/null \
            && echo "" || echo "健康检查失败"
    else
        echo -e "${RED}●${NC} ${APP_NAME} 未运行"
    fi
}

show_logs() {
    tail -f "${LOG_DIR}/ontology.log" 2>/dev/null \
        || journalctl -u ${APP_NAME} -f
}

# ========== 重建索引 ==========
rebuild_index() {
    local ns_code=""
    while [[ $# -gt 0 ]]; do
        case $1 in
            --ns) ns_code="$2"; shift 2 ;;
            *) shift ;;
        esac
    done

    if [ -n "$ns_code" ]; then
        info "重建 namespace: ${ns_code} ..."
        curl -X POST "http://127.0.0.1:${APP_PORT}/bontolink-ontology/api/ontology/rebuild?nsCode=${ns_code}"
    else
        info "全量重建（后台异步）..."
        curl -X POST "http://127.0.0.1:${APP_PORT}/bontolink-ontology/api/ontology/rebuild"
    fi
    echo ""
}

# ========== 入口 ==========
main() {
    case "${1:-help}" in
        install)
            precheck
            setup_dirs
            generate_config
            fetch_jar
            setup_service
            start_service
            ;;
        upgrade)
            fetch_jar
            restart_service
            info "升级完成"
            ;;
        start)   start_service ;;
        stop)    stop_service ;;
        restart) stop_service; sleep 2; start_service ;;
        status)  service_status ;;
        logs)    show_logs ;;
        rebuild) rebuild_index "${@:2}" ;;
        *)
            echo "用法: $0 {install|upgrade|start|stop|restart|status|logs|rebuild}"
            echo ""
            echo "   install  首次安装（目录 + 配置 + jar + 服务 + 启动）"
            echo "   upgrade  替换 jar + 重启"
            echo "   start    启动服务"
            echo "   stop     停止服务"
            echo "   restart  重启服务"
            echo "   status   查看运行状态"
            echo "   logs     查看实时日志"
            echo "   rebuild  重建索引 (--ns w_wtr 指定 namespace)"
            ;;
    esac
}

main "$@"
