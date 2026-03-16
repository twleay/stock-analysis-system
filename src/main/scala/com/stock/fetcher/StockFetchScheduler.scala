package com.stock.fetcher

import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import com.stock.config.AppConfig
import com.stock.connector.{KafkaProducerConnector, RedisConnector}
import com.stock.model.RealtimeQuote
import spray.json._
import com.stock.model.StockJsonProtocol._

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.util.{Success, Failure}

/**
 * 股票数据定时采集调度器
 */
object StockFetchScheduler {

  sealed trait Command
  case object FetchData extends Command
  case object Stop extends Command
  private case class KafkaSendResult(success: Boolean, count: Int, error: Option[String]) extends Command

  def apply(): Behavior[Command] = Behaviors.setup { context =>
    implicit val ec: ExecutionContext = context.executionContext
    implicit val classicSystem = context.system.classicSystem

    val kafkaProducer = KafkaProducerConnector()
    val stockList = AppConfig.StockFetcher.stockList

    context.log.info(s"启动股票数据采集调度器，监控 ${stockList.size} 只股票")
    context.log.info(s"股票列表: ${stockList.mkString(", ")}")

    // 定时任务：每5秒采集一次
    context.scheduleOnce(5.seconds, context.self, FetchData)

    Behaviors.receiveMessage {
      case FetchData =>
        context.log.info("开始采集股票数据...")

        // 采集数据
        val quotes = StockDataFetcher.fetchRealtimeQuotes(stockList)
        context.log.info(s"采集到 ${quotes.size} 条数据")

        // ✅ 在 Future 回调之前先打印日志和缓存数据
        if (quotes.nonEmpty) {
          // 打印部分数据（在 Actor 上下文中）
          quotes.take(3).foreach { quote =>
            val changePercent = (quote.currentPrice - quote.closePrice) / quote.closePrice * 100
            context.log.info(f"  ${quote.stockCode}: ${quote.currentPrice}%.2f (涨跌: ${changePercent}%.2f%%)")
          }

          // 同时缓存到Redis（在 Actor 上下文中）
          quotes.foreach { quote =>
            val key = s"realtime:${quote.stockCode}"
            val value = quote.toJson.compactPrint
            RedisConnector.set(key, value, 300) // 缓存5分钟
          }

          // 发送到Kafka（异步操作）
          kafkaProducer.sendRealtimeQuotes(quotes).onComplete {
            case Success(_) =>
              // ✅ 通过发送消息给 self 来处理结果，而不是直接访问 context
              context.self ! KafkaSendResult(success = true, count = quotes.size, error = None)

            case Failure(e) =>
              context.self ! KafkaSendResult(success = false, count = quotes.size, error = Some(e.getMessage))
          }
        }

        // 继续下一次调度
        context.scheduleOnce(5.seconds, context.self, FetchData)
        Behaviors.same

      case KafkaSendResult(success, count, error) =>
        // ✅ 在 Actor 消息处理中访问 context.log 是安全的
        if (success) {
          context.log.info(s"成功发送 $count 条数据到Kafka")
        } else {
          context.log.error(s"发送数据到Kafka失败: ${error.getOrElse("未知错误")}")
        }
        Behaviors.same

      case Stop =>
        context.log.info("停止采集调度器")
        StockDataFetcher.close()
        Behaviors.stopped
    }
  }
}