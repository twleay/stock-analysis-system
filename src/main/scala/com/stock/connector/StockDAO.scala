package com.stock.connector

import com.stock.model._
import java.sql.{Connection, DriverManager, PreparedStatement, ResultSet, Timestamp, Date}
import java.time.LocalDate
import scala.util.{Try, Using}
import scala.collection.mutable.ListBuffer

/**
 * 股票数据访问对象 - 使用纯JDBC实现
 */
object StockDAO {

  // 获取数据库连接
  private def getConnection: Connection = {
    Class.forName("com.mysql.cj.jdbc.Driver")
    DriverManager.getConnection(
      "jdbc:mysql://192.168.202.130:3306/stock_analysis?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8",
      "stock_user",
      "123456"
    )
  }

  // ========== 股票基本信息操作 ==========

  /**
   * 获取所有股票
   */
  def getAllStocks(): List[Stock] = {
    Using.resource(getConnection) { conn =>
      val stmt = conn.createStatement()
      val rs = stmt.executeQuery("SELECT stock_code, stock_name, market, sector FROM stocks")

      val result = ListBuffer[Stock]()
      while (rs.next()) {
        result += Stock(
          stockCode = rs.getString("stock_code"),
          stockName = rs.getString("stock_name"),
          market = rs.getString("market"),
          sector = Option(rs.getString("sector"))
        )
      }
      result.toList
    }
  }

  /**
   * 根据代码获取股票
   */
  def getStockByCode(stockCode: String): Option[Stock] = {
    Using.resource(getConnection) { conn =>
      val stmt = conn.prepareStatement(
        "SELECT stock_code, stock_name, market, sector FROM stocks WHERE stock_code = ?"
      )
      stmt.setString(1, stockCode)
      val rs = stmt.executeQuery()

      if (rs.next()) {
        Some(Stock(
          stockCode = rs.getString("stock_code"),
          stockName = rs.getString("stock_name"),
          market = rs.getString("market"),
          sector = Option(rs.getString("sector"))
        ))
      } else {
        None
      }
    }
  }

  /**
   * 插入或更新股票
   */
  def insertStock(stock: Stock): Int = {
    Using.resource(getConnection) { conn =>
      val sql = """
        INSERT INTO stocks (stock_code, stock_name, market, sector)
        VALUES (?, ?, ?, ?)
        ON DUPLICATE KEY UPDATE
        stock_name = VALUES(stock_name),
        market = VALUES(market),
        sector = VALUES(sector)
      """
      val stmt = conn.prepareStatement(sql)
      stmt.setString(1, stock.stockCode)
      stmt.setString(2, stock.stockName)
      stmt.setString(3, stock.market)
      stmt.setString(4, stock.sector.orNull)
      stmt.executeUpdate()
    }
  }

  /**
   * 批量插入股票
   */
  def insertStocks(stockList: List[Stock]): Int = {
    Using.resource(getConnection) { conn =>
      val sql = """
        INSERT INTO stocks (stock_code, stock_name, market, sector)
        VALUES (?, ?, ?, ?)
        ON DUPLICATE KEY UPDATE
        stock_name = VALUES(stock_name),
        market = VALUES(market),
        sector = VALUES(sector)
      """
      val stmt = conn.prepareStatement(sql)

      var count = 0
      stockList.foreach { stock =>
        stmt.setString(1, stock.stockCode)
        stmt.setString(2, stock.stockName)
        stmt.setString(3, stock.market)
        stmt.setString(4, stock.sector.orNull)
        stmt.addBatch()
      }

      val results = stmt.executeBatch()
      results.sum
    }
  }

  /**
   * 删除股票
   */
  def deleteStock(stockCode: String): Int = {
    Using.resource(getConnection) { conn =>
      val stmt = conn.prepareStatement("DELETE FROM stocks WHERE stock_code = ?")
      stmt.setString(1, stockCode)
      stmt.executeUpdate()
    }
  }

  // ========== K线数据操作 ==========

  /**
   * 插入K线数据
   */
  def insertKLineData(data: KLineData): Int = {
    Using.resource(getConnection) { conn =>
      val sql = """
        INSERT INTO kline_data (stock_code, trade_date, open_price, high_price, low_price, close_price, volume, amount)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        ON DUPLICATE KEY UPDATE
        open_price = VALUES(open_price),
        high_price = VALUES(high_price),
        low_price = VALUES(low_price),
        close_price = VALUES(close_price),
        volume = VALUES(volume),
        amount = VALUES(amount)
      """
      val stmt = conn.prepareStatement(sql)
      stmt.setString(1, data.stockCode)
      stmt.setDate(2, Date.valueOf(data.tradeDate))
      stmt.setDouble(3, data.openPrice)
      stmt.setDouble(4, data.highPrice)
      stmt.setDouble(5, data.lowPrice)
      stmt.setDouble(6, data.closePrice)
      stmt.setLong(7, data.volume)
      data.amount match {
        case Some(amt) => stmt.setDouble(8, amt)
        case None => stmt.setNull(8, java.sql.Types.DOUBLE)
      }
      stmt.executeUpdate()
    }
  }

