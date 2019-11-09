package org.john.interpreter.Service.ExecUtils;


import org.john.interpreter.Service.LLUtils.LLDrive;
import org.springframework.util.ResourceUtils;

import java.io.FileWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import static org.john.interpreter.Service.ExecUtils.CodeTable.*;

public class GramParser {

    private LLDrive llDrive;
    private int[][] llTable;
    private HashMap<Integer, String> int2StrMap = int2StrMap();
    private LinkedList<String> stack = new LinkedList<>(); //��������ջ
    private LinkedList<String> errorStack = new LinkedList<>(); // ������Ϣջ

    private LinkedList<String> matchStack = new LinkedList<>(); //����ƥ��ջ

    public GramParser() {
        try {
            llDrive = new LLDrive(production);
            llTable = llDrive.getTable();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public LinkedList<String> getErrorStack() {
        return errorStack;
    }

    //�������
    public static final int IDerror = -3; //��ʶ���Ĵ������

    // LL ��������
    public ASTNode LLParse(List<LexiNode> nodes) {
        try {
            boolean legal = true;

            //��ʼ��ջ����һ�� ��ʼ���� Ԫ�� + # ��
            stack.addFirst("Pro");
            stack.add("#");

            // ��ǰִ�в������﷨���ڵ�
            ASTNode curNode = null;
            //���ڵ�
            ASTNode rootNode = null;
            int childNum;

            String top, symbol; //ջ��ָ�룬ɨ�赽��token
            int y, x, pos; //LL������� �к���λ��
            int index = 0; //ɨ�������
            LexiNode node;
            while (index < nodes.size()) {

                node = nodes.get(index);
                top = stack.pop();

                while (node.getCode() == -1) {
                    legal = false;
                    errorStack.add("��" + node.getRow() + "�У���" + node.getCol() + "�г����޷�ʶ��� token");
                    index++;
                    if (index >= nodes.size())
                        break;
                    node = nodes.get(index); // ���Ե��޷�ʶ��� token
                }
                if (index >= nodes.size())
                    break;

                symbol = int2StrMap.get(nodes.get(index).getCode());
                //�ж�Ϊ�ս����
                if (top.charAt(0) < 'A' || top.charAt(0) > 'Z') {
                    //����ɨ��
                    if (top.equals(symbol)) {
                        index++;
                        if (symbol.equals("#"))
                            continue;
                        curNode = curNode.findLefted();
                        curNode.addChild(new ASTNode(0, top, true, true));

                    } else {
                        legal = false;
                        if (top.equals("#"))
                            //ջ���Ѿ�Ϊ�գ��޷�����ɨ����
                            break; // nearest namely while loop

                        //�Զ��������ȱ�ٵķ��� / �����÷���---
                        //index++;
                        curNode = curNode.findLefted();
                        curNode.addChild(new ASTNode(0, top, true, false));

                        errorStack.add("��" + node.getRow() + "��,��" + node.getCol() + "�д������﷨����,ȱ�� " + top);

                    }
                    updateMatch(top); //����ƥ��ջ TODO ����һ������ʱ�򣬸���ô�������ջ---

                } else { //Ϊ���ս����
                    y = llDrive.getNtMap().get(top);
                    x = llDrive.gettMap().get(symbol);
                    pos = llTable[y][x];

                    if (pos != -1) {
                        childNum = llDrive.addToStack(pos, stack);
                        if (rootNode == null) {
                            rootNode = new ASTNode(childNum, "Pro", false, true);
                            curNode = rootNode;
                        } else {
                            curNode = curNode.findLefted(); //�ҵ���ʣ���ӽڵ�û���ϵĽڵ�
                            curNode.addChild(new ASTNode(childNum, top, false, true));
                        }
                    } else {
                        //TODO  û�� "P"δ����ջʱ�����������
                        if (node.getCode() == -2) { // "#" ��
                            legal = false;
                            if (!errorStack.contains("ȱ�ٽ�����!"))
                                errorStack.add("ȱ�ٽ�����!"); //TODO handle undone
                        } else {
                            if (rootNode == null) {
                                // manually handle stack
                                rootNode = new ASTNode(2, "Pro", false, true);
                                stack.add("Statement");
                                stack.add("Pro");
                                curNode = rootNode;
                            }

                            // ������������ S��������һ�������﷨����
                            // �ڵ������� S �� First����ջһֱpop ֱ������ Pro����һ�����Ŀ�ʼ��
                            legal = false;
                            errorStack.add("��" + node.getRow() + "��,��" + node.getCol() + "�д������﷨����," + errorHandle(top, symbol));

                            // 1.ջ�Ķ���
                            while (!top.equals("Pro")) {  // "#" ���� "Pro" ?
                                curNode = curNode.findLefted(); // TODO Handle NullPointerException
                                curNode.addChild(new ASTNode(0, top, false, false));
                                top = stack.pop();
                            }
                            // ��ʱ�ҵ��� P�������ڷ���ѭ��ʱ���� popһ�Σ�����Ҫ����
                            stack.addFirst("Pro");
                            // 2.�ʷ������ڵ�Ķ���,�ҵ� S�� first���еĴʷ���Ԫ
                            List<String> stateStart = Arrays.asList(llDrive.getFirstMap().get("Statement").split(" "));
                            while (!stateStart.contains(symbol) && !symbol.equals("#")) {
                                index++;
                                if (index >= nodes.size())
                                    break;
                                symbol = int2StrMap.get(nodes.get(index).getCode());
                            }
                            // ��ʱ�Ѿ��ҵ���ʼ�����ˣ��ҷ���ѭ��ʱ������ index���ʲ��û���
                        }
                    }

                }
            }
            //������ɺ�ջ�л���Ԫ��
            if (stack.size() > 0) {
                if (!errorStack.contains("ȱ�ٽ�����!"))
                    errorStack.add("ȱ�ٽ�����!");
            }
            if (legal)
                System.out.println("\n�﷨�����ɹ�������\n");
            else
                System.out.println("�﷨����ʧ�ܣ��������£�");
            //�������ջ�е�����
            for (String error : errorStack) {
                System.out.println(error);
            }

            //��� AST
            System.out.println(rootNode.toJSON());

            //д���ļ�
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

    // ����ӳ��
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
        else if (top.equals(")") || top.equals("]") || top.equals("}"))
            matchStack.pop(); // �ƺ��ò��� canMatch����
    }

    // ���ս�� -> �ս�� ����ʱ���룬���ý�����ٵķ�������Ͼ���Ĵ���
    private String errorHandle(String top, String symbol) {
        List<String> stateStart = Arrays.asList(llDrive.getFirstMap().get("Statement").split(" "));

        // tune the priority
        if (symbol.equals(",")) {
            return "ȱ�� ��ʶ��";
        } else if (stateStart.contains(symbol) && !symbol.equals(";")) {
//            if (top == 'B' || top == '') ��ϸ�µĻ���
            return "ȱ�� ;";
        } else if (matchStack.size() != 0) {
            String src = matchStack.pop();
            return "ȱ�� " + parMap(src);
        } else if (symbol.equals(";"))
            return "ȱ�� ���ʽ";
        else
            return "��������";
    }

    public static void main(String[] args) {

    }
}