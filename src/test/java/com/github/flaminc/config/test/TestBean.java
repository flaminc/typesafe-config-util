package com.github.flaminc.config.test;

import javax.annotation.Resource;

/**
 * @author Chris Flaming 1/15/2015
 */
public class TestBean {
    @Resource
    private boolean primBoolean;
    @Resource
    private Boolean objBoolean;
    @Resource
    private int primInteger;
    @Resource
    private Integer objInteger;

    public boolean isPrimBoolean() {
        return primBoolean;
    }

    public void setPrimBoolean(boolean primBoolean) {
        this.primBoolean = primBoolean;
    }

    public Boolean getObjBoolean() {
        return objBoolean;
    }

    public void setObjBoolean(Boolean objBoolean) {
        this.objBoolean = objBoolean;
    }

    public int getPrimInteger() {
        return primInteger;
    }

    public void setPrimInteger(int primInteger) {
        this.primInteger = primInteger;
    }

    public Integer getObjInteger() {
        return objInteger;
    }

    public void setObjInteger(Integer objInteger) {
        this.objInteger = objInteger;
    }
}
