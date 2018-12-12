package slicktour.ecommerce.db

import com.github.tminglei.slickpg._
import slick.basic.Capability
import slick.jdbc.JdbcCapabilities

trait ExtendedPostgresProfile extends ExPostgresProfile
  with PgDate2Support
  with PgSprayJsonSupport {

  val pgjson: String = "jsonb"
  override protected def computeCapabilities: Set[Capability] = super.computeCapabilities + JdbcCapabilities.insertOrUpdate
  override val api: ExtendedAPI = ExtendedAPI

  trait ExtendedAPI extends API
    with DateTimeImplicits
    with JsonImplicits

  object ExtendedAPI extends ExtendedAPI
}

object ExtendedPostgresProfile extends ExtendedPostgresProfile
