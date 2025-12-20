package com.stock.connector

import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.scaladsl.Source
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import com.stock.config.AppConfig
import com.stock.model.RealtimeQuote
import spray.json._
import com.stock.model.StockJsonProtocol._

import scala.concurrent.Future

/**
 * Kafka生产者连接器
 */
class KafkaProducerConnector(implicit system: ActorSystem) {

  private val producerSettings = ProducerSettings(system, new StringSerializer, new StringSerializer)
    .withBootstrapServers(AppConfig.Kafka.bootstrapServers)

  /**
   * 发送单条消息
   */
  def sendMessage(topic: String, key: String, value: String): Future[_] = {
    val record = new ProducerRecord[String, String](topic, key, value)
    Source.single(record)
      .runWith(Producer.plainSink(producerSettings))
  }

  /**
   * 发送实时行情到Kafka
   */
  def sendRealtimeQuote(quote: RealtimeQuote): Future[_] = {
    val json = quote.toJson.compactPrint
    sendMessage(
      topic = AppConfig.Kafka.topic,
      key = quote.stockCode,
      value = json
    )
  }

  /**
   * 批量发送实时行情
   */
  def sendRealtimeQuotes(quotes: List[RealtimeQuote]): Future[_] = {
    val records = quotes.map { quote =>
      new ProducerRecord[String, String](
        AppConfig.Kafka.topic,
        quote.stockCode,
        quote.toJson.compactPrint
      )
    }

    Source(records)
      .runWith(Producer.plainSink(producerSettings))
  }
}

object KafkaProducerConnector {
  def apply()(implicit system: ActorSystem): KafkaProducerConnector =
    new KafkaProducerConnector()
}