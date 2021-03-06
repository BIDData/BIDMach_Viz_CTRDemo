package controllers

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
                          val numSlots: Int,
                          dataPath: String,
                          advertiserMap: Dict, keyPhraseMap: Dict, keyWordMap: Dict) {


  var source: FileSource = null
  var batchCount = 0

  // log metrics
  var curr_bid = 0.0
  var singleBid = 0
  var numAuctionInBatch = 0
  var numBids = 0
  var numOriginalBids = 0

  /**
    * start the simulation cycle.
    */
  def run(): mutable.MutableList[FMat] = {
    source = FileSource.apply(dataPath + "%d.fmat")
    source.opts.nend = 1 //need to change
    source.opts.dorows = true
    source.opts.batchSize = 10000
    source.init
    var metricList = mutable.MutableList[FMat]()

    while (source.hasNext) {
      batchCount += 1
      val recordsGroup = source.next
      recordsGroup.foreach((records: Mat) => {
        metricList ++= simulate(FMat(records), advertiserMap, keyPhraseMap)
      })
    }
    metricList
  }

  /**
    * run the simulation cycle for a batch.
    *
    * @return
    */
  def runBatch(): mutable.MutableList[FMat] = {
    if (source == null || (!source.hasNext)) {
      source = FileSource.apply(dataPath + "%d.fmat")
      source.opts.nend = 5 //need to change
      source.opts.dorows = true
      source.opts.batchSize = 50000
      source.init
    }
    val records = source.next(0)
    singleBid = 0
    numAuctionInBatch = 0
    numBids = 0
    numOriginalBids = 0
    val result = simulate(FMat(records), advertiserMap, keyPhraseMap)
    println("===========================")
    println("numBids: " + numBids)
    println("numOriginalBids: " + numOriginalBids)
    println("numResults: " + result.length + ", auction with 1 bid: " + singleBid.toString + ", total number of auction: " + numAuctionInBatch.toString)
    result

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
    * simulate auctions in a day-query group.
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
    numOriginalBids += bidList.size
    val advertiserList = group(?, 1).data.toList.map((id: Float) => advertiserMap(id.toInt))
    val bids: List[(String, Float)] = advertiserList.zip(bidList)
    numBids += bids.size

    var rowId = 0 // for identifying each row, in case there are duplicates of advertisers
    val qualityScores = bids map {
      case (advertiser: String, bid: Float) => {
        rowId += 1
        (advertiser, getQualityScore(advertiser, keyPhrase, bid), rowId - 1)
      }
    }

    val rankCounts = group(?, 4).data.groupBy(x => x).map(x => (x._1, x._2.length))
    val numAuction = rankCounts.valuesIterator.max
    val rankList = getRankings(qualityScores, numAuction)


    /* Metric Calculation */

    val profitMatrices = new mutable.ListBuffer[FMat]

    rankList.foreach((ranks: Map[Int, (String, Int)]) => {
      numAuctionInBatch += 1
      if (ranks.size == 1) {
        singleBid += 1
      }
      val auctionId = AdBiddingSimulation.generateAuctionId()
      val finalQuality = getFinalQualityScores(keyPhrase, ranks, bids)
      val rankToPrice = mutable.Map[Int, Float]()

      ranks.foreach {
        case (rank: Int, (advertiser: String, rowId: Int)) => {
          val profitMatrix:FMat = FMat(zeros(1, AdBiddingSimulation.NUM_METRIC_FIELDS))
          profitMatrix(0, AdBiddingSimulation.INDEX_BATCH_ID) = batchCount
          profitMatrix(0, AdBiddingSimulation.INDEX_AUCTION_ID) = auctionId.toFloat
          profitMatrix(0, AdBiddingSimulation.INDEX_ADVERTISER_ID) = advertiserMap(advertiser)
          //metric 1: profit per auction per advertiser
          if (rank <= numSlots) {
            val viewTuple = simulateView(finalQuality, keyPhrase, advertiser, rank)
            val price = viewTuple._1
            val numClick = viewTuple._2
            var nextPrice = 0.0f
            // Hack: Calculate the price for next advertiser, in order to get the price gap
            rankToPrice.put(rank, price)
            if (rank == ranks.size) {
              nextPrice = price
            } else {
              val viewTupleForNextOne = simulateView(finalQuality, keyPhrase, ranks(rank + 1)._1, rank + 1)
              nextPrice = viewTupleForNextOne._1
            }
            profitMatrix(0, AdBiddingSimulation.INDEX_PROFIT) = price * numClick
            //metric 2: number of clicks estimated for advertiser in this auction
            profitMatrix(0, AdBiddingSimulation.INDEX_CLICK) = numClick
            profitMatrix(0, AdBiddingSimulation.INDEX_PRICE) = price
            profitMatrix(0, AdBiddingSimulation.PRICE_GAP) = price - nextPrice
          } else {
            profitMatrix(0, AdBiddingSimulation.INDEX_PROFIT) = 0
            profitMatrix(0, AdBiddingSimulation.INDEX_CLICK) = 0
            profitMatrix(0, AdBiddingSimulation.INDEX_PRICE) = 0
            profitMatrix(0, AdBiddingSimulation.PRICE_GAP) = 0
          }
          profitMatrices += profitMatrix
        }
      }
      // log prints
      val mean = rankToPrice.valuesIterator.sum / rankToPrice.size
      val priceList = rankToPrice.toList.sortBy(_._2).map(x => x._2)
      //println(mean.toString, quantile(priceList, 25).toString, quantile(priceList, 75).toString)
      //println(rankToPrice)
    })
    profitMatrices.toList
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
  def getRankings(qualityScores: List[(String, Float, Int)], numAuctions: Int): List[Map[Int, (String, Int)]] = {
    val rankList: List[mutable.MutableList[(String, Int)]] = (0 until numAuctions).map(
      (i: Int) => {
        new mutable.MutableList[(String, Int)]()
      }).toList

    val sortedAdvertisers = qualityScores.sortBy(- _._2)
    var rankListInd = 0
    sortedAdvertisers.foreach(p => {
      rankList(rankListInd) += Tuple2(p._1, p._3) //p._3 is the rowID
      rankListInd = (rankListInd + 1) % rankList.size
    })

    rankList.map((ranks: mutable.MutableList[(String, Int)]) => {
      (0 until ranks.size).map((i: Int) => (i + 1, ranks(i))).toMap
    })
  }



  def getFinalQualityScores(keyPhrase: String, ranks: Map[Int, (String, Int)],
                            bids: List[(String, Float)]): Map[Int, Float] = {
    ranks map {
      case (rank: Int, (advertiser: String, rowId: Int)) => {
        val finalCTR = userModel.getCTR(rank, advertiser, keyPhrase)
        (rank, qualityFunc(finalCTR, bids(rowId)._2))
      }
    }
  }

  def simulateView(finalScores: Map[Int, Float], keyPhrase: String, advertiser: String, rank: Int) : Tuple2[Float, Float] = {
    // Now, calculate what we would have had to bid to maintain this position
    val finalCTR = userModel.getCTR(rank, advertiser, keyPhrase)

    // calculate the biding price for rank
    val curr_bid = invQualityFunc(finalScores(rank), finalCTR)

    if (reservePrice > curr_bid) {
      return Tuple2(0.0f, 0.0f)
    }

    // Grab the final score of the next ranking
    val nextScore = if (rank + 1 < finalScores.size) finalScores(rank + 1) else 0

    var price = invQualityFunc(nextScore, finalCTR)

    /**  IF the biding price from next bid is smaller than the reservePrice then pay bidding price
      *  O.W pay for the reservePrice
      */
    if (reservePrice >= price) {
      price = reservePrice
    }

    Tuple2(price, AdBiddingSimulation.IMPRESSION * userModel.getCTR(rank, advertiser, keyPhrase))
  }



  def updateParams(newAlpha: Float, newBeta: Float) = {
    alpha = newAlpha
    beta = newBeta
  }
  
  def getAlpha() : Float = {
    return alpha
  }
  
  def updateAlpha(newAlpha: Float) = {
    alpha = newAlpha
  }
  
  def getBeta() : Float = {
    return beta
  }
  
  def updateBeta(newBeta: Float) = {
    beta = newBeta
  }
  
  def getReserve() : Float = {
    return reservePrice
  }
  
  def updateReserve(newReserve: Float) = {
    reservePrice = newReserve
  }

  def getField(fieldName: String): Any = {
    this.getClass.getMethods.find(_.getName == fieldName).get.invoke(this)
  }

  def setField(fieldName: String, value: Any): Unit = {
    this.getClass.getMethods.find(_.getName == (fieldName + "_$eq")).get.invoke(this, value.asInstanceOf[AnyRef])
  }


  def qualityFunc(CTR: Float, bid: Float): Float = {
    (math.pow(CTR, alpha) * math.pow(bid, beta)).toFloat
  }

  def invQualityFunc(quality: Float, CTR: Float): Float  = {
    math.pow(quality / math.pow(CTR, alpha), 1 / beta).toFloat
  }

  def quantile(lst: List[Float], q:Int) : Float = {
    val n = Math.round(lst.length * q / 100).toInt
    lst(n)
  }


}

object AdBiddingSimulation {

  private var auctionIdgen: Long = 0L
  val IMPRESSION = 1000
  val NUM_METRIC_FIELDS = 7
  val INDEX_BATCH_ID = 0
  val INDEX_AUCTION_ID = 1
  val INDEX_ADVERTISER_ID = 2
  val INDEX_PROFIT = 3
  val INDEX_CLICK = 4
  val INDEX_PRICE = 5
  val PRICE_GAP = 6



  def generateAuctionId(): Long = {
    auctionIdgen += 1
    auctionIdgen - 1
  }
}
