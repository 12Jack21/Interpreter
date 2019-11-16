package org.john.interpreter.Service.SemanticUtils; /**
 * Created by Gatev on 2016/1/2.
 */

import org.john.interpreter.Service.SemanticUtils.*;

import java.util.ArrayList;
import java.util.Scanner;

public class Run {
    public String[] message;
    int readNum;
    int readCount;
    public  boolean error;
    private int level = 1;
    private SimpleTable simple;
    private ArrayTable array;
    private String errorMessage = "";
    private String result = "";
    private int lineNum;//zanshi buzhi dao
    public Run(){
        readNum = 0;
        readCount =0;
        simple = new SimpleTable();
        array = new ArrayTable();
        error =false;
    }
    public void setMessage(){

         message = UI.ui.input.getText().split("\n");

        //System.out.print(message.length);

    }

    public String getResult(){
        return result;
    }
    public void addError(String message){
        errorMessage += message;
    }
    public String getErrorMessage(){
        return errorMessage;
    }

    public void run(SimpleNode tree){

        int i = 0;
        while(i < tree.jjtGetNumChildren()){
            SimpleNode child = (SimpleNode)tree.jjtGetChild(i);
            int id = child.getId();
            String nodeName = ParseTreeConstants.jjtNodeName[id];
            if(nodeName.equals("start")){
                //start -> prgram
                run(child);
            }else if(nodeName.equals("program")){
                //program -> statements
                run(child);
            }else if(nodeName.equals("statements")) {
                //statements -> null || statement + statements
                run(child);
            }else if(nodeName.equals("statement")){
                //statement -> intDec, charDec, realDec, bool Dec
                run(child);
            }else if(nodeName.equals("ifstatement")){
                /*
                ifstatement->(condition, statements, statements)
                 */
                //If statement, 需要level++, 然后计算condition结点的值来判断
                //当condition对应表达式是string或者real时报错，整形就判断是否非0来看是不是true
                int num = child.jjtGetNumChildren();
                if(num == 2){//ifstatement -> (condition, statements)
                    ASTcondition con = (ASTcondition)child.jjtGetChild(0);
                    arithCalc(con);
                    String type = con.getType();
                    String nodevalue = con.getNodeValue();
                    if(type.equals("string") || type.equals("real")){
                        error = true;
                        addError("Error: if condition can't be char(string) type or real type\n");
                        return;
                    }else{
                        if(type.equals("int")){
                            int val = Integer.parseInt(nodevalue);
                            if(val == 0){
                                ;//do nothing, condition is false
                            }else{
                                level++;
                                run((SimpleNode)child.jjtGetChild(1));//condition is true
                                simple.deleteVariable(level);
                                array.deleteArrays(level);
                                level--;
                            }
                        }else if(type.equals("bool")){
                            boolean val = Boolean.parseBoolean(nodevalue);
                            if(val){
                                level++;
                                run((SimpleNode)child.jjtGetChild(1));
                                simple.deleteVariable(level);
                                array.deleteArrays(level);
                                level--;
                            }else{
                                ;//no else things to do
                            }
                        }else{
                            error =true;
                            addError("Error: unknown(err) condition, condition should be int or bool type\n");
                        }
                    }

                }else if(num == 3){//ifstatement ->(condition, statements, statements)
                    ASTcondition con = (ASTcondition)child.jjtGetChild(0);
                    arithCalc(con);
                    String type = con.getType();
                    String nodevalue = con.getNodeValue();
                    if(type.equals("string") || type.equals("real")){
                        error =true;
                        addError("Error: if condition can't be char(string) type or real type\n");
                        return;
                    }else{
                        if(type.equals("int")){
                            int val = Integer.parseInt(nodevalue);
                            if(val == 0){
                                level++;
                                run((SimpleNode)child.jjtGetChild(2));
                                simple.deleteVariable(level);
                                array.deleteArrays(level);
                                level--;
                            }else{
                                level++;
                                run((SimpleNode)child.jjtGetChild(1));//condition is true
                                simple.deleteVariable(level);
                                array.deleteArrays(level);
                                level--;
                            }
                        }else if(type.equals("bool")){
                            boolean val = Boolean.parseBoolean(nodevalue);
                            if(val){
                                level++;
                                run((SimpleNode)child.jjtGetChild(1));
                                simple.deleteVariable(level);
                                array.deleteArrays(level);
                                level--;
                            }else{
                                level++;
                                run((SimpleNode)child.jjtGetChild(2));
                                simple.deleteVariable(level);
                                array.deleteArrays(level);
                                level--;
                            }
                        }else{
                            error =true;
                            addError("Error: unknown(err) condition, condition should be int or bool type\n");
                        }
                    }
                }else {
                    error =true;
                    addError("Error: Ifstatement has more than three parts!\n");

                }
                return;

            }else if(nodeName.equals("condition")){
                /*
                  condition->xxNode
                 */
                ;//这个不会出现吧
                return;
            }else if(nodeName.equals("expression")){
                /*
                expression-> term || xxNode
                */
                return;
            }else if(nodeName.equals("term")){
                return;
            }else if(nodeName.equals("factor")){
                return;
            }else if(nodeName.equals("id")){
                return;
            }else if(nodeName.equals("whilestatement")){

                //region whilestatement
                int num = child.jjtGetNumChildren();
                ASTcondition con = (ASTcondition)child.jjtGetChild(0);
                arithCalc(con);
                //num = con.jjtGetNumChildren();
                String type = con.getType();
                num = con.jjtGetChild(0).jjtGetNumChildren();
                String nodevalue = con.getNodeValue();
                //System.out.print(type);
                //System.out.println("While statent, the condition type is " + type + " value is " + nodevalue + " num is " + num);
                if(type.equals("string") || type.equals("real") || type.equals("err")){
                    error =true;
                    addError("Error: condition should be int or bool type\n");
                    return;
                }else{
                    if(num == 1) {//可能是死循环while(condition); ?
                        if(type.equals("int")){
                            int val = Integer.parseInt(nodevalue);
                            //System.out.print(val);
                            if(val == 0){
                                return;
                            }else{
                                error =true;
                                addError("Error: while dead loop\n");
                                return;
                            }
                        }
                        else if(type.equals("bool")){
                            boolean val = Boolean.parseBoolean(nodevalue);
                            if(!val){
                                return;
                            }else{
                                error =true;
                                addError("Error: while dead loop\n");
                                return;
                            }
                        }
                    }else if(num == 2){//while(condition){statements}
                        if(type.equals("int")){
                            int val = Integer.parseInt(nodevalue);
                            //int out = 10000
                            while(val != 0){
                                level++;

                                run((SimpleNode)(child.jjtGetChild(1)));
                                simple.deleteVariable(level);
                                array.deleteArrays(level);
                                level--;
                                arithCalc(con);
                                val = Integer.parseInt(con.getNodeValue());
                            }
                        }
                        else if(type.equals("bool")){
                            boolean val = Boolean.parseBoolean(nodevalue);
                            //int out =0;
                            while(val){
                                level++;
                                run((SimpleNode)(child.jjtGetChild(1)));
                                simple.deleteVariable(level);
                                array.deleteArrays(level);
                                level--;
                                arithCalc(con);
                                val = Boolean.parseBoolean(con.getNodeValue());

                            }
                        }
                    }
                }
                return;
                //endregion

            }else if(nodeName.equals("void")){
                //这个结点好像没用吧...
                return;
            }else if(nodeName.equals("intDec")){
                //region int type declaration
                int sz = child.jjtGetNumChildren();
                if(sz == 1){//intDec->ids
                    SimpleNode dec = (SimpleNode)child.jjtGetChild(0);
                    for(int j = 0; j < dec.jjtGetNumChildren(); j++){
                        SimpleNode nex = (SimpleNode)dec.jjtGetChild(j);
                        if(nex.jjtGetNumChildren() == 0) {//整形变量
                            SimpleVariable var = new SimpleVariable();
                            var.setType("int");
                            var.setLevel(level);
                            var.setValue("0");
                            var.setName(((ASTid)nex).getName());
                            if(!simple.addVariable(var)){//声明失败，曾经被声明过
                                error =true;
                                addError("Error: variable " + var.getName() + " has been declared before\n");
                            }
                        }else{//整形数组
                            String name = ((ASTid)nex).getName();
                            arithCalc((SimpleNode)nex.jjtGetChild(0));//计算子树对应的值
                            String type = ((SimpleNode)nex.jjtGetChild(0)).getType();
                            String nodevalue = ((SimpleNode)nex.jjtGetChild(0)).getNodeValue();
                            if(type.equals("int")){
                                int length = Integer.parseInt(nodevalue);
                                if(length <= 0){
                                    error =true;
                                    addError("Error: The subscript of array " + name + " should be positive\n");
                                }
                                else {
                                    //System.out.println("Array " + name + " is declared, length is " + length);
                                    ArrayList<String> values = new ArrayList<String>();
                                    for (int k = 0; k < length; k++)
                                        values.add("0");
                                    ArrayVariable arr = new ArrayVariable();
                                    arr.setLevel(level);
                                    arr.setType("int");
                                    arr.setArrayName(name);
                                    arr.setLength(length);
                                    arr.setValues(values);
                                    if (!array.addVariable(arr)) {
                                        error =true;
                                        addError("Error: array variable " + arr.getArrayName() + " has been declared before\n");
                                    }
                                }
                            }else {
                                error =true;
                                addError("Error: The subscript of array "+ name +" must be a positive integer\n");}
                        }
                    }
                }else {
                    error =true;
                    addError("Error: int Declaration error\n");}
                return;
                //#endregion

            }else if(nodeName.equals("realDec")){
                //region real type declaration
                int sz = child.jjtGetNumChildren();
                if(sz == 1){//realDec->ids
                    SimpleNode dec = (SimpleNode)child.jjtGetChild(0);
                    for(int j = 0; j < dec.jjtGetNumChildren(); j++){
                        SimpleNode nex = (SimpleNode)dec.jjtGetChild(j);
                        if(nex.jjtGetNumChildren() == 0) {//real变量
                            SimpleVariable var = new SimpleVariable();
                            var.setType("real");
                            var.setLevel(level);
                            var.setValue("0");
                            var.setName(((ASTid)nex).getName());
                            if(!simple.addVariable(var)){//声明失败，曾经被声明过
                                error =true;
                                addError("Error: variable " + var.getName() + " has been declared before\n");
                            }
                        }else{//real形数组
                            String name = ((ASTid)nex).getName();
                            arithCalc((SimpleNode)nex.jjtGetChild(0));//计算子树对应的值
                            String type = ((SimpleNode)nex.jjtGetChild(0)).getType();
                            String nodevalue = ((SimpleNode)nex.jjtGetChild(0)).getNodeValue();
                            if(type.equals("int")){//数组下标必须是整数
                                int length = Integer.parseInt(nodevalue);
                                if(length <= 0){
                                    error =true;
                                    addError("Error: The subscript of array " + name + " shouldn't be non-positive\n");
                                }
                                else {
                                    //System.out.println("Array " + name + " is declared, length is " + length);
                                    ArrayList<String> values = new ArrayList<String>();
                                    for (int k = 0; k < length; k++)
                                        values.add("0");
                                    ArrayVariable arr = new ArrayVariable();
                                    arr.setLevel(level);
                                    arr.setType("real");
                                    arr.setArrayName(name);
                                    arr.setLength(length);
                                    arr.setValues(values);
                                    if (!array.addVariable(arr)) {
                                        error =true;
                                        addError("Error: array variable " + arr.getArrayName() + " has been declared before\n");
                                    }
                                }
                            }else {
                                error =true;
                                addError("Error: The subscript of array " + name + " must be a positive integer\n");}
                        }
                    }
                }else {
                    error =true;
                    addError("Error: real Declaration error\n");}
                return;
                //#endregion
            }else if(nodeName.equals("boolDec")){
                //region bool type declaration
                int sz = child.jjtGetNumChildren();
                if(sz == 1){//boolDec->ids
                    SimpleNode dec = (SimpleNode)child.jjtGetChild(0);
                    for(int j = 0; j < dec.jjtGetNumChildren(); j++){
                        SimpleNode nex = (SimpleNode)dec.jjtGetChild(j);
                        if(nex.jjtGetNumChildren() == 0) {//bool变量
                            SimpleVariable var = new SimpleVariable();
                            var.setType("bool");
                            var.setLevel(level);
                            var.setValue("0");
                            var.setName(((ASTid)nex).getName());
                            if(!simple.addVariable(var)){//声明失败，曾经被声明过
                                error =true;
                                addError("Error: variable " + var.getName() + " has been declared before\n");
                            }
                        }else{//real形数组
                            String name = ((ASTid)nex).getName();
                            arithCalc((SimpleNode)nex.jjtGetChild(0));//计算子树对应的值
                            String type = ((SimpleNode)nex.jjtGetChild(0)).getType();
                            String nodevalue = ((SimpleNode)nex.jjtGetChild(0)).getNodeValue();
                            if(type.equals("int")){//数组下标必须是整数
                                int length = Integer.parseInt(nodevalue);
                                if(length <= 0){
                                    error =true;
                                    addError("Error: The subscript of array " + name + " shouldn't be non-positive\n");
                                }
                                else {
                                    //System.out.println("Array " + name + " is declared, length is " + length);
                                    ArrayList<String> values = new ArrayList<String>();
                                    for (int k = 0; k < length; k++)
                                        values.add("false");
                                    ArrayVariable arr = new ArrayVariable();
                                    arr.setLevel(level);
                                    arr.setType("bool");
                                    arr.setArrayName(name);
                                    arr.setLength(length);
                                    arr.setValues(values);
                                    if (!array.addVariable(arr)) {
                                        error =true;
                                        addError("Error: array variable " + arr.getArrayName() + " has been declared before\n");
                                    }
                                }
                            }else {
                                error =true;
                                addError("Error: The subscript of array " + name + " must be a positive integer\n");}
                        }
                    }
                }else {
                    error =true;
                    addError("Error: bool Declaration error\n");}
                return;
                //#endregion
            }else if(nodeName.equals("charDec")){
                //region char type declaration
                int sz = child.jjtGetNumChildren();
                if(sz == 1){//charDec->ids
                    SimpleNode dec = (SimpleNode)child.jjtGetChild(0);
                    for(int j = 0; j < dec.jjtGetNumChildren(); j++){
                        SimpleNode nex = (SimpleNode)dec.jjtGetChild(j);
                        if(nex.jjtGetNumChildren() == 0) {//char变量(string)
                            SimpleVariable var = new SimpleVariable();
                            var.setType("string");
                            var.setLevel(level);
                            var.setValue("");
                            var.setName(((ASTid)nex).getName());
                            if(!simple.addVariable(var)){//声明失败，曾经被声明过
                                error =true;
                                addError("Error: variable " + var.getName() + " has been declared before\n");
                            }
                        }else{//real形数组
                            String name = ((ASTid)nex).getName();
                            arithCalc((SimpleNode)nex.jjtGetChild(0));//计算子树对应的值
                            String type = ((SimpleNode)nex.jjtGetChild(0)).getType();
                            String nodevalue = ((SimpleNode)nex.jjtGetChild(0)).getNodeValue();
                            if(type.equals("int")){//数组下标必须是整数
                                int length = Integer.parseInt(nodevalue);
                                if(length <= 0){
                                    error =true;
                                    addError("Error: The subscript of array " + name + " shouldn't be non-positive\n");
                                }
                                else {
                                    //System.out.println("Array " + name + " is declared, length is " + length);
                                    ArrayList<String> values = new ArrayList<String>();
                                    for (int k = 0; k < length; k++)
                                        values.add("");
                                    ArrayVariable arr = new ArrayVariable();
                                    arr.setLevel(level);
                                    arr.setType("string");
                                    arr.setArrayName(name);
                                    arr.setLength(length);
                                    arr.setValues(values);
                                    if (!array.addVariable(arr)) {
                                        error =true;
                                        addError("Error: array variable " + arr.getArrayName() + " has been declared before\n");
                                    }
                                }
                            }else {
                                error =true;
                                addError("Error: The subscript of array " + name + " must be a positive integer\n");}
                        }
                    }
                }else {
                    error =true;
                    addError("Error: char(string) Declaration error\n");}
                return;
                //#endregion
            }else if(nodeName.equals("ids")){
                return;
            }else if(nodeName.equals("readstatement")){
                //readCount++;
                //region read statement
                //String message = "";
                int num = child.jjtGetNumChildren();
                ASTid son = (ASTid)child.jjtGetChild(0);
                if(son.jjtGetNumChildren() == 0){//单独的变量
                    String varName = son.getName();//变量名
                    SimpleVariable var = simple.getVar(varName);
                    if(var == null){
                        error =true;
                        addError("Error: variable " + varName + " is not declared before use\n");
                    }else{
                        String type = var.getType();
                        //这一部分是从命令行读入的，后期要改为从前端读入
                        String input = null;
                        //message = UI.ui.input.getText();
                        //System.out.print(message);
                        //Scanner in = new Scanner(message);
                        //System.out.print(message);
                        //input = in.next();//从命令行获取的输入数据
                        if(readNum>=message.length||readNum==0&&message[0].isEmpty())
                        {
                            readCount++;
                            error =true;
                            addError("Error: input data not enough\n");
                            return;
                        }
                        //System.out.println(message[0]);

                        input = message[readNum];
                        //System.out.println(input);
                        readNum++;
                        //System.out.println("Now try to read variable " + varName + " of type " + type);
                        //System.out.println("The input is :" + input);
                        if(type.equals("int")){
                            try{
                                int tmp = Integer.parseInt(input);
                                var.setValue(Integer.toString(tmp));
                            }catch(Exception e){
                                error =true;
                                addError("Error: the input is not a integer!\n");
                            }
                        }else if(type.equals("real")){
                            try{
                                double tmp = Double.parseDouble(input);
                                var.setValue(Double.toString(tmp));
                            }catch(Exception e){
                                error =true;
                                addError("Error: the input is not a real number!\n");
                            }
                        }else if(type.equals("bool")){//注意输入bool变量需要输入true(部分大小写)否则都是false
                            try{
                                boolean tmp = Boolean.parseBoolean(input);
                                var.setValue(tmp ? "true":"false");
                            }catch(Exception e){
                                error =true;
                                addError("Error: the input is not a boolean\n");
                            }
                        }else if(type.equals("string")){
                            try{
                                var.setValue(input);
                            }catch(Exception e){
                                error =true;
                                addError("Error: error when read string\n");
                            }
                        }else{
                            error =true;
                            addError("Error: unknown variable type\n");
                        }
                    }

                }else if(son.jjtGetNumChildren() == 1){//是数组中的某个元素
                    String arrName = son.getName();
                    if(array.getArray(arrName) == null){
                        error =true;
                        addError("Error: The array " + arrName + " should be declared before use\n");
                    }else {
                        arithCalc((SimpleNode) son.jjtGetChild(0));
                        SimpleNode exp = (SimpleNode) son.jjtGetChild(0);
                        String type = exp.getType();
                        String value = exp.getNodeValue();
                        ArrayVariable arr = array.getArray(arrName);
                        if(type.equals("int")){
                            int pos = Integer.parseInt(value);
                            int length = arr.getLength();
                            if(pos >= length && !error){
                                error =true;
                                addError("Error: The subscribe of array " + arrName + " should be smaller than array size\n");
                                //return;
                            }else if(pos < 0){
                                error =true;
                                addError("Error: The subscribe of array can't be negative\n");
                            }else{
                                ArrayList<String> values = arr.getValues();
                                String arrType = arr.getType();
                                //从命令行读入的数组
                                if(readNum>=message.length||readNum==0&&message[0].isEmpty()){
                                    readCount++;
                                    error =true;
                                    addError("Error: input data not enough\n");
                                    return;
                                }
                                String input = null;
                                input = message[readNum];
                                readNum++;
                                //input = in.next();//从命令行获取的输入数据
                                //System.out.println("Now try to read variable of array " + arrName + " at pos " + pos + " type is "+type);
                                //System.out.println("The input is :" + input);
                                if(arrType.equals("int")){
                                    try{
                                        int tmp = Integer.parseInt(input);
                                        values.set(pos, Integer.toString(tmp));
                                    }catch(Exception e){
                                        error =true;
                                        addError("Error: the input is not a integer!\n");
                                    }
                                }else if(arrType.equals("real")){
                                    try{
                                        double tmp = Double.parseDouble(input);
                                        values.set(pos, Double.toString(tmp));
                                    }catch(Exception e){
                                        error =true;
                                        addError("Error: the input is not a real number!\n");
                                    }
                                }else if(arrType.equals("bool")){
                                    try{
                                        boolean tmp = Boolean.parseBoolean(input);
                                        values.set(pos, tmp ? "true" : "false");
                                    }catch(Exception e){
                                        error =true;
                                        addError("Error: the input is not a boolean\n");
                                    }
                                }else if(arrType.equals("string")){
                                    try{
                                        values.set(pos, input);
                                    }catch(Exception e){
                                        error =true;
                                        addError("Error: error when read string\n");
                                    }
                                }else{
                                    error =true;
                                    addError("Error: Unknown variable type read\n");
                                }
                            }
                        }else{
                            error =true;
                            addError("Error: The subscribe of array must be integer\n");
                        }
                    }




                }else {
                    error =true;
                    addError("Error: unknown variable information, not variable or array element\n");
                }
                //endregion
            }else if(nodeName.equals("writestatement")){
                //region read statement
                //writestatement -> expression
                int num = child.jjtGetNumChildren();
                arithCalc((SimpleNode)child.jjtGetChild(0));
                SimpleNode son = (SimpleNode)child.jjtGetChild(0);
                String type = son.getType();
                String nodevalue = son.getNodeValue();
                if(type.equals("err")){
                    error =true;
                    addError("Error: Unable to output the error type value in the write operation\n");
                }else{
                    //System.out.println("Write operation: " + nodevalue.toString());
                    nodevalue =nodevalue.replaceAll("#n#", "\n");
                    //System.out.println("Write operation: " + nodevalue.toString());
                    result += nodevalue+" ";//输出结果存在result中
                }
                //endregion
            }else if(nodeName.equals("ASSIGNNode")) {
                int num = child.jjtGetNumChildren();
                if(num == 2){//child[0]是ids列表的id, child[1]是expression
                    //这里我只在ids只含有一个元素的时候执行,否则报错,这个地方可能要改一下ASSIGNNode的语法
                    //也就是说 id, id, id = 123, 我是报错的
                    //只有id = value, 以及shuzu[pos] = value是合法赋值格式
                    //ASSIGNNode -> ids(id, may be array), expression
                    ASTids ids = (ASTids)child.jjtGetChild(0);
                    if(ids.jjtGetNumChildren() != 1){
                        error =true;
                        addError("Error: Only one left value is allowed for assignment\n");
                    }else{
                        arithCalc((SimpleNode)child.jjtGetChild(1));//计算右值

                        ASTid left = (ASTid)ids.jjtGetChild(0);
                        SimpleNode right = (SimpleNode)child.jjtGetChild(1);
                        if(left.jjtGetNumChildren() == 0){//单变量
                            String varName = left.getName();
                            SimpleVariable var = simple.getVar(varName);
                            if(var == null){
                                error =true;
                                addError("Error: variable " + varName + " should be declared before use\n");
                            }else{
                                String varType = var.getType();
                                String rightExpType = right.getType();
                                String rightExpValue = right.getNodeValue();
                                if(rightExpType.equals("err")){
                                    error =true;
                                    addError("Error: Assignment right value is error\n");
                                }else if(rightExpType.equals("string")){//string 只能赋值给string
                                    if(varType.equals("string")){
                                        var.setValue(rightExpValue);
                                    }else{
                                        error =true;
                                        addError("Error: String is not not allow to assigned to other type variable\n");
                                    }
                                }else if(rightExpType.equals("int")){//int 可以赋值给int, real, bool
                                    if(varType.equals("int")){
                                        var.setValue(rightExpValue);
                                    }else if(varType.equals("real")) {
                                        var.setValue(rightExpValue);
                                    }else if(varType.equals("bool")){
                                        if(Integer.parseInt(rightExpValue) != 0){
                                            var.setValue("true");
                                        }else var.setValue("false");
                                    }else{
                                        error =true;
                                        addError("Error: int is not allowed to assign to string or error type\n");
                                    }
                                }else if(rightExpType.equals("real")){//real 只能赋值给real
                                    if(varType.equals("real")){
                                        var.setValue(rightExpValue);
                                    }else{
                                        error =true;
                                        addError("Error: real type is not allowed to be assigned to other type\n");
                                    }

                                }else if(rightExpType.equals("bool")){//bool 可以赋值给bool, int real, 数值视为1或者0
                                    if(varType.equals("bool")){
                                        var.setValue(rightExpValue);
                                    }else if(varType.equals("int")){
                                        var.setValue(rightExpValue.equals("true") ? "1" : "0");
                                    }else if(varType.equals("real")){
                                        var.setValue(rightExpValue.equals("true") ? "1" : "0");
                                    }else{
                                        error =true;
                                        addError("Error: bool type is only allowed to be assigned to int, real or bool\n");
                                    }
                                }
                            }
                        }else{//数组访问
                            String arrName = left.getName();
                            if(array.getArray(arrName) == null){
                                error =true;
                                addError("Error: The array " + arrName + " should be declared before use\n");
                            } else{
                                ArrayVariable arr = array.getArray(arrName);
                                int maxLength = arr.getLength();
                                arithCalc((SimpleNode)left.jjtGetChild(0));
                                String type = ((SimpleNode) left.jjtGetChild(0)).getType();
                                String nodevalue = ((SimpleNode) left.jjtGetChild(0)).getNodeValue();
                                if(type.equals("int")){//数组下标必须是整形
                                    int pos = Integer.parseInt(nodevalue);
                                    if(pos >= maxLength) {//数组越界
                                        error =true;
                                        addError("Error: the subscribe of array " + arrName + " is larger then array length, please check\n");
                                    }else if(pos < 0) {
                                        error =true;
                                        addError("Error: the subscribe of array " + arrName + " can't be negative\n");
                                    }else{//可以成功访问数组第pos个元素
                                        ArrayList<String> list = arr.getValues();
                                        String arrType = arr.getType();
                                        //System.out.println("调试信息：access to array " + arr.getArrayName() + " the " + pos + "th element\n");
                                        String rightExpType = right.getType();
                                        String rightExpValue = right.getNodeValue();

                                        if(rightExpType.equals("err")){
                                            error =true;
                                            addError("Error: Assignment right value is error\n");
                                        }else if(rightExpType.equals("string")){//string 只能赋值给string
                                            if(arrType.equals("string")){
                                                list.set(pos, rightExpValue);
                                            }else{
                                                error =true;
                                                addError("Error: String is not not allow to assigned to other type variable\n");
                                            }
                                        }else if(rightExpType.equals("int")){//int 可以赋值给int, real, bool
                                            if(arrType.equals("int")){
                                                list.set(pos, rightExpValue);
                                            }else if(arrType.equals("real")) {
                                                list.set(pos, rightExpValue);
                                            }else if(arrType.equals("bool")){
                                                if(Integer.parseInt(rightExpValue) != 0){
                                                    list.set(pos, "true");
                                                }else list.set(pos, "false");
                                            }else{
                                                error =true;
                                                addError("Error: int is not allowed to assign to string or error type\n");
                                            }
                                        }else if(rightExpType.equals("real")){//real 只能赋值给real
                                            if(arrType.equals("real")){
                                                list.set(pos, rightExpValue);
                                            }else{
                                                error =true;
                                                addError("Error: real type is not allowed to be assigned to other type\n");
                                            }

                                        }else if(rightExpType.equals("bool")){//bool 可以赋值给bool, int real, 数值视为1或者0
                                            if(arrType.equals("bool")){
                                                list.set(pos, rightExpValue);
                                            }else if(arrType.equals("int")){
                                                list.set(pos, rightExpValue.equals("true") ? "1" : "0");
                                            }else if(arrType.equals("real")){
                                                list.set(pos, rightExpValue.equals("true") ? "1" : "0");
                                            }else{
                                                error =true;
                                                addError("Error: bool type is only allowed to be assigned to int, real or bool\n");
                                            }
                                        }

                                    }
                                }else{
                                    error =true;
                                    addError("Error: the subscribe of array "+ arrName + " must be integer\n");
                                }
                            }

                        }
                    }
                }else {error =true;
                    addError("Error: Assignment should have only one left value and one right value\n");}
                return;
            }
            i++;

        }
    }

