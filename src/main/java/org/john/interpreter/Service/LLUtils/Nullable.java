package org.john.interpreter.Service.LLUtils;

import org.john.interpreter.Service.ExecUtils.CodeTable;

import java.util.ArrayList;
import java.util.List;

import static org.john.interpreter.Service.LLUtils.Node.splitP;


public class Nullable {
    private String[] production;

    private List<String> nullableList; // 可空的 non-terminal

    public Nullable(String[] production) {
        this.production = production;
    }

    public Nullable() {
    }

    public List<String> countNullable() {
        nullableList = new ArrayList<>();
        int num = 0;
        boolean isStart = true; // 刚开始计算时的标志
        while (nullableList.size() > num || isStart) {
            isStart = false;
            num = nullableList.size();
            if (production == null || production.length == 0) {
                System.err.println("Error!!! 未设置产生式");
                break;
            }
            for (String p : production) {
                Node pNode = splitP(p);
                if (pNode.rightP.size() == 0) {
                    if (!nullableList.contains(pNode.leftP))
                        nullableList.add(pNode.leftP);
                } else {
                    List<String> rightP = pNode.rightP;
                    boolean isIn = true;
                    // 从前向后搜索，看是否 non-terminal都为 nullable集中的元素
                    for (int i = 0; i < rightP.size(); i++) {
                        String c = rightP.get(i);
                        if (!nullableList.contains(c)) {
                            isIn = false;
                            break;
                        }
                    }
                    // 若产生式右边都属于nullable，产生式左边也必定属于
                    if (isIn) {
                        if (!nullableList.contains(pNode.leftP))
                            nullableList.add(pNode.leftP);
                    }
                }
            }
        }
        return nullableList;
    }

    public List<String> getNullableList() {
        return nullableList;
    }

    public static void main(String[] args) {
        Nullable nullAble = new Nullable(CodeTable.productions);
        List<String> nullableList = nullAble.countNullable();
        System.out.println(nullableList);
    }
}
