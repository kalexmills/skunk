// Copyright (c) 2018 by Rob Norris
// This software is licensed under the MIT License (MIT).
// For more information see LICENSE or https://opensource.org/licenses/MIT

package skunk.exception

import cats.data.Ior
import cats.implicits._
import skunk.Query
import skunk.data.`Type`
import skunk.net.message.RowDescription
import skunk.util.Text
import skunk.syntax.list._

case class ColumnAlignmentException(
  query: Query[_, _],
  rd:    RowDescription
) extends SkunkException(
  sql       = Some(query.sql),
  message   = "Asserted and actual column types differ.",
  hint      = Some("The decoder you provided is incompatible with the output columns for this query. You may need to add or remove columns from the query or your decoder, change their types, or add explicit SQL casts."),
  sqlOrigin = Some(query.origin),
) {

  import Text.{ green, red, cyan, empty }
  implicit def stringToText(s: String): Text = Text(s)

  private def describeType(f: RowDescription.Field): Text =
    Text(Type.forOid(f.typeOid).fold(s"Unknown(${f.typeOid})")(_.name))

  private def describe(ior: Ior[RowDescription.Field, `Type`]): List[Text] =
    ior match {
      case Ior.Left(f)    => List(green(f.name), describeType(f), "->", red(""),                         cyan("── unmapped column"))
      case Ior.Right(t)   => List(empty,         empty,           "->", t.name,                          cyan("── missing column"))
      case Ior.Both(f, t) => List(green(f.name), describeType(f), "->", t.name, if (f.typeOid === t.oid) empty
                                                                                                    else cyan("── type mismatch"))
    }

  private def columns: String =
    s"""|The actual and asserted output columns are
        |
        |  ${Text.grid(rd.fields.align(query.decoder.types).map(describe)).intercalate(Text("\n|  ")).render}
        |
        |""".stripMargin

  override def sections =
    super.sections :+ columns

}

