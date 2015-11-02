# imports here

class AdBiddingSimulation {

  def init(adModel: Model, userModel: Model, quality_func: {(float, float) => float}) {

  }

  def simulate(keyPhrases: List[String], bids: Map[String => Float]) {

  }

  def getQualityScore( ) {

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

