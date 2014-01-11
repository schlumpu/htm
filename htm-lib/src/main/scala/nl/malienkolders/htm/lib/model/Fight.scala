package nl.malienkolders.htm.lib
package model

import net.liftweb._
import mapper._
import common._
import util._
import Helpers._
import scala.xml._

case class FightId(phase: String, id: Long)

case class MarshalledFight(
  tournament: MarshalledTournamentSummary,
  phaseType: String,
  id: Long,
  name: String,
  fighterA: MarshalledFighter,
  fighterB: MarshalledFighter,
  timeStart: Long,
  timeStop: Long,
  netDuration: Long,
  scores: List[MarshalledScore],
  timeLimit: Long,
  exchangeLimit: Int,
  possiblePoints: List[Int])
case class MarshalledFightSummary(
  tournament: MarshalledTournamentSummary,
  phaseType: String,
  id: Long,
  name: String,
  fighterA: MarshalledFighter,
  fighterB: MarshalledFighter,
  timeStart: Long,
  timeStop: Long,
  netDuration: Long,
  scores: List[MarshalledScore],
  timeLimit: Long,
  exchangeLimit: Int,
  possiblePoints: List[Int])
case class MarshalledFighter(label: String, participant: Option[MarshalledParticipant])

sealed abstract class Fighter {
  def format: String
  def participant: Option[Participant]
  def sameAs(other: Fighter): Boolean
  def toMarshalled = MarshalledFighter(toString, participant.map(_.toMarshalled))
}
object Fighter {
  def parse(s: String): Fighter = Winner.parse(s)
    .orElse(Loser.parse(s))
    .orElse(PoolFighter.parse(s))
    .orElse(SpecificFighter.parse(s))
    .getOrElse(UnknownFighter(s))
}
case class Winner(fight: EliminationFight) extends Fighter {
  def format = "F" + fight.id.get + "W"
  def participant = fight.finished_? match {
    case true => fight.winner
    case false => None
  }
  def sameAs(other: Fighter) = other match {
    case w: Winner => fight.id.get == w.fight.id.get
    case _ => false
  }
  override def toString = "Winner of " + fight.name.is
}
object Winner extends (EliminationFight => Winner) {
  val re = """^F(\d+)W$""".r

  def parse(s: String): Option[Winner] = re.findFirstIn(s) match {
    case Some(re(fightId)) => Some(Winner(EliminationFight.findByKey(fightId.toLong).get))
    case None => None
  }
}
case class Loser(fight: EliminationFight) extends Fighter {
  def format = "F" + fight.id.get + "L"
  def participant = fight.finished_? match {
    case true => fight.loser
    case false => None
  }
  def sameAs(other: Fighter) = other match {
    case l: Loser => fight.id.get == l.fight.id.get
    case _ => false
  }
  override def toString = "Loser of " + fight.name.is
}
object Loser extends (EliminationFight => Loser) {
  val re = """^F(\d+)L$""".r

  def parse(s: String): Option[Loser] = re.findFirstIn(s) match {
    case Some(re(fightId)) => Some(Loser(EliminationFight.findByKey(fightId.toLong).get))
    case None => None
  }
}
case class PoolFighter(pool: Pool, ranking: Int) extends Fighter {
  def format = "P" + pool.id.get + ":" + ranking
  def participant = pool.finished_? match {
    case true => Some(pool.ranked(ranking - 1))
    case false => None
  }
  def sameAs(other: Fighter) = other match {
    case pf: PoolFighter => pool.id.get == pf.pool.id.get && ranking == pf.ranking
    case _ => false
  }
  override def toString = "Number " + ranking + " of pool " + pool.poolName
}
object PoolFighter extends ((Pool, Int) => PoolFighter) {
  val re = """^P(\d+):(\d+)$""".r

