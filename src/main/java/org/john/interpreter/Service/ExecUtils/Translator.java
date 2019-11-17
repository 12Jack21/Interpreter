package org.john.interpreter.Service.ExecUtils;

import org.john.interpreter.Service.SemanticUtils.ArrayTable;
import org.john.interpreter.Service.SemanticUtils.ArrayVariable;
import org.john.interpreter.Service.SemanticUtils.SimpleTable;
import org.john.interpreter.Service.SemanticUtils.SimpleVariable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class Translator {

    private List<String> messages = new LinkedList<>();
    private int level; // ��ǰ������
    private SimpleTable simpleTable;
    private ArrayTable arrayTable;
    private String msg = "";
    private String result = ""; // unknown

    public Translator() {
        simpleTable = new SimpleTable();
        arrayTable = new ArrayTable();
        level = 1;
    }

    private String[] nonsenseNT = {"Pro"};

    // ������� AST���ݹ���ã�Ŀ�ģ�������������з����ó�����Ϣ���浽messages��
    // �ȼ����﷨�����Ѿ�ȫ��ͨ��
    public void translate(ASTNode root) {
        String name = root.getName();
        if (name.equals("Pro")) {
            for (int i = 0; i < root.getMaxChildNum(); i++) {
                translate(root.getChildren()[i]);
            }
        } else if (name.equals("Statement")) {
            for (int i = 0; i < root.getMaxChildNum(); i++) {
                translate(root.getChildren()[i]);
            }
        } else if (name.equals("Declare")) {
            // int, real, char, void
            String type = root.getChildren()[0].getChildren()[0].getName(); //������Ϊ�Ǿֲ��������������������������
            String identifier = root.getChildren()[1].getChildren()[0].getValue();
            ASTNode F = root.getChildren()[1].getChildren()[1];
            if (!F.getChildren()[0].getName().equals("(")) {
                // ���Ǻ�������
                ASTNode index_node = F.getChildren()[0]; // Index
                if (index_node.getMaxChildNum() == 0) {
                    // ������һ���򵥱���
                    ASTNode X_node = F.getChildren()[1];
                    if (X_node.getMaxChildNum() == 0) {
                        // ��ӱ����� �������ű���
                        if (!simpleTable.addVariable(new SimpleVariable(identifier, type, null, level)))
                            messages.add("����" + identifier + "�ѱ���������");
                        else
                            messages.add("����" + identifier + "������Ϊ" + type + "��");
                    } else {
                        // X ->= O
                        ASTNode O_node = X_node.getChildren()[1];
                        if (O_node.getChildren()[0].getName().equals("{")) {
                            // �����ʼ�� O->{ Y }
                            ASTNode Y_node = O_node.getChildren()[1];
                            if (Y_node.getMaxChildNum() == 0) {
                                // ��������Ϊ��

                            } else {
                                // �������ֵ
                                ArrayList<SimpleVariable> array_values = translateY(Y_node);
                                boolean matchType = true;
                                for (SimpleVariable variable : array_values) {
                                    if (!variable.getType().equals(type)) {
                                        matchType = false;
                                        break;
                                    }
                                }
                                if (matchType) {
                                    ArrayVariable arrayVariable = new ArrayVariable(identifier, type,
                                            array_values.size(), convertArray(array_values, type), level);
                                    if (arrayTable.addVariable(arrayVariable))
                                        messages.add("�������" + identifier + "������Ϊ" + type +
                                                "�Ͳ�����ʼ��Ϊ " + arrayVariable.getValues().toString());
                                    else
                                        messages.add("�������" + identifier + "�ѱ���������");
                                } else
                                    messages.add("������ֵ������������������" + type + "��һ�£��޷���ֵ������");
                            }
                        } else {
                            // O->Relation
                            ASTNode relation = O_node.getChildren()[0];
                            // �˴�ֻ�� type�� value���������
                            SimpleVariable relation_value = translateExp(relation);
                            if (!type.equals(relation_value.getType())) {
                                messages.add("���Ͳ�ƥ�䣬�޷���ֵ������ " + identifier);
                            } else {
                                relation_value.setName(identifier);
                                if (!simpleTable.addVariable(relation_value))
                                    messages.add("����" + identifier + "�ѱ�������");
                                else
                                    messages.add("����" + identifier + "������Ϊ" + type +
                                            "�Ͳ���ʼ��Ϊ" + relation_value.getValue());
                            }
                        }
                    }
                    ASTNode C_node = F.getChildren()[2];
                    //TODO �����������εݹ鴦��


                } else {
                    // ������һ���������
                    ASTNode relation = index_node.getChildren()[1];
                    SimpleVariable array_length = translateExp(relation);
                }

            }
        }
    }

    // when Y->Relation C', C'->, Y
    private ArrayList<SimpleVariable> translateY(ASTNode Y) {
        ArrayList<SimpleVariable> variables = new ArrayList<>();
        ASTNode relation = Y.getChildren()[0];
        SimpleVariable relation_value = translateExp(relation);
        variables.add(relation_value);

        ASTNode C_ = Y.getChildren()[1];
        if (C_.getMaxChildNum() != 0) {
            variables.addAll(translateY(C_.getChildren()[1]));
        }
        return variables;
    }

    // ���� ���б��ʽ exp,�������ʹ��������ת��
    private SimpleVariable translateExp(ASTNode arithmetic) {
        SimpleVariable arith_val = null;
        LinkedList<SimpleVariable> varStack = new LinkedList<>(); //����ջ
        LinkedList<String> symStack = new LinkedList<>(); //����ջ
        HashMap<String, Integer> prioMap = CodeTable.opPriority();

        // ��ʼ����
        ASTNode var = arithmetic.findNextNodeWithValueOrTip("Variable");
        SimpleVariable variable = translateVariable(var);
        varStack.addFirst(variable);
        ASTNode sym;
        // û����һ����������� , ����������ѡ�����������token����+����+��+...+
        while ((sym = arithmetic.findNextNodeWithValueOrTip("symbol")) != null) {
            String sym_value = sym.getValue();
            // �����ж����ȼ�
            while (symStack.size() > 0 && prioMap.get(sym_value) <= prioMap.get(symStack.get(0))) {
                // ջ�����ŵ����ȼ����߻���ȣ����ϣ�
                SimpleVariable v2 = varStack.pop();
                SimpleVariable v1 = varStack.pop();
                String top = symStack.pop();
                varStack.addFirst(calculate(v1, v2, top));
            }
            // ջ�����ŵ����ȼ���С,���·�����ջ
            symStack.addFirst(sym_value);
            variable = translateVariable(arithmetic.findNextNodeWithValueOrTip("Variable"));
            varStack.addFirst(variable);
        }
        // �������Ѿ�û�з����ٽ�ջ��
        while (symStack.size() != 0) {
            String top = symStack.pop();
            SimpleVariable v2 = varStack.pop();
            SimpleVariable v1 = varStack.pop();
            varStack.addFirst(calculate(v1, v2, top));
        }
//        arithmetic.flushFindTag(); // TODO ˢ��find��־���Թ�֮��ʹ��,���ڵݹ麯�����ⲿ
        arith_val = varStack.pop();
        if (varStack.size() == 0)
            System.out.println("������̽��гɹ�������");
        return arith_val;
    }

    // ������֮������㣬���԰��� ��������ϵ���߼�����
    private SimpleVariable calculate(SimpleVariable v1, SimpleVariable v2, String top) {
        SimpleVariable reVar;
        if (v1.getType().equals(v2.getType()) && v1.getType().equals("int")) {
            // ��Ϊ int��
            int a1 = Integer.parseInt(v1.getValue());
            int a2 = Integer.parseInt(v2.getValue());
            if (top.equals("*")) {
                messages.add(a1 + " * " + a2 + " = " + (a1 * a2));
                reVar = new SimpleVariable(null, "int", String.valueOf(a1 * a2), level);
            } else if (top.equals("/")) {
                if (a2 == 0) {
                    messages.add("�����������ֵ�Զ���Ϊ0");
                    reVar = new SimpleVariable(null, "int", "0", level);
                } else {
                    messages.add(a1 + " / " + a2 + " = " + (a1 / a2));
                    reVar = new SimpleVariable(null, "int", String.valueOf(a1 / a2), level);
                }
            } else if (top.equals("+")) {
                messages.add(a1 + " + " + a2 + " = " + (a1 + a2));
                reVar = new SimpleVariable(null, "int", String.valueOf(a1 + a2), level);
            } else if (top.equals("-")) {
                messages.add(a1 + " - " + a2 + " = " + (a1 - a2));
                reVar = new SimpleVariable(null, "int", String.valueOf(a1 - a2), level);
            } else {
                int val = 0;
                if (top.equals("=="))
                    val = a1 == a2 ? 1 : 0;
                else if (top.equals("<>"))
                    val = a1 == a2 ? 0 : 1;
                else if (top.equals("<"))
                    val = a1 < a2 ? 1 : 0;
                else if (top.equals("<="))
                    val = a1 <= a2 ? 1 : 0;
                else if (top.equals(">"))
                    val = a1 > a2 ? 1 : 0;
                else if (top.equals(">="))
                    val = a1 >= a2 ? 1 : 0;
                else if (top.equals("||"))
                    val = ((a1 != 0) || (a2 != 0)) ? 1 : 0;
                else if (top.equals("&&"))
                    val = ((a1 != 0) && (a2 != 0)) ? 1 : 0;
                else
                    messages.add("�����������");
                messages.add(a1 + top + a2 + " = " + val);
                reVar = new SimpleVariable(null, "int", String.valueOf(val), level);
            }
        } else { // ��real�ʹ���
            if (!v1.getType().equals(v2.getType())) {
                if (v1.getType().equals("real"))
                    messages.add("����" + v1.getType() + "��" + v2.getType() + "��ƥ��,�Զ��� " + v2.getValue() + "��������ת��");
                else
                    messages.add("����" + v1.getType() + "��" + v2.getType() + "��ƥ��,�Զ��� " + v1.getValue() + "��������ת��");
            }
            double a1 = Double.parseDouble(v1.getValue());
            double a2 = Double.parseDouble(v2.getValue());
            if (top.equals("*")) {
                messages.add(a1 + " * " + a2 + " = " + (a1 * a2));
                reVar = new SimpleVariable(null, "real", String.valueOf(a1 * a2), level);
            } else if (top.equals("/")) {
                if (a2 == 0.0) {
                    messages.add("�����������ֵ�Զ���Ϊ 0.0");
                    reVar = new SimpleVariable(null, "real", "0.0", level);
                } else {
                    messages.add(a1 + " / " + a2 + " = " + (a1 / a2));
                    reVar = new SimpleVariable(null, "real", String.valueOf(a1 / a2), level);
                }
            } else if (top.equals("+")) {
                messages.add(a1 + " + " + a2 + " = " + (a1 + a2));
                reVar = new SimpleVariable(null, "real", String.valueOf(a1 + a2), level);
            } else if (top.equals("-")) {
                messages.add(a1 + " - " + a2 + " = " + (a1 - a2));
                reVar = new SimpleVariable(null, "real", String.valueOf(a1 - a2), level);
            } else {
                //TODO �Ƿ�����������û�п��ǵ�
                int val = 0;
                if (top.equals("=="))
                    val = a1 == a2 ? 1 : 0;
                else if (top.equals("<>"))
                    val = a1 == a2 ? 0 : 1;
                else if (top.equals("<"))
                    val = a1 < a2 ? 1 : 0;
                else if (top.equals("<="))
                    val = a1 <= a2 ? 1 : 0;
                else if (top.equals(">"))
                    val = a1 > a2 ? 1 : 0;
                else if (top.equals(">="))
                    val = a1 >= a2 ? 1 : 0;
                else if (top.equals("||"))
                    val = ((a1 != 0.0) || (a2 != 0.0)) ? 1 : 0;
                else if (top.equals("&&"))
                    val = ((a1 != 0.0) && (a2 != 0.0)) ? 1 : 0;
                else
                    messages.add("�����������");
                messages.add(a1 + top + a2 + " = " + val);
                reVar = new SimpleVariable(null, "int", String.valueOf(val), level);
            }
        }
        return reVar;
    }

    private static ASTNode testArithmeticExp() {
        List<ASTNode> nodes = testVariable();
        ASTNode arith = new ASTNode(2, "Arithmetic", null);
        ASTNode item = new ASTNode(2, "Item", null);
        ASTNode v_node = new ASTNode(2, "V", null);
        ASTNode var_node = nodes.get(0);

        ASTNode fact = new ASTNode(2, "Factor", null);
        ASTNode mul = new ASTNode(0, "symbol", "*");
        ASTNode item1 = new ASTNode(2, "Item", null);
        ASTNode var1 = nodes.get(1);
        ASTNode fac1 = new ASTNode(0, "Factor", null);

        ASTNode plus = new ASTNode(0, "symbol", "-");
        ASTNode ari1 = new ASTNode(2, "Arithmetic", null);
        ASTNode item2 = new ASTNode(2, "Item", null);
        ASTNode var2 = nodes.get(2);
        ASTNode fac2 = new ASTNode(0, "Factor", null);

//        ASTNode v_node1 = new ASTNode(0, "V", null); // ��������ʱ��
        ASTNode v_node1 = new ASTNode(2, "V", null); // �ĸ�����ʱ��
        v_node1.addChild(new ASTNode(0, "symbol", "-"));
        ASTNode ar = new ASTNode(2, "Arithmetic", null);
        ASTNode it = new ASTNode(2, "Item", null);
        ASTNode v = new ASTNode(0, "V", null);
        ASTNode f = new ASTNode(0, "Factor", null);
        v_node1.addChild(ar);
        ar.addChild(it);
        ar.addChild(v);
        it.addChild(nodes.get(3));
        it.addChild(f);

        arith.addChild(item);
        arith.addChild(v_node);
        item.addChild(var_node);
        item.addChild(fact);
        fact.addChild(mul);
        fact.addChild(item1);
        item1.addChild(var1);
        item1.addChild(fac1);

        v_node.addChild(plus);
        v_node.addChild(ari1);
        ari1.addChild(item2);
        ari1.addChild(v_node1);
        item2.addChild(var2);
        item2.addChild(fac2);
        Translator t = new Translator();

        ASTNode p = Executor.analyze("2 -1*9 && 0==12 >=12 <>100 || -99;").getAstNode();
        ASTNode a = p.getChildren()[0].getChildren()[0];
        SimpleVariable s = t.translateExp(a);
        System.out.println("������Ϣ��" + t.messages);
        return arith;
    }

    // "Variable-> ..."
    private SimpleVariable translateVariable(ASTNode variable_node) {
        SimpleVariable variable = null;
        if (variable_node.getMaxChildNum() == 1) {
            // "Variable->Digit"
            ASTNode digit_node = variable_node.getChildren()[0];
            ASTNode positive_node = digit_node.getChildren()[digit_node.getMaxChildNum() - 1];
            //TODO try catch�ܷ�������ת���Ĵ���
            if (positive_node.getChildren()[0].getName().equals("integer")) {
                // ������
                Integer value = Integer.valueOf(positive_node.getChildren()[0].getValue());
                if (digit_node.getChildren()[0].getName().equals("-")) //����
                    value = -1 * value;
                variable = new SimpleVariable(null, "int", value.toString(), level);
            } else {
                // С��
                Double value = Double.valueOf(positive_node.getChildren()[0].getValue());
                if (digit_node.getChildren()[0].getName().equals("-")) //����
                    value = -1.0 * value;
                variable = new SimpleVariable(null, "real", value.toString(), level);
            }
        } else if (variable_node.getMaxChildNum() == 2) {
            // "Variable->identifier Call"
            ASTNode call_node = variable_node.getChildren()[1];
            String identifier = variable_node.getChildren()[0].getValue();
            if (call_node.getChildren()[0].getName().equals("Index")) {
                ASTNode index_node = call_node.getChildren()[0];
                if (index_node.getMaxChildNum() == 0) {
                    SimpleVariable id = simpleTable.getVar(identifier);
                    if (id == null)
                        messages.add("���� " + identifier + "δ���������޷�ʹ��");
                    else {
                        if (id.getValue() == null)
                            messages.add("���� " + identifier + "û�б���ʼ�����޷�ʹ��");
                        else
                            variable = id;
                    }
                } else {
                    // ����ȡ�±��ֵ
                    SimpleVariable index = translateExp(index_node.getChildren()[1]);

                    if (index.getType().equals("real"))
                        messages.add("�����±� " + index.getValue() + "����ΪС��");
                    else if (Integer.parseInt(index.getValue()) < 0)
                        messages.add("�����±�" + index.getValue() + "����Ϊ����");
                    else {
                        ArrayVariable arrayVariable = arrayTable.getArray(identifier);
                        if (arrayVariable == null)
                            messages.add("�������" + identifier + "δ�������޷�ʹ��");
                        else {
                            //TODO ����±�Խ�磬δ��ֵ������
                            if (arrayVariable.getValues() == null || arrayVariable.getValues().size() == 0)
                                messages.add("����" + identifier + "δ����ֵ���޷�ʹ��");
                            else {
                                Integer ix = Integer.valueOf(index.getValue());
                                if (ix > arrayVariable.getLength() - 1)
                                    messages.add("����" + identifier + "�±�" + ix + "Խ��");
                                else {
                                    ArrayList<String> array = arrayVariable.getValues();
                                    // ����������һ����ֵ
                                    variable = new SimpleVariable(null, arrayVariable.getType(), array.get(ix), level);
                                }
                            }
                        }
                    }
                }
            } else {
                // �������ã�Call->( Argument )
            }
        } else if (variable_node.getMaxChildNum() == 3) {
            // "Variable->( Relation )"
            variable = translateExp(variable_node.getChildren()[1]);
        }
        return variable;
    }

    //TODO �ѱ����б�ת�� Value�� String�б����Լ����������Ƿ�ƥ�䣬�ܷ��Զ�ת������ת��ԭʼ��ֵ
    private ArrayList<String> convertArray(ArrayList<SimpleVariable> arrayList, String type) {
        // ����������ֵ�����Ͷ��� match type ��
        ArrayList<String> list = new ArrayList<>();
        for (int i = 0; i < arrayList.size(); i++) {
            list.add(arrayList.get(i).getValue());
        }
        return list;
    }

    private static ASTNode testIdArrayVariable() {
        Translator translator = new Translator();

        translator.simpleTable.addVariable(new SimpleVariable("pp", "int", null, 1));
        ASTNode variable_node = new ASTNode(2, null, null);
        ASTNode id_node = new ASTNode(0, "identifier", "pp");
        ASTNode call_node = new ASTNode(1, "Call", null);
        ASTNode index_node = new ASTNode(0, "Index", null);
        // test fetching value of simple variable
        variable_node.addChild(id_node);
        variable_node.addChild(call_node);
        call_node.addChild(index_node);
        SimpleVariable s = translator.translateVariable(variable_node);
        System.out.println(translator.messages);

        List<ASTNode> var_node = testVariable();
        index_node = new ASTNode(3, "Index", null);
        call_node.getChildren()[0] = index_node;
        // �ٶ� indexֻ��Ϊ variableʱ�ų���
        index_node.getChildren()[1] = var_node.get(0);
        ArrayList<String> values = new ArrayList<>();
        values.add("43");
        values.add("90");
        translator.arrayTable.addVariable(new ArrayVariable("pp", "int", 2, values, 1));
        SimpleVariable s1 = translator.translateVariable(variable_node);
        System.out.println(translator.messages);

        return variable_node;
    }

    private static List<ASTNode> testVariable() {
        ASTNode variable_node1 = new ASTNode(1, "Variable", null);
        ASTNode a1 = new ASTNode(2, "Digit", null);
        ASTNode b0 = new ASTNode(0, "+", "+");
        ASTNode b1 = new ASTNode(1, "Positive", null);
        ASTNode c0 = new ASTNode(0, "fraction", "2.00");
        variable_node1.addChild(a1);
        a1.addChild(b0);
        a1.addChild(b1);
        b1.addChild(c0);
        Translator translator = new Translator();
        SimpleVariable s = translator.translateVariable(variable_node1);

        ASTNode variable_node2 = new ASTNode(1, "Variable", null);
        ASTNode a11 = new ASTNode(2, "Digit", null);
        ASTNode b00 = new ASTNode(0, "+", "+");
        ASTNode b11 = new ASTNode(1, "Positive", null);
        ASTNode c00 = new ASTNode(0, "integer", "1");
        variable_node2.addChild(a11);
        a11.addChild(b00);
        a11.addChild(b11);
        b11.addChild(c00);

        ASTNode variable_node3 = new ASTNode(1, "Variable", null);
        ASTNode a111 = new ASTNode(2, "Digit", null);
        ASTNode b000 = new ASTNode(0, "+", "+");
        ASTNode b111 = new ASTNode(1, "Positive", null);
        ASTNode c000 = new ASTNode(0, "integer", "1");
        variable_node3.addChild(a111);
        a111.addChild(b000);
        a111.addChild(b111);
        b111.addChild(c000);

        ASTNode variable_node4 = new ASTNode(1, "Variable", null);
        ASTNode a1111 = new ASTNode(2, "Digit", null);
        ASTNode b0000 = new ASTNode(0, "+", "+");
        ASTNode b1111 = new ASTNode(1, "Positive", null);
        ASTNode c0000 = new ASTNode(0, "integer", "1");
        variable_node4.addChild(a1111);
        a1111.addChild(b0000);
        a1111.addChild(b1111);
        b1111.addChild(c0000);

        List<ASTNode> nodes = new LinkedList<>();
        nodes.add(variable_node1);
        nodes.add(variable_node2);
        nodes.add(variable_node3);
        nodes.add(variable_node4);
        return nodes;
    }

    public static void main(String[] args) {
        testArithmeticExp();
    }
}
