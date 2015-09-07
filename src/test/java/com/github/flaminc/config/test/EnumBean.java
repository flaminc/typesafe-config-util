package com.github.flaminc.config.test;

import javax.annotation.Resource;

/**
 * @author Chris Flaming 1/15/2015
 */
public class EnumBean {
    @Resource
    private EnumInstance value;

    public EnumInstance getValue() {
        return value;
    }

    public void setValue(EnumInstance value) {
        this.value = value;
    }
}
