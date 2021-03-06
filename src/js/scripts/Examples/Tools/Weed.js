////////////////////////////////////////////////////////////////////////////////
// Values

var values = {
	size: 20,
	max: 10
};

////////////////////////////////////////////////////////////////////////////////
// Mouse handling

tool.eventInterval = 1000 / 100; // 100 times a second

var branches;
function onMouseDown(event) {
	branches = [];
	var count = Math.round(Math.random() * (values.max - 1) + 1);
	var group = new Group();
	for (var i = 0; i < count; i++)
		branches.push(new Branch(event.point, group));
}

function onMouseUp(event) {
	for (var i in branches)
		branches[i].finish();
}

function onMouseDrag(event) {
	for (var i in branches)
		branches[i].grow();
}

// Branch:
function Branch(point, group) {
	this.point = point;
	this.vector = new Point(Math.random() * values.size, 0).rotate(
			Math.random() * 360);
	this.path = new Path();
	this.path.add(point);
	group.appendTop(this.path);
}

Branch.prototype.grow = function() {
	this.vector = this.vector.transform(new Matrix().rotate(
			(Math.random() - 0.5) * 30));
	this.point += this.vector;
	this.path.add(this.point);
};

Branch.prototype.finish = function() {
	this.path.pointsToCurves();
};

////////////////////////////////////////////////////////////////////////////////
// Interface

var components = {
	size: {
		label: 'Size',
		range: [0, 100],
		steppers: true
	},
	max: {
		label: 'Max',
		range: [1, 500],
		steppers: true
	}
};

var palette = new Palette('Weed', components, values);
