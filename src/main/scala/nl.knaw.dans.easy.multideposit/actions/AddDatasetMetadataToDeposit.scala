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
package nl.knaw.dans.easy.multideposit.actions

import cats.syntax.either._
import nl.knaw.dans.easy.multideposit.PathExplorer.StagingPathExplorer
import nl.knaw.dans.easy.multideposit.model._
import nl.knaw.dans.easy.multideposit.{ ActionError, BetterFileExtensions, FailFast }
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat

import scala.xml.{ Elem, Null, PrefixedAttribute }

class AddDatasetMetadataToDeposit(formats: Set[String]) extends DebugEnhancedLogging {

  def addDatasetMetadata(deposit: Deposit)(implicit stage: StagingPathExplorer): FailFast[Unit] = {
    Either.catchNonFatal {
      logger.debug(s"add dataset metadata for ${ deposit.depositId }")

      stage.stagingDatasetMetadataFile(deposit.depositId).writeXml(depositToDDM(deposit))
    }.leftMap(e => ActionError(s"Could not write deposit metadata for ${ deposit.depositId }", e))
  }

  def depositToDDM(deposit: Deposit): Elem = {
    <ddm:DDM
      xmlns:ddm="http://easy.dans.knaw.nl/schemas/md/ddm/"
      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      xmlns:dc="http://purl.org/dc/elements/1.1/"
      xmlns:dct="http://purl.org/dc/terms/"
      xmlns:dcterms="http://purl.org/dc/terms/"
      xmlns:dcmitype="http://purl.org/dc/dcmitype/"
      xmlns:dcx="http://easy.dans.knaw.nl/schemas/dcx/"
      xmlns:dcx-dai="http://easy.dans.knaw.nl/schemas/dcx/dai/"
      xmlns:dcx-gml="http://easy.dans.knaw.nl/schemas/dcx/gml/"
      xmlns:narcis="http://easy.dans.knaw.nl/schemas/vocab/narcis-type/"
      xmlns:abr="http://www.den.nl/standaard/166/Archeologisch-Basisregister/"
      xmlns:id-type="http://easy.dans.knaw.nl/schemas/vocab/identifier-type/"
      xsi:schemaLocation="http://easy.dans.knaw.nl/schemas/md/ddm/ https://easy.dans.knaw.nl/schemas/md/ddm/ddm.xsd">
      {createProfile(deposit.profile)}
      {createMetadata(deposit.metadata, deposit.springfield)}
    </ddm:DDM>
  }

  def createProfile(profile: Profile): Elem = {
    <ddm:profile>
      {profile.titles.toList.map(elem("dc:title"))}
      {profile.descriptions.toList.map(elem("dcterms:description"))}
      {profile.creators.toList.map(createCreator)}
      {elem("ddm:created")(formatDate(profile.created))}
      {elem("ddm:available")(formatDate(profile.available))}
      {profile.audiences.toList.map(elem("ddm:audience"))}
      {elem("ddm:accessRights")(profile.accessright.toString)}
    </ddm:profile>
  }

  def formatDate(dateTime: DateTime): String = {
    dateTime.toString(ISODateTimeFormat.date())
  }

  private def createOrganisation(org: String, role: Option[ContributorRole.Value] = Option.empty): Elem = {
    <dcx-dai:organization>{
      <dcx-dai:name xml:lang="en">{org}</dcx-dai:name> ++
        role.map(createRole)
    }</dcx-dai:organization>
  }

  private def createRole(role: ContributorRole.Value): Elem = {
    <dcx-dai:role>{role.toString}</dcx-dai:role>
  }

  def createCreator(creator: Creator): Elem = {
    creator match {
      case CreatorOrganization(org, role) =>
        <dcx-dai:creatorDetails>{createOrganisation(org, role)}</dcx-dai:creatorDetails>
      case CreatorPerson(titles, initials, insertions, surname, organization, role, dai) =>
        <dcx-dai:creatorDetails>
          <dcx-dai:author>{
            titles.map(ts => <dcx-dai:titles>{ts}</dcx-dai:titles>) ++
              <dcx-dai:initials>{initials}</dcx-dai:initials> ++
              insertions.map(is => <dcx-dai:insertions>{is}</dcx-dai:insertions>) ++
              <dcx-dai:surname>{surname}</dcx-dai:surname> ++
              role.map(createRole) ++
              dai.map(d => <dcx-dai:DAI>{d}</dcx-dai:DAI>) ++
              organization.map(createOrganisation(_))
          }</dcx-dai:author>
        </dcx-dai:creatorDetails>
    }
  }

