package slicktour.ecommerce.service.order

import slicktour.ecommerce.db

object OrderMapper {
  /**
    * Difficulty: **
    * Hints:
    * - Use provided JoinMapper.mapOneToMany
    */
  def map(dbOrder: db.Order, dbOrderLinesAndItems: Seq[(db.OrderLine, db.Item)]): Order = ???
}
