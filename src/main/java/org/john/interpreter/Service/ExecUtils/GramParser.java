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
    LinkedList<Character> stack = new LinkedList<>(); //��������ջ
    LinkedList<String> errorStack = new LinkedList<>(); // ������Ϣջ

    LinkedList<Character> matchStack = new LinkedList<>(); //����ƥ��ջ

    public GramParser() {
        try {
            llDrive = new LLDrive(production);
            llTable = llDrive.getTable();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //�������
    public static final int IDerror = -3; //��ʶ���Ĵ������

    // LL ��������
    public ASTNode LLParse(List<LexiNode> nodes) {
        try {
            boolean legal = true;

            //��ʼ��ջ����һ�� ��ʼ���� Ԫ�� + # ��
            stack.addFirst('P');
            stack.add('#');

            // ��ǰִ�в������﷨���ڵ�
            ASTNode curNode = null;
            //���ڵ�
            ASTNode rootNode = null;
            int childNum;

            Character top, symbol; //ջ��ָ�룬ɨ�赽��token
            int y, x, pos; //LL������� �к���λ��
            int index = 0; //ɨ�������
            LexiNode node;
            while (index < nodes.size()) {

                node = nodes.get(index);
                symbol = str2CharMap.get(int2StrMap.get(node.getCode()));
                top = stack.pop();

                if (node.getCode() == -1) {
                    legal = false;
                    throw new Exception("(" + node.getRow() + "," + node.getCol() + ")�����ִʷ���������");
                }
                //�ж�Ϊ�ս����
                if (top < 'A' || top > 'Z') {
                    //����ɨ��
                    if (top == symbol) {
                        index++;
                        if (symbol == '#')
                            continue;
                        curNode = curNode.findLefted();
                        curNode.addChild(new ASTNode(0, top, true,true));

                    } else {
                        legal = false;
                        if (top == '#')
                            //ջ���Ѿ�Ϊ�գ��޷�����ɨ����
                            break; // nearest namely while loop

                        //TODO �Զ��������ȱ�ٵķ��� / �����÷���
                        //index++;
                        curNode = curNode.findLefted();
                        curNode.addChild(new ASTNode(0,top,true,false));

                        errorStack.add("(" + node.getRow() + "," + node.getCol() + ")�������﷨����,ȱ�� " + top);

                    }
                    updateMatch(top); //����ƥ��ջ TODO ����һ������ʱ�򣬸���ô�������ջ---

                } else { //Ϊ���ս����
                    y = llDrive.getYcharMap().get(top);
                    x = llDrive.getXcharMap().get(symbol);
                    pos = llTable[y][x];

                    if (pos != -1) {
                        childNum = llDrive.addStack(pos, stack);
                        if (rootNode == null) {
                            rootNode = new ASTNode(childNum, 'P', false,true);
                            curNode = rootNode;
                        } else {
                            curNode = curNode.findLefted(); //�ҵ���ʣ���ӽڵ�û���ϵĽڵ�
                            curNode.addChild(new ASTNode(childNum, top, false,true));
                        }
                    } else {
                        if (node.getCode() == -2) // '#'��
                            throw new Exception("ȱ�ٽ�����");
                        else{
                            // ������������ S��������һ�������﷨����
                            // �ڵ������� S �� First����ջһֱpop ֱ������ p����һ�����Ŀ�ʼ��
                            legal = false;
                            errorStack.add("(" + node.getRow() + "," + node.getCol() + ")�������﷨����," + errorHandle(top,symbol));

                            // 1.ջ�Ķ���
                            while (top != 'P') {
                                curNode = curNode.findLefted(); // TODO Handle NullPointerException
                                if (curNode != null)
                                    curNode.addChild(new ASTNode(0, top, false, false));

                                top = stack.pop();
                            }
                            // ��ʱ�ҵ��� P�������ڷ���ѭ��ʱ���� popһ�Σ�����Ҫ����
                            stack.addFirst('P');

                            // 2.�ʷ������ڵ�Ķ���,�ҵ� S�� first���еĴʷ���Ԫ
                            List<String> stateStart = Arrays.asList(llDrive.getFirstMap().get('S').split(","));

                            while (!stateStart.contains(symbol.toString())){
                                index++;
                                if (index >= nodes.size())
                                    continue;
                                symbol = str2CharMap.get(int2StrMap.get(nodes.get(index).getCode()));
                            }
                            // ��ʱ�Ѿ��ҵ���ʼ�����ˣ��ҷ���ѭ��ʱ������ index���ʲ��û���
                        }
                    }

                }
            }
            //������ɺ�ջ�л���Ԫ��
            if (stack.size() > 0)
                throw new Exception("ȱ�ٽ�����");

            if (legal)
                System.out.println("\n�﷨�����ɹ�������\n");
            else
                System.out.println("�﷨����ʧ�ܣ��������£�");
            //�������ջ�е�����
            for (String error:errorStack){
                System.out.println(error);
            }

            //��� AST
            System.out.println(rootNode.toJSON());

            //д���ļ�
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

    // ����ӳ��
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
            matchStack.pop(); // �ƺ��ò��� canMatch����

    }

    // ���ս�� -> �ս�� ����ʱ���룬���ý�����ٵķ�������Ͼ���Ĵ���
    private String errorHandle(char top,char symbol){
        List<String> stateStart = Arrays.asList(llDrive.getFirstMap().get('S').split(","));

        if (matchStack.size() != 0){
            Character src = matchStack.pop();
            return "ȱ�� " + parMap(src);
        }
        else if (stateStart.contains(String.valueOf(symbol)) && symbol != ';'){
//            if (top == 'B' || top == '') ��ϸ�µĻ���
            return "ȱ�� ;";
        }else if (symbol == ','){
            return "ȱ�� ��ʶ��";
        }else if (symbol == ';')
            return "ȱ�� ���ʽ";
        else
            return "��������";
    }

    public static void main(String[] args) {

    }
}