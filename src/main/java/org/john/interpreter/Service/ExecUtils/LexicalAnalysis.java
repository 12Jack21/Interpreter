package org.john.interpreter.Service.ExecUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.john.interpreter.Service.ExecUtils.CodeTable.*;

public class LexicalAnalysis {

    public LexicalAnalysis() {
    }

    //�Ƿ�Ϊ���Ż��� �»���
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

        //�жϻ��к� \t ���ŵ���������
        int row = node.getSymbol().equals("\n\0") ? node.getRow() + 1 : node.getRow();
        int col = node.getSymbol().equals("\t\0") ? node.getCol() + 4 : node.getCol() + node.getLength();
        if (row == node.getRow() + 1)
            col = 1; //�µ�һ��
        //ȥ���ո�, tab
        while (ch == ' ') {
            col++;
            ch = pro[p++];
            if (ch == ' ')
                ch = pro[p++];
        }
        // char�ַ���str�ַ������ж�
        if (ch == '\'' || ch == '\"') {
            token[i++] = ch;
            char left = ch;
            ch = pro[p++];
            // ɨ���κγ��˸�����֮����ַ���ͬʱע������ַ�����������
            while (ch != '\n' && ch != left && p < pro.length) {
                token[i++] = ch;
                ch = pro[p++];
            }
            if (p >= pro.length)
                special = "error"; //������ͷ��
            else {
                if (ch == '\n') {
                    special = "error"; //û��������ֱ�ӱ���
                    p--;
                } else if (ch == '\"') {
                    token[i] = '\"';
                    special = "string";
                } else {
                    token[i] = '\''; //����������ٴ����ַ����������
                    special = "character";
                }
            }
        }
        //��⵽����
        else if (signList.contains(String.valueOf(ch)) || ch == '\\') {
            token[i++] = ch;
            ch = pro[p++]; //�õ�ɨ�����ʼ����
            token[i++] = ch;

            String a = String.valueOf(token);
            boolean containInv = false;
            for (char inv : invs) {
                if (String.valueOf(token).contains(Character.toString(inv)))
                    containInv = true;
            }
            //����������÷��� ,���� <= , == ��
            if (containInv || !signList.contains(String.valueOf(token).trim())) {
                p--;
                token[i - 1] = '\0'; //����
            }
        }
        //��ʶ�����ж����� �����֡���ĸ���»�����ɵĴ�������������ĸ��ͷ���Ҳ������»��߽�β�Ĵ�
        else if (isLetter(ch) && ch != '_') {
            token[i++] = ch;
            ch = pro[p++];
            while (isLetter(ch) || isDigit(ch)) {
                //�ж��Ƿ����»��߽�β,���ܻ��� AB_12_a �����ı�ʶ��,���� p ���������һλ
                if (ch == '_' && !isLetter(pro[p]) && !isDigit(pro[p]))
                    break;
                token[i++] = ch;
                ch = pro[p++];
            }
            p--; //�����һλ����ǰ������
            //��Ϊ�ؼ���, ��Ϊ��ʶ��
            if (!keyList.contains(String.valueOf(token).trim()))
                special = "identifier";
        } else if (isDigit(ch)) { //���ֵ��ж�
            int mode = 0;
            if (ch == '0' && (pro[p] == 'x' || pro[p] == 'X')) {
                mode = 2;
                // ʮ��������
                token[i++] = ch; //���� 0
                token[i++] = pro[p++]; // ���� x or X
                ch = pro[p++]; // ��һ���ַ�
                if (!isHex(ch)) // ���治�������ֵĻ�������
                    mode = -1;
                else
                    while (isHex(ch)) {
                        token[i++] = ch;
                        ch = pro[p++];
                    }
            } else {
                //�������ֻ������ֿ�ͷ
                while (isDigit(ch) || ch == '.') {
                    //Բ�����������涨
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
                    if (ch == '-') { // ����֮����Ÿ���
                        token[i++] = ch;
                        ch = pro[p++];
                    }
                    // ���治�������֣��ͱ���
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
                special = "hexadecimal"; //ʶ��Ϊʮ��������
            else if (mode == 1)
                special = "fraction"; //ʶ��ΪС��
            else if (mode == 0)
                special = "integer"; //ʶ��Ϊ����
            else
                special = "error";
        } else {
            token[i] = ch;
            special = "error"; // ʶ��Ϊ ����
        }

        int newLength = token[i] == '\0' ? i : i + 1;
        char[] newArray = Arrays.copyOf(token, newLength);
        boolean isInv = false;

        //special ���ͨ���ַ���
        if (special == null)
            special = String.valueOf(token);
        //�ж��Ƿ�Ϊת���ַ�
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

    // ������ɨ��һ��������
    public static List<LexiNode> lexicalScan(String pro) {
        if (pro == null)
            return null;
        while (pro.endsWith("\n")) //��ȥ���Ļ���
            pro = pro.substring(0, pro.length() - 1);
        pro += " ";     // ��ֹ indexOutOfBound
        char[] program = pro.toCharArray();
        List<LexiNode> nodes = new ArrayList<>();
        HashMap<String, Integer> str2Code = str2IntMap();
        //��ʼ�ڵ�
        LexiNode node = new LexiNode("", -1, 1, 1, 0);

        boolean singleCom = false;
        boolean multiCom = false;
        do {
            node = oneScan(program, node); //����ɨ��

            // ע�͵��ж�
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
        if (node.getSymbol().equals("")) //��ȥ�����ܳ��ֵĿո�
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
        String b = "9e20"; // ����ֱ��ת�� hexadecimal
        String a = "0x12";
//        int k = Integer.parseInt(a);
        double k = Double.parseDouble(b);

        System.err.println(k);

        String pro = "char c = \"qweqe''''wqeq\";char";

        List<LexiNode> nodes = lexicalScan(pro);
        System.out.println(nodes);

    }

}

