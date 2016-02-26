package simulation

import BIDMat.{FMat, CSMat, Dict, SBMat}
import BIDMat.MatFunctions._

/**
  * Created by jianqiao on 2/25/16.
  */
object SimulationMain {

  def main(args: Array[String]): Unit = {

    val modelPath = "/Users/jianqiao/workspace/BIDMach_Viz/data/model/"
    val dataPath = "/Users/jianqiao/workspace/BIDMach_Viz/data/stream/"

    //initialize model
    println("Initializing CTR model")
    val adMap = loadSBMat(modelPath + "advertisers.sbmat")
    val keyPhrases = loadSBMat(modelPath + "keyphrases.sbmat")
    val adkwMap = loadIMat(modelPath + "ad_kw_map.imat")
    val posMat = loadFMat(modelPath + "rank_comp.fmat")
    val adkwMat = loadFMat(modelPath + "ad_kw_comp.fmat")

    val ctrModel = new CTRModel(adMap, keyPhrases, adkwMap, posMat, adkwMat)


    //initialize simulation
    println("Initializing Simulation")
    val alpha = 0.5f
    val beta = 0.5f
    val reservePrice = 4.0f

    val keyWords = loadSBMat(dataPath + "keywords.sbmat")

    val advertiserMap = Dict(adMap)
    val keyWordMap = Dict(keyWords)
    val keyPhrasesMap = Dict(keyPhrases)

    val simulation = new AdBiddingSimulation(ctrModel, ctrModel,
      alpha, beta, reservePrice, dataPath,
      advertiserMap, keyPhrasesMap, keyWordMap)


    //run simulation
    println("Running Simulation")
    val metricList = simulation.run()


    //present results
    println("Simulation finished")
    metricList.foreach((metric: FMat) => {
      println(metric)
    })

  }

}
