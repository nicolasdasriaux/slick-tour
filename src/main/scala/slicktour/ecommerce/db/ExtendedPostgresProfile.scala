package slicktour.ecommerce.db

import com.github.tminglei.slickpg._
import slick.basic.Capability
import slick.jdbc.{JdbcCapabilities, JdbcType}

trait ExtendedPostgresProfile extends ExPostgresProfile with array.PgArrayJdbcTypes with PgSprayJsonSupport {
  val pgjson: String = "jsonb"
  override protected def computeCapabilities: Set[Capability] = super.computeCapabilities + JdbcCapabilities.insertOrUpdate
  override val api: ExtendedAPI = ExtendedAPI

  trait ExtendedAPI extends API with JsonImplicits {
    implicit val stringListTypeMapper: JdbcType[List[String]] = new SimpleArrayJdbcType[String]("text").to(_.toList)
  }

  object ExtendedAPI extends ExtendedAPI
}

object ExtendedPostgresProfile extends ExtendedPostgresProfile
