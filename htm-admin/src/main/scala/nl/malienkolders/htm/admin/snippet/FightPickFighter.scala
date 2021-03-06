package nl.malienkolders.htm.admin.snippet

import net.liftweb._
import common._
import http._
import sitemap._
import Loc._
import util.Helpers._
import nl.malienkolders.htm.lib.model._
import java.text.SimpleDateFormat
import net.liftweb.http.js.JsCmds._
import java.util.Date
import scala.xml.Text
import nl.malienkolders.htm.lib.rulesets._

object FightPickFighter {

  val menu = (Menu.param[ParamInfo]("Pick Fighter", "Pick Fighter", s => Full(ParamInfo(s)),
    pi => pi.param) / "fights" / "pick" >> Hidden)
  lazy val loc = menu.toLoc

  def render = {

    val param = FightPickFighter.loc.currentValue.get.param
    val fightType = param.take(1)
    val id = param.drop(1).dropRight(1).toLong
    val side = param.takeRight(1)
    val current: Fight[_, _] = fightType match {
      case "E" => EliminationFight.findByKey(id).get
      case _ => FreeStyleFight.findByKey(id).get
    }
    val t = current.phase.foreign.get.tournament.foreign.get

    def fighter = side match {
      case "A" => current.fighterAFuture
      case _ => current.fighterBFuture
    }

    def redirect = RedirectTo(s"/tournaments/view/${t.identifier.get}#fight${current.id.get}") & Reload

    def pickFighter(p: Participant) = {
      fighter(SpecificFighter(Some(p)).format)
      current.save()
      redirect
    }

    def pickPoolFighter(p: Pool, ranking: Int) = {
      fighter(PoolFighter(p, ranking).format)
      current.save()
      redirect
    }

    def pickFightWinner(f: Fight[_, _]) = {
      f match {
        case ef: EliminationFight => fighter(Winner(ef).format)
        case ff: FreeStyleFight => fighter(Winner(ff).format)
        case _ => //ignore
      }
      current.save()
      redirect
    }

    def pickFightLoser(f: Fight[_, _]) = {
      f match {
        case ef: EliminationFight => fighter(Loser(ef).format)
        case ff: FreeStyleFight => fighter(Loser(ff).format)
        case _ => //ignore
      }
      current.save()
      redirect
    }

    def average(s: Scores): Scores = {
      val denominator = s.fields(0).value().max(1)
      GenericScores(s.numberOfFights, s.fields.map(s =>
        s.copy(value = () => s.value().toString.toDouble / denominator)))
    }

    def globalRanking = {
      implicit val random = scala.util.Random
      val r = t.poolPhase.rulesetImpl
      val rows = r.ranking(t.poolPhase).flatMap {
        case (pool, poolParticipants) =>
          poolParticipants.map { case (participant, scores) => (pool, participant, scores, average(scores)) }
      }
      rows.sortBy(_._4)
    }

    val mappings = if (t.poolPhase.pools.isEmpty) {
      "#pick-participant" #> (
        "* [class]" #> "in active" &
        "thead .pool" #> Nil &
        "thead .score" #> Nil &
        ".participant" #> t.subscriptions.map { s =>
          val p = s.participant.foreign.get
          ".number *" #> s.fighterNumber.get &
            ".name *" #> SHtml.a(() => pickFighter(p), Text(p.name.get)) &
            ".pool" #> Nil &
            ".score" #> Nil
        }) &
        "#pick-pool" #> Nil &
        ".nav .pool" #> Nil

    } else {
      "#pick-participant" #> (
        "thead" #> (".score" #> t.poolPhase.rulesetImpl.emptyScore.header) &
        ".participant" #> globalRanking.map {
          case (pool, participant, scores, average) =>
            ".number *" #> participant.subscription(t).get.fighterNumber.get &
              ".name *" #> SHtml.a(() => pickFighter(participant), Text(participant.name.get)) &
              ".pool *" #> pool.poolName &
              ".score" #> (("* *" #> scores.numberOfFights) :: (scores.fields.zip(average.fields).map { case (s, avg) => "* *" #> <span><span class="absolute-value">{ s.value().toString }</span>/<span class="average-value">{ avg.value().toString }</span></span> }).toList)

        }) &
        "#pick-pool" #> (
          ".pool" #> t.poolPhase.pools.map(p =>
            ".name *" #> s"Pool ${p.poolName}" &
              ".number" #> (1 to 8).map(i =>
                "a" #> SHtml.a(() => pickPoolFighter(p, i), Text(i.toString)))))
    }

    if (t.freeStylePhase.fights.isEmpty && t.eliminationPhase.fights.isEmpty) {
      mappings &
        "#pick-fight" #> Nil &
        ".nav .fight" #> Nil

    } else {
      mappings &
        "#pick-fight" #> (
          ".fight" #> (t.freeStylePhase.fights ++ t.eliminationPhase.fights).filterNot(_.id.is == current.id.is).map(f =>
            ".name *" #> f.name.get &
              ".winner *" #> SHtml.a(() => pickFightWinner(f), Text("Winner")) &
              ".loser *" #> SHtml.a(() => pickFightLoser(f), Text("Loser"))))
    }
  }

}