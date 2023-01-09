package com.kossal.api

import zio.*
import stream.*
import Console.*
import zhttp.http.*
import zhttp.html.Html
import Path.Segment.Root
import zhttp.service.Server

import caliban.{CalibanError, GraphQLInterpreter, ZHttpAdapter, GraphQLResponse}

import io.netty.buffer.ByteBuf
import java.nio.charset.Charset

object MainServer extends ZIOAppDefault {
  import com.kossal.gql.GraphQL

  private val graphQLApiPath: Path = !! / "api" / "graphql"

  private val gqlToolsRoute: HttpApp[Any, Nothing] =
    Http.collectZIO[Request] {
      case Method.GET -> !! / "graphiql" =>
        GraphiQL.render(graphQLApiPath.encode).map(Html.fromString).map(Response.html(_))
    }

  private def graphQLRoutes(interpreter: GraphQLInterpreter[Any, CalibanError]) =
    Http.collectHttp[Request] {
      case _ -> path if path == graphQLApiPath => ZHttpAdapter.makeHttpService(interpreter)
      case _ -> !! / "ws" / "graphql"  => ZHttpAdapter.makeWebSocketService(interpreter)
    }

  private val spaPath = os.pwd / "api" / "resource" / "index.html"

  private val jsPath = os.pwd / "out" / "web" / "fastOpt.dest" / "out.js"

  private val spaRoutes: HttpApp[Any, Nothing] =
    Http.collect[Request] {
      case Method.GET -> !! =>
        Response(
          status = Status.Ok,
          data = HttpData.fromFile(spaPath.toIO),
          headers = Headers(HeaderNames.contentType, HeaderValues.textHtml),
        )

      case Method.GET -> !! / "index.js" =>
        Response(
          status = Status.Ok,
          data = HttpData.fromFile(jsPath.toIO),
          headers = Headers(HeaderNames.contentType, "application/javascript; charset=utf-8")
        )
    }

  val server = for {
    interpreter: GraphQLInterpreter[Any, CalibanError] <- GraphQL.api.interpreter
    _ <- printLine("Started GraphQL server available in /api/graphql and /ws/graphql")
    _ <- Server
      .start(8088, gqlToolsRoute ++ graphQLRoutes(interpreter) ++ spaRoutes).forever
  } yield ()

  def run: ZIO[Environment with ZIOAppArgs with Scope, Any, Any] = server.exitCode

}
