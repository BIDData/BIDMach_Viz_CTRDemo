package simulation

import BIDMat._
import BIDMat.MatFunctions._


/**
  * A class containing the CTR model computed offline. By matrix factorization,
  * CTR_matrix = posComponent * adKwComponent.
  *
  * @param adMap a BIDMat SBMat, each column is an ad ID.
  * @param kwMap a BIDMat SBMat, each column is a keyphrase.
  * @param posComponent a len(rank) * 1 matrix.
  * @param adKwComponent a 1 * (number of ad-keyphrase pair) matrix.
  */

class CTRModel(adMap: SBMat, kwMap: SBMat, adKwMap: IMat, posComponent: FMat, adKwComponent: FMat) {

  /** Convert the mapping matrix into BIDMat Dict so that we can use ad/keyphrase to get their index. */
  /** Beware: The index in adKwMap is 1-indexed, while the matrix is 0-indexed. */
  val adDict = new Dict(adMap.toCSMat)
  val kwDict = new Dict(kwMap.toCSMat)
  var adKwCSMat = CSMat(adKwMap.nrows, 1)
  var i = 0
  while (i < adKwMap.nrows) {
    adKwCSMat(i, 0) = adKwMap(i, ?).toString
    i += 1
  }
  val adKwDict = new Dict(adKwCSMat)


  def getCTR(rank: Int, ad: String, kw: String): Float = {

    val ad_kw = row(adDict(ad), kwDict(kw))
    posComponent(0, rank-1) * adKwComponent(0, adKwDict(ad_kw.toString))
  }
}
