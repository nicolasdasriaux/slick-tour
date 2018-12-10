package slicktour.ecommerce.db

import ExtendedPostgresProfile.api._

class Items(tag: Tag) extends Table[Item](tag, "itemss") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name")
  override def * = (id.?, name) <> ((Item.apply _).tupled, Item.unapply)
}

object Items {
  val table = TableQuery[Items]
}
