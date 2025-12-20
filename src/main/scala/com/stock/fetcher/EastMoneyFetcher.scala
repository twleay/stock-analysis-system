package com.stock.fetcher

import sttp.client3._
import scala.util.{Try, Success, Failure}
import com.stock.model.RealtimeQuote
import spray.json._

/**
 * 东方财富网数据采集器
 */
object EastMoneyFetcher {

  private val backend = HttpURLConnectionBackend()

  /**
   * 转换股票代码格式
   * sh600000 -> 1.600000
   * sz000001 -> 0.000001
   */
  private def convertStockCode(code: String): String = {
    if (code.startsWith("sh")) {
      s"1.${code.substring(2)}"
    } else if (code.startsWith("sz")) {
      s"0.${code.substring(2)}"
    } else {
      code
    }
  }

  /**
   * 获取实时行情
   */
  def fetchRealtimeQuote(stockCode: String): Option[RealtimeQuote] = {
    val secid = convertStockCode(stockCode)
    val url = s"http://push2.eastmoney.com/api/qt/stock/get?secid=$secid&fields=f43,f44,f45,f46,f47,f48,f49,f50,f51,f52,f57,f58,f60,f107,f152,f162,f169,f170,f171"

    Try {
      val request = basicRequest
        .get(uri"$url")
        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
        .header("Referer", "http://quote.eastmoney.com/")

      val response = request.send(backend)

      response.body match {
        case Right(body) =>
          parseEastMoneyData(stockCode, body)
        case Left(error) =>
          println(s"获取 $stockCode 数据失败: $error")
          None
      }
    }.getOrElse(None)
  }

  /**
   * 解析东方财富返回的JSON数据
   */
  private def parseEastMoneyData(stockCode: String, jsonStr: String): Option[RealtimeQuote] = {
    Try {
      val json = jsonStr.parseJson.asJsObject
      val data = json.fields.get("data").map(_.asJsObject).getOrElse(return None)

      def getField(field: String): Double = {
        data.fields.get(field).map {
          case JsNumber(n) => n.toDouble
          case JsString(s) => s.toDouble
          case _ => 0.0
        }.getOrElse(0.0)
      }

      val currentPrice = getField("f43") / 100.0  // 最新价（需要除以100）
      if (currentPrice <= 0) {
        println(s"$stockCode 当前无交易")
        return None
      }

      Some(RealtimeQuote(
        stockCode = stockCode,
        timestamp = System.currentTimeMillis(),
        currentPrice = currentPrice,
        openPrice = getField("f46") / 100.0,
        highPrice = getField("f44") / 100.0,
        lowPrice = getField("f45") / 100.0,
        closePrice = getField("f60") / 100.0,  // 昨收
        volume = getField("f47").toLong,
        amount = getField("f48"),
        bidPrice1 = getField("f49") / 100.0,
        bidVolume1 = getField("f50").toLong,
        askPrice1 = getField("f51") / 100.0,
        askVolume1 = getField("f52").toLong
      ))
    } match {
      case Success(quote) => quote
      case Failure(e) =>
        println(s"解析 $stockCode 数据失败: ${e.getMessage}")
        println(s"原始数据: ${jsonStr.take(200)}")
        None
    }
  }

  /**
   * 批量获取
   */
  def fetchRealtimeQuotes(stockCodes: List[String]): List[RealtimeQuote] = {
    stockCodes.flatMap { code =>
      val result = fetchRealtimeQuote(code)
      Thread.sleep(100)  // 避免请求过快
      result
    }
  }

  def close(): Unit = {
    backend.close()
  }
}