  def parse(s: String): Option[PoolFighter] = re.findFirstIn(s) match {
    case Some(re(poolId, ranking)) => Pool.findByKey(poolId.toLong) match {
      case Full(p) => Some(PoolFighter(p, ranking.toInt))
      case _ => None
    }
    case None => None
  }
}
case class SpecificFighter(override val participant: Option[Participant]) extends Fighter {
  def format = participant.map(_.id.get.toString).getOrElse("PICK")
  def sameAs(other: Fighter) = other match {
    case SpecificFighter(Some(otherParticipant)) => participant.map(_.id.is == otherParticipant.id.is).getOrElse(false)
    case _ => false
  }
  override def toString = participant.map(_.name.get).getOrElse("")
}
object SpecificFighter extends (Option[Participant] => SpecificFighter) {
  val re = """^(\d+)$""".r

  def parse(s: String): Option[SpecificFighter] = re.findFirstIn(s) match {
    case Some(re(participantId)) => Some(SpecificFighter(Some(Participant.findByKey(participantId.toLong)).get))
    case None if s == "PICK" => Some(SpecificFighter(None))
    case None => None
  }
}
case class UnknownFighter(label: String) extends Fighter {
  def format = label
  def participant = None
  def sameAs(other: Fighter) = false
  override def toString = label
}

trait Fight[F <: Fight[F, S], S <: Score[S, F]] extends LongKeyedMapper[F] with IdPK with FightToScore[F, S] {

  self: F =>

  object name extends MappedString(this, 128)
  object inProgress extends MappedBoolean(this)
  object fighterAFuture extends MappedString(this, 16)
  def fighterAFuture(f: Fighter): F = fighterAFuture(f.format)
  def fighterAFuture(p: Participant): F = fighterAFuture(SpecificFighter(Some(p)))
  object fighterBFuture extends MappedString(this, 16)
  def fighterBFuture(f: Fighter): F = fighterBFuture(f.format)
  def fighterBFuture(p: Participant): F = fighterBFuture(SpecificFighter(Some(p)))
  object timeStart extends MappedLong(this)
  object timeStop extends MappedLong(this)
  object netDuration extends MappedLong(this)

  def phaseType: PhaseType
  def phase: MappedLongForeignKey[_, _ <: Phase[_]]
  def scheduled: MappedLongForeignKey[_, _ <: ScheduledFight[_]]

  def started_? = timeStart.is > 0 || inProgress.is
  def finished_? = timeStop.is > 0
  def grossDuration = timeStop.is - timeStart.is

  def addScore = {
    val score = scoreMeta.create
    scores += score
    score
  }

  def mapScores[A](map: Score[_, _] => A): Seq[A] = scores.map(map)

  def currentScore = scores.foldLeft(TotalScore(0, 0, 0, 0, 0, 0)) { (sum, score) =>
    TotalScore(
      sum.red + score.pointsRed.get,
      sum.redAfter + score.afterblowsRed.get,
      sum.blue + score.pointsBlue.get,
      sum.blueAfter + score.afterblowsBlue.get,
      sum.double + score.doubles.get,
      sum.exchangeCount + score.exchanges.get)
  }

  def inFight_?(p: Participant) = (for {
    a <- fighterA.participant
    b <- fighterB.participant
  } yield a.id.is == p.id.is || b.id.is == p.id.is) getOrElse false

  def fighterA: Fighter = Fighter.parse(fighterAFuture.get)
  def fighterB: Fighter = Fighter.parse(fighterBFuture.get)

  def winner = currentScore match {
    case TotalScore(a, _, b, _, _, _) if a > b => Full(fighterA.participant.get)
    case TotalScore(a, _, b, _, _, _) if a < b => Full(fighterB.participant.get)
    case _ => Empty
  }

  def loser = currentScore match {
    case TotalScore(a, _, b, _, _, _) if a < b => Full(fighterA.participant.get)
    case TotalScore(a, _, b, _, _, _) if a > b => Full(fighterB.participant.get)
    case _ => Empty
  }

  def shortLabel = fighterA.toString + " vs " + fighterB.toString

