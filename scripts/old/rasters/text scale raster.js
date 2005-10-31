function getCompoundArea(obj, area) {    if (area == null) area = 0;    if (obj.type == "path") return area + obj.getArea();    else if (obj.type == "compound" || obj.type == "group") {        var child = obj.firstChild;        while (child != null) {            area = getCompoundArea(child, area);            child = child.nextSibling;        }    }    return area;}var raster = null;var text = null;sel = getSelected();for (var i = 0; i < sel.length; i++) {    obj = sel[i];    if (raster == null && obj.type == "raster") raster = obj;    else if (text == null && obj.type == "text") text = obj;    if (raster != null && text != null) break;}if (raster != null && text != null) {//    print(raster.width + " " + raster.height);//    a;    tmp = text.clone();    tmp.transform(new Matrix().translate(0, 100));    var stream = tmp.getStream("write");    stream.remove();    stream.close();    var texts = new Array();    // first create text objects for every chars:    for (var i = 0; i < 256; i++) {        var obj = tmp.clone();        var stream = tmp.getStream("write");        var chr = String.fromCharCode(i);        stream.write(chr);        stream.close();        texts[i] = obj;    }    tmp.remove();    // now update the document    documents[0].redraw();    // and get the outlines of the texts:    var chars = new Array();    var maxArea = 0;    for (var i = 0; i < texts.length; i++) {        var obj = texts[i].createOutlines();        texts[i].remove();        var area = getCompoundArea(obj);        if (area > maxArea) maxArea = area;        chars[i] = {area: area, chr: chr, obj: obj};    }     stream = text.getStream("read");    var group = new Art("group");    var size = 25, minSize = 0.1, maxSize = 1.2;    for (var y = 0; y < raster.height; y++) {        for (var x = 0; x < raster.width; x++) {            var col = raster.getPixel(x, y);            col.type = "gray";            if (stream.available() == 1) stream.seek(0);             chr = stream.read(1);            info = chars[chr.charCodeAt(0) + 1];            var obj = info.obj.clone();            var matrix = new Matrix();            var scale = maxArea / info.area * (minSize + col.gray * (maxSize - minSize));                        matrix.scale(scale, scale);            matrix.translate(x * size, (raster.height - y) * size);            obj.transform(matrix);            group.append(obj);        }        documents[0].redraw();    }    stream.close();    for (var i = 0; i < chars.length; i++) {        chars[i].obj.remove();    }}