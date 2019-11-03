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
    public static String[] specials = {"identifier", "digit", "rdigit"};

    // ��Ӧ���ս����
    public static char[] key_ends = {'f', 'e', 'w', 'd', 'i', 'r', 'c', 'p', 's', 'b', 'o'};
    public static char[] sign_ends = {'/', '+', '-', '*', '<', '>', 'q', 'n', 'a', 'l',
            '&', '|', '=', '(', ')', '{', '}', '[', ']', '\'', '\"',';',
            ',', '\n', '\r', '\t'}; // ����ע����صķ���
    public static char[] spec_ends = {'t', 'g', 'u'}; // ��ʶ����������С���ս��

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

        map.put("#",-2);
        map.put("error", -1); // error

        return map;
    }

    public static HashMap<Integer,String> int2StrMap(){
        HashMap<Integer,String> map = new HashMap<>();
        for (Map.Entry<String,Integer> entry:str2IntMap().entrySet()){
            map.put(entry.getValue(),entry.getKey());
        }
        return map;
    }

    public static HashMap<String, Character> str2CharMap() {
        HashMap<String, Character> map = new HashMap<>();
        int i;
        for (i = 0; i < key_ends.length; i++)
            map.put(keys[i], key_ends[i]);
        for (i = 0; i < sign_ends.length; i++)
            map.put(signs[i], sign_ends[i]);
        for (i = 0; i < spec_ends.length; i++)
            map.put(specials[i],spec_ends[i]);
        map.put("#",'#');
        return map;
    }


    //����ʽ
    public static String[] production = {
            "P->SP", "P->",  // att: follow set of P should contains #
            "S->A;", "S->K;", "S->I", "S->W","S->N","S->;", //"S->L;"production versus "S->K"
            "A->iKC", "A->rKC", "A->cKC",
            "B->[R]", "B->", //�±������ϵ���ʽ
            "C->,KC","C->", //������ or ��ֵ
            "K->tBX",
            "X->=O","X->",
            "O->R","O->{Y}", // array assignment
            "Y->RZ","Y->",
            "Z->,Y","Z->",
            "I->f(L)HD",    //if
            "D->eH","D->", //else
            "W->w(L)H",     //while , do-----------------undone
            "H->S","H->{P}",
            "N->b;","N->o;", //break & continue
            "L->RJ",           //logic expression
            "J->|L", "J->&L", "J->",
            "R->MQ",           //relation expresion
            "Q->qR", "Q->nR", "Q->aR", "Q->lR", "Q->>R", "Q-><R", "Q->",
            "M->EV",
            "V->+M", "V->-M", "V->", //TODO att: �������ȼ�-�ҽ��
            "E->FT",
            "T->*E", "T->/E", "T->",
            "F->(R)", "F->tB", "F->G",
            "G->U","G->-U","G->+U", //ѡ���������߸���
            "U->g","U->u"  //������С��
    };

    //����ջ��Ϊ ���ս��������,�� ������ջ��ջ�ĽǶ�˼��
    public static String[] errorMsg = {
            "����Ƿ�ͷ", //���ᱨ��ȱ�� end�Ĵ���
            "�Ƿ�����俪ͷ", // Z
            "", // S ,�������Ŀ�ͷ�Ѿ��ǺϷ�����,�����д�
            "", // A��Sͬ��
            "û���ԷֺŽ�β / û���� )��β", //B  û����������Ϊ�����±꣬ ȱ�� ],
            "",
            "",
            "",
            "���ʽ�Ƿ� / ȱ�ٱ��ʽ", // L
            "", //TODO J ,  ��Ϊ ) ���� Follow(J), �пղ���ʽ�ķ��ս�����԰ѱ��������Ƹ���һ������
            "", //R ��L�е��� R�ı�������
            "ȱ�� ��", // �� L���� R,Q���ʡ�Q->��ʱ��һ��ƫ����ѡ�� )�������Ƹ���һ������
            "ȱ���������ʽ", // M,M �п���Ϊ �߼����ʽ�ģ�Ҳ�����Ǹ�ֵ����
            "ȱ�ٱ��ʽ /ȱ�� ) / ȱ�� ;", // V��ͬʱ�������ִ���
            "ȱ�ٱ��ʽ / ȱ�� ) / ȱ�� ;", // E
            "ȱ�ٱ��ʽ /ȱ�� ) / ȱ�� ;", // T
            "", // F,�� F��ʱ��ǰ�涼������
    };

    //TODO ջ���ս����ƥ��ʱ�����Ա���Ϊ ȱ��ĳ�ս����
    // ջ�����ս����ƥ��ʱ(Select����ƥ��)��1.Follow��Ҳ��ƥ��
    //                                  2.Follow��ƥ��


}
