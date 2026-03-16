package com.stock.processor

import com.stock.model.{RealtimeQuote, KLineData, AnomalyRecord}
import java.time.LocalDateTime

/**
 * 异常检测器
 */
object AnomalyDetector {

  /**
   * 异常类型枚举
   */
  object AnomalyType {
    val VOLUME_SPIKE = "VOLUME_SPIKE"           // 成交量异常放大
    val PRICE_JUMP = "PRICE_JUMP"               // 价格跳空
    val VOLATILITY_HIGH = "VOLATILITY_HIGH"     // 波动率异常
    val LIMIT_UP = "LIMIT_UP"                   // 涨停
    val LIMIT_DOWN = "LIMIT_DOWN"               // 跌停
    val SUSPEND = "SUSPEND"                     // 停牌
    val UNUSUAL_PATTERN = "UNUSUAL_PATTERN"     // 异常模式
  }

  /**
   * 严重程度
   */
  object Severity {
    val LOW = "low"
    val MEDIUM = "medium"
    val HIGH = "high"
  }

  /**
   * 检测成交量异常
   */
  def detectVolumeAnomaly(
                           quote: RealtimeQuote,
                           recentKlines: List[KLineData],
                           threshold: Double = 3.0
                         ): Option[AnomalyRecord] = {

    if (recentKlines.isEmpty) return None

    // 计算平均成交量
    val avgVolume = recentKlines.map(_.volume).sum.toDouble / recentKlines.length

    // 如果当前成交量超过平均值的threshold倍
    if (quote.volume > avgVolume * threshold && avgVolume > 0) {
      val ratio = quote.volume.toDouble / avgVolume

      val severity = if (ratio > 5.0) {
        Severity.HIGH
      } else if (ratio > 4.0) {
        Severity.MEDIUM
      } else {
        Severity.LOW
      }

      Some(AnomalyRecord(
        id = None,
        stockCode = quote.stockCode,
        anomalyType = AnomalyType.VOLUME_SPIKE,
        anomalyTime = LocalDateTime.now(),
        severity = severity,
        description = Some(f"成交量异常放大 ${ratio}%.2f 倍，当前: ${quote.volume}%,d，平均: ${avgVolume.toLong}%,d"),
        indicators = Some(s"""{"volume":${quote.volume},"avg_volume":$avgVolume,"ratio":$ratio}""")
      ))
    } else {
      None
    }
  }

  /**
   * 检测价格跳空
   */
  def detectPriceJump(
                       quote: RealtimeQuote,
                       lastClose: Double,
                       threshold: Double = 0.05
                     ): Option[AnomalyRecord] = {

    if (lastClose <= 0) return None

    val changePercent = math.abs((quote.currentPrice - lastClose) / lastClose)

    if (changePercent > threshold) {
      val direction = if (quote.currentPrice > lastClose) "上涨" else "下跌"

      val severity = if (changePercent > 0.1) {
        Severity.HIGH
      } else {
        Severity.MEDIUM
      }

      Some(AnomalyRecord(
        id = None,
        stockCode = quote.stockCode,
        anomalyType = AnomalyType.PRICE_JUMP,
        anomalyTime = LocalDateTime.now(),
        severity = severity,
        description = Some(f"价格$direction 跳空 ${changePercent * 100}%.2f%%，当前价: ${quote.currentPrice}%.2f，昨收: $lastClose%.2f"),
        indicators = Some(s"""{"current_price":${quote.currentPrice},"last_close":$lastClose,"change_percent":$changePercent}""")
      ))
    } else {
      None
    }
  }

  /**
   * 检测涨跌停
   */
  def detectLimitUpDown(quote: RealtimeQuote): Option[AnomalyRecord] = {
    if (quote.closePrice <= 0) return None

    val changePercent = (quote.currentPrice - quote.closePrice) / quote.closePrice

    // A股涨跌停限制为10%（ST股为5%）
    val limitThreshold = 0.095  // 9.5%作为判断阈值

    if (changePercent >= limitThreshold) {
      Some(AnomalyRecord(
        id = None,
        stockCode = quote.stockCode,
        anomalyType = AnomalyType.LIMIT_UP,
        anomalyTime = LocalDateTime.now(),
        severity = Severity.HIGH,
        description = Some(f"涨停，涨幅: ${changePercent * 100}%.2f%%"),
        indicators = Some(s"""{"current_price":${quote.currentPrice},"change_percent":$changePercent}""")
      ))
    } else if (changePercent <= -limitThreshold) {
      Some(AnomalyRecord(
        id = None,
        stockCode = quote.stockCode,
        anomalyType = AnomalyType.LIMIT_DOWN,
        anomalyTime = LocalDateTime.now(),
        severity = Severity.HIGH,
        description = Some(f"跌停，跌幅: ${changePercent * 100}%.2f%%"),
        indicators = Some(s"""{"current_price":${quote.currentPrice},"change_percent":$changePercent}""")
      ))
    } else {
      None
    }
  }

