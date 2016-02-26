name := "BIDMach_Viz"

version := "1.0"

scalaVersion := "2.11.7"

libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.3.0"
libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.4.1"

mainClass in (Compile, run) := Some("simulation.SimulationMain")

