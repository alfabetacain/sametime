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
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraint._
import play.api.i18n.I18nSupport

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

final case class RoomData(videoId: String, size: Int)

class Application @Inject() (@Named("room_manager") roomManager: ActorRef, cc: ControllerComponents)
                            (implicit system: ActorSystem, mat: Materializer, ec: ExecutionContext) extends AbstractController(cc)
  with I18nSupport {

  implicit val akkaAskTimeout: Timeout = 5.seconds

  def index = Action {
    Ok(views.html.index("Hello world"))
  }

  def websocket(room: String) = WebSocket.acceptOrResult[String, String] { implicit request =>
    (roomManager ? RoomManager.FindRoom(room)).map{
      case RoomManager.RoomDoesNotExist => Left(NotFound)
      case _: RoomManager.RoomExists =>
        Right(ActorFlow.actorRef{ out =>
          WebsocketConnection.props(room, roomManager, out)
        })
    }
  }

  val roomForm = Form(
    mapping(
      "videoId" -> text,
      "size" -> number(min = 1, max = 10)
    )(RoomData.apply)(RoomData.unapply))

  def createRoom() = Action.async { implicit request =>
    println("create room")
    roomForm.bindFromRequest.fold(
      errorForm => {
        Future.successful(BadRequest(views.html.create_room(errorForm)))
      }, roomData => {
        val roomName = Random.alphanumeric.take(40).mkString
        (roomManager ? RoomManager.CreateRoom(roomName, roomData.size, roomData.videoId)).map{
          case RoomManager.RoomCreated(`roomName`) =>
            Redirect(routes.Application.roomPage(roomName))
          case x => {
            println(x)
            InternalServerError("")
          }
        }
      }
    )
  }

  def roomPage(roomId: String) = Action.async {
      (roomManager ? RoomManager.FindRoom(roomId)).map{
        case RoomManager.RoomDoesNotExist => NotFound(s"No such room: $roomId")
        case RoomManager.RoomExists(name, size, video) =>
          Ok(views.html.room(roomId, video))
      }
  }

  def createRoomPage() = Action { implicit request =>
    Ok(views.html.create_room(roomForm))
  }
}
