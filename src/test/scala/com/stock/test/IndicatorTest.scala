package com.stock.test

import com.stock.processor.IndicatorCalculator
import com.stock.connector.StockDAO
import java.time.LocalDate

object IndicatorTest extends App {

  println("=" * 60)
  println("技术指标计算测试")
  println("=" * 60)

  val stockCode = "sh600000"

  // 准备测试数据（模拟60天K线）
  println(s"\n生成测试K线数据...")
  val testKlines = (0 until 60).map { i =>
    val date = LocalDate.now().minusDays(60 - i)
    val basePrice = 10.0 + math.sin(i * 0.1) * 2.0 + math.random() * 0.5

    com.stock.model.KLineData(
      stockCode = stockCode,
      tradeDate = date,
      openPrice = basePrice,
      highPrice = basePrice + 0.2,
      lowPrice = basePrice - 0.2,
      closePrice = basePrice + (math.random() - 0.5) * 0.3,
      volume = (1000000 + math.random() * 500000).toLong,
      amount = Some(basePrice * 1000000)
    )
  }.toList

  println(s"生成了 ${testKlines.size} 条K线数据")

  // 测试MACD
  println("\n[1/5] 测试MACD计算...")
  val macdResults = IndicatorCalculator.calculateMACD(testKlines)
  if (macdResults.nonEmpty) {
    println(s"✅ MACD计算成功，得到 ${macdResults.size} 条结果")
    val latest = macdResults.last
    println(f"  最新MACD: DIF=${latest._2}%.4f, DEA=${latest._3}%.4f, HIST=${latest._4}%.4f")
  } else {
    println("❌ MACD计算失败")
  }

  // 测试KDJ
  println("\n[2/5] 测试KDJ计算...")
  val kdjResults = IndicatorCalculator.calculateKDJ(testKlines)
  if (kdjResults.nonEmpty) {
    println(s"✅ KDJ计算成功，得到 ${kdjResults.size} 条结果")
    val latest = kdjResults.last
    println(f"  最新KDJ: K=${latest._2}%.2f, D=${latest._3}%.2f, J=${latest._4}%.2f")
  } else {
    println("❌ KDJ计算失败")
  }

  // 测试RSI
  println("\n[3/5] 测试RSI计算...")
  val rsi14 = IndicatorCalculator.calculateRSI(testKlines, 14)
  if (rsi14.nonEmpty) {
    println(s"✅ RSI计算成功，得到 ${rsi14.size} 条结果")
    val latest = rsi14.last
    println(f"  最新RSI(14): ${latest._2}%.2f")
  } else {
    println("❌ RSI计算失败")
  }

  // 测试MA
  println("\n[4/5] 测试MA计算...")
  val maResults = IndicatorCalculator.calculateMultipleMA(testKlines, List(5, 10, 20, 60))
  maResults.foreach { case (period, results) =>
    if (results.nonEmpty) {
      val latest = results.last
      println(f"  MA($period): ${latest._2}%.2f")
    }
  }

  // 测试BOLL
  println("\n[5/5] 测试BOLL计算...")
  val bollResults = IndicatorCalculator.calculateBOLL(testKlines)
  if (bollResults.nonEmpty) {
    println(s"✅ BOLL计算成功，得到 ${bollResults.size} 条结果")
    val latest = bollResults.last
    println(f"  最新BOLL: 上轨=${latest._3}%.2f, 中轨=${latest._2}%.2f, 下轨=${latest._4}%.2f")
  } else {
    println("❌ BOLL计算失败")
  }

  // 测试综合指标计算
  println("\n[综合] 计算完整技术指标集...")
  val allIndicators = IndicatorCalculator.calculateAllIndicators(stockCode, testKlines)
  if (allIndicators.nonEmpty) {
    println(s"✅ 综合指标计算成功，得到 ${allIndicators.size} 条完整指标")
    val latest = allIndicators.last
    println(s"\n最新指标汇总:")
    println(s"  日期: ${latest.tradeDate}")
    latest.macd.foreach(v => println(f"  MACD: $v%.4f"))
    latest.kdjK.foreach(v => println(f"  KDJ-K: $v%.2f"))
    latest.kdjD.foreach(v => println(f"  KDJ-D: $v%.2f"))
    latest.kdjJ.foreach(v => println(f"  KDJ-J: $v%.2f"))
    latest.rsi6.foreach(v => println(f"  RSI(6): $v%.2f"))
    latest.ma5.foreach(v => println(f"  MA(5): $v%.2f"))
    latest.ma20.foreach(v => println(f"  MA(20): $v%.2f"))
  }

  println("\n" + "=" * 60)
  println("🎉 技术指标测试完成！")
  println("=" * 60)
}