package com.stock.connector
import slick.jdbc.MySQLProfile.api._
import com.stock.model._
import java.time.{LocalDate, LocalDateTime}
import slick.lifted.ProvenShape
object DatabaseTables {

  // 自定义类型映射
  implicit val localDateColumnType: BaseColumnType[LocalDate] =
    MappedColumnType.base[LocalDate, java.sql.Date](
      ld => java.sql.Date.valueOf(ld),
      d => d.toLocalDate
    )

  implicit val localDateTimeColumnType: BaseColumnType[LocalDateTime] =
    MappedColumnType.base[LocalDateTime, java.sql.Timestamp](
      ldt => java.sql.Timestamp.valueOf(ldt),
      ts => ts.toLocalDateTime
    )

  // 表定义保持不变...


  /**
   * 股票基本信息表
   */
  class StocksTable(tag: Tag) extends Table[Stock](tag, "stocks") {
    def stockCode = column[String]("stock_code", O.PrimaryKey)
    def stockName = column[String]("stock_name")
    def market = column[String]("market")
    def sector = column[Option[String]]("sector")

    def * : ProvenShape[Stock] = (stockCode, stockName, market, sector).mapTo[Stock]
  }

  val stocks = TableQuery[StocksTable]

  /**
   * K线数据表
   */
  class KLineDataTable(tag: Tag) extends Table[KLineData](tag, "kline_data") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def stockCode = column[String]("stock_code")
    def tradeDate = column[LocalDate]("trade_date")
    def openPrice = column[Double]("open_price")
    def highPrice = column[Double]("high_price")
    def lowPrice = column[Double]("low_price")
    def closePrice = column[Double]("close_price")
    def volume = column[Long]("volume")
    def amount = column[Option[Double]]("amount")

    def * : ProvenShape[KLineData] = (stockCode, tradeDate, openPrice, highPrice, lowPrice, closePrice, volume, amount).mapTo[KLineData]

    def idx = index("idx_stock_date", (stockCode, tradeDate), unique = true)
  }

  val klineData = TableQuery[KLineDataTable]

  /**
   * 技术指标表
   */
  class TechnicalIndicatorsTable(tag: Tag) extends Table[TechnicalIndicators](tag, "technical_indicators") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def stockCode = column[String]("stock_code")
    def tradeDate = column[LocalDate]("trade_date")
    def macd = column[Option[Double]]("macd")
    def macdSignal = column[Option[Double]]("macd_signal")
    def macdHist = column[Option[Double]]("macd_hist")
    def kdjK = column[Option[Double]]("kdj_k")
    def kdjD = column[Option[Double]]("kdj_d")
    def kdjJ = column[Option[Double]]("kdj_j")
    def rsi6 = column[Option[Double]]("rsi_6")
    def rsi12 = column[Option[Double]]("rsi_12")
    def rsi24 = column[Option[Double]]("rsi_24")
    def ma5 = column[Option[Double]]("ma5")
    def ma10 = column[Option[Double]]("ma10")
    def ma20 = column[Option[Double]]("ma20")
    def ma60 = column[Option[Double]]("ma60")

    def * : ProvenShape[TechnicalIndicators] = (stockCode, tradeDate, macd, macdSignal, macdHist,
      kdjK, kdjD, kdjJ, rsi6, rsi12, rsi24, ma5, ma10, ma20, ma60).mapTo[TechnicalIndicators]

    def idx = index("idx_stock_date_tech", (stockCode, tradeDate), unique = true)
  }

  val technicalIndicators = TableQuery[TechnicalIndicatorsTable]

  /**
   * 交易策略表
   */
  class TradingStrategiesTable(tag: Tag) extends Table[TradingStrategy](tag, "trading_strategies") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def strategyName = column[String]("strategy_name")
    def strategyType = column[String]("strategy_type")
    def description = column[Option[String]]("description")
    def parameters = column[String]("parameters")
    def isActive = column[Boolean]("is_active")

    def * : ProvenShape[TradingStrategy] = (id.?, strategyName, strategyType, description, parameters, isActive).mapTo[TradingStrategy]
  }

  val tradingStrategies = TableQuery[TradingStrategiesTable]

  /**
   * 回测结果表
   */
  class BacktestResultsTable(tag: Tag) extends Table[BacktestResult](tag, "backtest_results") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def strategyId = column[Int]("strategy_id")
    def stockCode = column[String]("stock_code")
    def startDate = column[LocalDate]("start_date")
    def endDate = column[LocalDate]("end_date")
    def initialCapital = column[Double]("initial_capital")
    def finalCapital = column[Double]("final_capital")
    def totalReturn = column[Double]("total_return")
    def annualReturn = column[Option[Double]]("annual_return")
    def maxDrawdown = column[Option[Double]]("max_drawdown")
    def sharpeRatio = column[Option[Double]]("sharpe_ratio")
    def winRate = column[Option[Double]]("win_rate")
    def totalTrades = column[Option[Int]]("total_trades")

    def * : ProvenShape[BacktestResult] = (id.?, strategyId, stockCode, startDate, endDate,
      initialCapital, finalCapital, totalReturn, annualReturn, maxDrawdown, sharpeRatio, winRate, totalTrades).mapTo[BacktestResult]
  }

  val backtestResults = TableQuery[BacktestResultsTable]

  /**
   * 异常记录表
   */
  class AnomalyRecordsTable(tag: Tag) extends Table[AnomalyRecord](tag, "anomaly_records") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def stockCode = column[String]("stock_code")
    def anomalyType = column[String]("anomaly_type")
    def anomalyTime = column[LocalDateTime]("anomaly_time")
    def severity = column[String]("severity")
    def description = column[Option[String]]("description")
    def indicators = column[Option[String]]("indicators")

    def * : ProvenShape[AnomalyRecord] = (id.?, stockCode, anomalyType, anomalyTime, severity, description, indicators).mapTo[AnomalyRecord]
  }

  val anomalyRecords = TableQuery[AnomalyRecordsTable]
}