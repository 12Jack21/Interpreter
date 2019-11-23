package org.john.interpreter.Service.ExecUtils;

import org.apache.el.lang.ELArithmetic;
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

    private LinkedList<String> printList;
    private LinkedList<String> scanList;

    public Translator() {
        simpleTable = new SimpleTable();
        arrayTable = new ArrayTable();
        functionTable = new FunctionTable();
        returnTypeStack = new LinkedList<>();
        printList = new LinkedList<>();
        level = 1;
    }

    public void setScanList(LinkedList<String> scanList) {
        this.scanList = scanList;
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
                boolean exeELSEIF = false;
                ASTNode ELSEIF = root.getChildren()[5];
                // �ж��Ƿ���else if
                if (ELSEIF.getMaxChildNum() != 0) {
                    ArrayList<ASTNode> logics = translateELSEIF(ELSEIF);
                    int num = 0;
                    while (num < logics.size()) {
                        // һ��һ�����ж� else if ������
                        re = (int) Double.parseDouble(translateExp(logics.get(num)).getValue()) == 1;
                        if (re) {
                            messages.add("�����" + (num + 1) + "�� else if ��䣬ִ�иÿ�ĳ���");
                            exeELSEIF = true;
                            break;
                        }
                        num++;
                    }
                    if (exeELSEIF) {
                        // �ҵ�ִ�е� else if ��
                        while (num-- > 0) {
                            ELSEIF = ELSEIF.getChildren()[6];
                        }
                        ASTNode H_node = ELSEIF.getChildren()[5];
                        translate(H_node); // ִ�� H_node
                    }
                }

                // else if ���������������ʱ
                if (!exeELSEIF) {
                    if (root.getChildren()[6].getMaxChildNum() != 0) {
                        // ELSE->else H
                        messages.add("ִ�� else��ĳ���");
                        translate(root.getChildren()[5].getChildren()[1]);
                    }
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
        } else if (name.equals("FOR")) {
            // TODO like while
            whileNum++;
            if (root.getChildren()[3].getMaxChildNum() != 0) {
                ASTNode DA = root.getChildren()[2];
                ASTNode LO = root.getChildren()[3];
                ASTNode AS = root.getChildren()[5];
                if (DA.getMaxChildNum() == 1) {
                    level++;
                    translate(DA.getChildren()[0]); // Ϊ�˽��������������������
                    level--;
                } else if (DA.getMaxChildNum() == 2) {
                    ASTNode C_node = DA.getChildren()[0].getChildren()[1];
                    translate(DA.getChildren()[0].getChildren()[0]); // translate assignment
                    while (C_node.getMaxChildNum() != 0) {
                        translate(C_node.getChildren()[1]);
                        C_node = C_node.getChildren()[2];
                    }
                }// Ϊ���򲻹�

                ASTNode logic = LO.getChildren()[0];
                SimpleVariable log = translateExp(logic);
                boolean re = (int) Double.parseDouble(log.getValue()) == 1;
                logic.flushFindTag();
                level++;
                while (re) {
                    messages.add("����forѭ��������ִ��ѭ�������");
                    // �������� H_node ���߼�
                    ASTNode H_node = root.getChildren()[7];
                    translate(H_node.getMaxChildNum() == 1 ? H_node.getChildren()[0] : H_node.getChildren()[1]);
                    if (AS.getMaxChildNum() != 0) {
                        // ִ�еڶ����ֺ�֮��� ��ֵ���
                        ASTNode C_node = AS.getChildren()[0].getChildren()[1];
                        translate(AS.getChildren()[0].getChildren()[0]); // execute assignment
                        while (C_node.getMaxChildNum() != 0) {
                            translate(C_node.getChildren()[1]);
                            C_node = C_node.getChildren()[2];
                        }
                        // ˢ���Թ��´�����
                        AS.flushFindTag();
                    }

                    if (toBreak)
                        break;
                    re = (int) Double.parseDouble(translateExp(logic).getValue()) == 1;
                    logic.flushFindTag();
                    toContinue = false;
                    root.getChildren()[7].flushFindTag();
                }
                simpleTable.deleteVariable(level);
                arrayTable.deleteArrays(level);
                level--;
                if (toBreak) //TODO ��� while Ƕ�׵��������Ҫ��ջ����ȡ break ��
                    toBreak = false;
                else
                    messages.add("������forѭ��������ѭ���˳�");
                whileNum--;

            } else {
                // TODO ��������ѭ�������������� break
            }

        } else if (name.equals("Logic")) {
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

    private ArrayList<ASTNode> translateELSEIF(ASTNode ELSEIF) {
        ArrayList<ASTNode> logics = new ArrayList<>();
        logics.add(ELSEIF.getChildren()[3]);
        if (ELSEIF.getChildren()[6].getMaxChildNum() != 0) {
            logics.addAll(translateELSEIF(ELSEIF.getChildren()[6]));
        }
        return logics;
    }

    // when CC->, Parameter, like translateY�� just accept simple variable
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

    // ���� Index �ڵ��ж�Ϊ��ά����
    private ArrayList<SimpleVariable> translateIndex(ASTNode index) {
        ArrayList<SimpleVariable> indexs = new ArrayList<>();
        if (index.getMaxChildNum() != 0) {
            indexs.add(translateExp(index.getChildren()[1]));
            indexs.addAll(translateIndex(index.getChildren()[3]));
        }
        return indexs;
    }

    // ר�Ŵ�������ת�� �ĸ�ֵ
    private SimpleVariable typeHandle(SimpleVariable v, SimpleVariable logic_value) {
        if (logic_value.getType().equals("string")) {
            if (v.getType().equals("int"))
                v.setValue("0");
            else if (v.getType().equals("real"))
                v.setValue("0.0");
            else if (v.getType().equals("char"))
                v.setValue("\0");
            messages.add("�Ƿ�ʹ�� string���Զ�����Ĭ��ֵ " + v.getValue());
        } else {
            if (!v.getType().equals(logic_value.getType())) {
                if (v.getType().equals("int") && logic_value.getType().equals("real")) {
                    // ǿ��ת��
                    int val = (int) Double.parseDouble(logic_value.getValue());
                    messages.add("���Ͳ�ƥ�䣬" + logic_value.getValue() + "ǿ��ת��Ϊ" + val);
                    v.setValue(String.valueOf(val));
                } else if (v.getType().equals("real") && logic_value.getType().equals("int")) {
                    double val = Double.parseDouble(logic_value.getValue());
                    messages.add("���Ͳ�ƥ�䣬" + logic_value.getValue() + "�Զ�����ת��Ϊ" + val);
                    v.setValue(String.valueOf(val));
                } else if (v.getType().equals("char") && logic_value.getType().equals("int")) {
                    // int������û�ж�Ӧ�ַ�������
                    char val = (char) Integer.parseInt(logic_value.getValue());
                    messages.add("���Ͳ�ƥ�䣬" + logic_value.getValue() + "ǿ��ת��Ϊ" + val);
                    v.setValue(String.valueOf(val));
                } else if (v.getType().equals("int") && logic_value.getType().equals("char")) {
                    int val = (int) logic_value.getValue().charAt(0);
                    messages.add("���Ͳ�ƥ�䣬" + logic_value.getValue() + "�Զ�����ת��Ϊ" + val);
                    v.setValue(String.valueOf(val));
                } else if (v.getType().equals("char") && logic_value.getType().equals("real")) {
                    char val = (char) Double.parseDouble(logic_value.getValue()); //side effect
                    messages.add("���Ͳ�ƥ�䣬" + logic_value.getValue() + "�Զ�ת��Ϊ" + val);
                    v.setValue(String.valueOf(val));
                } else if (v.getType().equals("real") && logic_value.getType().equals("char")) {
                    // ��ת��int����ת��character
                    double val = Double.parseDouble(String.valueOf((int) logic_value.getValue().charAt(0)));
                    messages.add("���Ͳ�ƥ�䣬" + logic_value.getValue() + "ǿ��ת��Ϊ" + val);
                    v.setValue(String.valueOf(val));
                }
            } else
                v.setValue(logic_value.getValue());
        }
        return v;
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
                        v = typeHandle(v, logic_value); //ֱ�ӽ������ʹ���
                        messages.add("����" + identifier + "����ֵΪ" + v.getValue());
                    } else {
                        // �����ͳ�ʼ������ --------------------------------
                        // Ϊ����ʡ����һ�������Ŀ���
                        logic_value = typeHandle(new SimpleVariable(null, type, null, level), logic_value);
                        logic_value.setName(identifier);
                        if (!simpleTable.addVariable(logic_value))
                            messages.add("����" + identifier + "�ѱ�������");
                        else
                            messages.add("����" + identifier + "������Ϊ" + type +
                                    "�Ͳ���ʼ��Ϊ" + logic_value.getValue());
                    }
                }
            }
        } else { // �������飬��������±��λ�ø�ֵ
            ArrayList<SimpleVariable> dimension_logics = translateIndex(index_node);
            ArrayList<Integer> dimension_index = new ArrayList<>();// �±��б�
            // ����±��Ƿ�Ϸ�,���Ϸ����Զ��˳�
            for (SimpleVariable s : dimension_logics) {
                if (s.getType().equals("real")) {
                    messages.add("�����±겻����ΪС��" + s.getValue() + " ��ֻ��Ϊ������");
                    return;
                } else {
                    int ix = Integer.parseInt(s.getValue());
                    if (ix < 0) {
                        messages.add("����ʱ�±겻����Ϊ����" + s.getValue() + " ��ֻ��Ϊ������");
                        return;
                    } else
                        dimension_index.add(ix);
                }
            }

            // �±��Ѿ������˲�ΪС���͸���������
            if (X_node.getMaxChildNum() == 0) {
                // ֻ������û�г�ʼ�������
                if (type == null) //��ֵʱ�����������
                    return;
                // ��ӱ����� �������ű���  -δ��ֵ��ʹ�����⣬��ϵ translateVariable()
                ArrayList<String> zeroValues = new ArrayList<>();
                int total = 1; //�ܵ�������Ԫ������
                for (Integer ix : dimension_index)
                    total *= ix;
                if (type.equals("int")) {
                    while (total-- > 0) {
                        zeroValues.add(String.valueOf(0));
                    }
                } else if (type.equals("real")) {
                    while (total-- > 0) {
                        zeroValues.add(String.valueOf(0.0));
                    }
                } else if (type.equals("char")) {
                    while (total-- > 0)
                        zeroValues.add(String.valueOf('\0')); // �ַ�Ĭ��ֵ \0
                }
                if (!arrayTable.addVariable(new ArrayVariable(identifier, type, dimension_index, zeroValues, level)))
                    messages.add("�������" + identifier + "�ѱ���������");
                else {
                    String msg = "�������" + identifier + "������Ϊ" + type + "��,ά��Ϊ " + dimension_index.toString();
                    msg += " ,���Զ���ʼ��Ϊ" + zeroValues;
                    messages.add(msg);
                }
            } else {
                // X ->= O�������ų�ʼ������ֵ�������
                ASTNode O_node = X_node.getChildren()[1];
                if (O_node.getMaxChildNum() != 3) { //TODO add string
                    if (type != null) {
                        ArrayList<String> zeroValues = new ArrayList<>();
                        SimpleVariable log_val = translateExp(O_node.getChildren()[0]);
                        if (log_val.getType().equals("string") && type.equals("char")) {
                            // �� string ����ʼ�� ��ά���άchar����
                            String val = log_val.getValue();
                            int total = 1; //�ܵ�������Ԫ������
                            for (Integer ix : dimension_index)
                                total *= ix;
                            // ����������һ�� \0
                            if (val.length() > total - 1) {
                                messages.add("������ʼ�����ַ���������");
                            } else {
                                for (int i = 0; i < val.length(); i++)
                                    zeroValues.add(String.valueOf(val.charAt(i)));
                            }
                            int start = zeroValues.size();
                            // �������Զ�������ʼֵ \0,�����ַ������� \0
                            while (start++ < total)
                                zeroValues.add(String.valueOf('\0'));  // ֱ��ʹ�� "\0" �Ƿ���ͬ��
                        } else {
                            messages.add("�����õ����ı��ʽ����ʼ������char��֮�������" + identifier);
                            // ���ܳ�ʼ�������Զ�����
                            int total = 1; //�ܵ�������Ԫ������
                            for (Integer ix : dimension_index)
                                total *= ix;
                            if (type.equals("int")) {
                                while (total-- > 0) {
                                    zeroValues.add(String.valueOf(0));
                                }
                            } else if (type.equals("real")) {
                                while (total-- > 0) {
                                    zeroValues.add(String.valueOf(0.0));
                                }
                            } else if (type.equals("char")) {
                                while (total-- > 0)
                                    zeroValues.add(String.valueOf('\0'));
                            }
                        }
                        if (!arrayTable.addVariable(new ArrayVariable(identifier, type, dimension_index, zeroValues, level)))
                            messages.add("�������" + identifier + "�ѱ���������");
                        else {
                            String msg = "�������" + identifier + "������Ϊ" + type + "��,ά��Ϊ " + dimension_index.toString();
                            msg += " ,���Զ���ʼ��Ϊ" + zeroValues;
                            messages.add(msg);
                        }
                    } else {
                        // �����±�λ�� ��ֵ�����
                        ArrayVariable v = arrayTable.getArray(identifier);
                        if (v == null)
                            messages.add("�������δ�������޷���ֵ");
                        else {
                            // �ж��±��Ƿ����
                            if (dimension_index.size() != v.getDimensionList().size()) {
                                messages.add("�����±�������ƥ�䣬�޷���ֵ");
                                return;
                            }
                            ArrayList<Integer> dimensionList = v.getDimensionList();
                            // �ж��±��Ƿ�Խ�磬 ͬʱ����"����"�洢���±�
                            int real_index = 0;
                            for (int i = 0, ji = 2, c = 10; i < dimensionList.size(); i++) {
                                int temp = 1;
                                if (dimension_index.get(i) >= dimensionList.get(i)) {
                                    messages.add("�� " + i + " �������±�Խ��!");
                                    return;
                                } else {
                                    // ���һ��ά�Ȳ��ܳ�
                                    for (int j = i + 1; j < dimensionList.size(); j++)
                                        temp *= dimensionList.get(j);
                                    real_index += dimension_index.get(i) * temp;
                                }
                            }

                            SimpleVariable log = translateExp(O_node.getChildren()[0]);
                            if (log != null) {
//                                if (!v.getType().equals(log.getType())) { TODO δ��������
//                                    if (v.getType().equals("int")) {
//                                        // ǿ��ת��
//                                        int val = (int) Double.parseDouble(log.getValue());
//                                        messages.add("���Ͳ�ƥ�䣬" + log.getValue() + "ǿ��ת��Ϊ" + val);
//                                        v.getValues().set(real_index, String.valueOf(val));
//                                    } else if (v.getType().equals("real")) {
//                                        double val = Double.parseDouble(log.getValue());
//                                        messages.add("���Ͳ�ƥ�䣬" + log.getValue() + "�Զ�����ת��Ϊ" + val);
//                                        v.getValues().set(real_index, String.valueOf(val));
//                                    }
//                                } else
//                                    v.getValues().set(real_index, log.getValue());

                                SimpleVariable val_variable = typeHandle(new SimpleVariable(null, v.getType(), null, level), log);
                                v.getValues().set(real_index, val_variable.getValue());
                                messages.add("�������" + identifier + "��" + real_index + "��'����'λ�ñ���ֵΪ" + v.getValues().get(real_index)
                                        + ",���鵱ǰֵΪ" + v.getValues()); //TODO �޸Ķ�ά���ݵ���ʾ��ʽ
                            }

                        }
                    }
                } else {
                    if (type == null) {
                        messages.add("������һ����������ֵ!");
                        return;
                    }
                    // �����ʼ�� O->{ Y }, ��ά��Ҳת��һά��
                    ASTNode Y_node = O_node.getChildren()[1];
                    ArrayList<String> vals;
                    int total = 1; //�ܵ�������Ԫ������
                    for (Integer ix : dimension_index)
                        total *= ix;
                    int i = total;
                    if (Y_node.getMaxChildNum() == 0) {
                        vals = new ArrayList<>();
                        // ��������Ϊ��ʱ��ȫ��������ʼֵ
                        if (type.equals("int")) {
                            while (i-- > 0)
                                vals.add(String.valueOf(0));
                        } else if (type.equals("real")) {
                            while (i-- > 0)
                                vals.add(String.valueOf(0.0));
                        } else if (type.equals("char")) {
                            while (i-- > 0)
                                vals.add(String.valueOf('\0'));
                        }
                    } else {
                        // �������ֵ O->{ Y }
                        ArrayList<SimpleVariable> array_values = translateY(Y_node);
                        vals = convertArray(array_values, type);
                        if (vals.size() > total) {
                            messages.add("���ڳ�ʼ����������Ԫ�ع��࣬�Զ�ȫ�����˳�ֵ");
                            vals = new ArrayList<>();
                            if (type.equals("int")) {
                                while (i-- > 0)
                                    vals.add(String.valueOf(0));
                            } else if (type.equals("real")) {
                                while (i-- > 0)
                                    vals.add(String.valueOf(0.0));
                            } else if (type.equals("char"))
                                while (i-- > 0)
                                    vals.add(String.valueOf('\0'));
                        } else if (vals.size() < total) {
                            // ����Ԫ�ز���ʱ���Զ�����ʼֵ
                            messages.add("���ڳ�ʼ����������Ԫ�ع��٣��Զ��ó�ֵ�����ʣ�µ�Ԫ��");
                            i = vals.size();
                            while (i < total) {
                                vals.add(type.equals("int") ? String.valueOf(0) :
                                        (type.equals("real") ? String.valueOf(0.0) : String.valueOf('\0')));
                                i++;
                            }
                        }
                    }
                    ArrayVariable arrayVariable = new ArrayVariable(identifier, type,
                            dimension_index, vals, level);
                    if (arrayTable.addVariable(arrayVariable))
                        messages.add("�������" + identifier + "������Ϊ" + type +
                                "�Ͳ�����ʼ��Ϊ " + arrayVariable.getValues().toString());
                    else
                        //TODO ��������ʱȫ����ʼ��Ϊ ��ʼֵ
                        messages.add("�������" + identifier + "�ѱ�������,�޷����г�ʼ����");
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
        // �����ڶ��������һ�� ����
        if (C_.getMaxChildNum() != 0 && C_.getChildren()[1].getMaxChildNum() != 0) {
            variables.addAll(translateY(C_.getChildren()[1]));
        }
        return variables;
    }

    // TODO logical expression ��Ҫ���ж�·��ֵ
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
        arith_val = varStack.pop();
        return arith_val;
    }

    // ������֮������㣬���԰��� ��������ϵ���߼�����, �ַ��ܲ����������㣬���ַ���ֻ�ܲ��� �ӷ����� TODO mod operandδʵ��
    private SimpleVariable calculate(SimpleVariable v1, SimpleVariable v2, String top) {
        SimpleVariable reVar = null;
        // ���ַ������ڵ������
        if (v1.getType().equals("string") || v2.getType().equals("string")) {
            if (!top.equals("+")) {
                messages.add(top + " �����в��ܴ����ַ���,�Զ�����Ĭ��ֵ ��");
                return new SimpleVariable(null, "string", "", level);
            } else {
                // Ӧ��ȡ�±�Ϊ 0 ������ 1 ��value�Ѿ���ȥ�� ˫����
                String val = v1.getValue() + v2.getValue();
                return new SimpleVariable(v1.getName(), "string", val, level);
            }
        }
        if (!v1.getType().equals("real") && !v2.getType().equals("real")) {
            // û�� real ����ʱ����������
            if (v1.getType().equals("char") || v2.getType().equals("char"))
                messages.add("char ������" + v1.getValue() + "��" + v2.getValue() + "��������ʱ���Զ���������ת���� int�ٽ�������");

            int a1 = v1.getType().equals("int") ? Integer.parseInt(v1.getValue()) : (int) v1.getValue().charAt(0);
            int a2 = v2.getType().equals("int") ? Integer.parseInt(v2.getValue()) : (int) v2.getValue().charAt(0);
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
            } else if (top.equals("&")){
                messages.add(a1 + " & " + a2 + " = " + (a1 & a2));
                reVar = new SimpleVariable(v1.getName(),"int",String.valueOf(a1 & a2),level);;
            } else if (top.equals("|")){
                messages.add(a1 + " | " + a2 + " = " + (a1 | a2));
                reVar = new SimpleVariable(v1.getName(),"int",String.valueOf(a1 | a2),level);;
            } else if (top.equals("^")){
                messages.add(a1 + " ^ " + a2 + " = " + (a1 ^ a2));
                reVar = new SimpleVariable(v1.getName(),"int",String.valueOf(a1 ^ a2),level);;
            }
            else {
                // ��ϵ���߼�����
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
                    messages.add("����calculate��������");
                messages.add(a1 + top + a2 + " = " + val);
                reVar = new SimpleVariable(v1.getName(), "int", String.valueOf(val), level);
            }

        }// ���������д��� real �����
        else {//TODO left bit operation
            // ����֮����real�ʹ���
            if (v1.getType().equals("real"))
                messages.add("����" + v1.getType() + "��" + v2.getType() + "��ƥ��,�Զ��� " + v2.getValue() + "��������ת��");
            else
                messages.add("����" + v1.getType() + "��" + v2.getType() + "��ƥ��,�Զ��� " + v1.getValue() + "��������ת��");

            double a1 = v1.getType().equals("char") ? Double.parseDouble(String.valueOf((int) v1.getValue().charAt(0))) :
                    Double.parseDouble(v1.getValue());
            double a2 = v2.getType().equals("char") ? Double.parseDouble(String.valueOf((int) v2.getValue().charAt(0))) :
                    Double.parseDouble(v2.getValue());

            if (top.equals("*")) {
                messages.add(a1 + " * " + a2 + " = " + (a1 * a2));
                reVar = new SimpleVariable(v1.getName(), "real", String.valueOf(a1 * a2), level);
            } else if (top.equals("/")) {
                if (a2 == 0.0) {
                    messages.add("�����������ֵ�Զ���Ϊ 0.0");
                    System.err.println("�����������ֵ�Զ���Ϊ 0.0");
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

                // ��ϵ���߼�����
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
                    messages.add("����calculate��������");
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
            String name = variable_node.getChildren()[0].getName();
            if (name.equals("Digit")) {
                // "Variable->Digit"
                ASTNode digit_node = variable_node.getChildren()[0];
                ASTNode positive_node = digit_node.getChildren()[digit_node.getMaxChildNum() - 1];
                String symbol = digit_node.getChildren()[0].getName();
                if (positive_node.getChildren()[0].getName().equals("integer")) {
                    // ������
                    int value = (int)Double.parseDouble(positive_node.getChildren()[0].getValue());
                    if (symbol.equals("-")) //����
                        value = -1 * value;
                    else if (symbol.equals("~"))
                        value = ~value;
                    variable = new SimpleVariable(null, "int", String.valueOf(value), level);
                } else {
                    // С��
                    double value = Double.parseDouble(positive_node.getChildren()[0].getValue());
                    if (symbol.equals("-")) {//����
                        value = -1.0 * value;
                        variable = new SimpleVariable(null, "real", String.valueOf(value), level);
                    } else if (symbol.equals("~")) {
                        int val = ~(int) value;
                        messages.add("~ λ����ʹreal����" + value + "ת��Ϊint���� " + val);
                        variable = new SimpleVariable(null, "int", String.valueOf(val), level);
                    }else
                        variable = new SimpleVariable(null,"real",String.valueOf(value),level);
                }
            } else if (name.equals("character")) {
                // character,�˴����дʷ�����û�н��е� �ַ����ȵļ��
                String char_value = variable_node.getChildren()[0].getValue().split("\'")[1];
                if (char_value.length() != 1) {
                    messages.add("�ַ�" + char_value + "���ȷǷ����Զ�����Ĭ��ֵ '\\0' ");
                    variable = new SimpleVariable(null, "char", String.valueOf('\0'), level);
                } else
                    variable = new SimpleVariable(null, "char", String.valueOf(char_value), level);
            } else if (name.equals("string")) {
                // string
                String val = variable_node.getChildren()[0].getValue().split("\"")[1];
                variable = new SimpleVariable(null, "string", val, level);
            } else if (name.equals("SymbolVar")) {
                String identifier = variable_node.getChildren()[0].getChildren()[1].getValue();
                SimpleVariable id = simpleTable.getVar(identifier);
                String symbol = variable_node.getChildren()[0].getChildren()[0].getValue();
                if (id == null) {
                    messages.add("���� " + identifier + "δ���������޷�ʹ��,�Զ�����Ĭ��ֵ 0");
                    //TODO ��Щ�����Ƿ�Ӧ�ö�������,����ĵط����߳��� setValue �ĵط�Ӧ��Ϊ����
                    variable = new SimpleVariable(identifier, "int", "0", level);
                } else {
                    if (id.getValue() == null) {
                        messages.add("���� " + identifier + "û�б���ʼ�����Զ�����Ĭ��ֵ 0");
                        variable = new SimpleVariable(identifier, "int", "0", level);
                    } else {
                        // ���ÿ��� ����
                        if (symbol.equals("-")) {
                            if (!id.getType().equals("char")) {
                                //TODO ��һ������Ķ������⵼�¸�ֵ�����������б�����ֵ���ı������
                                //���ܼ򵥵ذѸ��ż���ȥ�����ܳ��� ��--2.14�� �޷�ת�������
                                String val = null;
                                if (id.getType().equals("real"))
                                    val = String.valueOf(-1.0 * Double.parseDouble(id.getValue()));
                                else if (id.getType().equals("int"))
                                    val = String.valueOf(-1 * Integer.parseInt(id.getValue()));
                                else
                                    System.err.println("- ����ʱ���ʹ���");
                                variable = new SimpleVariable(identifier,id.getType(),val,level);
                            } else {
                                // char ��������,ת�� int
                                int val = (int) id.getValue().charAt(0) * -1;
                                messages.add("char����" + identifier + "��ֵ" + id.getValue() + "�Զ�תΪ int");
                                variable = new SimpleVariable(identifier, "int", String.valueOf(val), level);
                            }
                        } else if (symbol.equals("~")) {
                            // λ���㲻֧�� real ����
                            if (id.getType().equals("real") || id.getType().equals("string")) {
                                int val = (int)Double.parseDouble(id.getValue());
                                messages.add("~ λ�����֧�� int��char����,real����" +id.getValue()+ "ǿ��ת��Ϊint�� " + val);
                                variable = new SimpleVariable(null, "int", String.valueOf(~ val), level);
                            } else {
                                if (id.getType().equals("char")) {
                                    int val = ~(int) id.getValue().charAt(0);
                                    messages.add("char ������" + id.getValue() + "��~�����Զ�תΪ" + val);
                                    variable = new SimpleVariable(identifier, id.getType(), String.valueOf(val), level);
                                } else if (id.getType().equals("int")) {
                                    int val = ~Integer.parseInt(id.getValue());
                                    variable = new SimpleVariable(identifier, id.getType(), String.valueOf(val), level);
                                } else
                                    System.err.println("~ λ����ʱ�����﷨��������");
                            }
                        } else
                            System.err.println("�����������ʱ�����﷨��������");
                    }
                }
            } else
                System.err.println("�﷨����δͨ����");
        } else if (variable_node.getMaxChildNum() == 2) {
            // "Variable->identifier Call"
            ASTNode call_node = variable_node.getChildren()[1];
            String identifier = variable_node.getChildren()[0].getValue();
            if (call_node.getChildren()[0].getName().equals("Index")) {
                ASTNode index_node = call_node.getChildren()[0];
                if (index_node.getMaxChildNum() == 0) {
                    // �п������ַ�����ȡ���� string����print(a),�������ȿ��� char ����
                    ArrayVariable array = arrayTable.getArray(identifier);
                    if (array != null) {
                        if (!array.getType().equals("char")) {
                            messages.add("���ܵ���ʹ�÷� char�����������!����Ĭ��ֵ 0");
                            variable = new SimpleVariable(identifier, "int", "0", level);
                        } else {
                            // �õ� char �����������ݣ�ƴ�Ӳ�����
                            String s = "";
                            for (String v : array.getValues())
                                s += v;
                            variable = new SimpleVariable(identifier, "string", s, level);
                        }
                    } else {
                        SimpleVariable id = simpleTable.getVar(identifier);
                        if (id == null) {
                            messages.add("���� " + identifier + "δ���������޷�ʹ��,�Զ�����Ĭ��ֵ 0");
                            //TODO ��Щ�����Ƿ�Ӧ�ö�������,����ĵط�Ӧ��Ϊ����
                            variable = new SimpleVariable(identifier, "int", "0", level);
                        } else {
                            if (id.getValue() == null) {
                                messages.add("���� " + identifier + "û�б���ʼ�����Զ�����Ĭ��ֵ 0");
                                variable = new SimpleVariable(identifier, "int", "0", level);
                            } else
                                variable = id;
                        }
                    }
                } else {
                    // ����ȡ�±��ֵ, ����ȷ�򷵻� Ĭ��ֵ 0
//                    SimpleVariable index = translateExp(index_node.getChildren()[1]); //Logical expression
                    ArrayList<SimpleVariable> dimension_logics = translateIndex(index_node);
                    ArrayList<Integer> dimension_index = new ArrayList<>();// �±��б�
                    // ����±��Ƿ�Ϸ�,���Ϸ����Զ��˳�
                    for (SimpleVariable s : dimension_logics) {
                        if (s.getType().equals("real")) {
                            messages.add("ȡֵʱ�����±겻����ΪС��" + s.getValue() + " ��ֻ��Ϊ������,�Զ�����Ĭ��ֵ 0");
                            variable = new SimpleVariable(null, "int", "0", level);
                            return variable;
                        } else {
                            int ix = Integer.parseInt(s.getValue());
                            if (ix < 0) {
                                messages.add("ȡֵʱ����ʱ�±겻����Ϊ����" + s.getValue() + " ��ֻ��Ϊ���������Զ�����Ĭ��ֵ 0");
                                variable = new SimpleVariable(null, "int", "0", level);
                                return variable;
                            } else
                                dimension_index.add(ix);
                        }
                    }

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
                            // �ж��±��Ƿ����
                            if (dimension_index.size() != arrayVariable.getDimensionList().size()) {
                                messages.add("�����±�������ƥ��,�޷�ȡ�����е�ֵ���Զ�����Ĭ��ֵ 0");
                                return new SimpleVariable(null, "int", "0", level);
                            }
                            ArrayList<Integer> dimensionList = arrayVariable.getDimensionList();
                            // �ж��±��Ƿ�Խ�磬 ͬʱ����"����"�洢���±�
                            int real_index = 0;
                            for (int i = 0; i < dimensionList.size(); i++) {
                                int temp = 1;
                                if (dimension_index.get(i) >= dimensionList.get(i)) {
                                    messages.add("�� " + i + " �������±�Խ��!�Զ�����Ĭ��ֵ 0");
                                    return new SimpleVariable(null, "int", "0", level);
                                } else {
                                    for (int j = i + 1; j < dimensionList.size(); j++)
                                        temp *= dimensionList.get(j);
                                    real_index += dimension_index.get(i) * temp;
                                }
                            }
                            ArrayList<String> array = arrayVariable.getValues();
                            // ����������һ����ֵ
                            variable = new SimpleVariable(null, arrayVariable.getType(), array.get(real_index), level);

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
        } else if (variable_node.getMaxChildNum() == 4) {
            ASTNode id = variable_node.getChildren()[0];
            // print��������, print char����ʱ��ֱ�Ӵ�ӡ�������ַ���
            if (id.getValue().equals("print")) {
                ASTNode logic = variable_node.getChildren()[2];
                SimpleVariable log = translateExp(logic);
                messages.add("������ print����������Ļ�����" + log.getValue() + ",����Ĭ��ֵ 1");
                printList.add(log.getValue()); // �������ջ
                System.out.println(log.getValue()); //�������Ļ��
                variable = new SimpleVariable(null, "int", "1", level);
            }
            // scan��������
            else if (id.getValue().equals("scan")) {
                Scanner scanner = new Scanner(System.in);
                // �õ�Ҫ��ֵ�ı��� logic expression
                SimpleVariable var = translateExp(variable_node.getChildren()[2]);
                ArrayVariable array = arrayTable.getArray(var.getName());
                String scanVal = scanList.pop(); // �õ����������
                if (array != null) {
                    // �����������,ֱ�ӽ����ַ����������ж�
                    if (array.getType().equals("char")) {
                        // ���� �ַ���
                        ArrayList<String> values = new ArrayList<>();
                        int total = 1; //�ܵ�������Ԫ������
                        for (Integer ix : array.getDimensionList())
                            total *= ix;
                        // ����������һ�� \0
                        if (scanVal.length() > total - 1) {
                            messages.add("���յ��ַ���������");
                            variable = new SimpleVariable(null, "int", "0", level);
                        } else {
                            for (int i = 0; i < scanVal.length(); i++)
                                values.add(String.valueOf(scanVal.charAt(i)));
                            values.add(String.valueOf('\0')); // TODO �������� \0
                            array.setValues(values);
                            messages.add("char �������" + array.getArrayName() + "�����ַ������뱻��ֵΪ " + array.getValues() + " ,����Ĭ��ֵ 1");
                            variable = new SimpleVariable(null, "int", "1", level);
                        }
                    } else {
                        messages.add("���ַ�����������������ֱ�ӽ������룡");
                        variable = new SimpleVariable(null, "int", "0", level);
                    }
                } else {
                    // ���Ǽ򵥱���
                    SimpleVariable vvv = simpleTable.getVar(var.getName()); // �õ������Ѿ������ı���
                    if (vvv != null) {
                        System.out.println("����ִ�� scan����ʼ����ֵ������" + var.getName());
                        if (vvv.getType().equals("char")) {
                            if (scanVal.length() > 1) {
                                messages.add("������ַ�����!");
                                variable = new SimpleVariable(null, "int", "0", level);
                            } else
                                vvv.setValue(scanVal);
                        } else {
//                        Double inp = scanner.nextDouble();
                            Double inp = Double.valueOf(scanVal);
                            if (vvv.getType().equals("int")) {
                                messages.add("scanʱǿ��ת��");
                                int i = (int) inp.doubleValue();
                                vvv.setValue(String.valueOf(i));
                            } else
                                vvv.setValue(String.valueOf(inp));
                        }
                        messages.add("����" + var.getName() + "���ܲ�����ֵΪ" + vvv.getValue() + ",����Ĭ��ֵ 1");
                        variable = new SimpleVariable(null, "int", "1", level);
                    } else {
                        messages.add("����" + var.getName() + "δ�������޷�scan�õ�ֵ������Ĭ��ֵ 0");
                        variable = new SimpleVariable(null, "int", "0", level);
                    }
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
        for (SimpleVariable var : arrayList)
            list.add(typeHandle(new SimpleVariable(null, type, null, level), var).getValue());
        return list;
    }

    private static void testWhileIf() {
        Translator t = new Translator();
        String pro = "int n = 5;\n" +
                "scan(n);\n" +
                "char c;\n" +
                "scan(c);\n" +
                "print(\"c=\" + c);";

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
        String s = "-1e-10";
        int a = 12;
        double d = Double.parseDouble(s);
        char x = 'w';
        a = -x;
//        testWhileIf();
    }
}
