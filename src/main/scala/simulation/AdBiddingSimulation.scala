package simulation

import BIDMach.datasources.FileSource
import BIDMat._
import BIDMat.MatFunctions._
import scala.collection.breakOut
import scala.collection.mutable
import scala.collection.immutable.Map
import scala.collection.immutable.ListMap


/**
  * The main class for running an ad bidding simulation.
  *
  * @param dataPath The path to read record data.
  * @param advertiserMap a BIDMat Dict between advertiser string and its integer id.
  * @param keyPhraseMap a BIDMat Dict between key phrase string and its integer id.
  */


class AdBiddingSimulation(adModel: CTRModel, userModel: CTRModel,
                          var alpha: Float, var beta: Float, var reservePrice: Float,
                          dataPath: String,
                          advertiserMap: Dict, keyPhraseMap: Dict, keyWordMap: Dict) {


  var source: FileSource = null


  /**
    * start the simulation cycle.
    */
  def run(): mutable.MutableList[FMat] = {
    source = FileSource.apply(dataPath + "%d.fmat")
    source.opts.nend = 1 //need to change
    source.opts.dorows=true
    source.init
    var metricList = mutable.MutableList[FMat]()

    var i = 0
    while (source.hasNext) {
      val recordsGroup = source.next
      recordsGroup.foreach((records: Mat) => {
        metricList ++= simulate(FMat(records), advertiserMap, keyPhraseMap)
      })
      println("batch %d finished", i)
      i += 1
    }

    metricList
  }


  /**
    * simulate ad auctions on a batch of records.
    *
    * @param records an FMat with bidding records, potentially from multiple auctions.
    * @param advertiserMap a BIDMat Dict between advertiser strings and their integer id in data.
    * @param keyPhraseMap a BIDMat Dict between key phrase strings and their integer id in data.
    * @return a list of metric results BIDMach FMat from each auction
    */

  def simulate(records: FMat, advertiserMap: Dict, keyPhraseMap: Dict) : mutable.MutableList[FMat] = {
    val uniqueKeyPhraseHash = unique(records(?, 2))
    var i = 0
    var results = mutable.MutableList[FMat]()
    while (i < uniqueKeyPhraseHash.nrows) {
      val keyPhraseHash = uniqueKeyPhraseHash(i, ?)(0, 0)
      val recordIndices = find(records(?, 2) == keyPhraseHash)
      val auction = records(recordIndices, ?) //get all records for this auction
      results += simulateAuction(auction, advertiserMap, keyPhraseMap)
      i += 1
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
    val keyPhraseID = auction(0, 2).toInt //TODO: find which column is the keyPhraseID
    val keyPhrase = keyPhraseMap(keyPhraseID)

    val bidList = auction(?, 3).data.toList
    val advertiserList = auction(?, 1).data.toList.map((id: Float) => advertiserMap(id.toInt))
    val bids: Map[String, Float] = advertiserList.zip(bidList)(breakOut)

    val qualityScores = bids map {
      case (advertiser: String, bid: Float) => {
        (advertiser, getQualityScore(advertiser, keyPhrase, bid))
      }
    }
    val ranks = getRanking(qualityScores)
    val finalQuality = getFinalQualityScores(keyPhrase, ranks, bids)
    val profits: Map[String, Float] = ranks map {
      case (advertiser: String, rank: Int) => {
        (advertiser, calculateProfit(finalQuality, keyPhrase, advertiser, rank))
      }
    }

    //TODO: could add more metrics, such as volume of ads, ads per advertiser, etc.
    val totalProfit = profits.values.foldLeft(0.0)(_+_)
    row(keyPhraseID, totalProfit)
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
    // Now, calculate what we would have had to bid to maintain this position
    val finalCTR = userModel.getCTR(rank, advertiser, keyPhrase)

    // calculate the biding price for rank
    val curr_bid = invQualityFunc(finalScores(rank), finalCTR)

    if (reservePrice > curr_bid) {
      return 0
    }
    // Grab the final score of the next ranking
    val nextScore = if (rank + 1 < finalScores.size) finalScores(rank + 1) else 0

    val profit = invQualityFunc(nextScore, finalCTR)

    /** IF the biding price from next bid is smaller than the reservePrice then pay bidding price
      *  O.W pay for the reservePrice
      */
    if (reservePrice < profit) {
      return profit
    } else {
      return reservePrice
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


}
