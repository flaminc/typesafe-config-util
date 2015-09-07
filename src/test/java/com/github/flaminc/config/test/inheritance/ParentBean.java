package com.github.flaminc.config.test.inheritance;

import javax.annotation.Resource;

/**
 * @author Chris Flaming 1/15/2015
 */
public class ParentBean {
    @Resource
    Parent obj;

    public Parent getObj() {
        return obj;
    }

    public void setObj(Parent obj) {
        this.obj = obj;
    }
}
