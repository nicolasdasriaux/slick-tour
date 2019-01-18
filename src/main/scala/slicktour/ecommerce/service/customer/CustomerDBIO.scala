package slicktour.ecommerce.service.customer

// import slicktour.ecommerce.db
import slicktour.ecommerce.db.ExtendedPostgresProfile.api._

import scala.concurrent.ExecutionContext

class CustomerDBIO(implicit executionContext: ExecutionContext) {
  def findAll(): DBIO[Seq[Customer]] = ???
  def find(id: Long): DBIO[Option[Customer]] = ???
  def insert(customer: CustomerPost): DBIO[Customer] = ???
  def update(customer: Customer): DBIO[Int] = ???
}
