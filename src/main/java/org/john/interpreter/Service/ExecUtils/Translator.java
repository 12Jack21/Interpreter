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
    private int whileNum = 0;
    private boolean toBreak = false;
    private boolean toContinue = false;
    private String msg = "";

    public Translator() {
        simpleTable = new SimpleTable();
        arrayTable = new ArrayTable();
        level = 1;
    }

    // 先序遍历 AST，递归调用，目的：输出遍历过程中分析得出的信息，存到messages中
    // 先假设语法分析已经全部通过
    public void translate(ASTNode root) {
        String name = root.getName();
        if (name.equals("Pro")) {
            for (int i = 0; i < root.getMaxChildNum(); i++) {
                //遇到 {} 时的 level变化问题
                if (root.getChildren()[i].getName().equals("{"))
                    level++;
                else if (root.getChildren()[i].getName().equals("}")) {
                    simpleTable.deleteVariable(level);
                    arrayTable.deleteArrays(level);
                    level--;
                } else
                    translate(root.getChildren()[i]);
            }
        } else if (name.equals("Statement")) {
            if (whileNum <= 0 || (!toBreak && !toContinue))
                translate(root.getChildren()[0]);
        } else if (name.equals("Declare")) {
            // int, real, char, void
            String type = root.getChildren()[0].getChildren()[0].getName(); //可以作为非局部变量保存起来，语句结束后清除
            String identifier = root.getChildren()[1].getChildren()[0].getValue();
            ASTNode F = root.getChildren()[1].getChildren()[1];
            if (!F.getChildren()[0].getName().equals("(")) {
                // 不是函数声明
                ASTNode index_node = F.getChildren()[0]; // Index
                ASTNode X_node = F.getChildren()[1]; // X
                translateIndexWithX(index_node, X_node, identifier, type);

                ASTNode C_node = F.getChildren()[2];
                while (C_node.getMaxChildNum() != 0) {
                    translateAssignment(C_node.getChildren()[1], type); // 处理Assignment
                    C_node = C_node.getChildren()[2];
                }
            }else {// TODO 函数定义
                // 保存 F_node 到函数表中
            }
        } else if (name.equals("Assignment")) {
            translateAssignment(root, null);
        } else if (name.equals("IF")) {
            ASTNode logic = root.getChildren()[2];
            SimpleVariable log = translateExp(logic);
            boolean re = (int) Double.parseDouble(log.getValue()) == 1;
            if (re) {
                messages.add("满足 if条件，执行下条程序");
                translate(root.getChildren()[4]);
            } else {
                messages.add("不满足 if条件");
                if (root.getChildren()[5].getMaxChildNum() != 0) {
                    // ELSE->else H
                    messages.add("执行 else里的程序");
                    translate(root.getChildren()[5].getChildren()[1]);
                }
            }
        } else if (name.equals("H")) {
            if (root.getMaxChildNum() == 1)
                translate(root.getChildren()[0]); // Statement
            else { // { Pro }
                level++;
                translate(root.getChildren()[1]);
                simpleTable.deleteVariable(level); //TODO 正确否,考虑用 level的区分
                arrayTable.deleteArrays(level);
                level--;
            }
        } else if (name.equals("WHILE")) {
            whileNum++;
            ASTNode logic = root.getChildren()[2];
            SimpleVariable log = translateExp(logic);
            boolean re = (int) Double.parseDouble(log.getValue()) == 1;
            logic.flushFindTag();
            while (re) {
                messages.add("满足while循环条件，执行循环体程序");
                translate(root.getChildren()[4]); // H_node
                if (toBreak)
                    break;
                re = (int) Double.parseDouble(translateExp(logic).getValue()) == 1;
                logic.flushFindTag();
                toContinue = false;
                root.getChildren()[4].flushFindTag();
            }
            if (toBreak)
                toBreak = false;
            else
                messages.add("不满足while循环条件，循环退出");
            whileNum--;
        } else if (name.equals("Interrupt")) {
            String na = root.getChildren()[0].getName();
            if (na.equals("break") && whileNum > 0) {
                toBreak = true;
                messages.add("遇到 break,循环退出");
            } else if (na.equals("continue") && whileNum > 0) {
                toContinue = true;
                messages.add("遇到 continue,跳到下一次循环");
            }
        }
    }

    private void translateAssignment(ASTNode assignment, String type) {
        String identifier = assignment.getChildren()[0].getValue();
        translateIndexWithX(assignment.getChildren()[1], assignment.getChildren()[2], identifier, type);
    }

    // if type == null,则为 Assignment调用的，否则为 Declare调用
    private void translateIndexWithX(ASTNode index_node, ASTNode X_node, String identifier, String type) {
        if (index_node.getMaxChildNum() == 0) {
            // 声明或者赋值一个简单变量
            if (X_node.getMaxChildNum() == 0) {
                if (type == null)
                    return;
                // 添加变量到 变量符号表中
                if (!simpleTable.addVariable(new SimpleVariable(identifier, type, null, level)))
                    messages.add("变量" + identifier + "已被声明过！");
                else
                    messages.add("变量" + identifier + "被声明为" + type + "型");
            } else {
                // X ->= O
                ASTNode O_node = X_node.getChildren()[1];
                if (O_node.getChildren()[0].getName().equals("{")) {
                    if (type == null) {
                        messages.add("无法用数组对变量进行赋值");
                        return;
                    }
                    messages.add("无法将数组用于初始化简单变量" + identifier);
                } else {
                    // O->Logic
                    ASTNode logic = O_node.getChildren()[0];
                    // 此处只有 type和 value是有意义的
                    SimpleVariable logic_value = translateExp(logic);
                    if (type == null) { //赋值过程
                        SimpleVariable v = simpleTable.getVar(identifier);
                        if (v == null) {
                            messages.add("变量" + identifier + "未声明，无法赋值");
                            return;
                        }
                        if (!v.getType().equals(logic_value.getType())) {
                            if (v.getType().equals("int")) {
                                // 强制转换
                                int val = (int) Double.parseDouble(logic_value.getValue());
                                messages.add("类型不匹配，" + logic_value.getValue() + "强制转换为" + val);
                                v.setValue(String.valueOf(val));
                            } else if (v.getType().equals("real")) {
                                double val = Double.parseDouble(logic_value.getValue());
                                messages.add("类型不匹配，" + logic_value.getValue() + "自动类型转换为" + val);
                                v.setValue(String.valueOf(val));
                            }
                        } else
                            v.setValue(logic_value.getValue());
                        messages.add("变量" + identifier + "被赋值为" + v.getValue());
                    } else {//声明过程
                        if (!type.equals(logic_value.getType())) {
                            if (type.equals("int")) {
                                // 强制转换
                                int val = (int) Double.parseDouble(logic_value.getValue());
                                messages.add("类型不匹配，" + logic_value.getValue() + "强制转换为" + val);
                                logic_value.setValue(String.valueOf(val));
                            } else if (type.equals("real")) {
                                double val = Double.parseDouble(logic_value.getValue());
                                messages.add("类型不匹配，" + logic_value.getValue() + "自动类型转换为" + val);
                                logic_value.setValue(String.valueOf(val));
                            }
                        }
                        logic_value.setName(identifier);
                        if (!simpleTable.addVariable(logic_value))
                            messages.add("变量" + identifier + "已被声明过");
                        else
                            messages.add("变量" + identifier + "被声明为" + type +
                                    "型并初始化为" + logic_value.getValue());
                    }
                }
            }
        } else {
            // 声明数组，或给数组下标的位置赋值
            ASTNode logic = index_node.getChildren()[1];
            SimpleVariable array_length = translateExp(logic);

            if (array_length.getType().equals("real")) {
                messages.add("数组下标不允许为小数" + array_length.getValue() + " ，只能为正整数");
            } else {
                int ix = Integer.parseInt(array_length.getValue());
                if (ix < 0)
                    messages.add("数组时下标不允许为负数" + array_length.getValue() + " ，只能为正整数");
                else {
                    // 下标已经满足了条件
                    if (X_node.getMaxChildNum() == 0) {
                        if (type == null) //赋值时此语句无意义
                            return;
                        // 只有声明没有初始化的情况
                        // 添加变量到 变量符号表中 TODO 未赋值的使用问题，联系 translateVariable(),下面同理
                        ArrayList<String> zeroValues = new ArrayList<>();
                        int i = ix;
                        if (type.equals("int")) {
                            while (i-- > 0) {
                                zeroValues.add(String.valueOf(0));
                            }
                        } else if (type.equals("real")) {
                            while (i-- > 0) {
                                zeroValues.add(String.valueOf(0.0));
                            }
                        }
                        if (!arrayTable.addVariable(new ArrayVariable(identifier, type, ix, zeroValues, level)))
                            messages.add("数组变量" + identifier + "已被声明过！");
                        else
                            messages.add("数组变量" + identifier + "被声明为" + type +
                                    "型,含" + ix + "个元素，并自动初始化为" + zeroValues);
                    } else {
                        // X ->= O，伴随着初始化（赋值）的情况
                        ASTNode O_node = X_node.getChildren()[1];
                        if (O_node.getMaxChildNum() != 3) {
                            if (type != null) {
                                messages.add("不能用单独的表达式来初始化数组" + identifier);
                                if (!arrayTable.addVariable(new ArrayVariable(identifier, type, ix, new ArrayList<>(ix), level)))
                                    messages.add("数组变量" + identifier + "已被声明过！");
                                else
                                    messages.add("数组变量" + identifier + "被声明为" + type + "型,含" + ix + "个元素");
                            } else {
                                // 数组下标位置 赋值的情况
                                ArrayVariable v = arrayTable.getArray(identifier);
                                if (v == null)
                                    messages.add("数组变量未声明，无法赋值");
                                else {
                                    if (ix >= v.getLength())
                                        messages.add("数组下标" + ix + " 越界");
                                    else {
                                        SimpleVariable log = translateExp(O_node.getChildren()[0]);
                                        if (log != null) {
                                            if (!v.getType().equals(log.getType())) {
                                                if (v.getType().equals("int")) {
                                                    // 强制转换
                                                    int val = (int) Double.parseDouble(log.getValue());
                                                    messages.add("类型不匹配，" + log.getValue() + "强制转换为" + val);
                                                    v.getValues().set(ix, String.valueOf(val));
                                                } else if (v.getType().equals("real")) {
                                                    double val = Double.parseDouble(log.getValue());
                                                    messages.add("类型不匹配，" + log.getValue() + "自动类型转换为" + val);
                                                    v.getValues().set(ix, String.valueOf(val));
                                                }
                                            } else
                                                v.getValues().set(ix, log.getValue());
                                            messages.add("数组变量" + identifier + "第" + ix + "个位置被赋值为" + v.getValues().get(ix)
                                                    + ",数组当前值为" + v.getValues());
                                        }
                                    }
                                }
                            }
                        } else {
                            if (type == null) {
                                //TODO 多维数组除外
                                messages.add("不能用一个数组来赋值");
                                return;
                            }
                            // 数组初始化 O->{ Y }
                            ASTNode Y_node = O_node.getChildren()[1];
                            ArrayList<String> vals;
                            int i = ix;
                            if (Y_node.getMaxChildNum() == 0) {
                                vals = new ArrayList<>();
                                // 数组声明为空时，全部赋给初始值
                                if (type.equals("int")) {
                                    while (i-- > 0) {
                                        vals.add(String.valueOf(0));
                                    }
                                } else if (type.equals("real")) {
                                    while (i-- > 0) {
                                        vals.add(String.valueOf(0.0));
                                    }
                                }
                            } else {
                                // 数组里的值
                                ArrayList<SimpleVariable> array_values = translateY(Y_node);
                                vals = convertArray(array_values, type);
                                if (vals.size() > ix) {
                                    messages.add("用于初始化的数组内元素过多，自动全部赋了初值");
                                    vals = new ArrayList<>();
                                    if (type.equals("int")) {
                                        while (i-- > 0) {
                                            vals.add(String.valueOf(0));
                                        }
                                    } else if (type.equals("real")) {
                                        while (i-- > 0) {
                                            vals.add(String.valueOf(0.0));
                                        }
                                    }
                                } else {
                                    //数组元素不足时，自动填充初始值
                                    i = vals.size();
                                    while (i < ix) {
                                        vals.add(type.equals("int") ? String.valueOf(0) : String.valueOf(0.0));
                                        i++;
                                    }
                                }
                            }
                            ArrayVariable arrayVariable = new ArrayVariable(identifier, type,
                                    vals.size(), vals, level);
                            if (arrayTable.addVariable(arrayVariable))
                                messages.add("数组变量" + identifier + "被声明为" + type +
                                        "型并被初始化为 " + arrayVariable.getValues().toString());
                            else
                                //TODO 考虑声明时全部初始化为 初始值
                                messages.add("数组变量" + identifier + "已被声明过,无法初始化！");
                        }
                    }
                }
            }
        }
    }

    // when Y->Logic C', C'->, Y
    private ArrayList<SimpleVariable> translateY(ASTNode Y) {
        ArrayList<SimpleVariable> variables = new ArrayList<>();
        ASTNode logic = Y.getChildren()[0];
        SimpleVariable logic_value = translateExp(logic);
        variables.add(logic_value);

        ASTNode C_ = Y.getChildren()[1];
        if (C_.getMaxChildNum() != 0) {
            variables.addAll(translateY(C_.getChildren()[1]));
        }
        return variables;
    }

    // 翻译 所有表达式 exp,处理类型错误和类型转换
    private SimpleVariable translateExp(ASTNode arithmetic) {
        SimpleVariable arith_val = null;
        LinkedList<SimpleVariable> varStack = new LinkedList<>(); //变量栈
        LinkedList<String> symStack = new LinkedList<>(); //符号栈
        HashMap<String, Integer> prioMap = CodeTable.opPriority();

        // 起始变量
        ASTNode var = arithmetic.findNextNodeWithValueOrTip("Variable");
        SimpleVariable variable = translateVariable(var);
        varStack.addFirst(variable);
        ASTNode sym;
        // 没有下一个运算符号了 , 结束条件的选择，所有运算的token，数+符号+数+...+
        while ((sym = arithmetic.findNextNodeWithValueOrTip("symbol")) != null) {
            String sym_value = sym.getValue();
            // 连续判断优先级
            while (symStack.size() > 0 && prioMap.get(sym_value) <= prioMap.get(symStack.get(0))) {
                // 栈顶符号的优先级更高或相等（左结合）
                SimpleVariable v2 = varStack.pop();
                SimpleVariable v1 = varStack.pop();
                String top = symStack.pop();
                varStack.addFirst(calculate(v1, v2, top));
            }
            // 栈顶符号的优先级更小,则新符号入栈
            symStack.addFirst(sym_value);
            variable = translateVariable(arithmetic.findNextNodeWithValueOrTip("Variable"));
            varStack.addFirst(variable);
        }
        // 到这里已经没有符号再进栈了
        while (symStack.size() != 0) {
            String top = symStack.pop();
            SimpleVariable v2 = varStack.pop();
            SimpleVariable v1 = varStack.pop();
            varStack.addFirst(calculate(v1, v2, top));
        }
//        arithmetic.flushFindTag(); // TODO 刷新find标志，以供之后使用,放在递归函数的外部
        arith_val = varStack.pop();
        if (varStack.size() == 0)
            System.out.println("运算过程进行成功！！！");
        return arith_val;
    }

    // 两个数之间的运算，可以包括 算术、关系和逻辑运算
    private SimpleVariable calculate(SimpleVariable v1, SimpleVariable v2, String top) {
        SimpleVariable reVar;
        if (v1.getType().equals(v2.getType()) && v1.getType().equals("int")) {
            // 都为 int型
            int a1 = Integer.parseInt(v1.getValue());
            int a2 = Integer.parseInt(v2.getValue());
            if (top.equals("*")) {
                messages.add(a1 + " * " + a2 + " = " + (a1 * a2));
                reVar = new SimpleVariable(null, "int", String.valueOf(a1 * a2), level);
            } else if (top.equals("/")) {
                if (a2 == 0) {
                    messages.add("发生除零错误，值自动变为0");
                    reVar = new SimpleVariable(null, "int", "0", level);
                } else {
                    messages.add(a1 + " / " + a2 + " = " + (a1 / a2));
                    reVar = new SimpleVariable(null, "int", String.valueOf(a1 / a2), level);
                }
            } else if (top.equals("+")) {
                messages.add(a1 + " + " + a2 + " = " + (a1 + a2));
                reVar = new SimpleVariable(null, "int", String.valueOf(a1 + a2), level);
            } else if (top.equals("-")) {
                messages.add(a1 + " - " + a2 + " = " + (a1 - a2));
                reVar = new SimpleVariable(null, "int", String.valueOf(a1 - a2), level);
            } else {
                int val = 0;
                if (top.equals("=="))
                    val = a1 == a2 ? 1 : 0;
                else if (top.equals("<>"))
                    val = a1 == a2 ? 0 : 1;
                else if (top.equals("<"))
                    val = a1 < a2 ? 1 : 0;
                else if (top.equals("<="))
                    val = a1 <= a2 ? 1 : 0;
                else if (top.equals(">"))
                    val = a1 > a2 ? 1 : 0;
                else if (top.equals(">="))
                    val = a1 >= a2 ? 1 : 0;
                else if (top.equals("||"))
                    val = ((a1 != 0) || (a2 != 0)) ? 1 : 0;
                else if (top.equals("&&"))
                    val = ((a1 != 0) && (a2 != 0)) ? 1 : 0;
                else
                    messages.add("运算出错！！！");
                messages.add(a1 + top + a2 + " = " + val);
                reVar = new SimpleVariable(null, "int", String.valueOf(val), level);
            }
        } else { // 有real型存在
            if (!v1.getType().equals(v2.getType())) {
                if (v1.getType().equals("real"))
                    messages.add("类型" + v1.getType() + "与" + v2.getType() + "不匹配,自动对 " + v2.getValue() + "进行类型转换");
                else
                    messages.add("类型" + v1.getType() + "与" + v2.getType() + "不匹配,自动对 " + v1.getValue() + "进行类型转换");
            }
            double a1 = Double.parseDouble(v1.getValue());
            double a2 = Double.parseDouble(v2.getValue());
            if (top.equals("*")) {
                messages.add(a1 + " * " + a2 + " = " + (a1 * a2));
                reVar = new SimpleVariable(null, "real", String.valueOf(a1 * a2), level);
            } else if (top.equals("/")) {
                if (a2 == 0.0) {
                    messages.add("发生除零错误，值自动变为 0.0");
                    reVar = new SimpleVariable(null, "real", "0.0", level);
                } else {
                    messages.add(a1 + " / " + a2 + " = " + (a1 / a2));
                    reVar = new SimpleVariable(null, "real", String.valueOf(a1 / a2), level);
                }
            } else if (top.equals("+")) {
                messages.add(a1 + " + " + a2 + " = " + (a1 + a2));
                reVar = new SimpleVariable(null, "real", String.valueOf(a1 + a2), level);
            } else if (top.equals("-")) {
                messages.add(a1 + " - " + a2 + " = " + (a1 - a2));
                reVar = new SimpleVariable(null, "real", String.valueOf(a1 - a2), level);
            } else {
                //TODO 是否会有其他情况没有考虑到
                int val = 0;
                if (top.equals("=="))
                    val = a1 == a2 ? 1 : 0;
                else if (top.equals("<>"))
                    val = a1 == a2 ? 0 : 1;
                else if (top.equals("<"))
                    val = a1 < a2 ? 1 : 0;
                else if (top.equals("<="))
                    val = a1 <= a2 ? 1 : 0;
                else if (top.equals(">"))
                    val = a1 > a2 ? 1 : 0;
                else if (top.equals(">="))
                    val = a1 >= a2 ? 1 : 0;
                else if (top.equals("||"))
                    val = ((a1 != 0.0) || (a2 != 0.0)) ? 1 : 0;
                else if (top.equals("&&"))
                    val = ((a1 != 0.0) && (a2 != 0.0)) ? 1 : 0;
                else
                    messages.add("运算出错！！！");
                messages.add(a1 + top + a2 + " = " + val);
                reVar = new SimpleVariable(null, "int", String.valueOf(val), level);
            }
        }
        return reVar;
    }

    // "Variable-> ..." TODO 取值的时候没有，就返回默认值（考虑 Null 会给上层带来不便）
    private SimpleVariable translateVariable(ASTNode variable_node) {
        SimpleVariable variable = null;
        if (variable_node.getMaxChildNum() == 1) {
            // "Variable->Digit"
            ASTNode digit_node = variable_node.getChildren()[0];
            ASTNode positive_node = digit_node.getChildren()[digit_node.getMaxChildNum() - 1];
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
                    if (id == null) {
                        messages.add("变量 " + identifier + "未被声明，无法使用,自动返回默认值 0");
                        variable = new SimpleVariable(null,"int","0",level);
                    }
                    else {
                        if (id.getValue() == null) {
                            messages.add("变量 " + identifier + "没有被初始化，无法使用，自动返回默认值 0");
                            variable = new SimpleVariable(null,"int","0",level);
                        }
                        else
                            variable = id;
                    }
                } else {
                    // 数组取下标的值
                    SimpleVariable index = translateExp(index_node.getChildren()[1]); //Logical expression
                    if (index.getType().equals("real"))
                        messages.add("数组下标 " + index.getValue() + "不能为小数");
                    else if (Integer.parseInt(index.getValue()) < 0)
                        messages.add("数组下标" + index.getValue() + "不能为负数");
                    else {
                        ArrayVariable arrayVariable = arrayTable.getArray(identifier);
                        if (arrayVariable == null) {
                            messages.add("数组变量" + identifier + "未声明，无法使用，自动返回默认值 0");
                            variable = new SimpleVariable(null,"int","0",level);
                        }
                        else {
                            //TODO 检测下标越界，未赋值等问题
                            if (arrayVariable.getValues() == null || arrayVariable.getValues().size() == 0) {
                                messages.add("数组" + identifier + "未被赋值，无法使用，自动返回默认值 0");
                                variable = new SimpleVariable(null,"int","0",level);
                            }
                            else {
                                Integer ix = Integer.valueOf(index.getValue());
                                if (ix > arrayVariable.getLength() - 1) {
                                    messages.add("数组" + identifier + "下标" + ix + "越界,自动返回默认值 0");
                                    variable = new SimpleVariable(null,"int","0",level);
                                }
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
                // TODO 函数调用，Call->( Argument )
            }
        } else if (variable_node.getMaxChildNum() == 3) {
            // "Variable->( Relation )"
            variable = translateExp(variable_node.getChildren()[1]);
        }
        return variable;
    }

    //把变量列表转成 Value的 String列表，可以检查变量类型是否匹配，进行自动转换和强制转换，并转换原始的值
    private ArrayList<String> convertArray(ArrayList<SimpleVariable> arrayList, String type) {
        // 这里数组里值的类型都是 match type 的
        ArrayList<String> list = new ArrayList<>();
        for (SimpleVariable var : arrayList) {
            if (!var.getType().equals(type)) {
                if (var.getType().equals("int")) {
                    double val = Double.parseDouble(var.getValue());
                    messages.add("类型不匹配，" + var.getValue() + "自动类型转换为" + val);
                    list.add(String.valueOf(val));
                } else if (var.getType().equals("real")) {
                    int val = (int) Double.parseDouble(var.getValue());
                    messages.add("类型不匹配，" + var.getValue() + "强制转换为" + val);
                    list.add(String.valueOf(val));
                }
            } else
                list.add(var.getValue());
        }
        return list;
    }

    private static void testWhileIf() {
        Translator t = new Translator();
        String pro = "int a ;\n" +
                "int factorial;\n" +
                "a =6;\n" +
                "factorial =1;\n" +
                "while( a <> 0 )\n" +
                "{\n" +
                "\tfactorial = factorial * a;\n" +
                "\ta = a -1;\n" +
                "}\n" +
                "write( factorial );\n" +
                "\n";
        ASTNode p = Executor.analyze(pro).getAstNode();
        ASTNode whi = p.getChildren()[0].getChildren()[0];

        t.simpleTable.addVariable(new SimpleVariable("p", "int", "0", 1));
        ArrayList<String> values = new ArrayList<>();
        values.add("43");
        values.add("90");
        t.arrayTable.addVariable(new ArrayVariable("p2", "int", 2, values, 1));

        t.translate(p);
        System.out.println(t.messages);
    }

    private static void testDeclareAssign() {
        Translator t = new Translator();
        ASTNode p = Executor.analyze("p = p2[2 -1*9 && 0==12 >=12 <>100 || -99]-1.2;").getAstNode();
        ASTNode declare = p.getChildren()[0].getChildren()[0];

        t.simpleTable.addVariable(new SimpleVariable("p", "real", "12.3", 1));
        ArrayList<String> values = new ArrayList<>();
        values.add("43");
        values.add("90");
        t.arrayTable.addVariable(new ArrayVariable("p2", "int", 2, values, 1));

        t.translate(declare);
        System.out.println(t.messages);
    }

    private static ASTNode testIdArrayVariable() {
        Translator translator = new Translator();
        translator.simpleTable.addVariable(new SimpleVariable("pp", "int", null, 1));
        ArrayList<String> values = new ArrayList<>();
        values.add("43");
        values.add("90");
        translator.arrayTable.addVariable(new ArrayVariable("pp", "int", 2, values, 1));

        ASTNode p = Executor.analyze("pp[2 -1*9 && 0==12 >=12 <>100 && 0];").getAstNode();
        ASTNode logic = p.getChildren()[0].getChildren()[0];
        SimpleVariable s1 = translator.translateExp(logic);
        System.out.println("取数信息：" + translator.messages);

        return logic;
    }

    private static List<ASTNode> testVariable() {
        ASTNode variable_node1 = new ASTNode(1, "Variable", null);
        ASTNode a1 = new ASTNode(2, "Digit", null);
        ASTNode b0 = new ASTNode(0, "+", "+");
        ASTNode b1 = new ASTNode(1, "Positive", null);
        ASTNode c0 = new ASTNode(0, "fraction", "2.00");
        variable_node1.addChild(a1);
        a1.addChild(b0);
        a1.addChild(b1);
        b1.addChild(c0);
        Translator translator = new Translator();
        SimpleVariable s = translator.translateVariable(variable_node1);

        ASTNode variable_node2 = new ASTNode(1, "Variable", null);
        ASTNode a11 = new ASTNode(2, "Digit", null);
        ASTNode b00 = new ASTNode(0, "+", "+");
        ASTNode b11 = new ASTNode(1, "Positive", null);
        ASTNode c00 = new ASTNode(0, "integer", "1");
        variable_node2.addChild(a11);
        a11.addChild(b00);
        a11.addChild(b11);
        b11.addChild(c00);

        ASTNode variable_node3 = new ASTNode(1, "Variable", null);
        ASTNode a111 = new ASTNode(2, "Digit", null);
        ASTNode b000 = new ASTNode(0, "+", "+");
        ASTNode b111 = new ASTNode(1, "Positive", null);
        ASTNode c000 = new ASTNode(0, "integer", "1");
        variable_node3.addChild(a111);
        a111.addChild(b000);
        a111.addChild(b111);
        b111.addChild(c000);

        ASTNode variable_node4 = new ASTNode(1, "Variable", null);
        ASTNode a1111 = new ASTNode(2, "Digit", null);
        ASTNode b0000 = new ASTNode(0, "+", "+");
        ASTNode b1111 = new ASTNode(1, "Positive", null);
        ASTNode c0000 = new ASTNode(0, "integer", "1");
        variable_node4.addChild(a1111);
        a1111.addChild(b0000);
        a1111.addChild(b1111);
        b1111.addChild(c0000);

        List<ASTNode> nodes = new LinkedList<>();
        nodes.add(variable_node1);
        nodes.add(variable_node2);
        nodes.add(variable_node3);
        nodes.add(variable_node4);
        return nodes;
    }

    public static void main(String[] args) {
        testWhileIf();

    }
}
