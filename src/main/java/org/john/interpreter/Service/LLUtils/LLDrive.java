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

    private Map<Character,String> syncMap; //ͬ�����ż�

    private List<String> selectList = new ArrayList<>(); //index��Ӧ�Ĳ���ʽ�� Select��

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
        //������Ӧ�� First���� Follow��
        Nullable nullAble = new Nullable(production);
        nullableList = nullAble.countNullable();
        First first = new First(production, nullableList);
        firstMap = first.countFirst();
        Follow follow = new Follow(production, nullableList, firstMap);
        followMap = follow.countFollow();

        xcharMap = new HashMap<>(); //�ұ��ս�� ����
        ycharMap = new HashMap<>(); //��߷��ս�� ����
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

    private void countFirst_s() { //���� Select��
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
                } else { //��Ϊ�ս�����Ҹ� ���ս����First��
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
            if ("".equals(rightP)) //�ձ��ʽ������£����� forѭ������ִ��
                sb.append(followMap.get(pNode.leftP));

            //���� sb �л����ظ��� �ս�������� HashSet��ȥ���ظ���Ԫ��
            Set<String> first_sSet = new HashSet<>(Arrays.asList(sb.toString().split(",")));
            if (first_sSet.size() == 0 ||sb.toString().contains(",,"))
                first_sSet.add(",");
            sb.delete(0, sb.length());

            first_sSet.remove("");
            for (String str : first_sSet)
                sb.append(str).append(",");
            sb.delete(sb.length() - 1, sb.length()); // ɾ�����һ�� ����
            selectList.add(sb.toString());

        }
    }

    private void initTable() throws Exception {
        //�������в���ʽ�� Select��
        countFirst_s();

        //���ڲ���
//        System.out.println(selectList);

        table = new int[ycharMap.size()][xcharMap.size()]; //���� LL������,��������ڼ�������ʽ

        for (int[] aTable : table) {
            Arrays.fill(aTable, -1); // ���� ����
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
                    throw new Exception("���޸Ĳ���ʽ���˷� LL(1) �ķ���");
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
            if (element < 'A' || element > 'Z') {    //�ս��
                if (token.equals(String.valueOf(element)))
                    ++i;
                else {
                    isTrue = false;
                    throw new Exception(i + " ����Ӧ��Ϊ " + element);
                }
            } else {                                                     //������ֵ���ս��
                int y = ycharMap.get(element);
                int x = xcharMap.get(token.charAt(0));
                int pos = table[y][x];
                if (pos > 0)
                    addStack(pos, stack);
                else
                    throw new Exception(i + " �����ִ���");
            }
        }
        if (stack.size() > 0)
            throw new Exception("���������벹��������");
        return isTrue;
    }

    public int addStack(int pos, LinkedList<Character> stack) {
        String rp = Node.splitP(production[pos]).rightP;
        for (int i = rp.length() - 1; i >= 0; i--) {
            char c = rp.charAt(i);
            stack.addFirst(c); //�ӵ���һ��λ��
        }
        return rp.length(); // Ϊ0˵��Ϊ�ձ��ʽ
    }

}
