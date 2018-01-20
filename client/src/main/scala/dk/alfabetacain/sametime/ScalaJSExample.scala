package dk.alfabetacain.sametime

import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

@JSExportTopLevel("ScalaJSExample")
object ScalaJSExample {

  @JSExport
  def roomEntry(videoId: String): Unit = {
    println(s"Received $videoId")
    //dom.document.getElementById("scalajsShoutOut").textContent = SharedMessages.itWorks
    val sametimePlayer = new SametimePlayer("player", videoId)
  }
}
