package com.stock.test

import com.stock.fetcher.StockDataFetcher
import com.stock.config.AppConfig

object FetcherTest extends App {

  println("=" * 60)
  println("测试股票数据采集")
  println("=" * 60)

  val stockList = AppConfig.StockFetcher.stockList

  println(s"\n测试采集 ${stockList.size} 只股票的数据:")
  stockList.foreach(code => println(s"  - $code"))

  println("\n开始采集...")
  val quotes = StockDataFetcher.fetchRealtimeQuotes(stockList)

  println(s"\n✅ 成功采集 ${quotes.size} 条数据:\n")

  quotes.foreach { quote =>
    val change = quote.currentPrice - quote.closePrice
    val changePercent = (change / quote.closePrice) * 100
    val arrow = if (change > 0) "↑" else if (change < 0) "↓" else "→"

    println(f"${quote.stockCode}:")
    println(f"  当前价: ${quote.currentPrice}%.2f  $arrow ${change}%.2f (${changePercent}%.2f%%)")
    println(f"  开盘价: ${quote.openPrice}%.2f")
    println(f"  最高价: ${quote.highPrice}%.2f")
    println(f"  最低价: ${quote.lowPrice}%.2f")
    println(f"  成交量: ${quote.volume}%,d 股")
    println(f"  成交额: ${quote.amount / 10000}%.2f 万元")
    println()
  }

  println("=" * 60)
  println("测试完成")
  println("=" * 60)

  StockDataFetcher.close()
}
