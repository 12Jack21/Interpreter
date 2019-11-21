package org.john.interpreter.Service.ExecUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.john.interpreter.Service.ExecUtils.CodeTable.*;

public class LexicalAnalysis {

    public LexicalAnalysis() {

    }

    //是否为符号或者 下划线
    private static boolean isLetter(char ch) {
        return (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || ch == '_';
    }

    private static boolean isDigit(char ch) {
        return ch >= '0' && ch <= '9';
    }

    private static LexiNode oneScan(char[] pro, LexiNode node) {
        HashMap<String, Integer> str2Code = str2IntMap();
        int p = node.getP(), i = 0;
        char[] token = new char[200];
        char ch = pro[p++];
        String special = null;

        //判断换行和 \t 符号的特殊条件
        int row = node.getSymbol().equals("\n\0") ? node.getRow() + 1 : node.getRow();
        int col = node.getSymbol().equals("\t\0") ? node.getCol() + 4 : node.getCol() + node.getLength();
        if (row == node.getRow() + 1)
            col = 1; //新的一行

        //去掉空格, tab
        while (ch == ' ') {
            col++;
            ch = pro[p++];
            if (ch == ' ')
                ch = pro[p++];
        }

        // char字符的判定
        if (ch == '\'' || ch == '\"') {
            token[i++] = ch;
            ch = pro[p++];
            while (ch != '\n' && ch != '\'' && ch != '\"' && p < pro.length) {
                token[i++] = ch;
                ch = pro[p++];
            }
            if (p >= pro.length)
                special = "error"; //解析到头了
            else {
                if (ch == '\n') {
                    special = "error"; //没有右引号直接报错
                    p--;
                } else if (ch == '\"') {
                    token[i] = '\"';
                    special = "string";
                } else {
                    token[i] = '\''; //到语义分析再处理字符过长的情况
                    special = "character";
                }
            }
        }
        //检测到符号 (最高优先级的判定),加号和减号单独拿出来考虑
        else if (ch == '|' || ch == '&' || signList.contains(String.valueOf(ch)) || ch == '\\') {
            token[i++] = ch;
            ch = pro[p++]; //拿到扫描的起始符号
            token[i++] = ch;

            String a = String.valueOf(token);
            boolean containInv = false;

            for (char inv : invs) {
                if (String.valueOf(token).contains(Character.toString(inv)))
                    containInv = true;
            }
            //如果还包含该符号 ,例如 <= , == 等
            if (containInv || !signList.contains(String.valueOf(token).trim())) {
                p--;
                token[i - 1] = '\0'; //回退
            }
        }
        //标识符的判定规则： 由数字、字母和下划线组成的串，但必须以字母开头、且不能以下划线结尾的串
        else if (isLetter(ch) && ch != '_') {
            token[i++] = ch;
            ch = pro[p++];

            while (isLetter(ch) || isDigit(ch)) {
                //判断是否以下划线结尾,可能会有 AB_12_a 这样的标识符,这里 p 就是向前看了一位
                if (ch == '_' && !isLetter(pro[p]) && !isDigit(pro[p]))
                    break;
                token[i++] = ch;
                ch = pro[p++];
            }
            p--; //多读了一位（超前搜索）

            //不为关键字, 即为标识符
            if (!keyList.contains(String.valueOf(token).trim()))
                special = "identifier";

        } else if (isDigit(ch)) { //数字的判定
            boolean hasDot = false;
            boolean hasSign = false;
            boolean legal = true;

            //跟着数字或者数字开头
            while (isDigit(ch) || ch == '.') {
                if (ch == '.' && hasDot) //圆点数量超过规定
                    break;
                else {
                    if (ch == '.')
                        hasDot = true;
                    token[i++] = ch;
                    ch = pro[p++];
                }
            }
            p--;
            if (hasDot)
                special = "fraction"; //识别为小数
            else
                special = "integer"; //识别为整数

        } else {
            token[i] = ch;
            special = "error"; // 识别为 错误
        }

        int newLength = token[i] == '\0' ? i : i + 1;
        char[] newArray = Arrays.copyOf(token, newLength);
        boolean isInv = false;

        //special 变成通用字符串
        if (special == null)
            special = String.valueOf(token);
        //判断是否为转义字符
        char first = special.length() == 0 ? '\0' : special.charAt(0);
        for (char inv : invs) {
            if (first == inv) {
                special = String.valueOf(first);
                isInv = true;
                break;
            }
        }
        if (!isInv)
            special = special.trim();

        // for debug...
        Integer code2 = str2Code.get(special.trim());
        Integer code = str2Code.get(special);
        return new LexiNode(String.valueOf(newArray), str2Code.get(special), row, col, p);
    }

    // 完整的扫描一整个程序
    public static List<LexiNode> lexicalScan(String pro) {
        if (pro == null)
            return null;
        while (pro.endsWith("\n")) //除去最后的换行
            pro = pro.substring(0, pro.length() - 1);
        pro += " ";     //TODO 防止 indexOutOfBound
        List<LexiNode> nodes = new ArrayList<>();
        HashMap<String, Integer> str2Code = str2IntMap();
        //起始节点
        LexiNode node = new LexiNode("", -1, 1, 1, 0);

        boolean singleCom = false;
        boolean multiCom = false;
        do {
            node = oneScan(pro.toCharArray(), node); //单词扫描

            // 注释的判定
            if (node.getSymbol().equals("//"))
                singleCom = true;
            else if (node.getCode() == str2Code.get("\n"))
                singleCom = false;
            else if (node.getSymbol().equals("/*"))
                multiCom = true;
            else if (node.getSymbol().equals("*/"))
                multiCom = false;

            if (!singleCom && !multiCom && !node.getSymbol().equals("*/"))
                nodes.add(node); // 浅拷贝?
        } while (node.getP() < pro.length() - 1);
        if (node.getSymbol().equals(""))
            nodes.remove(node);
        return nodes;
    }

    public static List<LexiNode> preprocess(List<LexiNode> nodes) {
        LexiNode[] nodeArray = nodes.toArray(new LexiNode[nodes.size()]);
        // remove inverse meaning token
        for (LexiNode node : nodeArray) {
            if (node.getLength() == 0) {
                // '\n' || '\r' || '\t'
                nodes.remove(node);
            }
        }
        nodes.add(new LexiNode("#", -2, -1, -1, -1));
        return nodes;
    }

    public static void main(String[] args) {
    }

}

