#!/usr/bin/env bash
# ============================================================
# Ontology 服务快速升级脚本
# 用法：
#   ./upgrade-ontology.sh bontolink-ontology-1.0.0.jar       # 本地 jar 升级
#   ./upgrade-ontology.sh user@build-server:/path/to/jar     # 远程拉取升级
#   ./upgrade-ontology.sh --rollback                         # 回滚到上个版本
# ============================================================
set -euo pipefail

APP_NAME="bontolink-ontology"
INSTALL_DIR="/opt/${APP_NAME}"
BACKUP_DIR="${INSTALL_DIR}/backup"
JAR_PATH="${INSTALL_DIR}/${APP_NAME}-1.0.0.jar"

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'
info()  { echo -e "${GREEN}[INFO]${NC} $1"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
error() { echo -e "${RED}[ERROR]${NC} $1"; exit 1; }

# 回滚
if [ "${1:-}" = "--rollback" ]; then
    LATEST_BACKUP=$(ls -t "${BACKUP_DIR}"/*.jar 2>/dev/null | head -1)
    if [ -z "$LATEST_BACKUP" ]; then
        error "没有找到备份文件，无法回滚"
    fi
    info "回滚到: ${LATEST_BACKUP}"
    systemctl stop "${APP_NAME}"
    cp "${LATEST_BACKUP}" "${JAR_PATH}"
    systemctl start "${APP_NAME}"
    info "回滚完成"
    exit 0
fi

# 获取新 jar
SOURCE="${1:-}"
if [ -z "$SOURCE" ]; then
    error "用法: $0 <jar 路径>  或  $0 --rollback"
fi

# 备份当前版本
mkdir -p "${BACKUP_DIR}"
if [ -f "${JAR_PATH}" ]; then
    BACKUP_FILE="${BACKUP_DIR}/${APP_NAME}-$(date +%Y%m%d_%H%M%S).jar"
    cp "${JAR_PATH}" "${BACKUP_FILE}"
    info "已备份当前版本: ${BACKUP_FILE}"
fi

# 复制新 jar
if [[ "$SOURCE" == *":"* ]]; then
    # 远程路径
    scp "$SOURCE" "${JAR_PATH}.tmp"
else
    # 本地路径
    cp "$SOURCE" "${JAR_PATH}.tmp"
fi
mv "${JAR_PATH}.tmp" "${JAR_PATH}"
info "新 jar 已就绪: ${JAR_PATH}"

# 重启
info "重启服务..."
systemctl restart "${APP_NAME}"

# 等待就绪
for i in $(seq 1 30); do
    if curl -sf "http://127.0.0.1:8089/bontolink-ontology/api/ontology/health" >/dev/null 2>&1; then
        info "服务升级完成! (耗时 ${i}s)"
        # 显示版本
        curl -sf "http://127.0.0.1:8089/bontolink-ontology/api/ontology/stats" | python3 -m json.tool 2>/dev/null || true
        exit 0
    fi
    sleep 1
done

warn "服务启动超时，请检查日志: journalctl -u ${APP_NAME} -f"
