package controllers

import play.api._
import play.api.mvc._
import play.api.libs.iteratee._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json._
import akka.actor.{Actor, Props, ActorSystem, ActorRef}
import scala.concurrent.duration._
import scala.collection._
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
  var simulation: AdBiddingSimulation = null

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
  
  // OPTION: this determines whether we plot advertiser's stats, or aggregate
  var plotAdvertisers = 0
  var advertisers = List(891, 900, 809, 894, 311)

  val keyWords = loadSBMat(dataPath + "keywords.sbmat")

  val advertiserMap = Dict(adMap)
  val keyWordMap = Dict(keyWords)
  val keyPhrasesMap = Dict(keyPhrases)

  val numSlots = 10

  simulation = new AdBiddingSimulation(ctrModel, ctrModel,
    alpha, beta, reservePrice, numSlots, dataPath,
    advertiserMap, keyPhrasesMap, keyWordMap)
  println("Done creating Simulation")
  var batch_number = 1;


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
        if (simulation != null) {
            simulation.setField("alpha", str2.toFloat);
            println("Now, alpha is: " + simulation.getField("alpha"));
            
        }
      }

      if (msg contains "beta:") {
        val Array(str1, str2) = msg.split(":");
        
        if (simulation != null) {
            simulation.setField("beta", str2.toFloat);
            println("Now, beta is: " + simulation.getField("beta"));
            
        }
      }

      if (msg contains "reserve:") {
        val Array(str1, str2) = msg.split(":");

        if (simulation != null) {
            simulation.setField("reservePrice", str2.toFloat);
            println("Now, reserve is: " + simulation.getField("reservePrice"));
            
        }
      }

      // Grab new data
      if (msg contains "Sending new data...") {
          
        if (simulation != null) {
            val metricList = simulation.runBatch()
            println("batch complete: " + batch_number)
            println(metricList.size)
            
            
            if (plotAdvertisers == 1) {
                
                println(advertisers.size)
                
                val profitBuffer  = scala.collection.mutable.ArrayBuffer.empty[Double]
                
                // Initialize the appropriate amount of 0's to the profit buffer
                advertisers.foreach((ad_id: Int) => {
                    profitBuffer += 0.0
                })
                
                // batch, auction_id, ad_id, profit, click, price
                metricList.foreach((record: FMat) => {
                    val adID = record(2)
                    val profit = record(3)
                    val clicks = record(4)
                    val bid_price = record(5)
                    
                    var index = advertisers.indexOf(adID.toFloat)
                    
                    if (index >= 0) {
                        //println("Matched: " + record)
                        profitBuffer(index) = profitBuffer(index) + profit.toFloat
                    }
                })
                
                println("Profits: " + profitBuffer)
                
                var jsonData = Json.obj();
                for (i <- profitBuffer.indices) {
                    jsonData += ("Ad " + i + "(id: " + advertisers(i) + ")"-> Json.toJson(profitBuffer(i)))
                }
                channel.push(Json.stringify(jsonData)); 
                
                
            } else {
                var total_profit:Float = 0
                var total_clicks:Float = 0
                var total_bids:Float = 0
                
                val profitBuffer  = scala.collection.mutable.ArrayBuffer.empty[Double]
                // Initialize the appropriate amount of 0's to the profit buffer
                advertisers.foreach((ad_id: Int) => {
                    profitBuffer += 0.0
                })
                
                
                // batch, auction_id, ad_id, profit, click, price
                metricList.foreach((record: FMat) => {
                    
                    val adID = record(2)
                    
                    val profit = record(3)
                    total_profit = total_profit + profit.toFloat
                        
                    val clicks = record(4)
                    total_clicks = total_clicks + clicks.toFloat
                        
                    val bid = record(5)
                    total_bids = total_bids + bid.toFloat
                    
                    var index = advertisers.indexOf(adID.toFloat)
                    if (index >= 0) {
                        //println("Matched: " + record)
                        profitBuffer(index) = profitBuffer(index) + profit.toFloat
                    }
                })
            
                var average_bid:Float = total_profit / metricList.size
                println("Total profit: " + total_profit)
                //println("Average bid: " + average_bid)
                println("Total clicks: " + total_clicks)
                println("Sum of Prices: " + total_bids)
            
                var jsonData = Json.obj();
                jsonData += ("Total Profit" -> Json.toJson(total_profit))
                jsonData += ("Total Clicks" -> Json.toJson(total_clicks * 100))
                jsonData += ("Sum of Prices" -> Json.toJson(total_bids * 100))
                
                for (i <- profitBuffer.indices) {
                    jsonData += ("Profit: Ad " + i + " (id: " + advertisers(i) + ")"-> Json.toJson(profitBuffer(i)))
                }
                
                //jsonData += ("Average Bid" -> Json.toJson(average_bid))
                channel.push(Json.stringify(jsonData)); 
            }
            
            batch_number = batch_number + 1
        } else {
          println("Waiting for simulation setup")
        }
        
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

          //server.changePara(msg)
          waiter !("browser", msg)
          

        //the channel will push to the Enumerator
        //val worker = system.actorOf(Props(classOf[Worker],waiter,channel));
        //worker ! msg
      }
      (in, out)
  }
 
}
