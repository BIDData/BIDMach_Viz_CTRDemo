import BIDMat.{Dict, IDict, FMat, IMat, SMat, SBMat}
import BIDMat.MatFunctions._
import BIDMat.SciFunctions._


/**
  * A class containing the CTR model computed offline. By matrix factorization,
  * CTR_matrix = posComponent * adKwComponent.
  *
  * @param adMap a BIDMat SBMat, each column is an ad ID.
  * @param kwMap a BIDMat SBMat, each column is a keyphrase.
  * @param posComponent a len(rank) * 1 matrix.
  * @param adKwComponent a 1 * (number of ad-keyphrase pair) matrix.
  */

class CTRModel(adMap: SBMat, kwMap: SBMat, adKwMap: IMat, posComponent: SMat, adKwComponent: SMat) {

  /** Convert the mapping matrix into BIDMat Dict so that we can use ad/keyphrase to get their index.*/
  val adDict = new Dict(adMap.toCSMat)
  val kwDict = new Dict(kwMap.toCSMat)
  var adKwCSMat = csrow(adKwMap(0, ?).toString)
  var i = 1
  while (i < adKwMap.nrows) {
      adKwCSMat = adKwCSMat on csrow(adKwMap(i, ?).toString)
      i += 1
  }
  val adKwDict = new Dict(adKwCSMat)
  

  def getCTR(rank: Int, ad: String, kw: String): Float = {
    val ad_kw = row(adDict(ad)+1, kwDict(kw)+1)
    posComponent(rank-1, 0) * adKwComponent(0, adKwDict(ad_kw.toString))
  }
}

