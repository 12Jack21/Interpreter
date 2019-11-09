package org.john.interpreter.Service.LLUtils;

import org.john.interpreter.Service.ExecUtils.CodeTable;

import java.util.*;

public class First {
    private String[] production;
    private List<String> nullableList;

    private Map<String, String> firstMap;

    public First(String[] production, List<String> nullableList) {
        this.production = production;
        this.nullableList = nullableList;
    }

    public First(List<String> nullableList) {
        this.nullableList = nullableList;
    }

    public Map<String, String> countFirst() {
        firstMap = new HashMap<>();
        boolean isChange = true;
        while (isChange) {
            isChange = false;
            for (String p : production) {
                Node pNode = Node.splitP(p);
                if (pNode.rightP.size() != 0) { //对于右边不为空的产生式
                    String  c = pNode.rightP.get(0);
                    if (c.charAt(0) < 'A' || c.charAt(0) > 'Z') { //不是大写字母，也就是不是非终结符
                        if (firstMap.get(pNode.leftP) == null) {
                            firstMap.put(pNode.leftP, String.valueOf(c)); //第一个终结符
                            isChange = true; // firstMap产生了变化
                        } else {
                            if (add(c, pNode.leftP))
                                isChange = true;
                        }
                    } else {
                        List<String> rightP = pNode.rightP;
                        for (int i = 0; i < rightP.size(); i++) {
                            String c1 = rightP.get(i);
                            if (i == 0 || nullableList.contains(rightP.get(i - 1))) {
                                String first = firstMap.get(pNode.leftP);
                                if (first != null && firstMap.get(c1) != null) {
                                    if (add(firstMap.get(c1), pNode.leftP))
                                        isChange = true;
                                } else if (firstMap.get(c1) != null) {
                                    firstMap.put(pNode.leftP, firstMap.get(c1));
                                    isChange = true;
                                }
                            } else
                                break;
                        }
                    }
                }
            }
        }

        return firstMap;
    }

    // 通用的 key(非终结符) 的First集增长函数
    private boolean add(String newPart, String key) {
        boolean isAdd = false;
        String first = firstMap.get(key); //key非终结符号的 当前First集
        String[] arr1 = first.split(" "); // 空格划分

        String[] arr2 = newPart.split(" ");
        Set<String> firstSet = new HashSet<>( Arrays.asList(arr1)); //HashSet去除重复值
        firstSet.addAll(Arrays.asList(arr2));
        StringBuilder sb = new StringBuilder();
        for (String element : firstSet) {
            sb.append(element).append(' ');
        }
        sb.delete(sb.length() - 1, sb.length()); //删除最后的 空格
        if (sb.length() != first.length()) {
            isAdd = true;                     // 添加了新的终结符
            firstMap.put(key, sb.toString()); //更新map
        }
        return isAdd;
    }

    public Map<String, String> getFirstMap() {
        return firstMap;
    }

    public static void main(String[] args) {
        Nullable nullable = new Nullable(CodeTable.productions);
        First first = new First(CodeTable.productions,nullable.countNullable());
        first.countFirst();
        System.out.println(first);
    }
}
