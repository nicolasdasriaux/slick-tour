package slicktour.ecommerce.db

import ExtendedPostgresProfile.api._

class OrderLines(tag: Tag) extends Table[OrderLine](tag, "order_lines") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def orderId = column[Long]("order_id")
  def itemId = column[Long]("item_id")
  def quantity = column[Int]("quantity")
  def * = (id.?, orderId, itemId, quantity).mapTo[OrderLine]

  def order = foreignKey("fk_order_lines_order_id", orderId, Orders.table)(_.id)
  def item = foreignKey("fk_order_lines_item_id", itemId, Items.table)(_.id)
  def orderIdItemIdIndex = index("idx_order_lines_order_id_item_id", (orderId, itemId), unique = true)
}

object OrderLines {
  val table = TableQuery[OrderLines]
}
