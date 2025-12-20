package com.stock.fetcher

import akka.actor.typed.ActorSystem
import com.stock.connector.{MySQLConnector, RedisConnector}

import scala.io.StdIn

/**
 * 数据采集主程序
 */
object FetcherMain extends App {

  println("=" * 60)
  println("股票数据采集系统启动")
  println("=" * 60)

  // 测试数据库连接
  println("\n检查数据库连接...")
  if (MySQLConnector.testConnection()) {
    println("✅ MySQL连接成功")
  } else {
    println("❌ MySQL连接失败")
  }

  if (RedisConnector.testConnection()) {
    println("✅ Redis连接成功")
  } else {
    println("⚠️  Redis连接失败（不影响Kafka功能）")
  }

  // 启动Actor系统
  println("\n启动采集调度器...")
  val system: ActorSystem[StockFetchScheduler.Command] =
    ActorSystem(StockFetchScheduler(), "stock-fetch-system")

  println("\n采集系统已启动！")
  println("数据将每5秒采集一次并发送到Kafka")
  println("按 ENTER 键停止...")
  println("=" * 60)

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