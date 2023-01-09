package com.kossal.gql

import zio.*
import stream.*

import caliban.GraphQL.graphQL
import caliban.RootResolver
import scala.util.Random

object GraphQL {
  val random = new Random
  case class Character(name: String, age: Int)

  private val initialCharacters: Seq[Character] = List(
    Character("Jose", 19),
    Character("Maria", 29),
    Character("Carlos", 27),
  )

  private val characters: UIO[Ref[Seq[Character]]] = Ref.make[Seq[Character]](initialCharacters)

  private val getCharacters: UIO[Seq[Character]] = characters.flatMap(_.get)

  private def getCharacter(name: String): UIO[Option[Character]] =
    getCharacters.map(_.find(_.name == name))

  private def changeAge(name: String, age: Int): UIO[Option[Character]] =
    characters.flatMap(_.updateAndGet { currentCharacters =>
      currentCharacters.find(_.name == name).fold(currentCharacters) { currentCharacter =>
        currentCharacters.filterNot(_ == currentCharacter) :+ currentCharacter.copy(age = age)
      }
    }).map(_.find(_.name == name))

  val getCharacterStream: ZStream[Any, Nothing, Character] =
    ZStream.fromZIO(getCharacters)
      .repeat(Schedule.spaced(1.second))
      .map { currentCharacter =>
        val index = random.nextInt(currentCharacter.length)
        currentCharacter(index)
      }

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

  private val queries: Queries = Queries(getCharacters, getCharacter)
  private val mutations: Mutations = Mutations(args => changeAge(args.name, args.age))
  private val subscriptions: Subscriptions = Subscriptions(getCharacterStream)

  val api = graphQL(RootResolver(queries, mutations, subscriptions))
}