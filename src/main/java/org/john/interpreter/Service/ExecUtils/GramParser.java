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
    private LinkedList<String> stack = new LinkedList<>(); //��������ջ
    private LinkedList<String> errorStack = new LinkedList<>(); // ������Ϣջ

    private LinkedList<String> matchStack = new LinkedList<>(); //����ƥ��ջ

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
            List<String> symList = signList.subList(0,12);
            while (index < nodes.size()) {

                node = nodes.get(index);
                top = stack.pop();

                while (node.getCode() == -1) {
                    legal = false;
                    errorStack.add("��" + node.getRow() + "�У���" + node.getCol() + "�� " + node.getSymbol() +" �����޷�ʶ��� token");
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

                        if (symList.contains(top) && !curNode.getName().equals("Digit")) // Ϊ�����
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
                            //ջ���Ѿ�Ϊ�գ��޷�����ɨ����
                            break; // nearest namely while loop

                        //�Զ��������ȱ�ٵķ��� / �����÷���---
                        //index++;
                        curNode = curNode.findLefted();
                        curNode.addChild(new ASTNode(0, top, true, false));

                        errorStack.add("��" + node.getRow() + "��,��" + node.getCol() + "�д� " + node.getSymbol() +" �����﷨����,ȱ�� " + top);

                    }
                    updateMatch(top); //����ƥ��ջ TODO ����һ������ʱ�򣬸���ô�������ջ---

                } else { //Ϊ���ս����
                    y = llDrive.getNtMap().get(top);
                    x = llDrive.gettMap().get(symbol);
                    pos = llTable[y][x];

                    if (pos != -1) {
                        if (pos == -2) {
                            /* ���������ì�ܲ���ʽ��ѡ��, ��ʶ����Ϊ Select��ì�ܵĵط�
                             * ɨ��ֺ�֮ǰ�����ķ��ţ����� �߼�������ϵ���������ǰ���� ��ֵ������ѡ�� ��ֵ���Ĳ���ʽ������֮*/
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
                            curNode = curNode.findLefted(); //�ҵ���ʣ���ӽڵ�û���ϵĽڵ�
                            curNode.addChild(new ASTNode(childNum, top, false, true));
                        }
                    } else {
                        //TODO  û�� "P" δ����ջʱ�����������
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
                            errorStack.add("��" + node.getRow() + "��,��" + node.getCol() + "�д� " + node.getSymbol() + " �����﷨����," + errorHandle(top, symbol));

                            // 1.ջ�Ķ���
                            while (!top.equals("Pro")) {  // "#" ���� "Pro" ?
                                curNode = curNode.findLefted(); // TODO Handle NullPointerException
                                curNode.addChild(new ASTNode(0, top, false, false));
                                top = stack.pop();
                            }
                            // ��ʱ�ҵ��� P�������ڷ���ѭ��ʱ���� popһ�Σ�����Ҫ����
                            stack.addFirst("Pro");
                            // 2.�ʷ������ڵ�Ķ���,�ҵ� S�� first���еĴʷ���Ԫ
                            List<String> stateStart = Arrays.asList(llDrive.getFirstMap().get("Pro").split(" ")); //�ĳ���Pro��First��
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
                errorStack.add("ȱ�ٽ�����!");
            }
            if (legal)
                System.out.println("\n�﷨�����ɹ�������\n");
            else
                System.out.println("�﷨����ʧ�ܣ��������£�");

            Set<String> set = new LinkedHashSet<>(errorStack);
            errorStack = new LinkedList<>(set);

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
        else if (top.equals(")") || top.equals("]") || top.equals("}")) {
            if (matchStack.size() != 0)
                matchStack.pop(); // TODO Handle matchStack.size == 0 Error !!!
        }
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
//            matchStack.addFirst(src); //���¼ӻ���
            return "ȱ�� " + parMap(src);
        } else if (symbol.equals(";"))
            return "ȱ�� ���ʽ";
        else
            return "��������";
    }

    public static void main(String[] args) {

    }
}