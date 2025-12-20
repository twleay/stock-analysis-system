package com.stock.test

import com.redis.RedisClient
import com.typesafe.config.ConfigFactory
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

import java.sql.DriverManager
import java.util.Properties
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

object ConnectionTest extends App {

  val config = ConfigFactory.load()

  println("=" * 60)
  println("开始测试连接到虚拟机服务...")
  println("虚拟机IP: 192.168.202.130")
  println("=" * 60)

  // 测试Redis连接
  def testRedis(): Unit = {
    println("\n[1/3] 测试Redis连接...")
    Try {
      val redis = new RedisClient(
        host = config.getString("redis.host"),
        port = config.getInt("redis.port"),
        secret = Some(config.getString("redis.password"))
      )

      redis.ping match {
        case Some(_) =>
          println("✅ Redis连接成功!")
          redis.set("test:key", "Hello from Scala")
          val value = redis.get("test:key")
          println(s"   测试读写: $value")
        case None =>
          println("❌ Redis连接失败!")
      }
      redis.disconnect
    } match {
      case Success(_) =>
      case Failure(e) =>
        println(s"❌ Redis错误: ${e.getMessage}")
    }
  }

  // 测试MySQL连接
  def testMySQL(): Unit = {
    println("\n[2/3] 测试MySQL连接...")
    Try {
      Class.forName(config.getString("mysql.db.driver"))
      val connection = DriverManager.getConnection(
        config.getString("mysql.db.url"),
        config.getString("mysql.db.user"),
        config.getString("mysql.db.password")
      )

      println("✅ MySQL连接成功!")

      // 测试查询
      val stmt = connection.createStatement()
      val rs = stmt.executeQuery("SELECT stock_code, stock_name FROM stocks LIMIT 3")
      println("   测试查询:")
      while (rs.next()) {
        println(s"   - ${rs.getString("stock_code")}: ${rs.getString("stock_name")}")
      }

      rs.close()
      stmt.close()
      connection.close()
    } match {
      case Success(_) =>
      case Failure(e) =>
        println(s"❌ MySQL错误: ${e.getMessage}")
    }
  }

  // 测试Kafka连接
  def testKafka(): Unit = {
    println("\n[3/3] 测试Kafka连接...")

    // Producer测试
    Try {
      val props = new Properties()
      props.put("bootstrap.servers", config.getString("kafka.bootstrap-servers"))
      props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
      props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
      props.put("acks", "1")

      val producer = new KafkaProducer[String, String](props)
      val record = new ProducerRecord[String, String](
        config.getString("kafka.topic"),
        "test-key",
        s"""{"symbol":"TEST","price":100.5,"timestamp":${System.currentTimeMillis()}}"""
      )

      val metadata = producer.send(record).get()
      println(s"✅ Kafka Producer连接成功!")
      println(s"   消息已发送到 topic:${metadata.topic()}, partition:${metadata.partition()}, offset:${metadata.offset()}")

      producer.close()
    } match {
      case Success(_) =>
      case Failure(e) =>
        println(s"❌ Kafka Producer错误: ${e.getMessage}")
    }

    // Consumer测试
    Thread.sleep(1000) // 等待消息被写入

    Try {
      val props = new Properties()
      props.put("bootstrap.servers", config.getString("kafka.bootstrap-servers"))
      props.put("group.id", "test-consumer-group")
      props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
      props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
      props.put("auto.offset.reset", "earliest")
      props.put("enable.auto.commit", "true")

      val consumer = new KafkaConsumer[String, String](props)
      consumer.subscribe(List(config.getString("kafka.topic")).asJava)

      println("✅ Kafka Consumer连接成功!")
      println("   尝试消费消息 (等待3秒)...")

      val records = consumer.poll(java.time.Duration.ofSeconds(3))
      println(s"   收到 ${records.count()} 条消息")

      records.asScala.take(3).foreach { record =>
        println(s"   - Key: ${record.key()}, Value: ${record.value()}")
      }

      consumer.close()
    } match {
      case Success(_) =>
      case Failure(e) =>
        println(s"❌ Kafka Consumer错误: ${e.getMessage}")
    }
  }

  // 执行所有测试
  testRedis()
  testMySQL()
  testKafka()

  println("\n" + "=" * 60)
  println("🎉 连接测试完成!")
  println("=" * 60)
}