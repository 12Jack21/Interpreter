package org.john.interpreter.Service.LLUtils;

import java.util.ArrayList;
import java.util.List;

import static org.john.interpreter.Service.LLUtils.Node.splitP;


public class Nullable {
    private String[] production;

    private List<Character> nullableList;

    public Nullable(String[] production) {
        this.production = production;
    }

    public Nullable() {
    }

    public List<Character> countNullable() {
        nullableList = new ArrayList<>();
        int num = 0;
        boolean isStart = true;
        while (nullableList.size() > num || isStart) {
            isStart = false;
            num = nullableList.size();
            for (String p : production) {
                Node pNode = splitP(p);
                if (pNode.rightP.equals("")) {
                    if (!nullableList.contains(pNode.leftP))
                        nullableList.add(pNode.leftP);
                } else {
                    String rightP = pNode.rightP;
                    boolean isIn = true;
                    for (int i = 0; i < rightP.length(); i++) {
                        char c = rightP.charAt(i);
                        if (!nullableList.contains(c)) {
                            isIn = false;
                            break;
                        }
                    }
                    if (isIn) {
                        if (!nullableList.contains(pNode.leftP))
                            nullableList.add(pNode.leftP);
                    }
                }
            }
        }
        return nullableList;
    }

    public List<Character> getNullableList() {
        return nullableList;
    }

    public static void main(String[] args) {
        Nullable nullAble = new Nullable();
        List<Character> nullableList = nullAble.countNullable();
        System.out.println(nullableList);
    }
}
