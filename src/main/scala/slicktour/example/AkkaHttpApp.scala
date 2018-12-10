package slicktour.example

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives.{complete, get, path}
import akka.stream.{ActorMaterializer, Materializer}

import scala.concurrent.ExecutionContext

object AkkaHttpApp {
  def main(args: Array[String]): Unit = {
    implicit val actorSystem: ActorSystem = ActorSystem("slick-tour")
    implicit val executionContext: ExecutionContext = actorSystem.dispatcher
    implicit val materializer: Materializer = ActorMaterializer()

    val route = (get & path("hello")) {
      complete("Hello World!")
    }

    val http = Http()
    http.bindAndHandle(route, "localhost", 8080)
  }
}
