package slicktour.ecommerce.service.customer

import slicktour.ecommerce.db.ExtendedPostgresProfile.api._

import scala.concurrent.{ExecutionContext, Future}

class CustomerService(database: Database)(implicit executionContext: ExecutionContext) {
  val dbio = new CustomerDBIO

  def findAll(): Future[Seq[Customer]] = ???
  def find(id: Long): Future[Option[Customer]] = ???
  def insert(customer: CustomerPost): Future[Customer] = ???
  def update(customer: Customer): Future[Int] = ???
}
