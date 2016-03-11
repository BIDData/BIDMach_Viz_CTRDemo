package controllers

import play.api.libs.json._
//import BIDMat.FMat
import BIDMat._
import BIDMat.MatFunctions._

import scala.collection.{mutable, breakOut}

class EngineClass {

    // we're gonna have an FMat of auction_id, advertiser_id, "price"
    var metricList = mutable.MutableList[FMat]() 
    
    val profitMatrix:FMat = FMat(zeros(10, 3))
    profitMatrix(0, 0) = 1
    profitMatrix(1, 0) = 1
    profitMatrix(2, 0) = 1
    profitMatrix(3, 0) = 2
    profitMatrix(4, 0) = 2
    profitMatrix(5, 0) = 2
    profitMatrix(6, 0) = 2
    profitMatrix(7, 0) = 2
    profitMatrix(8, 0) = 3
    profitMatrix(9, 0) = 3
    
    profitMatrix(0, 1) = 1
    profitMatrix(1, 1) = 2
    profitMatrix(2, 1) = 3
    profitMatrix(3, 1) = 1
    profitMatrix(4, 1) = 2
    profitMatrix(5, 1) = 3
    profitMatrix(6, 1) = 4
    profitMatrix(7, 1) = 5
    profitMatrix(8, 1) = 5
    profitMatrix(9, 1) = 6
    
    profitMatrix(0, 2) = 10
    profitMatrix(1, 2) = 9
    profitMatrix(2, 2) = 8
    profitMatrix(3, 2) = 6
    profitMatrix(4, 2) = 4.5
    profitMatrix(5, 2) = 3
    profitMatrix(6, 2) = 2
    profitMatrix(7, 2) = 1
    profitMatrix(8, 2) = 15
    profitMatrix(9, 2) = 14
    
    
    var alpha = 0.5;
    var beta = 0.1;
    var reserve = 0.1
    
    def addInt( a:Int, b:Int ) : Int = {
        var sum:Int = 0
        sum = a + b

        return sum
    }
    
    def computeQualityScore(): List[Double] = {
        return List(1.0 * alpha, 2.0 * alpha, 3.0 * alpha, 4.0 * beta, 5.0 * beta, 6.0 * beta);
    }

    def run(): mutable.MutableList[FMat] = {
        
        return metricList
    }
    
    def runFMat(): FMat = {
        return profitMatrix
    }

    def getAlpha() : Double = {
        return alpha
    }
    
    def getBeta() : Double = {
        return beta
    }
        
    def getReserve() : Double = {
        return reserve
    }
    
    def changeAlpha(newAlpha:Double) : Double = {
        alpha = newAlpha
        return alpha
    }
    
    def changeBeta(newBeta:Double) : Double = {
        beta = newBeta
        return beta
    }
    
    def changeReserve(newReserve:Double) : Double = {
        reserve = newReserve
        return reserve
    }
    
}