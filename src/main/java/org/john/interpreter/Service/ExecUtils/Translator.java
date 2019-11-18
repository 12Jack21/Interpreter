package org.john.interpreter.Service.ExecUtils;

import org.john.interpreter.Service.SemanticUtils.*;
import org.john.interpreter.dto.Wrapper;

import java.lang.reflect.Array;
import java.util.*;

@SuppressWarnings("ALL")
public class Translator {

    private List<String> messages = new LinkedList<>();
    private int level; // ��ǰ������
    private SimpleTable simpleTable;
    private ArrayTable arrayTable;
    private FunctionTable functionTable;
    private int whileNum = 0;
    private int proNum = 0;
    private boolean toBreak = false;
    private boolean toContinue = false;
    private boolean toReturn = false;
    private LinkedList<String> returnTypeStack;
    private String msg = "";
    private SimpleVariable returnVal = null;// ���ڴ��ݺ�������ֵ��Ϊ������Ĭ��ֵ 0��int��

    private LinkedList<String > printList;

    public Translator() {
        simpleTable = new SimpleTable();
        arrayTable = new ArrayTable();
        functionTable = new FunctionTable();
        returnTypeStack = new LinkedList<>();
        printList = new LinkedList<>();
        level = 1;
    }

    public LinkedList<String> getPrintList() {
        return printList;
    }

    public List<String> getMessages() {
        return messages;
    }

