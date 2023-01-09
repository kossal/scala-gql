package com.kossal.web

import org.scalajs.dom
import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import caliban.client.laminext.{Subscription, *}
import io.laminext.websocket.*
import gql.Client.*
import Character.CharacterView
import caliban.client.Operations.RootQuery
import caliban.client.{CalibanClientError, SelectionBuilder}

import scalajs.concurrent.JSExecutionContext.Implicits.*

object App {
  val characters: Var[List[CharacterView]] = Var(Nil)
  val characterSubscription: Var[Option[CharacterView]] = Var(None)

  val graphQLUri = "/api/graphql"
  val graphQLWSUri = "/ws/graphql"
  val ws = WebSocket.path(graphQLWSUri, "graphql-ws").graphql.build(managed = true)

  val getCharactersStream: EventStream[Either[CalibanClientError, List[CharacterView]]] =
    Queries.getCharacters(Character.view).toEventStream(graphQLUri)

  def charactersStream: Subscription[CharacterView] = Subscriptions.getCharacters(Character.view).toSubscription(ws)

  val app = div(
    "Hello World",
    div(
      "Characters: ",
      getCharactersStream.collect { case Right(value) => value } --> characters.set _,
      children <-- characters.signal.split(_.name) { case (_, _, characterSignal) =>
        div(child.text <-- characterSignal.map { case CharacterView(name, age) => s"$name $age" })
      }
    ),
    div(
      "Subscriptions: ",
      ws.connect,
      ws.connected
        .map(_ => ws.init())
        .flatMap(_ => charactersStream.received.collect { case Right(value) => Some(value) }) --> characterSubscription.set _,
      child.text <-- characterSubscription.signal.map {
        case None => "No Character yet"
        case Some(CharacterView(name, age)) => s"$name $age"
      }
    )
  )

  def main(args: Array[String]): Unit = {
    render(dom.document.getElementById("root"), app)
  }
}