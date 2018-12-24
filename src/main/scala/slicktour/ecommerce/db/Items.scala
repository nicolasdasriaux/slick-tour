package slicktour.ecommerce.db

import ExtendedPostgresProfile.api._

class Items(tag: Tag) extends Table[Item](tag, "items") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name")
  def * = (id.?, name).mapTo[Item]
}

object Items {
  val table = TableQuery[Items]
}
