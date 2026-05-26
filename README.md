# 北京交通大学就餐仿真系统

**S005 小组 · 黄惠 · 赖特 · 姚沚汐**  
**日期：2026年3月27日**

---

## 一、项目简介

本系统是北京交通大学经济管理学院综合实训课程的实训项目，通过 B/S 架构对校园食堂就餐全流程进行可视化仿真，帮助师生直观了解就餐高峰时段的座位占用、窗口排队等真实情况。

---

## 二、技术栈

| 层次 | 技术 |
|------|------|
| 前端 | HTML5 / CSS3 / JavaScript（原生，无框架依赖）|
| 后端 | Java 21 · Spring Boot 3.3.5 · MyBatis 3.0.3 |
| 数据库 | MySQL 8.0 |
| 构建工具 | Maven 3.x |
| 运行环境 | JDK 21+（已在 Java 26 上验证运行）|

---

## 三、项目结构

```
bjtu-canteen/
├── database/
│   └── init.sql                       # 数据库初始化脚本（建表 + 初始数据）
├── backend/                           # Spring Boot 后端
│   ├── pom.xml                        # Maven 依赖配置
│   └── src/main/
│       ├── java/com/bjtu/canteen/
│       │   ├── CanteenSimApplication.java    # 启动入口
│       │   ├── config/WebConfig.java         # CORS 跨域配置
│       │   ├── controller/
│       │   │   ├── SimController.java        # 仿真控制接口
│       │   │   └── CanteenController.java    # 窗口/菜品/评价接口
│       │   ├── model/                        # 数据模型（Seat、Food、SimSnapshot 等）
│       │   ├── mapper/                       # MyBatis 数据访问层
│       │   └── simulator/
│       │       ├── CanteenSimulator.java     # 核心仿真引擎
│       │       └── SimScheduler.java         # 定时驱动器（每秒推进仿真时钟）
│       └── resources/application.yml         # 应用配置
└── frontend/
    ├── index.html                     # 主页面（食堂平面图）
    ├── css/main.css                   # 样式
    └── js/
        ├── api.js                     # 后端 HTTP 通信层
        ├── sim.js                     # 仿真状态管理（含前端本地备用仿真）
        └── ui.js                      # UI 渲染层
```

---

## 四、快速启动

### 方式一：纯前端模式（无需安装任何环境）

直接用浏览器打开 `frontend/index.html`。

系统检测到后端不可用时，自动切换为**前端本地仿真模式**，所有仿真逻辑在浏览器内运行，无需后端和数据库。

---

### 方式二：完整后端模式（推荐）

#### 第一步：初始化数据库

在 MySQL 中执行初始化脚本：

```sql
source database/init.sql
```

修改 `backend/src/main/resources/application.yml` 中的数据库密码：

```yaml
spring:
  datasource:
    password: 你的MySQL密码
```

#### 第二步：编译打包

```bash
cd backend
mvn clean package -DskipTests
```

#### 第三步：启动后端

```bash
java -jar target/canteen-sim-1.0.0.jar
```

启动成功后终端输出：

```
Tomcat started on port 8888 (http) with context path '/api'
Started CanteenSimApplication in x.xxx seconds
```

#### 第四步：打开前端

用浏览器打开 `frontend/index.html`，页面右下角出现 **"✅ 已连接后端仿真服务"** 即表示成功。

> 后端接口地址：`http://localhost:8888/api`

---

## 五、功能说明

| 功能模块 | 描述 |
|----------|------|
| **实时仿真时钟** | 顶部显示仿真时间，速度可调（×1 ~ ×60）|
| **窗口排队可视化** | 7 个窗口排队人数实时展示，颜色区分拥挤程度（绿/黄/红）|
| **座位状态平面图** | 320 个座位（80桌×4座），绿=空座 / 黄=已占/排队 / 红=就餐中 |
| **手动占座** | 点击绿色空座即可手动占座 |
| **窗口菜品查看** | 点击任意窗口弹出菜品列表及价格 |
| **菜品评价** | 右侧面板提交评分与文字评论，数据存入数据库 |
| **仿真控制** | 支持开始 / 暂停 / 重置 / 调速 |

---

## 六、仿真规则

### 就餐时段

| 时段 | 时间 | 开放窗口 | 高峰人数 |
|------|------|----------|----------|
| 早餐 | 07:00 – 09:00 | 面条、包子馒头、清真（3个）| 约 100 人 |
| 午餐 | 11:00 – 13:00 | 全部（7个）| 约 300 人（接近满座）|
| 晚餐 | 17:00 – 19:00 | 全部（7个）| 约 250 人 |

### 人员流程

```
到达 → 先占座（reserved）→ 去窗口排队 → 打饭完成 → 回座就餐（dining）→ 离场（empty）
```

- **到达分布**：泊松分布，高峰期到达率更高
- **服务时间**：50 ~ 120 秒/人（各窗口不同）
- **就餐时长**：早餐 10~15 分钟，午/晚餐 20~30 分钟
- **非营业时段**：停止新人到达，现有排队人员继续服务直至队列清空

### 仿真速度

默认 ×60（1 秒现实时间 = 1 分钟仿真时间），可通过界面滑块调整。

---

## 七、后端 API 列表

| 方法 | 路径 | 说明 |
|------|------|------|
| GET  | `/api/sim/snapshot` | 获取仿真实时快照（前端每秒轮询）|
| POST | `/api/sim/start` | 启动仿真 |
| POST | `/api/sim/pause` | 暂停仿真 |
| POST | `/api/sim/reset` | 重置仿真 |
| POST | `/api/sim/speed` | 设置仿真速度，Body: `{"speed":60}` |
| POST | `/api/sim/seat/{id}/reserve` | 手动占座 |
| GET  | `/api/canteen/windows` | 获取全部窗口及菜品信息 |
| GET  | `/api/canteen/window/{id}` | 获取单个窗口详情 |
| POST | `/api/canteen/feedback` | 提交菜品评价 |
| GET  | `/api/canteen/feedback` | 获取评价列表 |

---

## 八、注意事项

1. **Java 版本**：编译目标为 Java 21 字节码，运行时需要 JDK 21 及以上（已在 JDK 26 测试通过）
2. **数据库字符集**：请确保 MySQL 数据库使用 `utf8mb4` 字符集
3. **端口占用**：后端默认占用 `8888` 端口，请确保未被其他程序占用
4. **前端跨域**：如果浏览器限制本地文件跨域，可使用 VS Code 的 Live Server 插件打开前端
