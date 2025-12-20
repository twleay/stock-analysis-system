package com.stock.processor

import com.stock.model.{KLineData, TechnicalIndicators}
import scala.annotation.tailrec

/**
 * 技术指标计算器
 */
object IndicatorCalculator {

  // ========== MACD计算 ==========

  /**
   * 计算MACD指标
   * @param klines K线数据（至少需要26+9=35条）
   * @param shortPeriod 短期EMA周期，默认12
   * @param longPeriod 长期EMA周期，默认26
   * @param signalPeriod 信号线周期，默认9
   */
  def calculateMACD(
                     klines: List[KLineData],
                     shortPeriod: Int = 12,
                     longPeriod: Int = 26,
                     signalPeriod: Int = 9
                   ): List[(KLineData, Double, Double, Double)] = {

    if (klines.length < longPeriod + signalPeriod) {
      println(s"MACD计算需要至少 ${longPeriod + signalPeriod} 条K线，当前只有 ${klines.length} 条")
      return List.empty
    }

    val prices = klines.map(_.closePrice)

    // 计算短期EMA
    val emaShort = calculateEMAList(prices, shortPeriod)

    // 计算长期EMA
    val emaLong = calculateEMAList(prices, longPeriod)

    if (emaShort.isEmpty || emaLong.isEmpty) {
      println("EMA计算失败")
      return List.empty
    }

    // 确保两个EMA列表长度一致（取较短的长度）
    val minLength = math.min(emaShort.length, emaLong.length)
    val emaShortAligned = emaShort.takeRight(minLength)
    val emaLongAligned = emaLong.takeRight(minLength)

    // 计算DIF（MACD线）= 短期EMA - 长期EMA
    val dif = emaShortAligned.zip(emaLongAligned).map { case (short, long) => short - long }

    // 计算DEA（信号线）= DIF的9日EMA
    val dea = calculateEMAList(dif, signalPeriod)

    if (dea.isEmpty) {
      println("DEA计算失败")
      return List.empty
    }

    // 计算MACD柱状图 = (DIF - DEA) * 2
    val difAligned = dif.takeRight(dea.length)
    val macdHist = difAligned.zip(dea).map { case (d, s) => (d - s) * 2 }

    // 返回结果
    val klinesAligned = klines.takeRight(dea.length)
    klinesAligned.zip(difAligned).zip(dea).zip(macdHist)
      .map { case (((kline, macd), signal), hist) =>
        (kline, macd, signal, hist)
      }
  }

  /**
   * 计算EMA列表（修复版）
   */
  private def calculateEMAList(values: List[Double], period: Int): List[Double] = {
    if (values.length < period) {
      println(s"EMA计算需要至少 $period 个数据点，当前只有 ${values.length} 个")
      return List.empty
    }

    val multiplier = 2.0 / (period + 1)

    // 第一个EMA值 = 前period个数的SMA
    val firstEma = values.take(period).sum / period

    // 从第period+1个数据开始计算EMA
    val emaValues = values.drop(period).foldLeft(List(firstEma)) { (emas, price) =>
      val newEma = (price - emas.head) * multiplier + emas.head
      newEma :: emas
    }

    emaValues.reverse
  }

  // ========== KDJ计算 ==========

  /**
   * 计算KDJ指标
   * @param klines K线数据（至少需要n+m条）
   * @param n RSV周期，默认9
   * @param m1 K值平滑周期，默认3
   * @param m2 D值平滑周期，默认3
   */
  def calculateKDJ(
                    klines: List[KLineData],
                    n: Int = 9,
                    m1: Int = 3,
                    m2: Int = 3
                  ): List[(KLineData, Double, Double, Double)] = {

    if (klines.length < n) return List.empty

    // 计算RSV（未成熟随机值）
    val rsv = klines.sliding(n).map { window =>
      val closes = window.map(_.closePrice)
      val highs = window.map(_.highPrice)
      val lows = window.map(_.lowPrice)

      val currentClose = closes.last
      val highestHigh = highs.max
      val lowestLow = lows.min

      if (highestHigh == lowestLow) {
        50.0 // 避免除以0
      } else {
        (currentClose - lowestLow) / (highestHigh - lowestLow) * 100
      }
    }.toList

    // 初始K和D值
    var k = 50.0
    var d = 50.0

    val results = rsv.zipWithIndex.map { case (rsvValue, idx) =>
      // K = (2/3) * 前一日K + (1/3) * 当日RSV
      k = (2.0 / 3.0) * k + (1.0 / 3.0) * rsvValue

      // D = (2/3) * 前一日D + (1/3) * 当日K
      d = (2.0 / 3.0) * d + (1.0 / 3.0) * k

      // J = 3K - 2D
      val j = 3 * k - 2 * d

      (k, d, j)
    }

    // 返回结果（从第n条开始）
    klines.drop(n - 1).zip(results).map { case (kline, (k, d, j)) =>
      (kline, k, d, j)
    }
  }

  // ========== RSI计算 ==========

