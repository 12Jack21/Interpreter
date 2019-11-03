package org.john.interpreter.Service.LLUtils;

import org.john.interpreter.Service.ExecUtils.CodeTable;

import static org.john.interpreter.Service.ExecUtils.CodeTable.*;

import java.util.*;

public class Follow {
    private String[] production;
    private Map<Character, String> followMap;

    private Map<Character, String> firstMap;
    private List<Character> nullableList;

    public Follow(String[] production, List<Character> nullableList, Map<Character, String> firstMap) {
        this.production = production;
        this.firstMap = firstMap;
        this.nullableList = nullableList;
    }

    public Map<Character, String> countFollow() {
        followMap = new HashMap<>();
        followMap.put('P',"#"); // firstly manual add #
        boolean isChange = true;
        while (isChange) {
            isChange = false;
            for (String p : production) {
                Node pNode = Node.splitP(p);
                char leftP = pNode.leftP;
                String followTemp = followMap.get(leftP); //用于暂存终结符
                String rightP = pNode.rightP;
                //对于产生式右边 从右往左遍历
                for (int i = rightP.length() - 1; i >= 0; i--) {
                    char c = rightP.charAt(i);
                    if (c < 'A' || c > 'Z') {
                        followTemp = String.valueOf(c);
                    } else {
                        String follow = followMap.get(c);
                        if (follow != null) {
                            if (followTemp != null)
                                if (add(followTemp, c)) //加入Follow集
                                    isChange = true;
                        } else if (followTemp != null) { //Follow集为空
                            followMap.put(c, followTemp.replace("\0,", ""));
                            isChange = true;
                        }
                        if (nullableList.contains(c) && followTemp != null && firstMap.get(c) != null) {
                            followTemp = followTemp + "," + firstMap.get(c);
                        } else
                            followTemp = firstMap.get(c); //也可以为null
                    }

                    if (c == ',')
                        System.out.println("逗号来了");
                }
            }
        }
        return followMap;
    }

    // 通用的 将 followTemp的终结符加入 key（非终结符）的follow集中
    private boolean add(String followTemp, char key) {
        boolean isAdd = false;
        boolean hasComma = false;
        String follow = followMap.get(key);

        String[] arr1 = follow.split(",");
        if (arr1.length == 0 || follow.contains(",,"))
            hasComma = true;

        String[] arr2 = followTemp.split(",");
        if (arr2.length == 0 || followTemp.contains(",,"))
            hasComma = true; //consider ,

        Set<String> followSet = new HashSet<>(Arrays.asList(arr1)); //两个里面只要有一个有就加上
        followSet.addAll(Arrays.asList(arr2));
        if (hasComma)
            followSet.add(",");

        followSet.remove(""); //去掉空格
        StringBuilder sb = new StringBuilder();
        for (String element : followSet) {
            if (element.charAt(0) != '\0')
                sb.append(element).append(',');
        }
        sb.delete(sb.length() - 1, sb.length());
        if (sb.length() != follow.length()) {
            isAdd = true;
            followMap.put(key, sb.toString());
        }
        return isAdd;
    }

    public Map<Character, String> getFollowMap() {
        return followMap;
    }

    public static void main(String[] args) {
        String[] production = CodeTable.production;

        Nullable nullAble = new Nullable(production);
        List<Character> nullableList = nullAble.countNullable();
        First first = new First(production, nullableList);
        Map<Character, String> firstMap = first.countFirst();
        Follow follow = new Follow(production, nullableList, firstMap);
        Map<Character, String> followMap = follow.countFollow();
        System.out.println(followMap);
    }
}
