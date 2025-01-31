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

import java.net.URI
import java.util.UUID

import better.files.File
import cats.data.NonEmptyList
import nl.knaw.dans.common.lang.dataset.AccessCategory
import nl.knaw.dans.easy.multideposit.model._
import nl.knaw.dans.easy.multideposit.parser.MultiDepositParser
import nl.knaw.dans.easy.multideposit.{ CustomMatchers, TestSupportFixture }
import org.joda.time.DateTime
import org.scalatest.BeforeAndAfterEach

import scala.xml.{ Elem, Node }

class AddDatasetMetadataToDepositSpec extends TestSupportFixture with CustomMatchers with BeforeAndAfterEach {

  private val action = new AddDatasetMetadataToDeposit(Set("text/xml"))
  private val depositId = "ds1"
  private val deposit: Deposit = Deposit(
    depositId = depositId,
    row = 1,
    depositorUserId = "dep",
    profile = Profile(
      titles = NonEmptyList.of("dataset title", "title2"), // TODO reject 2nd or put into dcmiMetadata
      descriptions = NonEmptyList.of("omschr1","omschr2"),
      creators = NonEmptyList.of(
        CreatorPerson(initials = "A.", surname = "Jones", organization = Option("Lorem ipsum dolor sit amet")),
        CreatorOrganization("consectetur adipiscing elit", Some(ContributorRole.SUPERVISOR)),
        CreatorOrganization("sed do eiusmod tempor incididunt ut labore et dolore magna aliqua")),
      created = DateTime.parse("1992-07-30"),
      available = DateTime.parse("1992-07-31"),
      audiences = NonEmptyList.of("everyone", "nobody", "some people", "people with yellow hear"),
      accessright = AccessCategory.NO_ACCESS
    ),
    baseUUID = Option(UUID.fromString("1de3f841-0f0d-048b-b3db-4b03ad4834d7")),
    metadata = Metadata(
      contributors = List(ContributorPerson(initials = "B.", surname = "Smith", organization = Option("Lorem ipsum dolor sit amet"), role = Some(ContributorRole.DATA_COLLECTOR))),
      alternatives = List("foobar"),
      publishers = List("random publisher"),
      identifiers = List(Identifier("123456", Some(IdentifierType.ISBN)), Identifier("id")),
      userLicense = Option(UserLicense("http://www.tapr.org/TAPR_Open_Hardware_License_v1.0.txt")),
      rightsholder = NonEmptyList.of("some rightsholder")
    )
  )

