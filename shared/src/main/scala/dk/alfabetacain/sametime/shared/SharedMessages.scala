package dk.alfabetacain.sametime.shared

import upickle.default._

object SharedMessages {
  def itWorks = "It works!"
}

sealed trait WebsocketMessage
final case object Register extends WebsocketMessage
final case object Play extends WebsocketMessage
final case class RoomStatus(numberOfOccupants: Int, size: Int) extends WebsocketMessage
object WebsocketMessage {
  implicit def rw: ReadWriter[WebsocketMessage] = macroRW
  implicit def roomStatusRw: ReadWriter[RoomStatus] = macroRW
}
