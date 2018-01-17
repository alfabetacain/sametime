package dk.alfabetacain.sametime.actors

import akka.actor.{Actor, ActorRef, Props}
import com.google.inject.{Inject, Singleton}

@Singleton
class RoomManager @Inject() () extends Actor {

  import RoomManager._
  var rooms: Map[String, Room] = Map("test" -> Room("test", Set.empty, 2))

  def receive = {
    case CreateRoom(name, size) =>
      rooms = rooms + (name -> Room(name, Set.empty, size))
    case Subscribe(name, sub) =>
      rooms.get(name).map{ room =>
        room.copy(occupants = room.occupants + sub)
      } match {
        case None => println(s"No such room: $name")
        case Some(up) =>
          rooms = rooms + (name -> up)
          if (up.isFull)
            up.notify(WebsocketConnection.Play)
      }
    case Unsubscribe(name, sub) =>
      rooms.get(name).map{ room =>
        room.copy(occupants = room.occupants - sub)
      } match {
        case Some(up) => rooms = rooms + (name -> up)
        case None => println(s"No such room: $name")
      }
    case FindRoom(room) =>
      if (rooms.contains(room))
        sender() ! RoomExists
      else
        sender() ! RoomDoesNotExist
    case x => println(s"Received ${x.toString}")
  }
}

case class Room(name: String, occupants: Set[ActorRef], size: Int) {
  val isFull: Boolean = occupants.size == size
  def notify(msg: Any): Unit =
    occupants.foreach(sub => sub ! msg)
}

object RoomManager {

  sealed trait Message
  case class CreateRoom(name: String, size: Int) extends Message
  case class Subscribe(room: String, sub: ActorRef) extends Message
  case class Unsubscribe(room: String, sub: ActorRef) extends Message
  case class FindRoom(name: String) extends Message
  case object RoomExists extends Message
  case object RoomDoesNotExist extends Message

  def props: Props = Props[RoomManager]
}
