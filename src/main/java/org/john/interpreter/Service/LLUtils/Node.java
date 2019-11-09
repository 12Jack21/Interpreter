package org.john.interpreter.Service.LLUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Node {
    public String leftP;
    public List<String> rightP;

    Node(String leftP, List<String> rightP) {
        this.leftP = leftP;
        this.rightP = rightP;
    }
    public static Node splitP(String p) {
        String[] array = p.split("->");
        if (array.length == 2)
            return new Node(array[0], Arrays.asList(array[1].split(" "))); //自动填充了逗号
        else
            return new Node(array[0], new ArrayList<>()); //空产生式
    }
}