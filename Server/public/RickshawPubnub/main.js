//Main entry
//Router to communicate with the Server

(function(exports)
 {
    
	 function trans(data)
	 {
		 var mat=data.mat.split("\n").filter(function(d){return d.length>0})
			 .map(function(d){return d.split(" ").filter(function(d){return d.length>0}).map(function(dd){return parseFloat(dd)})})
		 data.mat=mat
         data.index=data.index.split(" ").map(function(d){return parseInt(d)})
		 return data
	 }
	 

	 exports.main=function()
	 {
		 //var ws = new WebSocket("ws://stout.cs.berkeley.edu:9000/socket");
		 var ws = new WebSocket("ws://localhost:9000/socket");
		 ws.onmessage=function(msg)
		 {
		     /*console.log(msg.data.length)
		     var data=JSON.parse(msg.data)
		     console.log("fin")
		     if (msg.data.length>400000){
		         data={type:"meta",model:"LDA",opts:{name:["aa","bb"],type:[]},output:{name:["likelihood","model"],type:["scala","matrix"]},dict:["a","b","c"]}
		        view.panel.showMeta(data)
		     }*/
			 var data=JSON.parse(msg.data)
             //console.log(data.type)
			 if (data.type=="meta"){
			     console.log(data.output)
    		     //data.opts={name:["batchSize","dim","isprob","lambda","npasses","nsamps","power","pstep","putBack","rate","sample","useGPU","warmup","weps","wsize"],type:[]}
				 //data.opts={name:["addConstFeat","alpha","batchSize","beta","dim","doubleScore","evalStep","exppsi","featType","isprob","lambda","LDAeps","minuser","npasses","nsamps","nzPerColumn","power","pstep","putBack","resFile","sample","sizeMargin","startBlock","uiter","useGPU","warmup","weps","wsize"],type:[]}
				 view.panel.showMeta(data)
			 }
			 if (data.type=="data")
				 view.panel.update(data)
			 if (data.type=="No engine")
				 console.log("No engine")
		 }
		 
		 function createMat()
		 {
		     var n=10,m=10
		     res=[]
		     for(var i=0;i<n;i++)
		        for(var j=0;j<m;j++)
		            res.push({i:i,j:j,v:parseInt(100*Math.random())})
		     return res
		 }
		 
		 ws.onopen=function()
		 {
		     view.init({
                 send:function(obj){
                     ws.send(JSON.stringify(obj))
                 }
             })
             
             
		 }
	 }
 }
)(this)

main()





/*var state=4
    		 view.init({
                 send:function(obj){
                     if (obj.type=="model"){
                         view.panel.showMeta({
                             type:"model",
                             name:"LDA",
                             opts:{name:["addConstFeat","alpha","batchSize","beta","dim","doubleScore","evalStep","exppsi","featType","isprob","LDAeps","minuser","npasses","nzPerColumn","power","pstep","putBack","resFile","sample","sizeMargin","startBlock","uiter","useGPU","warmup","weps"],type:[]},
                             output:{name:["model","user","likeihood"],type:["matrix","matrix","scalar"]}
                         })
                     }
                     if (obj.type=="register"){
                         setInterval(function(){
                             if (obj.datatype=="scalar")
                                view.panel.update({id:obj.id,data:Math.random(state*(Math.random()-0.5))})
                            else
                                view.panel.update({id:obj.id,data:createMat()})
                            
                         },100+(obj.datatype=="matrix")*1000)
                     }
                     if (obj.type="adjust")
                     {
                         state=obj.value
                     }
                    //ws.send(JSON.stringify(obj))
             }})*/

