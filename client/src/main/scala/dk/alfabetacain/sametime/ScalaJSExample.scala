package dk.alfabetacain.sametime

import org.scalajs.dom
import org.scalajs.dom.html.Input
import org.scalajs.dom.raw.Event

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}
import scala.util.matching.Regex

@JSExportTopLevel("ScalaJSExample")
object ScalaJSExample {

  @JSExport
  def roomEntry(videoId: String, roomId: String, progressbarId: String): Unit = {
    println(s"Received $videoId")
    //dom.document.getElementById("scalajsShoutOut").textContent = SharedMessages.itWorks
    val sametimePlayer = new SametimePlayer("player", videoId, roomId, progressbarId)
  }

  private val youtubeLink = """youtube\.com""".r
  private val linkIdExtraction = """v=([^&]+)""".r

  @JSExport
  def createRoomEntry(linkInputId: String, videoOutputId: String): Unit = {
    //https://www.youtube.com/watch?v=JymZSQ7l2j4
    val videoOutput = dom.document.getElementById(videoOutputId).asInstanceOf[Input]
    val urlInput = dom.document.getElementById(linkInputId).asInstanceOf[Input]
    urlInput.oninput = updateVideoIdBasedOnVideoLink(linkIdExtraction, videoOutput, _)
  }

  def updateVideoIdBasedOnVideoLink(regex: Regex, videoOutput: Input, event: Event): Unit = {
    val url = event.target.asInstanceOf[Input].value
    println(s"url: $url")
    if (youtubeLink.findFirstIn(url).isDefined) {
      regex.findFirstMatchIn(url) match {
        case None =>
        //videoOutput.value = ""
        case Some(hit) => {
          videoOutput.value = hit.group(1)
        }
      }
    }
  }
}
