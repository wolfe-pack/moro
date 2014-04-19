function compileCode (input, usage, mode) {
    $.ajax({
       type: "POST",
       contentType: "application/json",
       url: '/compiler/' + mode,
       data: JSON.stringify(input),
       dataType: "json",
       success: usage,
       error: function(j, t, e) {}
    });
};

var heightUpdateFunction = function(editor, id) {
    // http://stackoverflow.com/questions/11584061/
    //console.log(id);
    //console.log(editor);
    var newHeight =
              editor.getSession().getScreenLength()
              * editor.renderer.lineHeight
              + editor.renderer.scrollBar.getWidth();

    $(id).height(newHeight.toString() + "px");
    // This call is required for the editor to fix all of
    // its inner structure for adapting to a change in size
    editor.resize();
};

var wolfeHeightUpdateFunction = function(editor, id) {
    // http://stackoverflow.com/questions/11584061/
    //console.log(id);
    //console.log(editor);
    //todo: this is a hack due to lineHeight not being properly set at the time of the first call to heightUpdate.
    var newHeight =
              editor.getSession().getScreenLength()
              * 15 * 1.3  //font size in pixels times line height
              + 20; //padding?

//    console.log(editor.getSession().getScreenLength())
//    console.log(editor.renderer.scrollBar.getWidth())
//    console.log(editor.renderer.lineHeight)
//    console.log(newHeight)

    $(id).height(newHeight.toString() + "px");
    // This call is required for the editor to fix all of
    // its inner structure for adapting to a change in size
    editor.resize();
};

function outputResult(doc, id, result, compilers) {
      switch(result.format) {
        case "html": doc.cells[id].renderDisplay.html(result.result); break;
        case "string": doc.cells[id].renderDisplay.html("<pre>" + result.result + "</pre>"); break;
        case "wolfe": doc.cells[id].renderDisplay.html("<div class=\"wolfe-result\"><pre>" + result.result + "</pre></div>"); break;
      }
      if(compilers[doc.cells[id].mode].hideAfterCompile) $('#toggleEditor'+id).click();
      MathJax.Hub.Queue(["Typeset",MathJax.Hub,"renderDisplay"+id]);
      /*
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
      }*/
}


function runCode(doc, id, compilers) {
  var input = compilers[doc.cells[id].mode].editorToInput(doc, id);
  compileCode(input,
      function(x) {
        outputResult(doc, id, x, compilers);
      }, doc.cells[id].mode);
  /*
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
  }*/
}

function changeMode(id, newMode) {
   compilers[doc.cells[id].mode].removeEditor(id);
   doc.cells[id].mode = newMode;
   /*
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
   */
   //text = doc.cells[id].editor.getSession().getValue();
   doc.cells[id].editor = compilers[newMode].editor(id);
   doc.cells[id].editor.focus();
}

function newCellDiv(id) {
   return '<div id="cell' + id + '" class="cellWrapper" onmouseover="document.getElementById(\'sidebarCell' + id + '\').style.display = \'block\';" onmouseout="document.getElementById(\'sidebarCell' + id + '\').style.display = \'none\';">' +
   '<div id="editCell' + id + '" class="light-border">' +
   '  <div id="sidebarCell' + id + '" class="sidebarCell text-right" style="display: none;">' +
   '    <div class="btn-group btn-group-xs">' +
   '      <!--button id="moveAbove' + id + '" type="button" class="btn btn-default" onclick="moveCellAbove(doc,' + id + ',compilers)"><i class="fa fa-chevron-up"></i></button-->' +
   '      <button id="addAbove' + id + '" type="button" class="btn btn-default" onclick="addCellAbove(doc,' + id + ',compilers)"><i class="fa fa-sort-up"></i><i class="fa fa-plus"></i></button>' +
   '      <button id="toggleEditor' + id + '" type="button" class="btn btn-default edit-btn" onclick="toggleEditor(doc,' + id + ')"><i class="fa fa-pencil fa-fw"></i></button>' +
   '      <button id="remove' + id + '" type="button" class="btn btn-default" onclick="removeCell(doc,' + id + ')"><span class="fa fa-trash-o"></span></button>' +
   '      <button id="addBelow' + id + '" type="button" class="btn btn-default" onclick="addCellBelow(doc,' + id + ',compilers)"><i class="fa fa-plus"></i><i class="fa fa-sort-down"></i></button>' +
   '      <!--button id="moveBelow' + id + '" type="button" class="btn btn-default" onclick="moveCellBelow(doc,' + id + ',compilers)"><i class="fa fa-chevron-down"></i></button-->' +
   '    </div>' +
   '  </div>' +
   //'  cell ' + id + ' contents' +
   '  <div class="input">' +
   '    <div id="modeForm' + id + '" class="btn-group btn-group-xs" data-toggle="buttons">' + editorToolbar() +
   '    </div>' +
   '    <div id="editor' + id + '" class="cell light-border"></div>' +
   '      <button id="runCode' + id + '" type="button" class="btn btn-default btn-xs" onclick="runCode(doc, ' + id + ',compilers)"><span class="glyphicon glyphicon-play"></span></button>' +
   '  </div>' +
   '  <div id="renderDisplay' + id + '" class="cell"  ondblclick="toggleEditor(doc,' + id + ')"></div>' +
   '</div>' +
   '</div>'
}