  def toMarshalled = MarshalledFight(
    phase.foreign.get.tournament.foreign.get.toMarshalledSummary,
    phaseType.code,
    id.is,
    name.is,
    fighterA.toMarshalled,
    fighterB.toMarshalled,
    timeStart.is,
    timeStop.is,
    netDuration.is,
    scores.map(_.toMarshalled).toList,
    phase.foreign.get.rulesetImpl.fightProperties.timeLimit,
    phase.foreign.get.rulesetImpl.fightProperties.exchangeLimit,
    phase.foreign.get.rulesetImpl.possiblePoints)
  def toMarshalledSummary = MarshalledFightSummary(
    phase.foreign.get.tournament.foreign.get.toMarshalledSummary,
    phaseType.code,
    id.is,
    name.is,
    fighterA.toMarshalled,
    fighterB.toMarshalled,
    timeStart.is,
    timeStop.is,
    netDuration.is,
    scores.map(_.toMarshalled).toList,
    phase.foreign.get.rulesetImpl.fightProperties.timeLimit,
    phase.foreign.get.rulesetImpl.fightProperties.exchangeLimit,
    phase.foreign.get.rulesetImpl.possiblePoints)
  def fromMarshalled(m: MarshalledFight) = {
    timeStart(m.timeStart)
    timeStop(m.timeStop)
    netDuration(m.netDuration)
    scores.clear
    m.scores.foreach(s => scores += scoreMeta.create.fromMarshalled(s))
    this
  }
  def fromMarshalledSummary(m: MarshalledFightSummary) = {
    if (timeStart.get == 0) {
      timeStart(m.timeStart)
    }
    timeStop(m.timeStop)
    netDuration(m.netDuration)
    m.scores.drop(scores.size).foreach(s => scores += scoreMeta.create.fromMarshalled(s))
    this
  }

  def schedule(time: Long, duration: Long): ScheduledFight[_]

  def sameFighters(other: Fight[_, _]) = {
    val myFighters = fighterA :: fighterB :: Nil
    val theirFighters = other.fighterA :: other.fighterB :: Nil
    myFighters.filterNot(my => theirFighters.exists(_.sameAs(my))).isEmpty
  }
}

object FightHelper {
  def dao(phaseType: PhaseType): LongKeyedMetaMapper[_ <: Fight[_, _]] = phaseType match {
    case PoolType => PoolFight
    case EliminationType => EliminationFight
    case _ => PoolFight
  }

  def dao(phaseType: String): LongKeyedMetaMapper[_ <: Fight[_, _]] = phaseType match {
    case PoolType.code => PoolFight
    case EliminationType.code => EliminationFight
    case _ => PoolFight
  }
}

case class MarshalledViewerPoolFightSummary(order: Long, fighterA: MarshalledParticipant, fighterB: MarshalledParticipant, started: Boolean, finished: Boolean, score: TotalScore)

class PoolFight extends Fight[PoolFight, PoolFightScore] {
  def getSingleton = PoolFight

  def scoreMeta = PoolFightScore

  object pool extends MappedLongForeignKey(this, Pool)
  object order extends MappedLong(this)
  object scheduled extends MappedLongForeignKey(this, ScheduledPoolFight)

  def phase = pool.foreign.get.phase
  val phaseType = PoolType

  def toViewerSummary = MarshalledViewerPoolFightSummary(
    order.is,
    fighterA.participant.get.toMarshalled,
    fighterB.participant.get.toMarshalled,
    started_?,
    finished_?,
    currentScore)

  def schedule(time: Long, duration: Long) = {
    val sf = ScheduledPoolFight.create.fight(this).time(time).duration(duration)
    scheduled(sf)
    sf
  }
}
object PoolFight extends PoolFight with LongKeyedMetaMapper[PoolFight]

class EliminationFight extends Fight[EliminationFight, EliminationFightScore] {
  def getSingleton = EliminationFight

  def scoreMeta = EliminationFightScore

  object phase extends MappedLongForeignKey(this, EliminationPhase)
  val phaseType = EliminationType

  object round extends MappedLong(this)
  object scheduled extends MappedLongForeignKey(this, ScheduledEliminationFight)

  def schedule(time: Long, duration: Long) = {
    val sf = ScheduledEliminationFight.create.fight(this).time(time).duration(duration)
    scheduled(sf)
    sf
  }
}
object EliminationFight extends EliminationFight with LongKeyedMetaMapper[EliminationFight]