package slicktour.ecommerce.db

case class OrderLine(id: Option[Long], orderId: Long, itemId: Long, quantity: Int)