function makeCellFunctional(doc,id,compiler,compilers,initialContent) {
    doc.cells[id] = new Object();
    doc.cells[id].id = id;
    doc.cells[id].mode = compiler;
    doc.cells[id].renderDisplay = $('#renderDisplay' + id);

    doc.cells[id].editor = compilers[compiler].editor(id,initialContent);
    doc.cells[id].showEditor = true;

    $('.btn').button();

    var sz = $('#modeForm'+id+' label');
    for (var i=0, len=sz.length; i<len; i++) {
        sz[i].onclick = function() {
            newMode = this.querySelector('input').value;
            //console.log(doc.cells[id].mode, newMode);
            if(doc.cells[id].mode != newMode) {
              changeMode(id, newMode);
            }
        };
    }
}

function addCellFromJson(doc,format,content,compilers) {
  $( "#cells" ).append(newCellDiv(doc.numCells));
  doc.ids.push(doc.numCells);
  makeCellFunctional(doc,doc.numCells,format,compilers,content);
  //doc.cells[doc.numCells].editor.getSession().setValue(content);
  $('#modeForm'+ doc.numCells +' label input[value='+ format +']').parent().click()
  //runCode(doc, doc.numCells, compilers);
  doc.numCells += 1;
}

function addCell(doc,compilers) {
  $( "#cells" ).append(newCellDiv(doc.numCells));
  doc.ids.push(doc.numCells);
  makeCellFunctional(doc,doc.numCells, "scala",compilers,"");
  doc.numCells += 1;
}

function addCellAbove(doc,id,compilers) {
  console.log("TODO: adding above " + id);
  $( "#cell"+id ).before(newCellDiv(doc.numCells));
  doc.ids.splice(doc.ids.indexOf(id),0,doc.numCells);
  makeCellFunctional(doc,doc.numCells, "scala",compilers,"");
  doc.numCells += 1;
}

function addCellBelow(doc,id,compilers) {
  console.log("TODO: adding below " + id);
  $( "#cell"+id ).after(newCellDiv(doc.numCells));
  doc.ids.splice(doc.ids.indexOf(id)+1,0,doc.numCells);
  makeCellFunctional(doc,doc.numCells, "scala",compilers,"");
  doc.numCells += 1;
}

function moveCellAbove(doc,id,compilers) {
  console.log("TODO: move cell up " + id);
  //$( "#cell"+id ).before(newCellDiv(doc.numCells));
  //doc.ids.splice(doc.ids.indexOf(id),0,doc.numCells);
  //makeCellFunctional(doc,doc.numCells, "scala",compilers);
  //doc.numCells += 1;
}

function moveCellBelow(doc,id,compilers) {
  console.log("TODO: move cell down " + id);
  //$( "#cell"+id ).after(newCellDiv(doc.numCells));
  //doc.ids.splice(doc.ids.indexOf(id)+1,0,doc.numCells);
  //makeCellFunctional(doc,doc.numCells, "scala",compilers);
  //doc.numCells += 1;
}

function toggleEditor(doc,id) {
  doc.cells[id].showEditor = !doc.cells[id].showEditor;
  if(doc.cells[id].showEditor) {
    // console.log("showing editor" + id);
    $('#editCell' + id + ' .input').show();
  } else {
    // console.log("hiding editor" + id);
    $('#editCell' + id + ' .input').hide();
  }
}

function removeCell(doc,id) {
  console.log("removing " + id);
  $('#cell' + id).remove();
  delete doc.cells[id];
}

function saveDoc(doc, compilers) {
  console.log("saving doc to " + $('#saveAsInput')[0].value);
  var d = docToJson(doc,compilers);
  console.log(d);
  $.ajax({
       type: "POST",
       contentType: "application/json",
       url: '/doc/save/' + $('#saveAsInput')[0].value,
       data: JSON.stringify(d),
       success: function(d) {
         bootstrap_alert("success", "Success", "Saved to " + $('#saveAsInput')[0].value, 2000);
         console.log(d);
       },
       error: function(j,t,e) {
         bootstrap_alert("danger", "Failed", JSON.stringify(e), 5000);
         console.log(d);
       }
    });
}

function runAll(doc, compilers) {
  for (var id in doc.ids){
    if (doc.cells.hasOwnProperty(id)) {
      runCode(doc, id, compilers);
    }
  }
}
