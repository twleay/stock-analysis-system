package com.stock.api

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.headers._
import com.stock.connector.{MySQLConnector, RedisConnector}
import com.stock.config.AppConfig
import scala.concurrent.ExecutionContextExecutor
import scala.io.StdIn

/**
 * REST API服务器
 */
object ApiServer {

  def main(args: Array[String]): Unit = {
    // 1. 初始化 Actor System
    implicit val system: ActorSystem = ActorSystem("stock-api-system")
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher

    println("=" * 60)
    println("股票分析API服务器启动中...")
    println("=" * 60)

    // 2. 测试数据库连接
    println("\n检查数据库连接...")
    if (MySQLConnector.testConnection()) {
      println(" MySQL连接成功")
    } else {
      println(" MySQL连接失败 (请检查 application.conf 或 数据库状态)")
    }

    if (RedisConnector.testConnection()) {
      println("Redis连接成功")
    } else {
      println("Redis连接失败 (实时数据可能无法读取)")
    }

    // 3. 获取原始路由
    val routes: Route = StockApiRoutes.routes


    // 允许前端 (localhost:5173) 访问后端 (localhost:8080)
    val corsHeaders = List(
      `Access-Control-Allow-Origin`.*, // 允许所有来源 (开发环境用)
      `Access-Control-Allow-Credentials`(true),
      `Access-Control-Allow-Headers`("Authorization", "Content-Type", "X-Requested-With")
    )

    def corsHandler(r: Route): Route = {
      // 处理预检请求 (OPTIONS) - 浏览器在发送 POST/PUT 前会先发这个
      val preflightRequestHandler: Route = options {
        complete(akka.http.scaladsl.model.HttpResponse(akka.http.scaladsl.model.StatusCodes.OK).withHeaders(
          `Access-Control-Allow-Methods`(OPTIONS, POST, PUT, GET, DELETE) :: corsHeaders
        ))
      }
      // 包装正常请求，加上允许跨域的头
      respondWithHeaders(corsHeaders) {
        preflightRequestHandler ~ r
      }
    }

    // 5. 将路由包裹在 CORS 处理器中
    val finalRoutes = corsHandler(routes)

    // 6. 启动 HTTP 服务器
    // 使用配置文件中的 host/port，如果没有配置则默认 0.0.0.0:8080
    val host = "0.0.0.0"
    val port = 8080

    val bindingFuture = Http().newServerAt(host, port).bind(finalRoutes)

    // 7. 打印控制台信息 (你的仪表盘)
    println(s"\nAPI服务器已启动")
    println(s"   地址: http://$host:$port")
    println("\n可用的API端点：")
    println("=" * 60)
    println("【基础接口】")
    println("  GET  /api/health                      - 健康检查")
    println("  GET  /api/stocks                      - 获取所有股票")
    println("  GET  /api/stocks/{code}               - 获取指定股票信息")
    println("\n【实时行情】")
    println("  GET  /api/realtime/{code}             - 获取指定股票实时行情")
    println("\n【K线数据】")
    println("  GET  /api/kline/{code}?limit=60       - 获取最近N条K线")
    println("\n【技术指标】")
    println("  GET  /api/indicators/{code}/latest    - 获取最新技术指标")
    println("  GET  /api/indicators/{code}?start=... - 获取历史指标")
    println("\n【异常告警】")
    println("  GET  /api/anomalies?stockCode={code}  - 获取异常记录")
    println("=" * 60)
    println("\n按 ENTER 键停止服务器...")

    // 8. 等待退出
    StdIn.readLine()

    bindingFuture
      .flatMap(_.unbind())
      .onComplete { _ =>
        println("\n正在关闭服务器...")
        MySQLConnector.close()
        RedisConnector.close()
        system.terminate()
        println("服务器已停止")
      }
  }
}