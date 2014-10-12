function cellConfigClicked(id, doc, compilers) {
  fillCellConfigDialog(id, doc, compilers)
  $('#cellConfigDialog').modal('show')
}

function fillCellConfigDialog(id, doc, compilers) {
  var cellConfigId = "cellConfigDialogContent"
  var div = $('#'+cellConfigId)
  $('#'+cellConfigId).empty()
  var headerDiv =
  '<div class="modal-header">' +
  '    <a href="#" class="close" data-dismiss="modal" aria-hidden="true">&times;</a>' +
  '    <h3 class="modal-title">Cell '+id+' Config:</h3>' +
  '</div>';
  div.append(headerDiv);
  var bodyDiv =
    '<div class="modal-body">' +
    '  <form class="form-horizontal" role="form">';
  var compiler = compilers[doc.cells[id].mode];
  for(var cidx in compiler.config) {
    var ce = compiler.config[cidx];
    var currentValue = ce.defaultValue;
    if(Object.prototype.hasOwnProperty.call(doc.cells[id].config, ce.key))
      currentValue = doc.cells[id].config[ce.key];
    var inputId = 'cellConfigInput_'+id+'_'+ce.key;
    bodyDiv = bodyDiv + '<div class="form-group">';
    bodyDiv = bodyDiv + '    <label for="'+inputId+'" class="col-sm-2 control-label">' + ce.label + '</label>';
    bodyDiv = bodyDiv + '    <div class="col-sm-10"><input id="'+inputId+'" type="'+ce.inputType+'" class="form-control"/ value="'+currentValue+'">';
    bodyDiv = bodyDiv + '    <span class="help-block">' + ce.description + '</span></div>';
    bodyDiv = bodyDiv + '</div>';
  }
  bodyDiv = bodyDiv + '  </form>' + '</div>';
  div.append(bodyDiv);
  var footerDiv =
    '<div class="modal-footer">' +
    '    <a href="#" class="btn btn-default" data-dismiss="modal">Cancel</a>' +
    '    <a href="#" class="btn btn-primary" onclick="cellConfigOkClicked('+id+', doc);">OK</a>' +
    '</div>';
  div.append(footerDiv);
}

function cellConfigOkClicked(id, doc) {
  doc.cells[id].config = {}
  var compiler = compilers[doc.cells[id].mode];
  for(var cidx in compiler.config) {
      var ce = compiler.config[cidx];
      var inputId = 'cellConfigInput_'+id+'_'+ce.key;
      var configValue = $('#'+inputId)[0].value;
      if(configValue != ce.defaultValue)
        doc.cells[id].config[ce.key] = configValue;
      else doc.cells[id].config[ce.key];
  }
  $('#cellConfigDialog').modal('hide');
}