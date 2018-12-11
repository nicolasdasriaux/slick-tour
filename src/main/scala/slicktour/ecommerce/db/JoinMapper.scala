package slicktour.ecommerce.db

import scala.collection.mutable

object JoinMapper {
  def mapOneToMany[RA, RB, A](dbABs: Seq[(RA, RB)])(mapAB: (RA, Seq[RB]) => A): Seq[A] = {
    if (dbABs.isEmpty) {
      Seq.empty
    } else {
      var dbBs = mutable.ListBuffer[RB]()
      val as = mutable.ListBuffer[A]()
      val it = dbABs.toIterator
      val (dbA, dbB) = it.next

      var currentDbA = dbA
      dbBs += dbB

      for ((dbA, dbB) <- it) {
        if (currentDbA != dbA) {
          as += mapAB(currentDbA, dbBs.toList)
          currentDbA = dbA
          dbBs.clear()
        }

        dbBs += dbB
      }

      as += mapAB(currentDbA, dbBs.toList)
      as.toList
    }
  }

  def mapOneToManyOrNone[RA, RB, A](dbABs: Seq[(RA, Option[RB])])(mapAB: (RA, Seq[RB]) => A): Seq[A] =
    mapOneToMany(dbABs)((dbA, dbBs) => mapAB(dbA, dbBs.flatten))
}
