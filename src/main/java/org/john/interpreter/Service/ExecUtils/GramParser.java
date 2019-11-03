package org.john.interpreter.Service.ExecUtils;


import org.john.interpreter.Service.LLUtils.LLDrive;

import java.io.FileWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import static org.john.interpreter.Service.ExecUtils.CodeTable.*;

public class GramParser {

    private LLDrive llDrive;
    private int[][] llTable;
    private HashMap<String, Character> str2CharMap = str2CharMap();
    private HashMap<Integer, String> int2StrMap = int2StrMap();
    LinkedList<Character> stack = new LinkedList<>(); //分析符号栈
    LinkedList<String> errorStack = new LinkedList<>(); // 错误信息栈

    LinkedList<Character> matchStack = new LinkedList<>(); //括号匹配栈

    public GramParser() {
        try {
            llDrive = new LLDrive(production);
            llTable = llDrive.getTable();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //错误代码
    public static final int IDerror = -3; //标识符的错误编码

    // LL 分析过程
    public ASTNode LLParse(List<LexiNode> nodes) {
        try {
            boolean legal = true;

            //初始化栈中有一个 开始符号 元素 + # 号
            stack.addFirst('P');
            stack.add('#');

            // 当前执行操作的语法树节点
            ASTNode curNode = null;
            //根节点
            ASTNode rootNode = null;
            int childNum;

            Character top, symbol; //栈顶指针，扫描到的token
            int y, x, pos; //LL分析表的 行和列位置
            int index = 0; //扫描的索引
            LexiNode node;
            while (index < nodes.size()) {

                node = nodes.get(index);
                symbol = str2CharMap.get(int2StrMap.get(node.getCode()));
                top = stack.pop();

                if (node.getCode() == -1) {
                    legal = false;
                    throw new Exception("(" + node.getRow() + "," + node.getCol() + ")处出现词法分析出错");
                }
                //判断为终结符号
                if (top < 'A' || top > 'Z') {
                    //继续扫描
                    if (top == symbol) {
                        index++;
                        if (symbol == '#')
                            continue;
                        curNode = curNode.findLefted();
                        curNode.addChild(new ASTNode(0, top, true,true));

                    } else {
                        legal = false;
                        if (top == '#')
                            //栈内已经为空，无法继续扫描了
                            break; // nearest namely while loop

                        //TODO 自动加上这个缺少的符号 / 跳过该符号
                        //index++;
                        curNode = curNode.findLefted();
                        curNode.addChild(new ASTNode(0,top,true,false));

                        errorStack.add("(" + node.getRow() + "," + node.getCol() + ")处出现语法错误,缺少 " + top);

                    }
                    updateMatch(top); //更新匹配栈 TODO 丢掉一个语句的时候，该怎么处理这个栈---

                } else { //为非终结符号
                    y = llDrive.getYcharMap().get(top);
                    x = llDrive.getXcharMap().get(symbol);
                    pos = llTable[y][x];

                    if (pos != -1) {
                        childNum = llDrive.addStack(pos, stack);
                        if (rootNode == null) {
                            rootNode = new ASTNode(childNum, 'P', false,true);
                            curNode = rootNode;
                        } else {
                            curNode = curNode.findLefted(); //找到还剩孩子节点没连上的节点
                            curNode.addChild(new ASTNode(childNum, top, false,true));
                        }
                    } else {
                        if (node.getCode() == -2) // '#'号
                            throw new Exception("缺少结束符");
                        else{
                            // 丢掉出错的语句 S，继续下一条语句的语法分析
                            // 节点搜索到 S 的 First集，栈一直pop 直到遇到 p（下一条语句的开始）
                            legal = false;
                            errorStack.add("(" + node.getRow() + "," + node.getCol() + ")处出现语法错误," + errorHandle(top,symbol));

                            // 1.栈的丢弃
                            while (top != 'P') {
                                curNode = curNode.findLefted(); // TODO Handle NullPointerException
                                if (curNode != null)
                                    curNode.addChild(new ASTNode(0, top, false, false));

                                top = stack.pop();
                            }
                            // 此时找到了 P，但由于返回循环时会再 pop一次，故需要回退
                            stack.addFirst('P');

                            // 2.词法分析节点的丢弃,找到 S的 first集中的词法单元
                            List<String> stateStart = Arrays.asList(llDrive.getFirstMap().get('S').split(","));

                            while (!stateStart.contains(symbol.toString())){
                                index++;
                                if (index >= nodes.size())
                                    continue;
                                symbol = str2CharMap.get(int2StrMap.get(nodes.get(index).getCode()));
                            }
                            // 此时已经找到起始符号了，且返回循环时不增加 index，故不用回退
                        }
                    }

                }
            }
            //分析完成后，栈中还有元素
            if (stack.size() > 0)
                throw new Exception("缺少结束符");

            if (legal)
                System.out.println("\n语法分析成功！！！\n");
            else
                System.out.println("语法分析失败，错误如下：");
            //输出错误栈中的内容
            for (String error:errorStack){
                System.out.println(error);
            }

            //输出 AST
            System.out.println(rootNode.toJSON());

            //写入文件
//            FileWriter fileWriter = new FileWriter("./AST.txt");
//            fileWriter.write(rootNode.toJSON());
//            fileWriter.flush();
//            fileWriter.close();
            return rootNode;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    private String formatOuput(ASTNode rootNode){

        return null;
    }

    // 括号映射
    public char parMap(char symbol){
        if (symbol == '(')
            return ')';
        else if (symbol == '[')
            return ']';
        else
            return '}';
    }

    public void updateMatch(char top){
        if (top == '(' || top == '[' || top == '{')
            matchStack.addFirst(top);
        else if (top == ')' || top == ']' || top == '}')
            matchStack.pop(); // 似乎用不到 canMatch函数

    }

    // 非终结符 -> 终结符 报错时进入，利用近似穷举的方法报错较具体的错误
    private String errorHandle(char top,char symbol){
        List<String> stateStart = Arrays.asList(llDrive.getFirstMap().get('S').split(","));

        if (matchStack.size() != 0){
            Character src = matchStack.pop();
            return "缺少 " + parMap(src);
        }
        else if (stateStart.contains(String.valueOf(symbol)) && symbol != ';'){
//            if (top == 'B' || top == '') 更细致的划分
            return "缺少 ;";
        }else if (symbol == ','){
            return "缺少 标识符";
        }else if (symbol == ';')
            return "缺少 表达式";
        else
            return "其他错误";
    }

    public static void main(String[] args) {

    }
}