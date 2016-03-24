/**
 * Copyright (C) 2015-2016 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
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

import nl.knaw.dans.easy.multideposit.{Settings, UnitSpec, _}
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}

import scala.util.Success

class AddBagToDepositSpec extends UnitSpec with BeforeAndAfter with BeforeAndAfterAll {

  implicit val settings = Settings(
    multidepositDir = new File(testDir, "md"),
    outputDepositDir = new File(testDir, "dd")
  )

  val datasetID = "ds1"
  val file1Text = "abcdef"
  val file2Text = "defghi"
  val file3Text = "ghijkl"
  val file4Text = "jklmno"
  val file5Text = "mnopqr"

  before {
    new File(multiDepositDir(settings, datasetID), "file1.txt").write(file1Text)
    new File(multiDepositDir(settings, datasetID), "folder1/file2.txt").write(file2Text)
    new File(multiDepositDir(settings, datasetID), "folder1/file3.txt").write(file3Text)
    new File(multiDepositDir(settings, datasetID), "folder2/file4.txt").write(file4Text)
    new File(multiDepositDir(settings, "ds2"), "folder3/file5.txt").write("file5Text")

    outputDepositBagDir(settings, datasetID).mkdirs
  }

  after {
    outputDepositBagDir(settings, datasetID).deleteDirectory()
  }

  override def afterAll = testDir.getParentFile.deleteDirectory()

  "checkPreconditions" should "succeed if the md folder does not exist" in {
    val inputDir = multiDepositDir(settings, datasetID)
    inputDir.deleteDirectory()

    AddBagToDeposit(1, datasetID).checkPreconditions shouldBe a[Success[_]]
  }

  it should "succeed if the md folder exists" in {
    AddBagToDeposit(1, datasetID).checkPreconditions shouldBe a[Success[_]]
  }

  "run" should "succeed given the current setup" in {
    AddBagToDeposit(1, datasetID).run() shouldBe a[Success[_]]
  }

  it should "create a bag with the files from ds1 in it and some meta-files around" in {
    AddBagToDeposit(1, datasetID).run() shouldBe a[Success[_]]

    val root = outputDepositBagDir(settings, datasetID)
    root should exist
    root.listRecursively.map(_.getName) should contain theSameElementsAs
      List("bag-info.txt",
        "bagit.txt",
        "file1.txt",
        "file2.txt",
        "file3.txt",
        "file4.txt",
        "manifest-md5.txt",
        "tagmanifest-md5.txt")
    outputDepositBagDataDir(settings, datasetID) should exist
  }

  it should "preserve the file content after making the bag" in {
    AddBagToDeposit(1, datasetID).run() shouldBe a[Success[_]]

    val root = outputDepositBagDataDir(settings, datasetID)
    new File(root, "file1.txt").read() shouldBe file1Text
    new File(root, "folder1/file2.txt").read() shouldBe file2Text
    new File(root, "folder1/file3.txt").read() shouldBe file3Text
    new File(root, "folder2/file4.txt").read() shouldBe file4Text
  }

  it should "create a bag with no files in data when the input directory does not exist" in {
    val settings = Settings(
      multidepositDir = new File(testDir, "md-empty"),
      outputDepositDir = new File(testDir, "dd")
    )

    val outputDir = outputDepositBagDir(settings, datasetID)
    outputDir.mkdirs
    outputDir should exist

    multiDepositDir(settings, datasetID) should not (exist)

    AddBagToDeposit(1, datasetID)(settings).run() shouldBe a[Success[_]]

    outputDepositDir(settings, datasetID) should exist
    outputDepositBagDataDir(settings, datasetID) should exist
    outputDepositBagDataDir(settings, datasetID).listRecursively shouldBe empty
    outputDepositBagDir(settings, datasetID).listRecursively.map(_.getName) should contain theSameElementsAs
      List("bag-info.txt",
        "bagit.txt",
        "manifest-md5.txt",
        "tagmanifest-md5.txt")

    val root = outputDepositBagDir(settings, datasetID)
    new File(root, "manifest-md5.txt").read() shouldBe empty
    new File(root, "tagmanifest-md5.txt").read() should include ("bag-info.txt")
    new File(root, "tagmanifest-md5.txt").read() should include ("bagit.txt")
    new File(root, "tagmanifest-md5.txt").read() should include ("manifest-md5.txt")
  }
}