    // ������� AST���ݹ���ã�Ŀ�ģ�������������з����ó�����Ϣ���浽messages��
    // �ȼ����﷨�����Ѿ�ȫ��ͨ��
    public void translate(ASTNode root) {
        String name = root.getName();
        if (name.equals("Pro")) {
            for (int i = 0; i < root.getMaxChildNum(); i++) {
                //���� {} ʱ�� level�仯����
                if (root.getChildren()[i].getName().equals("{"))
                    level++;
                else if (root.getChildren()[i].getName().equals("}")) {
                    simpleTable.deleteVariable(level);
                    arrayTable.deleteArrays(level);
                    level--;
                } else
                    translate(root.getChildren()[i]);
            }
        } else if (name.equals("Statement")) {
            if (whileNum <= 0 || (!toBreak && !toContinue))
                translate(root.getChildren()[0]);
        } else if (name.equals("Declare")) {
            // int, real, char, void
            String type = root.getChildren()[0].getChildren()[0].getName(); //������Ϊ�Ǿֲ��������������������������
            String identifier = root.getChildren()[1].getChildren()[0].getValue();
            ASTNode F = root.getChildren()[1].getChildren()[1];
            if (!F.getChildren()[0].getName().equals("(")) {
                // ���Ǻ�������
                ASTNode index_node = F.getChildren()[0]; // Index
                ASTNode X_node = F.getChildren()[1]; // X
                translateIndexWithX(index_node, X_node, identifier, type);

                ASTNode C_node = F.getChildren()[2];
                while (C_node.getMaxChildNum() != 0) {
                    translateAssignment(C_node.getChildren()[1], type); // ����Assignment
                    C_node = C_node.getChildren()[2];
                }
            } else {//  ��������
                // ���� pro_node ����������
                try {
                    ArrayList<Object> parameters = translateParameter(F.getChildren()[1]);
                    FunctionVariable v = new FunctionVariable(type, identifier, parameters, F.getChildren()[4]);
                    functionTable.addVariable(v);
                    String msg = "�����˺���" + identifier + ",";
                    if (parameters.size() == 0)
                        msg += "û�в���";
                    else {
                        msg += "�����б�Ϊ(";
                        for (Object param : parameters) {
                            if (param instanceof SimpleVariable)
                                msg += ((SimpleVariable) param).getType() + " " + ((SimpleVariable) param).getName();
                            else if (param instanceof ArrayVariable)
                                msg += ((ArrayVariable) param).getType() + " " + ((ArrayVariable) param).getArrayName()
                                        + "[" + ((ArrayVariable) param).getLength() + "]";
                            msg += ",";
                        }
                        if (msg.endsWith(","))
                            msg = msg.substring(0, msg.length() - 1);
                        msg += ")";
                    }
                    msg += ",��������Ϊ " + v.getType();
                    messages.add(msg);
                } catch (Exception e) {
                    messages.add("����" + identifier + "����ʧ�ܣ�");
                }
            }
        } else if (name.equals("Assignment")) {
            translateAssignment(root, null);
        } else if (name.equals("IF")) {
            ASTNode logic = root.getChildren()[2];
            SimpleVariable log = translateExp(logic);
            boolean re = (int) Double.parseDouble(log.getValue()) == 1;
            if (re) {
                messages.add("���� if������ִ����������");
                translate(root.getChildren()[4]);
            } else {
                messages.add("������ if����");
                if (root.getChildren()[5].getMaxChildNum() != 0) {
                    // ELSE->else H
                    messages.add("ִ�� else��ĳ���");
                    translate(root.getChildren()[5].getChildren()[1]);
                }
            }
        } else if (name.equals("H")) {
            if (root.getMaxChildNum() == 1)
                translate(root.getChildren()[0]); // Statement
            else { // { Pro }
                level++;
                translate(root.getChildren()[1]);
                simpleTable.deleteVariable(level);
                arrayTable.deleteArrays(level);
                level--;
            }
        } else if (name.equals("WHILE")) {
            whileNum++;
            ASTNode logic = root.getChildren()[2];
            SimpleVariable log = translateExp(logic);
            boolean re = (int) Double.parseDouble(log.getValue()) == 1;
            logic.flushFindTag();
            while (re) {
                messages.add("����whileѭ��������ִ��ѭ�������");
                translate(root.getChildren()[4]); // H_node
                if (toBreak)
                    break;
                re = (int) Double.parseDouble(translateExp(logic).getValue()) == 1;
                logic.flushFindTag();
                toContinue = false;
                root.getChildren()[4].flushFindTag();
            }
            if (toBreak)
                toBreak = false;
            else
                messages.add("������whileѭ��������ѭ���˳�");
            whileNum--;
        } else if (name.equals("Logic")){
            SimpleVariable s = translateExp(root);
        } else if (name.equals("Interrupt")) {
            String na = root.getChildren()[0].getName();
            if (na.equals("break") && whileNum > 0) {
                toBreak = true;
                messages.add("���� break,ѭ���˳�");
            } else if (na.equals("continue") && whileNum > 0) {
                toContinue = true;
                messages.add("���� continue,������һ��ѭ��");
            } else if (na.equals("return")) {
                //TODO ���� return ���ضϺ�������ִ��---
                ASTNode result_node = root.getChildren()[1];
                if (result_node.getMaxChildNum() != 0) {
                    SimpleVariable log = translateExp(result_node.getChildren()[0]);

                    String type = returnTypeStack.pop();
                    //����ֵ���� returnVal��
                    SimpleVariable tmp = new SimpleVariable(null, type, null, level);
                    if (!type.equals(log.getType())) {
                        if (type.equals("int")) {
                            // ǿ��ת��
                            int val = (int) Double.parseDouble(log.getValue());
                            messages.add("ֵ�����뷵�����Ͳ�ƥ�䣬" + log.getValue() + "ǿ��ת��Ϊ" + val + "������");
                            tmp.setValue(String.valueOf(val));
                        } else if (type.equals("real")) {
                            double val = Double.parseDouble(log.getValue());
                            messages.add("ֵ�����뷵�����Ͳ�ƥ�䣬" + log.getValue() + "�Զ�����ת��Ϊ" + val + "������");
                            tmp.setValue(String.valueOf(val));
                        }
                    } else
                        tmp.setValue(log.getValue());
                    returnVal = tmp;
                }
                // û�з���ֵ�򲻼����
                if (proNum > 0) {
                    toReturn = true;
                    messages.add("����return,�����˳�");
                }
            }
        }
    }

    // when CC->, Parameter, like translateY
    private ArrayList<Object> translateParameter(ASTNode parameter) throws Exception {
        ArrayList<Object> parameters = new ArrayList<>();

        if (parameter.getMaxChildNum() == 4) {
            // Parameter->Type identifier Index CC
            String type = parameter.getChildren()[0].getChildren()[0].getName(); //����Ϊ void��char
            String identifier = parameter.getChildren()[1].getValue();
            ASTNode index_node = parameter.getChildren()[2];
            ASTNode CC_node = parameter.getChildren()[3];
            if (index_node.getMaxChildNum() == 0) {
                //�򵥱����Ĳ���
                SimpleVariable v = new SimpleVariable(identifier, type, null, level);//TODO level��Ӱ��
                parameters.add(v);
            } else {
                //��������Ĳ������д�����޷��ɹ�����������throw
                ASTNode logic = index_node.getChildren()[1];
                SimpleVariable log = translateExp(logic);
                int len;
                if (log.getType().equals("real")) {
                    len = (int) Double.parseDouble(log.getValue());
                    messages.add("��Ϊ���������鳤�Ȳ���ΪС��" + log.getValue() + "����ǿ��ת��Ϊ" + len);
                    if (len < 0) {
                        messages.add("��Ϊ���������鳤�Ȳ���Ϊ����" + len);
                        throw new Exception();
                    }
                } else //int��
                    len = Integer.parseInt(log.getValue());
                ArrayVariable v = new ArrayVariable(identifier, type, len, null, level);
                parameters.add(v);
            }
            if (CC_node.getMaxChildNum() != 0)
                parameters.addAll(translateParameter(CC_node.getChildren()[1]));
        }
        return parameters;
    }

