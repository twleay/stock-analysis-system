package com.stock.model

import spray.json.{DefaultJsonProtocol, RootJsonFormat}
import java.time.{LocalDate, LocalDateTime}

/**
 * 股票基本信息
 */
case class Stock(
                  stockCode: String,
                  stockName: String,
                  market: String,      // sh/sz
                  sector: Option[String] = None
                )

/**
 * 实时行情数据
 */
case class RealtimeQuote(
                          stockCode: String,
                          timestamp: Long,
                          currentPrice: Double,
                          openPrice: Double,
                          highPrice: Double,
                          lowPrice: Double,
                          closePrice: Double,   // 昨收价
                          volume: Long,         // 成交量
                          amount: Double,       // 成交额
                          bidPrice1: Double,    // 买一价
                          bidVolume1: Long,     // 买一量
                          askPrice1: Double,    // 卖一价
                          askVolume1: Long      // 卖一量
                        )

/**
 * K线数据
 */
case class KLineData(
                      stockCode: String,
                      tradeDate: LocalDate,
                      openPrice: Double,
                      highPrice: Double,
                      lowPrice: Double,
                      closePrice: Double,
                      volume: Long,
                      amount: Option[Double] = None
                    )

/**
 * 技术指标
 */
case class TechnicalIndicators(
                                stockCode: String,
                                tradeDate: LocalDate,
                                // MACD指标
                                macd: Option[Double] = None,
                                macdSignal: Option[Double] = None,
                                macdHist: Option[Double] = None,
                                // KDJ指标
                                kdjK: Option[Double] = None,
                                kdjD: Option[Double] = None,
                                kdjJ: Option[Double] = None,
                                // RSI指标
                                rsi6: Option[Double] = None,
                                rsi12: Option[Double] = None,
                                rsi24: Option[Double] = None,
                                // 均线
                                ma5: Option[Double] = None,
                                ma10: Option[Double] = None,
                                ma20: Option[Double] = None,
                                ma60: Option[Double] = None
                              )

/**
 * 交易策略
 */
case class TradingStrategy(
                            id: Option[Int] = None,
                            strategyName: String,
                            strategyType: String,
                            description: Option[String] = None,
                            parameters: String,    // JSON格式的参数
                            isActive: Boolean = true
                          )

/**
 * 回测结果
 */
case class BacktestResult(
                           id: Option[Int] = None,
                           strategyId: Int,
                           stockCode: String,
                           startDate: LocalDate,
                           endDate: LocalDate,
                           initialCapital: Double,
                           finalCapital: Double,
                           totalReturn: Double,
                           annualReturn: Option[Double] = None,
                           maxDrawdown: Option[Double] = None,
                           sharpeRatio: Option[Double] = None,
                           winRate: Option[Double] = None,
                           totalTrades: Option[Int] = None
                         )

/**
 * 异常记录
 */
case class AnomalyRecord(
                          id: Option[Long] = None,
                          stockCode: String,
                          anomalyType: String,
                          anomalyTime: LocalDateTime,
                          severity: String,      // low/medium/high
                          description: Option[String] = None,
                          indicators: Option[String] = None  // JSON格式
                        )

/**
 * JSON序列化支持
 */
object StockJsonProtocol extends DefaultJsonProtocol {
  // 自定义LocalDate和LocalDateTime的格式化
  implicit object LocalDateFormat extends RootJsonFormat[LocalDate] {
    def write(date: LocalDate): spray.json.JsValue =
      spray.json.JsString(date.toString)

    def read(value: spray.json.JsValue): LocalDate = value match {
      case spray.json.JsString(s) => LocalDate.parse(s)
      case _ => throw new RuntimeException(s"Cannot parse LocalDate from $value")
    }
  }

  implicit object LocalDateTimeFormat extends RootJsonFormat[LocalDateTime] {
    def write(dt: LocalDateTime): spray.json.JsValue =
      spray.json.JsString(dt.toString)

    def read(value: spray.json.JsValue): LocalDateTime = value match {
      case spray.json.JsString(s) => LocalDateTime.parse(s)
      case _ => throw new RuntimeException(s"Cannot parse LocalDateTime from $value")
    }
  }

  // 基础数据模型
  implicit val stockFormat: RootJsonFormat[Stock] = jsonFormat4(Stock)
  implicit val realtimeQuoteFormat: RootJsonFormat[RealtimeQuote] = jsonFormat13(RealtimeQuote)
  implicit val klineDataFormat: RootJsonFormat[KLineData] = jsonFormat8(KLineData)

  // 技术指标 - 15个字段，所以用jsonFormat15
  implicit val technicalIndicatorsFormat: RootJsonFormat[TechnicalIndicators] = jsonFormat15(TechnicalIndicators)

  implicit val tradingStrategyFormat: RootJsonFormat[TradingStrategy] = jsonFormat6(TradingStrategy)
  implicit val backtestResultFormat: RootJsonFormat[BacktestResult] = jsonFormat13(BacktestResult)
  implicit val anomalyRecordFormat: RootJsonFormat[AnomalyRecord] = jsonFormat7(AnomalyRecord)
}