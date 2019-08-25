package com.lujian.mvc.test;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author lj167323@alibaba-inc.com
 */
public class TestUtility {

    public static void main(String[] args) {
        Map<Integer, Integer> map = new HashMap<>();
        map.put(1, 1);
        map.put(2, 2);
        for (Entry<Integer, Integer> entry : map.entrySet()) {
            if (entry.getKey() == 1) {
                map.put(3, 3);
            }
        }
        System.out.println(map);
    }

}
