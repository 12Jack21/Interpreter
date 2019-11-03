package org.john.interpreter.Service.LLUtils;

import java.util.*;

public class First {
    private String[] production;
    private List<Character> nullableList;

    private Map<Character, String> firstMap;

    public First(String[] production, List<Character> nullableList) {
        this.production = production;
        this.nullableList = nullableList;
    }

    public First(List<Character> nullableList) {
        this.nullableList = nullableList;
    }

    public Map<Character, String> countFirst() {
        firstMap = new HashMap<>();
        boolean isChange = true;
        while (isChange) {
            isChange = false;
            for (String p : production) {
                Node pNode = Node.splitP(p);
                if (!"".equals(pNode.rightP)) { //对于右边不为空的产生式
                    char c = pNode.rightP.charAt(0);
                    if (c < 'A' || c > 'Z') { //不是大写字母，也就是不是非终结符
                        if (firstMap.get(pNode.leftP) == null) {
                            firstMap.put(pNode.leftP, String.valueOf(c)); //第一个终结符
                            isChange = true; // firstMap产生了变化
                        } else {
                            if (add(String.valueOf(c), pNode.leftP))
                                isChange = true;
                        }
                    } else {
                        String rightP = pNode.rightP;
                        for (int i = 0; i < rightP.length(); i++) {
                            char c1 = rightP.charAt(i);
                            if (i == 0 || nullableList.contains(rightP.charAt(i - 1))) {
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
    private boolean add(String newPart, char key) {
        boolean isAdd = false;
        String first = firstMap.get(key); //key非终结符号的 当前First集
        String[] arr1 = first.split(",");
        List<String> arr = Arrays.asList(arr1);
        if (arr.size() == 0) {
            arr = new ArrayList<>(); // 由于逗号 被split掉了
            arr.add(",");
        }
        String[] arr2 = newPart.split(",");
        Set<String> firstSet = new HashSet<>(arr); //HashSet去除重复值
        firstSet.addAll(Arrays.asList(arr2));
        StringBuilder sb = new StringBuilder();
        for (String element : firstSet) {
            sb.append(element).append(',');
        }
        sb.delete(sb.length() - 1, sb.length()); //删除最后的 ,
        if (sb.length() != first.length()) {
            isAdd = true; // 添加了新的终结符
            firstMap.put(key, sb.toString()); //更新map
        }
        return isAdd;
    }

    public Map<Character, String> getFirstMap() {
        return firstMap;
    }

    public static void main(String[] args) {
    }
}
