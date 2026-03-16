package com.stock.api

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.model.{StatusCodes, ContentTypes, HttpEntity}
// import akka.http.scaladsl.model.headers.`Access-Control-Allow-Origin` // 不需要了
import com.stock.connector.{StockDAO, RedisConnector}
import com.stock.model._
import spray.json._
import com.stock.model.StockJsonProtocol._

import java.time.LocalDate

/**
 * 股票API路由
 * (已移除内部CORS配置，交由 ApiServer 全局处理)
 */
object StockApiRoutes {


  /**
   * 所有路由
   */
  def routes: Route = {
    pathPrefix("api") {
      // 移除了 respondWithHeaders(corsHeaders) 包裹
      concat(
        healthRoute,
        stocksRoute,
        realtimeRoute,
        klineRoute,
        indicatorsRoute,
        anomaliesRoute
      )
    }
  }

  /**
   * 创建JSON响应
   */
  private def jsonResponse(json: String): HttpEntity.Strict = {
    HttpEntity(ContentTypes.`application/json`, json)
  }

  /**
   * 创建错误响应
   */
  private def errorResponse(message: String): String = {
    s"""{"error":"$message"}"""
  }

  /**
   * 健康检查
   * GET /api/health
   */
  def healthRoute: Route = {
    path("health") {
      get {
        val response = s"""{"status":"ok","service":"stock-analysis-api","timestamp":${System.currentTimeMillis()}}"""
        complete(StatusCodes.OK, jsonResponse(response))
      }
    }
  }

  /**
   * 获取所有股票列表
   * GET /api/stocks
   */
  def stocksRoute: Route = {
    path("stocks") {
      get {
        try {
          val stocks = StockDAO.getAllStocks()
          complete(StatusCodes.OK, jsonResponse(stocks.toJson.toString))
        } catch {
          case e: Exception =>
            complete(StatusCodes.InternalServerError, jsonResponse(errorResponse(e.getMessage)))
        }
      }
    } ~
      path("stocks" / Segment) { stockCode =>
        get {
          try {
            StockDAO.getStockByCode(stockCode) match {
              case Some(stock) =>
                complete(StatusCodes.OK, jsonResponse(stock.toJson.toString))
              case None =>
                complete(StatusCodes.NotFound, jsonResponse(errorResponse(s"Stock $stockCode not found")))
            }
          } catch {
            case e: Exception =>
              complete(StatusCodes.InternalServerError, jsonResponse(errorResponse(e.getMessage)))
          }
        }
      }
  }

  /**
   * 获取实时行情
   * GET /api/realtime/{stockCode}
   * GET /api/realtime (所有股票)
   */
  def realtimeRoute: Route = {
    pathPrefix("realtime") {
      pathEnd {
        get {
          // 获取所有股票的实时行情
          try {
            val stocks = StockDAO.getAllStocks()
            val quotes = stocks.flatMap { stock =>
              val key = s"realtime:${stock.stockCode}"
              RedisConnector.get(key).flatMap { json =>
                try {
                  Some(json.parseJson.convertTo[RealtimeQuote])
                } catch {
                  case _: Exception => None
                }
              }
            }
            complete(StatusCodes.OK, jsonResponse(quotes.toJson.toString))
          } catch {
            case e: Exception =>
              complete(StatusCodes.InternalServerError, jsonResponse(errorResponse(e.getMessage)))
          }
        }
      } ~
        path(Segment) { stockCode =>
          get {
            try {
              val key = s"realtime:$stockCode"
              RedisConnector.get(key) match {
                case Some(json) =>
                  try {
                    val quote = json.parseJson.convertTo[RealtimeQuote]
                    complete(StatusCodes.OK, jsonResponse(quote.toJson.toString))
                  } catch {
                    case e: Exception =>
                      complete(StatusCodes.InternalServerError, jsonResponse(errorResponse(s"Parse error: ${e.getMessage}")))
                  }
                case None =>
                  complete(StatusCodes.NotFound, jsonResponse(errorResponse(s"No realtime data for $stockCode")))
              }
            } catch {
              case e: Exception =>
                complete(StatusCodes.InternalServerError, jsonResponse(errorResponse(e.getMessage)))
            }
          }
        }
    }
  }

