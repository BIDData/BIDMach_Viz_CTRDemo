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

  def getQualityScore(advertiser: String, keyword: String, bid: Float): Float = {
    val myCTR :Float = adModel.getCTR(1, advertiser, keyword);
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

  def calculateProfits(finalScores: Array[Double], ranking: Int):Double = {
    
    // If there's no next ranking, just return 0?
    if (ranking >= finalScores.length - 1) {
        return 0;   
    }
    
    // Grab the final score of the next ranking
    val a_score = finalScores(ranking)
    val next_score = finalScores(ranking+1)
    
    // Now, calculte what we would have had to bid to maintain this position
    // Q = CTR^a * bid^b
    // So, the required bid is (Q/CTR^a) ^ (1/b)
    //cost_a = inv_quality_func(next_score, ctr(rank, A))
    val a = 2.0
    val b = 1.0
    val ctr = 5.0
    
    val term = next_score / scala.math.pow(ctr, a)
    val cost_a = scala.math.pow(term, 1/b)
    return cost_a;
  }

  def aggregateMetrics() {

  }
}

