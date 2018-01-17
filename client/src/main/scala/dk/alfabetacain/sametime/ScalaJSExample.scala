package dk.alfabetacain.sametime

import dk.alfabetacain.sametime.shared.SharedMessages
import dk.alfabetacain.sametime.youtube.{Player, PlayerEvents, PlayerOptions}
import org.scalajs.dom
import org.scalajs.dom.html.Script
import org.scalajs.dom.raw.{Event, MessageEvent, WebSocket}
import upickle.default._

import scala.scalajs.js

object ScalaJSExample {

  def main(args: Array[String]): Unit = {
    dom.document.getElementById("scalajsShoutOut").textContent = SharedMessages.itWorks

    //load youtube api
    val tag = dom.document.createElement("script").asInstanceOf[Script]
    tag.src = "https://www.youtube.com/iframe_api"
    val firstScriptTag = dom.document.getElementsByTagName("script").item(0)
    firstScriptTag.parentNode.insertBefore(tag, firstScriptTag)

    val socket = new WebSocket(getWebsocketUri("test"))
    dom.window.asInstanceOf[js.Dynamic].onYouTubeIframeAPIReady = () => {
      val outerPlayer = new Player("player", PlayerOptions(
        width = "100%",
        height = "100%",
        videoId = "M7lc1UVf-VE",
        events = PlayerEvents(
          onReady = (ev: youtube.Event) => {
            val player = ev.target.toOption.get
            player.playVideo()
            player.seekTo(0.0, true)
            player.pauseVideo()
            player.seekTo(0.0, true)
            val socket = new WebSocket(getWebsocketUri("test"))
            socket.onopen = (_: Event) => {
              socket.send(write(shared.Register))
            }
            socket.onmessage = { (event: MessageEvent) =>
              val msg = read[shared.WebsocketMessage](event.data.toString)
              println(s"Got message $msg")
              msg match {
                case shared.Play =>
                  player.playVideo()
                case x =>
                  println(s"Received command $x")
              }
            }
          }
        )
      ))
    }
  }

  def getWebsocketUri(room: String): String = {
    val wsProtocol = if (dom.document.location.protocol == "https") "wss" else "ws"
    s"$wsProtocol://${dom.document.location.host}/ws?room=$room"
  }
}
