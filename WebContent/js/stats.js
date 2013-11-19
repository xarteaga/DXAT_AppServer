(function ($) {

	var margin = {top: 20, right: 10, bottom: 30, left: 50},
	width = 750 - margin.left - margin.right,
	height = 390 - margin.top - margin.bottom;

	var parseDate = d3.time.format("%d-%b-%y").parse,
	bisectDate = d3.bisector(function(d) { return d.date; }).left,
	formatValue = d3.format(",.2f"),
	formatCurrency = function(d) { return  formatValue(d)+" Mbps"; };

	var x = d3.time.scale()
	.range([0, width]);

	var y = d3.scale.linear()
	.range([height, 0]);

	var xAxis = d3.svg.axis()
	.scale(x)
	.orient("bottom");

	var yAxis = d3.svg.axis()
	.scale(y)
	.orient("left");

	var line = d3.svg.line()
	.x(function(d) { return x(d.date); })
	.y(function(d) { return y(d.close); });
	
	d3.tsv("js/data.tsv", function(error, data) {
		data.forEach(function(d) {
			d.date = parseDate(d.date);
			d.close = +d.close;
		});
		// Filter to one symbol; the S&P 500.
		var values = data;
		console.log(data);

		data.sort(function(a, b) {
			return a.date - b.date;
		});

		x.domain([data[0].date, data[data.length - 1].date]);
		y.domain(d3.extent(data, function(d) { return d.close; }));
		
		var svg = d3.select(".send-stats").append("svg")
		.attr("width", width + margin.left + margin.right)
		.attr("height", height + margin.top + margin.bottom)
		.append("g")
		.attr("transform", "translate(" + margin.left + "," + margin.top + ")");
	    //.on("click", click);
		
		svg.append("g")
		.attr("class", "x axis")
		.attr("transform", "translate(0," + height + ")")
		.call(xAxis);

		svg.append("g")
		.attr("class", "y axis")
		.call(yAxis)
		.append("text")
		.attr("transform", "rotate(-90)")
		.attr("y", 6)
		.attr("dy", ".71em")
		.style("text-anchor", "end")
		.text("Bandwith (Mbps)");

		svg.append("path")
		.datum(data)
		.attr("class", "line")
		.attr("d", line);

		var focus = svg.append("g")
		.attr("class", "focus")
		.style("display", "none");

		focus.append("circle")
		.attr("r", 4.5);

		focus.append("text")
		.attr("x", 9)
		.attr("dy", ".35em");

		svg.append("rect")
		.attr("class", "overlay")
		.attr("width", width)
		.attr("height", height)
		.on("mouseover", function() { focus.style("display", null); })
		.on("mouseout", function() { focus.style("display", "none"); })
		.on("mousemove", mousemove);

		function mousemove() {
			var x0 = x.invert(d3.mouse(this)[0]),
			i = bisectDate(data, x0, 1),
			d0 = data[i - 1],
			d1 = data[i],
			d = x0 - d0.date > d1.date - x0 ? d1 : d0;
			focus.attr("transform", "translate(" + x(d.date) + "," + y(d.close) + ")");
			focus.select("text").text(formatCurrency(d.close));
		}

		function getRandomArbitary (min, max) {
		    //return Math.random() * (max - min) + min;
		    return Math.floor(Math.random() * (max - min + 1)) + min;

		}
		function fakeUpdate(len,obj){
			console.log(obj);
			var ret = obj;
			var rand = getRandomArbitary(0, len-1);
			var j = rand;
			for(var i = 0; i < len; i++){
				ret[(i+rand)%len].close = getRandomArbitary(420, 600);
				j+=rand;
			}
			
			return ret;
		}
		
		// On click, update the x-axis.
//		function click() {
//			console.log(values);
//			var n = values.length - 1,
//			i = Math.floor(Math.random() * n / 2),
//			j = i + Math.floor(Math.random() * n / 2) + 1;
//			//x.domain([values[i].date, values[j].date]);
//			var t = svg.transition().duration(750);
//			t.select(".x.axis").call(xAxis);
//			//t.select(".area").attr("d", area(values));
//			t.select(".line").attr("d", line(values));
			setInterval(function(d){
				// push a new data point onto the back
				values = fakeUpdate(data.length - 1,values);
				data.push(values);

				// pop the old data point off the front
				//data.shift();
				var t = svg.transition().duration(750);
				// transition the line
				//t.select(".x.axis").call(xAxis);
				t.select(".line").attr("d", line(values));
				//t.transition().attr("d", line);

			},2000);
//		}
	});

})(jQuery);