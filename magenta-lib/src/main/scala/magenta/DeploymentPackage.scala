package magenta

import java.io.File
import org.json4s._
import magenta.deployment_type.DeploymentType

case class DeploymentPackage(
  name: String,
  pkgApps: Seq[App],
  pkgSpecificData: Map[String, JValue],
  deploymentTypeName: String,
  srcDir: File) {

  def mkAction(name: String): Action = pkgType.mkAction(name)(this)

  lazy val pkgType = DeploymentType.all find (_.name == deploymentTypeName) getOrElse (
    throw new IllegalArgumentException(s"Package type $deploymentTypeName of package $name is unknown")
  )

  val apps = pkgApps
}