# 📈 Scala Quant — 实时股票分析系统
 
基于 **Scala + React** 的全栈量化股票分析平台，支持实时行情采集、技术指标计算、异常检测与交互式终端看板。
 
---
 
## 技术栈
 
| 层级 | 技术 |
|---|---|
| 后端 | Scala、Akka HTTP、Apache Kafka |
| 存储 | MySQL 8、Redis |
| 前端 | React 18 + TypeScript + Vite、ECharts、Tailwind CSS |
 
---
 
## 系统架构
 
```
行情 API (东方财富 / 新浪)
    │
    ▼
FetcherMain ──► Kafka ──► EnhancedConsumerMain
                               │ 指标计算 + 异常检测
                    ┌──────────┴──────────┐
                  MySQL               Redis
                    └──────────┬──────────┘
                           ApiServer (:8080)
                               │
                          React 前端 (:5173)
```
 
---
 
## 快速开始
 
**环境要求：** JDK 11+、SBT、Kafka、MySQL 8、Redis、Node.js 18+
 
**1. 修改连接配置**（默认地址 `192.168.202.130`，按需改为自己的环境）
 
**2. 启动后端服务**
 
```bash
# 数据采集（拉取历史 K 线 + 实时推送至 Kafka）
sbt "runMain com.stock.fetcher.FetcherMain"
 
# 数据处理（消费 Kafka → 计算指标 → 异常检测）
sbt "runMain com.stock.processor.EnhancedConsumerMain"
 
# REST API 服务
sbt "runMain com.stock.api.ApiServer"
```
 
**3. 历史指标回填（首次运行）**
 
```bash
sbt "runMain com.stock.processor.IndicatorBackfill"
```
 
**4. 启动前端**
 
```bash
npm install && npm run dev
```
 
---
 
## API 接口
 
| 接口 | 说明 |
|---|---|
| `GET /api/stocks` | 股票列表 |
| `GET /api/realtime/{code}` | 实时行情 |
| `GET /api/kline/{code}?limit=60` | K 线数据 |
| `GET /api/indicators/{code}/latest` | 最新技术指标 |
| `GET /api/anomalies?stockCode={code}` | 异常告警记录 |
 
股票代码格式：`sh600519`（沪）/ `sz000001`（深）
 
---
 
## 主要功能
 
- **技术指标**：MACD、KDJ、RSI(6/12/24)、MA(5/10/20/60)、布林带，至少需要 60 条 K 线
- **异常检测**：成交量异常放大、价格跳空、涨跌停、高波动率、KDJ/RSI 超买超卖
- **前端看板**：K 线图（含均线、成交量）、实时价格、指标卡片、告警面板、多股概览网格
 
---
 
## License
 
MIT