  def createContributor(contributor: Contributor): Elem = {
    contributor match {
      case ContributorOrganization(org, role) =>
        <dcx-dai:contributorDetails>{createOrganisation(org, role)}</dcx-dai:contributorDetails>
      case ContributorPerson(titles, initials, insertions, surname, organization, role, dai) =>
        <dcx-dai:contributorDetails>
          <dcx-dai:author>{
            titles.map(ts => <dcx-dai:titles>{ts}</dcx-dai:titles>) ++
              <dcx-dai:initials>{initials}</dcx-dai:initials> ++
              insertions.map(is => <dcx-dai:insertions>{is}</dcx-dai:insertions>) ++
              <dcx-dai:surname>{surname}</dcx-dai:surname> ++
              role.map(createRole) ++
              dai.map(d => <dcx-dai:DAI>{d}</dcx-dai:DAI>) ++
              organization.map(createOrganisation(_))
          }</dcx-dai:author>
        </dcx-dai:contributorDetails>
    }
  }

  private val degreesURL = "http://www.opengis.net/def/crs/EPSG/0/4326"
  private val rdURL = "http://www.opengis.net/def/crs/EPSG/0/28992"

  def createSrsName(scheme: String): String = {
    Map(
      "degrees" -> degreesURL,
      "RD" -> rdURL
    ).getOrElse(scheme, "")
  }

  def createSpatialPoint(point: SpatialPoint): Elem = {
    val srsName = point.scheme.map(createSrsName).getOrElse("")

    // coordinate order x, y = longitude (DCX_SPATIAL_X), latitude (DCX_SPATIAL_Y)
    lazy val xy = s"${ point.x } ${ point.y }"
    // coordinate order y, x = latitude (DCX_SPATIAL_Y), longitude (DCX_SPATIAL_X)
    lazy val yx = s"${ point.y } ${ point.x }"

    val pos = srsName match {
      case `rdURL` => xy
      case `degreesURL` => yx
      case _ => yx
    }

    <dcx-gml:spatial srsName={srsName}>
      <Point xmlns="http://www.opengis.net/gml">
        <pos>{pos}</pos>
      </Point>
    </dcx-gml:spatial>
  }

  /*
   Note that Y is along North - South and X is along East - West
   The lower corner is with the minimal coordinate values and upper corner with the maximal coordinate values
   If x was increasing from West to East and y was increasing from South to North we would have
   lower corner (x,y) = (West,South) and upper corner (x,y) = (East,North)
   as shown in the schematic drawing of the box below.
   This is the case for the WGS84 and RD coordinate systems, but not per se for any other system!

                         upper(x,y)=(E,N)
                *---N---u
                |       |
                W       E
                |       |
    ^           l---S---*
    |           lower(x,y)=(W,S)
    y
     x -->

   */
  def createSpatialBox(box: SpatialBox): Elem = {
    val srsName = box.scheme.map(createSrsName).getOrElse("")

    lazy val xy = (s"${ box.west } ${ box.south }", s"${ box.east } ${ box.north }")
    lazy val yx = (s"${ box.south } ${ box.west }", s"${ box.north } ${ box.east }")

    val (lower, upper) = srsName match {
      case `rdURL` => xy
      case `degreesURL` => yx
      case _ => yx
    }

    <dcx-gml:spatial>
      <boundedBy xmlns="http://www.opengis.net/gml">
        <Envelope srsName={srsName}>
          <lowerCorner>{lower}</lowerCorner>
          <upperCorner>{upper}</upperCorner>
        </Envelope>
      </boundedBy>
    </dcx-gml:spatial>
  }

  def createTemporal(temporal: Temporal): Elem = {
    temporal.scheme
      .map(scheme => <dcterms:temporal xsi:type={scheme}>{temporal.temporal}</dcterms:temporal>)
      .getOrElse(<dcterms:temporal>{temporal.temporal}</dcterms:temporal>)
  }

  def createSubject(subject: Subject): Elem = {
    subject.scheme
      .map(scheme => <dc:subject xsi:type={scheme}>{subject.subject}</dc:subject>)
      .getOrElse(<dc:subject>{subject.subject}</dc:subject>)
  }

