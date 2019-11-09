package org.john.interpreter.Service.LLUtils;

import org.john.interpreter.Service.ExecUtils.CodeTable;

import java.util.*;

public class LLDrive {
    private int[][] table;
    private String[] productions;
    private Map<String, Integer> tMap;
    private Map<String, Integer> ntMap; // non-terminal

    private Map<String, String> followMap;
    private Map<String, String> firstMap;
    private List<String> nullableList;

    private List<String> selectList = new ArrayList<>(); // index对应的产生式的 Select集

    public LLDrive(String[] productions) throws Exception {
        this.productions = productions;
        init();
    }

    public Map<String, Integer> gettMap() {
        return tMap;
    }

    public Map<String, Integer> getNtMap() {
        return ntMap;
    }

    public Map<String, String> getFirstMap() {
        return firstMap;
    }

    private void init() throws Exception {
        //计算相应的 First集和 Follow集
        Nullable nullAble = new Nullable(productions);
        nullableList = nullAble.countNullable();
        First first = new First(productions, nullableList);
        firstMap = first.countFirst();
        Follow follow = new Follow(productions, nullableList, firstMap);
        followMap = follow.countFollow();

        tMap = new HashMap<>(); //右边终结符 计数
        ntMap = new HashMap<>(); //左边非终结符 计数
        int num = 0;
        for (String p : productions) {
            String t = Node.splitP(p).leftP;
            if (ntMap.get(t) != null) {
                continue;
            }
            ntMap.put(t, num++);
        }
        num = 0;
        for (String p : productions) {
            List<String> rp = Node.splitP(p).rightP;
            for (int i = 0; i < rp.size(); i++) {
                String t = rp.get(i);
                if (t.charAt(0) < 'A' || t.charAt(0) > 'Z') {
                    if (tMap.get(t) != null)
                        continue;
                    tMap.put(t, num++);
                }
            }
        }
        tMap.put("#", num); // add # to predict table column
        initTable();
    }

    private void initTable() throws Exception {
        //计算所有产生式的 Select集
        countFirst_s();

        List<String> special_production_list = Arrays.asList(CodeTable.special_production);
        table = new int[ntMap.size()][tMap.size()]; //构建 LL分析表,整数代表第几个产生式

        for (int[] aTable : table) {
            Arrays.fill(aTable, -1); // 代表 报错
        }
        String production;
        for (int i = 0; i < selectList.size(); i++) {
            String list = selectList.get(i);
            for (String cStr : list.split(" ")) {
                production = productions[i];
                int x = tMap.get(cStr);
                int y = ntMap.get(Node.splitP(production).leftP);
                if (manualProcess(table, cStr, i))
                    continue;
                if (table[y][x] == -1)
                    table[y][x] = i;
                else {
                    if (special_production_list.contains(production)){
                        table[y][x] = -2; // 特殊整数 -2 指代特殊处理
                        continue;
                    }
                    throw new Exception("请修改产生式，此非 LL(1) 文法！");
                }
            }
        }
    }

    private void countFirst_s() { //计算 Select集
        for (String p : productions) {
            StringBuilder sb = new StringBuilder();
            Node pNode = Node.splitP(p);
            List<String> rightP = pNode.rightP;
            for (int i = 0; i < rightP.size(); i++) {
                String t = rightP.get(i);
                if (t.charAt(0) < 'A' || t.charAt(0) > 'Z') {
                    if (sb.length() != 0)
                        sb.append(' ');
                    sb.append(t);
                    break;
                } else { //不为终结符则找该 非终结符的First集
                    if (sb.length() != 0)
                        sb.append(' ');
                    sb.append(firstMap.get(t));
                    if (!nullableList.contains(t))
                        break;
                }
//                String follow = followMap.get(pNode.leftP);
                String follow = followMap.get(t); //获得可以为空的非终结符的 Follow集
                if (follow != null) {
                    if (sb.length() > 0)
                        sb.append(' ');
                    sb.append(follow);
                }
            }
            if (rightP.size() == 0) //空表达式的情况下，上面 for循环不会执行
                sb.append(followMap.get(pNode.leftP));

            //由于 sb 中会有重复的 终结符，故用 HashSet来去掉重复的元素
            Set<String> first_sSet = new HashSet<>(Arrays.asList(sb.toString().split(" ")));
            sb.delete(0, sb.length());

            first_sSet.remove("");
            for (String str : first_sSet)
                sb.append(str).append(' ');
            sb.delete(sb.length() - 1, sb.length()); // 删除最后一个 空格
            selectList.add(sb.toString());
        }
    }

    //manual process the predict table (choose one of the contradict production to fill the table)
    private boolean manualProcess(int[][] table, String c, int index) {
        String nt = Node.splitP(productions[index]).leftP;
        return nt.equals("ELSE") && c.equals("else") && productions[index].equals("ELSE->");
    }

    public int[][] getTable() {
        return table;
    }

    public int addToStack(int pos, LinkedList<String> stack) {
        List<String> rp = Node.splitP(productions[pos]).rightP;
        for (int i = rp.size() - 1; i >= 0; i--) {
            String t = rp.get(i);
            stack.addFirst(t); //加到第一个位置
        }
        return rp.size(); // 为0说明为空表达式
    }

}
