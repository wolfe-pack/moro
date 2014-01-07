function compileCode (codeStr, usage, mode) {
    $.ajax({
       type: "POST",
       contentType: "application/json",
       url: '/compiler/' + mode,
       data: JSON.stringify({ code: codeStr }),
       dataType: "json",
       success: usage,
       error: function(j, t, e) {}
    });
};

var heightUpdateFunction = function(editor, id) {
    // http://stackoverflow.com/questions/11584061/
    var newHeight =
              editor.getSession().getScreenLength()
              * editor.renderer.lineHeight
              + editor.renderer.scrollBar.getWidth();

    $(id).height(newHeight.toString() + "px");
    // This call is required for the editor to fix all of
    // its inner structure for adapting to a change in size
    editor.resize();
};

function outputResult(doc, id, result) {
      doc.cells[id].renderDisplay.html(result.replace(/\n/g , "<br>"));
      // post compiling work
      switch(doc.cells[id].mode) {
        case "scala": break;
        case "markdown":
            $('#toggleEditor'+id).click();
            MathJax.Hub.Queue(["Typeset",MathJax.Hub,"renderDisplay"+id]);
            break;
        case "latex":
            $('#toggleEditor'+id).click();
            MathJax.Hub.Queue(["Typeset",MathJax.Hub,"renderDisplay"+id]);
            break;
        case "heading1":
            $('#toggleEditor'+id).click();
            break;
        case "heading2":
            $('#toggleEditor'+id).click();
            break;
        case "heading3":
            $('#toggleEditor'+id).click();
            break;
        case "heading4":
            $('#toggleEditor'+id).click();
            break;
        case "heading5":
            $('#toggleEditor'+id).click();
            break;
      }
}


function runCode(doc, id) {
  var code = doc.cells[id].editor.getSession().getValue();
  switch(doc.cells[id].mode) {
    case "heading1": outputResult(doc,id,"<h1>" + code + "</h1>"); break;
    case "heading2": outputResult(doc,id,"<h2>" + code + "</h2>"); break;
    case "heading3": outputResult(doc,id,"<h3>" + code + "</h3>"); break;
    case "heading4": outputResult(doc,id,"<h4>" + code + "</h4>"); break;
    case "heading5": outputResult(doc,id,"<h5>" + code + "</h5>"); break;
    default:
      compileCode(code,
        function(x) {
          outputResult(doc, id, x.result);
        }, doc.cells[id].mode); break;
  }
}

function changeMode(id, newMode) {
   doc.cells[id].mode = newMode;
      var editorMode = newMode;
      switch(newMode) {
        case "heading1": editorMode = "html"; break;
        case "heading2": editorMode = "html"; break;
        case "heading3": editorMode = "html"; break;
        case "heading4": editorMode = "html"; break;
        case "heading5": editorMode = "html"; break;
        default: break;
      }
   doc.cells[id].editor.getSession().setMode("ace/mode/" + editorMode);
   doc.cells[id].editor.focus();
   doc.cells[id].editor.navigateFileEnd();
}

function newCellDiv(id) {
   return '<div id="cell' + id + '" class="cellWrapper">' +
   '<div id="editCell' + id + '" class="editCell">' +
   //'  cell ' + id + ' contents' +
   '  <div class="input">' +
   '    <div id="modeForm' + id + '" class="btn-group btn-group-xs" data-toggle="buttons">' +
   '      <label class="btn btn-default active"><input type="radio" name="mode" value="scala"> Scala</label>' +
   '      <label class="btn btn-default"><input type="radio" name="mode" value="markdown"> Markdown</label>' +
   '      <label class="btn btn-default"><input type="radio" name="mode" value="latex"> Latex</label>' +
   '      <label class="btn btn-default"><input type="radio" name="mode" value="heading1"> <span class="glyphicon glyphicon-header">1</span></label>' +
   '      <label class="btn btn-default"><input type="radio" name="mode" value="heading2"> <span class="glyphicon glyphicon-header">2</span></label>' +
   '      <label class="btn btn-default"><input type="radio" name="mode" value="heading3"> <span class="glyphicon glyphicon-header">3</span></label>' +
   '      <label class="btn btn-default"><input type="radio" name="mode" value="heading4"> <span class="glyphicon glyphicon-header">4</span></label>' +
   '      <label class="btn btn-default"><input type="radio" name="mode" value="heading5"> <span class="glyphicon glyphicon-header">5</span></label>' +
   '    </div>' +
   '      <button id="runCode' + id + '" type="button" class="btn btn-default btn-xs" onclick="runCode(doc, ' + id + ')"><span class="glyphicon glyphicon-play"></span></button>' +
   '    <div id="editor' + id + '" class="cell light-border"></div>' +
   '  </div>' +
   '  <div id="renderDisplay' + id + '" class="cell"  ondblclick="toggleEditor(doc,' + id + ')"></div>' +
   '</div>' +
   '<div class="sidebarCell">' +
   '  <div class="btn-group btn-group-xs">' +
   '    <button id="addAbove' + id + '" type="button" class="btn btn-default" onclick="addCellAbove(doc,' + id + ')"><span class="glyphicon glyphicon-chevron-up"></span></button>' +
   '    <button id="toggleEditor' + id + '" type="button" class="btn btn-default edit-btn" data-toggle="button" onclick="toggleEditor(doc,' + id + ')"><span class="glyphicon glyphicon-pencil"></span></button>' +
   '    <button id="remove' + id + '" type="button" class="btn btn-default" onclick="removeCell(doc,' + id + ')"><span class="glyphicon glyphicon-minus-sign"></span></button>' +
   '    <button id="addBelow' + id + '" type="button" class="btn btn-default" onclick="addCellBelow(doc,' + id + ')"><span class="glyphicon glyphicon-chevron-down"></span></button>' +
   '  </div>' +
   '</div>' +
   '</div>'
}

