package virtualvoid.linux

import java.io._
import java.math.BigInteger

import virtualvoid.linux.SmapsReader.mapCmdLineInfo

import scala.io.Source
import scala.util.control.NonFatal

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

object SmapsReaderApp extends App {
  import SmapsReader._

  if (args.size > 0)
    output(readProcessSmaps(args(0).toInt))
  else {
    val proc = new File("/proc")
    val processes = proc.listFiles().filter(isAccessible).map(_.getName).filter(isProcess).map(_.toInt)
    val maps = processes.map(readProcessSmaps)

    def stat(key: String): Unit = {
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
      val byCmd = maps.groupBy(_.cmd).view.mapValues(_.map(_.total(key)).sum).toSeq.sortBy(-_._2)
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

/**
 * Reads in a /proc/<pid>/smaps file and shuffles, sorts and sums up data inside.
 */
object SmapsReader {
  val EntryHeader = """([0-9a-f]{8,16})-([0-9a-f]{8,16}) (.{4}) ([0-9a-f]{8,9}) (.{5}) (\d+)(?:\s+(.*))?""".r
  val EntryLine = """(\w+):\s+(\d*)(?: kB)?""".r
  val VmFlagsLine = """VmFlags: .*""".r

  def readProcessSmaps(pid: Int): ProcessSmaps = {
    val smaps = read("/proc/" + pid + "/smaps")

    ProcessSmaps(pid, readCmdLine(pid), smaps)
  }

  def read(smapsFile: String): Vector[SmapsEntry] = try {
    trait ReadingState {
      def nextLine(line: String): ReadingState
      def complete: List[SmapsEntry]
    }
    case class WaitingForHeader(existingEntries: List[SmapsEntry]) extends ReadingState {
      override def nextLine(line: String): ReadingState = line match {
        case EntryHeader(from, to, perms, offset, dev, inode, name) =>
          ReadingEntries(
            SmapsEntry(name.intern, from.asHexLong, to.asHexLong, perms.intern, offset.asHexLong, dev.intern, inode.toLong, _),
            Map.empty, existingEntries
          )
      }
      override def complete: List[SmapsEntry] = existingEntries.reverse
    }
    case class ReadingEntries(finish: Map[String, Long] => SmapsEntry, collected: Map[String, Long], existingEntries: List[SmapsEntry]) extends ReadingState {
      override def nextLine(line: String): ReadingState = line match {
        case EntryLine(name, number) => copy(collected = collected + (name.intern -> number.toLong))
        case VmFlagsLine()           => WaitingForHeader(finish(collected) :: existingEntries)
      }
      override def complete: List[SmapsEntry] = (finish(collected) :: existingEntries).reverse
    }
    Source.fromFile(smapsFile)
      .getLines
      .foldLeft(WaitingForHeader(Nil): ReadingState)(_.nextLine(_))
      .complete
      .toVector
  } catch {
    case io: IOException if io.getMessage.contains("Permission denied") => Vector.empty // ignore
    case _: FileNotFoundException                                       => Vector.empty // ignore, process might be gone already (SubstrateVM also reports permission denied as FileNotFoundException)
    case NonFatal(e) =>
      println(s"Problem in file [$smapsFile]")
      e.printStackTrace()
      Vector.empty
  }

  def output(processData: ProcessSmaps): Unit = {
    val data = processData.entries

    println("Statistics for " + processData.pid + " " + processData.cmdLine)
    def total(key: String) =
      data.map(_.stats(key)).sum

    def firstX(key: String) =
      data.filter(_.stats(key) > 0).sortBy(-_.stats(key)) take 10

    def stat(key: String) =
      "Total " + key + ": " + total(key) + " kB\n" + (firstX(key).map(e => "%5d kB RSS: %5d kB %08x-%08x %8d %s".format(e.stats(key), e.stats("Rss"), e.from, e.to, e.inode, e.name)) mkString "\n")

    println(stat("Shared_Clean"))
    println(stat("Shared_Dirty"))
    println(stat("Private_Clean"))
    println(stat("Private_Dirty"))
    println(stat("Swap"))
  }

  def readCmdLine(pid: Int): Seq[String] = try {
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
  } catch {
    case _: IOException => Nil // ignore
    case NonFatal(e) =>
      println(s"Problem getting cmdline for [$pid]")
      e.printStackTrace()
      Nil
  }
  def splitAt[T](x: Seq[T], at: T): Seq[Seq[T]] =
    if (x.isEmpty)
      Nil
    else {
      val (start, rest) = x.span(_ != at)
      start +: splitAt(rest.drop(1), at)
    }
  def isProcess(name: String): Boolean = try {
    name.toInt
    true
  } catch {
    case NonFatal(_) => false
  }
  def isAccessible(f: File): Boolean =
    new File(f, "smaps").canRead

  def printUnderlined(string: String): Unit = {
    println(string)
    println(string.replaceAll("[^|]", "-"))
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

  implicit class Conversion(val str: String) extends AnyVal {
    def asHexLong = new BigInteger(str, 16).longValue()
  }
}
