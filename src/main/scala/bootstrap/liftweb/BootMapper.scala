package bootstrap.liftweb

import net.liftweb._
import util._
import Helpers._

import common._
import http._
import mapper._
import sitemap._
import Loc._

import code.model._

/**
 * Boot Mapper config.
 */
object BootMapper {
  def boot {
    if (!DB.jndiJdbcConnAvailable_?) {
      val vendor = new DBVendor(Props.get("db.driver") openOr "org.h2.Driver",
                                Props.get("db.url") openOr "jdbc:h2:lift_proto.db;AUTO_SERVER=TRUE",
                                Props.get("db.user"),
                                Props.get("db.password"),
                                Props.get("db.schema"))

      LiftRules.unloadHooks.append(vendor.closeAllConnections_! _)
      DB.defineConnectionManager(DefaultConnectionIdentifier, vendor)
    }

    // Mapper default rules
    MapperRules.columnName = (_,name) => StringHelpers.snakify(name)
    MapperRules.tableName  = (_,name) => StringHelpers.snakify(name)

    // Use Lift's Mapper ORM to populate the database
    // you don't need to use Mapper to use Lift... use
    // any ORM you want
    val cmds = Schemifier.schemify(true, Schemifier.infoF _, User)
    if (!cmds.isEmpty)
    {
      sys.error("Database scheme is out of date. The following is missing:\n" + cmds.mkString("\n"))
    }
  }
}

class DBVendor(val driverName: String, val dbUrl: String, val dbUser: Box[String], val dbPassword: Box[String], val schemaName: Box[String] = Empty)
  extends StandardDBVendor(driverName, dbUrl, dbUser, dbPassword)
{
  Class.forName(driverName)

  override def newSuperConnection(name: ConnectionIdentifier) : Box[SuperConnection] =
    newConnection(name).map(c => new SuperConnection(c, () => releaseConnection(c), tryo(schemaName openOr "PUBLIC")))
}
