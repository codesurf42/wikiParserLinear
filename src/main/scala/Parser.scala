//import akka.routing.RoundRobinRouter
//import scala.concurrent.ExecutionContext.Implicits.global
//import akka.agent.Agent
//import akka.actor._
//import akka.event.Logging
import scala.io.Source
import scala.util.matching.Regex
import scala.xml.pull._
import scala.xml.pull.EvComment
import scala.xml.pull.EvElemEnd
import scala.xml.pull.EvElemStart
import scala.xml.pull.EvEntityRef
import scala.xml.pull.EvProcInstr
import scala.xml.pull.EvText

/**
 * Created by ab on 10.06.14.
 */
case class XmlFilename(name: String)
case class Article(title: String, text: String)
case class ArticleSummary(title: String, length: Int)

object log {
  def debug(a: Any*):Unit = {}
}

object XmlReader {
//  val log = Logging(this, this)

  def receive(data: Any) = data match {
    case XmlFilename(name) =>
      readXmlFile(name)
      LongestArticle.receive("stats")

      Parser.met ! ShowTime()

    case _ =>
  }

  def readXmlFile(name: String) = {
//    log.debug(s"Reading file $name")
    // parse xml content

    val t1 = TimeLib.getTime

    val xml = new XMLEventReader(Source.fromFile(name))

    val t2 = TimeLib.getTime
    Parser.met ! ExecTime("readXml-FromFile", t2-t1)

    parseXml(xml)

    val t3 = TimeLib.getTime
    Parser.met ! ExecTime("readXml-parseXml", t3-t2)
    println(f"Exec time: ${(t3 - t1) / Math.pow(10, 9)}%.2f sec")

  }

  def parseXml(xml: XMLEventReader) {
    var inPage = false
    var inPageText = false
    var inPageTitle = false
    var skipPage = false
    var pageText = ""
    var lastTitle = ""

    while (xml.hasNext) {
      log.debug("next: " + xml)
      val t1 = TimeLib.getTime

      xml.next match {
        //          case EvElemStart(pre, label, attrs, scope) if label == "mediawiki" =>
        //            println(s"!!! - $label")
        case EvElemStart(_, "page", _, _)               => inPage = true
        case EvElemStart(_, "title", _, _) if inPage    => inPageTitle = true
        case EvElemStart(_, "redirect", _, _) if inPage => skipPage = true // just skip them now

        case EvElemStart(_, label, _, _) if inPage && label == "text" => inPageText = true
        case EvElemStart(_, label, _, _) if inPage => log.debug(s"Elem of page: <$label>")
        case EvElemStart(pre, label, attrs, scope) => log.debug("START: ", pre, label, attrs, scope)

        case EvElemEnd(_, "page") =>
          inPage = false
          skipPage = false
          lastTitle = "" // safer to clear

        case EvElemEnd(_, "title")  => inPageTitle = false
        case EvElemEnd(_, "text")   =>
          log.debug(s"Got full article text [$lastTitle] - process it!")

//          context.actorSelection("/user/article") ! Article(lastTitle, pageText)
          ArticleParser.receive(Article(lastTitle, pageText))

          pageText = ""
          inPageText = false

        case EvElemEnd(pre, label)                    => log.debug("END: ",pre, label)
        case EvText(text) if inPageTitle              => lastTitle = text
        case EvText(text) if inPageText && !skipPage  => pageText += text
        case EvText(text)                             => log.debug("TEXT: " + text)

        case EvEntityRef(entity) if inPageText =>
          // TODO: add pageText entities to text!!! (how about entities from titles?)
          log.debug(s"Entity in text: ${entity}")
        case EvEntityRef(entity) => log.debug("ENTITY: " + entity)
        //        case POISON =>
        case EvProcInstr(target, text) => log.debug(s"PROCINSTR: $target, $text")
        case EvComment(text) => log.debug(s"EVCOMMENT: $text")
        case _ =>
      }
      Parser.met ! ExecTime("xmlHasNext", TimeLib.getTime - t1)
    }
  }
}

