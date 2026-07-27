# Ontology 服务远程部署

## 部署架构

```
┌──────────────────────┐     ┌──────────────────────────┐
│  应用服务器            │     │  数据服务器（远程）        │
│  (admin 8088)        │     │  (ontology 8089)         │
│                      │     │                          │
│  bontolink-admin     │     │  bontolink-ontology      │
│  + Redis             │     │  + Jena TDB2 (嵌入式)    │
│                      │     │  + PostgreSQL + pgvector │
└──────────────────────┘     └──────────────────────────┘
```

## 部署步骤

### 1. 数据服务器上执行

```bash
# 1a. 安装 PostgreSQL + pgvector
#   详见 setup-pgvector.sql（在已经存在的 PostgreSQL 里执行）

# 1b. 创建 TDB2 目录结构
chmod +x setup-tdb-dirs.sh
./setup-tdb-dirs.sh /data/bontolink/tdb2 w_wtr fin_acc gen_common

# 1c. 安装 JDK 21（如果还没装）
tar -xzf jdk-21_linux-x64_bin.tar.gz -C /usr/lib/jvm/
export JAVA_HOME=/usr/lib/jvm/jdk-21
```

### 2. 构建并推送

```bash
# 在开发机上构建
cd backend
./mvnw -DskipTests clean package -pl bontolink-ontology -am

# 复制 jar 到部署目录（或放到远程服务器上）
cp bontolink-ontology/target/bontolink-ontology-1.0.0.jar deploy/ontology/
```

### 3. 执行部署

```bash
# 在远程数据服务器上首次安装
cd /opt/bontolink/ontology
chmod +x deploy-ontology.sh
./deploy-ontology.sh install

# 后续升级
./deploy-ontology.sh upgrade

# 管理
./deploy-ontology.sh status
./deploy-ontology.sh logs
```

### 4. 验证

```bash
curl http://localhost:8089/bontolink-ontology/api/ontology/health
curl http://localhost:8089/bontolink-ontology/api/ontology/stats
```

## 启动方式

部署后，ontology 服务会作为 systemd 服务自动管理：

```bash
systemctl status bontolink-ontology
journalctl -u bontolink-ontology -f    # 查看实时日志
```

## 配置文件

部署脚本自动生成 `config/application-remote.yml`，关键配置项：

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `bontolink.ontology.storage-mode` | Jena 存储模式 | `tdb2` |
| `bontolink.ontology.tdb2-location` | TDB2 数据路径 | `/data/bontolink/tdb2` |
| `bontolink.ontology.reasoner-type` | 推理器类型 | `owl-micro` |
| `bontolink.fusion.keyword-only-threshold` | Keyword 高分短路阈值 | 0.9 |
| `bontolink.fusion.ask-timeout-ms` | 问答超时 | 5000 |
| `spring.datasource.url` | PostgreSQL 连接串 | `jdbc:postgresql://127.0.0.1:5432/bontolink` |
| `bontolink.embedding.provider` | Embedding 提供方 | `openai` |

## Nginx 反向代理

如果通过 Nginx 统一入口：

```nginx
# /etc/nginx/conf.d/bontolink.conf
server {
    listen 80;
    server_name api.bontolink.com;

    # admin 服务
    location /api/ {
        proxy_pass http://127.0.0.1:8088/bontolink/;
    }

    # ontology 服务
    location /api/ontology/ {
        proxy_pass http://127.0.0.1:8089/bontolink-ontology/api/ontology/;
    }

    # 前端静态文件
    location / {
        root /opt/bontolink/frontend/dist;
        try_files $uri $uri/ /index.html;
    }
}
```

## 文件清单

```
deploy/ontology/
├── README.md                  # 本文件
├── deploy-ontology.sh         # 主部署脚本（install/upgrade/start/stop/status/logs/rebuild）
├── setup-tdb-dirs.sh          # TDB2 目录初始化
├── setup-pgvector.sql         # PostgreSQL pgvector 建表
└── ontology.service           # systemd 服务单元（部署脚本自动生成）
```
