package org.john.interpreter.Service.ExecUtils;

import java.io.Serializable;
import java.util.Arrays;

public class ASTNode implements Serializable {

    private boolean find = false;

    private int maxChildNum;
    private int curIndex;
    private String name;
    private String value = null;
    private boolean isLeaf; //是否为叶子节点
    private boolean isLegal; //是否合法(正确解析)
    private ASTNode[] children;
    private ASTNode parent;

    public ASTNode() {
    }

    public ASTNode(ASTNode root){
        curIndex = 0;
        name = root.name;
        value = root.value;
        maxChildNum = root.maxChildNum;
        children = new ASTNode[maxChildNum];
        while (curIndex < maxChildNum) {
            children[curIndex] = new ASTNode(root.children[curIndex]);
            curIndex++;
        }
    }

    public ASTNode(int maxChildNum, String name, String value) {
        this.curIndex = 0;
        this.maxChildNum = maxChildNum;
        this.name = name;
        this.value = value;
        this.children = new ASTNode[maxChildNum];
    }

    public ASTNode(int maxChildNum, String name, boolean isLeaf, boolean isLegal) {
        this.curIndex = 0;
        this.maxChildNum = maxChildNum;
        this.name = name;
        this.isLeaf = isLeaf;
        this.children = new ASTNode[maxChildNum];
        this.isLegal = isLegal;
    }

    public ASTNode(int maxChildNum, String name, String value, boolean isLeaf, boolean isLegal) {
        this.maxChildNum = maxChildNum;
        this.name = name;
        this.value = value;
        this.isLeaf = isLeaf;
        this.isLegal = isLegal;
        this.curIndex = 0;
        this.children = new ASTNode[maxChildNum];
    }

    public String getValue() {
        return value;
    }

    public int getMaxChildNum() {
        return maxChildNum;
    }

    public boolean isLeaf() {
        return isLeaf;
    }

    public ASTNode getParent() {
        return parent;
    }

    public void addChild(ASTNode child) {
        children[curIndex++] = child;
        child.parent = this;
    }

    //孩子是否还剩下来
    public boolean hasChildLefted() {
        return curIndex != maxChildNum;
    }


    public ASTNode findLefted() {
        for (ASTNode ch : children) {
            if (ch != null && ch.isLegal && ch.hasChildLefted())
                return ch;
        }
        if (isLegal && hasChildLefted())
            return this;

        if (this.parent == null) // root node
            return null;

        return this.parent.findLefted();
    }

    @Override
    public String toString() {
        String str = "Node{" +
                "name=" + name;
        if (!isLeaf)
            str += ", children=" + Arrays.toString(children) + '}';
        else
            str += '}';
        return str;
    }

    public String toJSON() {
        StringBuilder json = new StringBuilder("{\"Name\":\" " + name + "\"");
        if (maxChildNum != 0) {
            json.append(",\"children\": [");
            for (ASTNode child : children) {
                if (child == null)
                    continue;
                json.append(child.toJSON()).append(",");
            }
            if (json.toString().endsWith(","))
                json.deleteCharAt(json.length() - 1); //删掉最后一个 逗号
            json.append("]");
        }
        json.append("}");
        return json.toString();
    }

    public void addNullTips() {
        // Pre-order traverse the tree
        if (maxChildNum == 0 && value == null) {
            if (name.charAt(0) > 'A' && name.charAt(0) < 'Z')
                name += " -> null";
        } else if (value != null)
            name += " (" + value + ")";
        for (ASTNode child : children) {
            if (child != null)
                child.addNullTips();
        }
    }

    public void setParentNull() {
        this.parent = null;
        for (ASTNode child : children) {
            if (child != null)
                child.setParentNull();
        }
    }

    public String getName() {
        return name;
    }

    public ASTNode[] getChildren() {
        return children;
    }

    // 找到先序遍历时 没找到过的最前叶子节点
    public ASTNode findNextNodeWithValueOrTip(String tip) {
        ASTNode re = null;
        for (int i = 0; i < maxChildNum; i++) {
            ASTNode child = children[i];

            // 找 name = tip 的节点
            if (child != null && child.name.equals(tip) && !child.find) {
                re = child;
                break;
            } else if (child != null) {
                re = child.findNextNodeWithValueOrTip(tip);
                if (re != null) {
                    re.find = true;
                    break;
                }
            }

        }
        return re;
    }

    public void flushFindTag() {
        find = false;
        for (ASTNode child : children) {
            if (child != null)
                child.flushFindTag();
        }
    }


    public static void main(String[] args) {
        ASTNode arith = new ASTNode(2, "Arithmetic", null);
        ASTNode item = new ASTNode(2, "Item", null);
        ASTNode v_node = new ASTNode(2, "V", null);
        ASTNode var_node = new ASTNode(0, "Variable", "1212");

        ASTNode fact = new ASTNode(2, "Factor", null);
        ASTNode mul = new ASTNode(0, "*", "*");
        ASTNode item1 = new ASTNode(2, "Item", null);
        ASTNode var1 = new ASTNode(0, "Variable", "90909012");
        ASTNode fac1 = new ASTNode(0, "Factor", null);

        ASTNode plus = new ASTNode(0, "+", "+");
        ASTNode ari1 = new ASTNode(2, "Arithmetic", null);
        ASTNode item2 = new ASTNode(2, "Item", null);
        ASTNode var2 = new ASTNode(0, "Variable", "qe123");
        ASTNode fac2 = new ASTNode(0, "Factor", null);
        ASTNode v_node1 = new ASTNode(0, "V", null);

        arith.addChild(item);
        arith.addChild(v_node);
        item.addChild(var_node);
        item.addChild(fact);
        fact.addChild(mul);
        fact.addChild(item1);
        item1.addChild(var1);
        item1.addChild(fac1);

        v_node.addChild(plus);
        v_node.addChild(ari1);
        ari1.addChild(item2);
        ari1.addChild(v_node1);
        item2.addChild(var2);
        item2.addChild(fac2);

        ASTNode var = arith.findNextNodeWithValueOrTip("Variable");
        ASTNode symbol = arith.findNextNodeWithValueOrTip("symbol");
        ASTNode copy = new ASTNode(arith);
        System.out.println(copy);
    }
}