object ArticleParser {
//  val log = Logging(context.system, this)

  def receive(data: Any) = data match {
    case art: Article => parseArticle(art)
    case _ =>
  }

  def parseArticle(art: Article) = {
    val t1 = TimeLib.getTime

    val limit = 50000
    log.debug(s"Parsing article: [${art.title}}] == " +
      art.text.substring(0,(
        if (art.text.length() > limit) limit
        else art.text.length()))
      + "..."
    )

    val t2 = TimeLib.getTime
    Parser.met ! ExecTime("parseArticle-1", t2-t1)

//    context.actorSelection("/user/longestArticle") ! ArticleSummary(art.title, art.text.length())
    LongestArticle.receive(ArticleSummary(art.title, art.text.length()))

    val t3 = TimeLib.getTime

    if (true) {
      val ap = new WikiParsing()
      val geoPos = ap.getGeo(art)
      val seePlaces = ap.getSeePlaces(art)
    } else {
//      val geoPos = context.actorSelection("/user/geoParser") ! art
//      val seePlaces = context.actorSelection("/user/seePlaces") ! art
    }
    Parser.met ! ExecTime("parseArticle-2", TimeLib.getTime-t3)
  }
}

class ArticleGeoParser {
  val ap = new WikiParsing()
  def receive(data: Any): Any = data match {
    case e: Article => return ap.getGeo(e)
    case _ =>
  }
}

class ArticleSeePlacesParser  {
  val ap = new WikiParsing()
  def receive(data: Any): Any = data match {
    case e: Article => return ap.getSeePlaces(e)
    case _ =>
  }
}

abstract class Subsection()
case class SubsectionString(s: String) extends Subsection
case class SubsectionMap(keyVal: Map[String, String]) extends Subsection

object LongestArticle {
//  val log = Logging(context.system, this)
  var max = 0
  var count = 0 // naive implementation, or actually a counter of processed msg by single actor
  var titleMax = ""

  def receive(data: Any): Unit = data match {
    case e: ArticleSummary =>
      val t1 = TimeLib.getTime

      log.debug(s"Got: ${e.title} [${e.length}]")
      count += 1
      if (count % 1000 == 0) println(count)

//      Parser.agentCount.send(_ + 1)

      // a bit more complex computation for an agent
      if (max < e.title.length)
        max = e.title.length

      // we can just compare which one is the longest
      if (titleMax.length < e.title.length)
        titleMax = e.title
      Parser.met ! ExecTime("longestArticle", TimeLib.getTime - t1)

    case "stats" =>
      println(s"LongestArt: $max ($titleMax), count: $count, $count")
  }
}

object Parser extends App {

//  implicit val system = ActorSystem("parser")
  //  val log = Logger

//  val reader = system.actorOf(Props[XmlReader], "reader")
//  val parser = system.actorOf(Props[ArticleParser].withRouter(RoundRobinRouter(3)), "article")
//  val art = system.actorOf(Props[LongestArticle].withRouter(RoundRobinRouter(2)), "longestArticle")
//  val geo = system.actorOf(Props[ArticleGeoParser].withRouter(RoundRobinRouter(2)), "geoParser")
//  val seePl = system.actorOf(Props[ArticleSeePlacesParser], "seePlaces")
//  val met = system.actorOf(Props[Metrics], "metrics")
  val met = Metrics

//  val agentCount = Agent(0)
//  val agentMaxArtTitle = Agent("")
//  val agentMaxArtTitleLen = Agent(0)

  println("Sending flnm")
//  val inbox = Inbox.create(system)

  val file = if (false) "/tmp/wiki/enwiki_part1.xml" // shorter partial file of
  else "/tmp/wiki/enwikivoyage-20140520-pages-articles.xml" // full unpacked XML dump of Wiki-Voyage

//  inbox.send(reader, XmlFilename(file))
  XmlReader.receive(XmlFilename(file))

}