function makeCellFunctional(doc,id) {
    doc.cells[id] = new Object();
    doc.cells[id].id = id;
    doc.cells[id].mode = "scala";
    doc.cells[id].renderDisplay = $('#renderDisplay' + id);

    doc.cells[id].editor = ace.edit("editor"+id);
    doc.cells[id].editor.setTheme("ace/theme/solarized_light");
    doc.cells[id].editor.getSession().setMode("ace/mode/scala");
    doc.cells[id].editor.focus();
    doc.cells[id].editor.navigateFileEnd();
    doc.cells[id].editor.setBehavioursEnabled(false);
    doc.cells[id].showEditor = true;

    heightUpdateFunction(doc.cells[id].editor, '#editor'+id);
    doc.cells[id].editor.getSession().on('change', function () {
        heightUpdateFunction(doc.cells[id].editor, '#editor'+id);
    });

    doc.cells[id].editor.commands.addCommand({
        name: "runCode",
        bindKey: {win: "Ctrl-Enter", mac: "Ctrl-Enter"},
        exec: function(editor) {
            document.getElementById("runCode"+id).click();
        }
    })

    $('.btn').button();

    var sz = $('#modeForm'+id+' label');
    for (var i=0, len=sz.length; i<len; i++) {
        sz[i].onclick = function() {
            newMode = this.querySelector('input').value;
            console.log(doc.cells[id].mode, newMode);
            if(doc.cells[id].mode != newMode) {
              changeMode(id, newMode);
            }
        };
    }
}

function addCellFromJson(doc,format,content,output) {
  $( "#cells" ).append(newCellDiv(doc.numCells));
  doc.ids.push(doc.numCells);
  makeCellFunctional(doc,doc.numCells);
  doc.cells[doc.numCells].editor.getSession().setValue(content);
  $('#modeForm'+ doc.numCells +' label input[value='+ format +']').parent().click()
  switch(format) {
    case "scala":
        doc.cells[doc.numCells].renderDisplay.text(output);
        break;
    default:
        //runCode(doc, doc.numCells);
        break;
  }
  doc.numCells += 1;
}

function addCell(doc) {
  $( "#cells" ).append(newCellDiv(doc.numCells));
  doc.ids.push(doc.numCells);
  makeCellFunctional(doc,doc.numCells);
  doc.numCells += 1;
}

function addCellAbove(doc,id) {
  console.log("TODO: adding above " + id);
  $( "#cell"+id ).before(newCellDiv(doc.numCells));
  doc.ids.splice(doc.ids.indexOf(id),0,doc.numCells);
  makeCellFunctional(doc,doc.numCells);
  doc.numCells += 1;
}

function addCellBelow(doc,id) {
  console.log("TODO: adding below " + id);
  $( "#cell"+id ).after(newCellDiv(doc.numCells));
  doc.ids.splice(doc.ids.indexOf(id)+1,0,doc.numCells);
  makeCellFunctional(doc,doc.numCells);
  doc.numCells += 1;
}

function toggleEditor(doc,id) {
  doc.cells[id].showEditor = !doc.cells[id].showEditor;
  if(doc.cells[id].showEditor) {
    console.log("showing editor" + id);
    $('#editCell' + id + ' .input').show();
  } else {
    console.log("hiding editor" + id);
    $('#editCell' + id + ' .input').hide();
  }
}

function removeCell(doc,id) {
  console.log("removing " + id);
  $('#cell' + id).remove();
  delete doc.cells[id];
}

function saveDoc(doc) {
  console.log("saving doc to " + $('#filePath')[0].value);
  var d = docToJson(doc);
  console.log(d);
  $.ajax({
       type: "POST",
       contentType: "application/json",
       url: '/doc/save/' + $('#filePath')[0].value,
       data: JSON.stringify(d),
       success: function(d) {
         console.log(d);
       }
    });
}

function runAll(doc) {
  for (var id in doc.ids){
    if (doc.cells.hasOwnProperty(id)) {
      runCode(doc, id);
    }
  }
}
