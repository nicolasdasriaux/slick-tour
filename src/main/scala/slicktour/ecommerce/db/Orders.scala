package slicktour.ecommerce.db

import ExtendedPostgresProfile.api._

class Orders(tag: Tag) extends Table[Order](tag, "orders") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def customerId = column[Long]("customer_id")
  def * = (id.?, customerId) <> ((Order.apply _).tupled, Order.unapply)

  def customer = foreignKey("fk_orders_customer_id", customerId, Customers.table)(_.id)
}

object Orders {
  val table = TableQuery[Orders]
}
