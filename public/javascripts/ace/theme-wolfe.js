/* ***** BEGIN LICENSE BLOCK *****
 * Distributed under the BSD license:
 * 
 * Copyright 2011 Irakli Gozalishvili. All rights reserved.
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 * ***** END LICENSE BLOCK ***** */

ace.define('ace/theme/wolfe', ['require', 'exports', 'module' , 'ace/lib/dom'], function(require, exports, module) {

exports.isDark = true;
exports.cssClass = "ace-wolfe";
exports.cssText = ".ace-wolfe .ace_gutter {\
background: #3d3d3d;\
color: #595959;\
//border-right: 1px solid #282828;\
}\
.ace-wolfe .ace_gutter-cell.ace_warning {\
background-image: none;\
background: #FC0;\
border-left: none;\
padding-left: 0;\
color: #000;\
}\
.ace-wolfe .ace_gutter-cell.ace_error {\
background-position: -6px center;\
background-image: none;\
background: #F10;\
border-left: none;\
padding-left: 0;\
color: #000;\
}\
.ace-wolfe .ace_print-margin {\
border-left: 1px solid #555;\
right: 0;\
background: #3d3d3d;\
}\
.ace-wolfe {\
text-shadow: none;\
border-radius: 5px 5px 5px 5px;\
box-shadow: 0 1px 1px rgba(0,0,0,.3);\
background-color: #3d3d3d;\
color: #E6E1DC;\
//padding: 20px;\
margin: 0 0px 20px 20px;\
line-height: 1.5em;\
font-size: 14px;\
font-family: Menlo, Consolas, \"Courier New\", Courier, \"Liberation Mono\", monospace;\
}\
.ace-wolfe .ace_cursor {\
border-left: 2px solid #FFFFFF;\
}\
.ace-wolfe .ace_cursor.ace_overwrite {\
border-left: 0px;\
border-bottom: 1px solid #FFFFFF;\
}\
.ace-wolfe .ace_marker-layer .ace_selection {\
background: #494836;\
}\
.ace-wolfe .ace_marker-layer .ace_step {\
background: rgb(198, 219, 174);\
}\
.ace-wolfe .ace_marker-layer .ace_bracket {\
margin: -1px 0 0 -1px;\
border: 1px solid #FCE94F;\
}\
.ace-wolfe .ace_marker-layer .ace_active-line {\
background: #333;\
}\
.ace-wolfe .ace_gutter-active-line {\
background-color: #3d3d3d;\
}\
.ace-wolfe .ace_invisible {\
color: #404040;\
}\
.ace-wolfe .ace_keyword {\
color:#f0e68c;\
}\
.ace-wolfe .ace_keyword.ace_operator {\
color:#FF308F;\
}\
.ace-wolfe .ace_constant {\
color:#1EDAFB;\
}\
.ace-wolfe .ace_constant.ace_language {\
color:#FDC251;\
}\
.ace-wolfe .ace_constant.ace_library {\
color:#8DFF0A;\
}\
.ace-wolfe .ace_constant.ace_numeric {\
color:#58C554;\
}\
.ace-wolfe .ace_invalid {\
color:#FFFFFF;\
background-color:#990000;\
}\
.ace-wolfe .ace_invalid.ace_deprecated {\
color:#FFFFFF;\
background-color:#990000;\
}\
.ace-wolfe .ace_support {\
color: #999;\
}\
.ace-wolfe .ace_support.ace_function {\
color:#00AEEF;\
}\
.ace-wolfe .ace_function {\
color:#00AEEF;\
}\
.ace-wolfe .ace_string {\
color:#58C554;\
}\
.ace-wolfe .ace_comment {\
color:#555;\
font-style:italic;\
padding-bottom: 0px;\
}\
.ace-wolfe .ace_variable {\
color:#997744;\
}\
.ace-wolfe .ace_meta.ace_tag {\
color:#BE53E6;\
}\
.ace-wolfe .ace_entity.ace_other.ace_attribute-name {\
color:#FFFF89;\
}\
.ace-wolfe .ace_markup.ace_underline {\
text-decoration: underline;\
}\
.ace-wolfe .ace_fold-widget {\
text-align: center;\
}\
.ace-wolfe .ace_fold-widget:hover {\
color: #777;\
}\
.ace-wolfe .ace_fold-widget.ace_start,\
.ace-wolfe .ace_fold-widget.ace_end,\
.ace-wolfe .ace_fold-widget.ace_closed{\
background: none;\
border: none;\
box-shadow: none;\
}\
.ace-wolfe .ace_fold-widget.ace_start:after {\
content: '▾'\
}\
.ace-wolfe .ace_fold-widget.ace_end:after {\
content: '▴'\
}\
.ace-wolfe .ace_fold-widget.ace_closed:after {\
content: '‣'\
}\
.ace-wolfe .ace_indent-guide {\
border-right:1px dotted #333;\
margin-right:-1px;\
}\
.ace-wolfe .ace_fold { \
background: #222; \
border-radius: 3px; \
color: #7AF; \
border: none; \
}\
.ace-wolfe .ace_fold:hover {\
background: #CCC; \
color: #000;\
}\
";

var dom = require("../lib/dom");
dom.importCssString(exports.cssText, exports.cssClass);

});