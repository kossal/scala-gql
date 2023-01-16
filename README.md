## Objective

The motivation for this project was to create a reactive and live component in a web app using laminar. This is fairly
useful if you want users to be notified of any event on the server without having to pool or refresh their browser, this
can be accomplished fairly easily with websockets.

There is many ways of implementing websockets, in this case we're using GraphQL subscriptions, this will give
us free schema checks and message format.

## Stack

- Language: Scala
- Build tool: Mill
- Back-End: Caliban (GraphQL) + ZIO http (API) + ZIO (Concurrency and streams)
- Front-End: Laminar (reactive-ui) + Laminext (helpers) + Caliban Client (GraphQL scala.js client)

## Architecture (check build.sc)

The `api` module will expose the GraphQL API and HTML + JS assets, the assets will be created
on the `web` module. Caliban can create a gql schema using scala objects which we also want
to reuse on the `web` module so the `gql` module will both define the methods for `api` and create the
scalajs objects for `web`. Additionally, we're adding GraphiQL just because.

To run the project simply run `mill -i -j 0 api.runMain com.kossal.api.MainServer` (make sure to have [mill installed](https://com-lihaoyi.github.io/mill/mill/Installation.html))

Then go to http://127.0.0.1:8088

## Implementation

In essence, we need 2 elements, the graphql subscription method and to use it on the front end.
APIs are created in Caliban by instanciating a `Query` object which has queries, mutations and subscriptions.
Queries and mutations are objects whose parameters are values or functions, the difference with subsciptions is
that the return type must be a ZStream.

```scala
case class Queries(
  getCharacters: UIO[Seq[Character]],
  getCharacter: String => UIO[Option[Character]]
)

case class Mutations(
  changeAge: Character => UIO[Option[Character]]
)

case class Subscriptions(
  getCharacters: ZStream[Any, Nothing, Character]
)
```

In this project we simply have a list of characters who each second are randomly sent to the ZStream. On the
front-end we want to show this character and change it when-ever the server send us a different one.

For that we'll open a websocket connection into an airStream EventStream (which laminar components can listen to):

```scala
val graphQLWSUri = "/ws/graphql"
// managed = true is important as it indicates that the ws connection should be opened when the laminar component
// is mounted on the browser, and disconnect when it's unmounted
val ws = WebSocket.path(graphQLWSUri, "graphql-ws").graphql.build(managed = true)

def charactersStream: Subscription[CharacterView] = Subscriptions.getCharacters(Character.view).toSubscription(ws)

// This will only serve to store the Characters
// We could read the stream directly to the child.text
val characterSubscription: Var[Option[CharacterView]] = Var(None)

div(
  "Subscriptions: ",
  ws.connect, // This will only trigger when the div is mounted
  ws.connected
    .map(_ => ws.init())
    .flatMap(_ => charactersStream.received.collect { case Right(value) => Some(value) }) --> characterSubscription.set _,
  child.text <-- characterSubscription.signal.map {
    case None => "No Character yet"
    case Some(CharacterView(name, age)) => s"$name $age"
  }
)
```

## Challenges

Interestingly the subsciption was the least of the challenges, the 2 biggest pains were:

### 1) Creating the mill project

Caliban doesn't have a mill plugin so creating the gql client and schema had to be done 
in 2 subprocesses in a different mill module `gql`.

### 2) ZIO http

I tried to use ZIO http as caliban already supports ZIO, but it's still on a very early stage
and many elements you would expect an http client or server to have (like finagle or akka-http)
doesn't exist directly. Maybe in the future this will change, until then I would still recommending
other mature [libraries](https://github.com/lauris/awesome-scala#http).