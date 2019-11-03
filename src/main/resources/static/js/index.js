$(document).ready(function () {
    // preNumber();


});

$("#upload").click(function (e) {
    e.preventDefault();
    let formData = new FormData($('#uploadForm')[0]);
    $.ajax({
        type: 'post',
        url: "upload", //relative path
        data: formData,
        cache: false,
        processData: false,
        contentType: false,
    }).success(function (data) {
        alert(data);
        // update code card
        $("#code_area").textContent = data; // fill the text area with codes

    }).error(function () {
        alert("上传失败");
    });

});

// 加上行号
function preNumber() {
    var num = 10;
    var str = "";
    for (let i = 1; i < num + 1; i++) {
        str += "<li style='color: rgb(153, 153, 153);'>" + i + "</li>"
    }
    $(".pre-number").append(str);
}

var treeData = [
    {
        text: "item1",
        icon: "fa fa-cube",
        nodes: [
            {
                text: "item1-1",
                icon: "fa fa-cube",
                href: "index1.html",
                state: {
                    checked: false,
                    disabled: false,
                    expanded: false,
                    selected: true
                }
            },
            {
                text: "item1-2",
                icon: "fa fa-cube",
                href: "index2.html",
                state: {
                    checked: false,
                    disabled: false,
                    expanded: false,
                    selected: false
                }
            }
        ]
    },
    {
        text: "item2",
        icon: "fa fa-cube",
        href: "index3.html",
        state: {
            checked: false,
            disabled: false,
            expanded: false,
            selected: false
        }
    }
];

//初始化
$('#menuTree').treeview({
    data: treeData,// 树形菜单数据
    emptyIcon: "icon-circle",
    // expandIcon:"glyphicon glyphicon-chevron-down",
    // collapseIcon:"glyphicon glyphicon-chevron-right",
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

// // 设置初始化时展开的节点
// $('#menuTree').treeview('expandNode', [0, {
//     levels: 2,
//     silent: true
// }]);