  /**
   * 检测波动率异常
   */
  def detectVolatility(
                        stockCode: String,
                        recentKlines: List[KLineData],
                        threshold: Double = 0.03
                      ): Option[AnomalyRecord] = {

    if (recentKlines.length < 2) return None

    val latest = recentKlines.last
    val volatility = (latest.highPrice - latest.lowPrice) / latest.closePrice

    if (volatility > threshold) {
      val severity = if (volatility > 0.07) {
        Severity.HIGH
      } else {
        Severity.MEDIUM
      }

      Some(AnomalyRecord(
        id = None,
        stockCode = stockCode,
        anomalyType = AnomalyType.VOLATILITY_HIGH,
        anomalyTime = LocalDateTime.now(),
        severity = severity,
        description = Some(f"日内波动率过高 ${volatility * 100}%.2f%%，最高: ${latest.highPrice}%.2f，最低: ${latest.lowPrice}%.2f"),
        indicators = Some(s"""{"volatility":$volatility,"high":${latest.highPrice},"low":${latest.lowPrice}}""")
      ))
    } else {
      None
    }
  }

  /**
   * 综合检测
   */
  def detectAll(
                 quote: RealtimeQuote,
                 recentKlines: List[KLineData]
               ): List[AnomalyRecord] = {

    val anomalies = scala.collection.mutable.ListBuffer[AnomalyRecord]()

    // 检测成交量异常
    detectVolumeAnomaly(quote, recentKlines).foreach(a => anomalies += a)

    // 检测价格跳空
    detectPriceJump(quote, quote.closePrice).foreach(a => anomalies += a)

    // 检测涨跌停
    detectLimitUpDown(quote).foreach(a => anomalies += a)

    // 检测波动率
    if (recentKlines.nonEmpty) {
      detectVolatility(quote.stockCode, recentKlines).foreach(a => anomalies += a)
    }

    anomalies.toList
  }

  /**
   * 检测技术指标异常
   */
  def detectIndicatorAnomaly(
                              stockCode: String,
                              kdj: (Double, Double, Double),
                              rsi: Double
                            ): List[AnomalyRecord] = {

    val anomalies = scala.collection.mutable.ListBuffer[AnomalyRecord]()

    // KDJ超买超卖
    val (k, d, j) = kdj
    if (k > 80 && d > 80) {
      anomalies += AnomalyRecord(
        id = None,
        stockCode = stockCode,
        anomalyType = "KDJ_OVERBOUGHT",
        anomalyTime = LocalDateTime.now(),
        severity = Severity.MEDIUM,
        description = Some(f"KDJ超买，K=$k%.2f, D=$d%.2f"),
        indicators = Some(s"""{"k":$k,"d":$d,"j":$j}""")
      )
    } else if (k < 20 && d < 20) {
      anomalies += AnomalyRecord(
        id = None,
        stockCode = stockCode,
        anomalyType = "KDJ_OVERSOLD",
        anomalyTime = LocalDateTime.now(),
        severity = Severity.MEDIUM,
        description = Some(f"KDJ超卖，K=$k%.2f, D=$d%.2f"),
        indicators = Some(s"""{"k":$k,"d":$d,"j":$j}""")
      )
    }

    // RSI超买超卖
    if (rsi > 70) {
      anomalies += AnomalyRecord(
        id = None,
        stockCode = stockCode,
        anomalyType = "RSI_OVERBOUGHT",
        anomalyTime = LocalDateTime.now(),
        severity = Severity.LOW,
        description = Some(f"RSI超买，RSI=$rsi%.2f"),
        indicators = Some(s"""{"rsi":$rsi}""")
      )
    } else if (rsi < 30) {
      anomalies += AnomalyRecord(
        id = None,
        stockCode = stockCode,
        anomalyType = "RSI_OVERSOLD",
        anomalyTime = LocalDateTime.now(),
        severity = Severity.LOW,
        description = Some(f"RSI超卖，RSI=$rsi%.2f"),
        indicators = Some(s"""{"rsi":$rsi}""")
      )
    }

    anomalies.toList
  }
}