package org.john.interpreter.Service.ExecUtils;


import org.john.interpreter.Service.LLUtils.LLDrive;
import org.springframework.util.ResourceUtils;

import java.io.FileWriter;
import java.util.*;

import static org.john.interpreter.Service.ExecUtils.CodeTable.*;

public class GramParser {

    private LLDrive llDrive;
    private int[][] llTable;
    private HashMap<Integer, String> int2StrMap = int2StrMap();
    private LinkedList<String> stack = new LinkedList<>(); //分析符号栈
    private LinkedList<String> errorStack = new LinkedList<>(); // 错误信息栈

    private LinkedList<String> matchStack = new LinkedList<>(); //括号匹配栈

    public GramParser() {
        try {
            llDrive = new LLDrive(productions);
            llTable = llDrive.getTable();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public LinkedList<String> getErrorStack() {
        return errorStack;
    }

    //错误代码
    public static final int IDerror = -3; //标识符的错误编码

    // LL 分析过程
    public ASTNode LLParse(List<LexiNode> nodes) {
        try {
            boolean legal = true;

            //初始化栈中有一个 开始符号 元素 + # 号
            stack.addFirst("Pro");
            stack.add("#");

            // 当前执行操作的语法树节点
            ASTNode curNode = null;
            //根节点
            ASTNode rootNode = null;
            int childNum;

            String top, symbol; //栈顶指针，扫描到的token
            int y, x, pos; //LL分析表的 行和列位置
            int index = 0; //扫描的索引
            LexiNode node;
            List<String> symList = signList.subList(0,12);
            while (index < nodes.size()) {

                node = nodes.get(index);
                top = stack.pop();

                while (node.getCode() == -1) {
                    legal = false;
                    errorStack.add("第" + node.getRow() + "行，第" + node.getCol() + "列 " + node.getSymbol() +" 出现无法识别的 token");
                    index++;
                    if (index >= nodes.size())
                        break;
                    node = nodes.get(index); // 忽略掉无法识别的 token
                }
                if (index >= nodes.size())
                    break;

                symbol = int2StrMap.get(nodes.get(index).getCode());
                //判断为终结符号
                if (top.charAt(0) < 'A' || top.charAt(0) > 'Z') {
                    //继续扫描
                    if (top.equals(symbol)) {
                        index++;
                        if (symbol.equals("#"))
                            continue;
                        curNode = curNode.findLefted();

                        if (symList.contains(top) && !curNode.getName().equals("Digit")) // 为运算符
                            curNode.addChild(new ASTNode(0,"symbol",top,true,true));
                        else
                            curNode.addChild(new ASTNode(0, top, node.getSymbol(), true, true));

//                        if (Arrays.asList(value_contain_token).contains(top))
//                            curNode.addChild(new ASTNode(0, top, node.getSymbol(), true, true));
//                        else
//                            curNode.addChild(new ASTNode(0, top, true, true));

                    } else {
                        legal = false;
                        if (top.equals("#"))
                            //栈内已经为空，无法继续扫描了
                            break; // nearest namely while loop

                        //自动加上这个缺少的符号 / 跳过该符号---
                        //index++;
                        curNode = curNode.findLefted();
                        curNode.addChild(new ASTNode(0, top, true, false));

                        errorStack.add("第" + node.getRow() + "行,第" + node.getCol() + "列处 " + node.getSymbol() +" 出现语法错误,缺少 " + top);

                    }
                    updateMatch(top); //更新匹配栈 TODO 丢掉一个语句的时候，该怎么处理这个栈---

                } else { //为非终结符号
                    y = llDrive.getNtMap().get(top);
                    x = llDrive.gettMap().get(symbol);
                    pos = llTable[y][x];

                    if (pos != -1) {
                        if (pos == -2) {
                            /* 特殊情况下矛盾产生式的选择, 标识符作为 Select集矛盾的地方
                             * 扫描分号之前遇到的符号，遇到 逻辑符、关系符、运算符前遇到 赋值符号则选择 赋值语句的产生式，否则反之*/
                            int t_index = index;
                            LexiNode temp = nodes.get(t_index++);
                            String[] p1 = {"||", "&&", "<", "<=", "<>", ">", ">=", "==", "+", "-", "*", "/", "(", ")"};
                            String selection = null;
                            int mode = top.equals("Statement") ? 0:1;
                            while (!temp.getSymbol().trim().equals(";")) {
                                for (String p : p1) {
                                    if (p.equals(temp.getSymbol().trim())) {
                                        selection = special_production[mode * 2];
                                        break;
                                    }
                                }
                                if (selection != null)
                                    break;
                                if ("=".equals(temp.getSymbol().trim())) {
                                    selection = special_production[mode * 2 + 1];
                                    break;
                                }

                                temp = nodes.get(t_index++);
                            }
                            if (selection == null)
                                selection = special_production[mode * 2];
                            pos = Arrays.asList(productions).indexOf(selection);
                        }
                        childNum = llDrive.addToStack(pos, stack);
                        if (rootNode == null) {
                            rootNode = new ASTNode(childNum, "Pro", false, true);
                            curNode = rootNode;
                        } else {
                            curNode = curNode.findLefted(); //找到还剩孩子节点没连上的节点
                            curNode.addChild(new ASTNode(childNum, top, false, true));
                        }
                    } else {
                        //TODO  没有 "P" 未进入栈时程序出错的情况
                        if (node.getCode() == -2) { // "#" 号
                            legal = false;
                            if (!errorStack.contains("缺少结束符!"))
                                errorStack.add("缺少结束符!"); //TODO handle undone
                        } else {
                            if (rootNode == null) {
                                // manually handle stack
                                rootNode = new ASTNode(2, "Pro", false, true);
                                stack.add("Statement");
                                stack.add("Pro");
                                curNode = rootNode;
                            }

                            // 丢掉出错的语句 S，继续下一条语句的语法分析
                            // 节点搜索到 S 的 First集，栈一直pop 直到遇到 Pro（下一条语句的开始）
                            legal = false;
                            errorStack.add("第" + node.getRow() + "行,第" + node.getCol() + "列处 " + node.getSymbol() + " 出现语法错误," + errorHandle(top, symbol));

                            // 1.栈的丢弃
                            while (!top.equals("Pro")) {  // "#" 还是 "Pro" ?
                                curNode = curNode.findLefted(); // TODO Handle NullPointerException
                                curNode.addChild(new ASTNode(0, top, false, false));
                                top = stack.pop();
                            }
                            // 此时找到了 P，但由于返回循环时会再 pop一次，故需要回退
                            stack.addFirst("Pro");
                            // 2.词法分析节点的丢弃,找到 S的 first集中的词法单元
                            List<String> stateStart = Arrays.asList(llDrive.getFirstMap().get("Pro").split(" ")); //改成了Pro的First集
                            while (!stateStart.contains(symbol) && !symbol.equals("#")) {
                                index++;
                                if (index >= nodes.size())
                                    break;
                                symbol = int2StrMap.get(nodes.get(index).getCode());
                            }
                            // 此时已经找到起始符号了，且返回循环时不增加 index，故不用回退
                        }
                    }

                }
            }
            //分析完成后，栈中还有元素
            if (stack.size() > 0) {
                errorStack.add("缺少结束符!");
            }
            if (legal)
                System.out.println("\n语法分析成功！！！\n");
            else
                System.out.println("语法分析失败，错误如下：");

            Set<String> set = new LinkedHashSet<>(errorStack);
            errorStack = new LinkedList<>(set);

            //输出错误栈中的内容
            for (String error : errorStack) {
                System.out.println(error);
            }

            //输出 AST
            System.out.println(rootNode.toJSON());

            //写入文件
            String prefix = ResourceUtils.getFile("classpath:others").getAbsolutePath();
            FileWriter fileWriter = new FileWriter(prefix + "/GramOutput.txt");
            fileWriter.write(rootNode.toJSON());
            fileWriter.flush();
            fileWriter.close();

            return rootNode;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    // 括号映射
    public String parMap(String symbol) {
        if (symbol.equals("("))
            return ")";
        else if (symbol.equals("["))
            return "]";
        else
            return "}";
    }

    public void updateMatch(String top) {
        if (top.equals("(") || top.equals("[") || top.equals("{"))
            matchStack.addFirst(top);
        else if (top.equals(")") || top.equals("]") || top.equals("}")) {
            if (matchStack.size() != 0)
                matchStack.pop(); // TODO Handle matchStack.size == 0 Error !!!
        }
    }

    // 非终结符 -> 终结符 报错时进入，利用近似穷举的方法报错较具体的错误
    private String errorHandle(String top, String symbol) {
        List<String> stateStart = Arrays.asList(llDrive.getFirstMap().get("Statement").split(" "));

        // tune the priority
        if (symbol.equals(",")) {
            return "缺少 标识符";
        } else if (stateStart.contains(symbol) && !symbol.equals(";")) {
//            if (top == 'B' || top == '') 更细致的划分
            return "缺少 ;";
        } else if (matchStack.size() != 0) {
            String src = matchStack.pop();
//            matchStack.addFirst(src); //重新加回来
            return "缺少 " + parMap(src);
        } else if (symbol.equals(";"))
            return "缺少 表达式";
        else
            return "其他错误";
    }

    public static void main(String[] args) {

    }
}