package virtualvoid.linux

import java.io._
import java.math.BigInteger

import scala.util.control.NonFatal

object ParserHelper {
  trait Conversion {
    def asLong: Long
  }
  implicit def conversion(str: String) = new Conversion {
    def asLong = java.lang.Long.parseLong(str)
    def asHexLong = new BigInteger(str, 16).longValue()
  }
}

/**
 * Reads in a /proc/<pid>/smaps file and shuffles, sorts and sums up data inside.
 */
object SmapsReader {
  case class SmapsEntry(
      name:   String,
      from:   Long,
      to:     Long,
      perms:  String,
      offset: Long,
      dev:    String,
      inode:  Long,
      stats:  Map[String, Long])

  case class ProcessSmaps(pid: Int, cmdLine: Seq[String], entries: Seq[SmapsEntry]) {
    def total(key: String): Long =
      entries.map(_.stats.get(key).getOrElse(0L)).sum

    lazy val cmd: String =
      mapCmdLineInfo(
        cmdLine.headOption
          .getOrElse("")
          .split(' ')
          .headOption.getOrElse(""), cmdLine)

    def extraInfo: String =
      if (cmd == "sbt")
        " " + workingDirectory.getName
      else
        ""

    def workingDirectory = new File("/proc/" + pid + "/cwd").getCanonicalFile
  }

  def mapCmdLineInfo(cmd: String, cmdLine: Seq[String]): String =
    if (cmd.contains("java"))
      mapJavaCmdLineInfo(cmd, cmdLine)
    else
      cmd

  def mapJavaCmdLineInfo(cmd: String, cmdLine: Seq[String]): String =
    if (cmdLine.exists(_.contains("sbt")))
      "sbt"
    else
      cmd

  import ParserHelper._

  val EntryHeader = """([0-9a-f]{8,16})-([0-9a-f]{8,16}) (.{4}) ([0-9a-f]{8}) (.{5}) (\d+)(?:\s+(.*))?""".r
  val VmFlagsLine = """VmFlags: .*""".r
  val DataLine = """(\w+):\s+(\d*) kB""".r
  val dataReader: PartialFunction[String, (String, Long)] = { case DataLine(name, number) => (name, number.asLong) }

  def read(reader: BufferedReader): List[SmapsEntry] = try {
    def collectData: (Map[String, Long], String) = {
      val res = new scala.collection.mutable.HashMap[String, Long]
      var line = reader.readLine
      while (dataReader.isDefinedAt(line)) {
        res += dataReader.apply(line)
        line = reader.readLine
      }
      while (VmFlagsLine.unapplySeq(line).isDefined) line = reader.readLine()
      (res.toMap, line)
    }

    def readEntry(first: String): (SmapsEntry, String) = first match {
      case EntryHeader(from, to, perms, offset, dev, inode, name) =>
        val (stats, nextLine) = collectData
        (SmapsEntry(name, from.asHexLong, to.asHexLong, perms, offset.asHexLong, dev, inode.asLong, stats), nextLine)
    }
    var line = reader.readLine
    if (line != null) {
      val buffer = new scala.collection.mutable.ListBuffer[SmapsEntry]
      do {
        val (entry, next) = readEntry(line)
        buffer += entry
        line = next
      } while (line != null)
      buffer.toList
    } else Nil
  } catch {
    case io: IOException if io.getMessage.contains("Permission denied") => Nil // ignore
    case NonFatal(e) =>
      e.printStackTrace()
      Nil
  }

  def output(processData: ProcessSmaps): Unit = {
    val data = processData.entries

    println("Statistics for " + processData.pid + " " + processData.cmdLine)
    def total(key: String) =
      data.map(_.stats(key)).sum

    def firstX(key: String) =
      data.sortBy(-_.stats(key)) filter (_.stats(key) > 0) take 10

    def stat(key: String) =
      "Total " + key + ": " + total(key) + " kB\n" + (firstX(key).map(e => "%5d kB RSS: %5d kB %08x-%08x %8d %s".format(e.stats(key), e.stats("Rss"), e.from, e.to, e.inode, e.name)) mkString "\n")

    println(stat("Shared_Clean"))
    println(stat("Shared_Dirty"))
    println(stat("Private_Clean"))
    println(stat("Private_Dirty"))
    println(stat("Swap"))
  }
  def splitAt[T](x: Seq[T], at: T): Seq[Seq[T]] =
    if (x.isEmpty)
      Nil
    else {
      val (start, rest) = x.span(_ != at)
      start +: splitAt(rest.drop(1), at)
    }
  def readCmdLine(pid: Int): Seq[String] = {
    val file = "/proc/" + pid + "/cmdline"
    val is = new FileInputStream(file)
    val bytes = new collection.mutable.ArrayBuffer[Byte](file.length)

    val buffer = new Array[Byte](1000)
    var read = is.read(buffer, 0, file.length)
    bytes ++= buffer.take(read)
    while (read != -1) {
      read = is.read(buffer, 0, file.length)
      bytes ++= buffer.take(read)
    }

    splitAt[Byte](bytes.toSeq, 0).map(x => new String(x.toArray))
  }
  def readProcessSmaps(pid: Int): ProcessSmaps = {
    val reader = new BufferedReader(new FileReader("/proc/" + pid + "/smaps"))
    val smaps = read(reader)
    reader.close()

    ProcessSmaps(pid, readCmdLine(pid), smaps)
  }
  def isProcess(name: String): Boolean = try {
    name.toInt
    true
  } catch {
    case NonFatal(_) => false
  }
  def isAccessible(f: File): Boolean =
    new File(f, "smaps").canRead

  def printUnderlined(string: String) {
    println(string)
    println(string.replaceAll("[^\\|]", "-"))
  }

  def main(args: Array[String]): Unit = {
    if (args.size > 0)
      output(readProcessSmaps(args(0).toInt))
    else {
      val proc = new File("/proc")
      val processes = proc.listFiles().filter(isAccessible).map(_.getName).filter(isProcess).map(_.toInt)

      val maps = processes.map(readProcessSmaps)

      def stat(key: String) {
        val list = maps.map(x => (x, x.total(key))).sortBy(-_._2)
        println("Total " + key + ": " + maps.map(_.total(key)).sum + " KB\n")
        println("Top 20 " + key + "\n")
        printUnderlined("Memory Usg | PID   | Command line")

        list.take(20).foreach {
          case (proc, size) =>
            println("%7d KB | %5d | %s%s" format (size, proc.pid, proc.cmd, proc.extraInfo))
        }
        println()
        println("Top 20 %s by cmd\n" format key)
        printUnderlined("Memory Usg | Command line")
        val byCmd = maps.groupBy(_.cmd).mapValues(_.map(_.total(key)).sum).toSeq.sortBy(-_._2)
        byCmd.take(20).foreach {
          case (cmd, size) =>
            println("%7d KB | %s" format (size, cmd))
        }
      }
      stat("Pss")
      println()
      stat("Swap")
    }
  }
}
