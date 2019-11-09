package org.john.interpreter.Service.ExecUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CodeTable {

    //关键词表
    public static String[] keys = {"if", "else", "while", "do", "int",
            "real", "char", "print", "scan", "break", "continue"};

    // 把 '.' 删去
    public static String[] signs = {"/", "+", "-", "*", "<", ">", "==", "<>", ">=", "<=",
            "&&", "||", "=", "(", ")", "{", "}", "[", "]", "'", "\"", ";",
            ",", "\\n", "\\r", "\\t", "\n", "\r", "\t", "//", "/*", "*/"};
    // 特例
    public static String[] specials = {"identifier", "digit", "r_digit"};

    public static char[] invs = {'\n', '\r', '\t'};// TODO 转义字符，题目要求是哪种， '\n' or '\\n'

    public static List<String> keyList = Arrays.asList(keys);

    public static List<String> signList = Arrays.asList(signs);


    //从字符串 token 到 种别码 code 的映射
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

    //产生式
    public static String[] production = {
            "P->S P", "P->{ P } P","P->",  // att: follow set of P should contains #
            "S->A ;", "S->K ;", "S->II", "S->W","S->N","S->;", //"S->L;"production versus "S->K"
            "S'->{ S' }","S'->S","S'->",
            "A->int K C", "A->real K C", "A->char K C",
            "B->[ R ]", "B->",  //下标允许关系表达式
            "C->, K C","C->",   //多声明 or 赋值
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
            "V->+ M", "V->- M", "V->", //att: 负号优先级-右结合
            "E->F T",
            "T->* E", "T->/ E", "T->",
            "F->( R )", "F->identifier B", "F->G",
            "G->U","G->- U","G->+ U", //选择正数或者负数
            "U->digit","U->r_digit"  //整数、小数
    };

    // 栈顶终结符不匹配时，可以报错为 缺少某终结符；
    // 栈顶非终结符不匹配时(Select集不匹配)：
    // 1.Follow集也不匹配
    // 2.Follow集匹配


}
