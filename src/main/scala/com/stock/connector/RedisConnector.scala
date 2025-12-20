package com.stock.connector

import com.redis.RedisClient
import scala.util.Try

/**
 * Redis缓存连接管理
 */
object RedisConnector {

  // 根据配置文件，密码应该是123456，不是stock123456
  private val REDIS_HOST = "192.168.202.130"
  private val REDIS_PORT = 6379
  private val REDIS_PASSWORD = "123456"  // 这里修改为123456
  private val REDIS_DATABASE = 0
  private val REDIS_TIMEOUT = 5000

  private var clientOpt: Option[RedisClient] = None

  /**
   * 获取Redis客户端
   */
  def getClient: RedisClient = {
    clientOpt match {
      case Some(client) => client
      case None =>
        println(s"正在连接Redis: $REDIS_HOST:$REDIS_PORT")
        val client = new RedisClient(
          host = REDIS_HOST,
          port = REDIS_PORT,
          secret = if (REDIS_PASSWORD.isEmpty) None else Some(REDIS_PASSWORD),
          database = REDIS_DATABASE,
          timeout = REDIS_TIMEOUT
        )
        clientOpt = Some(client)
        client
    }
  }

  /**
   * 设置缓存
   */
  def set(key: String, value: String, expireSeconds: Int = 300): Boolean = {
    Try {
      getClient.set(key, value)
      if (expireSeconds > 0) {
        getClient.expire(key, expireSeconds)
      }
      true
    }.recover {
      case e: Exception =>
        println(s"Redis设置操作失败: ${e.getMessage}")
        false
    }.getOrElse(false)
  }

  /**
   * 获取缓存
   */
  def get(key: String): Option[String] = {
    Try {
      getClient.get(key)
    }.recover {
      case e: Exception =>
        println(s"Redis获取操作失败: ${e.getMessage}")
        None
    }.toOption.flatten
  }

  /**
   * 删除缓存
   */
  def delete(key: String): Boolean = {
    Try {
      getClient.del(key).getOrElse(0L) > 0
    }.recover {
      case e: Exception =>
        println(s"Redis删除操作失败: ${e.getMessage}")
        false
    }.getOrElse(false)
  }

  /**
   * 测试连接
   */
  def testConnection(): Boolean = {
    Try {
      val result = getClient.ping
      val success = result.isDefined && result.get == "PONG"
      if (success) {
        println("Redis ping成功!")
      }
      success
    }.recover {
      case e: Exception =>
        println(s"Redis连接测试失败: ${e.getMessage}")
        false
    }.getOrElse(false)
  }

  /**
   * 关闭连接
   */
  def close(): Unit = {
    Try {
      clientOpt.foreach(_.disconnect)
      clientOpt = None
      println("Redis连接已关闭")
    }.recover {
      case e: Exception =>
        println(s"关闭Redis连接时出错: ${e.getMessage}")
    }
  }
}