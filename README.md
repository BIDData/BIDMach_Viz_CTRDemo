# BIDMach_Viz


### Visualization Demo
To view the demo, open BIDMach_Viz/RickshawPubnub/examples/Demo.html in a web browser.


This demo uses Rickshaw (a Javascript library for creating real time graphs) and Pubnub (to handle data streaming). Note that this means that the data requires an Internet connection in order for PubNub to work.

Rickshaw: https://github.com/shutterstock/rickshaw  
Pubnub: https://github.com/pubnub/pubnub-rickshaw

The demo simulates profits from different advertisers over time, and shows them in a real time stacked area plot. Note that the data is currently randomly generated (and some advertiser's profits are set to 0), and there are controls for Alpha and Beta to simulate how we can use squashing.
