#!/usr/bin/env bash
# ============================================================
# TDB2 目录结构初始化脚本
# 在远程数据服务器上创建 Jena TDB2 的 namespace 目录结构
# 用法：
#   ./setup-tdb-dirs.sh                    # 按 prompts 输入
#   ./setup-tdb-dirs.sh /data/bontolink/tdb2   # 指定根目录
#   ./setup-tdb-dirs.sh /data/bontolink/tdb2 w_wtr fin_acc   # 预建 namespace
# ============================================================
set -euo pipefail

TDB2_ROOT="${1:-/data/bontolink/tdb2}"
NAMESPACES=("${@:2}")

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; NC='\033[0m'
info()  { echo -e "${GREEN}[INFO]${NC} $1"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }

# 如果没传 namespace 列表，交互式输入
if [ ${#NAMESPACES[@]} -eq 0 ]; then
    echo "请输入 namespace 代码（用空格分隔，如: w_wtr fin_acc gen_common）:"
    read -ra NAMESPACES
fi

# 创建根目录
mkdir -p "${TDB2_ROOT}"
info "TDB2 根目录: ${TDB2_ROOT}"

# 创建每个 namespace 的子目录
for ns in "${NAMESPACES[@]}"; do
    [ -z "$ns" ] && continue
    NS_DIR="${TDB2_ROOT}/namespaces/${ns}"
    mkdir -p "${NS_DIR}"
    # 初始化版本文件（记录 jena_version）
    echo "0" > "${NS_DIR}/.jena_version"
    info "  ├─ ${ns}/  (version=0, 待首次同步)"
done

# 创建日志目录
mkdir -p "${TDB2_ROOT}/logs"

# 设置权限
chmod -R 755 "${TDB2_ROOT}"

# 生成目录树
info ""
info "目录结构:"
if command -v tree >/dev/null 2>&1; then
    tree "${TDB2_ROOT}"
else
    find "${TDB2_ROOT}" -type d | sort | head -30
fi

info ""
info "完成！首次启动 ontology 服务后，各 namespace 会在查询时自动构建索引。"
info "手动触发全量构建:"
echo "  curl -X POST http://localhost:8089/bontolink-ontology/api/ontology/rebuild"
info "手动触发指定 namespace:"
echo "  curl -X POST http://localhost:8089/bontolink-ontology/api/ontology/rebuild?nsCode=w_wtr"
