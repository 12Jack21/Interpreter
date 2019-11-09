package org.john.interpreter.Service.ExecUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CodeTable {

    //�ؼ��ʱ�
    public static String[] keys = {"if", "else", "while", "do", "int",
            "real", "char", "print", "scan", "break", "continue"};

    // �� '.' ɾȥ
    public static String[] signs = {"/", "+", "-", "*", "<", ">", "==", "<>", ">=", "<=",
            "&&", "||", "=", "(", ")", "{", "}", "[", "]", "'", "\"", ";",
            ",", "\\n", "\\r", "\\t", "\n", "\r", "\t", "//", "/*", "*/"};
    // ����
    public static String[] specials = {"identifier", "integer", "fraction"};

    public static char[] invs = {'\n', '\r', '\t'};// TODO ת���ַ�����ĿҪ�������֣� '\n' or '\\n'

    public static List<String> keyList = Arrays.asList(keys);

    public static List<String> signList = Arrays.asList(signs);


    //���ַ��� token �� �ֱ��� code ��ӳ��
    public static HashMap<String, Integer> str2IntMap() {
        HashMap<String, Integer> map = new HashMap<>();
        int i;
        for (i = 1; i <= keys.length; i++)
            map.put(keys[i - 1], i);
        for (i = 1; i <= signs.length; i++)
            map.put(signs[i - 1], i + keys.length);
        for (i = 1; i <= specials.length; i++)
            map.put(specials[i - 1], i + keys.length + signs.length);

        map.put("error", -1); // error
        map.put("#",-2);

        return map;
    }

    public static HashMap<Integer,String> int2StrMap(){
        HashMap<Integer,String> map = new HashMap<>();
        for (Map.Entry<String,Integer> entry:str2IntMap().entrySet()){
            map.put(entry.getValue(),entry.getKey());
        }
        return map;
    }

    //����ʽ
    public static String[] production = {
            "Pro->Statement Pro", "Pro->{ Pro } Pro","Pro->; Pro","Pro->",  // att: follow set of Pro should contains #
            "Statement->Declare", "Statement->Assignment ;", "Statement->IF", "Statement->WHILE","Statement->Interrupt", //"S->L;"production versus "S->Declare"

            "Assignment->identifier Index X", // Assignment
            "Declare->Type Assign",         // Declaration
            "Type->int","Type->real","Type->char","Type->void", // Type Specifier
            "Index->[ Relation ]", "Index->", // �±������ϵ���ʽ
            "C->, Assignment C","C->",        //������ or ��ֵ

            "Assign->identifier F",
            "F->( Parameter ) { Pro }","F->Index X C ;",   // define function
            "Parameter->Type identifier Index CC","Parameter->", //function parameters
            "CC->, Parameter","CC->",     // can replace CC with original non-terminal

            "X->= O","X->",
            "O->Relation","O->{ Y }",     // array assignment
            "Y->Relation C'","Y->",
            "C'->, Y","C'->",             // for array assignment {}

            "IF->if ( Logic ) H ELSE",    //if
            "ELSE->else H","ELSE->",      //else
            "WHILE->while ( Logic ) H",   //while , do-----------undone---
            "H->Statement","H->{ Pro }",
            "Interrupt->break ;","Interrupt->continue ;", //break & continue

            "Logic->Relation L",          //logic expression
            "L->|| Logic", "L->&& Logic", "L->",

            "Relation->Arithmetic R",     //relation expresion
            "R->== Relation", "R-><> Relation", "R->>= Relation", "R-><= Relation", "R->> Relation", "R->< Relation", "R->",

            "Arithmetic->Item V",
            "V->+ Arithmetic", "V->- Arithmetic", "V->", //att: �ҽ��
            "Item->Variable Factor",
            "Factor->* Item", "Factor->/ Item", "Factor->",
            "Variable->( Relation )", "Variable->identifier Index", "Variable->Digit",

            "Digit->Positive","Digit->- Positive","Digit->+ Positive", //ѡ���������߸���
            "Positive->integer","Positive->fraction"  //������С��
    };

    /*
    * Thoughts: ����ĳ������ʽ�����Ǹò���ʽ�Ǵ���ģ��������Ա����ȷ�Ĵ���
    *       ie. �����в����б�����ֵ�Ĳ���ʽ�����룬���Ǳ���������
    * */
    /*
     ջ���ս����ƥ��ʱ�����Ա���Ϊ ȱ��ĳ�ս����
     ջ�����ս����ƥ��ʱ(Select����ƥ��)��
     1.Follow��Ҳ��ƥ��
     2.Follow��ƥ��
    */

}
