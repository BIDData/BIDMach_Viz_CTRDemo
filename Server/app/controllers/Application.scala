package controllers

import play.api._
import play.api.mvc._
import play.api.libs.iteratee._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json._
import akka.actor.{Actor, Props, ActorSystem,ActorRef}
import scala.concurrent.duration._
import akka.util.Timeout
import akka.pattern.ask
//import Engine.server
//import BIDMat.MatFunctions._
//import BIDMat.SciFunctions._

class Waiter() extends Actor{
    var engine:ActorRef = null
    var channel:Concurrent.Channel[String] = null
  	def receive ={
        case ("Online")=>{
          engine=sender
          println("Engine Connected")
        }
        
        case (c:Concurrent.Channel[String])=>
            channel=c
        case ("browser",msg:String)=>{
            if (engine!=null)
                engine ! msg
        }   
        case ("engine",msg:String)=>{
            //println("sending "+msg.length)
            if (channel!=null)
                channel.push(msg)
        }
	}
}

class Application extends Controller{
	def index = Action{
		Ok(views.html.index("Your new application is ready."))
	}
	println("Running")
	val system = ActorSystem("BIDDemo");
	val waiter = system.actorOf(Props(classOf[Waiter]),"server");
	//println(System.getProperty("java.library.path"))
	//server.init(system,waiter)
	
	def getSocket = WebSocket.using[String]{
		request =>
		    println("Socket")
    		val (out,channel) = Concurrent.broadcast[String];
            waiter ! channel
            implicit val timeout = Timeout(5 seconds);
    		val in = Iteratee.foreach[String] {
    			msg => 
                    println(msg)
                    //channel.push("{ 'Ad1': 10, 'Ad2': 15, 'Ad3': 12, 'Ad4': 5, 'Ad5': 6, 'Ad6': 7.5}")
                    channel.push("{ \"Ad1\": 10,  \"Ad2\": 10,  \"Ad3\": 10,  \"Ad4\": 10,  \"Ad5\": 10,  \"Ad6\": 10}")
                    //server.changePara(msg)
                    //waiter ! ("browser",msg)
    			
    			//the channel will push to the Enumerator
    			//val worker = system.actorOf(Props(classOf[Worker],waiter,channel));
    			//worker ! msg
    		}
    		(in, out)
	}

}
