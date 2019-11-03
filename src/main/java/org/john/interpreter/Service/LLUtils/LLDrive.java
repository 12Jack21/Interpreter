package org.john.interpreter.Service.LLUtils;

import java.util.*;

public class LLDrive {
    private int[][] table;
    private String[] production;
    private Map<Character, Integer> xcharMap;
    private Map<Character, Integer> ycharMap;

    private Map<Character, String> followMap;
    private Map<Character, String> firstMap;
    private List<Character> nullableList;

    private Map<Character,String> syncMap; //同步符号集

    private List<String> selectList = new ArrayList<>(); //index对应的产生式的 Select集

    public LLDrive(String[] production) throws Exception {
        this.production = production;
        init();
    }

    public Map<Character, Integer> getXcharMap() {
        return xcharMap;
    }

    public Map<Character, Integer> getYcharMap() {
        return ycharMap;
    }

    public Map<Character, String> getFirstMap() {
        return firstMap;
    }

    private void init() throws Exception {
        //计算相应的 First集和 Follow集
        Nullable nullAble = new Nullable(production);
        nullableList = nullAble.countNullable();
        First first = new First(production, nullableList);
        firstMap = first.countFirst();
        Follow follow = new Follow(production, nullableList, firstMap);
        followMap = follow.countFollow();

        xcharMap = new HashMap<>(); //右边终结符 计数
        ycharMap = new HashMap<>(); //左边非终结符 计数
        int num = 0;
        for (String p : production) {
            char c = p.charAt(0);
            if (ycharMap.get(c) != null) {
                continue;
            }
            ycharMap.put(c, num++);
        }
        num = 0;
        for (String p : production) {
            String lp = Node.splitP(p).rightP;
            for (int i = 0; i < lp.length(); i++) {
                char c = lp.charAt(i);
                if (c < 'A' || c > 'Z') {
                    if (xcharMap.get(c) != null)
                        continue;
                    xcharMap.put(c, num++);
                }
            }
        }
        xcharMap.put('#',num); // add # to predict table column
        initTable();
    }

    private void countFirst_s() { //计算 Select集
        for (String p : production) {
             StringBuilder sb = new StringBuilder();
            Node pNode = Node.splitP(p);
            String rightP = pNode.rightP;
            for (int i = 0; i < rightP.length(); i++) {
                char c = rightP.charAt(i);
                if (c < 'A' || c > 'Z') {
                    if (sb.length() != 0)
                        sb.append(",");
                    sb.append(c);
                    break;
                } else { //不为终结符则找该 非终结符的First集
                    if (sb.length() != 0)
                        sb.append(",");
                    sb.append(firstMap.get(c));
                    if (!nullableList.contains(c))
                        break;
                }
                String follow = followMap.get(pNode.leftP);
                if (follow != null) {
                    if (sb.length() > 0)
                        sb.append(",");
                    sb.append(follow);
                }
            }
            if ("".equals(rightP)) //空表达式的情况下，上面 for循环不会执行
                sb.append(followMap.get(pNode.leftP));

            //由于 sb 中会有重复的 终结符，故用 HashSet来去掉重复的元素
            Set<String> first_sSet = new HashSet<>(Arrays.asList(sb.toString().split(",")));
            if (first_sSet.size() == 0 ||sb.toString().contains(",,"))
                first_sSet.add(",");
            sb.delete(0, sb.length());

            first_sSet.remove("");
            for (String str : first_sSet)
                sb.append(str).append(",");
            sb.delete(sb.length() - 1, sb.length()); // 删除最后一个 逗号
            selectList.add(sb.toString());

        }
    }

    private void initTable() throws Exception {
        //计算所有产生式的 Select集
        countFirst_s();

        //用于测试
//        System.out.println(selectList);

        table = new int[ycharMap.size()][xcharMap.size()]; //构建 LL分析表,整数代表第几个产生式

        for (int[] aTable : table) {
            Arrays.fill(aTable, -1); // 代表 报错
        }
        for (int i = 0; i < selectList.size(); i++) {
            String list = selectList.get(i);
            for (String cStr : commaSplit(list)) {
                char c = cStr.charAt(0);
                int x = xcharMap.get(c);
                int y = ycharMap.get(production[i].charAt(0));
                if (manualProcess(table,c,i))
                    continue;
                if (table[y][x] == -1)
                    table[y][x] = i;
                else
                    throw new Exception("请修改产生式，此非 LL(1) 文法！");
            }

        }
    }

    //manual process the predict tabel
    private boolean manualProcess(int[][] table,char c,int index){
        char unTerminal = production[index].charAt(0);
        return unTerminal == 'D' && c == 'e' && production[index].equals("D->");
    }

    // split string with comma, but remains the original comma
    private String[] commaSplit(String list){
        HashSet<String> set = new HashSet<>(Arrays.asList(list.split(",")));
        if (list.length() != 0 && (set.size() == 0 || list.contains(",,")))
            set.add(",");
        set.remove("");
        return set.toArray(new String[set.size()]);
    }

    public int[][] getTable() {
        return table;
    }


    public boolean drive(String[] content) throws Exception {
        boolean isTrue = true;
        LinkedList<Character> stack = new LinkedList<>();
        addStack(0, stack);
        int i = 0;
        while (i < content.length) {
            String token = content[i];
            Character element = stack.pop();
            if (element < 'A' || element > 'Z') {    //终结符
                if (token.equals(String.valueOf(element)))
                    ++i;
                else {
                    isTrue = false;
                    throw new Exception(i + " 处，应该为 " + element);
                }
            } else {                                                     //弹出的值非终结符
                int y = ycharMap.get(element);
                int x = xcharMap.get(token.charAt(0));
                int pos = table[y][x];
                if (pos > 0)
                    addStack(pos, stack);
                else
                    throw new Exception(i + " 处出现错误！");
            }
        }
        if (stack.size() > 0)
            throw new Exception("不完整，请补充完整！");
        return isTrue;
    }

    public int addStack(int pos, LinkedList<Character> stack) {
        String rp = Node.splitP(production[pos]).rightP;
        for (int i = rp.length() - 1; i >= 0; i--) {
            char c = rp.charAt(i);
            stack.addFirst(c); //加到第一个位置
        }
        return rp.length(); // 为0说明为空表达式
    }

}