  /**
   * 获取K线数据
   * GET /api/kline/{stockCode}?limit=60
   * GET /api/kline/{stockCode}?start=2024-01-01&end=2024-12-31
   */
  def klineRoute: Route = {
    path("kline" / Segment) { stockCode =>
      get {
        parameters("limit".as[Int].?, "start".?, "end".?) { (limitOpt, startOpt, endOpt) =>
          try {
            val klines = (startOpt, endOpt) match {
              case (Some(start), Some(end)) =>
                // 日期范围查询
                val startDate = LocalDate.parse(start)
                val endDate = LocalDate.parse(end)
                StockDAO.getKLineData(stockCode, startDate, endDate)

              case _ =>
                // 获取最近N条
                val limit = limitOpt.getOrElse(60)
                StockDAO.getLatestKLineData(stockCode, limit)
            }

            complete(StatusCodes.OK, jsonResponse(klines.toJson.toString))
          } catch {
            case e: Exception =>
              complete(StatusCodes.InternalServerError, jsonResponse(errorResponse(e.getMessage)))
          }
        }
      }
    }
  }

  /**
   * 获取技术指标
   * GET /api/indicators/{stockCode}?date=2024-12-20
   * GET /api/indicators/{stockCode}/latest
   */
  def indicatorsRoute: Route = {
    pathPrefix("indicators") {
      path(Segment / "latest") { stockCode =>
        get {
          try {
            // 获取最新的技术指标
            val klines = StockDAO.getLatestKLineData(stockCode, 1)
            if (klines.nonEmpty) {
              val latestDate = klines.head.tradeDate
              StockDAO.getTechnicalIndicators(stockCode, latestDate) match {
                case Some(indicators) =>
                  complete(StatusCodes.OK, jsonResponse(indicators.toJson.toString))
                case None =>
                  complete(StatusCodes.NotFound, jsonResponse(errorResponse(s"No indicators found for $stockCode")))
              }
            } else {
              complete(StatusCodes.NotFound, jsonResponse(errorResponse(s"No data for $stockCode")))
            }
          } catch {
            case e: Exception =>
              complete(StatusCodes.InternalServerError, jsonResponse(errorResponse(e.getMessage)))
          }
        }
      } ~
        path(Segment) { stockCode =>
          get {
            parameters("date".?, "start".?, "end".?) { (dateOpt, startOpt, endOpt) =>
              try {
                val result = (dateOpt, startOpt, endOpt) match {
                  case (Some(date), _, _) =>
                    // 指定日期
                    val targetDate = LocalDate.parse(date)
                    StockDAO.getTechnicalIndicators(stockCode, targetDate).toList

                  case (_, Some(start), Some(end)) =>
                    // 日期范围
                    val startDate = LocalDate.parse(start)
                    val endDate = LocalDate.parse(end)
                    StockDAO.getTechnicalIndicatorsRange(stockCode, startDate, endDate)

                  case _ =>
                    // 默认返回最近30天
                    val endDate = LocalDate.now()
                    val startDate = endDate.minusDays(30)
                    StockDAO.getTechnicalIndicatorsRange(stockCode, startDate, endDate)
                }

                complete(StatusCodes.OK, jsonResponse(result.toJson.toString))
              } catch {
                case e: Exception =>
                  complete(StatusCodes.InternalServerError, jsonResponse(errorResponse(e.getMessage)))
              }
            }
          }
        }
    }
  }

  /**
   * 获取异常记录
   * GET /api/anomalies?stockCode=sh600000&limit=100
   * GET /api/anomalies/recent?limit=50
   */
  def anomaliesRoute: Route = {
    pathPrefix("anomalies") {
      pathEnd {
        get {
          parameters("stockCode".?, "limit".as[Int].?) { (stockCodeOpt, limitOpt) =>
            try {
              val limit = limitOpt.getOrElse(100)

              val anomalies = stockCodeOpt match {
                case Some(stockCode) =>
                  StockDAO.getAnomalyRecords(stockCode, limit)
                case None =>
                  // 返回空列表或实现获取所有异常的逻辑
                  List.empty[AnomalyRecord]
              }

              complete(StatusCodes.OK, jsonResponse(anomalies.toJson.toString))
            } catch {
              case e: Exception =>
                complete(StatusCodes.InternalServerError, jsonResponse(errorResponse(e.getMessage)))
            }
          }
        }
      } ~
        path("recent") {
          get {
            parameters("limit".as[Int].?) { limitOpt =>
              try {
                val limit = limitOpt.getOrElse(50)
                val anomalies = List.empty[AnomalyRecord] // 暂时为空
                complete(StatusCodes.OK, jsonResponse(anomalies.toJson.toString))
              } catch {
                case e: Exception =>
                  complete(StatusCodes.InternalServerError, jsonResponse(errorResponse(e.getMessage)))
              }
            }
          }
        }
    }
  }
}