package com.stock.test

import com.stock.processor.AnomalyDetector
import com.stock.model.{RealtimeQuote, KLineData}
import java.time.LocalDate

object AnomalyDetectorTest extends App {

  println("=" * 60)
  println("异常检测测试")
  println("=" * 60)

  // 准备测试数据
  val stockCode = "sh600000"

  // 模拟正常K线数据
  val normalKlines = (1 to 20).map { i =>
    KLineData(
      stockCode = stockCode,
      tradeDate = LocalDate.now().minusDays(21 - i),
      openPrice = 10.0,
      highPrice = 10.2,
      lowPrice = 9.8,
      closePrice = 10.0,
      volume = 1000000L,
      amount = Some(10000000.0)
    )
  }.toList

  // 测试1：成交量异常
  println("\n[测试1] 成交量异常检测")
  val volumeSpike = RealtimeQuote(
    stockCode = stockCode,
    timestamp = System.currentTimeMillis(),
    currentPrice = 10.5,
    openPrice = 10.0,
    highPrice = 10.6,
    lowPrice = 9.9,
    closePrice = 10.0,
    volume = 5000000L,  // 5倍于正常量
    amount = 52000000.0,
    bidPrice1 = 10.49,
    bidVolume1 = 10000L,
    askPrice1 = 10.51,
    askVolume1 = 10000L
  )

  val volumeAnomaly = AnomalyDetector.detectVolumeAnomaly(volumeSpike, normalKlines)
  volumeAnomaly match {
    case Some(anomaly) =>
      println(s"✅ 检测到异常: ${anomaly.anomalyType}")
      println(s"   严重程度: ${anomaly.severity}")
      println(s"   描述: ${anomaly.description.getOrElse("")}")
    case None =>
      println("❌ 未检测到异常")
  }

  // 测试2：价格跳空
  println("\n[测试2] 价格跳空检测")
  val priceJump = RealtimeQuote(
    stockCode = stockCode,
    timestamp = System.currentTimeMillis(),
    currentPrice = 11.0,  // 比昨收高10%
    openPrice = 11.0,
    highPrice = 11.2,
    lowPrice = 10.9,
    closePrice = 10.0,
    volume = 1000000L,
    amount = 11000000.0,
    bidPrice1 = 10.99,
    bidVolume1 = 10000L,
    askPrice1 = 11.01,
    askVolume1 = 10000L
  )

  val jumpAnomaly = AnomalyDetector.detectPriceJump(priceJump, 10.0)
  jumpAnomaly match {
    case Some(anomaly) =>
      println(s"✅ 检测到异常: ${anomaly.anomalyType}")
      println(s"   严重程度: ${anomaly.severity}")
      println(s"   描述: ${anomaly.description.getOrElse("")}")
    case None =>
      println("❌ 未检测到异常")
  }

  // 测试3：涨停检测
  println("\n[测试3] 涨停检测")
  val limitUp = RealtimeQuote(
    stockCode = stockCode,
    timestamp = System.currentTimeMillis(),
    currentPrice = 11.0,  // 涨停10%
    openPrice = 10.0,
    highPrice = 11.0,
    lowPrice = 10.0,
    closePrice = 10.0,
    volume = 2000000L,
    amount = 21000000.0,
    bidPrice1 = 11.0,
    bidVolume1 = 100000L,
    askPrice1 = 11.0,
    askVolume1 = 0L
  )

  val limitAnomaly = AnomalyDetector.detectLimitUpDown(limitUp)
  limitAnomaly match {
    case Some(anomaly) =>
      println(s"✅ 检测到异常: ${anomaly.anomalyType}")
      println(s"   严重程度: ${anomaly.severity}")
      println(s"   描述: ${anomaly.description.getOrElse("")}")
    case None =>
      println("❌ 未检测到异常")
  }

  // 测试4：技术指标异常
  println("\n[测试4] 技术指标异常检测")
  val indicatorAnomalies = AnomalyDetector.detectIndicatorAnomaly(
    stockCode = stockCode,
    kdj = (85.0, 82.0, 91.0),  // KDJ超买
    rsi = 75.0  // RSI超买
  )

  if (indicatorAnomalies.nonEmpty) {
    println(s"✅ 检测到 ${indicatorAnomalies.size} 个指标异常:")
    indicatorAnomalies.foreach { anomaly =>
      println(s"   - ${anomaly.anomalyType}: ${anomaly.description.getOrElse("")}")
    }
  } else {
    println("❌ 未检测到异常")
  }

  // 测试5：综合检测
  println("\n[测试5] 综合异常检测")
  val allAnomalies = AnomalyDetector.detectAll(volumeSpike, normalKlines)
  if (allAnomalies.nonEmpty) {
    println(s"✅ 综合检测发现 ${allAnomalies.size} 个异常:")
    allAnomalies.foreach { anomaly =>
      println(s"   - [${anomaly.severity}] ${anomaly.anomalyType}: ${anomaly.description.getOrElse("")}")
    }
  } else {
    println("未检测到异常")
  }

  println("\n" + "=" * 60)
  println("🎉 异常检测测试完成！")
  println("=" * 60)
}