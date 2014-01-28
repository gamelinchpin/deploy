package magenta
package tasks

// represents a command line to execute
//  including the ability to actually execute and become remote
case class CommandLine(commandLine: List[String], successCodes: List[Int] = List(0)) {
  lazy val quoted = commandLine map quoteIfNeeded mkString " "
  private def quoteIfNeeded(s: String) = if (s.contains(" ")) "\"" + s + "\"" else s

  def suppressor(filteredOut: String => Unit) = { line:String =>
    if ( !line.startsWith("tcgetattr") &&
      !line.startsWith("Warning: Permanently added") )
        filteredOut(line)
  }

  def capAt(cap: Int, filteredOut: String => Unit): String => Unit = new CapAt(cap, filteredOut).processLine

  def run() {
    import sys.process._
    MessageBroker.infoContext("$ " + quoted) {
      val returnValue = commandLine ! ProcessLogger(
        capAt(250, MessageBroker.commandOutput),
        suppressor(capAt(250, MessageBroker.commandError))
      )
      MessageBroker.verbose("return value " + returnValue)
      if (!successCodes.contains(returnValue)) {
        MessageBroker.fail(s"Exit code $returnValue from command: $quoted")
      }
    }
  }
}

class CapAt(capAt:Int, forwarder: String => Unit) {
  var counter = 0
  def processLine(line:String) {
    counter = counter+1
    counter match {
      case normal if normal < capAt => forwarder(line)
      case notify if notify % capAt == 0 => forwarder(s"WARNING: Too many lines output ($counter) ignoring output")
      case _ =>
    }
  }
}

object CommandLine {
  implicit def fromStringList(c: List[String]) = CommandLine(c)
  implicit def fromString(c: String) = CommandLine(List(c))
}