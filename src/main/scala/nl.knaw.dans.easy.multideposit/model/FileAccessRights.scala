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

import nl.knaw.dans.common.lang.dataset.AccessCategory
import nl.knaw.dans.common.lang.dataset.AccessCategory._

/**
 * Enumeration of the file properties VisibleTo and AccessibleTo
 * Copied from easy-stage-file-item
 */
// TODO move this enum to the DDM library
object FileAccessRights extends Enumeration {

  type FileAccessRights = Value

  val
  ANONYMOUS, // a user that is not logged in
  RESTRICTED_REQUEST, // a user that received permission to access the dataset
  NONE // none of the above
  = Value

  private val rightsMap = Map[AccessCategory, FileAccessRights.Value](
    // https://github.com/DANS-KNAW/easy-app/blob/1080eff457/lib/easy-business/src/main/java/nl/knaw/dans/easy/domain/model/AccessibleTo.java#L23-L37
    // the legacy code lacks OPEN_ACCESS, ACCESS_ELSEWHERE, FREELY_AVAILABLE
    ANONYMOUS_ACCESS -> ANONYMOUS,
    REQUEST_PERMISSION -> RESTRICTED_REQUEST,
    OPEN_ACCESS -> ANONYMOUS,
    // deprecated keys
    OPEN_ACCESS_FOR_REGISTERED_USERS -> RESTRICTED_REQUEST,
    GROUP_ACCESS -> RESTRICTED_REQUEST,
    NO_ACCESS -> NONE, // used by Excel2EasyMetadataXMLTask.java for invalid values in some spread-sheet cell
    ACCESS_ELSEWHERE -> NONE,
    FREELY_AVAILABLE -> ANONYMOUS // used for thumbnails in EASY-v1
  )

  // map should be exhaustive
  require(AccessCategory.values().toSet == rightsMap.keySet)

  /**
   *
   * @param s toString value of the desired category
   * @return
   */
  def valueOf(s: String): Option[FileAccessRights.Value] =
    FileAccessRights.values.find(_.toString equalsIgnoreCase s)

  /** gets the default category of users that have download permission for files in a new dataset,
   * an archivist may decide differently
   *
   * @param datasetAccesCategory from the EMD of the dataset
   * @return
   */
  def accessibleTo(datasetAccesCategory: AccessCategory): FileAccessRights.Value =
    rightsMap(datasetAccesCategory)
}
