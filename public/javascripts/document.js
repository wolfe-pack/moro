function cellToJson(cell) {
  var json = new Object();
  json.id = cell.id;
  json.format = cell.mode;
  json.content = cell.editor.getSession().getValue();
  json.extra = new Object();
  switch(cell.mode) {
    case "scala":
        json.extra["output"] = cell.renderDisplay.text()
        break;
    case "markdown":
        break;
    case "latex":
        json.extra["surroundWithAlign"] = "true"
        break;
  }
  return json;
}

function docToJson(name, cells) {
  var doc = new Object();
  doc.name = name;
  doc.cells = new Array();
  for (var id in cells){
    if (cells.hasOwnProperty(id)) {
      doc.cells.push(cellToJson(cells[id]));
    }
  }
  return doc;
}