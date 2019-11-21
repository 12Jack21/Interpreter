package org.john.interpreter.Service.ExecUtils;

import org.apache.el.lang.ELArithmetic;
import org.john.interpreter.Service.SemanticUtils.*;
import org.john.interpreter.dto.Wrapper;

import java.lang.reflect.Array;
import java.util.*;

@SuppressWarnings("ALL")
public class Translator {

    private List<String> messages = new LinkedList<>();
    private int level; // 当前作用域
    private SimpleTable simpleTable;
    private ArrayTable arrayTable;
    private FunctionTable functionTable;
    private int whileNum = 0;
    private int proNum = 0;
    private boolean toBreak = false;
    private boolean toContinue = false;
    private boolean toReturn = false;
    private LinkedList<String> returnTypeStack;
    private String msg = "";
    private SimpleVariable returnVal = null;// 用于传递函数返回值，为空则置默认值 0（int）

    private LinkedList<String> printList;

    public Translator() {
        simpleTable = new SimpleTable();
        arrayTable = new ArrayTable();
        functionTable = new FunctionTable();
        returnTypeStack = new LinkedList<>();
        printList = new LinkedList<>();
        level = 1;
    }

    public LinkedList<String> getPrintList() {
        return printList;
    }

    public List<String> getMessages() {
        return messages;
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
            } else {//  函数定义
                // 保存 pro_node 到函数表中
                try {
                    ArrayList<Object> parameters = translateParameter(F.getChildren()[1]);
                    FunctionVariable v = new FunctionVariable(type, identifier, parameters, F.getChildren()[4]);
                    functionTable.addVariable(v);
                    String msg = "声明了函数" + identifier + ",";
                    if (parameters.size() == 0)
                        msg += "没有参数";
                    else {
                        msg += "参数列表为(";
                        for (Object param : parameters) {
                            if (param instanceof SimpleVariable)
                                msg += ((SimpleVariable) param).getType() + " " + ((SimpleVariable) param).getName();
                            else if (param instanceof ArrayVariable)
                                msg += ((ArrayVariable) param).getType() + " " + ((ArrayVariable) param).getArrayName()
                                        + "[" + ((ArrayVariable) param).getLength() + "]";
                            msg += ",";
                        }
                        if (msg.endsWith(","))
                            msg = msg.substring(0, msg.length() - 1);
                        msg += ")";
                    }
                    msg += ",返回类型为 " + v.getType();
                    messages.add(msg);
                } catch (Exception e) {
                    messages.add("函数" + identifier + "声明失败！");
                }
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
                boolean exeELSEIF = false;
                ASTNode ELSEIF = root.getChildren()[5];
                // 判断是否有else if
                if (ELSEIF.getMaxChildNum() != 0) {
                    ArrayList<ASTNode> logics = translateELSEIF(ELSEIF);
                    int num = 0;
                    while (num < logics.size()) {
                        // 一个一个地判断 else if 的条件
                        re = (int) Double.parseDouble(translateExp(logics.get(num)).getValue()) == 1;
                        if (re) {
                            messages.add("满足第" + (num + 1) + "个 else if 语句，执行该块的程序");
                            exeELSEIF = true;
                            break;
                        }
                        num++;
                    }
                    if (exeELSEIF) {
                        // 找到执行的 else if 块
                        while (num-- > 0) {
                            ELSEIF = ELSEIF.getChildren()[6];
                        }
                        ASTNode H_node = ELSEIF.getChildren()[5];
                        translate(H_node); // 执行 H_node
                    }
                }

                // else if 里的条件都不满足时
                if (!exeELSEIF) {
                    if (root.getChildren()[6].getMaxChildNum() != 0) {
                        // ELSE->else H
                        messages.add("执行 else里的程序");
                        translate(root.getChildren()[5].getChildren()[1]);
                    }
                }
            }
        } else if (name.equals("H")) {
            if (root.getMaxChildNum() == 1)
                translate(root.getChildren()[0]); // Statement
            else { // { Pro }
                level++;
                translate(root.getChildren()[1]);
                simpleTable.deleteVariable(level);
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
        } else if (name.equals("Logic")) {
            SimpleVariable s = translateExp(root);
        } else if (name.equals("Interrupt")) {
            String na = root.getChildren()[0].getName();
            if (na.equals("break") && whileNum > 0) {
                toBreak = true;
                messages.add("遇到 break,循环退出");
            } else if (na.equals("continue") && whileNum > 0) {
                toContinue = true;
                messages.add("遇到 continue,跳到下一次循环");
            } else if (na.equals("return")) {
                //TODO 程序 return 后会截断后面代码的执行---
                ASTNode result_node = root.getChildren()[1];
                if (result_node.getMaxChildNum() != 0) {
                    SimpleVariable log = translateExp(result_node.getChildren()[0]);

                    String type = returnTypeStack.pop();
                    //返回值置入 returnVal中
                    SimpleVariable tmp = new SimpleVariable(null, type, null, level);
                    if (!type.equals(log.getType())) {
                        if (type.equals("int")) {
                            // 强制转换
                            int val = (int) Double.parseDouble(log.getValue());
                            messages.add("值类型与返回类型不匹配，" + log.getValue() + "强制转换为" + val + "并返回");
                            tmp.setValue(String.valueOf(val));
                        } else if (type.equals("real")) {
                            double val = Double.parseDouble(log.getValue());
                            messages.add("值类型与返回类型不匹配，" + log.getValue() + "自动类型转换为" + val + "并返回");
                            tmp.setValue(String.valueOf(val));
                        }
                    } else
                        tmp.setValue(log.getValue());
                    returnVal = tmp;
                }
                // 没有返回值则不加理会
                if (proNum > 0) {
                    toReturn = true;
                    messages.add("遇到return,程序退出");
                }
            }
        }
    }

    private ArrayList<ASTNode> translateELSEIF(ASTNode ELSEIF) {
        ArrayList<ASTNode> logics = new ArrayList<>();
        logics.add(ELSEIF.getChildren()[3]);
        if (ELSEIF.getChildren()[6].getMaxChildNum() != 0) {
            logics.addAll(translateELSEIF(ELSEIF.getChildren()[6]));
        }
        return logics;
    }

    // when CC->, Parameter, like translateY， just accept simple variable
    private ArrayList<Object> translateParameter(ASTNode parameter) throws Exception {
        ArrayList<Object> parameters = new ArrayList<>();

        if (parameter.getMaxChildNum() == 4) {
            // Parameter->Type identifier Index CC
            String type = parameter.getChildren()[0].getChildren()[0].getName(); //可能为 void，char
            String identifier = parameter.getChildren()[1].getValue();
            ASTNode index_node = parameter.getChildren()[2];
            ASTNode CC_node = parameter.getChildren()[3];
            if (index_node.getMaxChildNum() == 0) {
                //简单变量的参数
                SimpleVariable v = new SimpleVariable(identifier, type, null, level);//TODO level的影响
                parameters.add(v);
            } else {
                //数组变量的参数，有错误就无法成功声明，考虑throw
                ASTNode logic = index_node.getChildren()[1];
                SimpleVariable log = translateExp(logic);
                int len;
                if (log.getType().equals("real")) {
                    len = (int) Double.parseDouble(log.getValue());
                    messages.add("作为参数的数组长度不能为小数" + log.getValue() + "，将强制转换为" + len);
                    if (len < 0) {
                        messages.add("作为参数的数组长度不能为负数" + len);
                        throw new Exception();
                    }
                } else //int型
                    len = Integer.parseInt(log.getValue());
                ArrayVariable v = new ArrayVariable(identifier, type, len, null, level);
                parameters.add(v);
            }
            if (CC_node.getMaxChildNum() != 0)
                parameters.addAll(translateParameter(CC_node.getChildren()[1]));
        }
        return parameters;
    }

    private void translateAssignment(ASTNode assignment, String type) {
        String identifier = assignment.getChildren()[0].getValue();
        translateIndexWithX(assignment.getChildren()[1], assignment.getChildren()[2], identifier, type);
    }

    // 翻译 Index 节点判断为几维数组
    private ArrayList<SimpleVariable> translateIndex(ASTNode index) {
        ArrayList<SimpleVariable> indexs = new ArrayList<>();
        if (index.getMaxChildNum() != 0) {
            indexs.add(translateExp(index.getChildren()[1]));
            indexs.addAll(translateIndex(index.getChildren()[3]));
        }
        return indexs;
    }

    // 专门处理类型转换 的赋值
    private SimpleVariable typeHandle(SimpleVariable v, SimpleVariable logic_value) {
        if (logic_value.getType().equals("string")) {
            if (v.getType().equals("int"))
                v.setValue("0");
            else if (v.getType().equals("real"))
                v.setValue("0.0");
            else if (v.getType().equals("char"))
                v.setValue("\0");
            messages.add("非法使用 string，自动返回默认值 " + v.getValue());
        } else {
            if (!v.getType().equals(logic_value.getType())) {
                if (v.getType().equals("int") && logic_value.getType().equals("real")) {
                    // 强制转换
                    int val = (int) Double.parseDouble(logic_value.getValue());
                    messages.add("类型不匹配，" + logic_value.getValue() + "强制转换为" + val);
                    v.setValue(String.valueOf(val));
                } else if (v.getType().equals("real") && logic_value.getType().equals("int")) {
                    double val = Double.parseDouble(logic_value.getValue());
                    messages.add("类型不匹配，" + logic_value.getValue() + "自动类型转换为" + val);
                    v.setValue(String.valueOf(val));
                } else if (v.getType().equals("char") && logic_value.getType().equals("int")) {
                    // int过大导致没有对应字符的问题
                    char val = (char) Integer.parseInt(logic_value.getValue());
                    messages.add("类型不匹配，" + logic_value.getValue() + "强制转换为" + val);
                    v.setValue(String.valueOf(val));
                } else if (v.getType().equals("int") && logic_value.getType().equals("char")) {
                    int val = (int) logic_value.getValue().charAt(0);
                    messages.add("类型不匹配，" + logic_value.getValue() + "自动类型转换为" + val);
                    v.setValue(String.valueOf(val));
                } else if (v.getType().equals("char") && logic_value.getType().equals("real")) {
                    char val = (char) Double.parseDouble(logic_value.getValue()); //side effect
                    messages.add("类型不匹配，" + logic_value.getValue() + "自动转换为" + val);
                    v.setValue(String.valueOf(val));
                } else if (v.getType().equals("real") && logic_value.getType().equals("char")) {
                    // 先转成int，再转成character
                    double val = Double.parseDouble(String.valueOf((int) logic_value.getValue().charAt(0)));
                    messages.add("类型不匹配，" + logic_value.getValue() + "强制转换为" + val);
                    v.setValue(String.valueOf(val));
                }
            } else
                v.setValue(logic_value.getValue());
        }
        return v;
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
                        v = typeHandle(v, logic_value); //直接进行类型处理
                        messages.add("变量" + identifier + "被赋值为" + v.getValue());
                    } else {
                        // 声明和初始化过程--------------------------------
                        // 为了少省定义一个变量的开销
                        logic_value = typeHandle(new SimpleVariable(null, type, null, level), logic_value);
                        logic_value.setName(identifier);
                        if (!simpleTable.addVariable(logic_value))
                            messages.add("变量" + identifier + "已被声明过");
                        else
                            messages.add("变量" + identifier + "被声明为" + type +
                                    "型并初始化为" + logic_value.getValue());
                    }
                }
            }
        } else { // 声明数组，或给数组下标的位置赋值
            ArrayList<SimpleVariable> dimension_logics = translateIndex(index_node);
            ArrayList<Integer> dimension_index = new ArrayList<>();// 下标列表
            // 检查下标是否合法,不合法则自动退出
            for (SimpleVariable s : dimension_logics) {
                if (s.getType().equals("real")) {
                    messages.add("数组下标不允许为小数" + s.getValue() + " ，只能为正整数");
                    return;
                } else {
                    int ix = Integer.parseInt(s.getValue());
                    if (ix < 0) {
                        messages.add("数组时下标不允许为负数" + s.getValue() + " ，只能为正整数");
                        return;
                    } else
                        dimension_index.add(ix);
                }
            }

            // 下标已经满足了不为小数和负数的条件
            if (X_node.getMaxChildNum() == 0) {
                // 只有声明没有初始化的情况
                if (type == null) //赋值时此语句无意义
                    return;
                // 添加变量到 变量符号表中  -未赋值的使用问题，联系 translateVariable()
                ArrayList<String> zeroValues = new ArrayList<>();
                int total = 1; //总的数组内元素数量
                for (Integer ix : dimension_index)
                    total *= ix;
                if (type.equals("int")) {
                    while (total-- > 0) {
                        zeroValues.add(String.valueOf(0));
                    }
                } else if (type.equals("real")) {
                    while (total-- > 0) {
                        zeroValues.add(String.valueOf(0.0));
                    }
                } else if (type.equals("char")) {
                    while (total-- > 0)
                        zeroValues.add(String.valueOf('\0')); // 字符默认值 \0
                }
                if (!arrayTable.addVariable(new ArrayVariable(identifier, type, dimension_index, zeroValues, level)))
                    messages.add("数组变量" + identifier + "已被声明过！");
                else {
                    String msg = "数组变量" + identifier + "被声明为" + type + "型,维度为 " + dimension_index.toString();
                    msg += " ,并自动初始化为" + zeroValues;
                    messages.add(msg);
                }
            } else {
                // X ->= O，伴随着初始化（赋值）的情况
                ASTNode O_node = X_node.getChildren()[1];
                if (O_node.getMaxChildNum() != 3) { //TODO add string
                    if (type != null) {
                        ArrayList<String> zeroValues = new ArrayList<>();
                        SimpleVariable log_val = translateExp(O_node.getChildren()[0]);
                        if (log_val.getType().equals("string") && type.equals("char")) {
                            // 用 string 来初始化 单维或多维char数组
                            String val = log_val.getValue();
                            int total = 1; //总的数组内元素数量
                            for (Integer ix : dimension_index)
                                total *= ix;
                            // 包括上最后的一个 \0
                            if (val.length() > total - 1){
                                messages.add("用来初始化的字符串过长！");
                            }else {
                                for (int i = 0; i < val.length(); i++)
                                    zeroValues.add(String.valueOf(val.charAt(i)));
                            }
                            int start = zeroValues.size();
                            // 不够的自动赋给初始值 \0,包括字符串最后的 \0
                            while (start++ < total)
                                zeroValues.add(String.valueOf('\0'));  // 直接使用 "\0" 是否相同？
                        } else {
                            messages.add("不能用单独的表达式来初始化除了char型之外的数组" + identifier);
                            // 不能初始化，就自动声明
                            int total = 1; //总的数组内元素数量
                            for (Integer ix : dimension_index)
                                total *= ix;
                            if (type.equals("int")) {
                                while (total-- > 0) {
                                    zeroValues.add(String.valueOf(0));
                                }
                            } else if (type.equals("real")) {
                                while (total-- > 0) {
                                    zeroValues.add(String.valueOf(0.0));
                                }
                            } else if (type.equals("char")){
                                while (total-- >0)
                                    zeroValues.add(String.valueOf('\0'));
                            }
                        }
                        if (!arrayTable.addVariable(new ArrayVariable(identifier, type, dimension_index, zeroValues, level)))
                            messages.add("数组变量" + identifier + "已被声明过！");
                        else {
                            String msg = "数组变量" + identifier + "被声明为" + type + "型,维度为 " + dimension_index.toString();
                            msg += " ,并自动初始化为" + zeroValues;
                            messages.add(msg);
                        }
                    } else { //TODO add char array
                        // 数组下标位置 赋值的情况
                        ArrayVariable v = arrayTable.getArray(identifier);
                        if (v == null)
                            messages.add("数组变量未声明，无法赋值");
                        else {
                            // 判断下标是否过多
                            if (dimension_index.size() != v.getDimensionList().size()) {
                                messages.add("数组下标数量不匹配，无法赋值");
                                return;
                            }
                            ArrayList<Integer> dimensionList = v.getDimensionList();
                            // 判断下标是否越界， 同时计算"物理"存储的下标
                            int real_index = 0;
                            for (int i = 0, ji = 2, c = 10; i < dimensionList.size(); i++) {
                                int temp = 1;
                                if (dimension_index.get(i) >= dimensionList.get(i)) {
                                    messages.add("第 " + i + " 个数组下标越界!");
                                    return;
                                } else {
                                    // 最后一个维度不能乘
                                    for (int j = i + 1; j < dimensionList.size(); j++)
                                        temp *= dimensionList.get(j);
                                    real_index += dimension_index.get(i) * temp;
                                }
                            }

                            SimpleVariable log = translateExp(O_node.getChildren()[0]);
                            if (log != null) {
//                                if (!v.getType().equals(log.getType())) { TODO 未经过测试
//                                    if (v.getType().equals("int")) {
//                                        // 强制转换
//                                        int val = (int) Double.parseDouble(log.getValue());
//                                        messages.add("类型不匹配，" + log.getValue() + "强制转换为" + val);
//                                        v.getValues().set(real_index, String.valueOf(val));
//                                    } else if (v.getType().equals("real")) {
//                                        double val = Double.parseDouble(log.getValue());
//                                        messages.add("类型不匹配，" + log.getValue() + "自动类型转换为" + val);
//                                        v.getValues().set(real_index, String.valueOf(val));
//                                    }
//                                } else
//                                    v.getValues().set(real_index, log.getValue());

                                SimpleVariable val_variable = typeHandle(new SimpleVariable(null, type, null, level), log);
                                v.getValues().set(real_index, val_variable.getValue());
                                messages.add("数组变量" + identifier + "第" + real_index + "个'物理'位置被赋值为" + v.getValues().get(real_index)
                                        + ",数组当前值为" + v.getValues()); //TODO 修改多维数据的显示方式
                            }

                        }
                    }
                } else {
                    if (type == null) {
                        messages.add("不能用一个数组来赋值!");
                        return;
                    }
                    // 数组初始化 O->{ Y }, 多维的也转成一维的
                    ASTNode Y_node = O_node.getChildren()[1];
                    ArrayList<String> vals;
                    int total = 1; //总的数组内元素数量
                    for (Integer ix : dimension_index)
                        total *= ix;
                    int i = total;
                    if (Y_node.getMaxChildNum() == 0) {
                        vals = new ArrayList<>();
                        // 数组声明为空时，全部赋给初始值
                        if (type.equals("int")) {
                            while (i-- > 0)
                                vals.add(String.valueOf(0));
                        } else if (type.equals("real")) {
                            while (i-- > 0)
                                vals.add(String.valueOf(0.0));
                        } else if (type.equals("char")) {
                            while (i-- > 0)
                                vals.add(String.valueOf('\0'));
                        }
                    } else {
                        // 数组里的值 O->{ Y }
                        ArrayList<SimpleVariable> array_values = translateY(Y_node);
                        vals = convertArray(array_values, type);
                        if (vals.size() > total) {
                            messages.add("用于初始化的数组内元素过多，自动全部赋了初值");
                            vals = new ArrayList<>();
                            if (type.equals("int")) {
                                while (i-- > 0)
                                    vals.add(String.valueOf(0));
                            } else if (type.equals("real")) {
                                while (i-- > 0)
                                    vals.add(String.valueOf(0.0));
                            } else if (type.equals("char"))
                                while (i-- > 0)
                                    vals.add(String.valueOf('\0'));
                        } else {
                            // 数组元素不足时，自动填充初始值
                            messages.add("用于初始化的数组内元素过少，自动用初值填充了剩下的元素");
                            i = vals.size();
                            while (i < total) {
                                vals.add(type.equals("int") ? String.valueOf(0) :
                                        (type.equals("real") ? String.valueOf(0.0) : String.valueOf('\0')));
                                i++;
                            }
                        }
                    }
                    ArrayVariable arrayVariable = new ArrayVariable(identifier, type,
                            dimension_index, vals, level);
                    if (arrayTable.addVariable(arrayVariable))
                        messages.add("数组变量" + identifier + "被声明为" + type +
                                "型并被初始化为 " + arrayVariable.getValues().toString());
                    else
                        //TODO 考虑声明时全部初始化为 初始值
                        messages.add("数组变量" + identifier + "已被声明过,无法进行初始化！");
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
        // 可以在多个数最后加一个 逗号
        if (C_.getMaxChildNum() != 0 && C_.getChildren()[1].getMaxChildNum() != 0) {
            variables.addAll(translateY(C_.getChildren()[1]));
        }
        return variables;
    }

    // TODO logical expression 需要进行短路求值
//    private SimpleVariable translateLogic(ASTNode logic){
//        SimpleVariable log = null;
//
//
//        return log;
//    }

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
                // 栈顶符号的优先级更高或相等（左结合） TODO 短路求值
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
        arith_val = varStack.pop();
        return arith_val;
    }

    // 两个数之间的运算，可以包括 算术、关系和逻辑运算, 字符能参与所有运算，而字符串只能参与 加法运算 TODO mod operand未实现
    private SimpleVariable calculate(SimpleVariable v1, SimpleVariable v2, String top) {
        SimpleVariable reVar = null;
        // 有字符串存在的情况下
        if (v1.getType().equals("string") || v2.getType().equals("string")) {
            if (!top.equals("+")) {
                messages.add(top + " 操作中不能存在字符串,自动返回默认值 空");
                return new SimpleVariable(null, "string", "", level);
            } else {
                // 应该取下标为 0 而不是 1 ， TODO value是否已经除去了 双引号
                String val = v1.getValue() + v2.getValue();
                return new SimpleVariable(v1.getName(), "string", val, level);
            }
        }
        if (!v1.getType().equals("real") && !v2.getType().equals("real")) {
            // 没有 real 存在时，操作类似
            if (v1.getType().equals("char") || v2.getType().equals("char"))
                messages.add("char 类型数" + v1.getValue() + "和" + v2.getValue() + "进行运算时，自动进行类型转换成 int再进行运算");

            int a1 = v1.getType().equals("int") ? Integer.parseInt(v1.getValue()) : (int) v1.getValue().charAt(0);
            int a2 = v2.getType().equals("int") ? Integer.parseInt(v2.getValue()) : (int) v2.getValue().charAt(0);
            if (top.equals("*")) {
                messages.add(a1 + " * " + a2 + " = " + (a1 * a2));
                reVar = new SimpleVariable(v1.getName(), "int", String.valueOf(a1 * a2), level);
            } else if (top.equals("/")) {
                if (a2 == 0) {
                    messages.add("发生除零错误，值自动变为0");
                    reVar = new SimpleVariable(v1.getName(), "int", "0", level);
                } else {
                    messages.add(a1 + " / " + a2 + " = " + (a1 / a2));
                    reVar = new SimpleVariable(v1.getName(), "int", String.valueOf(a1 / a2), level);
                }
            } else if (top.equals("+")) {
                messages.add(a1 + " + " + a2 + " = " + (a1 + a2));
                reVar = new SimpleVariable(v1.getName(), "int", String.valueOf(a1 + a2), level);
            } else if (top.equals("-")) {
                messages.add(a1 + " - " + a2 + " = " + (a1 - a2));
                reVar = new SimpleVariable(v1.getName(), "int", String.valueOf(a1 - a2), level);
            } else {
                // 关系和逻辑运算
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
                else if (top.equals("||")) {
                    val = a1 != 0 ? 1 : 0;
                    if (val == 1) // 物理上的短路求值
                        val = ((a1 != 0) || (a2 != 0)) ? 1 : 0;
                } else if (top.equals("&&")) {
                    val = a1 != 0 ? 1 : 0;
                    if (val == 1) // 物理上的短路求值
                        val = ((a1 != 0) && (a2 != 0)) ? 1 : 0;
                } else
                    messages.add("运算出错！！！");
                messages.add(a1 + top + a2 + " = " + val);
                reVar = new SimpleVariable(v1.getName(), "int", String.valueOf(val), level);
            }

        }// 两者类型中存在 real 的情况
        else {
            // 两个之中有real型存在
            if (v1.getType().equals("real"))
                messages.add("类型" + v1.getType() + "与" + v2.getType() + "不匹配,自动对 " + v2.getValue() + "进行类型转换");
            else
                messages.add("类型" + v1.getType() + "与" + v2.getType() + "不匹配,自动对 " + v1.getValue() + "进行类型转换");

            double a1 = v1.getType().equals("char") ? Double.parseDouble(String.valueOf((int) v1.getValue().charAt(0))) :
                    Double.parseDouble(v1.getValue());
            double a2 = v2.getType().equals("char") ? Double.parseDouble(String.valueOf((int) v2.getValue().charAt(0))) :
                    Double.parseDouble(v2.getValue());

            if (top.equals("*")) {
                messages.add(a1 + " * " + a2 + " = " + (a1 * a2));
                reVar = new SimpleVariable(v1.getName(), "real", String.valueOf(a1 * a2), level);
            } else if (top.equals("/")) {
                if (a2 == 0.0) {
                    messages.add("发生除零错误，值自动变为 0.0");
                    reVar = new SimpleVariable(v1.getName(), "real", "0.0", level);
                } else {
                    messages.add(a1 + " / " + a2 + " = " + (a1 / a2));
                    reVar = new SimpleVariable(v1.getName(), "real", String.valueOf(a1 / a2), level);
                }
            } else if (top.equals("+")) {
                messages.add(a1 + " + " + a2 + " = " + (a1 + a2));
                reVar = new SimpleVariable(v1.getName(), "real", String.valueOf(a1 + a2), level);
            } else if (top.equals("-")) {
                messages.add(a1 + " - " + a2 + " = " + (a1 - a2));
                reVar = new SimpleVariable(v1.getName(), "real", String.valueOf(a1 - a2), level);
            } else {
                //TODO 是否会有其他情况没有考虑到

                // 关系和逻辑运算
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
                else if (top.equals("||")) {
                    val = a1 != 0.0 ? 1 : 0;
                    if (val == 1) // 物理上的短路求值
                        val = ((a1 != 0.0) || (a2 != 0)) ? 1 : 0;
                } else if (top.equals("&&")) {
                    val = a1 != 0.0 ? 1 : 0;
                    if (val == 1) // 物理上的短路求值
                        val = ((a1 != 0.0) && (a2 != 0)) ? 1 : 0;
                } else
                    messages.add("运算出错！！！");
                messages.add(a1 + top + a2 + " = " + val);
                reVar = new SimpleVariable(v1.getName(), "int", String.valueOf(val), level);
            }
        }
        return reVar;
    }

    // "Variable-> ..." TODO 取值的时候没有，就返回默认值（考虑 Null 会给上层带来不便）
    private SimpleVariable translateVariable(ASTNode variable_node) {
        SimpleVariable variable = null;
        if (variable_node.getMaxChildNum() == 1) {
            if (variable_node.getChildren()[0].getName().equals("Digit")) {
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
            } else if (variable_node.getChildren()[0].getName().equals("character")) {
                // character,此处进行词法分析没有进行的 字符长度的检查
                String char_value = variable_node.getChildren()[0].getValue().split("\'")[1];
                if (char_value.length() != 1) {
                    messages.add("字符" + char_value + "长度非法，自动返回默认值 '\\0' ");
                    variable = new SimpleVariable(null, "char", String.valueOf('\0'), level);
                } else
                    variable = new SimpleVariable(null, "char", String.valueOf(char_value), level);
            } else {
                // string
                String val = variable_node.getChildren()[0].getValue().split("\"")[1];
                variable = new SimpleVariable(null, "string", val, level);
            }
        } else if (variable_node.getMaxChildNum() == 2) {

            // "Variable->identifier Call" TODO + id, - id
            ASTNode call_node = variable_node.getChildren()[1];
            String identifier = variable_node.getChildren()[0].getValue();
            if (call_node.getChildren()[0].getName().equals("Index")) {
                ASTNode index_node = call_node.getChildren()[0];
                if (index_node.getMaxChildNum() == 0) {
                    SimpleVariable id = simpleTable.getVar(identifier);
                    if (id == null) {
                        messages.add("变量 " + identifier + "未被声明，无法使用,自动返回默认值 0");
                        //TODO 这些变量是否应该都有名字
                        variable = new SimpleVariable(identifier, "int", "0", level);
                    } else {
                        if (id.getValue() == null) {
                            messages.add("变量 " + identifier + "没有被初始化，无法使用，自动返回默认值 0");
                            variable = new SimpleVariable(identifier, "int", "0", level);
                        } else
                            variable = id;
                    }
                } else {
                    // 数组取下标的值 TODO 不正确则返回 默认值 0
//                    SimpleVariable index = translateExp(index_node.getChildren()[1]); //Logical expression
                    ArrayList<SimpleVariable> dimension_logics = translateIndex(index_node);
                    ArrayList<Integer> dimension_index = new ArrayList<>();// 下标列表
                    // 检查下标是否合法,不合法则自动退出
                    for (SimpleVariable s : dimension_logics) {
                        if (s.getType().equals("real")) {
                            messages.add("取值时数组下标不允许为小数" + s.getValue() + " ，只能为正整数,自动返回默认值 0");
                            variable = new SimpleVariable(null, "int", "0", level);
                            return variable;
                        } else {
                            int ix = Integer.parseInt(s.getValue());
                            if (ix < 0) {
                                messages.add("取值时数组时下标不允许为负数" + s.getValue() + " ，只能为正整数，自动返回默认值 0");
                                variable = new SimpleVariable(null, "int", "0", level);
                                return variable;
                            } else
                                dimension_index.add(ix);
                        }
                    }

                    ArrayVariable arrayVariable = arrayTable.getArray(identifier);
                    if (arrayVariable == null) {
                        messages.add("数组变量" + identifier + "未声明，无法使用，自动返回默认值 0");
                        variable = new SimpleVariable(null, "int", "0", level);
                    } else {
                        // 检测下标越界，未赋值等问题
                        if (arrayVariable.getValues() == null || arrayVariable.getValues().size() == 0) {
                            messages.add("数组" + identifier + "未被赋值，无法使用，自动返回默认值 0");
                            variable = new SimpleVariable(null, "int", "0", level);
                        } else {
                            // 判断下标是否过多
                            if (dimension_index.size() != arrayVariable.getDimensionList().size()) {
                                messages.add("数组下标数量不匹配,无法取数组中的值，自动返回默认值 0");
                                return new SimpleVariable(null, "int", "0", level);
                            }
                            ArrayList<Integer> dimensionList = arrayVariable.getDimensionList();
                            // 判断下标是否越界， 同时计算"物理"存储的下标
                            int real_index = 0;
                            for (int i = 0; i < dimensionList.size(); i++) {
                                int temp = 1;
                                if (dimension_index.get(i) >= dimensionList.get(i)) {
                                    messages.add("第 " + i + " 个数组下标越界!自动返回默认值 0");
                                    return new SimpleVariable(null, "int", "0", level);
                                } else {
                                    for (int j = i + 1; j < dimensionList.size(); j++)
                                        temp *= dimensionList.get(j);
                                    real_index += dimension_index.get(i) * temp;
                                }
                            }
                            ArrayList<String> array = arrayVariable.getValues();
                            // 假设数组里一定有值
                            variable = new SimpleVariable(null, arrayVariable.getType(), array.get(real_index), level);

                        }

                    }
                }
            } else {
                // 函数调用
                ArrayList<SimpleVariable> arguments = translateArgument(call_node.getChildren()[1]);
                FunctionVariable func = functionTable.getVar(identifier);
                if (func == null) {
                    messages.add("函数" + identifier + "未声明无法调用，自动返回默认值 0");
                    variable = new SimpleVariable(null, "int", "0", level);
                } else {
                    ArrayList<Object> parameters = func.getParameters();
                    if (parameters.size() != arguments.size()) {
                        messages.add("函数" + identifier + "调用时参数个数不匹配，自动返回默认值 0");
                        variable = new SimpleVariable(null, "int", "0", level);
                    } else {
                        boolean canExecute = true;
                        for (int i = 0; i < arguments.size(); i++) {
                            if (parameters.get(i) instanceof ArrayVariable) {
                                messages.add("函数" + identifier + "的第" + i + "个参数"
                                        + ((ArrayVariable) parameters.get(i)).getArrayName() + "声明为数组变量，与调用参数类型不匹配，自动返回默认值 0");
                                canExecute = false;
                                variable = new SimpleVariable(null, "int", "0", level);
                                break;
                            }
                            SimpleVariable par = (SimpleVariable) parameters.get(i);
                            SimpleVariable arg = arguments.get(i);
                            // 函数里的局部变量，与参数的名称相同
                            SimpleVariable local = new SimpleVariable(par.getName(), par.getType(), null, level + 1);
                            if (!par.getType().equals(arg.getType())) {
                                if (par.getType().equals("int")) {
                                    // 强制转换
                                    int val = (int) Double.parseDouble(arg.getValue());
                                    messages.add("调用类型不匹配，" + arg.getValue() + "强制转换为" + val);
                                    local.setValue(String.valueOf(val));
                                } else if (par.getType().equals("real")) {
                                    double val = Double.parseDouble(arg.getValue());
                                    messages.add("调用类型不匹配，" + arg.getValue() + "自动类型转换为" + val);
                                    local.setValue(String.valueOf(val));
                                }
                            } else
                                local.setValue(arg.getValue());
                            simpleTable.addVariable(local); // 添加进变量表中，当前的高 level
                        }
                        if (canExecute) {
                            // 执行函数中的程序
                            level++;
                            proNum++; //子程序个数加一
                            messages.add("正在调用函数" + identifier);
                            // 返回类型入栈
                            returnTypeStack.addFirst(func.getType());

                            func.getPro_node().flushFindTag(); // 调用之前就得flush
                            translate(func.getPro_node());
                            proNum--;
                            // 把返回值置入 variable变量中
                            if (returnVal == null) {
                                messages.add("函数调用后没有返回值，自动返回默认值 0");
                                variable = new SimpleVariable(null, "int", "0", level - 1);
                            } else {
                                messages.add("函数调用后返回了值：" + returnVal.getValue());
                                variable = returnVal;
                            }
                            simpleTable.deleteVariable(level);
                            arrayTable.deleteArrays(level);
                            level--;
                        } else {
                            simpleTable.deleteVariable(level + 1);
                            arrayTable.deleteArrays(level + 1);
                        }

                    }
                }

            }
        } else if (variable_node.getMaxChildNum() == 3) {
            // "Variable->( Relation )"
            variable = translateExp(variable_node.getChildren()[1]);
        } else if (variable_node.getMaxChildNum() == 4) {
            ASTNode id = variable_node.getChildren()[0];
            // print函数调用
            if (id.getValue().equals("print")) {
                ASTNode logic = variable_node.getChildren()[2];
                SimpleVariable log = translateExp(logic);
                messages.add("调用了 print函数，在屏幕上输出" + log.getValue() + ",返回默认值 1");
                printList.add(log.getValue()); // 存入输出栈
                System.out.println(log.getValue()); //输出到屏幕上
                variable = new SimpleVariable(null, "int", "1", level);
            }
            // scan函数调用
            else if (id.getValue().equals("scan")) {
                Scanner scanner = new Scanner(System.in);
                // 拿到要赋值的变量 logic expression
                SimpleVariable var = translateExp(variable_node.getChildren()[2]);

                SimpleVariable vvv = simpleTable.getVar(var.getName()); // 拿到表里已经声明的变量
                // TODO 从列表中 pop 读入输入的数据（如从文件中导入大量的输入数据）
                if (vvv != null) {
                    System.out.println("正在执行 scan，开始接受值给变量" + var.getName());
                    Double inp = scanner.nextDouble();
                    if (vvv.getType().equals("int")) {
                        messages.add("scan时强制转换");
                        int i = (int) inp.doubleValue();
                        vvv.setValue(String.valueOf(i));
                    } else
                        vvv.setValue(String.valueOf(inp));
                    messages.add("变量" + var.getName() + "接受并被赋值为" + String.valueOf(inp) + ",返回默认值 1");
                    variable = new SimpleVariable(null, "int", "1", level);
                } else {
                    messages.add("变量" + var.getName() + "未声明，无法scan得到值，返回默认值 0");
                    variable = new SimpleVariable(null, "int", "0", level);
                }
            }
        }
        return variable;
    }

    // TODO 如果要传数组参数的话，就需要文法中实现 指针功能，这里暂定传入的参数为简单变量
    private ArrayList<SimpleVariable> translateArgument(ASTNode argument) {
        ArrayList<SimpleVariable> args = new ArrayList<>();
        if (argument.getMaxChildNum() != 0) {
            ASTNode logic = argument.getChildren()[0];
            SimpleVariable log = translateExp(logic);
            args.add(log);
            if (argument.getChildren()[1].getMaxChildNum() != 0) // CCC->, Argument
                args.addAll(translateArgument(argument.getChildren()[1].getChildren()[1]));
        }
        return args;
    }

    //把变量列表转成 Value的 String列表，可以检查变量类型是否匹配，进行自动转换和强制转换，并转换原始的值
    private ArrayList<String> convertArray(ArrayList<SimpleVariable> arrayList, String type) {
        // 这里数组里值的类型都是 match type 的
        ArrayList<String> list = new ArrayList<>();
        for (SimpleVariable var : arrayList)
            list.add(typeHandle(new SimpleVariable(null, type, null, level), var).getValue());
        return list;
    }

    private static void testWhileIf() {
        Translator t = new Translator();
        String pro = "char po[2][2] = \"12k\";";

        Wrapper w = Executor.analyze(pro);
        t.simpleTable.addVariable(new SimpleVariable("p", "int", "0", 1));
        ArrayList<String> values = new ArrayList<>();
        values.add("43");
        values.add("90");
        t.arrayTable.addVariable(new ArrayVariable("p2", "int", 2, values, 1));

        List<String> msg = w.getMessages();
        for (String m : msg)
            System.out.println(m);
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
