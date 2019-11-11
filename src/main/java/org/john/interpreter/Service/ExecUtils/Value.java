package org.john.interpreter.Service.ExecUtils;

import java.io.Serializable;

public class Value implements Serializable {
    private String value;
    private Type type;

    enum Type{
        INT, REAL, CHAR
    }
    public Value(String value) {
        this.value = value;
    }

    public Value(String value, Type type) {
        this.value = value;
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }
}