  def createRelation(relation: Relation): Elem = {
    relation match {
      case QualifiedRelation(qualifier, Some(link), Some(title)) =>
        <key href={link.toString}>{title}</key>.copy(label = s"ddm:${ qualifier.toString }")
      case QualifiedRelation(qualifier, Some(link), None) =>
          <key href={link.toString}/>.copy(label = s"ddm:${ qualifier.toString }")
      case QualifiedRelation(qualifier, None, Some(title)) =>
        <key>{title}</key>.copy(label = s"dcterms:${ qualifier.toString }")
      case UnqualifiedRelation(Some(link), Some(title)) =>
        <ddm:relation href={link.toString}>{title}</ddm:relation>
      case UnqualifiedRelation(Some(link), None) =>
          <ddm:relation href={link.toString}/>
      case UnqualifiedRelation(None, Some(title)) =>
        <dc:relation>{title}</dc:relation>
      case other => throw new UnsupportedOperationException(s"Relation $other is not supported. You should not even be able to create this object!")
    }
  }

  def createSurrogateRelation(springfield: Springfield): Elem = {
    <ddm:relation scheme="STREAMING_SURROGATE_RELATION">{
      s"/domain/${ springfield.domain }/user/${ springfield.user }/collection/${ springfield.collection }/presentation/$$sdo-id"
    }</ddm:relation>
  }

  def createDate(date: Date): Elem = {
    date match {
      case QualifiedDate(d, q) => elem(s"dcterms:$q")(formatDate(d))
      case TextualDate(text) => elem("dc:date")(text)
    }
  }

  def createIdentifier(identifier: Identifier): Elem = {
    identifier.idType
      .map(idType => <dc:identifier xsi:type={s"id-type:$idType"}>{identifier.id}</dc:identifier>)
      .getOrElse(<dc:identifier>{identifier.id}</dc:identifier>)
  }

  def createType(dcType: DcType.Value): Elem = {
    <dcterms:type xsi:type="dcterms:DCMIType">{dcType.toString}</dcterms:type>
  }

  def createFormat(format: String): Elem = {
    val xml = elem("dc:format")(format)

    if (formats.contains(format))
      xml % new PrefixedAttribute("xsi", "type", "dcterms:IMT", Null)
    else
      xml
  }

  def createLanguage(lang: String): Elem = {
    <dc:language xsi:type="dcterms:ISO639-2">{lang}</dc:language>
  }

  def createUserLicense(license: UserLicense): Elem = {
    <dcterms:license xsi:type="dcterms:URI">{license.license}</dcterms:license>
  }

  def createSpatial(spatial: Spatial): Elem = {
    spatial.spatialScheme
      .map(spatialScheme => <dcterms:spatial xsi:type={s"$spatialScheme"}>{spatial.value}</dcterms:spatial>)
      .getOrElse(<dcterms:spatial>{spatial.value}</dcterms:spatial>)
  }

  def createMetadata(metadata: Metadata, maybeSpringfield: Option[Springfield] = Option.empty): Elem = {
    <ddm:dcmiMetadata>
      {metadata.alternatives.map(elem("dcterms:alternative"))}
      {metadata.publishers.map(elem("dcterms:publisher"))}
      {metadata.types.toList.map(createType)}
      {metadata.formats.map(createFormat)}
      {metadata.identifiers.map(createIdentifier)}
      {metadata.sources.map(elem("dc:source"))}
      {metadata.languages.map(createLanguage)}
      {metadata.spatials.map(createSpatial)}
      {metadata.rightsholder.toList.map(elem("dcterms:rightsHolder"))}
      {metadata.relations.map(createRelation) ++ maybeSpringfield.map(createSurrogateRelation) }
      {metadata.dates.map(createDate)}
      {metadata.contributors.map(createContributor)}
      {metadata.subjects.map(createSubject)}
      {metadata.spatialPoints.map(createSpatialPoint)}
      {metadata.spatialBoxes.map(createSpatialBox)}
      {metadata.temporal.map(createTemporal)}
      {metadata.userLicense.map(createUserLicense).toSeq}
    </ddm:dcmiMetadata>
  }

  def elem(key: String)(value: String): Elem = {
    <key>{value}</key>.copy(label = key)
  }
}
