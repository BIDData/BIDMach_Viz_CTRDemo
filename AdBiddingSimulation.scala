import scala.collection.mutable
import scala.collections.mutable.Array
import scala.collections.HashMap


class AdBiddingSimulation(adModel: CTRModel, userModel: CTRModel, quality_func: (Float, Float) => Float) {

  def simulate(keyPhrases: Array[String], bids: mutable.HashMap[String => Float]) {

  }

  def getQualityScore(advertiser: String, keyword: String, bid: Float): Float = {
    val myCTR :Float = adModel.getCTR(advertiser, keyword);
    val quality :Float = quality_func(myCTR, bid);
  }

  def getRanking() {

  }

  def getCTR(advertiser: String, keyword: String, rank: Int): Float = {
    
    // TODO: Instead of random, read from ad*kw row vector
    // (assuming we have the mapping from ad*kw -> index)
    val ad_kw_CTR = scala.util.Random.nextFloat

    // TODO: Instead of random, read from rank column vector
    // (just use rank as the index)
    val rank_CTR = scala.util.Random.nextFloat

    // Estimated CTR of this (ad, keyword) at this rank
    var CTR = ad_kw_CTR * rank_CTR

    return CTR
  }

  def calculateProfit() {

  }

  def aggregateMetrics() {

  }
}

