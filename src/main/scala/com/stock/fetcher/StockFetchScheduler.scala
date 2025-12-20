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

        // 发送到Kafka
        if (quotes.nonEmpty) {
          kafkaProducer.sendRealtimeQuotes(quotes).onComplete {
            case Success(_) =>
              context.log.info(s"成功发送 ${quotes.size} 条数据到Kafka")

              // 同时缓存到Redis
              quotes.foreach { quote =>
                val key = s"realtime:${quote.stockCode}"
                val value = quote.toJson.compactPrint
                RedisConnector.set(key, value, 300) // 缓存5分钟
              }

            case Failure(e) =>
              context.log.error(s"发送数据到Kafka失败: ${e.getMessage}")
          }

          // 打印部分数据
          quotes.take(3).foreach { quote =>
            context.log.info(f"  ${quote.stockCode}: ${quote.currentPrice}%.2f (涨跌: ${((quote.currentPrice - quote.closePrice) / quote.closePrice * 100)}%.2f%%)")
          }
        }

        // 继续下一次调度
        context.scheduleOnce(5.seconds, context.self, FetchData)
        Behaviors.same

      case Stop =>
        context.log.info("停止采集调度器")
        StockDataFetcher.close()
        Behaviors.stopped
    }
  }
}