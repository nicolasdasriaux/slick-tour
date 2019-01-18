package slicktour.ecommerce.service.order

import slicktour.ecommerce.db.ExtendedPostgresProfile.api._

import scala.concurrent.{ExecutionContext, Future}

class OrderService(database: Database)(implicit executionContext: ExecutionContext) {
 /**
   * Difficulty: *
   * Hints:
   * - Use OrderDBIO.find
   */
 def find(id: Long): Future[Order] = ???
}
