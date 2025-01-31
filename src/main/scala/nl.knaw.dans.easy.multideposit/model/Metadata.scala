/*
 * Copyright (C) 2016 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.easy.multideposit.model

import java.net.URI

import cats.data.NonEmptyList
import nl.knaw.dans.easy.multideposit.model.ContributorRole.ContributorRole
import org.joda.time.DateTime

case class Metadata(alternatives: List[String] = List.empty,
                    publishers: List[String] = List.empty,
                    types: NonEmptyList[DcType.Value] = NonEmptyList.one(DcType.DATASET),
                    formats: List[String] = List.empty,
                    identifiers: List[Identifier] = List.empty,
                    sources: List[String] = List.empty,
                    languages: List[String] = List.empty,
                    spatials: List[Spatial] = List.empty,
                    rightsholder: NonEmptyList[String],
                    relations: List[Relation] = List.empty,
                    dates: List[Date] = List.empty,
                    contributors: List[Contributor] = List.empty,
                    subjects: List[Subject] = List.empty,
                    spatialPoints: List[SpatialPoint] = List.empty,
                    spatialBoxes: List[SpatialBox] = List.empty,
                    temporal: List[Temporal] = List.empty,
                    userLicense: Option[UserLicense] = Option.empty,
                   )

case class Identifier(id: String, idType: Option[IdentifierType.Value] = Option.empty)

sealed abstract class Relation
case class QualifiedRelation(qualifier: RelationQualifier.Value,
                             link: Option[URI] = Option.empty,
                             title: Option[String] = Option.empty) extends Relation {
  require(link.isDefined || title.isDefined, "at least one of [link, title] must be filled in")
}
case class UnqualifiedRelation(link: Option[URI] = Option.empty,
                               title: Option[String] = Option.empty) extends Relation {
  require(link.isDefined || title.isDefined, "at least one of [link, title] must be filled in")
}

sealed abstract class Date
case class QualifiedDate(date: DateTime, qualifier: DateQualifier.Value) extends Date
case class TextualDate(text: String) extends Date

sealed abstract class Contributor
case class ContributorOrganization(organization: String,
                                   role: Option[ContributorRole] = Option.empty) extends Contributor
case class ContributorPerson(titles: Option[String] = Option.empty,
                             initials: String,
                             insertions: Option[String] = Option.empty,
                             surname: String,
                             organization: Option[String] = Option.empty,
                             role: Option[ContributorRole] = Option.empty,
                             dai: Option[String] = Option.empty) extends Contributor

case class Subject(subject: String = "", scheme: Option[String] = Option.empty)

case class Spatial(value: String, spatialScheme: Option[SpatialScheme.Value] = Option.empty)

case class SpatialPoint(x: String, y: String, scheme: Option[String] = Option.empty)

case class SpatialBox(north: String,
                      south: String,
                      east: String,
                      west: String,
                      scheme: Option[String] = Option.empty)

case class Temporal(temporal: String = "", scheme: Option[String] = Option.empty)

case class UserLicense(license: String)
