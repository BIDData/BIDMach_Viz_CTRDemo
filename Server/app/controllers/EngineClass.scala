package controllers

import play.api.libs.json._

class EngineClass {

    val engine = "hi";
    
    var alpha = 0.5;
    var beta = 0.1;
    
    def addInt( a:Int, b:Int ) : Int = {
        var sum:Int = 0
        sum = a + b

        return sum
    }
    
    def computeQualityScore(): List[Double] = {
        return List(1.0 * alpha, 2.0 * alpha, 3.0 * alpha, 4.0 * beta, 5.0 * beta, 6.0 * beta);

    }


    def getAlpha() : Double = {
        return alpha
    }
    
    def getBeta() : Double = {
        return beta
    }
    
    def changeAlpha(newAlpha:Double) : Double = {
        alpha = newAlpha
        return alpha
    }
    
    def changeBeta(newBeta:Double) : Double = {
        beta = newBeta
        return beta
    }
    
}