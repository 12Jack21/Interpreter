package org.john.interpreter.Service.ExecUtils;

import org.john.interpreter.Service.SemanticUtils.ArrayTable;
import org.john.interpreter.Service.SemanticUtils.ArrayVariable;
import org.john.interpreter.Service.SemanticUtils.SimpleTable;
import org.john.interpreter.Service.SemanticUtils.SimpleVariable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Translator {

    private List<String> messages = new LinkedList<>();
    private int level; // 当前作用域
    private SimpleTable simpleTable;
    private ArrayTable arrayTable;
    private String msg = "";
    private String result = ""; // unknown

    public Translator() {
        simpleTable = new SimpleTable();
        arrayTable = new ArrayTable();
        level = 1;
    }

    private String[] nonsenseNT = {"Pro"};

    // 先序遍历 AST，递归调用，目的：输出遍历过程中分析得出的信息，存到messages中
    // 先假设语法分析已经全部通过
    public void translate(ASTNode root) {
        String name = root.getName();
        if (name.equals("Pro")) {
            for (int i = 0; i < root.getMaxChildNum(); i++) {
                translate(root.getChildren()[i]);
            }
        } else if (name.equals("Statement")) {
            for (int i = 0; i < root.getMaxChildNum(); i++) {
                translate(root.getChildren()[i]);
            }
        } else if (name.equals("Declare")) {
            // int, real, char, void
            String type = root.getChildren()[0].getChildren()[0].getName(); //可以作为非局部变量保存起来，语句结束后清除
            String identifier = root.getChildren()[1].getChildren()[0].getValue();
            ASTNode F = root.getChildren()[1].getChildren()[1];
            if (!F.getChildren()[0].getName().equals("(")) {
                // 不是函数声明
                ASTNode index_node = F.getChildren()[0]; // Index
                if (index_node.getMaxChildNum() == 0) {
                    // 声明了一个简单变量
                    ASTNode X_node = F.getChildren()[1];
                    if (X_node.getMaxChildNum() == 0) {
                        // 添加变量到 变量符号表中
                        if (!simpleTable.addVariable(new SimpleVariable(identifier, type, null, level)))
                            messages.add("变量" + identifier + "已被声明过！");
                        else
                            messages.add("变量" + identifier + "被声明为" + type + "型");
                    } else {
                        // X ->= O
                        ASTNode O_node = X_node.getChildren()[1];
                        if (O_node.getChildren()[0].getName().equals("{")) {
                            // 数组初始化 O->{ Y }
                            ASTNode Y_node = O_node.getChildren()[1];
                            if (Y_node.getMaxChildNum() == 0) {
                                // 数组声明为空

                            } else {
                                // 数组里的值
                                ArrayList<SimpleVariable> array_values = translateY(Y_node);
                                boolean matchType = true;
                                for (SimpleVariable variable : array_values) {
                                    if (!variable.getType().equals(type)) {
                                        matchType = false;
                                        break;
                                    }
                                }
                                if (matchType) {
                                    ArrayVariable arrayVariable = new ArrayVariable(identifier, type,
                                            array_values.size(), convertArray(array_values,type),level);
                                    if (arrayTable.addVariable(arrayVariable))
                                        messages.add("数组变量" + identifier + "被声明为"+type+
                                                "型并被初始化为 "+arrayVariable.getValues().toString());
                                    else
                                        messages.add("数组变量" + identifier+"已被声明过！");
                                }else
                                    messages.add("数组中值的类型与声明的类型" + type + "不一致，无法赋值给变量");
                            }
                        } else {
                            // O->Relation
                            ASTNode relation = O_node.getChildren()[0];
                            // 此处只有 type和 value是有意义的
                            SimpleVariable relation_value = translateExp(relation);
                            if (!type.equals(relation_value.getType())) {
                                messages.add("类型不匹配，无法赋值给变量 " + identifier);
                            } else {
                                relation_value.setName(identifier);
                                if (!simpleTable.addVariable(relation_value))
                                    messages.add("变量" + identifier + "已被声明过");
                                else
                                    messages.add("变量" + identifier + "被声明为" + type +
                                            "型并初始化为" + relation_value.getValue());
                            }
                        }
                    }
                    ASTNode C_node = F.getChildren()[2];
                    //TODO 多变量声明如何递归处理


                } else {
                    // 声明了一个数组变量
                    ASTNode relation = index_node.getChildren()[1];
                    SimpleVariable array_length = translateExp(relation);
                }

            }
        }
    }

    // when Y->Relation C', C'->, Y
    private ArrayList<SimpleVariable> translateY(ASTNode Y) {
        ArrayList<SimpleVariable> variables = new ArrayList<>();
        ASTNode relation = Y.getChildren()[0];
        SimpleVariable relation_value = translateExp(relation);
        variables.add(relation_value);

        ASTNode C_ = Y.getChildren()[1];
        if (C_.getMaxChildNum() != 0) {
            variables.addAll(translateY(C_.getChildren()[1]));
        }

        return variables;
    }

    // TODO 借助符号栈和 数值（变量）栈进行分析,期间可以 报错，返回匿名变量(type,value)
    private SimpleVariable translateExp(ASTNode exp) {


        return null;
    }

    //TODO 把变量列表转成 Value的String列表，可以检查变量类型是否匹配，能否自动转换，并转换原始的值
    private ArrayList<String> convertArray(ArrayList<SimpleVariable> arrayList, String type) {
        // 这里数组里值的类型都是 match type 的
        ArrayList<String> list = new ArrayList<>();
        for (int i=0;i<arrayList.size();i++){
            list.add(arrayList.get(i).getValue());
        }
        return list;
    }

}
