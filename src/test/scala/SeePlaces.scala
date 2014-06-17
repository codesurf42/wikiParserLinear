import org.scalatest._

/**
  * Created by ab on 16.06.14.
 */
class SeePlaces extends FlatSpec with Matchers {
  "A parser" should "return whole section" in {

    val p = new ArticleParsingLib

    val strip =
      """
        |foo
        |
        |""".stripMargin

    assert(strip === "\nfoo\n\n")

    val markup0 =
    """  asas
        |==Fee==
        |foo bar
        |==Bar==
        |bar1 bar2
        |""".stripMargin

    p.getSection(markup0, "==Fee==") should be ("foo bar")

    val markup1 =
      """  asas
        |==Fee2==
        |foo bar
        | baz
        |==Bar==
        |bar1 bar2
        |
        |""".stripMargin

    p.getSection(markup1, "==Fee2==") should be ("foo bar\n baz")
    p.getSection(markup1, "==Bar==") should be ("bar1 bar2\n\n")

    val markup =
    """ aooo asjka
        |==See==
        |[[File:Binnendieze 's-Hertogenbosch.jpg|thumb|The Binnendieze is a maze of canals under the city.]]
        |[[File:Sint-Jans-Hertogenbosch.jpg|thumb|The large St John's Cathedral is a central point in the city.]]
        |'s-Hertogenbosch is a medieval city and among the oldest cities in the Netherlands. When the Netherlands were still young it was a fortified city that served for the protection of The Netherlands. Especially on the south side of the city, a lot of these fortifications have been saved and over time restored.  Start at ''Bastion Vught'' and walk northwards via the ''Parklaan'', ''Spinhuiswal'', ''Zuidwal'' and ''Bastion Oranje'' and ''Hekellaan'' until you reach the bridge over the ''Zuid Willemsvaart''. This way you covert the best part of the old fortifications. In 2004, the city was awarded ''European Fortress of the year''.
        |
        |The '''Sint Jans Cathedral''' is one of the most prominent landmarks of Den Bosch. Building on the cathedral as we know it right now started in 1380 and is built in Gothic style. Because the exterior of the building is deteriorating fast due to toxic rain they started in 1998 with the restoration of the exterior. It will take years to restore the full church, but the first sections are already finished and can be seen at this moment. This restoration only applies to the exterior.
        |
        |==Do==
        |*  {{do
        || name=Efteling Park | alt= | url=http://www.efteling.com/ | email=
        || address= | lat=51.64979 | long=5.04895 | directions=
        || phone= | t
        |
      """.stripMargin

    val sectionSee =
    """[[File:Binnendieze 's-Hertogenbosch.jpg|thumb|The Binnendieze is a maze of canals under the city.]]
        |[[File:Sint-Jans-Hertogenbosch.jpg|thumb|The large St John's Cathedral is a central point in the city.]]
        |'s-Hertogenbosch is a medieval city and among the oldest cities in the Netherlands. When the Netherlands were still young it was a fortified city that served for the protection of The Netherlands. Especially on the south side of the city, a lot of these fortifications have been saved and over time restored.  Start at ''Bastion Vught'' and walk northwards via the ''Parklaan'', ''Spinhuiswal'', ''Zuidwal'' and ''Bastion Oranje'' and ''Hekellaan'' until you reach the bridge over the ''Zuid Willemsvaart''. This way you covert the best part of the old fortifications. In 2004, the city was awarded ''European Fortress of the year''.
        |
        |The '''Sint Jans Cathedral''' is one of the most prominent landmarks of Den Bosch. Building on the cathedral as we know it right now started in 1380 and is built in Gothic style. Because the exterior of the building is deteriorating fast due to toxic rain they started in 1998 with the restoration of the exterior. It will take years to restore the full church, but the first sections are already finished and can be seen at this moment. This restoration only applies to the exterior.
        |""".stripMargin
    val sectionDo =
    """*  {{do
      || name=Efteling Park | alt= | url=http://www.efteling.com/ | email=
      || address= | lat=51.64979 | long=5.04895 | directions=
      || phone= | t
      |
      |      """.stripMargin

    p.getSection(markup, "==See==") should be (sectionSee)
    p.getSection(markup, "==Do==") should be (sectionDo)

    val markup2 =
      """===Restaurants===
        |The city centre is packed with small and large restaurants that serve all kinds of crowds. The '''Korte Putstraat''' and the '''Lange Putstraat''' are your best bet if you're looking for a meal, as they have a particularly broad selection of places with nice outdoor terraces in summer. Typically you'll have no problems finding a table somewhere, but if you have a particular establishment in mind or if you want a good table on the terrace it's definitely wise to reserve ahead, as the best places are often full.
        |
        |===Budget===
        |*{{eat
        || name=Eetcafé 't Keershuys  | alt= | url= http://www.keershuys.nl/| email=info@keershuys.nl
        || address=L
        |
        |""".stripMargin

    val sectionBudget =
      """*{{eat
        || name=Eetcafé 't Keershuys  | alt= | url= http://www.keershuys.nl/| email=info@keershuys.nl
        || address=L
        |
        |""".stripMargin
//    println(s"[$markup2]")
//    println(s"==> section:[$sectionBudget]")

    p.getSection(markup2, "===Budget===") should be (sectionBudget)
  }
}
