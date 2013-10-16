package nl.malienkolders.htm.admin.snippet

import net.liftweb._
import http._
import mapper._
import util.Helpers._
import scala.xml.NodeSeq
import nl.malienkolders.htm.lib.model._
import java.text.SimpleDateFormat
import java.util.Date

class ArenaList {

  val df = new SimpleDateFormat("HH:mm")

  def render = {
    val colspan = 12 / Arena.count
    ".arena" #> Arena.findAll.map(a =>
      ".arena [class+]" #> ("col-md-" + colspan) &
        ".arenaName" #> a.name.get &
        ".pool" #> a.pools.map { implicit p =>
          implicit val r = p.round.foreign.get
          implicit val t = r.tournament.foreign.get
          ".pool [class+]" #> (if (p.finished_?) "success" else "waiting") &
            ".time *" #> df.format(new Date(p.startTime.is)) &
            ".tournament *" #> tournamentName &
            ".round *" #> roundName &
            ".poolName *" #> poolName
        })
  }

  def poolName(implicit t: Tournament, p: Pool) =
    <a href={ "/tournaments/view/" + t.identifier.get + "#pool" + p.id.get }>
      { "Pool " + p.order.get }
    </a>

  def roundName(implicit t: Tournament, r: Round) =
    <a href={ "/tournaments/view/" + t.identifier.get + "#round" + r.id.get }>
      { r.name.get }
    </a>

  def tournamentName(implicit t: Tournament) =
    <a href={ "/tournaments/view/" + t.identifier.get }>
      { t.name.get }
    </a>

}