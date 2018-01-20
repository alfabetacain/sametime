package dk.alfabetacain.sametime.actors

import akka.actor.{Actor, ActorRef, Props}
import com.google.inject.{Inject, Singleton}

@Singleton
class RoomManager @Inject() () extends Actor {

  import RoomManager._
  var rooms: Map[String, Room] = Map("test" -> Room("test", Set.empty, 2, ""))

  def receive = {
    case CreateRoom(name, size, videoId) =>
      rooms = rooms + (name -> Room(name, Set.empty, size, videoId))
      sender() ! RoomCreated(name)
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
      rooms.get(room).fold(sender() ! RoomDoesNotExist) { r =>
        sender() ! RoomExists(r.name, r.size, r.videoId)
      }
    case x => println(s"Received ${x.toString}")
  }
}

case class Room(name: String, occupants: Set[ActorRef], size: Int, videoId: String) {
  val isFull: Boolean = occupants.size == size
  def notify(msg: Any): Unit =
    occupants.foreach(sub => sub ! msg)
}

object RoomManager {

  sealed trait Message
  case class CreateRoom(name: String, size: Int, videoId: String) extends Message
  case class RoomCreated(name: String) extends Message
  case class Subscribe(room: String, sub: ActorRef) extends Message
  case class Unsubscribe(room: String, sub: ActorRef) extends Message
  case class FindRoom(name: String) extends Message
  case class RoomExists(name: String, size: Int, videoId: String) extends Message
  case object RoomDoesNotExist extends Message

  def props: Props = Props[RoomManager]
}
