package virtualvoid.linux

import java.io._

object ParserHelper {
  trait Conversion {
    def asLong: Long
  }
  implicit def conversion(str: String) = new Conversion {
    def asLong = java.lang.Long.parseLong(str)
    def asHexLong = java.lang.Long.parseLong(str, 16)
  }
}

/**
 * Reads in a smaps file and shuffles, sorts and sums up data inside.
 */
object SmapsReader {
  case class SmapsEntry(name: String,
                        from: Long,
                        to: Long,
                        perms: String,
                        offset: Long,
                        dev: String,
                        inode: Long,
                        stats: Map[String, Long])

  import ParserHelper._

  val EntryHeader = """([0-9a-f]{8})-([0-9a-f]{8}) (.{4}) ([0-9a-f]{8}) (.{5}) (\d+)(?:\s+(.*))?""".r
  val DataLine = """(\w+):\s+(\d*) kB""".r
  val dataReader: PartialFunction[String, (String, Long)] = { case DataLine(name, number) => (name, number.asLong) }

  def read(reader: BufferedReader): List[SmapsEntry] = {
    def collectData: (Map[String, Long], String) = {
      val res = new scala.collection.mutable.HashMap[String, Long]
      var line = reader.readLine
      while (dataReader.isDefinedAt(line)) {
        res += dataReader.apply(line)
        line = reader.readLine
      }
      (res.toMap, line)
    }
    def readEntry(first: String): (SmapsEntry, String) = first match {
      case EntryHeader(from, to, perms, offset, dev, inode, name) =>       
        val (stats, nextLine) = collectData
        (SmapsEntry(name, from.asHexLong, to.asHexLong, perms, offset.asHexLong, dev, inode.asLong, stats), nextLine)
    }
    var line = reader.readLine
    val buffer = new scala.collection.mutable.ListBuffer[SmapsEntry]
    do {
      val (entry, next) = readEntry(line)
      buffer += entry
      line = next
    } while (line != null)
    buffer.toList
  }

  def main(args: Array[String]): Unit = {
    val reader = new BufferedReader(new InputStreamReader(System.in))
    println(read(reader))
  }
}
