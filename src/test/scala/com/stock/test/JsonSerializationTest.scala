package com.stock.test

import com.stock.model._
import com.stock.model.StockJsonProtocol._
import spray.json._

import java.time.{LocalDate, LocalDateTime}

object JsonSerializationTest extends App {

  println("=" * 60)
  println("测试JSON序列化")
  println("=" * 60)

  // 测试Stock
  val stock = Stock("sh600000", "浦发银行", "sh", Some("银行"))
  val stockJson = stock.toJson.prettyPrint
  println("\n1. Stock JSON:")
  println(stockJson)
  val stockParsed = stockJson.parseJson.convertTo[Stock]
  println(s"   解析结果: $stockParsed")
  assert(stock == stockParsed, "Stock序列化/反序列化失败")
  println("   ✅ Stock序列化测试通过")

  // 测试RealtimeQuote
  val quote = RealtimeQuote(
    stockCode = "sh600000",
    timestamp = System.currentTimeMillis(),
    currentPrice = 10.50,
    openPrice = 10.30,
    highPrice = 10.60,
    lowPrice = 10.20,
    closePrice = 10.40,
    volume = 1000000L,
    amount = 10500000.0,
    bidPrice1 = 10.49,
    bidVolume1 = 5000L,
    askPrice1 = 10.51,
    askVolume1 = 4500L
  )
  val quoteJson = quote.toJson.prettyPrint
  println("\n2. RealtimeQuote JSON:")
  println(quoteJson)
  val quoteParsed = quoteJson.parseJson.convertTo[RealtimeQuote]
  assert(quote == quoteParsed, "RealtimeQuote序列化/反序列化失败")
  println("   ✅ RealtimeQuote序列化测试通过")

  // 测试KLineData
  val kline = KLineData(
    stockCode = "sh600000",
    tradeDate = LocalDate.now(),
    openPrice = 10.30,
    highPrice = 10.60,
    lowPrice = 10.20,
    closePrice = 10.50,
    volume = 1000000L,
    amount = Some(10500000.0)
  )
  val klineJson = kline.toJson.prettyPrint
  println("\n3. KLineData JSON:")
  println(klineJson)
  val klineParsed = klineJson.parseJson.convertTo[KLineData]
  assert(kline == klineParsed, "KLineData序列化/反序列化失败")
  println("   ✅ KLineData序列化测试通过")

  // 测试TechnicalIndicators
  val indicators = TechnicalIndicators(
    stockCode = "sh600000",
    tradeDate = LocalDate.now(),
    macd = Some(0.15),
    macdSignal = Some(0.12),
    macdHist = Some(0.03),
    kdjK = Some(75.5),
    kdjD = Some(70.2),
    kdjJ = Some(85.1),
    rsi6 = Some(65.0),
    rsi12 = Some(60.0),
    rsi24 = Some(55.0),
    ma5 = Some(10.45),
    ma10 = Some(10.40),
    ma20 = Some(10.35),
    ma60 = Some(10.20)
  )
  val indicatorsJson = indicators.toJson.prettyPrint
  println("\n4. TechnicalIndicators JSON:")
  println(indicatorsJson)
  val indicatorsParsed = indicatorsJson.parseJson.convertTo[TechnicalIndicators]
  assert(indicators == indicatorsParsed, "TechnicalIndicators序列化/反序列化失败")
  println("   ✅ TechnicalIndicators序列化测试通过")

  // 测试AnomalyRecord
  val anomaly = AnomalyRecord(
    id = Some(1L),
    stockCode = "sh600000",
    anomalyType = "VOLUME_SPIKE",
    anomalyTime = LocalDateTime.now(),
    severity = "high",
    description = Some("成交量异常放大"),
    indicators = Some("""{"volume_ratio": 3.5}""")
  )
  val anomalyJson = anomaly.toJson.prettyPrint
  println("\n5. AnomalyRecord JSON:")
  println(anomalyJson)
  val anomalyParsed = anomalyJson.parseJson.convertTo[AnomalyRecord]
  println("   ✅ AnomalyRecord序列化测试通过")

  println("\n" + "=" * 60)
  println("🎉 所有JSON序列化测试通过!")
  println("=" * 60)
}