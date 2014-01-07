function cellToJson(cell) {
  var json = new Object();
  json.id = cell.id;
  json.content = cell.editor.getSession().getValue();
  json.extra = new Object();
  switch(cell.mode) {
    case "heading1": json.format = cell.mode; break;
    case "heading2": json.format = cell.mode; break;
    case "heading3": json.format = cell.mode; break;
    case "heading4": json.format = cell.mode; break;
    case "heading5": json.format = cell.mode; break;
    case "scala": json.format = cell.mode;
         json.extra["output"] = cell.renderDisplay.text(); break;
    case "latex": json.format = cell.mode;
         json.extra["surroundWithAlign"] = "true"; break;
    case "markdown": json.format = cell.mode; break;
  }
  return json;
}

function docToJson(doc) {
  var returnDoc = new Object();
  returnDoc.name = doc.name;
  returnDoc.cells = new Array();
  for (var id in doc.ids){
    if (doc.cells.hasOwnProperty(id)) {
      returnDoc.cells.push(cellToJson(doc.cells[id]));
    }
  }
  return returnDoc;
}

function newDoc(name) {
    var doc = new Object();
    doc.numCells = 0;
    doc.name = name;
    doc.cells = new Object();
    doc.ids = new Array();
    return doc;
}
