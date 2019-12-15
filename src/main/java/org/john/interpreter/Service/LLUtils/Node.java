package org.john.interpreter.Service.LLUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Node {
    public String leftP;//产生式左部非终结符
    public List<String> rightP;//产生式右部符号列表

    Node(String leftP, List<String> rightP) {
        this.leftP = leftP;
        this.rightP = rightP;
    }
    public static Node splitP(String p) {
        String[] array = p.split("->");
        if (array.length == 2)
            //自动填充了逗号
            return new Node(array[0], Arrays.asList(array[1].split(" ")));
        else
            return new Node(array[0], new ArrayList<>()); //空产生式
    }
}