    private void translateAssignment(ASTNode assignment, String type) {
        String identifier = assignment.getChildren()[0].getValue();
        translateIndexWithX(assignment.getChildren()[1], assignment.getChildren()[2], identifier, type);
    }

    // if type == null,��Ϊ Assignment���õģ�����Ϊ Declare����
    private void translateIndexWithX(ASTNode index_node, ASTNode X_node, String identifier, String type) {
        if (index_node.getMaxChildNum() == 0) {
            // �������߸�ֵһ���򵥱���
            if (X_node.getMaxChildNum() == 0) {
                if (type == null)
                    return;
                // ��ӱ����� �������ű���
                if (!simpleTable.addVariable(new SimpleVariable(identifier, type, null, level)))
                    messages.add("����" + identifier + "�ѱ���������");
                else
                    messages.add("����" + identifier + "������Ϊ" + type + "��");
            } else {
                // X ->= O
                ASTNode O_node = X_node.getChildren()[1];
                if (O_node.getChildren()[0].getName().equals("{")) {
                    if (type == null) {
                        messages.add("�޷�������Ա������и�ֵ");
                        return;
                    }
                    messages.add("�޷����������ڳ�ʼ���򵥱���" + identifier);
                } else {
                    // O->Logic
                    ASTNode logic = O_node.getChildren()[0];
                    // �˴�ֻ�� type�� value���������
                    SimpleVariable logic_value = translateExp(logic);
                    if (type == null) { //��ֵ����
                        SimpleVariable v = simpleTable.getVar(identifier);
                        if (v == null) {
                            messages.add("����" + identifier + "δ�������޷���ֵ");
                            return;
                        }
                        if (!v.getType().equals(logic_value.getType())) {
                            if (v.getType().equals("int")) {
                                // ǿ��ת��
                                int val = (int) Double.parseDouble(logic_value.getValue());
                                messages.add("���Ͳ�ƥ�䣬" + logic_value.getValue() + "ǿ��ת��Ϊ" + val);
                                v.setValue(String.valueOf(val));
                            } else if (v.getType().equals("real")) {
                                double val = Double.parseDouble(logic_value.getValue());
                                messages.add("���Ͳ�ƥ�䣬" + logic_value.getValue() + "�Զ�����ת��Ϊ" + val);
                                v.setValue(String.valueOf(val));
                            }
                        } else
                            v.setValue(logic_value.getValue());
                        messages.add("����" + identifier + "����ֵΪ" + v.getValue());
                    } else {//��������
                        if (!type.equals(logic_value.getType())) {
                            if (type.equals("int")) {
                                // ǿ��ת��
                                int val = (int) Double.parseDouble(logic_value.getValue());
                                messages.add("���Ͳ�ƥ�䣬" + logic_value.getValue() + "ǿ��ת��Ϊ" + val);
                                logic_value.setValue(String.valueOf(val));
                            } else if (type.equals("real")) {
                                double val = Double.parseDouble(logic_value.getValue());
                                messages.add("���Ͳ�ƥ�䣬" + logic_value.getValue() + "�Զ�����ת��Ϊ" + val);
                                logic_value.setValue(String.valueOf(val));
                            }
                        }
                        logic_value.setName(identifier);
                        if (!simpleTable.addVariable(logic_value))
                            messages.add("����" + identifier + "�ѱ�������");
                        else
                            messages.add("����" + identifier + "������Ϊ" + type +
                                    "�Ͳ���ʼ��Ϊ" + logic_value.getValue());
                    }
                }
            }
        } else {
            // �������飬��������±��λ�ø�ֵ
            ASTNode logic = index_node.getChildren()[1];
            SimpleVariable array_length = translateExp(logic);

            if (array_length.getType().equals("real")) {
                messages.add("�����±겻����ΪС��" + array_length.getValue() + " ��ֻ��Ϊ������");
            } else {
                int ix = Integer.parseInt(array_length.getValue());
                if (ix < 0)
                    messages.add("����ʱ�±겻����Ϊ����" + array_length.getValue() + " ��ֻ��Ϊ������");
                else {
                    // �±��Ѿ�����������
                    if (X_node.getMaxChildNum() == 0) {
                        if (type == null) //��ֵʱ�����������
                            return;
                        // ֻ������û�г�ʼ�������
                        // ��ӱ����� �������ű��� TODO δ��ֵ��ʹ�����⣬��ϵ translateVariable(),����ͬ��
                        ArrayList<String> zeroValues = new ArrayList<>();
                        int i = ix;
                        if (type.equals("int")) {
                            while (i-- > 0) {
                                zeroValues.add(String.valueOf(0));
                            }
                        } else if (type.equals("real")) {
                            while (i-- > 0) {
                                zeroValues.add(String.valueOf(0.0));
                            }
                        }
                        if (!arrayTable.addVariable(new ArrayVariable(identifier, type, ix, zeroValues, level)))
                            messages.add("�������" + identifier + "�ѱ���������");
                        else
                            messages.add("�������" + identifier + "������Ϊ" + type +
                                    "��,��" + ix + "��Ԫ�أ����Զ���ʼ��Ϊ" + zeroValues);
                    } else {
                        // X ->= O�������ų�ʼ������ֵ�������
                        ASTNode O_node = X_node.getChildren()[1];
                        if (O_node.getMaxChildNum() != 3) {
                            if (type != null) {
                                messages.add("�����õ����ı��ʽ����ʼ������" + identifier);
                                if (!arrayTable.addVariable(new ArrayVariable(identifier, type, ix, new ArrayList<>(ix), level)))
                                    messages.add("�������" + identifier + "�ѱ���������");
                                else
                                    messages.add("�������" + identifier + "������Ϊ" + type + "��,��" + ix + "��Ԫ��");
                            } else {
                                // �����±�λ�� ��ֵ�����
                                ArrayVariable v = arrayTable.getArray(identifier);
                                if (v == null)
                                    messages.add("�������δ�������޷���ֵ");
                                else {
                                    if (ix >= v.getLength())
                                        messages.add("�����±�" + ix + " Խ��");
                                    else {
                                        SimpleVariable log = translateExp(O_node.getChildren()[0]);
                                        if (log != null) {
                                            if (!v.getType().equals(log.getType())) {
                                                if (v.getType().equals("int")) {
                                                    // ǿ��ת��
                                                    int val = (int) Double.parseDouble(log.getValue());
                                                    messages.add("���Ͳ�ƥ�䣬" + log.getValue() + "ǿ��ת��Ϊ" + val);
                                                    v.getValues().set(ix, String.valueOf(val));
                                                } else if (v.getType().equals("real")) {
                                                    double val = Double.parseDouble(log.getValue());
                                                    messages.add("���Ͳ�ƥ�䣬" + log.getValue() + "�Զ�����ת��Ϊ" + val);
                                                    v.getValues().set(ix, String.valueOf(val));
                                                }
                                            } else
                                                v.getValues().set(ix, log.getValue());
                                            messages.add("�������" + identifier + "��" + ix + "��λ�ñ���ֵΪ" + v.getValues().get(ix)
                                                    + ",���鵱ǰֵΪ" + v.getValues());
                                        }
                                    }
                                }
                            }
                        } else {
                            if (type == null) {
                                //TODO ��ά�������
                                messages.add("������һ����������ֵ");
                                return;
                            }
                            // �����ʼ�� O->{ Y }
                            ASTNode Y_node = O_node.getChildren()[1];
                            ArrayList<String> vals;
                            int i = ix;
                            if (Y_node.getMaxChildNum() == 0) {
                                vals = new ArrayList<>();
                                // ��������Ϊ��ʱ��ȫ��������ʼֵ
                                if (type.equals("int")) {
                                    while (i-- > 0) {
                                        vals.add(String.valueOf(0));
                                    }
                                } else if (type.equals("real")) {
                                    while (i-- > 0) {
                                        vals.add(String.valueOf(0.0));
                                    }
                                }
                            } else {
                                // �������ֵ
                                ArrayList<SimpleVariable> array_values = translateY(Y_node);
                                vals = convertArray(array_values, type);
                                if (vals.size() > ix) {
                                    messages.add("���ڳ�ʼ����������Ԫ�ع��࣬�Զ�ȫ�����˳�ֵ");
                                    vals = new ArrayList<>();
                                    if (type.equals("int")) {
                                        while (i-- > 0) {
                                            vals.add(String.valueOf(0));
                                        }
                                    } else if (type.equals("real")) {
                                        while (i-- > 0) {
                                            vals.add(String.valueOf(0.0));
                                        }
                                    }
                                } else {
                                    //����Ԫ�ز���ʱ���Զ�����ʼֵ
                                    i = vals.size();
                                    while (i < ix) {
                                        vals.add(type.equals("int") ? String.valueOf(0) : String.valueOf(0.0));
                                        i++;
                                    }
                                }
                            }
                            ArrayVariable arrayVariable = new ArrayVariable(identifier, type,
                                    vals.size(), vals, level);
                            if (arrayTable.addVariable(arrayVariable))
                                messages.add("�������" + identifier + "������Ϊ" + type +
                                        "�Ͳ�����ʼ��Ϊ " + arrayVariable.getValues().toString());
                            else
                                //TODO ��������ʱȫ����ʼ��Ϊ ��ʼֵ
                                messages.add("�������" + identifier + "�ѱ�������,�޷���ʼ����");
                        }
                    }
                }
            }
        }
    }

