package com.stock.processor

import com.stock.connector.StockDAO
import java.time.LocalDate

/**
 * 指标回填工具
 * 作用：读取所有历史K线，计算所有技术指标，并写入数据库
 * 运行方式：直接运行此 Object
 */
object IndicatorBackfill extends App {

  println("=" * 60)
  println("开始执行技术指标全量回填...")
  println("=" * 60)

  // 1. 获取所有股票
  val stocks = StockDAO.getAllStocks()
  println(s"发现 ${stocks.size} 只股票: ${stocks.map(_.stockCode).mkString(", ")}")

  var totalInserted = 0

  stocks.foreach { stock =>
    val code = stock.stockCode
    print(s"\n处理 [$code] ... ")

    // 2. 获取该股票所有历史K线 (从2020年开始)
    val klines = StockDAO.getKLineData(code, LocalDate.of(2020, 1, 1), LocalDate.now())

    if (klines.length < 60) {
      println(s"K线数据不足60条 (当前: ${klines.length})，跳过")
    } else {
      // 3. 计算所有历史日期的指标
      // calculateAllIndicators 会返回一个列表，包含每一天的指标
      val allIndicators = IndicatorCalculator.calculateAllIndicators(code, klines)

      if (allIndicators.isEmpty) {
        println("计算结果为空")
      } else {
        print(s"计算出 ${allIndicators.length} 条指标数据 ... ")

        // 4. 批量写入数据库
        var count = 0
        allIndicators.foreach { indicator =>
          try {
            StockDAO.insertTechnicalIndicators(indicator)
            count += 1
          } catch {
            case e: Exception => // 忽略个别插入错误
          }
        }
        println(s"成功写入 $count 条")
        totalInserted += count
      }
    }
  }

  println("=" * 60)
  println(s"回填完成！共写入 $totalInserted 条指标数据")
  println("现在请刷新前端页面，指标应该都显示了。")
  println("=" * 60)
}