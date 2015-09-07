package com.github.flaminc.config.test;

import javax.annotation.Resource;

/**
 * @author Chris Flaming 1/14/2015
 */
public class SimpleBean {
    @Resource
    private String value;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public SimpleBean() {
    }

    public SimpleBean(String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SimpleBean)) return false;

        SimpleBean that = (SimpleBean) o;

        return !(value != null ? !value.equals(that.value) : that.value != null);

    }

    @Override
    public int hashCode() {
        return value != null ? value.hashCode() : 0;
    }
}
