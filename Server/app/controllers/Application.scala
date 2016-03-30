package controllers

import play.api._
import play.api.mvc._
import play.api.libs.iteratee._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json._
import akka.actor.{Actor, Props, ActorSystem, ActorRef}
import scala.concurrent.duration._
import akka.util.Timeout
import akka.pattern.ask

import BIDMat._
import BIDMat.MatFunctions._
import BIDMat.SciFunctions._

//import Engine.server
//import BIDMat.MatFunctions._
//import BIDMat.SciFunctions._



class Waiter() extends Actor {
  var engine: ActorRef = null
  var channel: Concurrent.Channel[String] = null

  val system = akka.actor.ActorSystem("system")

  import system.dispatcher

  /*
  val Tick = "tick"
  val tickActor = system.actorOf(Props(new Actor {
      def receive = {
          case Tick ⇒
              val metrics = ec.computeQualityScore();

              println("ticking...");
              channel.push("{ \"Ad1\":" + metrics(0) + ",\"Ad2\":" + metrics(1) + ",\"Ad3\":" + metrics(2) + ",\"Ad4\":" + metrics(3) + ",\"Ad5\":" + metrics(4) + ",\"Ad6\":" +  metrics(5) + "}");

      }

  }));
  val cancellable = system.scheduler.schedule(0 milliseconds,2000 milliseconds,tickActor,Tick);
  */


  /*
    var ec = new EngineClass();
    println("alpha: " + ec.getAlpha());
    println("beta: " + ec.getBeta());
    println("reserve: " + ec.getReserve());
    
    var result:FMat = ec.runFMat();
    println("results: " + result);
    println("size cols: " + result.ncols);
    println("size rows: " + result.nrows);
    println("2,2 rows: " + result(2,2));
    
    println("sum: " + maxi(result));
    */


  //Mat.checkMKL

  //val v = cumsum(result)
  //println("unique: " + v);

  //val diag = BIDMat.GMat(1 on 2 on 3)
  
    // grab the path to the application, so we can read the model/data files
  val applicationPath = play.Play.application().path().getAbsolutePath()
  val modelPath = applicationPath + "/app/controllers/model/"
  val dataPath = applicationPath + "/app/controllers/stream/"

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



  def receive = {
    case ("Online") => {
      engine = sender
      println("Engine Connected")
    }

    case (c: Concurrent.Channel[String]) =>
      channel = c
    case ("browser", msg: String) => {

      if (msg contains "alpha:") {
        val Array(str1, str2) = msg.split(":");
        //ec.changeAlpha(str2.toDouble);
        //println("Now, alpha is: " + ec.getAlpha());
      }

      if (msg contains "beta:") {
        val Array(str1, str2) = msg.split(":");
        //ec.changeBeta(str2.toDouble);
        //println("Now, beta is: " + ec.getBeta());
      }

      if (msg contains "reserve:") {
        val Array(str1, str2) = msg.split(":");
        // ec.changeReserve(str2.toDouble);
        //println("Now, reserve is: " + ec.getReserve());
      }

      // For now, just grab new data every second
      if (msg contains "Sending new data...") {
          
          val metricList = simulation.runBatch()

          println("batch complete: ")
          println(metricList.size)
          var total_profit:Float = 0
          
          metricList.foreach((record: FMat) => {
            val profit = record(2)
            total_profit = total_profit + profit.toFloat
          })
          println("Total sum: " + total_profit)
          
        var jsonData = Json.obj();
        jsonData += ("Total Profit" -> Json.toJson(total_profit))
        
        
        /*
        //val metrics = ec.computeQualityScore();
        val metrics = List(1, 2, 3, 4, 5, 6);


        var jsonData = Json.obj();
        var i = 0;
        for (i <- 0 until metrics.length) {
          jsonData += ("Ad" + i -> Json.toJson(metrics(i)))
        }
        println("JSON data: " + jsonData);
        //for (int i=0; i < metrics.length; i++) {
        //    println("metric " + i + " :" + metrics[i]);
        //}
        */
        channel.push(Json.stringify(jsonData));
        //channel.push("{ \"Ad1\":" + metrics(0) + ",\"Ad2\":" + metrics(1) + ",\"Ad3\":" + metrics(2) + ",\"Ad4\":" + metrics(3) + ",\"Ad5\":" + metrics(4) + ",\"Ad6\":" +  metrics(5) + "}");

      }


      if (engine != null)
        engine ! msg
    }
    case ("engine", msg: String) => {
      println("sending " + msg.length)

      if (channel != null)
        channel.push(msg)
    }
  }
}

class Application extends Controller {
  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  println("Running")
  val system = ActorSystem("BIDDemo");
  val waiter = system.actorOf(Props(classOf[Waiter]), "server");
  //println(System.getProperty("java.library.path"))
  //server.init(system,waiter)

    /*
  // grab the path to the application, so we can read the model/data files
  val applicationPath = play.Play.application().path().getAbsolutePath()
  val modelPath = applicationPath + "/app/controllers/model/"
  val dataPath = applicationPath + "/app/controllers/stream/"

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

  println("Simulation set up")
  println("Running Simulation")
  */

  //#waiter ! ("engine","result");
  //println("Sent from engine");

  def getSocket = WebSocket.using[String] {
    request =>
      println("Socket")
      val (out, channel) = Concurrent.broadcast[String];
      waiter ! channel
      implicit val timeout = Timeout(5 seconds);
      val in = Iteratee.foreach[String] {
        msg =>
          println("From browser: " + msg)
          //channel.push("{ 'Ad1': 10, 'Ad2': 15, 'Ad3': 12, 'Ad4': 5, 'Ad5': 6, 'Ad6': 7.5}")
          //channel.push("{ \"Ad1\": 10,  \"Ad2\": 10,  \"Ad3\": 10,  \"Ad4\": 10,  \"Ad5\": 10,  \"Ad6\": 10}")

          //server.changePara(msg)
          waiter !("browser", msg)
          
          
          /*
          val metricList = simulation.runBatch()

          println("batch complete: ")
          println(metricList.size)
          var total_profit:Float = 0
          
          metricList.foreach((record: FMat) => {
            val profit = record(2)
            total_profit = total_profit + profit.toFloat
          })
          println("Total sum: " + total_profit)
          */

        //the channel will push to the Enumerator
        //val worker = system.actorOf(Props(classOf[Worker],waiter,channel));
        //worker ! msg
      }
      (in, out)
  }

  /*
  for (i <- 0 to 50) {
    val metricList = simulation.runBatch()

    println("batch " + i.toString)
    println(metricList.size)
    println(metricList(0))
  }

  println("Done Simulation")
  */
  
}
