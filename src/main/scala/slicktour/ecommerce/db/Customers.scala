package slicktour.ecommerce.db

import ExtendedPostgresProfile.api._

class Customers(tag: Tag) extends Table[Customer](tag, "customers") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def firstName = column[String]("first_name")
  def lastName = column[String]("last_name")
  def * = (id.?, firstName, lastName).mapTo[Customer]

  def fullName = firstName ++ " " ++ lastName // Calculated field
}

object Customers {
  val table = TableQuery[Customers]
}
