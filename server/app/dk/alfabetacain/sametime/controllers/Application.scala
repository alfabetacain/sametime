package dk.alfabetacain.sametime.controllers

import javax.inject._

import akka.actor.{ActorRef, ActorSystem}
import dk.alfabetacain.sametime.actors.{RoomManager, WebsocketConnection}
import dk.alfabetacain.sametime.shared.SharedMessages
import play.api.libs.streams.ActorFlow
import play.api.mvc._
import akka.pattern.ask
import akka.stream.Materializer

import scala.concurrent.duration._
import akka.util.Timeout

import scala.concurrent.ExecutionContext

@Singleton
class Application @Inject() (@Named("room_manager") roomManager: ActorRef, cc: ControllerComponents)
                            (implicit system: ActorSystem, mat: Materializer, ec: ExecutionContext) extends AbstractController(cc) {

  implicit val akkaAskTimeout: Timeout = 5.seconds

  def index = Action {
    Ok(views.html.index(SharedMessages.itWorks))
  }

  def websocket(room: String) = WebSocket.acceptOrResult[String, String] { implicit request =>
    (roomManager ? RoomManager.FindRoom(room)).map{
      case RoomManager.RoomDoesNotExist => Left(NotFound)
      case RoomManager.RoomExists =>
        Right(ActorFlow.actorRef{ out =>
          WebsocketConnection.props(room, roomManager, out)
        })
    }
  }
}
