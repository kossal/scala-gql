package com.kossal.api

import zio.{ZIO, UIO}
import scalatags.Text.all.*
import scalatags.Text.tags2.{title => mainTitle, style => mainStyle}

object GraphiQL {
  import com.kossal.gql.GraphQL

  def render(schemaPath: String): UIO[String] = ZIO.succeedUnsafe { _ =>
    html(
      lang := "en",
      head(
        mainTitle("GraphiQL"),
        mainStyle("""body {
        height: 100%;
        margin: 0;
        width: 100%;
        overflow: hidden;
      }

      #graphiql {
        height: 100vh;
      }"""),
        script(src := "https://unpkg.com/react@17/umd/react.development.js", integrity := "sha512-Vf2xGDzpqUOEIKO+X2rgTLWPY+65++WPwCHkX2nFMu9IcstumPsf/uKKRd5prX3wOu8Q0GBylRpsDB26R6ExOg==", crossorigin := "anonymous"),
        script(src := "https://unpkg.com/react-dom@17/umd/react-dom.development.js", integrity := "sha512-Wr9OKCTtq1anK0hq5bY3X/AvDI5EflDSAh0mE9gma+4hl+kXdTJPKZ3TwLMBcrgUeoY0s3dq9JjhCQc7vddtFg==", crossorigin := "anonymous"),
        link(rel := "stylesheet", href := "https://unpkg.com/graphiql/graphiql.min.css")
      ),
      body(
        div(id := "graphiql","Loading..."),
        script(src := "https://unpkg.com/graphiql/graphiql.min.js", `type` := "application/javascript"),
        script(s"""ReactDOM.render(
        React.createElement(GraphiQL, {
          fetcher: GraphiQL.createFetcher({
            url: '$schemaPath',
          }),
          defaultEditorToolsVisibility: true,
        }),
        document.getElementById('graphiql'),
      );""")
      )
    ).toString
  }.memoize.flatten
}