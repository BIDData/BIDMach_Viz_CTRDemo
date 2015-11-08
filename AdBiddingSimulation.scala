import scala.collection.mutable
import scala.collections.mutable.Array
import scala.collections.HashMap
import scala.collection.immutable.ListMap
import util.Random


class AdBiddingSimulation(adModel: CTRModel, userModel: CTRModel, quality_func: (Float, Float) => Float) {

  def simulate(keyPhrases: Array[String], bids: mutable.HashMap[String => Float]) {

  }

  def getQualityScore( ) {

  }
//Simulate a rank_CTR matrix
  val Rnk_CTR = new Array[Float](10)
  Rnk_CTR(0) = 1   
  Rnk_CTR(1) = 0.9f
  Rnk_CTR(2) = 0.8f
  Rnk_CTR(3) = 0.7f 
  Rnk_CTR(4) = 0.6f 
  Rnk_CTR(5) = 0.5f    
  Rnk_CTR(6) = 0.4f 
  Rnk_CTR(7) = 0.3f    
  Rnk_CTR(8) = 0.2f   
  Rnk_CTR(9) = 0.1f  

  //Generate some random bids
val bid_map = Map("Ad1" -> abs(Random.nextFloat*10), "Ad2" -> abs(Random.nextFloat*10), "Ad3" ->abs(Random.nextFloat*10), "Ad4" ->abs(Random.nextFloat*10), "Ad5" ->abs(Random.nextFloat*10),"Ad6" ->abs(Random.nextFloat*10),"Ad7" ->abs(Random.nextFloat*10),"Ad8" ->abs(Random.nextFloat*10),"Ad9" ->abs(Random.nextFloat*10))

  //Generate a random Ad/Quality Map   
  val Ad_Example = Map("Ad1" -> abs(Random.nextInt(100)), "Ad2"->abs(Random.nextInt(100)), "Ad3"->abs(Random.nextInt(100)),"Ad4" -> abs(Random.nextInt(100)), "Ad5"->abs(Random.nextInt(100)), "Ad6"->abs(Random.nextInt(100)),"Ad7" -> abs(Random.nextInt(10)), "Ad8"->abs(Random.nextInt(100)), "Ad9"->abs(Random.nextInt(100)))
  
//Get the quality score Map
  def Get_Quality_Map(Keyword: String, Bid_map: collection.mutable.Map[String, Float]) = {
  val Ad_Real = collection.mutable.Map[String, Float]()
  for ((k,v) <- Bid_map){
    Ad_Real += k-> (getQualityScore(k,Keyword,v))
  }
  return Ad_Real
  }

  def getRanking(advertiser_map: Map) {


  }



def getRanking(Keyword: String, Ad_Real: collection.mutable.Map[String, Float] ) = {  
//Sort the map Advertiser Quality Score
  val m1 = ListMap(Ad_Example.toSeq.sortWith(_._2 > _._2):_*)
    val sorted_ad = collection.mutable.Map[String, Int]()
    var i = 1
    for ((advertiser, quality) <- m1) {
    sorted_ad += advertiser -> i; i = i +1;
  }
  //Get the map advertiser final quality
  val final_quality = collection.mutable.Map[String, Float]()
  for ((k,v) <- sorted_ad){
  final_quality += k-> (Ad_Real(k)*Rnk_CTR(v))
}
//Get the final map advertiser ranking
val m2 = ListMap(final_quality.toSeq.sortWith(_._2 > _._2):_*) 
    val sorted_ad2 = collection.mutable.Map[String, Int]()
    var i2 = 1
    for ((advertiser, quality) <- m2) {
    sorted_ad2 += advertiser -> i2; i2 = i2 +1; 
}
return sorted_ad2
} 

def getProfit(Rank_Map:collection.mutable.Map[String, Int])
  
 //Get the map rank -> final quality score   
 val rank_quality = collection.mutable.Map[Int, Float]()
 var i3 = 1
 for ((advertiser, quality) <- Rank_Map){
   rank_quality += i3 -> quality
   i3 = i3 + 1
 }
 //Get the map rank -> advertiser 
 val rank_ad = collection.mutable.Map[Int, String]()
 var i4 = 1
 for ((advertiser, quality) <- Rank_Map){
   rank_ad +=  i4 -> advertiser
   i4 = i4 + 1
 }
 //Return a profit map for a given number of slots: advertiser --> profit (the reserve is not handled here)
 //Not the correct quality function 
val slots = 4
val profit_map = collection.mutable.Map[String, Float]()
 for( a <- 1 to slots){
   profit_map +=  rank_ad(a) -> bid_map(rank_ad(a+1))*Rnk_CTR(a) 
 }
//Compute the total profit
var total_profit = 0.0
for ((advertiser, profit2) <- profit_map){
  total_profit = total_profit + profit2   
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

