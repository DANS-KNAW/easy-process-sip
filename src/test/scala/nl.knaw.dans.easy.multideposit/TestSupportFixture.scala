/**
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
package nl.knaw.dans.easy.multideposit

import java.util.UUID

import better.files.File
import better.files.File.currentWorkingDirectory
import nl.knaw.dans.common.lang.dataset.AccessCategory
import nl.knaw.dans.easy.multideposit.PathExplorer.{ InputPathExplorer, OutputPathExplorer, StagingPathExplorer }
import nl.knaw.dans.easy.multideposit.model._
import org.joda.time.DateTime
import org.scalatest.enablers.Existence
import org.scalatest._

trait TestSupportFixture extends FlatSpec with Matchers with OptionValues with EitherValues with ValidatedValues with Inside with InputPathExplorer with StagingPathExplorer with OutputPathExplorer {

  implicit def existenceOfFile[FILE <: better.files.File]: Existence[FILE] = _.exists

  lazy val testDir: File = {
    val path = currentWorkingDirectory / s"target/test/${ getClass.getSimpleName }"
    if (path.exists) path.delete()
    path.createDirectories()
    path
  }
  override val multiDepositDir: File = testDir / "md"
  override val stagingDir: File = testDir / "sd"
  override val outputDepositDir: File = testDir / "od"
  override val reportFile: File = testDir / "report.csv"

  implicit val inputPathExplorer: InputPathExplorer = this
  implicit val stagingPathExplorer: StagingPathExplorer = this
  implicit val outputPathExplorer: OutputPathExplorer = this

  private val userLicensesFile: File = currentWorkingDirectory / "src" / "main" / "assembly" / "dist" / "cfg" / "licenses.txt"
  val userLicenses =
    if (userLicensesFile.exists) userLicensesFile.lines.map(_.trim).toSet
    else fail("Cannot find file: licenses.txt")

  def testInstructions1: Instructions = {
    Instructions(
      depositId = "ruimtereis01",
      row = 2,
      depositorUserId = "ruimtereiziger1",
      profile = Profile(
        titles = List("Reis naar Centaur-planetoïde", "Trip to Centaur asteroid"),
        descriptions = List("Een tweedaagse reis per ruimteschip naar een bijzondere planetoïde in de omgeving van Jupiter.", "A two day mission to boldly go where no man has gone before"),
        creators = List(
          CreatorPerson(
            titles = Some("Captain"),
            initials = "J.T.",
            surname = "Kirk",
            organization = Some("United Federation of Planets")
          )
        ),
        created = DateTime.parse("2015-05-19"),
        audiences = List("D30000"),
        accessright = AccessCategory.OPEN_ACCESS
      ),
      baseUUID = Option(UUID.fromString("1de3f841-0f0d-048b-b3db-4b03ad4834d7")),
      metadata = Metadata(
        formats = List("video/mpeg", "text/plain"),
        languages = List("NL", "encoding=UTF-8"),
        subjects = List(Subject("astronomie"), Subject("ruimtevaart"), Subject("planetoïden"))
      ),
      files = Map(
        testDir / "md/ruimtereis01/reisverslag/centaur.mpg" -> FileDescriptor(Option("flyby of centaur")),
        testDir / "md/ruimtereis01/path/to/a/random/video/hubble.mpg" -> FileDescriptor(Option("video about the hubble space telescope")),
      ),
      audioVideo = AudioVideo(
        springfield = Option(Springfield("dans", "janvanmansum", "Jans-test-files", PlayMode.Menu)),
        avFiles = Map(
          testDir / "md/ruimtereis01/reisverslag/centaur.mpg" -> Set(
            SubtitlesFile(testDir / "md/ruimtereis01/reisverslag/centaur.srt", Option("en")),
            SubtitlesFile(testDir / "md/ruimtereis01/reisverslag/centaur-nederlands.srt", Option("nl"))
          )
        )
      )
    )
  }

  def testInstructions2: Instructions = {
    Instructions(
      depositId = "deposit-2",
      row = 5,
      depositorUserId = "ruimtereiziger2",
      profile = Profile(
        titles = List("Title 1 of deposit 2", "Title 2 of deposit 2"),
        descriptions = List("A sample deposit with a not very long description"),
        creators = List(CreatorOrganization("Creator A")),
        created = DateTime.now(),
        available = DateTime.parse("2016-07-30"),
        audiences = List("D37000"),
        accessright = AccessCategory.GROUP_ACCESS
      ),
      baseUUID = Option(UUID.fromString("1de3f841-0f0d-048b-b3db-4b03ad4834d7")),
      metadata = Metadata(
        contributors = List(ContributorOrganization("Contributor 1"), ContributorOrganization("Contributor 2")),
        subjects = List(Subject("subject 1", Option("abr:ABRcomplex")), Subject("subject 2"), Subject("subject 3")),
        publishers = List("publisher 1"),
        types = List(DcType.STILLIMAGE),
        identifiers = List(Identifier("id1234"))
      ),
      files = Map(
        testDir / "md/ruimtereis02/path/to/images/Hubble_01.jpg" -> FileDescriptor(Some("Hubble"), Some(FileAccessRights.RESTRICTED_REQUEST))
      )
    )
  }
}
