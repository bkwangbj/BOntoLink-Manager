#!/bin/bash
# -------------------------------------------------------------------
# BOntoLink 后端启动脚本
# JAR:       /home/BOntoLink/bontolink-backend.jar
# 配置文件:   /home/BOntoLink/setting/
# -------------------------------------------------------------------

set -e

APP_HOME=/home/BOntoLink
JAR_FILE=${APP_HOME}/bontolink-backend.jar
CONFIG_DIR=${APP_HOME}/setting
LOG_DIR=${APP_HOME}/logs
PID_FILE=${APP_HOME}/bontolink.pid
LOG_FILE=${LOG_DIR}/bontolink.log

# JVM 参数
JAVA_OPTS="-Xms512m -Xmx2048m -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=${LOG_DIR}"

# 使用系统 Java，若需指定 JDK 可取消下面注释
# JAVA_HOME=/usr/local/jdk-21
# export JAVA_HOME

JAVA_CMD="${JAVA_HOME:+$JAVA_HOME/bin/}java"

usage() {
    echo "用法: $0 {start|stop|restart|status}"
    exit 1
}

# 检查是否已在运行
is_running() {
    if [ -f "$PID_FILE" ]; then
        local pid
        pid=$(cat "$PID_FILE")
        if kill -0 "$pid" 2>/dev/null; then
            return 0
        fi
    fi
    return 1
}

start() {
    if is_running; then
        echo "[BOntoLink] 服务已在运行中，PID: $(cat $PID_FILE)"
        return 1
    fi

    mkdir -p "$LOG_DIR"

    echo "[BOntoLink] 启动中..."

    cd "$APP_HOME"

    nohup $JAVA_CMD \
        $JAVA_OPTS \
        -jar "$JAR_FILE" \
        --spring.config.additional-location=file:${CONFIG_DIR}/ \
        >> "$LOG_FILE" 2>&1 &

    echo $! > "$PID_FILE"
    echo "[BOntoLink] 已启动，PID: $(cat $PID_FILE)"
    echo "[BOntoLink] 日志: $LOG_FILE"
}

stop() {
    if ! is_running; then
        echo "[BOntoLink] 服务未运行"
        rm -f "$PID_FILE"
        return 0
    fi

    local pid
    pid=$(cat "$PID_FILE")
    echo "[BOntoLink] 停止中... PID: $pid"

    kill "$pid"

    # 等待最多 30 秒
    local count=0
    while kill -0 "$pid" 2>/dev/null && [ $count -lt 30 ]; do
        sleep 1
        count=$((count + 1))
    done

    if kill -0 "$pid" 2>/dev/null; then
        echo "[BOntoLink] 强制终止..."
        kill -9 "$pid"
    fi

    rm -f "$PID_FILE"
    echo "[BOntoLink] 已停止"
}

restart() {
    stop
    sleep 2
    start
}

status() {
    if is_running; then
        local pid
        pid=$(cat "$PID_FILE")
        echo "[BOntoLink] 运行中 — PID: $pid"
        echo "[BOntoLink] 日志: $LOG_FILE"
    else
        echo "[BOntoLink] 未运行"
    fi
}

case "$1" in
    start)   start   ;;
    stop)    stop    ;;
    restart) restart ;;
    status)  status  ;;
    *)       usage   ;;
esac
