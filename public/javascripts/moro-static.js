/*
function makeStaticCellFunctional(doc,id,mode,compilers) {
    doc.cells[id] = new Object();
    doc.cells[id].id = id;
    doc.cells[id].mode = mode;
    doc.cells[id].config = {}
    doc.cells[id].renderDisplay = $('#renderDisplay' + id);
    $('.btn').button();
}

function addStaticCellFromJson(id,doc,mode,input,compilers) {
  doc.ids.push(id);
  makeStaticCellFunctional(doc,id,mode,compilers);
  doc.cells[id].config = input.extraFields;
  if(!compilers[mode].hideAfterCompile) {
    $('#editCell' + id).show();
    doc.cells[id].editor = compilers[mode].editor(id);
    doc.cells[id].editor.getSession().setValue(input.code);
  } else {
    toggleEditor(doc,id);
  }
}
*/

function compileStaticCell(id,doc,mode,input,compilers) {
  var compiler = compilers[mode];
  if(compiler.aggregate) {
    var aggregatedCells = new Array();
    for(var midx in doc.ids) {
      var mid = doc.ids[midx];
      if(doc.cells[mid].mode == mode) {
        if(id == mid) break;
        // code
        aggregatedCells.push(compiler.editorToInput(doc, doc.cells[mid].id).code);
      }
    }
    input.extraFields['aggregatedCells'] = JSON.stringify(aggregatedCells);
  }
  compileCode(input,
      function(x) {
        outputResult(doc, id, x, compilers);
      }, doc.cells[id].mode);
}

function createStaticCellHTML(id,section,doc,mode,input,compilers) {
  input.sessionId = doc.guid;
  delete input['outputFormat'];
  if(input.extraFields == null)
    input.extraFields = {}
  var cellDiv = document.createElement('div');
  cellDiv.id = 'cell'+id;
  cellDiv.className = 'cellWrapper';
  if(input.extraFields.hasOwnProperty('fragment') && input.extraFields.fragment === 'true')
    cellDiv.className += ' fragment';
  if(input.extraFields.hasOwnProperty('hide'))
    cellDiv.setAttribute('hidden', input.extraFields.hide)
  var createEditor = !compilers[mode].hideAfterCompile;
  if(input.extraFields.hasOwnProperty('showEditor') && input.extraFields.showEditor === 'true')
    createEditor = true
  if(createEditor) {
    // edit cell
    var editCellDiv = document.createElement('div');
    editCellDiv.id = 'editCell'+id;
    editCellDiv.className = 'cell';
    editCellDiv.setAttribute('hidden' , 'true');
    var inputDiv = document.createElement('div');
    inputDiv.className = "input";
    $(inputDiv).append('<a id="runCode'+id+'" type="button" class="runButton" onclick="runCode(doc, '+id+', compilers)"><i class="fa fa-play-circle-o fa-2x"></i></span></a>');
    var editorCellDiv = document.createElement('div');
    editorCellDiv.id = "editor" + id
    editorCellDiv.className = 'cell light-border editor';
    $(editorCellDiv).html(input.code);
    $(inputDiv).append(editorCellDiv);
    $(editCellDiv).append(inputDiv);
    $(cellDiv).append(editCellDiv);
  }
  $(cellDiv).append('<div id="renderDisplay'+id+'" class="cell">Loading...</div>');
  section.append(cellDiv);

  // make functional
  doc.ids.push(id);
  doc.cells[id] = new Object();
  doc.cells[id].id = id;
  doc.cells[id].mode = mode;
  doc.cells[id].renderDisplay = $('#renderDisplay' + id);
  doc.cells[id].input = input;
  doc.cells[id].config = input.extraFields;
  if(createEditor) {
    $('.btn').button();
    $('#editCell' + id).show();
    doc.cells[id].editor = compilers[mode].editor(id, input.code);
  }
  compileStaticCell(id, doc, mode, input, compilers);
}
