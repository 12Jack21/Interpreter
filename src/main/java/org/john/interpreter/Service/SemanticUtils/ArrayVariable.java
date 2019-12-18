package org.john.interpreter.Service.SemanticUtils;

import java.util.ArrayList;

public class ArrayVariable {
    private String arrayName; //数组名
    private String type; //数组类型
    private ArrayList<Integer> dimensionList; // 多维数组的维度表
    private ArrayList<String> values; //数组存的值列表
    private int level; // 作用域

    public ArrayVariable() {
    }

    public ArrayVariable(String arrayName, String type, ArrayList<Integer> dimensionList, ArrayList<String> values, int level) {
        this.arrayName = arrayName;
        this.type = type;
        if (dimensionList == null)
            this.dimensionList = null;
        else
            this.dimensionList = new ArrayList<>(dimensionList);
        if (values == null)
            this.values = null;
        else
            this.values = new ArrayList<>(values);
        this.level = level;
    }

    public void setArrayName(String arrayName) {
        this.arrayName = arrayName;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setValues(ArrayList<String> values) {
        this.values = values;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getArrayName() {
        return arrayName;
    }

    public String getType() {
        return type;
    }

    public ArrayList<String> getValues() {
        return values;
    }

    public int getLevel() {
        return level;
    }

    public ArrayList<Integer> getDimensionList() {
        return dimensionList;
    }

    public void setDimensionList(ArrayList<Integer> dimensionList) {
        this.dimensionList = dimensionList;
    }

    public Integer getMaxLengthByDimension(int dimension) {
        // 从第0维开始
        return dimensionList.get(dimension);
    }
}
