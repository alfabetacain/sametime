import com.google.inject.AbstractModule
import dk.alfabetacain.sametime.actors.RoomManager
import play.api.libs.concurrent.AkkaGuiceSupport

class Module extends AbstractModule with AkkaGuiceSupport {
  override def configure(): Unit = {
    bindActor[RoomManager]("room_manager")
  }
}
