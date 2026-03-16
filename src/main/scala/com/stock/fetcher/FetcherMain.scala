package com.stock.fetcher

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter._ // 关键：用于将 Typed ActorSystem 转为 Classic
import com.stock.connector.{MySQLConnector, RedisConnector}
import scala.io.StdIn
import scala.concurrent.ExecutionContext

/**
 * 数据采集主程序
 */
object FetcherMain extends App {

  println("=" * 60)
  println("股票数据采集系统启动")
  println("=" * 60)

  // 1. 测试数据库连接
  println("\n检查数据库连接...")
  if (MySQLConnector.testConnection()) {
    println(" MySQL连接成功")
  } else {
    println("MySQL连接失败")
  }

  if (RedisConnector.testConnection()) {
    println("Redis连接成功")
  } else {
    println(" Redis连接失败（不影响Kafka功能）")
  }

  // 2. 启动Actor系统
  println("\n启动采集调度器...")
  val system: ActorSystem[StockFetchScheduler.Command] =
    ActorSystem(StockFetchScheduler(), "stock-fetch-system")

  // ==========================================
  // 初始化历史数据
  // ==========================================
  implicit val classicSystem: akka.actor.ActorSystem = system.toClassic
  implicit val ec: ExecutionContext = system.executionContext

  // 修改点：将列表扩展为所有 5 只股票
  // 包含：浦发银行(sh600000), 茅台(sh600519), 招商银行(sh600036), 平安银行(sz000001), 万科A(sz000002)
  val targetStocks = List("sh600000", "sh600519", "sh600036", "sz000001", "sz000002")

  println(s"\n[1/2] 正在初始化历史数据 (共 ${targetStocks.size} 只股票)...")
  targetStocks.foreach { code =>
    // 异步调用，会自动处理 HTTPS 请求
    HistoryFetcher.initHistory(code)
  }
  // ==========================================

  println("\n[2/2] 采集系统已启动！")
  println("数据将每5秒采集一次并发送到Kafka")
  println("按 ENTER 键停止...")
  println("=" * 60)

  // 给调度器发送开始指令
  system ! StockFetchScheduler.FetchData

  // 等待用户输入
  StdIn.readLine()

  // 停止系统
  println("\n正在停止系统...")
  system ! StockFetchScheduler.Stop
  Thread.sleep(2000)
  system.terminate()

  // 关闭连接
  MySQLConnector.close()
  RedisConnector.close()

  println("系统已停止")
}