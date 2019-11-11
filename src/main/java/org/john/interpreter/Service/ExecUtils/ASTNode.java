package org.john.interpreter.Service.ExecUtils;

import java.io.Serializable;
import java.util.Arrays;

public class ASTNode implements Serializable {

    private int maxChildNum;
    private int curIndex;
    private String name;
    private String value = null; // TODO can be defined as complex data structure
    private boolean isLeaf; //是否为叶子节点
    private boolean isLegal; //是否合法(正确解析)
    private ASTNode[] children;
    private ASTNode parent;

    public ASTNode(){}

    public ASTNode(int maxChildNum, String name, boolean isLeaf,boolean isLegal) {
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
            if (ch != null &&  ch.isLegal&&ch.hasChildLefted())
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

    public String toJSON(){
        StringBuilder json = new StringBuilder("{\"Name\":\" " + name + "\"");
        if (maxChildNum != 0) {
            json.append(",\"children\": [");
            for (ASTNode child:children) {
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

    public void addNullTips(){
        // Pre-order traverse the tree
        if (maxChildNum == 0 && value == null) {
            if (name.charAt(0) > 'A' && name.charAt(0) < 'Z')
                name += " -> null";
        }
        else if (value != null)
            name += " (" + value + ")";
        for (ASTNode child : children){
            if (child != null)
                child.addNullTips();
        }
    }

    public String getName() {
        return name;
    }

    public ASTNode[] getChildren() {
        return children;
    }

    public static void  main(String[] args){
        ASTNode node = new ASTNode(1,"P",false,true);
        ASTNode node1 = new ASTNode(0,"S",true,false);
        node.addChild(node1);
        System.out.println(node.toJSON());
    }
}
