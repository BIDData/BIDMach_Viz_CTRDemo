import scala.collection.mutable
import scala.collections.mutable.Array
import scala.collections.HashMap


class AdBiddingSimulation(adModel: CTRModel, userModel: CTRModel, quality_func: (Float, Float) => Float) {

  def simulate(keyPhrases: Array[String], bids: mutable.HashMap[String => Float]) {

  }

  def getQualityScore( ) {

  }

  def getRanking() {
     val Ad_Example = Map("Ad1" -> Random.nextInt, "Ad2"->Random.nextInt, "Ad3"->Random.nextInt,"Ad4" -> Random.nextInt, "Ad5"->Random.nextInt, "Ad6"->Random.nextInt,"Ad7" -> Random.nextInt, "Ad8"->Random.nextInt, "Ad9"->Random.nextInt)
  val m1 = ListMap(Ad_Example.toSeq.sortBy(_._2):_*) 
  for ((k,v) <- m1) 
    printf("key: %s, value: %s\n",k,v)
   
    val map1 = collection.mutable.Map("Kim" -> 90)
    var i = 1
    for ((advertiser, quality) <- m1) {
    map1 += advertiser -> i; i = i +1;

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

