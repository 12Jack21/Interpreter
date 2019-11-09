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
    public static String[] specials = {"identifier", "integer", "fraction"};

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
            "Pro->Statement Pro", "Pro->{ Pro } Pro","Pro->",  // att: follow set of Pro should contains #
            "Statement->Declare ;", "Statement->Assignment ;", "Statement->IF", "Statement->WHILE","Statement->Interrupt","Statement->;", //"S->L;"production versus "S->Declare"
            "Declare->int Assignment Continuation", "Declare->real Assignment Continuation", "Declare->char Assignment Continuation",
            "Index->[ Relation ]", "Index->",  //下标允许关系表达式
            "Continuation->, Assignment Continuation","Continuation->",   //多声明 or 赋值
            "Assignment->identifier Index X",
            "X->= O","X->",
            "O->Relation","O->{ Y }", // array assignment
            "Y->Relation Continuation'","Y->",
            "Continuation'->, Y","Continuation'->",   // for array assignment {}
            "IF->if ( Logic ) H ELSE",    //if
            "ELSE->else H","ELSE->",     //else
            "WHILE->while ( Logic ) H",    //while , do-----------------undone
            "H->Statement","H->{ P }",
            "Interrupt->break ;","Interrupt->continue ;", //break & continue
            "Logic->Relation J",           //logic expression
            "J->|| Logic", "J->&& Logic", "J->",
            "Relation->Arithmetic Q",           //relation expresion
            "Q->== Relation", "Q-><> Relation", "Q->>= Relation", "Q-><= Relation", "Q->> Relation", "Q->< Relation", "Q->",
            "Arithmetic->Item V",
            "V->+ Arithmetic", "V->- Arithmetic", "V->", //att: 负号优先级-右结合
            "Item->F Factor",
            "Factor->* Item", "Factor->/ Item", "Factor->",
            "F->( Relation )", "F->identifier Index", "F->Digit",
            "Digit->Positive","Digit->- Positive","Digit->+ Positive", //选择正数或者负数
            "Positive->integer","Positive->fraction"  //整数、小数
    };

    // 栈顶终结符不匹配时，可以报错为 缺少某终结符；
    // 栈顶非终结符不匹配时(Select集不匹配)：
    // 1.Follow集也不匹配
    // 2.Follow集匹配


}
