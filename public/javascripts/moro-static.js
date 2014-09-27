function makeStaticCellFunctional(doc,id,compiler,compilers) {
    doc.cells[id] = new Object();
    doc.cells[id].id = id;
    doc.cells[id].mode = compiler;
    doc.cells[id].renderDisplay = $('#renderDisplay' + id);
    $('.btn').button();
}

function addStaticCellFromJson(id,doc,mode,input,compilers) {
  doc.ids.push(id);
  makeStaticCellFunctional(doc,id,mode,compilers);
  if(!compilers[mode].hideAfterCompile) {
    $('#editCell' + id).show();
    doc.cells[id].editor = compilers[mode].editor(id);
    doc.cells[id].editor.getSession().setValue(input.code);
  } else {
    toggleEditor(doc,id);
  }
}

function compileStaticCell(id,doc,mode,input,compilers) {
  var compiler = compilers[mode];
  if(compiler.aggregate) {
    var prefixInput = "";
    for(var midx in doc.ids) {
      var mid = doc.ids[midx];
      if(doc.cells[mid].mode == mode) {
        if(id == mid) break;
        prefixInput = prefixInput + compiler.editorToInput(doc, doc.cells[mid].id).code + "\n";
      }
    }
    input.code = prefixInput + input.code;
  }
  compileCode(input,
      function(x) {
        outputResult(doc, id, x, compilers);
      }, doc.cells[id].mode);
}
