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

    private static LexiNode oneScan(char[] pro, LexiNode node) {
        HashMap<String, Integer> str2Code = str2IntMap();
        int p = node.getP(), i = 0;
        char ch = pro[p++];
        char[] token = new char[20];
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

        //��⵽���� (������ȼ����ж�),�Ӻźͼ��ŵ����ó�������
        if (ch == '|' || ch == '&' || signList.contains(String.valueOf(ch)) || ch == '\\') {
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
                //�ж��Ƿ����»��߽�β,���ܻ��� AB_12_a �����ı�ʶ��,���� p ������ǰ����һλ
                if (ch == '_' && !isLetter(pro[p]) && !isDigit(pro[p]))
                    break;
                token[i++] = ch;
                ch = pro[p++];
            }
            p--; //�����һλ����ǰ������

            //��Ϊ�ؼ���, ��Ϊ��ʶ��
            if (!keyList.contains(String.valueOf(token).trim()))
                special = "identifier";

        } else if (isDigit(ch) || ch == '+' || ch == '-') { //���ֵ��ж�
            boolean hasDot = false;
            boolean hasSign = false;
            boolean legal = true;

//            if (ch == '+' || ch == '-') {
//                //ֱ��ʶ��Ϊ��/����
//                return new LexiNode(String.valueOf(ch), str2Code.get(String.valueOf(ch)), row, col, p);
//            }
            //�������ֻ������ֿ�ͷ
            while (isDigit(ch) || ch == '.') {
                if (ch == '.' && hasDot) //Բ�����������涨
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
                special = "rdigit"; //ʶ��ΪС��
            else
                special = "digit"; //ʶ��Ϊ����

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
        Integer code2 = str2Code.get(special.trim());
        Integer code = str2Code.get(special);
        return new LexiNode(String.valueOf(newArray), str2Code.get(special), row, col, p);
    }

    // ������ɨ��һ��������
    public static List<LexiNode> lexicalScan(String pro) {
        List<LexiNode> nodes = new ArrayList<>();
        HashMap<String, Integer> str2Code = str2IntMap();
        //��ʼ�ڵ�
        LexiNode node = new LexiNode("", -1, 1, 1, 0);

        boolean singleCom = false; //TODO �����Ƿ� �ո���Ϊ һ�� Node
        boolean multiCom = false;
        do {
            node = oneScan(pro.toCharArray(), node); //����ɨ��

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
                nodes.add(node); // ǳ����?
        } while (node.getP() < pro.length());
        if (node.getSymbol().equals(""))
            nodes.remove(node);
        return nodes;
    }

    public static List<LexiNode> preprocess(List<LexiNode> nodes) {
        LexiNode[] nodeArray = nodes.toArray(new LexiNode[nodes.size()]);
        // remove inverse meaning token
        for (LexiNode node : nodeArray) {
            if (node.getLength() != 0) {
                char c = node.getSymbol().charAt(0);
                if (c == '\n' || c == '\r' || c == '\t')
                    nodes.remove(node);
            }
        }
        nodes.add(new LexiNode("#", -2, -1, -1, -1));
        return nodes;
    }

    public static void main(String[] args) {

    }

}

