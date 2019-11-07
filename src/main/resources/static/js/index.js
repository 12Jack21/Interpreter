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
        let code = $("#code_area").val();
        $.ajax({
            type: 'Post',
            url: "analyze", //relative path
            data: {
                codes: code
            },
            dataType: 'json', //expected return json format data
            cache: false,
            /* contentType and processData should set to false or the data cannot pass to the server*/
            traditional: true,
        }).done(function (data) {
            console.log("Wrapper: ",data);
            // fill lexical result panel
            showLexiResult(data.lexiResult);

            let treeData = obj2treeview(data.astNode, columnStructure);
            console.log("Column: ",treeData);

            // fill the tree with data
            $('#menuTree').treeview({
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
                onhoverColor: "rgba(186,186,184,0.22)",// hover时的颜色
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

            let errorList = data.errors;
            console.log("Errors:",errorList);
            alertGramError(errorList);

        }).fail(function () {
            alert("upload code failed!");
        }).always(function () {

        });
    });

});

function showLexiResult(result) {
    let list = result.split("\n");
    let lexical = $("#lexical span");
    let inner = "";
    $.each(list,function (key,token) {
        inner += token + "\n\r";
    });
    lexical.text(inner);
}

function alertGramError(errorList) {

    let alert = $("#gramAlert");
    if (errorList.length === 0)
        alert.addClass('alert-success').removeClass('alert-danger').text("Grammar parse succeed !");
    else {
        let inner = "";
        $.each(errorList,function (key, value) {
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


// // 设置初始化时展开的节点
// $('#menuTree').treeview('expandNode', [0, {
//     levels: 2,
//     silent: true
// }]);