package org.john.interpreter.Service.ExecUtils;

import org.john.interpreter.Service.SemanticUtils.ArrayTable;
import org.john.interpreter.Service.SemanticUtils.ArrayVariable;
import org.john.interpreter.Service.SemanticUtils.SimpleTable;
import org.john.interpreter.Service.SemanticUtils.SimpleVariable;

import java.util.ArrayList;
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

    // TODO ��������ջ�� ��ֵ��������ջ���з���,�ڼ���� ����������������(type,value)
    private SimpleVariable translateExp(ASTNode exp) {


        return null;
    }


    private SimpleVariable translateLogicExp(ASTNode logic){
        SimpleVariable logic_val = null;


        return logic_val;
    }
    private SimpleVariable translateRelationExp(ASTNode relation){
        SimpleVariable relation_val = null;


        return relation_val;
    }
    // ���� ������ʽ Arithmetic
    private SimpleVariable translateArithmeticExp(ASTNode arithmetic) {
        SimpleVariable arith_val = null;


        return arith_val;
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
        }else if (variable_node.getMaxChildNum() == 2){
            // "Variable->identifier Call"
            ASTNode call_node = variable_node.getChildren()[1];
            String identifier = variable_node.getChildren()[0].getValue();
            if (call_node.getChildren()[0].getName().equals("Index")){
                ASTNode index_node = call_node.getChildren()[0];
                if (index_node.getMaxChildNum() == 0){
                    SimpleVariable id = simpleTable.getVar(identifier);
                    if (id == null)
                        messages.add("���� " + identifier + "δ���������޷�ʹ��");
                    else {
                        if (id.getValue() == null)
                            messages.add("���� " +identifier+"û�б���ʼ�����޷�ʹ��");
                        else
                            variable = id;
                    }
                }else {
                    // ����ȡ�±��ֵ   ��ʱ����Variable���LogicExp�������±�
//                    SimpleVariable index = translateLogicExp(index_node.getChildren()[1]);
                    SimpleVariable index = translateVariable(index_node.getChildren()[1]);

                    if (index.getType().equals("real"))
                        messages.add("�����±� " + index.getValue() +"����ΪС��");
                    else if (Integer.parseInt(index.getValue()) < 0)
                        messages.add("�����±�" + index.getValue()+"����Ϊ����");
                    else{
                        ArrayVariable arrayVariable = arrayTable.getArray(identifier);
                        if (arrayVariable == null)
                            messages.add("�������" +identifier+"δ�������޷�ʹ��");
                        else {
                            //TODO ����±�Խ�磬δ��ֵ������
                            if (arrayVariable.getValues() == null || arrayVariable.getValues().size() == 0)
                                messages.add("����" + identifier + "δ����ֵ���޷�ʹ��");
                            else {
                                Integer ix = Integer.valueOf(index.getValue());
                                if (ix > arrayVariable.getLength() - 1)
                                    messages.add("����" +identifier+"�±�" + ix+"Խ��");
                                else {
                                    ArrayList<String> array = arrayVariable.getValues();
                                    // ����������һ����ֵ
                                    variable = new SimpleVariable(null,arrayVariable.getType(),array.get(ix),level);
                                }
                            }
                        }
                    }
                }
            }else {
                // �������ã�Call->( Argument )
            }
        }else if (variable_node.getMaxChildNum() == 3){
            // "Variable->( Relation )"
            variable = translateRelationExp(variable_node.getChildren()[1]);
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

    private static ASTNode testIdArrayVariable(){
        Translator translator = new Translator();

        translator.simpleTable.addVariable(new SimpleVariable("pp","int",null,1));
        ASTNode variable_node = new ASTNode(2,null,null);
        ASTNode id_node = new ASTNode(0,"identifier","pp");
        ASTNode call_node = new ASTNode(1,"Call",null);
        ASTNode index_node = new ASTNode(0,"Index",null);
        // test fetching value of simple variable
        variable_node.addChild(id_node);
        variable_node.addChild(call_node);
        call_node.addChild(index_node);
        SimpleVariable s = translator.translateVariable(variable_node);
        System.out.println(translator.messages);

        ASTNode var_node = testVariable();
        index_node = new ASTNode(3,"Index",null);
        call_node.getChildren()[0] = index_node;
        // �ٶ� indexֻ��Ϊ variableʱ�ų���
        index_node.getChildren()[1] = var_node;
        ArrayList<String> values = new ArrayList<>();
        values.add("43");
        values.add("90");
        translator.arrayTable.addVariable(new ArrayVariable("pp","int",2,values,1));
        SimpleVariable s1 = translator.translateVariable(variable_node);
        System.out.println(translator.messages);

        return variable_node;
    }
    private static ASTNode testVariable(){
        ASTNode variable_node = new ASTNode(1,null,null);
        ASTNode a1 = new ASTNode(2,"Digit",null);
        ASTNode b0 = new ASTNode(0,"+",null);
        ASTNode b1 = new ASTNode(1,"Positive",null);
        ASTNode c0 = new ASTNode(0,"integer","0");
        variable_node.addChild(a1);
        a1.addChild(b0);
        a1.addChild(b1);
        b1.addChild(c0);
        Translator translator = new Translator();
        SimpleVariable s = translator.translateVariable(variable_node);
        System.out.println(s);

        return variable_node;
    }
    public static void main(String[] args){
        testIdArrayVariable();
    }
}
