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
    public static String[] specials = {"identifier", "digit", "r_digit"};

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
            "P->S P", "P->{ P } P","P->",  // att: follow set of P should contains #
            "S->A ;", "S->K ;", "S->II", "S->W","S->N","S->;", //"S->L;"production versus "S->K"
            "S'->{ S' }","S'->S","S'->",
            "A->int K C", "A->real K C", "A->char K C",
            "B->[ R ]", "B->",  //�±������ϵ���ʽ
            "C->, K C","C->",   //������ or ��ֵ
            "K->identifier B X",
            "X->= O","X->",
            "O->R","O->{ Y }", // array assignment
            "Y->R Z","Y->",
            "Z->, Y","Z->",
            "II->if ( L ) H D",    //if
            "D->else H","D->",     //else
            "W->while ( L ) H",    //while , do-----------------undone
            "H->S","H->{ P }",
            "N->break ;","N->continue ;", //break & continue
            "L->R J",           //logic expression
            "J->|| L", "J->&& L", "J->",
            "R->M Q",           //relation expresion
            "Q->== R", "Q-><> R", "Q->>= R", "Q-><= R", "Q->> R", "Q->< R", "Q->",
            "M->E V",
            "V->+ M", "V->- M", "V->", //att: �������ȼ�-�ҽ��
            "E->F T",
            "T->* E", "T->/ E", "T->",
            "F->( R )", "F->identifier B", "F->G",
            "G->U","G->- U","G->+ U", //ѡ���������߸���
            "U->digit","U->r_digit"  //������С��
    };

    // ջ���ս����ƥ��ʱ�����Ա���Ϊ ȱ��ĳ�ս����
    // ջ�����ս����ƥ��ʱ(Select����ƥ��)��
    // 1.Follow��Ҳ��ƥ��
    // 2.Follow��ƥ��


}