  private val expectedXml: Elem = <ddm:DDM
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
      <ddm:profile>
        <dc:title>dataset title</dc:title>
        <dc:title>title2</dc:title>
        <dcterms:description>omschr1</dcterms:description>
        <dcterms:description>omschr2</dcterms:description>
        <dcx-dai:creatorDetails>
          <dcx-dai:author>
            <dcx-dai:initials>A.</dcx-dai:initials>
            <dcx-dai:surname>Jones</dcx-dai:surname>
            <dcx-dai:organization>
              <dcx-dai:name xml:lang="en">Lorem ipsum dolor sit amet</dcx-dai:name>
            </dcx-dai:organization>
          </dcx-dai:author>
        </dcx-dai:creatorDetails>
        <dcx-dai:creatorDetails>
          <dcx-dai:organization>
            <dcx-dai:name xml:lang="en">consectetur adipiscing elit</dcx-dai:name>
            <dcx-dai:role>Supervisor</dcx-dai:role>
          </dcx-dai:organization>
        </dcx-dai:creatorDetails>
        <dcx-dai:creatorDetails>
          <dcx-dai:organization>
            <dcx-dai:name xml:lang="en">sed do eiusmod tempor incididunt ut labore et dolore magna aliqua</dcx-dai:name>
          </dcx-dai:organization>
        </dcx-dai:creatorDetails>
        <ddm:created>1992-07-30</ddm:created>
        <ddm:available>1992-07-31</ddm:available>
        <ddm:audience>everyone</ddm:audience>
        <ddm:audience>nobody</ddm:audience>
        <ddm:audience>some people</ddm:audience>
        <ddm:audience>people with yellow hear</ddm:audience>
        <ddm:accessRights>NO_ACCESS</ddm:accessRights>
      </ddm:profile>
      <ddm:dcmiMetadata>
        <dcterms:alternative>foobar</dcterms:alternative>
        <dcterms:publisher>random publisher</dcterms:publisher>
        <dcterms:type xsi:type="dcterms:DCMIType">Dataset</dcterms:type>
        <dc:identifier xsi:type="id-type:ISBN">123456</dc:identifier>
        <dc:identifier>id</dc:identifier>
        <dcterms:rightsHolder>some rightsholder</dcterms:rightsHolder>
        <dcx-dai:contributorDetails>
          <dcx-dai:author>
            <dcx-dai:initials>B.</dcx-dai:initials>
            <dcx-dai:surname>Smith</dcx-dai:surname>
            <dcx-dai:role>DataCollector</dcx-dai:role>
            <dcx-dai:organization><dcx-dai:name xml:lang="en">Lorem ipsum dolor sit amet</dcx-dai:name></dcx-dai:organization>
          </dcx-dai:author>
        </dcx-dai:contributorDetails>
        <dcterms:license xsi:type="dcterms:URI">http://www.tapr.org/TAPR_Open_Hardware_License_v1.0.txt</dcterms:license>
      </ddm:dcmiMetadata>
    </ddm:DDM>

  override def beforeEach(): Unit = {
    val targetFolder = multiDepositDir / depositId / "reisverslag"

    if (targetFolder.exists) targetFolder.delete()
    targetFolder.createDirectories()

    File(getClass.getResource("/allfields/input/ruimtereis01/reisverslag/centaur.mpg").toURI)
      .copyTo(targetFolder / "centaur.mpg")
    File(getClass.getResource("/allfields/input/ruimtereis01/reisverslag/centaur.srt").toURI)
      .copyTo(targetFolder / "centaur.srt")
  }

  "addDatasetMetadata" should "write the metadata to a file at the correct place" in {
    val file = stagingDatasetMetadataFile(depositId)
    if (file.exists) file.delete()

    file.toJava shouldNot exist

    action.addDatasetMetadata(deposit) shouldBe right[Unit]

    file.toJava should exist
  }

  "depositToDDM" should "return the expected xml" in {
    action.depositToDDM(deposit) should equalTrimmed(expectedXml)
  }

  it should "return xml on reading from the allfields input instructions csv" in {
    val multidepositDir = File(getClass.getResource("/allfields/input").toURI)
    MultiDepositParser.parse(multidepositDir, userLicenses).map(_.map(action.depositToDDM)).value should have size 4
  }

