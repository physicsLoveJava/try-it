package com.lujian.mvc.test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.lujian.mvc.annotation.RequestParam;
import org.junit.Test;

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

    @Test
    public void testMain1() {
        Method[] methods = TestUtility.class.getMethods();
        for (Method method : methods) {
            Annotation[][] pa = method.getParameterAnnotations();
            System.out.println("method parameterAnnotations: " + method.getName());
            for (int i = 0; i < pa.length; i++) {
                for (Annotation annotation : pa[i]) {
                    System.out.println(String.format("index: %s, annotation: %s", i, annotation));
                }
            }
            System.out.println();
            System.out.println("method parameterTypes");
            Class<?>[] paramsTypes = method.getParameterTypes();
            for (int i = 0; i < paramsTypes.length; i++) {
                System.out.println(String.format("index: %s, parameterType: %s", i, paramsTypes));
            }
        }
    }

    @Test
    public void testMain2() {
        System.out.println("[tom]".replaceAll("\\[|\\]", ""));
    }

    public void methodA(String d, @RequestParam String a, @RequestParam String b, String c) {

    }

}
