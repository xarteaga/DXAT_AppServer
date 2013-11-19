(function ($) {

	var Device = Backbone.Model.extend({
		defaults:{
			id:"its unique id",
			name:"device name",
			status:"actual state",
			portsConnected:"number of active ports",
			totalPorts:"total number of ports",
			mac1:"mac1",
			mac2:"mac2",
			mac3:"mac3",
			mac4:"mac4"
		}
	});

	var DevicesList = Backbone.Collection.extend({
		model: Device,
		//url:'http://localhost:8080/device-manager-api/webapi/devices/routers',
		url:'http://localhost:7474/db/data/node/1',
		parse:function (response) {
			console.log(response);
			//response.id = response.inventoryId;
			// Parse the response and construct models
//			for ( var i = 0, length = response.routers.length; i < length; i++) {
//				var currentValue = response.routers[i];
//				var devObject = {};
//				console.log(currentValue);
//				devObject.interfaces = currentValue.interfaces;
//				devObject.routingTable = currentValue.routingTable;
//				devObject.ports = currentValue.ports;
//				devObject.inventoryId = currentValue.inventoryId;
//				// push the model object
//				this.push(devObject);
//			}
//			console.log(this.toJSON());

			//return models
			return this.models;
			//return response;
		}
	});
	
	var devices = new DevicesList();
	
	var DeviceView = Backbone.View.extend({
		model: new Device(),
		tagName:"div",
		className:"device",
		template:$("#device-template").html(),

		render:function () {
			var tmpl = _.template(this.template); //tmpl is a function that takes a JSON object and returns html

			this.$el.html(tmpl(this.model.toJSON())); //this.el is what we defined in tagName. use $el to get access to jQuery html() function
			return this;
		}
	});


//	var device = new Device({
//	id:"1",
//	name:"Device1",
//	status:"UP",
//	portsConnected:"3",
//	totalPorts:"4",
//	mac1:"11:11:11:11:11:11",
//	mac2:"12:12:12:12:12:12",
//	mac3:"13:13:13:13:13:13",
//	mac4:"14:14:14:14:14:14"
//	});


//	deviceView = new DeviceView({
//		model: Device
//	});

	//Global View if needed, now not used
	//Maybe to be used for nodes association on graph topology
	var DevicesView = Backbone.View.extend({
		model: devices,
		el: $('#devices-container'),
		initialize: function(){
			var self = this;
			this.model.on('add', this.render, this);
			this.model.on('remove', this.render,this);
			// get all devices (Backbone.sync powah!!!)
			this.model.fetch({
				success: function(response,xhr) {
					console.log("Success fetchingg");
					self.render();
				},
				error:function () {
					console.log(arguments);
				}	
			});

		},	

		render: function(){
			var self = this;
			self.$el.html('');
			_.each(this.model.toArray(),function(device,i){
				self.$el.append((new DeviceView({model: device})).render().$el);
			});
			return this;
		}

	});
	var deviceView = new DeviceView();
	var devicesView = new DevicesView();

	$(document).ready(function(){
		//$("#devices").html(devicesView.render().el);
		//$("#devices").html(deviceView.render().el);
		$("#devices").html(deviceView.render().el);
		$("#devices").append("for each here, per mac and so on!<p></p>");
		
		$('#add-device').submit(function(ev){
			//var the_id= "RT-"+ ++numDevices;
			//var device = new Device({id:the_id,name:$('#device-name').val(),ip:$('#device-ip').val(),description:$('#device-description').val()});
			var device = new Device();//{inventoryId:$('#device-inventoryId').val(),ports:$('#device-ports').val(),interfaces:$('#device-interfaces').val(),routingTable:$('#device-routingTable').val()});
			devices.add(device);
			console.log(devices.toJSON());
//			device.save({id:device.get('id'),name:$('#device-name').val(),ip:$('#device-ip').val(),description:$('#device-description').val()},{
//			succes: function(){ consol.log("successfully saved device!");},
//			error: function(){ console.log("error saving device!");}
//			})
			return false;	
		});
	});
	function renderDevice(/*id*/){
		var device = new Device();
		$("#devices").html(deviceView.render().el);
	};
	
	//put the name of the Device selected on the button
	$(function(){  
	  $(".dropdown-menu li a").click(function(){
	    $(".btn.dropdown-toggle:first-child").text($(this).text());
	    $(".btn.dropdown-toggle:first-child:first-child").val($(this).text()+ ' <span class="caret"></span>');
	  });
	});
	
	
	
	
	
	
	
	
	//TOPO
	var width = 710, height = 420;

	var color = d3.scale.category20();

	var force = d3.layout.force().charge(-1120).linkDistance(80).size(
			[ width, height ]);

	var svg = d3.select(".topo").append("svg").attr("width", width).attr(
			"height", height);

	/*POST GUAY CON OBTENCION DE PARAMETROS!*/

	var RESTapi = "http://localhost:7474/db/data/node/1/traverse/node";
	//var RESTapi= "http://ec2-54-229-220-96.eu-west-1.compute.amazonaws.com:7474/db/data/node/2/traverse/node";
	var nodes=[];
	var nodesTag;
	var totalNodes = 0;
	var nodeOutLinks = new Array();
	var ifaces;
	var rawIfaces = new Array();
	var rawLinks=new Array();
	var numHosts=0;
	var links = [];
	var data = [];
	var fromTo = new Array();

	var filter =
	{
			"return_filter" : {
				"body" : "position.length()<100;",
				"language" : "javascript"
			},
			"prune_evaluator" : {
				"name" : "none",
				"language" : "builtin"
			}
	}
	$.ajaxSetup({async:false});  //execute synchronously
	var command = JSON.stringify(filter);

	$.ajax( {
		type : "POST",
		url : RESTapi,
		data : command,
		dataType : "json",
		contentType : "application/json",
		success : function(result, xhr, textStatus) {

			for ( var i = 0; i < result.length; i++) {
				//alert(result[i].data.inventoryId);
				for ( var j = 0; j < result.length; j++) {
					if (result[i].data.inventoryId == ("SW-" + j).toString()) {
						//alert("Node discovered: "+result[i].data.inventoryId+ "Type: " +result[i].data.type);
						//nodes array
						var isNew = true;
						if (nodes.length>0){
							for (var k=0; k<nodes.length;k++){
								if (nodes[k].data.inventoryId==result[i].data.inventoryId){
									isNew=false;
									//alert("EXISTE");
								}	
							}
						}
						if ((isNew)&&(result[i].data.type=="MASTER")){
							nodes.push(result[i]);
						}

					}else{
						if(result[i].data.inventoryId==("HOST-"+j).toString()){
							//alert("Añado HOST!: "+result[i].data.inventoryId);
							nodes.push(result[i]);
							numHosts++;
						}


						for (var k=0;k<result.length;k++){

							if(result[i].data.inventoryId == ("SW-"+k+":" + j).toString()) {
								//alert("Interface discovered: ["+k+"] ["+j+"] " +result[i].data.inventoryId);
								rawIfaces.push(result[i]);
							}
							if(result[i].data.inventoryId == ("HOST-"+k+":" + j).toString()) {
								//alert("Interface discovered: ["+k+"] ["+j+"] " +result[i].data.inventoryId);
								rawIfaces.push(result[i]);
							}
						}
					}

				}
				//ES poden trobar mes nodes y tal pero per ara tinc els que minteressen es poden veure tots a:
				//alert(result[i].data.inventoryId);
			}

		},
		error : function(xhr) {
			window.console && console.log(xhr);
		},
		complete : function() {


		}
	});
	$.ajaxSetup( {async : true}); //execute synchronously

	for(i=0;i<nodes.length;i++){
		for(var j=0;j<rawIfaces.length;j++){
			if ((rawIfaces[j].data.inventoryId.search(nodes[i].data.inventoryId)!= -1)){
				$.ajaxSetup({async:false});  //execute synchronously
				$.ajax( {
					type : "GET",
					url : rawIfaces[j].self+"/relationships/out",
					//data : command,
					//dataType : "json",
					//contentType : "application/json",
					success : function(result, xhr, textStatus) {

						for (var i=0;i<result.length;i++){
							if (result[i].data.inventoryId!=null){
								//Aki tinc la informacio dels links! 
								//alert(result[i].data.inventoryId);
								rawLinks.push(result[i]);	
							}
						}
					},
					error : function(xhr) {
						window.console && console.log(xhr);
					},
					complete : function() {


					}
				});
				$.ajaxSetup( {async : true}); //execute synchronously
			}
		}	
	}
	var fromTo = [];//new Array(rawLinks.length);
	var source = [];
	var target = [];
	for (var i=0;i<rawLinks.length;i++){
		//alert(rawLinks[i].data.inventoryId);
		var aux = new Array(2);
		aux =rawLinks[i].data.inventoryId.split("_");
		if (aux[0]!= null){

			if ((aux[0].search("HOST")!=-1)||(aux[1].search("HOST")!=-1)){

				if(aux[0].search("HOST")!=-1){
					aux[0]=aux[0].substr(0,6);
					//alert("HOST TO LINK!");

					if(aux[1].search("SW-")!=-1){
						aux[1]=aux[1].substr(0,4);
					}
				}
				else{
					//alert("LINK TO HOST!");
					aux[1]=aux[1].substr(0,6);

					if(aux[0].search("SW-")!=-1){
						aux[0]=aux[0].substr(0,4);
					}
				}
			}


			//alert("link 1: "+aux[0]+" link 2: " +aux[1]);
			source.push(aux[0]);
			target.push(aux[1]);
		}       	
	}
	var src,trg;

	for (i=0; i<source.length;i++){	
		for (var j=0;j<nodes.length;j++){
			//alert(source[j]+ " "+ nodes[i].data.inventoryId);
			if (source[i]==(nodes[j].data.inventoryId)){	
				src=j;
				for (var k=0;k<nodes.length;k++){

					if (target[i]==(nodes[k].data.inventoryId)){	
						trg = k;

						//alert(src+" "+trg);
						links.push({"source":src,"target":trg,"value":8});
					}
				}

			}
		}	
	}

	force
	.nodes(nodes)
	.links(links)
	.start();

	var link = svg.selectAll(".link")
	.data(links)
	.enter().append("line")
	.attr("class", "link")
	//.style("stroke", "#F00")
	.style("stroke-width", function(d) { return Math.sqrt(d.value); });




	var node = 	svg.selectAll(".node")
	.data(nodes)
	.enter().append("circle")
	.attr("class", (function(d) {
		if (d.data.status == true) {
			return "nodeGREEN"
		} else {

			if (d.data.inventoryId == ("HOST-0")){
				return "nodeBLUE"
			}
			if (d.data.inventoryId == ("HOST-1")){
				return "nodeBLUE"
			}
			if (d.data.inventoryId == ("HOST-2")){
				return "nodeBLUE"
			}
			if (d.data.inventoryId == ("HOST-3")){
				return "nodeBLUE"
			}

			return "nodeRED"

		}
	}))	
	.on("click", function(d){renderDevice();}) 
	.attr("r", 25).call(force.drag);


	node.append("title").text(function(d) {
		return ("inventoryId: "+d.data.inventoryId+"\n getOfAddr: "+d.data.getOfAddr+"\n software: "+d.data.software+
				"\n model: "+d.data.model+"\n hardware: "+d.data.hardware+"\n status: "+d.data.status+"\n manufacturer: "+d.data.manufacturer+
				"\n serialNumber: "+d.data.serialNumber+"\n ifaces: " +d.data.ifaces+"\n type: "+d.data.type+"\n datapath: "+d.data.datapath)
	});

	force.on("tick", function() {
		link.attr("x1", function(d) { return d.source.x; })
		.attr("y1", function(d) { return d.source.y; })
		.attr("x2", function(d) { return d.target.x; })
		.attr("y2", function(d) { return d.target.y; });

		node.attr("cx", function(d) {
			return d.x;
		}).attr("cy", function(d) {
			return d.y;
		});
	});
	
})(jQuery);