  /**
   * 批量插入K线数据
   */
  def insertKLineDataBatch(dataList: List[KLineData]): Int = {
    Using.resource(getConnection) { conn =>
      val sql = """
        INSERT INTO kline_data (stock_code, trade_date, open_price, high_price, low_price, close_price, volume, amount)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        ON DUPLICATE KEY UPDATE
        open_price = VALUES(open_price),
        high_price = VALUES(high_price),
        low_price = VALUES(low_price),
        close_price = VALUES(close_price),
        volume = VALUES(volume),
        amount = VALUES(amount)
      """
      val stmt = conn.prepareStatement(sql)

      dataList.foreach { data =>
        stmt.setString(1, data.stockCode)
        stmt.setDate(2, Date.valueOf(data.tradeDate))
        stmt.setDouble(3, data.openPrice)
        stmt.setDouble(4, data.highPrice)
        stmt.setDouble(5, data.lowPrice)
        stmt.setDouble(6, data.closePrice)
        stmt.setLong(7, data.volume)
        data.amount match {
          case Some(amt) => stmt.setDouble(8, amt)
          case None => stmt.setNull(8, java.sql.Types.DOUBLE)
        }
        stmt.addBatch()
      }

      val results = stmt.executeBatch()
      results.sum
    }
  }

  /**
   * 获取指定股票指定日期范围的K线数据
   */
  def getKLineData(stockCode: String, startDate: LocalDate, endDate: LocalDate): List[KLineData] = {
    Using.resource(getConnection) { conn =>
      val sql = """
        SELECT stock_code, trade_date, open_price, high_price, low_price, close_price, volume, amount
        FROM kline_data
        WHERE stock_code = ? AND trade_date >= ? AND trade_date <= ?
        ORDER BY trade_date ASC
      """
      val stmt = conn.prepareStatement(sql)
      stmt.setString(1, stockCode)
      stmt.setDate(2, Date.valueOf(startDate))
      stmt.setDate(3, Date.valueOf(endDate))
      val rs = stmt.executeQuery()

      val result = ListBuffer[KLineData]()
      while (rs.next()) {
        result += KLineData(
          stockCode = rs.getString("stock_code"),
          tradeDate = rs.getDate("trade_date").toLocalDate,
          openPrice = rs.getDouble("open_price"),
          highPrice = rs.getDouble("high_price"),
          lowPrice = rs.getDouble("low_price"),
          closePrice = rs.getDouble("close_price"),
          volume = rs.getLong("volume"),
          amount = Option(rs.getDouble("amount")).filter(_ => !rs.wasNull())
        )
      }
      result.toList
    }
  }

  /**
   * 获取最新N条K线数据
   */
  def getLatestKLineData(stockCode: String, limit: Int): List[KLineData] = {
    Using.resource(getConnection) { conn =>
      val sql = """
        SELECT stock_code, trade_date, open_price, high_price, low_price, close_price, volume, amount
        FROM kline_data
        WHERE stock_code = ?
        ORDER BY trade_date DESC
        LIMIT ?
      """
      val stmt = conn.prepareStatement(sql)
      stmt.setString(1, stockCode)
      stmt.setInt(2, limit)
      val rs = stmt.executeQuery()

      val result = ListBuffer[KLineData]()
      while (rs.next()) {
        result += KLineData(
          stockCode = rs.getString("stock_code"),
          tradeDate = rs.getDate("trade_date").toLocalDate,
          openPrice = rs.getDouble("open_price"),
          highPrice = rs.getDouble("high_price"),
          lowPrice = rs.getDouble("low_price"),
          closePrice = rs.getDouble("close_price"),
          volume = rs.getLong("volume"),
          amount = Option(rs.getDouble("amount")).filter(_ => !rs.wasNull())
        )
      }
      result.reverse.toList // 反转为升序
    }
  }

  /**
   * 获取最新一条K线数据
   */
  def getLatestKLine(stockCode: String): Option[KLineData] = {
    val results = getLatestKLineData(stockCode, 1)
    results.headOption
  }

  /**
   * 删除指定日期之前的K线数据
   */
  def deleteKLineDataBefore(stockCode: String, beforeDate: LocalDate): Int = {
    Using.resource(getConnection) { conn =>
      val stmt = conn.prepareStatement(
        "DELETE FROM kline_data WHERE stock_code = ? AND trade_date < ?"
      )
      stmt.setString(1, stockCode)
      stmt.setDate(2, Date.valueOf(beforeDate))
      stmt.executeUpdate()
    }
  }

  // ========== 技术指标操作 ==========

