package nl.knaw.dans.easy.multideposit.actions

import java.io.File
import javax.naming.directory.{ Attributes, BasicAttribute, BasicAttributes }

import nl.knaw.dans.easy.multideposit.{ Ldap, Settings, UnitSpec, _ }
import org.scalamock.scalatest.MockFactory
import org.scalatest.BeforeAndAfterAll

import scala.util.{ Failure, Success }

class RetrieveDatamanagerActionSpec extends UnitSpec with BeforeAndAfterAll with MockFactory {

  val ldapMock: Ldap = mock[Ldap]
  implicit val settings = Settings(
    multidepositDir = new File(testDir, "md"),
    stagingDir = new File(testDir, "sd"),
    datamanager = "dm",
    ldap = ldapMock
  )

  private val correctDatamanagerAttrs = createDatamanagerAttributes()

  /**
   * Default creates correct BasicAttributes
   */
  def createDatamanagerAttributes(state: String = "ACTIVE",
                                  roles: Seq[String] = Seq("USER","ARCHIVIST"),
                                  mail: String = "dm@test.org"): BasicAttributes = {

    val a = new BasicAttributes()
    a.put("dansState", state)
    a.put({
      val r = new BasicAttribute("easyRoles")
      roles.foreach(r.add)
      r
    })
    a.put("mail", mail)
    a
  }

  def mockLdapForDatamanager(attrs: Attributes): Unit = {
    (ldapMock.query(_: String)(_: Attributes => Attributes)) expects ("dm", *) returning Success(Seq(attrs))
  }

  override def afterAll: Unit = testDir.getParentFile.deleteDirectory()

  "checkPreconditions" should "succeed if the datamanager email can be retrieved" in {
    mockLdapForDatamanager(correctDatamanagerAttrs)

    RetrieveDatamanagerAction().checkPreconditions shouldBe a[Success[_]]
  }

  it should "fail if ldap does not return anything for the datamanager" in {
    (ldapMock.query(_: String)(_: Attributes => Attributes)) expects ("dm", *) returning Success(Seq.empty)

    inside(RetrieveDatamanagerAction().checkPreconditions) {
      case Failure(ActionException(_, message, _)) => message should include ("""The datamanager "dm" is unknown""")
    }
  }

  it should "fail when the datamanager is not an active user" in {
    val nonActiveDatamanagerAttrs = createDatamanagerAttributes(state = "BLOCKED")
    mockLdapForDatamanager(nonActiveDatamanagerAttrs)

    inside(RetrieveDatamanagerAction().checkPreconditions) {
      case Failure(ActionException(_, message, _)) => message should include ("not an active user")
    }
  }

  it should "fail when the datamanager is not an achivist" in {
    val nonArchivistDatamanagerAttrs = createDatamanagerAttributes(roles = Seq("USER"))
    mockLdapForDatamanager(nonArchivistDatamanagerAttrs)

    inside(RetrieveDatamanagerAction().checkPreconditions) {
      case Failure(ActionException(_, message, _)) => message should include ("is not an archivist")
    }
  }

  it should "fail when the datamanager has no email" in {
    val nonEmailDatamanagerAttrs = createDatamanagerAttributes(mail = "")
    mockLdapForDatamanager(nonEmailDatamanagerAttrs)

    inside(RetrieveDatamanagerAction().checkPreconditions) {
      case Failure(ActionException(_, message, _)) => message should include ("does not have an email address")
    }
  }

  "execute" should "generate the properties file" in {
    mockLdapForDatamanager(correctDatamanagerAttrs)

    inside(RetrieveDatamanagerAction().execute()) {
      case Success(mail) => mail shouldBe "dm@test.org"
    }
  }
}