    // when Y->Logic C', C'->, Y
    private ArrayList<SimpleVariable> translateY(ASTNode Y) {
        ArrayList<SimpleVariable> variables = new ArrayList<>();
        ASTNode logic = Y.getChildren()[0];
        SimpleVariable logic_value = translateExp(logic);
        variables.add(logic_value);

        ASTNode C_ = Y.getChildren()[1];
        if (C_.getMaxChildNum() != 0) {
            variables.addAll(translateY(C_.getChildren()[1]));
        }
        return variables;
    }

    // ��Ҫ���ж�·��ֵ
//    private SimpleVariable translateLogic(ASTNode logic){
//        SimpleVariable log = null;
//
//
//        return log;
//    }

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
                // ջ�����ŵ����ȼ����߻���ȣ����ϣ� TODO ��·��ֵ
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
                reVar = new SimpleVariable(v1.getName(), "int", String.valueOf(a1 * a2), level);
            } else if (top.equals("/")) {
                if (a2 == 0) {
                    messages.add("�����������ֵ�Զ���Ϊ0");
                    reVar = new SimpleVariable(v1.getName(), "int", "0", level);
                } else {
                    messages.add(a1 + " / " + a2 + " = " + (a1 / a2));
                    reVar = new SimpleVariable(v1.getName(), "int", String.valueOf(a1 / a2), level);
                }
            } else if (top.equals("+")) {
                messages.add(a1 + " + " + a2 + " = " + (a1 + a2));
                reVar = new SimpleVariable(v1.getName(), "int", String.valueOf(a1 + a2), level);
            } else if (top.equals("-")) {
                messages.add(a1 + " - " + a2 + " = " + (a1 - a2));
                reVar = new SimpleVariable(v1.getName(), "int", String.valueOf(a1 - a2), level);
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
                else if (top.equals("||")) {
                    val = a1 != 0 ? 1 : 0;
                    if (val == 1) // �����ϵĶ�·��ֵ
                        val = ((a1 != 0) || (a2 != 0)) ? 1 : 0;
                } else if (top.equals("&&")) {
                    val = a1 != 0 ? 1 : 0;
                    if (val == 1) // �����ϵĶ�·��ֵ
                        val = ((a1 != 0) && (a2 != 0)) ? 1 : 0;
                } else
                    messages.add("�����������");
                messages.add(a1 + top + a2 + " = " + val);
                reVar = new SimpleVariable(v1.getName(), "int", String.valueOf(val), level);
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
                reVar = new SimpleVariable(v1.getName(), "real", String.valueOf(a1 * a2), level);
            } else if (top.equals("/")) {
                if (a2 == 0.0) {
                    messages.add("�����������ֵ�Զ���Ϊ 0.0");
                    reVar = new SimpleVariable(v1.getName(), "real", "0.0", level);
                } else {
                    messages.add(a1 + " / " + a2 + " = " + (a1 / a2));
                    reVar = new SimpleVariable(v1.getName(), "real", String.valueOf(a1 / a2), level);
                }
            } else if (top.equals("+")) {
                messages.add(a1 + " + " + a2 + " = " + (a1 + a2));
                reVar = new SimpleVariable(v1.getName(), "real", String.valueOf(a1 + a2), level);
            } else if (top.equals("-")) {
                messages.add(a1 + " - " + a2 + " = " + (a1 - a2));
                reVar = new SimpleVariable(v1.getName(), "real", String.valueOf(a1 - a2), level);
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
                else if (top.equals("||")) {
                    val = a1 != 0.0 ? 1 : 0;
                    if (val == 1) // �����ϵĶ�·��ֵ
                        val = ((a1 != 0.0) || (a2 != 0)) ? 1 : 0;
                } else if (top.equals("&&")) {
                    val = a1 != 0.0 ? 1 : 0;
                    if (val == 1) // �����ϵĶ�·��ֵ
                        val = ((a1 != 0.0) && (a2 != 0)) ? 1 : 0;
                } else
                    messages.add("�����������");
                messages.add(a1 + top + a2 + " = " + val);
                reVar = new SimpleVariable(v1.getName(), "int", String.valueOf(val), level);
            }
        }
        return reVar;
    }

    // "Variable-> ..." TODO ȡֵ��ʱ��û�У��ͷ���Ĭ��ֵ������ Null ����ϲ�������㣩
    private SimpleVariable translateVariable(ASTNode variable_node) {
        SimpleVariable variable = null;
        if (variable_node.getMaxChildNum() == 1) {
            // "Variable->Digit"
            ASTNode digit_node = variable_node.getChildren()[0];
            ASTNode positive_node = digit_node.getChildren()[digit_node.getMaxChildNum() - 1];
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
                    if (id == null) {
                        messages.add("���� " + identifier + "δ���������޷�ʹ��,�Զ�����Ĭ��ֵ 0");
                        //TODO ��Щ�����Ƿ�Ӧ�ö�������
                        variable = new SimpleVariable(identifier, "int", "0", level);
                    } else {
                        if (id.getValue() == null) {
                            messages.add("���� " + identifier + "û�б���ʼ�����޷�ʹ�ã��Զ�����Ĭ��ֵ 0");
                            variable = new SimpleVariable(identifier, "int", "0", level);
                        } else
                            variable = id;
                    }
                } else {
                    // ����ȡ�±��ֵ TODO ����ȷ�򷵻� Ĭ��ֵ 0
                    SimpleVariable index = translateExp(index_node.getChildren()[1]); //Logical expression
                    if (index.getType().equals("real")) {
                        messages.add("�����±� " + index.getValue() + "����ΪС�����Զ�����Ĭ��ֵ 0");
                        new SimpleVariable(null, "int", "0", level);
                    } else if (Integer.parseInt(index.getValue()) < 0) {
                        messages.add("�����±�" + index.getValue() + "����Ϊ����,�Զ�����Ĭ��ֵ 0");
                        variable = new SimpleVariable(null, "int", "0", level);
                    } else {
                        ArrayVariable arrayVariable = arrayTable.getArray(identifier);
                        if (arrayVariable == null) {
                            messages.add("�������" + identifier + "δ�������޷�ʹ�ã��Զ�����Ĭ��ֵ 0");
                            variable = new SimpleVariable(null, "int", "0", level);
                        } else {
                            // ����±�Խ�磬δ��ֵ������
                            if (arrayVariable.getValues() == null || arrayVariable.getValues().size() == 0) {
                                messages.add("����" + identifier + "δ����ֵ���޷�ʹ�ã��Զ�����Ĭ��ֵ 0");
                                variable = new SimpleVariable(null, "int", "0", level);
                            } else {
                                Integer ix = Integer.valueOf(index.getValue());
                                if (ix > arrayVariable.getLength() - 1) {
                                    messages.add("����" + identifier + "�±�" + ix + "Խ��,�Զ�����Ĭ��ֵ 0");
                                    variable = new SimpleVariable(null, "int", "0", level);
                                } else {
                                    ArrayList<String> array = arrayVariable.getValues();
                                    // ����������һ����ֵ
                                    variable = new SimpleVariable(null, arrayVariable.getType(), array.get(ix), level);
                                }
                            }
                        }
                    }
                }
            } else {
                // ��������
                ArrayList<SimpleVariable> arguments = translateArgument(call_node.getChildren()[1]);
                FunctionVariable func = functionTable.getVar(identifier);
                if (func == null) {
                    messages.add("����" + identifier + "δ�����޷����ã��Զ�����Ĭ��ֵ 0");
                    variable = new SimpleVariable(null, "int", "0", level);
                } else {
                    ArrayList<Object> parameters = func.getParameters();
                    if (parameters.size() != arguments.size()) {
                        messages.add("����" + identifier + "����ʱ����������ƥ�䣬�Զ�����Ĭ��ֵ 0");
                        variable = new SimpleVariable(null, "int", "0", level);
                    } else {
                        boolean canExecute = true;
                        for (int i = 0; i < arguments.size(); i++) {
                            if (parameters.get(i) instanceof ArrayVariable) {
                                messages.add("����" + identifier + "�ĵ�" + i + "������"
                                        + ((ArrayVariable) parameters.get(i)).getArrayName() + "����Ϊ�������������ò������Ͳ�ƥ�䣬�Զ�����Ĭ��ֵ 0");
                                canExecute = false;
                                variable = new SimpleVariable(null, "int", "0", level);
                                break;
                            }
                            SimpleVariable par = (SimpleVariable) parameters.get(i);
                            SimpleVariable arg = arguments.get(i);
                            // ������ľֲ��������������������ͬ
                            SimpleVariable local = new SimpleVariable(par.getName(), par.getType(), null, level + 1);
                            if (!par.getType().equals(arg.getType())) {
                                if (par.getType().equals("int")) {
                                    // ǿ��ת��
                                    int val = (int) Double.parseDouble(arg.getValue());
                                    messages.add("�������Ͳ�ƥ�䣬" + arg.getValue() + "ǿ��ת��Ϊ" + val);
                                    local.setValue(String.valueOf(val));
                                } else if (par.getType().equals("real")) {
                                    double val = Double.parseDouble(arg.getValue());
                                    messages.add("�������Ͳ�ƥ�䣬" + arg.getValue() + "�Զ�����ת��Ϊ" + val);
                                    local.setValue(String.valueOf(val));
                                }
                            } else
                                local.setValue(arg.getValue());
                            simpleTable.addVariable(local); // ��ӽ��������У���ǰ�ĸ� level
                        }
                        if (canExecute) {
                            // ִ�к����еĳ���
                            level++;
                            proNum++; //�ӳ��������һ
                            messages.add("���ڵ��ú���" + identifier);
                            // ����������ջ
                            returnTypeStack.addFirst(func.getType());

                            func.getPro_node().flushFindTag(); // ����֮ǰ�͵�flush
                            translate(func.getPro_node());
                            proNum--;
                            // �ѷ���ֵ���� variable������
                            if (returnVal == null) {
                                messages.add("�������ú�û�з���ֵ���Զ�����Ĭ��ֵ 0");
                                variable = new SimpleVariable(null, "int", "0", level - 1);
                            } else {
                                messages.add("�������ú󷵻���ֵ��" + returnVal.getValue());
                                variable = returnVal;
                            }
                            simpleTable.deleteVariable(level);
                            arrayTable.deleteArrays(level);
                            level--;
                        } else {
                            simpleTable.deleteVariable(level + 1);
                            arrayTable.deleteArrays(level + 1);
                        }

                    }
                }

            }
        } else if (variable_node.getMaxChildNum() == 3) {
            // "Variable->( Relation )"
            variable = translateExp(variable_node.getChildren()[1]);
        } else if (variable_node.getMaxChildNum() == 4){
            ASTNode id = variable_node.getChildren()[0];
            // print��������
            if (id.getValue().equals("print")){
                ASTNode logic = variable_node.getChildren()[2];
                SimpleVariable log = translateExp(logic);
                messages.add("������ print����������Ļ�����" + log.getValue()+",����Ĭ��ֵ 1");
                printList.add(log.getValue()); // �������ջ
                variable = new SimpleVariable(null,"int","1",level);
            }
            // scan��������
            else if (id.getValue().equals("scan")){
                Scanner scanner = new Scanner(System.in);
                // �õ�Ҫ��ֵ�ı��� logic expression
                SimpleVariable var = translateExp(variable_node.getChildren()[2]);

                SimpleVariable vvv = simpleTable.getVar(var.getName()); // �õ������Ѿ������ı���
                // TODO
                if (vvv != null){
                    System.out.println("����ִ�� scan����ʼ����ֵ������" + var.getName());
                    Double inp = scanner.nextDouble();
                    if (vvv.getType().equals("int")){
                        messages.add("scanʱǿ��ת��");
                        int i = (int)inp.doubleValue();
                        vvv.setValue(String.valueOf(i));
                    }else
                        vvv.setValue(String.valueOf(inp));
                    messages.add("�������ܲ�����ֵΪ" + String.valueOf(inp) +",����Ĭ��ֵ 1");
                    variable = new SimpleVariable(null,"int","1",level);
                }else {
                    messages.add("����" + var.getName() + "δ�������޷�scan�õ�ֵ������Ĭ��ֵ 0");
                    variable = new SimpleVariable(null,"int","0",level);
                }
            }
        }
        return variable;
    }

    // TODO ���Ҫ����������Ļ�������Ҫ�ķ���ʵ�� ָ�빦�ܣ������ݶ�����Ĳ���Ϊ�򵥱���
    private ArrayList<SimpleVariable> translateArgument(ASTNode argument) {
        ArrayList<SimpleVariable> args = new ArrayList<>();
        if (argument.getMaxChildNum() != 0) {
            ASTNode logic = argument.getChildren()[0];
            SimpleVariable log = translateExp(logic);
            args.add(log);
            if (argument.getChildren()[1].getMaxChildNum() != 0) // CCC->, Argument
                args.addAll(translateArgument(argument.getChildren()[1].getChildren()[1]));
        }
        return args;
    }

    //�ѱ����б�ת�� Value�� String�б����Լ����������Ƿ�ƥ�䣬�����Զ�ת����ǿ��ת������ת��ԭʼ��ֵ
    private ArrayList<String> convertArray(ArrayList<SimpleVariable> arrayList, String type) {
        // ����������ֵ�����Ͷ��� match type ��
        ArrayList<String> list = new ArrayList<>();
        for (SimpleVariable var : arrayList) {
            if (!var.getType().equals(type)) {
                if (var.getType().equals("int")) {
                    double val = Double.parseDouble(var.getValue());
                    messages.add("���Ͳ�ƥ�䣬" + var.getValue() + "�Զ�����ת��Ϊ" + val);
                    list.add(String.valueOf(val));
                } else if (var.getType().equals("real")) {
                    int val = (int) Double.parseDouble(var.getValue());
                    messages.add("���Ͳ�ƥ�䣬" + var.getValue() + "ǿ��ת��Ϊ" + val);
                    list.add(String.valueOf(val));
                }
            } else
                list.add(var.getValue());
        }
        return list;
    }

    private static void testWhileIf() {
        Translator t = new Translator();
        String pro = "int a =20;scan(a);print(a);";

        Wrapper w = Executor.analyze(pro);

        t.simpleTable.addVariable(new SimpleVariable("p", "int", "0", 1));
        ArrayList<String> values = new ArrayList<>();
        values.add("43");
        values.add("90");
        t.arrayTable.addVariable(new ArrayVariable("p2", "int", 2, values, 1));

        List<String> msg = w.getMessages();
        for (String m : msg)
            System.out.println(m);
    }

    private static void testDeclareAssign() {
        Translator t = new Translator();
        ASTNode p = Executor.analyze("p = p2[2 -1*9 && 0==12 >=12 <>100 || -99]-1.2;").getAstNode();
        ASTNode declare = p.getChildren()[0].getChildren()[0];

        t.simpleTable.addVariable(new SimpleVariable("p", "real", "12.3", 1));
        ArrayList<String> values = new ArrayList<>();
        values.add("43");
        values.add("90");
        t.arrayTable.addVariable(new ArrayVariable("p2", "int", 2, values, 1));

        t.translate(declare);
        System.out.println(t.messages);
    }

    private static ASTNode testIdArrayVariable() {
        Translator translator = new Translator();
        translator.simpleTable.addVariable(new SimpleVariable("pp", "int", null, 1));
        ArrayList<String> values = new ArrayList<>();
        values.add("43");
        values.add("90");
        translator.arrayTable.addVariable(new ArrayVariable("pp", "int", 2, values, 1));

        ASTNode p = Executor.analyze("pp[2 -1*9 && 0==12 >=12 <>100 && 0];").getAstNode();
        ASTNode logic = p.getChildren()[0].getChildren()[0];
        SimpleVariable s1 = translator.translateExp(logic);
        System.out.println("ȡ����Ϣ��" + translator.messages);

        return logic;
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
        testWhileIf();

    }
}
