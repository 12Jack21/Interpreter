$(document).ready(function () {
    // preNumber();

    $('textarea').on('input change', function () {
        //显示隐藏发送按钮
        var text = $(this).val();

        //动态改变文本框的高度
        var arr = text.split('\n'),
            len = arr.length;
        if (len > 5) {
            $(this).attr("rows", len);
        } else {
            $(this).attr("rows", 5);
        }
    });

    $("#upload").click(function (e) {
        e.preventDefault();
        let formData = new FormData($('#uploadForm')[0]);
        console.log(formData);
        if (formData == null) {
            alert("Please select file to upload!");
            return;
        }

        let a = $('#uploadForm');
        console.log(a);
        $.ajax({
            type: 'post',
            url: "upload", //relative path
            data: formData,
            cache: false,
            processData: false,
            contentType: false,
        }).done(function (data) {
            let text_area = $("#code_area");
            // update code card
            text_area.text(data); // fill the text area with codes
            let arr = data.split('\n'),
                len = arr.length;
            if (len >= 5) {
                text_area.attr("rows", len + 1);
            } else {
                text_area.attr("rows", 5);
            }
        }).fail(function () {
            alert("上传失败");
        }).always(function () {

        });

    });

    /*
    * Lexical, Grammar, Semantic Result handle here
    * */
    $("#analyze").click(function (e) {
        let index = $("#index-picker").val();
        let code = $("#code_area").val();
        let code_seg = code.split("-----");

        let scanInputs = $("#input_area").val();
        if (code_seg.length === undefined || code_seg.length === 0) {
            alert("Please input the program!");
            return;
        } else if (code_seg.length < index) {
            alert("Illegal index of program!");
            return;
        }

        $.ajax({
            type: 'Post',
            url: "analyze", //relative path
            data: {
                codes: code,
                index: index,
                scans: scanInputs
            },
            dataType: 'json', //expected return json format data
            cache: false,
            /* contentType and processData should set to false or the data cannot pass to the server*/
            traditional: true,
        }).done(function (data) {
            console.log("Wrapper: ", data);
            // fill lexical result panel
            showLexiResult(data.lexiResult);

            let treeData = obj2treeview(data.astNode, columnStructure);
            console.log("Column: ", treeData);

            // fill the tree with data
            let tree = $("#menuTree");
            tree.treeview({
                data: treeData,// 树形菜单数据
                emptyIcon: "icon-circle",
                enableLinks: false,
                levels: 1,// 展开层级
                backColor: "transparent",// 背景
                color: "#454545",// 文本颜色
                selectable: true,
                //设置展开、选中等操作
                state: {
                    checked: true,
                    disabled: true,
                    expanded: true,
                    selected: true
                },
                selectedBackColor: "rgba(186,186,184,0.22)",// 选中时的背景色
                selectedColor: '', //选中时的文本颜色
                onHoverColor: "rgba(186,186,184,0.22)",// hover时的颜色
                showBorder: false,
                expandIcon: 'fa fa-angle-right',     // 展开图标
                collapseIcon: 'fa fa-angle-down',  // 收缩图标

                onNodeSelected: function (event, data) {// 选中事件
                    if (data && data.href) {
                        // 打开相应的页面，内嵌iframe的方式
                        $('#currentPage').attr('src', data.href)
                    }
                    // 重置收缩展开
                    if (data.nodes != null) {
                        var select_node = $('#menuTree').treeview('getSelected');
                        if (select_node[0].state.expanded) {
                            $('#menuTree').treeview('collapseNode', select_node);
                            select_node[0].state.selected = false;
                        } else {
                            $('#menuTree').treeview('expandNode', select_node);
                            select_node[0].state.selected = false;
                        }
                    }
                }
            });
            // expand all the nodes
            tree.treeview('expandAll', {
                silent: true
            });

            let errorList = data.errors;
            // 语法错误消息展示
            alertGramError(errorList);
            // 语义消息填充
            showSemanticMsg(data.messages, data.outputList);
            console.log("Messages:", data.messages);
            console.log("Outputs:", data.outputList);

        }).fail(function () {
            alert("Upload code failed!");
        }).always(function () {

        });
    });

});

function showSemanticMsg(msgList, outList) {
    let debug = $("#debug_area");
    let out = $("#output_area");
    let inner = "";
    $.each(msgList, function (key, value) {
        inner += value + "\n\r";
    });
    debug.text(inner);
    let len = msgList.length;
    if (len >= 5)
        debug.attr("rows", len + 1);
    else
        debug.attr("rows", 5);

    inner = "";
    $.each(outList, function (key, value) {
        inner += value + " "; // 不用加 换行，保留原本的样子
    });
    out.text(inner);
    len = inner.split("\n").length;
    if (len >= 5)
        out.attr("rows", len + 1);
    else
        out.attr("rows", 5);
}

// function showSemanticMsg(msgList,outList) {
//     let semantic_span = $("#semantic span");
//     let inner = "";
//     $.each(msgList,function (key,value) {
//         inner += value + "\n\r";
//     });
//     semantic_span.text(inner);
// }
function showLexiResult(result) {
    let list = result.split("\n");
    let lexical = $("#lexical span");
    let inner = "";
    $.each(list, function (key, token) {
        inner += token + "\n\r";
    });
    lexical.text(inner);
}

function alertGramError(errorList) {
    let alert = $("#gramAlert");
    if (errorList.length === 0) {
        alert.addClass('alert-success').removeClass('alert-danger').text("Grammar parse succeed !");
    } else {
        let alert = $("#gramAlert");
        let inner = "";
        $.each(errorList, function (key, value) {
            inner += value + "</br>";
        });
        alert.html(inner);
        alert.addClass('alert-danger').removeClass('alert-success');
    }
}

var columnStructure = [{text: "name", nodes: "children"}];//外来数据转化为treeView数据的转化结构
loopLevel = 0;

function obj2treeview(resp, structure) {
    let nodeArray = [];
    let i = 0;
    if (resp.length === undefined)
        resp = [resp];
    let textStr = structure[0].text;
    let nodeStr = structure[0].nodes;
    for (i = 0; i < resp.length; i++) {
        let treeViewNodeObj;
        let subNode;
        if (resp[i] == null)
            continue;
        if (resp[i][nodeStr] !== undefined && resp[i][nodeStr].length !== 0) {
            loopLevel++;
            subNode = obj2treeview(resp[i][nodeStr], structure);
            loopLevel--;
        }
        if (subNode != undefined) {
            treeViewNodeObj = {
                text: resp[i][textStr],
                nodes: subNode
            };
        } else {
            treeViewNodeObj = {
                text: resp[i][textStr]
            };
        }
        nodeArray.push(treeViewNodeObj);
    }
    return nodeArray
}

// add line number
function preNumber() {
    var num = 10;
    var str = "";
    for (let i = 1; i < num + 1; i++) {
        str += "<li style='color: rgb(153, 153, 153);'>" + i + "</li>"
    }
    $(".pre-number").append(str);
}


