# BIDMach_Viz

This project explores the scope of interactive visualizations, using BIDMach to run an advertisement auction simulation and a Scala Play frontend to plot simulation metrics.

### Visualization Demo
Navigate to /Server, and run the following command in a terminal: ./activator run 

This will start up the Scala Play Web Server. Once it starts up, navigate your browswer to:
http://localhost:9000/examples/Viz.html





This demo uses Rickshaw (a Javascript library for creating real time graphs) and Pubnub (to handle data streaming). Note that this means that the visualization requires an Internet connection in order for PubNub to work.

Rickshaw: https://github.com/shutterstock/rickshaw  
Pubnub: https://github.com/pubnub/pubnub-rickshaw

The demo simulates total profit and total number of clicks from auctions in each mini-batch. There are controls for Alpha, Beta, and Reserve Pricing to show how we can tune parameters.
