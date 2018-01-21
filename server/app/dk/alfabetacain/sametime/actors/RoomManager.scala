package dk.alfabetacain.sametime.actors

import akka.actor.{Actor, ActorRef, PoisonPill, Props, Timers}
import com.google.inject.{Inject, Singleton}

import scala.concurrent.duration._

@Singleton
class RoomManager @Inject() () extends Actor with Timers {

  import RoomManager._
  var rooms: Map[String, Room] = Map("test" -> Room("test", Set.empty, 2, ""))


  def receive = {
    case CreateRoom(name, size, videoId) =>
      rooms = rooms + (name -> Room(name, Set.empty, size, videoId))
      sender() ! RoomCreated(name)
      timers.startPeriodicTimer(TickKey(name), Tick(name), 1.seconds)
      timers.startSingleTimer(DestroyRoomTickKey(name), DestroyRoom(name), 10.minutes)
    case Subscribe(name, sub) =>
      rooms.get(name).map{ room =>
        room.copy(occupants = room.occupants + sub)
      } match {
        case None => println(s"No such room: $name")
        case Some(up) =>
          rooms = rooms + (name -> up)
          sub ! WebsocketConnection.RoomStatus(name, up.occupants.size, up.size)
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
    case Tick(room) => rooms.get(room) match {
      case None => timers.cancel(TickKey(room))
      case Some(actualRoom) =>
        actualRoom.occupants.foreach{ occupant =>
          occupant ! WebsocketConnection.RoomStatus(room,
            actualRoom.occupants.size, actualRoom.size)
        }
    }

    case DestroyRoom(name) =>
      rooms = rooms.get(name).map{ room =>
        room.occupants.foreach(_ ! PoisonPill)
        rooms - name
      }.getOrElse(rooms)

    case x => println(s"Received ${x.toString}")
  }
}

case class Room(name: String, occupants: Set[ActorRef], size: Int, videoId: String) {
  val isFull: Boolean = occupants.size == size
  def notify(msg: Any): Unit =
    occupants.foreach(sub => sub ! msg)
}

object RoomManager {

  private[RoomManager] final case class TickKey(room: String)
  private[RoomManager] final case class Tick(room: String)
  private[RoomManager] final case class DestroyRoomTickKey(room: String)

  sealed trait Message
  final case class CreateRoom(name: String, size: Int, videoId: String) extends Message
  final case class DestroyRoom(name: String) extends Message
  final case class RoomCreated(name: String) extends Message
  final case class Subscribe(room: String, sub: ActorRef) extends Message
  final case class Unsubscribe(room: String, sub: ActorRef) extends Message
  final case class FindRoom(name: String) extends Message
  final case class RoomExists(name: String, size: Int, videoId: String) extends Message
  final case object RoomDoesNotExist extends Message

  def props: Props = Props[RoomManager]
}
