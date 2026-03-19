# 志愿推荐项目

## 环境要求

- Java 17
- Node.js 24+
- npm 11+
- MySQL 8.x

## 项目结构

- `src/main/java`：Spring Boot 后端
- `src/main/resources/static`：前端构建产物
- `frontend`：独立 Vue + Vite 前端工程
- `sql`：数据库初始化脚本

## 数据库初始化

默认数据库名是 `college_recommendation`，默认账号配置见 `src/main/resources/application.yml`。

直接执行：

```bat
init-db.bat
```

如果需要自定义连接参数：

```bat
init-db.bat localhost 3306 root 1234 college_recommendation
```

检查数据库连接：

```bat
check-db.bat localhost 3306 root 1234
```

## 后端启动

项目已包含 Maven Wrapper，不需要本机单独安装 Maven。

命令行启动：

```bat
.\mvnw.cmd spring-boot:run
```

默认访问地址：

```text
http://localhost:8080
```

## 前端开发启动

前端已拆分到独立工程，开发时建议前后端分开启动。

安装依赖：

```bat
cd frontend
npm install
```

启动开发服务器：

```bat
cd frontend
npm run dev
```

默认前端地址：

```text
http://localhost:5173
```

说明：

- Vite 已代理 `/api` 到 `http://localhost:8080`
- 所以前端开发时，后端需要先启动

## 前端构建

构建命令：

```bat
cd frontend
npm run build
```

构建产物会直接输出到：

```text
src/main/resources/static
```

构建完成后，启动 Spring Boot 即可由后端直接托管前端页面。

## 常用开发流程

### 本地开发

1. 初始化 MySQL 数据库
2. 启动后端 `.\mvnw.cmd spring-boot:run`
3. 启动前端 `cd frontend && npm run dev`
4. 打开 `http://localhost:5173`

### 本地联调/打包预览

1. 执行 `cd frontend && npm run build`
2. 启动后端
3. 打开 `http://localhost:8080`

## 测试

后端测试：

```bat
.\mvnw.cmd test
```

当前仓库没有单独配置前端测试。
