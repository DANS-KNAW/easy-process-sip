/**
 * Copyright (C) 2016 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.easy.multideposit.actions

import java.io.File

import nl.knaw.dans.common.lang.dataset.AccessCategory
import nl.knaw.dans.easy.multideposit._
import nl.knaw.dans.easy.multideposit.actions.AddDatasetMetadataToDeposit.datasetToXml
import nl.knaw.dans.easy.multideposit.parser._
import org.joda.time.DateTime
import org.scalatest.BeforeAndAfterAll

import scala.util.Success
import scala.xml.{ Elem, Node, Utility }

class AddDatasetMetadataToDepositSpec extends UnitSpec with BeforeAndAfterAll {

  implicit val settings = Settings(
    multidepositDir = new File(testDir, "md"),
    stagingDir = new File(testDir, "sd")
  )

  val datasetID = "ds1"
  val dataset: Dataset = Dataset(
    datasetId = datasetID,
    row = 1,
    depositorId = "dep",
    profile = Profile(
      titles = List("dataset title"),
      descriptions = List("omschr1"),
      creators = List(
        CreatorPerson(initials = "A.", surname = "Jones", organization = Option("Lorem ipsum dolor sit amet")),
        CreatorOrganization("consectetur adipiscing elit"),
        CreatorOrganization("sed do eiusmod tempor incididunt ut labore et dolore magna aliqua")),
      created = DateTime.parse("1992-07-30"),
      available = DateTime.parse("1992-07-31"),
      audiences = List("everyone", "nobody", "some people", "people with yellow hear"),
      accessright = AccessCategory.NO_ACCESS
    ),
    metadata = Metadata(
      alternatives = List("foobar"),
      types = List("random test data")
    )
  )

  val expectedXml: Elem = <ddm:DDM
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
  xsi:schemaLocation="http://easy.dans.knaw.nl/schemas/md/ddm/ https://easy.dans.knaw.nl/schemas/md/ddm/ddm.xsd">
    <ddm:profile>
      <dc:title>dataset title</dc:title>
      <dcterms:description>omschr1</dcterms:description>
      <dcx-dai:creatorDetails>
        <dcx-dai:author>
          <dcx-dai:initials>A.</dcx-dai:initials>
          <dcx-dai:surname>Jones</dcx-dai:surname>
          <dcx-dai:name xml:lang="en">Lorem ipsum dolor sit amet</dcx-dai:name>
        </dcx-dai:author>
      </dcx-dai:creatorDetails>
      <dcx-dai:creatorDetails>
        <dcx-dai:organization>
          <dcx-dai:name xml:lang="en">consectetur adipiscing elit</dcx-dai:name>
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
      <dcterms:type>random test data</dcterms:type>
    </ddm:dcmiMetadata>
  </ddm:DDM>

  override def beforeAll(): Unit = {
    super.beforeAll()
    new File(getClass.getResource("/allfields/input/ruimtereis01/reisverslag/centaur.mpg").toURI)
      .copyFile(new File(settings.multidepositDir, s"$datasetID/reisverslag/centaur.mpg"))
    new File(getClass.getResource("/allfields/input/ruimtereis01/reisverslag/centaur.srt").toURI)
      .copyFile(new File(settings.multidepositDir, s"$datasetID/reisverslag/centaur.srt"))
  }
  
  "execute" should "write the metadata to a file at the correct place" in {
    val file = stagingDatasetMetadataFile(datasetID)

    file should not (exist)

    AddDatasetMetadataToDeposit(dataset).execute shouldBe a[Success[_]]

    file should exist
  }

  "datasetToXml" should "return the expected xml" in {
    verify(datasetToXml(dataset), expectedXml)
  }

  it should "return xml on reading from the allfields input instructions csv" in {
    implicit val s2 = settings.copy(multidepositDir = new File(getClass.getResource("/allfields/input").toURI))
    val csv = new File(getClass.getResource("/allfields/input/instructions.csv").toURI)
    inside(new MultiDepositParser()(s2).parse(csv).map(_.map(datasetToXml))) {
      case Success(xmls) => xmls should have size 3
    }
  }

  "createDcmiMetadata" should "return the expected dcmidata" in {
    val metadata = Metadata(
      alternatives = List("alt1", "alt2"),
      publishers = List("pub1"),
      types = List("type1", "type2"),
      formats = List("text"),
      identifiers = List("ds1"),
      sources = List("src", "test"),
      languages = List("Scala", "Haskell"),
      spatials = List("sp1"),
      rightsholder = List("rh1"),
      relations = List(
        QualifiedLinkRelation("q1", "l1"),
        QualifiedTitleRelation("q2", "t1"),
        LinkRelation("l2"),
        TitleRelation("t2")),
      contributors = List(
        ContributorOrganization("contr1"),
        ContributorPerson(initials = "A.B.", surname = "Jones"),
        ContributorPerson(Option("dr."), "C.", Option("X"), "Jones", Option("contr2"), Option("dai"))),
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
        SpatialBoxx("1", "2", "3", "4", Option("RD")),
        SpatialBoxx("5", "6", "7", "8", None),
        SpatialBoxx("9", "10", "11", "12", Option("degrees"))),
      temporal = List(
        Temporal("1992-2016"),
        Temporal("PALEOV", Option("abr:ABRperiode")),
        Temporal("some arbitrary text"))
    )
    val expectedXml = <ddm>
      <ddm:dcmiMetadata>
        <dcterms:alternative>alt1</dcterms:alternative>
        <dcterms:alternative>alt2</dcterms:alternative>
        <dcterms:publisher>pub1</dcterms:publisher>
        <dcterms:type>type1</dcterms:type>
        <dcterms:type>type2</dcterms:type>
        <dc:format>text</dc:format>
        <dc:identifier>ds1</dc:identifier>
        <dc:source>src</dc:source>
        <dc:source>test</dc:source>
        <dc:language>Scala</dc:language>
        <dc:language>Haskell</dc:language>
        <dcterms:spatial>sp1</dcterms:spatial>
        <dcterms:rightsHolder>rh1</dcterms:rightsHolder>
        <dcterms:q1>l1</dcterms:q1>
        <dcterms:q2>t1</dcterms:q2>
        <dc:relation>l2</dc:relation>
        <dc:relation>t2</dc:relation>
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
            <dcx-dai:DAI>dai</dcx-dai:DAI>
            <dcx-dai:name xml:lang="en">contr2</dcx-dai:name>
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
    </ddm>
    verify(<ddm>{AddDatasetMetadataToDeposit.createMetadata(metadata)}</ddm>, expectedXml)
  }

  "createSurrogateRelation" should "return the expected streaming surrogate relation" in {
    val springfield = Springfield("randomdomainname", "randomusername", "randomcollectionname")
    val expectedXml = <ddm:relation scheme="STREAMING_SURROGATE_RELATION">/domain/randomdomainname/user/randomusername/collection/randomcollectionname/presentation/$sdo-id</ddm:relation>

    verify(AddDatasetMetadataToDeposit.createSurrogateRelation(springfield), expectedXml)
  }

  it should "return a path with the default domain when no domain is specified" in {
    val springfield = Springfield(user = "randomusername", collection = "randomcollectionname")
    val expectedXml = <ddm:relation scheme="STREAMING_SURROGATE_RELATION">/domain/dans/user/randomusername/collection/randomcollectionname/presentation/$sdo-id</ddm:relation>

    verify(AddDatasetMetadataToDeposit.createSurrogateRelation(springfield), expectedXml)
  }

  def verify(actualXml: Node, expectedXml: Node): Unit = {
    Utility.trim(actualXml).toString() shouldBe Utility.trim(expectedXml).toString()
  }
}