  /**
   * 插入技术指标
   */
  def insertTechnicalIndicators(indicators: TechnicalIndicators): Int = {
    Using.resource(getConnection) { conn =>
      val sql = """
        INSERT INTO technical_indicators
        (stock_code, trade_date, macd, macd_signal, macd_hist, kdj_k, kdj_d, kdj_j,
         rsi_6, rsi_12, rsi_24, ma5, ma10, ma20, ma60)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON DUPLICATE KEY UPDATE
        macd = VALUES(macd), macd_signal = VALUES(macd_signal), macd_hist = VALUES(macd_hist),
        kdj_k = VALUES(kdj_k), kdj_d = VALUES(kdj_d), kdj_j = VALUES(kdj_j),
        rsi_6 = VALUES(rsi_6), rsi_12 = VALUES(rsi_12), rsi_24 = VALUES(rsi_24),
        ma5 = VALUES(ma5), ma10 = VALUES(ma10), ma20 = VALUES(ma20), ma60 = VALUES(ma60)
      """
      val stmt = conn.prepareStatement(sql)
      stmt.setString(1, indicators.stockCode)
      stmt.setDate(2, Date.valueOf(indicators.tradeDate))

      def setDoubleOrNull(index: Int, value: Option[Double]): Unit = {
        value match {
          case Some(v) => stmt.setDouble(index, v)
          case None => stmt.setNull(index, java.sql.Types.DOUBLE)
        }
      }

      setDoubleOrNull(3, indicators.macd)
      setDoubleOrNull(4, indicators.macdSignal)
      setDoubleOrNull(5, indicators.macdHist)
      setDoubleOrNull(6, indicators.kdjK)
      setDoubleOrNull(7, indicators.kdjD)
      setDoubleOrNull(8, indicators.kdjJ)
      setDoubleOrNull(9, indicators.rsi6)
      setDoubleOrNull(10, indicators.rsi12)
      setDoubleOrNull(11, indicators.rsi24)
      setDoubleOrNull(12, indicators.ma5)
      setDoubleOrNull(13, indicators.ma10)
      setDoubleOrNull(14, indicators.ma20)
      setDoubleOrNull(15, indicators.ma60)

      stmt.executeUpdate()
    }
  }

  /**
   * 获取技术指标
   */
  def getTechnicalIndicators(stockCode: String, tradeDate: LocalDate): Option[TechnicalIndicators] = {
    Using.resource(getConnection) { conn =>
      val sql = """
        SELECT stock_code, trade_date, macd, macd_signal, macd_hist, kdj_k, kdj_d, kdj_j,
               rsi_6, rsi_12, rsi_24, ma5, ma10, ma20, ma60
        FROM technical_indicators
        WHERE stock_code = ? AND trade_date = ?
      """
      val stmt = conn.prepareStatement(sql)
      stmt.setString(1, stockCode)
      stmt.setDate(2, Date.valueOf(tradeDate))
      val rs = stmt.executeQuery()

      def getDoubleOption(col: String): Option[Double] = {
        val value = rs.getDouble(col)
        if (rs.wasNull()) None else Some(value)
      }

      if (rs.next()) {
        Some(TechnicalIndicators(
          stockCode = rs.getString("stock_code"),
          tradeDate = rs.getDate("trade_date").toLocalDate,
          macd = getDoubleOption("macd"),
          macdSignal = getDoubleOption("macd_signal"),
          macdHist = getDoubleOption("macd_hist"),
          kdjK = getDoubleOption("kdj_k"),
          kdjD = getDoubleOption("kdj_d"),
          kdjJ = getDoubleOption("kdj_j"),
          rsi6 = getDoubleOption("rsi_6"),
          rsi12 = getDoubleOption("rsi_12"),
          rsi24 = getDoubleOption("rsi_24"),
          ma5 = getDoubleOption("ma5"),
          ma10 = getDoubleOption("ma10"),
          ma20 = getDoubleOption("ma20"),
          ma60 = getDoubleOption("ma60")
        ))
      } else {
        None
      }
    }
  }

  // ========== 统计查询 ==========

  /**
   * 获取数据库中的股票数量
   */
  def getStockCount(): Int = {
    Using.resource(getConnection) { conn =>
      val stmt = conn.createStatement()
      val rs = stmt.executeQuery("SELECT COUNT(*) as cnt FROM stocks")
      if (rs.next()) rs.getInt("cnt") else 0
    }
  }

  /**
   * 获取指定股票的K线数据数量
   */
  def getKLineCount(stockCode: String): Int = {
    Using.resource(getConnection) { conn =>
      val stmt = conn.prepareStatement("SELECT COUNT(*) as cnt FROM kline_data WHERE stock_code = ?")
      stmt.setString(1, stockCode)
      val rs = stmt.executeQuery()
      if (rs.next()) rs.getInt("cnt") else 0
    }
  }

  /**
   * 测试连接
   */
  def testConnection(): Boolean = {
    Try {
      Using.resource(getConnection) { conn =>
        println(">>> MySQL URL:", conn.getMetaData.getURL)
        conn.isValid(5)
      }
    }.recover {
      case e: Exception =>
        println("❌ MySQL 连接异常:")
        e.printStackTrace()
        false
    }.get
  }

}