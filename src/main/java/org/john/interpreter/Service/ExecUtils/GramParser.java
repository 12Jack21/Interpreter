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

    // LL 分析过程
    public ASTNode LLParse(List<LexiNode> nodes) {
        try {
            boolean legal = true;

            //初始化栈中有一个 开始符号 和 #号
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
            List<String> symList = signList.subList(0, signList.indexOf("&") + 1);
            while (index < nodes.size()) {
                node = nodes.get(index);
                top = stack.pop();
                while (node.getCode() == -1) {
                    legal = false;
                    errorStack.add("第" + node.getRow() + "行，第" + node.getCol() + "列 " +
                            node.getSymbol() + " 出现无法识别的 token");
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
                        if (symList.contains(top) && !curNode.getName().equals("Digit") && !curNode.getName().equals("SymbolVar")) // 为双目运算符
                            curNode.addChild(new ASTNode(0, "symbol", top, true, true));
                        else
                            curNode.addChild(new ASTNode(0, top, node.getSymbol().trim(), true, true));
                    } else {
                        legal = false;
                        if (top.equals("#")) { //栈内已经为空，无法继续扫描了
                            errorStack.add("无效的结束符！");
                            break; // nearest namely the upper while loop
                        }
                        //自动加上这个缺少的符号
                        curNode = curNode.findLefted();
                        curNode.addChild(new ASTNode(0, top, true, false));
                        errorStack.add("第" + node.getRow() + "行,第" + node.getCol() + "列处 " + node.getSymbol() + " 出现语法错误,缺少 " + top);
                    }
                    updateMatch(top); //更新匹配栈                           TODO 丢掉一个语句的时候，该怎么处理这个栈---

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
                            String[] pSym = {"||", "&&", "<", "<=", "<>", ">", ">=", "==", "+", "-", "*", "/", "(", ")","|","&","^","~"};
                            String selection = null;
                            int mode = top.equals("Statement") ? 0 : (top.equals("ELSEIF") ? 1 : (top.equals("Variable") ? 2 : 3));

                            if (mode == 1) {
                                // else if 产生式 判断下一个符号是否是 if 的同时要判定当前是不是 else,否则可能有 if() sta } 类似的情况
                                if (temp.getSymbol().equals("else") && nodes.get(t_index).getSymbol().equals("if"))
                                    selection = special_production[mode * 2];
                                else
                                    selection = special_production[mode * 2 + 1];
                            } else if (mode == 2) {
                                // Variable产生式，看下一个token是identifier还是 integer or fraction
                                temp = nodes.get(t_index);
                                if (int2StrMap.get(temp.getCode()).equals("identifier"))
                                    selection = special_production[mode * 2];
                                else
                                    selection = special_production[mode * 2 + 1];
                            } else if (mode == 0) {
                                // Statement 产生式
                                while (!temp.getSymbol().trim().equals(";") && t_index <= nodes.size()) {
                                    for (String p : pSym) {
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
                            } else {
                                // Con 多重赋值产生式，看后面的赋值符号的数量 1 or 2
                                int assCount = 0;
                                List<String> diffSymbol = Arrays.asList(pSym);// 用以区分的符号，只有表达式中才会出现
                                while (!temp.getSymbol().trim().equals(";") && !temp.getSymbol().trim().equals(",")
                                        && !diffSymbol.contains(temp.getSymbol().trim())) {
                                    if ("=".equals(temp.getSymbol().trim()))
                                        assCount++;
                                    if (assCount >= 2)
                                        break;
                                    temp = nodes.get(t_index++);
                                }
                                selection = assCount >= 2 ? special_production[mode * 2] : special_production[mode * 2 + 1];
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
                        if (node.getCode() == -2) { // "#" 号
                            legal = false;
                            if (!errorStack.contains("缺少结束符!"))
                                errorStack.add("缺少结束符!");               //TODO handle undone
                        } else {
                            if (rootNode == null) {
                                // manually handle stack
                                rootNode = new ASTNode(2, "Pro", false, true);
                                stack.add("Statement");
                                stack.add("Pro");
                                curNode = rootNode;
                            }

                            // 丢掉出错的语句 Statement，继续下一条语句的语法分析
                            // 节点搜索到 Pro 的 First集，栈一直pop 直到遇到 Pro（下一条语句的开始）
                            legal = false;
                            errorStack.add("第" + node.getRow() + "行,第" + node.getCol() + "列处 " +
                                    node.getSymbol() + " 出现语法错误," + errorHandle(top, symbol));
                            // 1.栈的丢弃
                            while (!top.equals("Pro")) {  // "#" 还是 "Pro" ?
                                curNode = curNode.findLefted();// TODO handle some NullPointerException
                                curNode.addChild(new ASTNode(0, top, false, false));
                                top = stack.pop();
                            }
                            // 此时找到了 Pro，但由于返回循环时会再 pop一次，故需要回退
                            stack.addFirst("Pro");
                            // 2.词法单元Token的丢弃,找到 Pro 的First集中的词法单元
                            List<String> stateStart = Arrays.asList(llDrive.getFirstMap().get("Pro").split(" ")); //改成了Pro的First集
                            symbol = int2StrMap.get(nodes.get(++index).getCode()); // 至少丢掉一个Token 防止进入死循环
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
        else if (symbol.equals("{"))
            return "}";
        else
            return symbol; // ' and " are both the same in map
    }

    public void updateMatch(String top) {
        if (top.equals("(") || top.equals("[") || top.equals("{") ||
                top.equals("\'") && !matchStack.contains("\'") || top.equals("\"") && !matchStack.contains("\""))
            matchStack.addFirst(top);
            // seem to be not necessary to handle error here
        else if (top.equals(")") || top.equals("]") || top.equals("}") ||
                top.equals("\'") || top.equals("\"")) {
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
        } else if (symbol.equals(";"))
            return "缺少 表达式";
        else if (matchStack.size() != 0) {
            String src = matchStack.pop();
//            matchStack.addFirst(src); //重新加回来
            return "缺少 " + parMap(src);
        }
        else
            return "其他错误";
    }

    public static void main(String[] args) {
        int a = 1, b = 2, c = 3;
        int c1 = a = b = 2 + 1 + 2;
        System.out.println(a);
        System.out.println(b);
        System.out.println(c1);
    }
}