  /**
   * 计算RSI指标
   * @param klines K线数据
   * @param period 周期，默认14
   */
  def calculateRSI(klines: List[KLineData], period: Int = 14): List[(KLineData, Double)] = {
    if (klines.length < period + 1) return List.empty

    // 计算价格变化
    val priceChanges = klines.sliding(2).map { pair =>
      pair(1).closePrice - pair(0).closePrice
    }.toList

    // 分离上涨和下跌
    val gains = priceChanges.map(change => if (change > 0) change else 0.0)
    val losses = priceChanges.map(change => if (change < 0) -change else 0.0)

    // 计算平均涨幅和平均跌幅（使用SMA）
    val avgGains = calculateSMA(gains, period)
    val avgLosses = calculateSMA(losses, period)

    // 计算RSI
    val rsi = avgGains.zip(avgLosses).map { case (gain, loss) =>
      if (loss == 0) {
        100.0
      } else {
        val rs = gain / loss
        100 - (100 / (1 + rs))
      }
    }

    // 返回结果（从第period条开始）
    klines.drop(period).zip(rsi).map { case (kline, rsiValue) =>
      (kline, rsiValue)
    }
  }

  /**
   * 计算SMA（简单移动平均）
   */
  private def calculateSMA(values: List[Double], period: Int): List[Double] = {
    values.sliding(period).map(window => window.sum / period).toList
  }

  // ========== MA（移动平均线）计算 ==========

  /**
   * 计算移动平均线
   * @param klines K线数据
   * @param period 周期
   */
  def calculateMA(klines: List[KLineData], period: Int): List[(KLineData, Double)] = {
    if (klines.length < period) return List.empty

    val prices = klines.map(_.closePrice)
    val ma = calculateSMA(prices, period)

    klines.drop(period - 1).zip(ma).map { case (kline, maValue) =>
      (kline, maValue)
    }
  }

  /**
   * 批量计算多个周期的MA
   */
  def calculateMultipleMA(
                           klines: List[KLineData],
                           periods: List[Int] = List(5, 10, 20, 60)
                         ): Map[Int, List[(KLineData, Double)]] = {
    periods.map { period =>
      period -> calculateMA(klines, period)
    }.toMap
  }

  // ========== BOLL（布林带）计算 ==========

  /**
   * 计算布林带
   * @param klines K线数据
   * @param period 周期，默认20
   * @param stdDev 标准差倍数，默认2
   */
  def calculateBOLL(
                     klines: List[KLineData],
                     period: Int = 20,
                     stdDev: Double = 2.0
                   ): List[(KLineData, Double, Double, Double)] = {

    if (klines.length < period) return List.empty

    val prices = klines.map(_.closePrice)

    val results = prices.sliding(period).toList.map { window =>
      val mean = window.sum / period
      val variance = window.map(p => math.pow(p - mean, 2)).sum / period
      val std = math.sqrt(variance)

      val upper = mean + stdDev * std
      val lower = mean - stdDev * std

      (mean, upper, lower)
    }

    klines.drop(period - 1).zip(results).map { case (kline, (middle, upper, lower)) =>
      (kline, middle, upper, lower)
    }
  }

  // ========== 综合指标计算 ==========

  /**
   * 计算完整的技术指标集
   */
  def calculateAllIndicators(
                              stockCode: String,
                              klines: List[KLineData]
                            ): List[TechnicalIndicators] = {

    if (klines.length < 60) {
      println(s"$stockCode K线数据不足60条，无法计算完整指标")
      return List.empty
    }

    // 计算各项指标
    val macdResults = calculateMACD(klines)
    val kdjResults = calculateKDJ(klines)
    val rsi6 = calculateRSI(klines, 6)
    val rsi12 = calculateRSI(klines, 12)
    val rsi24 = calculateRSI(klines, 24)
    val ma5 = calculateMA(klines, 5)
    val ma10 = calculateMA(klines, 10)
    val ma20 = calculateMA(klines, 20)
    val ma60 = calculateMA(klines, 60)

    // 创建日期到指标的映射
    val macdMap = macdResults.map { case (kline, macd, signal, hist) =>
      kline.tradeDate -> (macd, signal, hist)
    }.toMap

    val kdjMap = kdjResults.map { case (kline, k, d, j) =>
      kline.tradeDate -> (k, d, j)
    }.toMap

    val rsi6Map = rsi6.map { case (kline, value) => kline.tradeDate -> value }.toMap
    val rsi12Map = rsi12.map { case (kline, value) => kline.tradeDate -> value }.toMap
    val rsi24Map = rsi24.map { case (kline, value) => kline.tradeDate -> value }.toMap
    val ma5Map = ma5.map { case (kline, value) => kline.tradeDate -> value }.toMap
    val ma10Map = ma10.map { case (kline, value) => kline.tradeDate -> value }.toMap
    val ma20Map = ma20.map { case (kline, value) => kline.tradeDate -> value }.toMap
    val ma60Map = ma60.map { case (kline, value) => kline.tradeDate -> value }.toMap

    // 合并所有指标（从第60条开始，确保所有指标都有数据）
    klines.drop(59).map { kline =>
      val date = kline.tradeDate

      TechnicalIndicators(
        stockCode = stockCode,
        tradeDate = date,
        macd = macdMap.get(date).map(_._1),
        macdSignal = macdMap.get(date).map(_._2),
        macdHist = macdMap.get(date).map(_._3),
        kdjK = kdjMap.get(date).map(_._1),
        kdjD = kdjMap.get(date).map(_._2),
        kdjJ = kdjMap.get(date).map(_._3),
        rsi6 = rsi6Map.get(date),
        rsi12 = rsi12Map.get(date),
        rsi24 = rsi24Map.get(date),
        ma5 = ma5Map.get(date),
        ma10 = ma10Map.get(date),
        ma20 = ma20Map.get(date),
        ma60 = ma60Map.get(date)
      )
    }
  }
}