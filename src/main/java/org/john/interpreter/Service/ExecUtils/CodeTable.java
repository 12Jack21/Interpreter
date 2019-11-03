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
    public static String[] specials = {"identifier", "digit", "rdigit"};

    // 对应的终结符号
    public static char[] key_ends = {'f', 'e', 'w', 'd', 'i', 'r', 'c', 'p', 's', 'b', 'o'};
    public static char[] sign_ends = {'/', '+', '-', '*', '<', '>', 'q', 'n', 'a', 'l',
            '&', '|', '=', '(', ')', '{', '}', '[', ']', '\'', '\"',';',
            ',', '\n', '\r', '\t'}; // 忽略注释相关的符号
    public static char[] spec_ends = {'t', 'g', 'u'}; // 标识符，整数，小数终结符

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


    //产生式
    public static String[] production = {
            "P->SP", "P->",  // att: follow set of P should contains #
            "S->A;", "S->K;", "S->I", "S->W","S->N","S->;", //"S->L;"production versus "S->K"
            "A->iKC", "A->rKC", "A->cKC",
            "B->[R]", "B->", //下标允许关系表达式
            "C->,KC","C->", //多声明 or 赋值
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
            "V->+M", "V->-M", "V->", //TODO att: 负号优先级-右结合
            "E->FT",
            "T->*E", "T->/E", "T->",
            "F->(R)", "F->tB", "F->G",
            "G->U","G->-U","G->+U", //选择正数或者负数
            "U->g","U->u"  //整数、小数
    };

    //考虑栈顶为 非终结符的问题,从 符号入栈出栈的角度思考
    public static String[] errorMsg = {
            "程序非法头", //最后会报出缺少 end的错误
            "非法的语句开头", // Z
            "", // S ,这里语句的开头已经是合法的了,不会有错
            "", // A与S同理
            "没有以分号结尾 / 没有以 )结尾", //B  没有以数字作为数组下标， 缺少 ],
            "",
            "",
            "",
            "表达式非法 / 缺少表达式", // L
            "", //TODO J ,  因为 ) 属于 Follow(J), 有空产生式的非终结符可以把报错任务推给下一个符号
            "", //R （L承担了 R的报错任务）
            "缺少 ）", // 有 L才有 R,Q，故”Q->“时下一个偏向于选择 )，或者推给下一个符号
            "缺少算数表达式", // M,M 有可能为 逻辑表达式的，也可能是赋值语句的
            "缺少表达式 /缺少 ) / 缺少 ;", // V，同时报出两种错误
            "缺少表达式 / 缺少 ) / 缺少 ;", // E
            "缺少表达式 /缺少 ) / 缺少 ;", // T
            "", // F,到 F的时候前面都报错了
    };

    //TODO 栈顶终结符不匹配时，可以报错为 缺少某终结符；
    // 栈顶非终结符不匹配时(Select集不匹配)：1.Follow集也不匹配
    //                                  2.Follow集匹配


}
