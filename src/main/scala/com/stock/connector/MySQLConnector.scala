package com.stock.connector

import slick.jdbc.MySQLProfile.api._
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try

/**
 * MySQL数据库连接管理（使用硬编码配置，避免配置文件冲突）
 */
object MySQLConnector {

  // 数据库配置（默认占位符，真实配置请通过 application.conf 或环境变量覆盖）
  private val DB_URL = "jdbc:mysql://localhost:3306/stock_analysis?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8"
  private val DB_USER = "stock_user"
  private val DB_PASSWORD = "CHANGE_ME"

  // 创建数据库连接
  lazy val db: Database = {
    Database.forURL(
      url = DB_URL,
      user = DB_USER,
      password = DB_PASSWORD,
      driver = "com.mysql.cj.jdbc.Driver"
    )
  }

  /**
   * 执行数据库操作（同步）
   */
  def runSync[T](action: DBIO[T], timeout: Duration = 10.seconds): T = {
    Await.result(db.run(action), timeout)
  }

  /**
   * 执行数据库操作（异步）
   */
  def runAsync[T](action: DBIO[T]): Future[T] = {
    db.run(action)
  }

  /**
   * 测试数据库连接
   */
  def testConnection(): Boolean = {
    Try {
      val result = runSync(sql"SELECT 1".as[Int])
      result.headOption.contains(1)
    }.getOrElse(false)
  }

  /**
   * 关闭数据库连接
   */
  def close(): Unit = {
    db.close()
  }
}