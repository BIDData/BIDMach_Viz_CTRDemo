package simulation

import akka.actor.Actor
import BIDMat.{Dict, IDict, FMat, IMat, SMat, SBMat}
import BIDMat.MatFunctions._
import scala.collection.immutable.Map
import scala.collection.immutable.ListMap

import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.Serialization


import scala.collection.mutable



class AdBiddingSimulation(adModel: CTRModel, userModel: CTRModel,
                          var alpha: Float, var beta: Float, actor: Actor) {

  /**
    * start the simulation cycle.
    *
    * @param dataPath The path to read record data.
    * @param advertiserMap a BIDMat Dict between advertiser string and its integer id.
    * @param keyPhraseMap a BIDMat Dict between key phrase string and its integer id.
    */
  def run(dataPath: String, advertiserMap: Dict, keyPhraseMap: Dict) = {
    //TODO: where/how to get data?

    //TODO: how to check hasData()? Might need to be implemented on Server Side
    while (hasData()) {
      val records = getBatchData() //Data should come from disk, in the form of BIDMach matrix
      //TODO: keyWordMap and keyPhraseMap should be BIDMat Dict
      val metrics = simulate(records, keyWordMap, keyPhraseMap)
      sendMetrics(metrics)
    }
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

  /**
    * simulate ad auctions on a batch of records.
    *
    * @param records an FMat with bidding records, potentially from multiple auctions.
    * @param advertiserMap a BIDMat Dict between advertiser strings and their integer id in data.
    * @param keyPhraseMap a BIDMat Dict between key phrase strings and their integer id in data.
    * @return a list of metric results from each auction
    */

  def simulate(records: FMat, advertiserMap: Dict, keyPhraseMap: Dict) : String = {
    val uniqueKeyPhraseHash = unique(records(kp_hash)) //TODO: find which column/row is kp_hash
    val i = 0
    var results = mutable.ListBuffer[Map]()
    while (i < uniqueKeyPhraseHash.nrows) {
      val keyPhraseHash = uniqueKeyPhraseHash(i, ?)
      val auction = records(?, records(kp_hash) == keyPhraseHash)
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
    * @return a Map containing the metric results.
    */

  def simulateAuction(auction: FMat, advertiserMap: Dict, keyPhraseMap: Dict) : Map = {
    val keyPhrase = keyPhraseMap(auction(kp_col)) //TODO: find which col is for keyPhrase

    val bids = Map(auction(?, advertiser_col).copyToFloatArray().toList zip
                   auction(?, bid_col).copyToFloatArray().toList)

    val qualityScores = bids map {
      case (advertiserHash: Float, bid: Float) => {
        val advertiser = advertiserMap(advertiserHash)
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
    Map("keyPhrase" -> keyPhrase, "profit" -> aggregateProfits(profits))
  }



  def getQualityScore(advertiser: String, keyPhrase: String, bid: Float): Float = {
    val myCTR:Float = adModel.getCTR(1, advertiser, keyPhrase)
    qualityFunc(myCTR, bid)

  }

  def getRanking(qualityScores: Map[String, Float]): Map[String, Int] = {
    val sorted_scores = ListMap(qualityScores.toSeq.sortWith(
      (scorePair1: (String, Float), scorePair2: (String, Float)) => scorePair1._2 > scorePair2._2):_*)
    val sorted_map = mutable.Map[String, Int]()
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
        (rank, qualityFunc(finalCTR, bids.get(advertiser)))
      }
    }
  }

  def calculateProfit(finalScores: Map[Int, Float], keyPhrase: String, advertiser: String, rank: Int) : Float = {
    //TODO: adding reserve price
    if (rank >= finalScores.keysIterator.max) {
      return 1 //for the last advertiser, just set the price it pays to 1
    }

    // Grab the final score of the next ranking
    val nextScore = finalScores.get(rank + 1)

    // Now, calculate what we would have had to bid to maintain this position
    val finalCTR = userModel.getCTR(rank, advertiser, keyPhrase)
    invQualityFunc(nextScore, finalCTR)

  }

  def aggregateProfits(profits: Map[String, Float]): Float = {
    profits.values.sum
  }

  //TODO: fix the logic of producing and sending metrics
  def sendMetrics(metrics: mutable.ListBuffer[Map]) = {
    val jsonStr = Serialization.write(metrics)
    actor ! jsonStr
  }

}
