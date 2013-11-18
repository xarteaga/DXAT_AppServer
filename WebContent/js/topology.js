(function ($) {

	var width = 710, height = 420;

	var color = d3.scale.category20();

	var force = d3.layout.force().charge(-2000).linkDistance(120).size(
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
						for (var k=0;k<result.length;k++){

							if(result[i].data.inventoryId == ("SW-"+k+":" + j).toString()) {
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
						alert(src+" "+trg);
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
			return "nodeRED"
		}
	}))
	.attr("r", 20)
	.on("click", function(d,i) { 
		alert("device id: "+d.data.inventoryId); 
		console.log(d.data.inventoryId);
		//$("#devices").html(jQuery.deviceView.render().el);
		//renderDevice();
		console.log(i);
	})
	.call(force.drag);


	node.append("title").text(function(d) {
		return d.data.inventoryId
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