  "createDcmiMetadata" should "return the expected dcmidata" in {
    val metadata = Metadata(
      alternatives = List("alt1", "alt2"),
      publishers = List("pub1"),
      types = NonEmptyList.of(DcType.INTERACTIVERESOURCE, DcType.SOFTWARE),
      formats = List("arbitrary format", "text/xml"),
      identifiers = List(
        Identifier("123456", Some(IdentifierType.ISBN)),
        Identifier("id"),
        Identifier("987654", Some(IdentifierType.E_DNA))),
      sources = List("src", "test"),
      languages = List("eng", "nld"),
      spatials = List(Spatial("sp1")),
      rightsholder = NonEmptyList.of("rh1"),
      relations = List(
        QualifiedRelation(RelationQualifier.Replaces, link = Some(new URI("http://does.not.exist1.dans.knaw.nl/")), title = Some("t1")),
        QualifiedRelation(RelationQualifier.IsVersionOf, link = Some(new URI("http://does.not.exist2.dans.knaw.nl/"))),
        QualifiedRelation(RelationQualifier.HasVersion, title = Some("t3")),
        UnqualifiedRelation(link = Some(new URI("http://does.not.exist4.dans.knaw.nl/")), title = Some("t4")),
        UnqualifiedRelation(link = Some(new URI("http://does.not.exist5.dans.knaw.nl/"))),
        UnqualifiedRelation(title = Some("t6"))),
      dates = List(
        QualifiedDate(new DateTime(2017, 7, 30, 0, 0), DateQualifier.VALID),
        QualifiedDate(new DateTime(2017, 7, 31, 0, 0), DateQualifier.DATE_SUBMITTED),
        TextualDate("foobar")
      ),
      contributors = List(
        ContributorOrganization("contr1"),
        ContributorPerson(initials = "A.B.", surname = "Jones"),
        ContributorPerson(Option("dr."), "C.", Option("X"), "Jones", Option("contr2"), Option(ContributorRole.PROJECT_MANAGER), Option("dai"))),
      subjects = List(
        Subject("me"),
        Subject("you"),
        Subject("him"),
        Subject("GX", Option("abr:ABRcomplex"))),
      spatialPoints = List(
        SpatialPoint("1", "2", Option("RD")),
        SpatialPoint("3", "4"),
        SpatialPoint("5", "6", Option("degrees"))),
      spatialBoxes = List(
        SpatialBox("1", "2", "3", "4", Option("RD")),
        SpatialBox("5", "6", "7", "8", None),
        SpatialBox("9", "10", "11", "12", Option("degrees"))),
      temporal = List(
        Temporal("1992-2016"),
        Temporal("PALEOV", Option("abr:ABRperiode")),
        Temporal("some arbitrary text"))
    )

    val actual: Node = action.createMetadata(metadata)
    val expectedXml: Node = <ddm:dcmiMetadata>
      <dcterms:alternative>alt1</dcterms:alternative>
      <dcterms:alternative>alt2</dcterms:alternative>
      <dcterms:publisher>pub1</dcterms:publisher>
      <dcterms:type xsi:type="dcterms:DCMIType">InteractiveResource</dcterms:type>
      <dcterms:type xsi:type="dcterms:DCMIType">Software</dcterms:type>
      <dc:format>arbitrary format</dc:format>
      <dc:format xsi:type="dcterms:IMT">text/xml</dc:format>
      <dc:identifier xsi:type="id-type:ISBN">123456</dc:identifier>
      <dc:identifier>id</dc:identifier>
      <dc:identifier xsi:type="id-type:eDNA-project">987654</dc:identifier>
      <dc:source>src</dc:source>
      <dc:source>test</dc:source>
      <dc:language xsi:type='dcterms:ISO639-2'>eng</dc:language>
      <dc:language xsi:type='dcterms:ISO639-2'>nld</dc:language>
      <dcterms:spatial>sp1</dcterms:spatial>
      <dcterms:rightsHolder>rh1</dcterms:rightsHolder>
      <ddm:replaces href="http://does.not.exist1.dans.knaw.nl/">t1</ddm:replaces>
      <ddm:isVersionOf href="http://does.not.exist2.dans.knaw.nl/"/>
      <dcterms:hasVersion>t3</dcterms:hasVersion>
      <ddm:relation href="http://does.not.exist4.dans.knaw.nl/">t4</ddm:relation>
      <ddm:relation href="http://does.not.exist5.dans.knaw.nl/"/>
      <dc:relation>t6</dc:relation>
      <dcterms:valid>2017-07-30</dcterms:valid>
      <dcterms:dateSubmitted>2017-07-31</dcterms:dateSubmitted>
      <dc:date>foobar</dc:date>
      <dcx-dai:contributorDetails>
        <dcx-dai:organization>
          <dcx-dai:name xml:lang="en">contr1</dcx-dai:name>
        </dcx-dai:organization>
      </dcx-dai:contributorDetails>
      <dcx-dai:contributorDetails>
        <dcx-dai:author>
          <dcx-dai:initials>A.B.</dcx-dai:initials>
          <dcx-dai:surname>Jones</dcx-dai:surname>
        </dcx-dai:author>
      </dcx-dai:contributorDetails>
      <dcx-dai:contributorDetails>
        <dcx-dai:author>
          <dcx-dai:titles>dr.</dcx-dai:titles>
          <dcx-dai:initials>C.</dcx-dai:initials>
          <dcx-dai:insertions>X</dcx-dai:insertions>
          <dcx-dai:surname>Jones</dcx-dai:surname>
          <dcx-dai:role>ProjectManager</dcx-dai:role>
          <dcx-dai:DAI>dai</dcx-dai:DAI>
          <dcx-dai:organization>
            <dcx-dai:name xml:lang="en">contr2</dcx-dai:name>
          </dcx-dai:organization>
        </dcx-dai:author>
      </dcx-dai:contributorDetails>
      <dc:subject>me</dc:subject>
      <dc:subject>you</dc:subject>
      <dc:subject>him</dc:subject>
      <dc:subject xsi:type="abr:ABRcomplex">GX</dc:subject>
      <dcx-gml:spatial srsName="http://www.opengis.net/def/crs/EPSG/0/28992">
        <Point xmlns="http://www.opengis.net/gml">
          <pos>1 2</pos>
        </Point>
      </dcx-gml:spatial>
      <dcx-gml:spatial srsName="">
        <Point xmlns="http://www.opengis.net/gml">
          <pos>4 3</pos>
        </Point>
      </dcx-gml:spatial>
      <dcx-gml:spatial srsName="http://www.opengis.net/def/crs/EPSG/0/4326">
        <Point xmlns="http://www.opengis.net/gml">
          <pos>6 5</pos>
        </Point>
      </dcx-gml:spatial>
      <dcx-gml:spatial>
        <boundedBy xmlns="http://www.opengis.net/gml">
          <Envelope srsName="http://www.opengis.net/def/crs/EPSG/0/28992">
            <lowerCorner>4 2</lowerCorner>
            <upperCorner>3 1</upperCorner>
          </Envelope>
        </boundedBy>
      </dcx-gml:spatial>
      <dcx-gml:spatial>
        <boundedBy xmlns="http://www.opengis.net/gml">
          <Envelope srsName="">
            <lowerCorner>6 8</lowerCorner>
            <upperCorner>5 7</upperCorner>
          </Envelope>
        </boundedBy>
      </dcx-gml:spatial>
      <dcx-gml:spatial>
        <boundedBy xmlns="http://www.opengis.net/gml">
          <Envelope srsName="http://www.opengis.net/def/crs/EPSG/0/4326">
            <lowerCorner>10 12</lowerCorner>
            <upperCorner>9 11</upperCorner>
          </Envelope>
        </boundedBy>
      </dcx-gml:spatial>
      <dcterms:temporal>1992-2016</dcterms:temporal>
      <dcterms:temporal xsi:type="abr:ABRperiode">PALEOV</dcterms:temporal>
      <dcterms:temporal>some arbitrary text</dcterms:temporal>
    </ddm:dcmiMetadata>

    actual should equalTrimmed(expectedXml)
  }

  "createSurrogateRelation" should "return the expected streaming surrogate relation" in {
    val springfield = Springfield("randomdomainname", "randomusername", "randomcollectionname", PlayMode.Continuous)
    val expectedXml = <ddm:relation scheme="STREAMING_SURROGATE_RELATION">/domain/randomdomainname/user/randomusername/collection/randomcollectionname/presentation/$sdo-id</ddm:relation>

    action.createSurrogateRelation(springfield) should equalTrimmed(expectedXml)
  }

  it should "return a path with the default domain when no domain is specified" in {
    val springfield = Springfield(user = "randomusername", collection = "randomcollectionname", playMode = PlayMode.Continuous)
    val expectedXml = <ddm:relation scheme="STREAMING_SURROGATE_RELATION">/domain/dans/user/randomusername/collection/randomcollectionname/presentation/$sdo-id</ddm:relation>

    action.createSurrogateRelation(springfield) should equalTrimmed(expectedXml)
  }
}
