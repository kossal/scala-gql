package com.kossal.gql

import zio.*
import os.*
import caliban.tools.*

object GenGQLSchema extends ZIOAppDefault {
  private def createSchema(file: String): Task[Unit] =
    ZIO.attemptUnsafe { _ => os.write.over(os.Path(file), GraphQL.api.render) }

  def run: ZIO[ZIOAppArgs, Throwable, Unit] = {
    getArgs.flatMap { args =>
      Console.printLine(s"Creating gql schema in ${args(0)}") *>
        createSchema(args(0))
    }
  }
}

object GenGQLClient extends ZIOAppDefault {
  private def generate(
                        schemaPath: => String,
                        toPath: => String,
                        packageName: => String,
                        objectName: => String
                      ) = Codegen.generate(
    Options(
      schemaPath = schemaPath,
      toPath = toPath,
      fmtPath = None,
      headers = None,
      packageName = Option(packageName),
      genView = Some(true),
      effect = None,
      scalarMappings = None,
      imports = None,
      abstractEffectType = None,
      splitFiles = Some(false),
      clientName = Some(objectName),
      enableFmt = Some(true),
      extensibleEnums = Some(false),
      preserveInputNames = Some(true)
    ),
    Codegen.GenType.Client
  )

  def run: ZIO[ZIOAppArgs, Throwable, Unit] = {
    getArgs.flatMap { args =>
      Console.printLine(
        s"""
           |Creating gql scala code using schema ${args(0)}
           |on path ${args(1)}
           |using package name ${args(2)}
           |using object name ${args(3)}
           |""".stripMargin
      ) *>
        generate(
          schemaPath = args(0),
          toPath = args(1),
          packageName = args(2),
          objectName = args(3)
        ).unit
    }
  }
}