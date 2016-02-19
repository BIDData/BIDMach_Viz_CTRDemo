package simulation

import akka.actor.Actor
import BIDMach.datasources.FileSource
import BIDMach.datasinks.MatSink
import BIDMat.{Dict, IDict, FMat, IMat, SMat, SBMat}
import BIDMat.MatFunctions._
import scala.collection.breakOut
import scala.collection.immutable.Map
import scala.collection.immutable.ListMap

import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization


import scala.collection.mutable

/**
  * The main class for running an ad bidding simulation.
  *
  * @param dataPath The path to read record data.
  * @param advertiserMap a BIDMat Dict between advertiser string and its integer id.
  * @param keyPhraseMap a BIDMat Dict between key phrase string and its integer id.
  */


class AdBiddingSimulation(adModel: CTRModel, userModel: CTRModel,
                          var alpha: Float, var beta: Float, actor: Actor,
                          dataPath: String,
                          advertiserMap: Dict, keyPhraseMap: Dict, keyWordMap: Dict,
                          dataSink: MatSink) {


  val source: FileSource = _



  /**
    * start the simulation cycle.
    */
  def run() = {
    source = new FileSource(dataPath)
    while (source.hasNext()) {
      val records = source.next()
      val metrics = simulate(records, keyWordMap, keyPhraseMap)
      dumpMetrics(metrics)
    }
  }


  /**
    * simulate ad auctions on a batch of records.
    *
    * @param records an FMat with bidding records, potentially from multiple auctions.
    * @param advertiserMap a BIDMat Dict between advertiser strings and their integer id in data.
    * @param keyPhraseMap a BIDMat Dict between key phrase strings and their integer id in data.
    *
    * @return a list of metric results BIDMach FMat from each auction
    */

  def simulate(records: FMat, advertiserMap: Dict, keyPhraseMap: Dict) : String = {
    val uniqueKeyPhraseHash = unique(records(kp_hash)) //TODO: find which column/row is kp_hash
    val i = 0
    var results = mutable.ListBuffer[FMat]()
    while (i < uniqueKeyPhraseHash.nrows) {
      val keyPhraseHash = uniqueKeyPhraseHash(i, ?)
      val auction = records(?, records(kp_hash) == keyPhraseHash) //get all records for this auction
      results += simulateAuction(auction, advertiserMap, keyPhraseMap)
    }
    results
  }


  /**
    * simulate an auction.
    *
    * @param auction an FMat containing the bidding records.
    * @param advertiserMap a BIDMat Dict between advertiser strings and their integer id in data.
    * @param keyPhraseMap a BIDMat Dict between key phrase strings and their integer id in data.

    * @return a BIDMach FMat containing (keyPhrase ID, total profit).
    */

  def simulateAuction(auction: FMat, advertiserMap: Dict, keyPhraseMap: Dict) : FMat = {
    val keyPhraseID = auction(0, kp_col).toInt //TODO: find which column is the keyPhraseID
    val keyPhrase = keyPhraseMap(keyPhraseID)

    val bidList = auction(?, bid_col).data.toList
    val advertiserList = auction(?, advertiser_col).data.toList.foreach((id: Float) => advertiserMap(id.toInt))
    val bids: Map[String, Float] = (advertiserList.zip(bidList))(breakOut)

    val qualityScores = bids map {
      case (advertiser: String, bid: Float) => {
        (advertiser, getQualityScore(advertiser, keyPhrase, bid))
      }
    }
    val ranks = getRanking(qualityScores)
    val finalQuality = getFinalQualityScores(keyPhrase, ranks, bids)
    val profits = ranks map {
      case (advertiser: String, rank: Int) => {
        (advertiser, calculateProfit(finalQuality, keyPhrase, advertiser, rank))
      }
    }

    //TODO: could add more metrics, such as volume of ads, ads per advertiser, etc.
    row(keyPhraseID, profits.foldLeft(0)(_+_._2))

  }



  def getQualityScore(advertiser: String, keyPhrase: String, bid: Float): Float = {
    val myCTR:Float = adModel.getCTR(1, advertiser, keyPhrase)
    qualityFunc(myCTR, bid)

  }

  def getRanking(qualityScores: Map[String, Float]): Map[String, Int] = {
    val sorted_scores = ListMap(qualityScores.toSeq.sortWith(
      (scorePair1: (String, Float), scorePair2: (String, Float)) => scorePair1._2 > scorePair2._2):_*)
    val sorted_map = mutable.HashMap.empty[String, Int]
    var i = 1
    for ((advertiser, quality) <- sorted_scores) {
      sorted_map += (advertiser -> i)
      i = i + 1
    }
    sorted_map.toMap
  }

  def getFinalQualityScores(keyPhrase: String, ranks: Map[String, Int],
                            bids: Map[String, Float]): Map[Int, Float] = {
    ranks map {
      case (advertiser: String, rank: Int) => {
        val finalCTR = userModel.getCTR(rank, advertiser, keyPhrase)
        (rank, qualityFunc(finalCTR, bids(advertiser)))
      }
    }
  }

  def calculateProfit(finalScores: Map[Int, Float], keyPhrase: String, advertiser: String, rank: Int) : Float = {
    //TODO:adding reserve pricing
    if (rank >= finalScores.keysIterator.max) {
      return 1 //for the last advertiser, just set the price it pays to 1
    }

    // Grab the final score of the next ranking
    val nextScore = finalScores.get(rank + 1)

    // Now, calculate what we would have had to bid to maintain this position
    val finalCTR = userModel.getCTR(rank, advertiser, keyPhrase)
    invQualityFunc(nextScore, finalCTR)
  }

  def dumpMetrics(metrics: mutable.ListBuffer[FMat]) = {
    metrics.foreach(dataSink.put)
  }


  def updateParams(newAlpha: Float, newBeta: Float) = {
    alpha = newAlpha
    beta = newBeta
  }


  def qualityFunc(CTR: Float, bid: Float): Float = {
    (math.pow(CTR, alpha) * math.pow(bid, beta)).toFloat
  }

  def invQualityFunc(quality: Float, CTR: Float): Float  = {
    math.pow(quality / math.pow(CTR, alpha), 1 / beta).toFloat
  }


}
