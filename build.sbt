import scala.sys.process._

lazy val resetDb = TaskKey[Unit]("resetDb")

lazy val root = (project in file("."))
  .settings(
    organization := "slicktour",
    name := "slick-tour",
    version := "0.1",
    scalaVersion := "2.12.8",

    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http" % "10.1.5",
      "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.5",
      "com.typesafe.akka" %% "akka-stream" % "2.5.19",
      "com.typesafe.akka" %% "akka-actor" % "2.5.19",
      "com.typesafe.akka" %% "akka-slf4j" % "2.5.19",

      "com.typesafe.slick" %% "slick" % "3.2.3",
      "com.typesafe.slick" %% "slick-hikaricp" % "3.2.3",

      "com.github.tminglei" %% "slick-pg" % "0.16.3",
      "com.github.tminglei" %% "slick-pg_spray-json" % "0.16.3",
      "org.postgresql" % "postgresql" % "42.2.5",

      "com.typesafe" % "config" % "1.3.3",
      "com.github.kxbmap" %% "configs" % "0.4.4",

      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
      "ch.qos.logback" % "logback-classic" % "1.2.3"
    ),

    reStart / mainClass := Some("slicktour.ecommerce.api.ECommerceApiApp"),

    resetDb := {
      reStop.value
      "bash db/reset-db.sh".!
    },
  )
