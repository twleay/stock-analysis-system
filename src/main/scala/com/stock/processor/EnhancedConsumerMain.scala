package com.stock.processor

import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import com.stock.connector.{KafkaConsumerConnector, MySQLConnector, RedisConnector, StockDAO}
import com.stock.model.RealtimeQuote
import com.stock.processor.{IndicatorCalculator, AnomalyDetector}
import spray.json._
import com.stock.model.StockJsonProtocol._

import scala.concurrent.ExecutionContext
import scala.io.StdIn
import scala.util.{Success, Failure, Try}
import java.time.LocalDate

/**
 * 增强版Kafka消费者（带技术指标计算和异常检测）
 */
object EnhancedConsumerMain extends App {

  println("=" * 60)
  println("股票数据智能分析系统启动")
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

  // 消费数据并处理
  val control = consumer.createConsumerSource()
    .map { record =>
      println(s"\n收到消息: topic=${record.topic()}, partition=${record.partition()}, offset=${record.offset()}")

      // 解析消息
      Try {
        record.value().parseJson.convertTo[RealtimeQuote]
      } match {
        case Success(quote) =>
          println(f"  ${quote.stockCode}: 当前价=${quote.currentPrice}%.2f, 成交量=${quote.volume}%,d")

          // 处理行情数据
          processQuote(quote)

          Some(quote)

        case Failure(e) =>
          println(s"  解析失败: ${e.getMessage}")
          None
      }
    }
    .runWith(Sink.ignore)

  println("\n消费者已启动，等待消息...")
  println("系统功能：")
  println("  ✓ 实时行情接收")
  println("  ✓ 技术指标计算")
  println("  ✓ 异常检测")
  println("  ✓ 数据持久化")
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

  /**
   * 处理实时行情
   */
  def processQuote(quote: RealtimeQuote): Unit = {
    try {
      val stockCode = quote.stockCode

      // 1. 缓存实时数据到Redis
      val key = s"realtime:$stockCode"
      RedisConnector.set(key, quote.toJson.compactPrint, 300)

      // 2. 获取历史K线数据
      val recentKlines = StockDAO.getLatestKLineData(stockCode, 60)

      if (recentKlines.length >= 60) {
        println(s"  [$stockCode] 开始计算技术指标...")

        // 3. 计算技术指标
        val indicators = IndicatorCalculator.calculateAllIndicators(stockCode, recentKlines)

        if (indicators.nonEmpty) {
          val latest = indicators.last
          println(s"  [$stockCode] 技术指标计算完成")

          // 显示关键指标
          latest.macd.foreach(v => println(f"    MACD: $v%.4f"))
          latest.kdjK.foreach(k => latest.kdjD.foreach(d =>
            println(f"    KDJ: K=$k%.2f, D=$d%.2f")
          ))
          latest.rsi12.foreach(v => println(f"    RSI(12): $v%.2f"))
          latest.ma5.foreach(ma5 => latest.ma20.foreach(ma20 =>
            println(f"    MA: 5日=$ma5%.2f, 20日=$ma20%.2f")
          ))

          // 4. 保存指标到数据库（异步）
          Try {
            StockDAO.insertTechnicalIndicators(latest)
          } match {
            case Success(_) => println(s"  [$stockCode] 技术指标已保存到数据库")
            case Failure(e) => println(s"  [$stockCode] 保存指标失败: ${e.getMessage}")
          }

          // 5. 检测技术指标异常
          val kdjAnomaly = for {
            k <- latest.kdjK
            d <- latest.kdjD
            j <- latest.kdjJ
          } yield (k, d, j)

          kdjAnomaly.foreach { kdj =>
            latest.rsi12.foreach { rsi =>
              val indicatorAnomalies = AnomalyDetector.detectIndicatorAnomaly(stockCode, kdj, rsi)
              if (indicatorAnomalies.nonEmpty) {
                println(s"  [$stockCode] ⚠️  检测到技术指标异常:")
                indicatorAnomalies.foreach { anomaly =>
                  println(s"    - ${anomaly.description.getOrElse("")}")

                  // 保存异常记录
                  Try {
                    StockDAO.insertAnomalyRecord(anomaly)
                  }
                }
              }
            }
          }
        } else {
          println(s"  [$stockCode] K线数据不足，跳过指标计算")
        }
      } else {
        println(s"  [$stockCode] K线数据不足60条（当前${recentKlines.length}条），跳过指标计算")
      }

      // 6. 检测实时行情异常
      val realtimeAnomalies = AnomalyDetector.detectAll(quote, recentKlines)
      if (realtimeAnomalies.nonEmpty) {
        println(s"  [$stockCode] 🚨 检测到实时行情异常:")
        realtimeAnomalies.foreach { anomaly =>
          println(s"    - [${anomaly.severity}] ${anomaly.description.getOrElse("")}")

          // 保存异常记录
          Try {
            StockDAO.insertAnomalyRecord(anomaly)
          } match {
            case Success(id) => println(s"      已保存异常记录，ID=$id")
            case Failure(e) => println(s"      保存失败: ${e.getMessage}")
          }
        }
      }

    } catch {
      case e: Exception =>
        println(s"  处理失败: ${e.getMessage}")
        e.printStackTrace()
    }
  }
}