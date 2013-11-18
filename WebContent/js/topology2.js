(function ($) {

	var width = 710, height = 420;

	var color = d3.scale.category10();

	var force = d3.layout.force().charge(-420).linkDistance(5).size(
			[ width, height ]);

	var svg = d3.select(".topo").append("svg").attr("width", width).attr(
			"height", height);

	/*POST GUAY CON OBTENCION DE PARAMETROS!*/
	var RESTapi= "http://localhost:7474/db/data/node/2/traverse/node";
	var nodes=[];
	var nodesTag;
	var totalNodes = 0;
	var nodeOutLinks = new Array();
	var ifaces;
	var rawIfaces = new Array();


	var links = new Array();
	var data = [];



	var filter =/* {
		"return_filter" : {

			"body" : "position.endNode().getProperty('inventoryId').contains('SW')",
			"language" : "javascript"
		},
		"prune_evaluator" : {
			"name" : "none",
			"language" : "builtin"
		}
	};*/
	{
			"return_filter" : {
				"body" : "position.length()<100;",
				"language" : "javascript"
			},
			"prune_evaluator" : {
				"name" : "none",
				"language" : "builtin"
			}
	};
	$.ajaxSetup({async:false});  //execute synchronously
	var command = JSON.stringify(filter);

	$.ajax( {
		type : "POST",
		url : RESTapi,
		data : command,
		dataType : "json",
		contentType : "application/json",
		success : function(result, xhr, textStatus) {

			console.log(result);
			console.log(result[0].data);
			console.log(result[1].data);
			console.log(result[2].data);

			for ( var i = 0; i < result.length; i++) {
				for ( var j = 0; j < result.length; j++) {
					if (result[i].data.inventoryId == ("SW-" + j).toString()) {
						//alert("Node discovered: "+result[i].data.inventoryId);
						//nodes array
						nodes.push(result[i]);
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

	for(var i=0;i<nodes.length;i++){
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
								//var aux = result[i].data.inventoryId.split(_);

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

	force.nodes(nodes).start();

	var node = svg.selectAll(".node").data(nodes).enter().append("circle").attr(
			"class", (function(d) {
				if (d.data.status == true) {
					return "nodeGREEN";
				} else {
					return "nodeRED";
				}
			})).attr("r", 20)
			.on("click", function(d,i) { 
				alert("device id: "+d.data.inventoryId); 
				console.log(d.data.inventoryId);
				//$("#devices").html(jQuery.deviceView.render().el);
				//renderDevice();
				console.log(i);
			})
			.call(force.drag);

	node.append("title").text(function(d) {
		return d.data.inventoryId;
	});

	node.selectAll("circle.node").on("click", function(){
		d3.select(this).attr('r', 5)
		.style("fill","lightcoral")
		.style("stroke","red");
	});

	force.on("tick", function() {
		/*link.attr("x1", function(d) { return d.source.x; })
		    .attr("y1", function(d) { return d.source.y; })
		    .attr("x2", function(d) { return d.target.x; })
		    .attr("y2", function(d) { return d.target.y; });*/

		node.attr("cx", function(d) {
			return d.x;
		}).attr("cy", function(d) {
			return d.y;
		});
	});





	/*BANC DE PROVES ANTERIOR

		$.post((nodes[i]+"/traverse/node/"), {
				"return_filter" : {
					"body" : "position.length()<7;",
					"language" : "javascript"
				}
			}, function(result) {
				ifaces[i]=new Array(result.length);

				for (var j=0; j<result.length;j++){
					if((result[j].data.inventoryId)!= "SDNnetwork"){
						var aux =result[j];
						ifaces[i][j]=aux;
						//UNCOMMENT TO VIEW INFO = interfaces SW-0_P0, SW-0_P1...
						//window.alert(ifaces[i][j].data.inventoryId);

						//UNCOMMENT TO VIEW INFO = URL GET the outgoing relation with other node*/
//	window.alert(result[j].outgoing_relationships);
	/*GET the URL to find the links between interfaces
	$.getJSON(ifaces[i][j].outgoing_relationships, {
		format : "json"
	}, function(res) {
		//Links information: SW-1_P4-PC-3_P0,SW-3_P0-CT_P3,SW-2_P3-SW-3_P1
		links= res;
		//LINKS THREATMENT
		for (var k=0; k<links.length; k++){
				//window.alert(links[k].data.id);
				var aux2 = new Array();
				aux2 =(links[k].data.id.match(/SW/g));
				if (((links[k].data.id.search("CT")) == -1)&&(aux2.length==2)){
					//LINKS SW-to-SW
					window.alert(links[k].data.id);
				}
		}
	});
	}
	}	
	}	
	);
	}
	 FINAL DE BANC DE PROVES*/
})(jQuery);