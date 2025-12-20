package com.stock.connector

import akka.actor.ActorSystem
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.kafka.scaladsl.Consumer
import akka.stream.scaladsl.Source
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import com.stock.config.AppConfig
import com.stock.model.RealtimeQuote
import spray.json._
import com.stock.model.StockJsonProtocol._

import scala.util.{Try, Success, Failure}

/**
 * Kafka消费者连接器
 */
class KafkaConsumerConnector(implicit system: ActorSystem) {

  private val consumerSettings = ConsumerSettings(system, new StringDeserializer, new StringDeserializer)
    .withBootstrapServers(AppConfig.Kafka.bootstrapServers)
    .withGroupId(AppConfig.Kafka.groupId)

  /**
   * 创建消费者流
   */
  def createConsumerSource(): Source[ConsumerRecord[String, String], Consumer.Control] = {
    Consumer
      .plainSource(consumerSettings, Subscriptions.topics(AppConfig.Kafka.topic))
  }

  /**
   * 解析消息为RealtimeQuote
   */
  def parseMessage(record: ConsumerRecord[String, String]): Option[RealtimeQuote] = {
    Try {
      record.value().parseJson.convertTo[RealtimeQuote]
    } match {
      case Success(quote) => Some(quote)
      case Failure(e) =>
        println(s"解析消息失败: ${e.getMessage}")
        None
    }
  }
}

object KafkaConsumerConnector {
  def apply()(implicit system: ActorSystem): KafkaConsumerConnector =
    new KafkaConsumerConnector()
}