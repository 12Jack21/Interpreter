package org.john.interpreter.Service.LLUtils;

public class Node {
    public char leftP;
    public String rightP = "";

    Node(char leftP, String rightP) {
        this.leftP = leftP;
        this.rightP = rightP;
    }
    public static Node splitP(String p) {
        String[] array = p.split("->");
        if (array.length == 2)
            return new Node(array[0].charAt(0), array[1]); //自动填充了逗号
        else
            return new Node(array[0].charAt(0), ""); //空产生式
    }
}