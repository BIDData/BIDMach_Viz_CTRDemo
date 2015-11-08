import scala.collection.mutable.HashMap


class AdBiddingSimulation(adModel: CTRModel, userModel: CTRModel, quality_func: (Float, Float) => Float) {

  def simulate(keyPhrases: Array[String], bids: HashMap[String => Float]) {
    val totalProfits = keyPhrases.map((keyPhrase: String) =>  {
      val qualityScores = bids.map((advertiser: String) => {getQualityScore(advertiser, keyPhrase, bids(advertiser))})
      val ranks = getRanking(qualityScores)
      val finalQuality = getFinalQualityScore(keyPhrase, ranks, bids)
      val profits = ranks.map((rank: Int) => (calculateProfit(finalQuality, rank)))
      //TODO: what to do after getting profits for each key phrase?
      profits.foldleft(0.0)(_+_)
    }
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

