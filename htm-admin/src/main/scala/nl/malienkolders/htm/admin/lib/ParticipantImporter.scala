package nl.malienkolders.htm.admin.lib

import scala.xml._
import java.net.URL
import scala.io.Source
import scala.util.matching._
import nl.malienkolders.htm.lib.model._
import nl.malienkolders.htm.admin.model._
import net.liftweb.mapper._
import net.liftweb.util._
import TimeHelpers._
import nl.htm.importer.DummyImporter
import java.io.File
import nl.htm.importer.EventData

object ParticipantImporter {

  def doImport(data: EventData) = {

    val noCountry = Country.find(By(Country.code2, "")).get

    Arena.bulkDelete_!!()
    // create as many arenas as are missing
    for (i <- 1 to data.arenas) {
      Arena.create.name("Arena " + i).save()
    }

    val tournaments = if (Tournament.count == 0) {
      data.tournaments.map { case t => Tournament.create.name(t.name).identifier(t.id).saveMe }
    } else {
      Tournament.findAll(OrderBy(Tournament.id, Ascending))
    }

    val ps = data.participants.map(p => Participant.create.externalId(p.sourceIds.head.id).
      name(p.name).
      shortName(p.shortName).
      club(p.club).
      clubCode(p.clubCode).
      country(Country.find(By(Country.code2, p.country)).getOrElse(noCountry)))

    // insert participant if it doesn't exist yet
    ps.foreach(_.save)

    // add participants to tournaments
    tournaments.foreach { t =>
      t.participants.clear
      t.save
    }
    data.subscriptions.foreach {
      case (tournament, subs) =>
        tournaments.find(_.identifier == tournament.id).foreach { t =>
          t.subscriptions ++= subs.map {
            case (sub, p) =>
              TournamentParticipants.create.participant(ps.find(_.externalId.get == p.sourceIds.head.id).get).fighterNumber(sub.number).primary(sub.primary).experience(sub.xp)
          }
          if (t.rounds.size == 0) {
            t.rounds += Round.create.name("Round 1").
              order(1).
              ruleset(tournament.ruleset).
              timeLimitOfFight(180 seconds).
              breakDuration(0).
              breakInFightAt(0).
              timeBetweenFights(120 seconds).
              exchangeLimit(10)
          }
          val round = t.rounds.head
          subs.foreach {
            case (sub, p) =>
              sub.pool foreach { poolNr =>
                while (round.pools.size < poolNr)
                  round.addPool
                round.pools.find(_.order.get == poolNr).foreach { pool =>
                  pool.participants += ps.find(_.externalId.get == p.sourceIds.head.id).get
                }
              }
          }
        }

    }
    tournaments.foreach(_.save)

  }

}