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
package nl.knaw.dans.easy.multideposit.model

import java.util.UUID

import better.files.File

case class Instructions(depositId: DepositId,
                        row: Int,
                        depositorUserId: DepositorUserId,
                        profile: Profile,
                        baseUUID: Option[BaseUUID] = Option.empty,
                        metadata: Metadata = Metadata(),
                        files: Map[File, FileDescriptor] = Map.empty,
                        audioVideo: AudioVideo = AudioVideo()) {
  def toDeposit(fileMetadatas: Seq[FileMetadata] = Seq.empty): Deposit = {
    Deposit(
      depositId = depositId,
      baseUUID = baseUUID,
      row = row,
      depositorUserId = depositorUserId,
      profile = profile,
      metadata = metadata,
      files = fileMetadatas,
      springfield = audioVideo.springfield
    )
  }
}

case class Deposit(depositId: DepositId,
                   bagId: BagId = UUID.randomUUID(),
                   baseUUID: Option[BaseUUID] = Option.empty,
                   row: Int,
                   depositorUserId: DepositorUserId,
                   profile: Profile,
                   metadata: Metadata = Metadata(),
                   files: Seq[FileMetadata] = Seq.empty,
                   springfield: Option[Springfield] = Option.empty)
