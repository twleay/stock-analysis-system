package com.stock.fetcher

import akka.actor.ActorSystem
import akka.http.scaladsl.{ConnectionContext, Http} // ✅ 修正点1：引入 ConnectionContext
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.stock.connector.StockDAO
import com.stock.model.KLineData
import spray.json._
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.net.ssl.SSLContext
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object HistoryFetcher {

  case class HistoryResponse(data: Option[HistoryData])
  case class HistoryData(klines: List[String])

  object HistoryJsonProtocol extends DefaultJsonProtocol {
    implicit val historyDataFormat: RootJsonFormat[HistoryData] = jsonFormat1(HistoryData)
    implicit val historyResponseFormat: RootJsonFormat[HistoryResponse] = jsonFormat1(HistoryResponse)
  }

  import HistoryJsonProtocol._

  def initHistory(stockCode: String)(implicit system: ActorSystem, ec: ExecutionContext): Future[Unit] = {
    val secId = if (stockCode.startsWith("sh")) s"1.${stockCode.drop(2)}" else s"0.${stockCode.drop(2)}"

    // 东财历史 K 线接口 (HTTPS)
    val url = s"https://push2his.eastmoney.com/api/qt/stock/kline/get?secid=$secId&fields1=f1&fields2=f51,f52,f53,f54,f55,f56,f57&klt=101&fqt=1&end=20500101&lmt=365"

    println(s"[History] 开始抓取 $stockCode 历史数据: $url")

    // 配置宽松的 SSL 上下文 (忽略证书验证)
    val badSslContext = SSLContext.getInstance("TLS")
    badSslContext.init(null, null, null)

    // 修正点2：使用 ConnectionContext.https 创建上下文
    val httpsContext = ConnectionContext.https(badSslContext)

    // 发送请求
    val request = Http().singleRequest(HttpRequest(uri = url), connectionContext = httpsContext)

    request.flatMap { response =>
      if (response.status.isSuccess()) {
        Unmarshal(response.entity).to[String].map { jsonStr =>
          try {
            val json = jsonStr.parseJson.convertTo[HistoryResponse]
            json.data match {
              case Some(data) =>
                val klines = data.klines.map { line =>
                  val parts = line.split(",")
                  KLineData(
                    stockCode = stockCode,
                    tradeDate = LocalDate.parse(parts(0), DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                    openPrice = parts(1).toDouble,
                    closePrice = parts(2).toDouble,
                    highPrice = parts(3).toDouble,
                    lowPrice = parts(4).toDouble,
                    volume = parts(5).toLong,
                    amount = Some(parts(6).toDouble)
                  )
                }
                println(s"[History] $stockCode 解析成功，正在写入 ${klines.length} 条数据...")
                klines.foreach(k => StockDAO.insertKLineData(k))
                println(s"[History] $stockCode 历史数据初始化完成！")
              case None =>
                println(s"[History] $stockCode 接口返回成功但无数据")
            }
          } catch {
            case e: Exception =>
              println(s"[History] $stockCode JSON解析失败: ${e.getMessage}")
          }
        }
      } else {
        println(s"[History] $stockCode 请求失败，状态码: ${response.status}")
        Future.successful(())
      }
    }.recover {
      case e: Exception =>
        println(s"[History] $stockCode 网络请求失败: ${e.getMessage}")
    }
  }
}