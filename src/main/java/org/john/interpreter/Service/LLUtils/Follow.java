package org.john.interpreter.Service.LLUtils;

import org.john.interpreter.Service.ExecUtils.CodeTable;

import java.util.*;

public class Follow {
    private String[] production;
    private Map<String, String> followMap;

    private Map<String, String> firstMap;
    private List<String> nullableList;

    public Follow(String[] production, List<String> nullableList, Map<String, String> firstMap) {
        this.production = production;
        this.firstMap = firstMap;
        this.nullableList = nullableList;
    }

    public Map<String, String> countFollow() {
        followMap = new HashMap<>();
        followMap.put("Pro","#"); // firstly manual add #
        boolean isChange = true;
        while (isChange) {
            isChange = false;
            for (String p : production) {
                Node pNode = Node.splitP(p);
                String leftP = pNode.leftP;
                String followTemp = followMap.get(leftP); //用于暂存终结符
                List<String> rightP = pNode.rightP;

                //对于产生式右边 从右往左遍历
                for (int i = rightP.size() - 1; i >= 0; i--) {
                    String t = rightP.get(i);
                    if (t.charAt(0) < 'A' || t.charAt(0) > 'Z') {
                        followTemp = t;
                    } else {
                        String follow = followMap.get(t);
                        if (follow != null) {
                            if (followTemp != null)
                                if (add(followTemp, t)) //加入Follow集
                                    isChange = true;
                        } else if (followTemp != null) { //Follow集为空
                            followMap.put(t, followTemp.replace("\0,", ""));
                            isChange = true;
                        }
                        if (nullableList.contains(t) && followTemp != null && firstMap.get(t) != null) {
                            followTemp = followTemp + ' ' + firstMap.get(t);
                        } else
                            followTemp = firstMap.get(t); //也可以为null
                    }
                }
            }
        }
        return followMap;
    }

    @SuppressWarnings("DuplicatedCode")
    // 通用的 将 followTemp的终结符加入 key（非终结符）的follow集中
    private boolean add(String followTemp, String key) {
         boolean isAdd = false;
        String follow = followMap.get(key);

        String[] arr1 = follow.split(" ");
        String[] arr2 = followTemp.split(" ");
        Set<String> followSet = new HashSet<>(Arrays.asList(arr1)); //两个里面只要有一个有就加上
        followSet.addAll(Arrays.asList(arr2));
        followSet.remove(""); // 去掉 空
        StringBuilder sb = new StringBuilder();
        for (String element : followSet) {
            if (element.charAt(0) != '\0') // TODO doubt
                sb.append(element).append(' ');
        }
        sb.delete(sb.length() - 1, sb.length());
        if (sb.length() != follow.length()) {
            isAdd = true;
            followMap.put(key, sb.toString());
        }
        return isAdd;
    }

    public Map<String, String> getFollowMap() {
        return followMap;
    }

    public static void main(String[] args) {
        String[] production = CodeTable.productions;

        Nullable nullAble = new Nullable(production);
        List<String> nullableList = nullAble.countNullable();
        First first = new First(production, nullableList);
        Map<String, String> firstMap = first.countFirst();
        Follow follow = new Follow(production, nullableList, firstMap);
        Map<String, String> followMap = follow.countFollow();

        try {

            LLDrive drive = new LLDrive(production);
            System.out.println("Non-terminal map: " + drive.getNtMap());
        }catch (Exception e){
            e.printStackTrace();
        }

        System.out.println(followMap);
    }
}
