package dk.alfabetacain.sametime.actors

import akka.actor.{Actor, ActorRef, Props}
import dk.alfabetacain.sametime.shared
import upickle.default._

class WebsocketConnection(room: String, roomManager: ActorRef, out: ActorRef) extends Actor {
  import WebsocketConnection._

  def receive = {
    case shared.Register =>
      roomManager ! RoomManager.Subscribe(room, self)
    case shared.Play => println("Received Play from websocket")
    case Play =>
      println("Received play!")
      out ! write(shared.Play)
    case json: String =>
      println(s"Received json: $json")
      self ! read[shared.WebsocketMessage](json)
    case x => println(s"Received ${x.toString}")
  }

  override def postStop(): Unit = {
    roomManager ! RoomManager.Unsubscribe(room, self)
  }

}

object WebsocketConnection {
  def props(room: String, roomManager: ActorRef, out: ActorRef) = Props(
    new WebsocketConnection(room, roomManager, out)
  )

  sealed trait Message
  case object Play extends Message
}
