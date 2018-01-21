package dk.alfabetacain.sametime


import dk.alfabetacain.sametime.shared.SharedMessages
import dk.alfabetacain.sametime.youtube.{Player, PlayerEvents, PlayerOptions}
import org.scalajs.dom
import org.scalajs.dom.html.Script
import org.scalajs.dom.raw.{Event, HTMLDivElement, MessageEvent, WebSocket}
import upickle.default._

import scala.scalajs.js
import scala.scalajs.js.UndefOr


case class State(socketReady: Boolean, playerReady: Boolean)

class SametimePlayer(val playerElementId: String, val videoId: String, val roomId: String, progressbarId: String, val whenReady: () => Unit = () => {}) {

  println(s"progress bar: $progressbarId")
  val progressbar = dom.document.getElementById(progressbarId)
  var socket: WebSocket = null
  var player: Player = null
  var first = true
  var isZeroed = false
  var playerIsReady = false

  //load youtube script api
  val tag = dom.document.createElement("script").asInstanceOf[Script]
  tag.src = "https://www.youtube.com/iframe_api"
  val firstScriptTag = dom.document.getElementsByTagName("script").item(0)
  firstScriptTag.parentNode.insertBefore(tag, firstScriptTag)

  createYoutubePlayer(playerElementId)

  def createYoutubePlayer(id: String): Unit = {
    dom.window.asInstanceOf[js.Dynamic].onYouTubeIframeAPIReady = () => {
      player = new Player(id, PlayerOptions(
        width = "640",
        height = "390",
        videoId = videoId,
        events = PlayerEvents(
          onReady = UndefOr.any2undefOrA(onYoutubePlayerReady),
          onStateChange = UndefOr.any2undefOrA(onYoutubePlayerStateChange _),
          onError = (ev: youtube.Event) => {
            println("error occurred!")
          }
        )
      ))
    }
  }

  def onYoutubePlayerReady(ev: youtube.Event): Unit = {
    player.playVideo()
  }

  def onYoutubePlayerStateChange(ev: youtube.Event): Unit = {
    ev.data.get.asInstanceOf[Int] match {
      case Player.State.PLAYING if first =>
        println("First playthrough")
        first = false
        ev.target.get.seekTo(0.0, true)
      case Player.State.PLAYING if !first && !isZeroed =>
        isZeroed = true
        ev.target.get.pauseVideo()
      case Player.State.PAUSED if !playerIsReady =>
        playerIsReady = true
        createSocket()
      case x =>
        println(s"Received state change: $x")
    }
  }

  def createSocket(): Unit = {
    socket = new WebSocket(getWebsocketUri(roomId))
    socket.onopen = socketOnOpen
  }

  def socketOnOpen(ev: Event): Unit = {
    socket.onmessage = socketOnMessage
    socket.send(write(shared.Register))
  }

  def socketOnMessage(ev: MessageEvent): Unit = {
    val msg = read[shared.WebsocketMessage](ev.data.toString)
    println(s"Received message: $msg")
    msg match {
      case shared.Play =>
        player.playVideo()
      case shared.RoomStatus(numberOfOccupants, size) =>
        println(s"Room status: $numberOfOccupants/$size")
        progressbar.setAttribute("aria-valuenow", s"$numberOfOccupants")
        progressbar.setAttribute("aria-valuemax", s"$size")
        progressbar.setAttribute("style", s"width: ${(numberOfOccupants / (size + 0.0) * 100).toInt}%")
      case x =>
        println(s"Received unknown message: $msg")
    }
  }

  def getWebsocketUri(room: String): String = {
    val wsProtocol = if (dom.document.location.protocol == "https") "wss" else "ws"
    s"$wsProtocol://${dom.document.location.host}/room/$room/ws"
  }
}


