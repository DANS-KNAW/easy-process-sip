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
package nl.knaw.dans.easy.multideposit

import cats.syntax.either._
import javax.naming.directory.{ Attributes, SearchControls }
import javax.naming.ldap.LdapContext

import scala.collection.JavaConverters._

trait Ldap extends AutoCloseable {

  protected val ctx: LdapContext

  def query[T](userId: String)(f: Attributes => T): FailFast[Seq[T]] = {
    Either.catchNonFatal {
      val searchFilter = s"(&(objectClass=easyUser)(uid=$userId))"
      val searchControls = new SearchControls() {
        setSearchScope(SearchControls.SUBTREE_SCOPE)
      }

      ctx.search("dc=dans,dc=knaw,dc=nl", searchFilter, searchControls)
        .asScala.toSeq
        .map(f compose (_.getAttributes))
    }.leftMap(e => ActionError(e.getMessage, e))
  }

  override def close(): Unit = ctx.close()
}
object Ldap {
  def apply(context: LdapContext): Ldap = new Ldap {
    override protected val ctx: LdapContext = context
  }
}
