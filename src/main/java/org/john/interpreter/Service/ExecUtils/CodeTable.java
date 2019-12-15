package org.john.interpreter.Service.ExecUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CodeTable {

    //关键词表
    public static String[] keys = {"if", "else", "while", "do", "int",
            "real", "char", "for", "print", "scan", "break", "continue", "return"};

    // 把 '.' 删去
    public static String[] signs = {"/", "+", "-", "*", "%", "<", ">", "==", "<>", ">=",
            "<=", "&&", "||", "*/", "~", "|", "&", "=", "(", ")", "{", "}", "[", "]", "'", "\"", ";",
            ",", "\\n", "\\r", "\\t", "\n", "\r", "\t", "//", "/*"};
    // 特例 (科学计数法的数直接当成 fraction)
    public static String[] specials = {"identifier", "integer", "fraction", "character", "string", "hexadecimal"};

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
        map.put("#", -2);

        return map;
    }

    public static HashMap<Integer, String> int2StrMap() {
        HashMap<Integer, String> map = new HashMap<>();
        for (Map.Entry<String, Integer> entry : str2IntMap().entrySet()) {
            map.put(entry.getValue(), entry.getKey());
        }
        return map;
    }

    // 当作特殊情况处理的矛盾产生式
    public static String[] special_production = {
            "Statement->Logic ;", "Statement->Assignment ;",
            "ELSEIF->else if ( Logic ) H ELSEIF", "ELSEIF->",
            "Variable->SymbolVar", "Variable->Digit",
            "Con->= identifier Index Con", "Con->"
    };
    public static String[] value_contain_token = {
            "identifier", "integer", "fraction"
    };

    //"S->Logic;" production versus "S->Declare"
    //产生式
    public static String[] productions = {
            "Pro->Statement Pro", "Pro->{ Pro } Pro", "Pro->",  // att: follow set of Pro should contains #
            "Statement->Declare", "Statement->Assignment ;", "Statement->IF", "Statement->WHILE",
            "Statement->Interrupt", "Statement->Logic ;", "Statement->FOR", "Statement->;",

            "Assignment->identifier Index Con X", // Single Assignment
            "Con->= identifier Index Con", "Con->",// like  a = b = 3;
            "Declare->Type Assign",           // Declaration
            "Type->int", "Type->real", "Type->char", "Type->void", // Type Specifier
            "Index->[ Logic ] Index", "Index->",
            "C->, Assignment C", "C->",        // Multiple declare or assign

            "Assign->identifier F",
            "F->( Parameter ) { Pro }", "F->Index Con X C ;",         // define function
            "Parameter->Type identifier Index CC", "Parameter->", //function parameters no.24
            "CC->, Parameter", "CC->",     // can replace CC with original non-terminal
            "ConAssign->Assignment C",     // continuing assignment

            "X->= O", "X->", //no.28
            "O->Logic", "O->{ Y }",     // array assignment
            "Y->Logic C'", "Y->",//TODO 是否删掉空数组初始化
            "C'->, Y", "C'->",             // for array assignment {}

            "IF->if ( Logic ) H ELSEIF ELSE",    //if
            "ELSEIF->else if ( Logic ) H ELSEIF", "ELSEIF->",          //else if
            "ELSE->else H", "ELSE->",      //else
            "WHILE->while ( Logic ) H",    //while , do-----------undone---
            "H->Statement", "H->{ Pro }",
            "FOR->for ( DA LO ; AS ) H",   //for loop,can be null in any position
            "DA->Declare", "DA->ConAssign ;", "DA->;", //for the reason that Declare takes ;
            "LO->Logic", "LO->",
            "AS->ConAssign", "AS->",
            "Interrupt->break ;", "Interrupt->continue ;", "Interrupt->return Result ;", //break & continue
            "Result->Logic", "Result->",

            "Logic->Relation L",        //logic expression
            "L->|| Logic", "L->&& Logic", "L->",

            "Relation->Arithmetic R",     //relation expresion
            "R->== Relation", "R-><> Relation", "R->>= Relation", "R-><= Relation", "R->> Relation", "R->< Relation", "R->",

            "Arithmetic->Variable Operation",
            "Operation->+ Arithmetic", "Operation->- Arithmetic", "Operation->* Arithmetic", "Operation->/ Arithmetic",
            "Operation->| Arithmetic", "Operation->^ Arithmetic", "Operation->& Arithmetic", "Operation->% Arithmetic",
            "Operation->",

            "Variable->( Relation )", "Variable->identifier Call", "Variable->Digit", "Variable->SymbolVar",
            "Variable->print ( Logic )", "Variable->scan ( Logic )", "Variable->character", "Variable->string",
            "SymbolVar->+ identifier", "SymbolVar->- identifier", "SymbolVar->~ identifier",
            "Call->( Argument )", "Call->Index", //TODO add c1 = c2 = c3 = 0 Error in LL table

            "Argument->Logic CCC", "Argument->",
            "CCC->, Argument", "CCC->", //no.73

            "Digit->Positive", "Digit->- Positive", "Digit->+ Positive", "Digit->~ Positive", //选择正数或者负数
            "Positive->integer", "Positive->fraction", "Positive->hexadecimal"  //整数、小数、十六进制数
    };

    /*
     * Thoughts: 允许某个产生式，但是该产生式是错误的，这样可以报出最精确的错误
     *       ie. 函数中参数列表允许赋值的产生式被加入，但是本身不被允许
     * */
    /*
     栈顶终结符不匹配时，可以报错为 缺少某终结符；
     栈顶非终结符不匹配时(Select集不匹配)：
     1.Follow集也不匹配
     2.Follow集匹配
    */

//                "Arithmetic->Item V",
//            "V->+ Arithmetic", "V->- Arithmetic", "V->", //右结合
//            "Item->Variable Factor",
//            "Factor->* Item", "Factor->/ Item", "Factor->",
//                "A->= Relation","A->",

    // 各个运算符的优先级
    public static HashMap<String, Integer> opPriority() {
        HashMap<String, Integer> map = new HashMap<>();
        map.put("||", 1);
        map.put("&&", 2);
        map.put("|", 3);
        map.put("^", 4);
        map.put("&", 5);
        map.put("==", 6);
        map.put("<>", 6);
        map.put("<", 7);
        map.put("<=", 7);
        map.put(">", 7);
        map.put(">=", 7);
        map.put("+", 8);
        map.put("-", 8);
        map.put("*", 9);
        map.put("/", 9);
        map.put("%", 9);
        map.put("(", 10);
        return map;
    }

    public static void main(String[]args){
        HashMap<Integer,String> map = int2StrMap();
        for (int i = 1;i < map.size();i++) {
            System.out.print(map.get(i) + " : " + i + '\t');
            if (i % 10 == 0)
                System.out.println();
        }
    }
}
