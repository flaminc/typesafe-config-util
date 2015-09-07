package com.github.flaminc.config.test;

import javax.annotation.Resource;
import java.util.Map;

/**
 * @author Chris Flaming 1/14/2015
 */
public class BeanWithMap {
    @Resource
    private Map<String, SimpleBean> map;

    public Map<String, SimpleBean> getMap() {
        return map;
    }

    public void setMap(Map<String, SimpleBean> map) {
        this.map = map;
    }
}
