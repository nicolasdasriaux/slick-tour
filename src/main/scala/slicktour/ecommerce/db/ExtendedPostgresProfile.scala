package slicktour.ecommerce.db

import com.github.tminglei.slickpg._
import slick.basic.Capability
import slick.jdbc.JdbcCapabilities

// Extend PostgreSQL profile with java.time and Spray JSON support
trait ExtendedPostgresProfile extends ExPostgresProfile with PgDate2Support with PgSprayJsonSupport {
  val pgjson: String = "jsonb" // jsonb type for JSON column

  // Add 'insert or update' capability
  override protected def computeCapabilities: Set[Capability] =
    super.computeCapabilities + JdbcCapabilities.insertOrUpdate

  override val api: ExtendedAPI = ExtendedAPI
  // Add API support for Date and Time, and JSON
  trait ExtendedAPI extends API with DateTimeImplicits with JsonImplicits
  object ExtendedAPI extends ExtendedAPI
}

object ExtendedPostgresProfile extends ExtendedPostgresProfile
