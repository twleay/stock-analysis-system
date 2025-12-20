name := "stock-analysis-system"
version := "1.0"
scalaVersion := "2.13.12"

val AkkaVersion = "2.8.5"
val AkkaHttpVersion = "10.5.3"

// 添加国内Maven仓库加速
resolvers ++= Seq(
  "Aliyun Maven" at "https://maven.aliyun.com/repository/public",
  "Huawei Maven" at "https://repo.huaweicloud.com/repository/maven/"
)

libraryDependencies ++= Seq(
  // Akka核心
  "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,

  // Kafka
  "com.typesafe.akka" %% "akka-stream-kafka" % "4.0.2",
  "org.apache.kafka" % "kafka-clients" % "3.6.0",

  // 数据库
  "mysql" % "mysql-connector-java" % "8.0.33",
  "com.typesafe.slick" %% "slick" % "3.4.1",
  "com.typesafe.slick" %% "slick-hikaricp" % "3.4.1",

  // Redis
  "net.debasishg" %% "redisclient" % "3.42",

  // JSON处理
  "com.typesafe.akka" %% "akka-http-spray-json" % AkkaHttpVersion,
  "io.spray" %% "spray-json" % "1.3.6",

  // HTTP客户端
  "com.softwaremill.sttp.client3" %% "core" % "3.9.1",
  "com.softwaremill.sttp.client3" %% "akka-http-backend" % "3.9.1",

  // 配置文件
  "com.typesafe" % "config" % "1.4.3",

  // 日志
  "com.typesafe.akka" %% "akka-slf4j" % AkkaVersion,
  "ch.qos.logback" % "logback-classic" % "1.4.11",

  // 测试
  "com.typesafe.akka" %% "akka-stream-testkit" % AkkaVersion % Test,
  "org.scalatest" %% "scalatest" % "3.2.17" % Test
)
