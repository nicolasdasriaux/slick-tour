package slicktour.ecommerce.db

import java.time.LocalDate

import ExtendedPostgresProfile.api._

class Orders(tag: Tag) extends Table[Order](tag, "orders") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def customerId = column[Long]("customer_id")
  def date = column[LocalDate]("date")
  def * = (id.?, customerId, date).mapTo[Order]

  def customer = foreignKey("fk_orders_customer_id", customerId, Customers.table)(_.id)
}

object Orders {
  val table = TableQuery[Orders]
}
