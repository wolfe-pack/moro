function FG(){}
FG.create = function(id, data) {
	

	var force = d3.layout.force()
		.size([data.width, data.height])
		.charge(-200)
		.gravity(0.03)
		.linkStrength(0.5)
		.linkDistance(50)

	var drag = force.drag()
	var svg = d3.select("#" + id).append("svg")
	 .attr("class", "factorgraph")
	 .attr("width", data.width)
	 .attr("height", data.height)
	 .style("overflow", "visible");
	var link = svg.selectAll(".link")
	var node = svg.selectAll(".fgshape")
	var label = svg.selectAll(".label");


	force.nodes(data.graph.nodes)
		.links(data.graph.links)
		.start();

	var defs = svg.append("svg:defs")

	var link = link.data(data.graph.links)
	    .enter().append("line")
	    .attr("class", "link")


	link.each( function(d, i) {
	    defs.append("svg:marker")
			.attr("id", "markerArrow" + i)
			.attr("class", "markerArrow")
			.attr("markerWidth", "100")
			.attr("markerHeight", "100")
			.attr("refX", 0)
			.attr("orient", "auto")
			.append("svg:path")
			.attr("d",
				d3.svg.symbol()
			   .type('triangle-up')
			   .size(30))
			.attr("fill", "white");
	});

	var node = node.data(data.graph.nodes)
		.enter().append("path")
		.attr("class", function(d) {
	return 'fgshape ' +
		(d.type == 'factor' ? 'fgfactor' : d.type == 'observednode' ? 'fgnode_observed' : 'fgnode') +
		(d.fixed ? ' fgfixed' : '');
	})
		.attr("d", d3.svg.symbol()
			.type(function(d) { return d.type == 'factor' ? 'square' : 'circle' })
			.size(function(d) { return d.type == 'factor' ? 500 : 2000 }))
		.on("mouseover", function(d){
			if(d.hoverhtml != undefined) {
				setTooltip(d.hoverhtml);
				tooltip.transition()
					.duration(300)
					.style("opacity", .9);
				tooltipNode = d;
				moveTooltip();
			}
		})
		.on("mouseout", function(d){
			tooltip.transition()
		            .duration(300)
		            .style("opacity", 0)
		})
		.call(drag);

	var label = label.data(data.graph.nodes)
		.enter().append("text")
		.attr("class", "label")
		.attr("text-anchor", "middle")
		.text(function(d) { return d.text == undefined ? "" : d.text })
	 	.style("font-size", function(d) { return Math.min(700 / this.getComputedTextLength(), 15) + "px"; })
		.attr("dy", ".35em")
		.call(drag);

	var tooltipNode = null
	var tooltip = null


	var checkBounds = function() {
	    node.each(function(d) {
	     d.x = Math.max(25, Math.min(data.width-25, d.x));
	     d.y = Math.max(25, Math.min(data.height-25, d.y));
	    });
	}

	force.on("tick", checkBounds);
	for(var i=0; i<100; i++) {force.alpha(0.1); force.tick();}
	/*while(force.alpha() > 0.01) {force.tick();}*/

	var tick = function() {
		checkBounds();
		link.attr("x1", function(d) { return d.source.x; })
			.attr("y1", function(d) { return d.source.y; })
			.attr("x2", function(d) { return d.target.x; })
			.attr("y2", function(d) { return d.target.y; });


		node.attr("transform", function(d) {return "translate(" + d.x + "," + d.y + ")"});
		label.attr("transform", function(d) {return "translate(" + d.x + "," + (d.y) + ")"});
		moveTooltip();

	}


	var setTooltip = function(html) {
		if(tooltip != null) {
			tooltip.remove()
		}
		tooltip = svg.insert("foreignObject")
			.attr("class", "tooltip")
			.attr("width", "350")
			.attr("height", "500")
			.style("opacity", 0)
			.html("<div class='tooltipinner'>" + html + "</div>")
	}


	var moveTooltip = function() {
		if(tooltipNode != null) {
			tooltip.attr("transform", "translate(" + (tooltipNode.x-150) + "," + (tooltipNode.y+15) + ")" );
		}
	}

	force.on("tick", tick);
	tick();
}