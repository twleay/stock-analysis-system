package com.stock.processor

import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import com.stock.connector.{KafkaConsumerConnector, MySQLConnector, RedisConnector}
import com.stock.model.RealtimeQuote
import spray.json._
import com.stock.model.StockJsonProtocol._

import scala.concurrent.ExecutionContext
import scala.io.StdIn
import scala.util.{Success, Failure, Try}

/**
 * Kafka消费者主程序
 */
object ConsumerMain extends App {

  println("=" * 60)
  println("股票数据消费系统启动")
  println("=" * 60)

  implicit val system: ActorSystem = ActorSystem("stock-consumer-system")
  implicit val ec: ExecutionContext = system.dispatcher

  // 测试连接
  println("\n检查数据库连接...")
  if (MySQLConnector.testConnection()) {
    println("✅ MySQL连接成功")
  } else {
    println("❌ MySQL连接失败")
  }

  if (RedisConnector.testConnection()) {
    println("✅ Redis连接成功")
  } else {
    println("⚠️  Redis连接失败")
  }

  // 创建Kafka消费者
  println("\n启动Kafka消费者...")
  val consumer = KafkaConsumerConnector()

  // 消费数据
  val control = consumer.createConsumerSource()
    .map { record =>
      println(s"收到消息: topic=${record.topic()}, partition=${record.partition()}, offset=${record.offset()}")

      // 解析消息
      Try {
        record.value().parseJson.convertTo[RealtimeQuote]
      } match {
        case Success(quote) =>
          println(f"  ${quote.stockCode}: 当前价=${quote.currentPrice}%.2f, 成交量=${quote.volume}%,d")

          // 这里可以添加数据处理逻辑
          // 例如：计算技术指标、异常检测、存入数据库等

          Some(quote)

        case Failure(e) =>
          println(s"  解析失败: ${e.getMessage}")
          None
      }
    }
    .runWith(Sink.ignore)

  println("\n消费者已启动，等待消息...")
  println("按 ENTER 键停止...")
  println("=" * 60)

  // 等待用户输入
  StdIn.readLine()

  // 停止系统
  println("\n正在停止系统...")
  Thread.sleep(2000)
  system.terminate()

  // 关闭连接
  MySQLConnector.close()
  RedisConnector.close()

  println("系统已停止")
}