    //计算以now为根节点的表达式的值
    private void arithCalc(SimpleNode now){
        int sz = now.jjtGetNumChildren();
        String name = now.getNodeName();
        if(sz == 0)//常量或者单变量
        {
            if(name.equals("integer")) {
                now.setType("int");
                now.setNodeValue(((ASTinteger) now).getName());
            }else if(name.equals("string")){
                now.setType("string");
                String tmpValue = ((ASTstring)now).getName();
                if(!tmpValue.isEmpty()){
                    if(tmpValue.charAt(0) == '\"')
                        tmpValue = tmpValue.substring(1);
                }
                if(!tmpValue.isEmpty()){
                    if(tmpValue.charAt(tmpValue.length() - 1) == '\"'){
                        tmpValue = tmpValue.substring(0, tmpValue.length() - 1);
                    }
                }
                now.setNodeValue(tmpValue);
            }else if(name.equals("real")){
                now.setType("real");
                now.setNodeValue(((ASTreal)now).getName());
            }else if(name.equals("bool")){
                now.setType("bool");
                now.setNodeValue(((ASTbool)now).getName());
            }else if(name.equals("id")){
                String idName = ((ASTid)now).getName();//变量名
                if(simple.getVar(idName) != null){//找到了之前最后声明过的该变量名
                    SimpleVariable var = simple.getVar(idName);
                    now.setType(var.getType());
                    now.setNodeValue(var.getValue());
                }else{
                    error =true;
                    addError("Error: variable " + idName + " should be declared before use\n");
                    now.setType("err");
                    now.setNodeValue("0");
                }
            }
            return;
        }
        if(name.equals("id")){//说明是数组中某个元素
            String arrName = ((ASTid)now).getName();
            ArrayVariable arr = array.getArray(arrName);
            if(arr != null){//找到了这个数组
                int length = arr.getLength();
                arithCalc((SimpleNode)now.jjtGetChild(0));
                String type = ((SimpleNode) now.jjtGetChild(0)).getType();
                String nodevalue = ((SimpleNode) now.jjtGetChild(0)).getNodeValue();
                if(type.equals("int")){//数组下标必须是整形
                    int pos = Integer.parseInt(nodevalue);
                    if(pos >= length) {//数组越界
                        error =true;
                        addError("Error: the subscribe of array " + arrName + " is larger then array length, please check\n");
                        now.setType("err");
                        now.setNodeValue("0");
                    }else if(pos < 0){
                        error =true;
                        addError("Error: the subscribe of array " + arrName + " can't be negative\n");
                        now.setType("err");
                        now.setNodeValue("0");
                    }else {//成功访问数组第pos个元素
                        String value = arr.getValues().get(pos);
                        String arrType = arr.getType();
                        now.setType(arrType);
                        now.setNodeValue(value);
                        //System.out.println("access to array " + arr.getArrayName() + " the " + pos + "th element\n");
                    }
                }else{
                    error =true;
                    addError("Error: the subscribe of array "+ arrName + " must be integer\n");
                    now.setType("err");
                    now.setNodeValue("0");
                }
            }else{
                error =true;
                addError("Error: array " + arrName + " should be declared before use\n");
                now.setType("err");
                now.setNodeValue("0");
            }
            return;
        }
        for(int i = 0; i < sz; i++) {
            SimpleNode child = (SimpleNode) now.jjtGetChild(i);
            arithCalc(child);
            if(child.getType().equals("err")){
                now.setType("err");
                now.setNodeValue("err");
                return;
            }
        }

        if(name.equals("EQNode")){//==
            //region 判断相等
            SimpleNode left = (SimpleNode)now.jjtGetChild(0);
            SimpleNode right = (SimpleNode)now.jjtGetChild(1);
            if(left.getType().equals(right.getType())){
                if(left.getType().equals("string")){
                    now.setType("bool");
                    now.setNodeValue((left.getNodeValue().equals(right.getNodeValue()))? "true" : "false");
                }
                else if(left.getType().equals("int")){
                    now.setType("bool");
                    int lef = Integer.parseInt(left.getNodeValue());
                    int rig = Integer.parseInt(right.getNodeValue());
                    now.setNodeValue(lef == rig ? "true" : "false");
                }else if(left.getType().equals("real")){
                    now.setType("bool");
                    double lef = Double.parseDouble(left.getNodeValue());
                    double rig = Double.parseDouble(right.getNodeValue());
                    now.setNodeValue(Math.abs(lef - rig) < 1e-12 ? "true" : "false");
                }else if(left.getType().equals("bool")){
                    now.setType("bool");
                    boolean lef = Boolean.parseBoolean(left.getNodeValue());
                    boolean rig = Boolean.parseBoolean(right.getNodeValue());
                    now.setNodeValue(lef == rig ? "true" : "false");
                }
            }else{
                if(left.getType().equals("string") || right.getType().equals("string")) {
                    error =true;
                    addError("Error: string can't be compared with others\n");
                    now.setType("err");
                    now.setNodeValue("0");
                }else {
                    double lef = 0, rig = 0;
                    if(left.getType().equals("boolean")){
                        lef = left.getNodeValue().equals("false")?0:1;
                    }else lef = Double.parseDouble(left.getNodeValue());
                    if(right.getType().equals("boolean")){
                        rig = right.getNodeValue().equals("false")?0:1;
                    }else rig = Double.parseDouble(right.getNodeValue());
                    now.setNodeValue("bool");
                    now.setNodeValue(Math.abs(lef - rig) < 1e-12 ? "true" : "false");
                }
            }
            //endregion
        }else if(name.equals("NENode")){//!= <>
            //region 判断不等
            SimpleNode left = (SimpleNode)now.jjtGetChild(0);
            SimpleNode right = (SimpleNode)now.jjtGetChild(1);
            if(left.getType().equals(right.getType())){
                if(left.getType().equals("string")){
                    now.setType("bool");
                    now.setNodeValue((left.getNodeValue().equals(right.getNodeValue()))? "false" : "true");
                }
                else if(left.getType().equals("int")){
                    now.setType("bool");
                    int lef = Integer.parseInt(left.getNodeValue());
                    int rig = Integer.parseInt(right.getNodeValue());
                    now.setNodeValue(lef == rig ? "false" : "true");
                }else if(left.getType().equals("real")){
                    now.setType("bool");
                    double lef = Double.parseDouble(left.getNodeValue());
                    double rig = Double.parseDouble(right.getNodeValue());
                    now.setNodeValue(Math.abs(lef - rig) < 1e-12 ? "false" : "true");
                }else if(left.getType().equals("bool")){
                    now.setType("bool");
                    boolean lef = Boolean.parseBoolean(left.getNodeValue());
                    boolean rig = Boolean.parseBoolean(right.getNodeValue());
                    now.setNodeValue(lef == rig ? "false" : "true");
                }
            }else{
                if(left.getType().equals("string") || right.getType().equals("string")) {
                    error =true;
                    addError("Error: string can't be compared with others\n");
                    now.setType("err");
                    now.setNodeValue("0");
                }else {
                    double lef = 0, rig = 0;
                    if(left.getType().equals("boolean")){
                        lef = left.getNodeValue().equals("false")?0:1;
                    }else lef = Double.parseDouble(left.getNodeValue());
                    if(right.getType().equals("boolean")){
                        rig = right.getNodeValue().equals("false")?0:1;
                    }else rig = Double.parseDouble(right.getNodeValue());
                    now.setNodeValue("bool");
                    now.setNodeValue(Math.abs(lef - rig) < 1e-12 ? "false" : "true");
                }
            }
            //endregion
        }else if(name.equals("GTNode")){// >
            //region 判断大于 其中bool 的 true视作1, false视作0
            SimpleNode left = (SimpleNode)now.jjtGetChild(0);
            SimpleNode right = (SimpleNode)now.jjtGetChild(1);
            if(left.getType().equals(right.getType())){
                if(left.getType().equals("string")){
                    now.setType("bool");
                    now.setNodeValue(left.getNodeValue().compareTo(right.getNodeValue()) > 0 ? "true" : "false");
                }
                else if(left.getType().equals("int")){
                    now.setType("bool");
                    //System.out.println(left.getNodeValue() + " " + right.getNodeValue());
                    int lef = Integer.parseInt(left.getNodeValue());
                    int rig = Integer.parseInt(right.getNodeValue());
                    now.setNodeValue(lef > rig ? "true" : "false");
                }else if(left.getType().equals("real")){
                    now.setType("bool");
                    double lef = Double.parseDouble(left.getNodeValue());
                    double rig = Double.parseDouble(right.getNodeValue());
                    now.setNodeValue(lef > rig ? "true" : "false");
                }else if(left.getType().equals("bool")){
                    now.setType("bool");
                    boolean lef = Boolean.parseBoolean(left.getNodeValue());
                    boolean rig = Boolean.parseBoolean(right.getNodeValue());
                    now.setNodeValue((lef && !rig) ? "true" : "false");
                }
            }else{
                if(left.getType().equals("string") || right.getType().equals("string")) {
                    error =true;
                    addError("Error: string can't be compared with others\n");
                    now.setType("err");
                    now.setNodeValue("0");
                }else {
                    double lef = 0, rig = 0;
                    if(left.getType().equals("boolean")){
                        lef = left.getNodeValue().equals("false")?0:1;
                    }else lef = Double.parseDouble(left.getNodeValue());
                    if(right.getType().equals("boolean")){
                        rig = right.getNodeValue().equals("false")?0:1;
                    }else rig = Double.parseDouble(right.getNodeValue());
                    now.setNodeValue("bool");
                    now.setNodeValue(lef > rig ? "true" : "false");
                }
            }
            //endregion
        }else if(name.equals("LTNode")){// <
            //region 判断小于
            SimpleNode left = (SimpleNode)now.jjtGetChild(0);
            SimpleNode right = (SimpleNode)now.jjtGetChild(1);
            if(left.getType().equals(right.getType())){
                if(left.getType().equals("string")){
                    now.setType("bool");
                    now.setNodeValue((left.getNodeValue().compareTo(right.getNodeValue())) < 0 ? "true" : "false");
                }
                else if(left.getType().equals("int")){
                    now.setType("bool");
                    int lef = Integer.parseInt(left.getNodeValue());
                    int rig = Integer.parseInt(right.getNodeValue());
                    now.setNodeValue(lef < rig ? "true" : "false");
                }else if(left.getType().equals("real")){
                    now.setType("bool");
                    double lef = Double.parseDouble(left.getNodeValue());
                    double rig = Double.parseDouble(right.getNodeValue());
                    now.setNodeValue(lef < rig ? "true" : "false");
                }else if(left.getType().equals("bool")){
                    now.setType("bool");
                    boolean lef = Boolean.parseBoolean(left.getNodeValue());
                    boolean rig = Boolean.parseBoolean(right.getNodeValue());
                    now.setNodeValue((!lef && rig) ? "true" : "false");
                }
            }else{
                if(left.getType().equals("string") || right.getType().equals("string")) {
                    error =true;
                    addError("Error: string can't be compared with others\n");
                    now.setType("err");
                    now.setNodeValue("0");
                }else {
                    double lef = 0, rig = 0;
                    if(left.getType().equals("boolean")){
                        lef = left.getNodeValue().equals("false")?0:1;
                    }else lef = Double.parseDouble(left.getNodeValue());
                    if(right.getType().equals("boolean")){
                        rig = right.getNodeValue().equals("false")?0:1;
                    }else rig = Double.parseDouble(right.getNodeValue());
                    now.setNodeValue("bool");
                    now.setNodeValue(lef < rig ? "true" : "false");
                }
            }
            //endregion
        }else if(name.equals("LENode")){// <=
            //region 判断<=
            SimpleNode left = (SimpleNode)now.jjtGetChild(0);
            SimpleNode right = (SimpleNode)now.jjtGetChild(1);
            if(left.getType().equals(right.getType())){
                if(left.getType().equals("string")){
                    now.setType("bool");
                    now.setNodeValue((left.getNodeValue().compareTo(right.getNodeValue())) <= 0 ? "true" : "false");
                }
                else if(left.getType().equals("int")){
                    now.setType("bool");
                    int lef = Integer.parseInt(left.getNodeValue());
                    int rig = Integer.parseInt(right.getNodeValue());
                    now.setNodeValue(lef <= rig ? "true" : "false");
                }else if(left.getType().equals("real")){
                    now.setType("bool");
                    double lef = Double.parseDouble(left.getNodeValue());
                    double rig = Double.parseDouble(right.getNodeValue());
                    now.setNodeValue(lef - rig < 1e-12 ? "true" : "false");
                }else if(left.getType().equals("bool")){
                    now.setType("bool");
                    boolean lef = Boolean.parseBoolean(left.getNodeValue());
                    boolean rig = Boolean.parseBoolean(right.getNodeValue());
                    now.setNodeValue((rig || (!lef)) ? "true" : "false");
                }
            }else{
                if(left.getType().equals("string") || right.getType().equals("string")) {
                    error =true;
                    addError("Error: string can't be compared with others\n");
                    now.setType("err");
                    now.setNodeValue("0");
                }else {
                    double lef = 0, rig = 0;
                    if(left.getType().equals("boolean")){
                        lef = left.getNodeValue().equals("false")?0:1;
                    }else lef = Double.parseDouble(left.getNodeValue());
                    if(right.getType().equals("boolean")){
                        rig = right.getNodeValue().equals("false")?0:1;
                    }else rig = Double.parseDouble(right.getNodeValue());
                    now.setNodeValue("bool");
                    now.setNodeValue(lef - rig < 1e-12 ? "true" : "false");
                }
            }
            //endregion
        }else if(name.equals("GENode")){// >=
            //region 判断>=
            SimpleNode left = (SimpleNode)now.jjtGetChild(0);
            SimpleNode right = (SimpleNode)now.jjtGetChild(1);
            if(left.getType().equals(right.getType())){
                if(left.getType().equals("string")){
                    now.setType("bool");
                    now.setNodeValue((left.getNodeValue().compareTo(right.getNodeValue())) >= 0 ? "true" : "false");
                }
                else if(left.getType().equals("int")){
                    now.setType("bool");
                    int lef = Integer.parseInt(left.getNodeValue());
                    int rig = Integer.parseInt(right.getNodeValue());
                    now.setNodeValue(lef >= rig ? "true" : "false");
                }else if(left.getType().equals("real")){
                    now.setType("bool");
                    double lef = Double.parseDouble(left.getNodeValue());
                    double rig = Double.parseDouble(right.getNodeValue());
                    now.setNodeValue(lef - rig > -1e-12 ? "true" : "false");
                }else if(left.getType().equals("bool")){
                    now.setType("bool");
                    boolean lef = Boolean.parseBoolean(left.getNodeValue());
                    boolean rig = Boolean.parseBoolean(right.getNodeValue());
                    now.setNodeValue((!rig || (lef)) ? "true" : "false");
                }
            }else{
                if(left.getType().equals("string") || right.getType().equals("string")) {
                    error =true;
                    addError("Error: string can't be compared with others\n");
                    now.setType("err");
                    now.setNodeValue("0");
                }else {
                    double lef, rig;
                    if(left.getType().equals("boolean")){
                        lef = left.getNodeValue().equals("false")?0:1;
                    }else lef = Double.parseDouble(left.getNodeValue());
                    if(right.getType().equals("boolean")){
                        rig = right.getNodeValue().equals("false")?0:1;
                    }else rig = Double.parseDouble(right.getNodeValue());
                    now.setNodeValue("bool");
                    now.setNodeValue(lef - rig > -1e-12 ? "true" : "false");
                }
            }
           //endregion
        }else if(name.equals("ANDNode")){//&&
            //region && 运算, 不能和real及string相操作
            SimpleNode left = (SimpleNode)now.jjtGetChild(0);
            SimpleNode right = (SimpleNode)now.jjtGetChild(1);
            if(left.getType().equals("string") || right.getType().equals("string")
                    || left.getType().equals("real") || right.getType().equals("real")) {
                error =true;
                addError("Error: unable to do && operation for string or real\n");
                now.setType("err");
                now.setNodeValue("0");
            }else {
                boolean lef = false, rig = false;
                //System.out.print(left.getNodeValue());
                if(left.getType().equals("int")){
                    lef = Integer.parseInt(left.getNodeValue()) != 0;

                }else if(left.getNodeValue()=="true") lef = true;
                else lef = false;
                if(right.getType().equals("int")){
                    rig = Integer.parseInt(right.getNodeValue()) != 0;
                }else if(right.getNodeValue() =="true") rig = true;
                else rig = false;
                now.setNodeValue("bool");
                now.setNodeValue(lef && rig ? "true":"false");
            }
            //endregion
        }else if(name.equals("ORNode")){// ||
            //region || 运算, 不能和real及string相操作
            SimpleNode left = (SimpleNode)now.jjtGetChild(0);
            SimpleNode right = (SimpleNode)now.jjtGetChild(1);
            if(left.getType().equals("string") || right.getType().equals("string")
                    || left.getType().equals("real") || right.getType().equals("real")) {
                error =true;
                addError("Error: unable to do && operation for string or real\n");
                now.setType("err");
                now.setNodeValue("0");
            }else {
                boolean lef = false, rig = false;
                if(left.getType().equals("int")){
                    lef = Integer.parseInt(left.getNodeValue()) != 0;
                }else lef = Boolean.parseBoolean(left.getNodeValue());
                if(right.getType().equals("int")){
                    rig = Integer.parseInt(right.getNodeValue()) != 0;
                }else rig = Boolean.parseBoolean(right.getNodeValue());
                now.setNodeValue("bool");
                now.setNodeValue(lef || rig ? "true":"false");
            }
            //endregion
        }else if(name.equals("PLUSNode")){// +
            //region
            SimpleNode left = (SimpleNode)now.jjtGetChild(0);
            SimpleNode right = (SimpleNode)now.jjtGetChild(1);
            if(left.getType().equals(right.getType())){
                now.setType(left.getType());
                if(left.getType().equals("bool")){
                    now.setNodeValue((left.getNodeValue().equals("true") || right.getNodeValue().equals("true")) ? "true" : "false");
                }
                else if(left.getType().equals("string")){
                    now.setNodeValue(left.getNodeValue() + right.getNodeValue());
                }
                else if(left.getType().equals("int")){
                    int lef = Integer.parseInt(left.getNodeValue());
                    int rig = Integer.parseInt(right.getNodeValue());
                    now.setNodeValue(Integer.toString(lef + rig));
                }
                else if(left.getType().equals("real")){
                    double lef = Double.parseDouble(left.getNodeValue());
                    double rig = Double.parseDouble(right.getNodeValue());
                    now.setNodeValue(Double.toString(lef + rig));
                }
            }else
            {
                if(left.getType().equals("string") || right.getType().equals("string")){
                    error =true;
                    addError("Error: no + operation can be do between string and other type\n");
                    now.setType("err");
                    now.setNodeValue("0");
                }
                if(left.getType().equals("real") || right.getType().equals("real")){
                    double lef, rig;
                    if(left.getType().equals("bool")){
                        lef = left.getNodeValue().equals("false") ? 0 : 1;
                    }else lef = Double.parseDouble(left.getNodeValue());
                    if(right.getType().equals("bool")){
                        rig = right.getNodeValue().equals("false") ? 0 : 1;
                    }else rig = Double.parseDouble(right.getNodeValue());
                    now.setType("real");
                    now.setNodeValue(Double.toString(lef + rig));
                }
                else{
                int lef, rig;
                if(left.getType().equals("bool")){
                    lef = left.getNodeValue().equals("false") ? 0 : 1;
                }else lef = Integer.parseInt(left.getNodeValue());
                if(right.getType().equals("bool")){
                    rig = right.getNodeValue().equals("false") ? 0 : 1;
                }else rig = Integer.parseInt(right.getNodeValue());
                now.setType("int");
                now.setNodeValue(Integer.toString(lef + rig));
            }
            }
            //endregion

        }else if(name.equals("MINUSNode")){// -

            SimpleNode left = (SimpleNode)now.jjtGetChild(0);
            SimpleNode right = (SimpleNode)now.jjtGetChild(1);
            if(left.getType().equals(right.getType())){
                now.setType(left.getType());
                if(left.getType().equals("bool")){
                    int lef = left.getNodeValue().equals("false")?0:1;
                    int rig = right.getNodeValue().equals("false")?0:1;
                    now.setNodeValue(lef - rig == 0 ? "false":"true");
                }
                else if(left.getType().equals("string")){
                    error =true;
                    //now.setNodeValue(left.getNodeValue() + right.getNodeValue());
                    addError("Error: string can't do - operation\n");
                }
                else if(left.getType().equals("int")){
                    int lef = Integer.parseInt(left.getNodeValue());
                    int rig = Integer.parseInt(right.getNodeValue());
                    now.setNodeValue(Integer.toString(lef - rig));
                }
                else if(left.getType().equals("real")){
                    double lef = Double.parseDouble(left.getNodeValue());
                    double rig = Double.parseDouble(right.getNodeValue());
                    now.setNodeValue(Double.toString(lef - rig));
                }
            }else
            {
                if(left.getType().equals("string") || right.getType().equals("string")){
                    error =true;
                    addError("Error: no - operation can be do between string and other type\n");
                    now.setType("err");
                    now.setNodeValue("0");
                }
                if(left.getType().equals("real") || right.getType().equals("real")){
                    double lef, rig;
                    if(left.getType().equals("bool")){
                        lef = left.getNodeValue().equals("false") ? 0 : 1;
                    }else lef = Double.parseDouble(left.getNodeValue());
                    if(right.getType().equals("bool")){
                        rig = right.getNodeValue().equals("false") ? 0 : 1;
                    }else rig = Double.parseDouble(right.getNodeValue());
                    now.setType("real");
                    now.setNodeValue(Double.toString(lef - rig));
                }
                else{int lef, rig;
                if(left.getType().equals("bool")){
                    lef = left.getNodeValue().equals("false") ? 0 : 1;
                }else lef = Integer.parseInt(left.getNodeValue());
                if(right.getType().equals("bool")){
                    rig = right.getNodeValue().equals("false") ? 0 : 1;
                }else rig = Integer.parseInt(right.getNodeValue());
                now.setType("int");
                now.setNodeValue(Integer.toString(lef - rig));
            }
            }
        }else if(name.equals("MULTINode")){// *
            SimpleNode left = (SimpleNode)now.jjtGetChild(0);
            SimpleNode right = (SimpleNode)now.jjtGetChild(1);
            if(left.getType().equals(right.getType())){
                now.setType(left.getType());
                if(left.getType().equals("bool")){
                    int lef = left.getNodeValue().equals("false")?0:1;
                    int rig = right.getNodeValue().equals("false")?0:1;
                    now.setNodeValue(lef * rig == 0 ? "false":"true");
                }
                else if(left.getType().equals("string")){
                    error =true;
                    //now.setNodeValue(left.getNodeValue() + right.getNodeValue());
                    addError("Error: string can't do * operation\n");
                }
                else if(left.getType().equals("int")){
                    int lef = Integer.parseInt(left.getNodeValue());
                    int rig = Integer.parseInt(right.getNodeValue());
                    now.setNodeValue(Integer.toString(lef * rig));
                }
                else if(left.getType().equals("real")){
                    double lef = Double.parseDouble(left.getNodeValue());
                    double rig = Double.parseDouble(right.getNodeValue());
                    now.setNodeValue(Double.toString(lef * rig));
                }
            }else
            {
                if(left.getType().equals("string") || right.getType().equals("string")){
                    error =true;
                    addError("Error: no * operation can be do between string and other type\n");
                    now.setType("err");
                    now.setNodeValue("0");
                }
                if(left.getType().equals("real") || right.getType().equals("real")){
                    double lef, rig;
                    if(left.getType().equals("bool")){
                        lef = left.getNodeValue().equals("false") ? 0 : 1;
                    }else lef = Double.parseDouble(left.getNodeValue());
                    if(right.getType().equals("bool")){
                        rig = right.getNodeValue().equals("false") ? 0 : 1;
                    }else rig = Double.parseDouble(right.getNodeValue());
                    now.setType("real");
                    now.setNodeValue(Double.toString(lef * rig));
                }else{
                int lef, rig;
                if(left.getType().equals("bool")){
                    lef = left.getNodeValue().equals("false") ? 0 : 1;
                }else lef = Integer.parseInt(left.getNodeValue());
                if(right.getType().equals("bool")){
                    rig = right.getNodeValue().equals("false") ? 0 : 1;
                }else rig = Integer.parseInt(right.getNodeValue());
                now.setType("int");
                now.setNodeValue(Integer.toString(lef * rig));
            }
            }

        }else if(name.equals("DIVNode")){// /

            SimpleNode left = (SimpleNode)now.jjtGetChild(0);
            SimpleNode right = (SimpleNode)now.jjtGetChild(1);
            if(left.getType().equals(right.getType())){

                now.setType(left.getType());
                if(left.getType().equals("bool")){
                    int lef = left.getNodeValue().equals("false")?0:1;
                    int rig = right.getNodeValue().equals("false")?0:1;
                    if(rig == 0){
                        error =true;
                        addError("Error: divide by zero error\n");
                        now.setType("err");
                        now.setNodeValue("0");
                    }else
                        now.setNodeValue(lef == 0 ? "false":"true");
                }
                else if(left.getType().equals("string")){
                    error =true;
                    //now.setNodeValue(left.getNodeValue() + right.getNodeValue());
                    addError("Error: string can't do / operation\n");
                }
                else if(left.getType().equals("int")){
                    int lef = Integer.parseInt(left.getNodeValue());
                    int rig = Integer.parseInt(right.getNodeValue());
                    if(rig == 0){
                        error =true;
                        addError("Error: divide by zero error\n");
                        now.setType("err");
                        now.setNodeValue("0");
                    }else
                        now.setNodeValue(Integer.toString(lef / rig));
                }
                else if(left.getType().equals("real")){
                    double lef = Double.parseDouble(left.getNodeValue());
                    double rig = Double.parseDouble(right.getNodeValue());
                    if(Math.abs(rig) < 1e-12){
                        error =true;
                        addError("Error: divide by zero error\n");
                        now.setType("err");
                        now.setNodeValue("0");
                    }else
                        now.setNodeValue(Double.toString(lef / rig));
                }
            }else
            {
                if(left.getType().equals("string") || right.getType().equals("string")){
                    error =true;
                    addError("Error: no - operation can be do between string and other type\n");
                    now.setType("err");
                    now.setNodeValue("0");
                }
                if(left.getType().equals("real") || right.getType().equals("real")){
                    double lef, rig;
                    //System.out.println("real Operation");
                    if(left.getType().equals("bool")){
                        lef = left.getNodeValue().equals("false") ? 0 : 1;
                    }else lef = Double.parseDouble(left.getNodeValue());
                    if(right.getType().equals("bool")){
                        rig = right.getNodeValue().equals("false") ? 0 : 1;
                    }else rig = Double.parseDouble(right.getNodeValue());
                    now.setType("real");
                    if(Math.abs(rig) < 1e-12){
                        error =true;
                        addError("Error: divide by zero error\n");
                        now.setType("err");
                        now.setNodeValue("0");
                    }else
                        now.setNodeValue(Double.toString(lef / rig));
                }
                else{
                int lef, rig;
                if(left.getType().equals("bool")){
                    lef = left.getNodeValue().equals("false") ? 0 : 1;
                }else lef = Integer.parseInt(left.getNodeValue());
                if(right.getType().equals("bool")){
                    rig = right.getNodeValue().equals("false") ? 0 : 1;
                }else rig = Integer.parseInt(right.getNodeValue());
                now.setType("int");
                if(rig == 0){
                    error =true;
                    addError("Error: divide by zero error\n");
                    now.setType("err");
                    now.setNodeValue("0");
                }else
                    now.setNodeValue(Integer.toString(lef / rig));
            }

            }


        }else if(name.equals("MODNode")){// %
            SimpleNode left = (SimpleNode)now.jjtGetChild(0);
            SimpleNode right = (SimpleNode)now.jjtGetChild(1);
            if(left.getType().equals(right.getType())){
                now.setType(left.getType());
                if(left.getType().equals("bool")){
                    int lef = left.getNodeValue().equals("false")?0:1;
                    int rig = right.getNodeValue().equals("false")?0:1;
                    if(rig == 0){
                        error =true;
                        addError("Error: module by zero error\n");
                        now.setType("err");
                        now.setNodeValue("0");
                    }else
                        now.setNodeValue(lef == 0 ? "false":"true");
                }
                else if(left.getType().equals("string")){
                    error =true;
                    //now.setNodeValue(left.getNodeValue() + right.getNodeValue());
                    addError("Error: string can't do module operation\n");
                }
                else if(left.getType().equals("int")){
                    int lef = Integer.parseInt(left.getNodeValue());
                    int rig = Integer.parseInt(right.getNodeValue());
                    if(rig == 0){
                        error =true;
                        addError("Error: module by zero error\n");
                        now.setType("err");
                        now.setNodeValue("0");
                    }else
                        now.setNodeValue(Integer.toString(lef / rig));
                }
                else if(left.getType().equals("real")){
                    error =true;
                    addError("Error: real number can't do module operation\n");
                    now.setType("err");
                    now.setNodeValue("0");
                }
            }else
            {
                if(left.getType().equals("string") || right.getType().equals("string")){
                    error =true;
                    addError("Error: no - operation can be do between string and other type\n");
                    now.setType("err");
                    now.setNodeValue("0");
                }
                if(left.getType().equals("real") || right.getType().equals("real")){
                    error =true;
                    addError("Error: real number can't do module operation\n");
                    now.setType("err");
                    now.setNodeValue("0");
                }
                else{
                int lef, rig;
                if(left.getType().equals("bool")){
                    lef = left.getNodeValue().equals("false") ? 0 : 1;
                }else lef = Integer.parseInt(left.getNodeValue());
                if(right.getType().equals("bool")){
                    rig = right.getNodeValue().equals("false") ? 0 : 1;
                }else rig = Integer.parseInt(right.getNodeValue());
                now.setType("int");
                if(rig == 0){
                    error =true;
                    addError("Error: module by zero error\n");
                    now.setType("err");
                    now.setNodeValue("0");
                }else
                    now.setNodeValue(Integer.toString(lef / rig));
            }
            }

        }else if(sz == 1){//为了类似于expression -> term -> factor -> integer 的向上继承
            SimpleNode child = (SimpleNode)now.jjtGetChild(0);
            now.setType(child.getType());
            now.setNodeValue(child.getNodeValue());
        }
    }
}
/*
"start",
    "program",
    "statements",
    "statement",
    "ifstatement",
    "condition",
    "EQNode",
    "NENode",
    "GTNode",
    "LTNode",
    "LENode",
    "GENode",
    "ANDNode",
    "ORNode",
    "expression",
    "PLUSNode",
    "MINUSNode",
    "term",
    "MULTINode",
    "DIVNode",
    "MODNode",
    "factor",
    "id",
    "integer",
    "string",
    "real",
    "bool",
    "whilestatement",
    "void",
    "intDec",
    "ANNSIGNNode",
    "realDec",
    "boolDec",
    "charDec",
    "ids",
    "readstatement",
    "writestatement",
 */
