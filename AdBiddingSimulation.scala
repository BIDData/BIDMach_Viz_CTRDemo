Package Simulation

import scala.collection.Map
import scala.collection.immutable.ListMap


class AdBiddingSimulation(adModel: CTRModel, userModel: CTRModel, alpha: Float, beta: Float) {

  def qualityFunc(CTR: Float, bid: Float) = {(math.pow(CTR, alpha) * math.pow(bid, beta)).toFloat}

  def invQualityFunc(quality: Float, CTR: Float) = {math.pow(quality / math.pow(CTR, alpha), 1 / beta).toFloat}

  //TODO: since not all advertiser bid for all key phrases, bids should be keyPhrases specific
  def simulate(keyPhrases: Array[String], bids: Map[String, Float]) : Map[String, Float] = {
    val totalProfits = keyPhrases map {
      case (keyPhrase: String) => {
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
        //TODO: what to do after getting profits for each key phrase?
        (keyPhrase, aggregateProfits(profits))
      }
    }
    totalProfits.toMap
  }

  def getQualityScore(advertiser: String, keyPhrase: String, bid: Float): Float = {
    val myCTR:Float = adModel.getCTR(1, advertiser, keyPhrase);
    qualityFunc(myCTR, bid);

  }

  def getRanking(qualityScores: Map[String, Float]): Map[String, Int] = {
    val sorted_scores = ListMap(qualityScores.toSeq.sortWith(_._2 > _._2):_*)
    val sorted_map = collection.mutable.Map[String, Int]()
    var i = 1
    for ((advertiser, quality) <- sorted_scores) {
      sorted_map += advertiser -> i
      i = i + 1
    }
    sorted_map
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
    //TODO: adding reserve price
    if (rank >= finalScores.keys.max - 1) {
        return 0
    }
    // Grab the final score of the next ranking
    val nextScore = finalScores(rank + 1)

    // Now, calculate what we would have had to bid to maintain this position
    val finalCTR = userModel.getCTR(rank, advertiser, keyPhrase)
    invQualityFunc(nextScore, finalCTR)
  }

  def aggregateProfits(profits: Map[String, Float]): Float = {
    profits.values.sum
  }
}

