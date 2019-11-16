package org.john.interpreter.Service.ExecUtils;

import org.john.interpreter.Service.SemanticUtils.ArrayTable;
import org.john.interpreter.Service.SemanticUtils.ArrayVariable;
import org.john.interpreter.Service.SemanticUtils.SimpleTable;
import org.john.interpreter.Service.SemanticUtils.SimpleVariable;

import java.util.ArrayList;
import java.util.HashMap;
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
                                            array_values.size(), convertArray(array_values, type), level);
                                    if (arrayTable.addVariable(arrayVariable))
                                        messages.add("数组变量" + identifier + "被声明为" + type +
                                                "型并被初始化为 " + arrayVariable.getValues().toString());
                                    else
                                        messages.add("数组变量" + identifier + "已被声明过！");
                                } else
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


    private SimpleVariable translateLogicExp(ASTNode logic) {
        SimpleVariable logic_val = null;


        return logic_val;
    }

    private SimpleVariable translateRelationExp(ASTNode relation) {
        SimpleVariable relation_val = null;


        return relation_val;
    }

    // 翻译 运算表达式 Arithmetic,处理类型错误和类型转换
    private SimpleVariable translateArithmeticExp(ASTNode arithmetic) {
        SimpleVariable arith_val = null;
        LinkedList<SimpleVariable> varStack = new LinkedList<>(); //变量栈
        LinkedList<String> symStack = new LinkedList<>(); //符号栈
        HashMap<String, Integer> prioMap = CodeTable.opPriority();

        // 起始变量
        ASTNode var = arithmetic.findNextNodeWithValueOrTip("Variable");
        SimpleVariable variable = translateVariable(var);
        varStack.addFirst(variable);
        ASTNode sym;
        // 没有下一个运算符号了 TODO 结束条件的选择，所有运算的token，数+符号+数+...+
        while ((sym = arithmetic.findNextNodeWithValueOrTip("symbol")) != null) {
            String sym_value = sym.getValue();
            if (symStack.size() > 0 && prioMap.get(sym_value) <= prioMap.get(symStack.get(0))) {
                // 栈顶符号的优先级更高或相等（左结合）
                SimpleVariable v2 = varStack.pop();
                SimpleVariable v1 = varStack.pop();
                String top = symStack.pop();
                varStack.addFirst(calculate(v1,v2,top));
            } else {
                // 栈顶符号的优先级更小,则新符号入栈
                symStack.addFirst(sym_value);
            }

            variable = translateVariable(arithmetic.findNextNodeWithValueOrTip("Variable"));
            varStack.addFirst(variable);
        }
        // 到这里已经没有符号再进栈了
        while (symStack.size() != 0){
            String top = symStack.pop();
            SimpleVariable v2 = varStack.pop();
            SimpleVariable v1 = varStack.pop();
            varStack.addFirst(calculate(v1,v2,top));
        }

        arith_val = varStack.pop();
        if (varStack.size() == 0)
            System.out.println("运算过程成功！！！");
        return arith_val;
    }

    // 两个数之间的运算，可以包括 算术、关系和逻辑运算
    private SimpleVariable calculate(SimpleVariable v1,SimpleVariable v2,String top){
        SimpleVariable reVar = null;
        if (v1.getType().equals(v2.getType()) && v1.getType().equals("int")) {
            // 都为 int型
            int a1 = Integer.parseInt(v1.getValue());
            int a2 = Integer.parseInt(v2.getValue());
            if (top.equals("*")) {
                messages.add(a1 + " * " + a2 + " = " + (a1 * a2));
                reVar = new SimpleVariable(null, "int", String.valueOf(a1 * a2), level);
            } else if (top.equals("/")) {
                //TODO 除零错误
                if (a2 == 0) {
                    messages.add("发生除零错误，值自动变为0");
                    reVar = new SimpleVariable(null,"int","0",level);
                }else {
                    messages.add(a1 + " / " +a2 + " = " + (a1 / a2));
                    reVar = new SimpleVariable(null,"int",String.valueOf(a1/a2),level);
                }
            } else if (top.equals("+")) {
                messages.add(a1 + " + " + a2 + " = " + (a1 + a2));
                reVar = new SimpleVariable(null, "int", String.valueOf(a1 + a2), level);
            } else {
                // 没有其他情况了
                messages.add(a1 + " - " + a2 + " = " + (a1 - a2));
                reVar = new SimpleVariable(null, "int", String.valueOf(a1 - a2), level);
            }
        }
        else {
            if (!v1.getType().equals(v2.getType())) {
                if (v1.getType().equals("real"))
                    messages.add("类型" + v1.getType() + "与" + v2.getType() + "不匹配,自动对 " + v2.getValue() +"进行类型转换");
                else
                    messages.add("类型" + v1.getType() + "与" + v2.getType() + "不匹配,自动对 " + v1.getValue() +"进行类型转换");
            }
            double a1 = Double.parseDouble(v1.getValue());
            double a2 = Double.parseDouble(v2.getValue());
            if (top.equals("*")) {
                messages.add(a1 + " * " + a2 + " = " + (a1 * a2));
                reVar = new SimpleVariable(null, "real", String.valueOf(a1 * a2), level);
            } else if (top.equals("/")) {
                //TODO 除零错误
                if (a2 == 0.0) {
                    messages.add("发生除零错误，值自动变为 0.0");
                    reVar = new SimpleVariable(null,"real","0.0",level);
                }else {
                    messages.add(a1 + " / " +a2 + " = " + (a1 / a2));
                    reVar = new SimpleVariable(null,"real",String.valueOf(a1/a2),level);
                }
            } else if (top.equals("+")) {
                messages.add(a1 + " + " + a2 + " = " + (a1 + a2));
                reVar = new SimpleVariable(null, "real", String.valueOf(a1 + a2), level);
            } else {
                messages.add(a1 + " - " + a2 + " = " + (a1 - a2));
                reVar = new SimpleVariable(null, "real", String.valueOf(a1 - a2), level);
            }
        }
        return reVar;
    }

    private ASTNode testArithmeticExp() {
        ASTNode arith = new ASTNode(2, "Arithmetic", null);
        ASTNode item = new ASTNode(2, "Item", null);
        ASTNode v_node = new ASTNode(2, "V", null);
        ASTNode var_node = new ASTNode(0, "Variable", "1212");

        ASTNode fact = new ASTNode(2, "Factor", null);
        ASTNode mul = new ASTNode(0, "*", "*");
        ASTNode item1 = new ASTNode(2, "Item", null);
        ASTNode var1 = new ASTNode(0, "Variable", "90909012");
        ASTNode fac1 = new ASTNode(0, "Factor", null);

        ASTNode plus = new ASTNode(0, "+", "+");
        ASTNode ari1 = new ASTNode(2, "Arithmetic", null);
        ASTNode item2 = new ASTNode(2, "Item", null);
        ASTNode var2 = new ASTNode(0, "Variable", "qe123");
        ASTNode fac2 = new ASTNode(0, "Factor", null);
        ASTNode v_node1 = new ASTNode(0, "V", null);

        arith.addChild(item);
        arith.addChild(v_node);
        item.addChild(var_node);
        item.addChild(fact);
        fact.addChild(mul);
        fact.addChild(item1);
        item1.addChild(var1);
        item1.addChild(fac1);

        v_node.addChild(plus);
        v_node.addChild(ari1);
        ari1.addChild(item2);
        ari1.addChild(v_node1);
        item2.addChild(var2);
        item2.addChild(fac2);
        return arith;
    }

    // "Variable-> ..."
    private SimpleVariable translateVariable(ASTNode variable_node) {
        SimpleVariable variable = null;
        if (variable_node.getMaxChildNum() == 1) {
            // "Variable->Digit"
            ASTNode digit_node = variable_node.getChildren()[0];
            ASTNode positive_node = digit_node.getChildren()[digit_node.getMaxChildNum() - 1];
            //TODO try catch能否处理类型转换的错误
            if (positive_node.getChildren()[0].getName().equals("integer")) {
                // 正整数
                Integer value = Integer.valueOf(positive_node.getChildren()[0].getValue());
                if (digit_node.getChildren()[0].getName().equals("-")) //负数
                    value = -1 * value;
                variable = new SimpleVariable(null, "int", value.toString(), level);
            } else {
                // 小数
                Double value = Double.valueOf(positive_node.getChildren()[0].getValue());
                if (digit_node.getChildren()[0].getName().equals("-")) //负数
                    value = -1.0 * value;
                variable = new SimpleVariable(null, "real", value.toString(), level);
            }
        } else if (variable_node.getMaxChildNum() == 2) {
            // "Variable->identifier Call"
            ASTNode call_node = variable_node.getChildren()[1];
            String identifier = variable_node.getChildren()[0].getValue();
            if (call_node.getChildren()[0].getName().equals("Index")) {
                ASTNode index_node = call_node.getChildren()[0];
                if (index_node.getMaxChildNum() == 0) {
                    SimpleVariable id = simpleTable.getVar(identifier);
                    if (id == null)
                        messages.add("变量 " + identifier + "未被声明，无法使用");
                    else {
                        if (id.getValue() == null)
                            messages.add("变量 " + identifier + "没有被初始化，无法使用");
                        else
                            variable = id;
                    }
                } else {
                    // 数组取下标的值   暂时先用Variable替代LogicExp来代表下标
//                    SimpleVariable index = translateLogicExp(index_node.getChildren()[1]);
                    SimpleVariable index = translateVariable(index_node.getChildren()[1]);

                    if (index.getType().equals("real"))
                        messages.add("数组下标 " + index.getValue() + "不能为小数");
                    else if (Integer.parseInt(index.getValue()) < 0)
                        messages.add("数组下标" + index.getValue() + "不能为负数");
                    else {
                        ArrayVariable arrayVariable = arrayTable.getArray(identifier);
                        if (arrayVariable == null)
                            messages.add("数组变量" + identifier + "未声明，无法使用");
                        else {
                            //TODO 检测下标越界，未赋值等问题
                            if (arrayVariable.getValues() == null || arrayVariable.getValues().size() == 0)
                                messages.add("数组" + identifier + "未被赋值，无法使用");
                            else {
                                Integer ix = Integer.valueOf(index.getValue());
                                if (ix > arrayVariable.getLength() - 1)
                                    messages.add("数组" + identifier + "下标" + ix + "越界");
                                else {
                                    ArrayList<String> array = arrayVariable.getValues();
                                    // 假设数组里一定有值
                                    variable = new SimpleVariable(null, arrayVariable.getType(), array.get(ix), level);
                                }
                            }
                        }
                    }
                }
            } else {
                // 函数调用，Call->( Argument )
            }
        } else if (variable_node.getMaxChildNum() == 3) {
            // "Variable->( Relation )"
            variable = translateRelationExp(variable_node.getChildren()[1]);
        }

        return variable;
    }

    //TODO 把变量列表转成 Value的 String列表，可以检查变量类型是否匹配，能否自动转换，并转换原始的值
    private ArrayList<String> convertArray(ArrayList<SimpleVariable> arrayList, String type) {
        // 这里数组里值的类型都是 match type 的
        ArrayList<String> list = new ArrayList<>();
        for (int i = 0; i < arrayList.size(); i++) {
            list.add(arrayList.get(i).getValue());
        }
        return list;
    }

    private static ASTNode testIdArrayVariable() {
        Translator translator = new Translator();

        translator.simpleTable.addVariable(new SimpleVariable("pp", "int", null, 1));
        ASTNode variable_node = new ASTNode(2, null, null);
        ASTNode id_node = new ASTNode(0, "identifier", "pp");
        ASTNode call_node = new ASTNode(1, "Call", null);
        ASTNode index_node = new ASTNode(0, "Index", null);
        // test fetching value of simple variable
        variable_node.addChild(id_node);
        variable_node.addChild(call_node);
        call_node.addChild(index_node);
        SimpleVariable s = translator.translateVariable(variable_node);
        System.out.println(translator.messages);

        ASTNode var_node = testVariable();
        index_node = new ASTNode(3, "Index", null);
        call_node.getChildren()[0] = index_node;
        // 假定 index只能为 variable时才成立
        index_node.getChildren()[1] = var_node;
        ArrayList<String> values = new ArrayList<>();
        values.add("43");
        values.add("90");
        translator.arrayTable.addVariable(new ArrayVariable("pp", "int", 2, values, 1));
        SimpleVariable s1 = translator.translateVariable(variable_node);
        System.out.println(translator.messages);

        return variable_node;
    }

    private static ASTNode testVariable() {
        ASTNode variable_node = new ASTNode(1, null, null);
        ASTNode a1 = new ASTNode(2, "Digit", null);
        ASTNode b0 = new ASTNode(0, "+", null);
        ASTNode b1 = new ASTNode(1, "Positive", null);
        ASTNode c0 = new ASTNode(0, "integer", "0");
        variable_node.addChild(a1);
        a1.addChild(b0);
        a1.addChild(b1);
        b1.addChild(c0);
        Translator translator = new Translator();
        SimpleVariable s = translator.translateVariable(variable_node);
        System.out.println(s);

        return variable_node;
    }

    public static void main(String[] args) {
        testIdArrayVariable();
    }
}
