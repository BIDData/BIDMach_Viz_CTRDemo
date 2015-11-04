import BIDMat.{Dict, IDict, FMat, SMat, SBMat}
import BIDMat.MatFunctions._
import BIDMat.SciFunctions._

class CTRModel(adMap: SBMat, kwMap: SBMat, adKwMap: IMat, valMat: SMat) {

  val adDict = Dict(adMap)
  val kwDict = Dict(kwMap)
  val adKwDict = IDict(adKwMap)

  def getValMat() = valMat

  // get CTR from rank-(ad, kw) matrix
  def getCTR(rank: Int, ad: String, kw: String): Float = {
    val ad_kw = row(adDict(ad), kwDict(kw))
    valMat(rank, adKwDict(ad_kw))
  }

  def getCTR(ad: String, kw: String): Float = {
    //TODO: for the ad-kw matrix, how to incorporate it into this model?
  }

}