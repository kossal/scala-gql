import mill._
import scalalib._
import scalajslib._

trait CommonScala extends ScalaModule {
  def scalaVersion = "3.2.1"
}

object graphql extends CommonScala {
  override def ivyDeps = Agg(
    ivy"com.lihaoyi::os-lib:0.9.0",
    ivy"dev.zio::zio:2.0.5",
    ivy"dev.zio::zio-streams:2.0.5",
    ivy"com.github.ghostdogpr::caliban:2.0.2",
    ivy"com.github.ghostdogpr::caliban-tools:2.0.1"
  )

  def createGQLSchema: T[PathRef] = T {
    val dest = T.ctx().dest / "schema.gql"
    mill.modules.Jvm.runSubprocess(
      "com.kossal.gql.GenGQLSchema",
      runClasspath().map(_.path),
      forkArgs(),
      forkEnv(),
      Seq(dest.toString),
      workingDir = forkWorkingDir(),
      useCpPassingJar = runUseArgsFile()
    )
    PathRef(dest)
  }

  def createGQLClient: T[PathRef] = T {
    val dest = T.ctx().dest / "client.scala"
    mill.modules.Jvm.runSubprocess(
      "com.kossal.gql.GenGQLClient",
      runClasspath().map(_.path),
      forkArgs(),
      forkEnv(),
      Seq(createGQLSchema().path.toString, dest.toString, "com.kossal.web.gql", "Client"),
      workingDir = forkWorkingDir(),
      useCpPassingJar = runUseArgsFile()
    )
    PathRef(dest)
  }
}

trait CommonModule extends CommonScala {
  override def moduleDeps: Seq[JavaModule] = super.moduleDeps ++ Seq(graphql)
  override def generatedSources = Seq(graphql.createGQLClient())
}

object web extends CommonModule with ScalaJSModule {
  def scalaJSVersion = "1.12.0"

  override def ivyDeps = Agg(
    ivy"org.scala-js::scalajs-dom::2.3.0",
    ivy"com.raquo::laminar::0.14.2",
    ivy"com.raquo::airstream::0.14.2",
    ivy"io.laminext::fetch::0.14.4",
    ivy"io.laminext::websocket::0.14.4",
    ivy"com.github.ghostdogpr::caliban-client::2.0.2",
    ivy"com.github.ghostdogpr::caliban-client-laminext::2.0.2"
  )
}

object api extends CommonModule {
  override def generatedSources = super.generatedSources() ++ Seq(web.fastOpt())

  override def ivyDeps = Agg(
    ivy"com.lihaoyi::os-lib:0.9.0",
    ivy"dev.zio::zio:2.0.5",
    ivy"dev.zio::zio-streams:2.0.5",
    ivy"dev.zio::zio-http:0.0.3",
    ivy"com.github.ghostdogpr::caliban:2.0.2",
    ivy"com.github.ghostdogpr::caliban-zio-http:2.0.2",
    ivy"com.lihaoyi::scalatags:0.12.0"
  )
}