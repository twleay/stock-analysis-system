package com.stock.test

import com.stock.connector.{MySQLConnector, RedisConnector, StockDAO}
import com.stock.model.{Stock, KLineData, TechnicalIndicators}
import java.time.LocalDate

object DatabaseTest extends App {

  println("=" * 60)
  println("测试数据库访问层（纯JDBC版本）")
  println("=" * 60)

  // 测试MySQL连接
  println("\n[1/5] 测试MySQL连接...")
  if (StockDAO.testConnection()) {
    println("✅ MySQL连接成功!")
  } else {
    println("❌ MySQL连接失败!")
    System.exit(1)
  }

  // 测试Redis连接
  println("\n[2/5] 测试Redis连接...")
  if (RedisConnector.testConnection()) {
    println("✅ Redis连接成功!")

    RedisConnector.set("test:key", "Hello from Scala", 60)
    val value = RedisConnector.get("test:key")
    println(s"   Redis读写测试: $value")
  } else {
    println("⚠️  Redis连接失败（不影响功能）")
  }

  // 测试查询股票
  println("\n[3/5] 测试查询股票数据...")
  try {
    val stocks = StockDAO.getAllStocks()
    println(s"✅ 查询到 ${stocks.size} 只股票:")
    stocks.take(3).foreach(s => println(s"   - ${s.stockCode}: ${s.stockName}"))

    val count = StockDAO.getStockCount()
    println(s"   股票总数: $count")
  } catch {
    case e: Exception =>
      println(s"❌ 查询失败: ${e.getMessage}")
      e.printStackTrace()
  }

  // 测试插入K线数据
  println("\n[4/5] 测试插入K线数据...")
  try {
    val today = LocalDate.now()
    val klineData = KLineData(
      stockCode = "sh600000",
      tradeDate = today,
      openPrice = 10.30,
      highPrice = 10.60,
      lowPrice = 10.20,
      closePrice = 10.50,
      volume = 1000000L,
      amount = Some(10500000.0)
    )

    val result = StockDAO.insertKLineData(klineData)
    println(s"✅ K线数据插入成功! 影响行数: $result")

    val latestData = StockDAO.getLatestKLineData("sh600000", 1)
    latestData.headOption match {
      case Some(data) =>
        println(s"   最新K线: 日期=${data.tradeDate}, 收盘=${data.closePrice}")
      case None =>
        println("   未找到K线数据")
    }

    // 测试批量插入
    val batchData = List(
      KLineData("sh600000", today.minusDays(1), 10.20, 10.50, 10.10, 10.40, 950000L, Some(9900000.0)),
      KLineData("sh600000", today.minusDays(2), 10.10, 10.40, 10.00, 10.30, 920000L, Some(9500000.0))
    )
    val batchResult = StockDAO.insertKLineDataBatch(batchData)
    println(s"✅ 批量插入K线数据成功! 影响行数: $batchResult")

  } catch {
    case e: Exception =>
      println(s"❌ K线数据操作失败: ${e.getMessage}")
      e.printStackTrace()
  }

  // 测试技术指标
  println("\n[5/5] 测试技术指标...")
  try {
    val indicators = TechnicalIndicators(
      stockCode = "sh600000",
      tradeDate = LocalDate.now(),
      macd = Some(0.15),
      macdSignal = Some(0.12),
      macdHist = Some(0.03),
      kdjK = Some(75.5),
      kdjD = Some(70.2),
      kdjJ = Some(85.1),
      ma5 = Some(10.45),
      ma10 = Some(10.40)
    )

    val result = StockDAO.insertTechnicalIndicators(indicators)
    println(s"✅ 技术指标插入成功! 影响行数: $result")

    val retrieved = StockDAO.getTechnicalIndicators("sh600000", LocalDate.now())
    retrieved match {
      case Some(ind) =>
        println(s"   MACD: ${ind.macd}, KDJ-K: ${ind.kdjK}")
      case None =>
        println("   未找到技术指标")
    }

  } catch {
    case e: Exception =>
      println(s"❌ 技术指标操作失败: ${e.getMessage}")
      e.printStackTrace()
  }

  // 关闭连接
  println("\n关闭数据库连接...")
  RedisConnector.close()

  println("\n" + "=" * 60)
  println("🎉 数据库访问层测试完成!")
  println("=" * 60)
}