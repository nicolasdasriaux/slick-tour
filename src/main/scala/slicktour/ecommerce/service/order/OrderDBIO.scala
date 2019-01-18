package slicktour.ecommerce.service.order

import slicktour.ecommerce.db
import slicktour.ecommerce.db.ExtendedPostgresProfile.api._

import scala.concurrent.ExecutionContext

class OrderDBIO(implicit executionContext: ExecutionContext) {
  /**
    * Difficulty: ***
    * Hints:
    * - Find db.Order by id in db.Orders table
    * - Find all db.OrderLine by orderId in b.OrderLines table
    *   joining db.OrderLines and db.Items tables
    *   sorting by orderLineId
    */
  def findDb(id: Long): DBIO[(db.Order, Seq[(db.OrderLine, db.Item)])] = ???

  /**
    * Difficulty: *
    * Hints:
    * - Use findDb
    * - Use OrderMap.map
    */
  def find(id: Long): DBIO[Seq[Order]] = ???
}
