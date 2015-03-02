function nextEditor(doc, id) {
  pos = doc.ids.indexOf(id);
  // TODO find next undeleted one
  //console.log("next editor at position: " + (pos+1) + " with id: " + doc.ids[pos+1]);
  return doc.cells[doc.ids[pos+1]].editor;
}

function prevEditor(doc, id) {
  pos = doc.ids.indexOf(id);
  // TODO find prev undeleted one
  //console.log("prev editor at position: " + (pos-1) + " with id: " + doc.ids[pos-1]);
  return doc.cells[doc.ids[pos-1]].editor;
}