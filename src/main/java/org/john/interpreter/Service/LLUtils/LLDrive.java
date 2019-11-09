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

    private List<String> selectList = new ArrayList<>(); // index��Ӧ�Ĳ���ʽ�� Select��

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
        //������Ӧ�� First���� Follow��
        Nullable nullAble = new Nullable(productions);
        nullableList = nullAble.countNullable();
        First first = new First(productions, nullableList);
        firstMap = first.countFirst();
        Follow follow = new Follow(productions, nullableList, firstMap);
        followMap = follow.countFollow();

        tMap = new HashMap<>(); //�ұ��ս�� ����
        ntMap = new HashMap<>(); //��߷��ս�� ����
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
        //�������в���ʽ�� Select��
        countFirst_s();

        List<String> special_production_list = Arrays.asList(CodeTable.special_production);
        table = new int[ntMap.size()][tMap.size()]; //���� LL������,��������ڼ�������ʽ

        for (int[] aTable : table) {
            Arrays.fill(aTable, -1); // ���� ����
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
                        table[y][x] = -2; // �������� -2 ָ�����⴦��
                        continue;
                    }
                    throw new Exception("���޸Ĳ���ʽ���˷� LL(1) �ķ���");
                }
            }
        }
    }

    private void countFirst_s() { //���� Select��
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
                } else { //��Ϊ�ս�����Ҹ� ���ս����First��
                    if (sb.length() != 0)
                        sb.append(' ');
                    sb.append(firstMap.get(t));
                    if (!nullableList.contains(t))
                        break;
                }
//                String follow = followMap.get(pNode.leftP);
                String follow = followMap.get(t); //��ÿ���Ϊ�յķ��ս���� Follow��
                if (follow != null) {
                    if (sb.length() > 0)
                        sb.append(' ');
                    sb.append(follow);
                }
            }
            if (rightP.size() == 0) //�ձ��ʽ������£����� forѭ������ִ��
                sb.append(followMap.get(pNode.leftP));

            //���� sb �л����ظ��� �ս�������� HashSet��ȥ���ظ���Ԫ��
            Set<String> first_sSet = new HashSet<>(Arrays.asList(sb.toString().split(" ")));
            sb.delete(0, sb.length());

            first_sSet.remove("");
            for (String str : first_sSet)
                sb.append(str).append(' ');
            sb.delete(sb.length() - 1, sb.length()); // ɾ�����һ�� �ո�
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
            stack.addFirst(t); //�ӵ���һ��λ��
        }
        return rp.size(); // Ϊ0˵��Ϊ�ձ��ʽ
    }

}
