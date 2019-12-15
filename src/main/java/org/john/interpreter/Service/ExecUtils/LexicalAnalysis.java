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

    private static boolean isHex(char ch) {
        return isDigit(ch) || ch >= 'a' && ch <= 'f' || ch >= 'A' && ch <= 'F';
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
        // char字符和str字符串的判定
        if (ch == '\'' || ch == '\"') {
            token[i++] = ch;
            char left = ch;
            ch = pro[p++];
            // 扫描任何除了该引号之外的字符，同时注意程序字符数组可能溢出
            while (ch != '\n' && ch != left && p < pro.length) {
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
        //检测到符号
        else if (signList.contains(String.valueOf(ch)) || ch == '\\') {
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
                //判断是否以下划线结尾,可能会有 AB_12_a 这样的标识符,这里 p 就是向后看了一位
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
            int mode = 0;
            if (ch == '0' && (pro[p] == 'x' || pro[p] == 'X')) {
                mode = 2;
                // 十六进制数
                token[i++] = ch; //存入 0
                token[i++] = pro[p++]; // 存入 x or X
                ch = pro[p++]; // 下一个字符
                if (!isHex(ch)) // 后面不跟着数字的话，报错
                    mode = -1;
                else
                    while (isHex(ch)) {
                        token[i++] = ch;
                        ch = pro[p++];
                    }
            } else {
                //跟着数字或者数字开头
                while (isDigit(ch) || ch == '.') {
                    //圆点数量超过规定
                    if (ch == '.' && mode == 1)
                        break;
                    else {
                        if (ch == '.')
                            mode = 1;
                        token[i++] = ch;
                        ch = pro[p++];
                    }
                }
                if (ch == 'e' || ch == 'E') {
                    token[i++] = ch;
                    ch = pro[p++];
                    if (ch == '-') { // 考虑之后接着负号
                        token[i++] = ch;
                        ch = pro[p++];
                    }
                    // 后面不跟着数字，就报错
                    if (!isDigit(ch))
                        mode = -1;
                    while (isDigit(ch)) {
                        token[i++] = ch;
                        ch = pro[p++];
                    }
                    if (mode != -1)
                        mode = 1;
                }
            }
            p--;
            if (mode == 2)
                special = "hexadecimal"; //识别为十六进制数
            else if (mode == 1)
                special = "fraction"; //识别为小数
            else if (mode == 0)
                special = "integer"; //识别为整数
            else
                special = "error";
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
        Integer code = str2Code.get(special);
        Integer code_trim = str2Code.get(special.trim());
        return new LexiNode(String.valueOf(newArray), str2Code.get(special), row, col, p);
    }

    // 完整的扫描一整个程序
    public static List<LexiNode> lexicalScan(String pro) {
        if (pro == null)
            return null;
        while (pro.endsWith("\n")) //除去最后的换行
            pro = pro.substring(0, pro.length() - 1);
        pro += " ";     // 防止 indexOutOfBound
        char[] program = pro.toCharArray();
        List<LexiNode> nodes = new ArrayList<>();
        HashMap<String, Integer> str2Code = str2IntMap();
        //起始节点
        LexiNode node = new LexiNode("", -1, 1, 1, 0);

        boolean singleCom = false;
        boolean multiCom = false;
        do {
            node = oneScan(program, node); //单词扫描

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
                nodes.add(node);
        } while (node.getP() < pro.length() - 1);
        if (node.getSymbol().equals("")) //除去最后可能出现的空格
            nodes.remove(node);
        return nodes;
    }

    public static List<LexiNode> preprocess(List<LexiNode> nodes) {
        LexiNode[] nodeArray = nodes.toArray(new LexiNode[0]);
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
        String b = "9e20"; // 不能直接转化 hexadecimal
        String a = "0x12";
//        int k = Integer.parseInt(a);
        double k = Double.parseDouble(b);

        System.err.println(k);

        String pro = "char c = \"qweqe''''wqeq\";char";

        List<LexiNode> nodes = lexicalScan(pro);
        System.out.println(nodes);

    }

}

