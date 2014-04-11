package ci

import org.scalatest.FunSuite
import rx.lang.scala.Observable
import org.scalatest.matchers.ShouldMatchers
import scala.concurrent.ExecutionContext
import concurrent.duration._

class UnseenTest extends FunSuite with ShouldMatchers {
  test("should only provide unseen elements") {
    val unseen = Unseen(Observable.interval(10.millis).take(5).map(_ % 3).map(Seq(_)))
    unseen.toBlockingObservable.toList should be(List(Seq(0),Seq(1),Seq(2), Seq(), Seq()))
  }

  test("should not pass on anything if nothing seen previously") {
    val notFirstBatch = NotFirstBatch(Observable.timer(0.millis, 10.millis).take(5).map(Seq(_)))
    notFirstBatch.toBlockingObservable.toList should be(List(Seq(1),Seq(2), Seq(3), Seq(4)))
  }

  test("should ignore multiple empty elements") {
    val stuff = Seq(Seq(), Seq(), Seq(1,2), Seq(2,3,4), Seq(2,3,4))
    val notFirstBatch = NotFirstBatch(Observable.interval(10.millis).take(5).map(_.toInt).map(stuff(_)))
    notFirstBatch.toBlockingObservable.toList should be(List(Seq(2,3,4), Seq(2,3,4)))
  }

  test("can combine unseen with not first batch") {
    val stuff = Seq(Seq(), Seq(), Seq(1,2), Seq(2,3,4), Seq(2,3,4), Seq(2,3,4,5))
    val notFirstUnseen = NotFirstBatch(Unseen(Observable.timer(0.millis, 10.millis).take(6).map(_.toInt).map(stuff(_))))
    notFirstUnseen.toBlockingObservable.toList should be(List(Seq(3,4), Seq(), Seq(5)))
  }

  test("bounded set obeys contains") {
    val set = BoundedSet[Int](5) + 1 + 2
    Seq(1,2) map (i => set.contains(i) should be (true))
    set.contains(3) should be (false)
  }

  test("bounded set should dump first element if overflows") {
    val set = BoundedSet[Int](5) + 1 + 2 + 3 + 4 + 5 + 6

    Seq(2,3,4,5,6) map (i => set.contains(i) should be (true))
    set.contains(1) should be (false)
  }

  test("bounded set's capacity isn't affected by duplicates") {
    val set = BoundedSet[Int](5) + 1 + 2 + 2 + 2 + 2 + 3

    Seq(1,2,3) map (i => set.contains(i) should be (true))
  }

  test("bounded set should keep the most recent regardless of dupes")  {
    val set = BoundedSet[Int](2) + 1 + 2 + 2 + 2 + 1 + 3

    Seq(1,3) map (i => set.contains(i) should be (true))
  }

  test("latest can give latest item for some unique key on that item type") {
    case class Thing(name: String, v: Int)
    implicit val oThing = Ordering.by[Thing, Int](_.v)
    implicit val key = new Keyed[Thing, String] {
      override def apply(t: Thing) = t.name
    }

    val things = Seq(Thing("a", 1), Thing("b", 2), Thing("b", 1))
    val latest = Latest(Observable.interval(10.millis).take(3).map(_.toInt).map(things(_)))

    latest.toBlockingObservable.toList should be (List(Thing("a", 1), Thing("b", 2)))
  }

  test("bounded map will work as a map") {
    val map = BoundedMap[Int, Int](1) + (1 -> 2)
    map.get(1) should be (Some(2))
    map.get(0) should be (None)
  }

  test("bounded map should overflow") {
    val map = BoundedMap[Int, Int](1) + (1 -> 2) + (2 -> 3)
    map.get(2) should be (Some(3))
    map.get(1) should be (None)
  }
}
