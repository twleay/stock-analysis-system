package com.stock.test

import sttp.client3._

object ApiTest extends App {

  println("=" * 60)
  println("API测试")
  println("=" * 60)

  val backend = HttpURLConnectionBackend()
  val baseUrl = "http://localhost:8080/api"

  def testEndpoint(name: String, url: String): Unit = {
    println(s"\n测试: $name")
    println(s"URL: $url")

    try {
      val request = basicRequest.get(uri"$url")
      val response = request.send(backend)

      response.body match {
        case Right(body) =>
          println(s"✅ 成功 (${response.code})")
          // 只显示前200个字符
          val preview = if (body.length > 200) body.take(200) + "..." else body
          println(s"响应: $preview")
        case Left(error) =>
          println(s"❌ 失败: $error")
      }
    } catch {
      case e: Exception =>
        println(s"❌ 错误: ${e.getMessage}")
    }
  }

  // 测试各个端点
  testEndpoint("健康检查", s"$baseUrl/health")
  testEndpoint("获取股票列表", s"$baseUrl/stocks")
  testEndpoint("获取指定股票", s"$baseUrl/stocks/sh600000")
  testEndpoint("获取实时行情", s"$baseUrl/realtime/sh600000")
  testEndpoint("获取K线数据", s"$baseUrl/kline/sh600000?limit=10")
  testEndpoint("获取最新技术指标", s"$baseUrl/indicators/sh600000/latest")
  testEndpoint("获取异常记录", s"$baseUrl/anomalies?stockCode=sh600000&limit=10")

  backend.close()

  println("\n" + "=" * 60)
  println("测试完成")
  println("=" * 60)
}