package slicktour.example

import com.typesafe.config.ConfigFactory
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile

object SlickApp {
  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load()
    val databaseConfig = DatabaseConfig.forConfig[JdbcProfile]("ecommerce.database", config)
    val database = databaseConfig.db

    try {
    } finally database.close()
  }
}
