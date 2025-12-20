package com.stock.fetcher

import sttp.client3._
import scala.util.{Try, Success, Failure}
import com.stock.model.RealtimeQuote
import spray.json._
import scala.concurrent.{Future, ExecutionContext}

/**
 * 股票数据采集器 - 从新浪财经API获取实时行情
 */
object StockDataFetcher {

  private val backend = HttpURLConnectionBackend()

  // 辅助方法：安全转换字符串为Double
  private def toDoubleOption(s: String): Option[Double] = Try(s.toDouble).toOption

  // 辅助方法：安全转换字符串为Long
  private def toLongOption(s: String): Option[Long] = Try(s.toLong).toOption

  /**
   * 获取实时行情数据
   * @param stockCode 股票代码，如 sh600000, sz000001
   */
  def fetchRealtimeQuote(stockCode: String): Option[RealtimeQuote] = {
    val url = s"http://hq.sinajs.cn/list=$stockCode"

    Try {
      // 添加请求头模拟浏览器
      val request = basicRequest
        .get(uri"$url")
        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
        .header("Referer", "http://finance.sina.com.cn")
        .header("Accept", "*/*")
        .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
        .header("Connection", "keep-alive")

      val response = request.send(backend)

      response.body match {
        case Right(body) =>
          if (body.trim.isEmpty || body.contains("\"\"")) {
            println(s"$stockCode 返回空数据，可能是非交易时间或股票代码错误")
            None
          } else {
            parseRealtimeData(stockCode, body)
          }
        case Left(error) =>
          println(s"获取 $stockCode 数据失败: $error")
          None
      }
    }.getOrElse(None)
  }

  /**
   * 批量获取实时行情
   */
  def fetchRealtimeQuotes(stockCodes: List[String]): List[RealtimeQuote] = {
    stockCodes.flatMap { code =>
      val result = fetchRealtimeQuote(code)
      // 添加延迟避免请求过快
      Thread.sleep(200)
      result
    }
  }

  /**
   * 批量获取（一次请求多个股票）
   */
  def fetchRealtimeQuotesBatch(stockCodes: List[String]): List[RealtimeQuote] = {
    if (stockCodes.isEmpty) return List.empty

    // 新浪支持一次查询多个股票
    val url = s"http://hq.sinajs.cn/list=${stockCodes.mkString(",")}"

    Try {
      val request = basicRequest
        .get(uri"$url")
        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
        .header("Referer", "http://finance.sina.com.cn")
        .header("Accept", "*/*")
        .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")

      val response = request.send(backend)

      response.body match {
        case Right(body) =>
          // 按行分割，每行是一个股票的数据
          val lines = body.split("\n").filter(_.nonEmpty)
          lines.flatMap { line =>
            // 提取股票代码
            val codePattern = """var hq_str_(\w+)=""".r
            codePattern.findFirstMatchIn(line).flatMap { m =>
              val code = m.group(1)
              parseRealtimeData(code, line)
            }
          }.toList
        case Left(error) =>
          println(s"批量获取数据失败: $error")
          List.empty
      }
    }.getOrElse(List.empty)
  }

  /**
   * 异步获取实时行情
   */
  def fetchRealtimeQuoteAsync(stockCode: String)(implicit ec: ExecutionContext): Future[Option[RealtimeQuote]] = {
    Future {
      fetchRealtimeQuote(stockCode)
    }
  }

  /**
   * 批量异步获取
   */
  def fetchRealtimeQuotesAsync(stockCodes: List[String])(implicit ec: ExecutionContext): Future[List[RealtimeQuote]] = {
    Future.sequence(stockCodes.map(code => fetchRealtimeQuoteAsync(code))).map(_.flatten)
  }

  /**
   * 解析新浪财经返回的数据
   * 返回格式示例: var hq_str_sh600000="浦发银行,10.50,10.40,10.51,10.60,10.20,10.50,10.51,1000000,10500000,..."
   */
  private def parseRealtimeData(stockCode: String, body: String): Option[RealtimeQuote] = {
    Try {
      // 提取引号内的数据
      val pattern = """"(.+?)"""".r
      val dataString = pattern.findFirstMatchIn(body).map(_.group(1)).getOrElse("")

      if (dataString.isEmpty || dataString == "") {
        println(s"$stockCode 无数据")
        return None
      }

      val fields = dataString.split(",")

      if (fields.length < 32) {
        println(s"$stockCode 数据格式不完整，字段数: ${fields.length}")
        return None
      }

      // 检查是否有有效价格数据
      val currentPrice = toDoubleOption(fields(3)).getOrElse(0.0)
      if (currentPrice <= 0) {
        println(s"$stockCode 当前无交易（可能是非交易时间或停牌）")
        return None
      }

      // 解析各个字段
      Some(RealtimeQuote(
        stockCode = stockCode,
        timestamp = System.currentTimeMillis(),
        currentPrice = currentPrice,
        openPrice = toDoubleOption(fields(1)).getOrElse(0.0),
        highPrice = toDoubleOption(fields(4)).getOrElse(0.0),
        lowPrice = toDoubleOption(fields(5)).getOrElse(0.0),
        closePrice = toDoubleOption(fields(2)).getOrElse(0.0),  // 昨收价
        volume = toLongOption(fields(8)).getOrElse(0L),
        amount = toDoubleOption(fields(9)).getOrElse(0.0),
        bidPrice1 = toDoubleOption(fields(6)).getOrElse(0.0),
        bidVolume1 = toDoubleOption(fields(10)).map(v => (v * 100).toLong).getOrElse(0L),
        askPrice1 = toDoubleOption(fields(7)).getOrElse(0.0),
        askVolume1 = toDoubleOption(fields(20)).map(v => (v * 100).toLong).getOrElse(0L)
      ))
    } match {
      case Success(quote) => quote
      case Failure(e) =>
        println(s"解析 $stockCode 数据失败: ${e.getMessage}")
        None
    }
  }

  /**
   * 获取历史K线数据（简单版本）
   */
  def fetchHistoricalKLine(stockCode: String, scale: Int = 240, datalen: Int = 60): Option[String] = {
    // 去掉前缀，如 sh600000 -> 600000
    val code = if (stockCode.length > 6) stockCode.substring(2) else stockCode
    val market = if (stockCode.startsWith("sh")) "sh" else "sz"

    val url = s"http://money.finance.sina.com.cn/quotes_service/api/json_v2.php/CN_MarketData.getKLineData?symbol=$market$code&scale=$scale&datalen=$datalen"

    Try {
      val request = basicRequest
        .get(uri"$url")
        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
        .header("Referer", "http://finance.sina.com.cn")

      val response = request.send(backend)

      response.body match {
        case Right(body) => Some(body)
        case Left(error) =>
          println(s"获取 $stockCode 历史数据失败: $error")
          None
      }
    }.getOrElse(None)
  }

  /**
   * 关闭HTTP客户端
   */
  def close(): Unit = {
    backend.close()
  }
}