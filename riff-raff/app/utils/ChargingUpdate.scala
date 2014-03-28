package utils

import org.joda.time.{Interval, DateTime}
import play.api.mvc.RequestHeader

object ChargingUpdate {
  lazy val interval = new Interval(
    new DateTime(2014,4,1,0,0,0), new DateTime(2014,4,1,13,0,0)
  )

  def choose[A](normal: A)(inInterval: A):A =
    if (interval.contains(new DateTime())) inInterval else normal

  def visible(implicit request:RequestHeader) = {
    request.session.get("seenChargeDialog").isEmpty
    //choose(false)(true)
  }
}