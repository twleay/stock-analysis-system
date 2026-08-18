package com.stock.config

import com.typesafe.config.{Config, ConfigFactory}
import scala.util.Try
import scala.jdk.CollectionConverters._

/**
 * 应用配置管理
 */
object AppConfig {

  private val config: Config = Try(ConfigFactory.load()).getOrElse(ConfigFactory.empty())

  // Kafka配置
  object Kafka {
    val bootstrapServers: String = getStringOrDefault("stock.kafka.bootstrap-servers", "localhost:9092")
    val topic: String = getStringOrDefault("stock.kafka.topic", "stock-realtime")
    val groupId: String = getStringOrDefault("stock.kafka.group-id", "stock-consumer-group")

    object Producer {
      val acks: String = getStringOrDefault("stock.kafka.producer.acks", "1")
      val retries: Int = getIntOrDefault("stock.kafka.producer.retries", 3)
      val batchSize: Int = getIntOrDefault("stock.kafka.producer.batch-size", 16384)
      val lingerMs: Int = getIntOrDefault("stock.kafka.producer.linger-ms", 1)
    }

    object Consumer {
      val autoOffsetReset: String = getStringOrDefault("stock.kafka.consumer.auto-offset-reset", "earliest")
      val enableAutoCommit: Boolean = getBooleanOrDefault("stock.kafka.consumer.enable-auto-commit", true)
      val maxPollRecords: Int = getIntOrDefault("stock.kafka.consumer.max-poll-records", 500)
    }
  }

  // Redis配置
  object Redis {
    val host: String = getStringOrDefault("stock.redis.host", "localhost")
    val port: Int = getIntOrDefault("stock.redis.port", 6379)
    val password: String = getStringOrDefault("stock.redis.password", "CHANGE_ME")
    val database: Int = getIntOrDefault("stock.redis.database", 0)
    val timeout: Int = getIntOrDefault("stock.redis.timeout", 5000)
  }

  // MySQL配置
  object MySQL {
    val driver: String = getStringOrDefault("stock.database.driver", "com.mysql.cj.jdbc.Driver")
    val url: String = getStringOrDefault("stock.database.url",
      "jdbc:mysql://localhost:3306/stock_analysis?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8")
    val user: String = getStringOrDefault("stock.database.user", "stock_user")
    val password: String = getStringOrDefault("stock.database.password", "CHANGE_ME")
    val numThreads: Int = getIntOrDefault("stock.database.numThreads", 10)
    val maxConnections: Int = getIntOrDefault("stock.database.maxConnections", 20)
  }

  // HTTP配置
  object Http {
    val host: String = getStringOrDefault("stock.http.host", "0.0.0.0")
    val port: Int = getIntOrDefault("stock.http.port", 8080)
  }

  // 股票采集配置
  object StockFetcher {
    val fetchInterval: String = getStringOrDefault("stock.stock-fetcher.fetch-interval", "5s")
    val batchSize: Int = getIntOrDefault("stock.stock-fetcher.batch-size", 10)
    val retryTimes: Int = getIntOrDefault("stock.stock-fetcher.retry-times", 3)
    val stockList: List[String] = getStringListOrDefault("stock.stock-fetcher.stock-list",
      List("sh600000", "sh600036", "sz000001", "sz000002", "sh600519"))
  }

  // 辅助方法
  private def getStringOrDefault(path: String, default: String): String = {
    Try(config.getString(path)).getOrElse(default)
  }

  private def getIntOrDefault(path: String, default: Int): Int = {
    Try(config.getInt(path)).getOrElse(default)
  }

  private def getBooleanOrDefault(path: String, default: Boolean): Boolean = {
    Try(config.getBoolean(path)).getOrElse(default)
  }

  private def getStringListOrDefault(path: String, default: List[String]): List[String] = {
    Try(config.getStringList(path).asScala.toList).getOrElse(default)
  }
}