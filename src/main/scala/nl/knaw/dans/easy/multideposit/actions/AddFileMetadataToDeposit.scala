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

import nl.knaw.dans.easy.multideposit.actions.AddFileMetadataToDeposit._
import nl.knaw.dans.easy.multideposit.{ Settings, UnitAction, _ }
import nl.knaw.dans.easy.multideposit.parser.{ AVFile, Dataset, FileAccessRights, Subtitles }
import nl.knaw.dans.lib.error._
import org.apache.tika.Tika

import scala.util.control.NonFatal
import scala.util.{ Failure, Success, Try }
import scala.xml.Elem

case class AddFileMetadataToDeposit(dataset: Dataset)(implicit settings: Settings) extends UnitAction[Unit] {

  sealed abstract class AudioVideo(val vocabulary: String)
  case object Audio extends AudioVideo("http://schema.org/AudioObject")
  case object Video extends AudioVideo("http://schema.org/VideoObject")

  class FileMetadata(val filepath: File, val mimeType: MimeType)
  case class AVFileMetadata(override val filepath: File,
                            override val mimeType: MimeType,
                            vocabulary: AudioVideo,
                            title: String,
                            accessibleTo: FileAccessRights.UserCategory,
                            subtitles: Seq[Subtitles]
                           ) extends FileMetadata(filepath, mimeType)

  private lazy val fileInstructions: Map[File, AVFile] = {
    dataset.audioVideo.avFiles.map(avFile => avFile.file -> avFile).toMap
  }

  private lazy val avAccessibility: FileAccessRights.Value = {
    dataset.audioVideo.accessibility
      .getOrElse(FileAccessRights.accessibleTo(dataset.profile.accessright))
  }

  private def getFileMetadata(file: File): Try[FileMetadata] = {
    def mkFileMetadata(m: MimeType, voca: AudioVideo): AVFileMetadata = {
      fileInstructions.get(file) match {
        case Some(AVFile(_, optTitle, subtitles)) =>
          val title = optTitle.getOrElse(file.getName)
          AVFileMetadata(file, m, voca, title, avAccessibility, subtitles)
        case None =>
          AVFileMetadata(file, m, voca, file.getName, avAccessibility, Seq.empty)
      }
    }

    getMimeType(file).map {
      case mimeType if mimeType startsWith "audio" => mkFileMetadata(mimeType, Audio)
      case mimeType if mimeType startsWith "video" => mkFileMetadata(mimeType, Video)
      case mimeType => new FileMetadata(file, mimeType)
    }
  }

  private lazy val fileMetadata: Try[Seq[FileMetadata]] = Try {
    val datasetDir = multiDepositDir(dataset.datasetId)
    if (datasetDir.exists()) {
      datasetDir.listRecursively
        .map(file => getFileMetadata(file.getAbsoluteFile))
        .collectResults
    }
    else // if the dataset does not contain any data
      Success { List.empty }
  }.flatten

  /**
   * Verifies whether all preconditions are met for this specific action.
   * All files referenced in the instructions are checked for existence.
   *
   * @return `Success` when all preconditions are met, `Failure` otherwise
   */
  override def checkPreconditions: Try[Unit] = {
    def checkSFColumnsIfDatasetContainsAVFiles(mimetypes: Seq[FileMetadata]): Try[Unit] = {
      val avFiles = mimetypes.collect { case fmd: AVFileMetadata => fmd.filepath }

      (dataset.audioVideo.springfield.isDefined, avFiles.isEmpty) match {
        case (true, false) | (false, true) => Success(())
        case (true, true) =>
          Failure(ActionException(dataset.row,
            "Values found for these columns: [SF_DOMAIN, SF_USER, SF_COLLECTION]\n" +
              "cause: these columns should be empty because there are no audio/video files " +
              "found in this dataset"))
        case (false, false) =>
          Failure(ActionException(dataset.row,
            "No values found for these columns: [SF_USER, SF_COLLECTION]\n" +
              "cause: these columns should contain values because audio/video files are " +
              s"found:\n${ avFiles.map(filepath => s" - $filepath").mkString("\n") }"))
      }
    }

    fileMetadata.flatMap(checkSFColumnsIfDatasetContainsAVFiles)
  }

  override def execute(): Try[Unit] = {
    datasetToFileXml
      .map(stagingFileMetadataFile(dataset.datasetId).writeXml(_))
      .recoverWith {
        case NonFatal(e) => Failure(ActionException(dataset.row, s"Could not write file meta data: $e", e))
      }
  }

  def datasetToFileXml: Try[Elem] = {
    fileMetadata.map(fileXmls(_) match {
      case Nil => <files xmlns:dcterms="http://purl.org/dc/terms/"/>
      case files =>
        // @formatter:off
        <files xmlns:dcterms="http://purl.org/dc/terms/">{files}</files>
        // @formatter:on
    })
  }

  private def fileXmls(fmds: Seq[FileMetadata]): Seq[Elem] = {
    fmds.map {
      case av: AVFileMetadata => avFileXml(av)
      case fmd: FileMetadata => fileXml(fmd)
    }
  }

  private def fileXml(fmd: FileMetadata): Elem = {
    // @formatter:off
    <file filepath={s"data/${formatFilePath(fmd.filepath)}"}>
      <dcterms:format>{fmd.mimeType}</dcterms:format>
    </file>
    // @formatter:on
  }

  private def avFileXml(fmd: AVFileMetadata): Elem = {
    // @formatter:off
    <file filepath={s"data/${formatFilePath(fmd.filepath)}"}>
      <dcterms:type>{fmd.vocabulary.vocabulary}</dcterms:type>
      <dcterms:format>{fmd.mimeType}</dcterms:format>
      <dcterms:title>{fmd.title}</dcterms:title>
      <dcterms:accessRights>{fmd.accessibleTo}</dcterms:accessRights>
      {fmd.subtitles.map(subtitleXml)}
    </file>
    // @formatter:on
  }

  private def subtitleXml(subtitle: Subtitles): Elem = {
    val filepath = formatFilePath(subtitle.file)

    // @formatter:off
    subtitle.language
      .map(lang => <dcterms:relation xml:lang={lang}>{s"data/$filepath"}</dcterms:relation>)
      .getOrElse(<dcterms:relation>{s"data/$filepath"}</dcterms:relation>)
    // @formatter:on
  }

  private def formatFilePath(file: File): File = {
    multiDepositDir(dataset.datasetId)
      .getAbsoluteFile
      .toPath
      .relativize(file.toPath)
      .toFile
  }
}

object AddFileMetadataToDeposit {
  private val tika = new Tika
  type MimeType = String

  /**
   * Identify the mimeType of a file.
   *
   * @param file the file to identify
   * @return the mimeType of the file if the identification was successful; `Failure` otherwise
   */
  def getMimeType(file: File): Try[MimeType] = Try {
    tika.detect(file)
  }
}
