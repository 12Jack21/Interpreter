package org.john.interpreter.Service.ExecUtils;

public class LexiNode {
    private String symbol;
    private int code;
    private int row;
    private int col;

    private int p; //词法扫描的指针位置

    public LexiNode(String symbol, int code, int row, int col, int p) {
        this.symbol = symbol;
        this.code = code;
        this.row = row;
        this.col = col;
        this.p = p;
    }

    public int getLength() {
        return symbol.length();
    }

    public String errorMsg() {
        return null;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public int getCode() {
        return code;
    }

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public int getCol() {
        return col;
    }

    public void setCol(int col) {
        this.col = col;
    }

    public int getP() {
        return p;
    }

    public void setP(int p) {
        this.p = p;
    }

    @Override
    public String toString() {
        String symbol = this.symbol;

        if (!symbol.equals("")) {
            if (this.symbol.charAt(0) == '\n')
                symbol = "\\n";
            else if (this.symbol.charAt(0) == '\r')
                symbol = "\\r";
            else if (this.symbol.charAt(0) == '\t')
                symbol = "\\t";
        }
        String format = "<" + symbol.trim() + ", " + code + ", " + row + ", " + col + ">";
        if (code == -1 && !symbol.equals(""))
            format += "\tError Occurs!!!";
        return format;
    }
}
