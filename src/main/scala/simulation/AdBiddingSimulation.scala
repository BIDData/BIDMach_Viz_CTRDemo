package simulation

import BIDMach.datasources.FileSource
import BIDMat._
import BIDMat.MatFunctions._
import scala.collection.{mutable, breakOut}
import scala.collection.immutable.Map
import scala.collection.immutable.ListMap
import scala.collection.mutable.ListBuffer


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
      val group = records(recordIndices, ?) //get all records for this auction
      results ++= simulateAuction(group, advertiserMap, keyPhraseMap)
      i += 1
    }
    results
  }


  /**
    * simulate auctions in a group.
    *
    * @param group an FMat containing the bidding records.
    * @param advertiserMap a BIDMat Dict between advertiser strings and their integer id in data.
    * @param keyPhraseMap a BIDMat Dict between key phrase strings and their integer id in data.

    * @return a BIDMach FMat containing (keyPhrase ID, total profit).
    */

  def simulateAuction(group: FMat, advertiserMap: Dict, keyPhraseMap: Dict) : List[FMat] = {
    val keyPhraseID = group(0, 2).toInt //TODO: find which column is the keyPhraseID
    val keyPhrase = keyPhraseMap(keyPhraseID)

    val bidList = group(?, 3).data.toList
    val advertiserList = group(?, 1).data.toList.map((id: Float) => advertiserMap(id.toInt))
    val bids: Map[String, Float] = advertiserList.zip(bidList)(breakOut)

    val qualityScores = bids map {
      case (advertiser: String, bid: Float) => {
        (advertiser, getQualityScore(advertiser, keyPhrase, bid))
      }
    }

    val numAuction = unique(group(0, 4)).nnz
    val rankList = getRankings(qualityScores, numAuction)
    val profitMatrices = rankList.map((ranks: Map[String, Int]) => {
      val auctionId = AdBiddingSimulation.generateAuctionId()
      val finalQuality = getFinalQualityScores(keyPhrase, ranks, bids)
      val profits: Map[String, Float] = ranks map {
        case (advertiser: String, rank: Int) => {
          (advertiser, calculateProfit(finalQuality, keyPhrase, advertiser, rank))
        }
      }

      //TODO: could add more metrics, such as volume of ads, ads per advertiser, etc.
      val profitMatrix:FMat = FMat(zeros(profits.size, 3))
      profitMatrix(?, 0) = auctionId.toFloat
      val profitsList = profits.toList
      (0 until profits.size).foreach((i: Int) => {
        profitMatrix(i, 1) = advertiserMap(profitsList(i)._1)
        profitMatrix(i, 2) = profitsList(i)._2
      })

      profitMatrix
    })
    profitMatrices

  }



  def getQualityScore(advertiser: String, keyPhrase: String, bid: Float): Float = {
    val myCTR:Float = adModel.getCTR(1, advertiser, keyPhrase)
    qualityFunc(myCTR, bid)

  }

  /*
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
  */

  /**
    * calculate ranking for all auctions in this group of (day, keyPhrase).
    *
    * @param qualityScores the quality score for each advertiser.
    * @param numAuctions number of auctions in this group.
    * @return a list of (advertiser -> rank)
    */
  def getRankings(qualityScores: Map[String, Float], numAuctions: Int): List[Map[String, Int]] = {
    val rankList: List[mutable.MutableList[String]] = (0 until numAuctions).map(
      (i: Int) => {
        new mutable.MutableList[String]()
      }).toList

    val sortedAdvertisers = qualityScores.toList.sortBy(- _._2)
    var rankListInd = 0
    sortedAdvertisers.foreach(p => {
      rankList(rankListInd) += p._1
      rankListInd = (rankListInd + 1) % rankList.size
    })

    rankList.map((ranks: mutable.MutableList[String]) => {
      (0 until ranks.size).map((i: Int) => (ranks(i), i + 1)).toMap
    })
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

    var price = invQualityFunc(nextScore, finalCTR)

    /** IF the biding price from next bid is smaller than the reservePrice then pay bidding price
      *  O.W pay for the reservePrice
      */
    if (reservePrice >= price) {
      price = reservePrice
    }

    price * 1000 * adModel.getRankingCTR(rank)
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



object AdBiddingSimulation {

  private var auctionIdgen: Long = 0L;

  def generateAuctionId(): Long = {
    auctionIdgen += 1
    auctionIdgen - 1
  }
}
