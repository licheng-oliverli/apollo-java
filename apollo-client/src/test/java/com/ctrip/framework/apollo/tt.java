package com.ctrip.framework.apollo;

import com.ctrip.framework.apollo.spring.property.SpringValue;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import org.springframework.beans.factory.BeanFactory;

import java.util.Map;

public class tt {
    public static void main(String[] args) {
        Multimap<String, String> map = Multimaps.synchronizedListMultimap(LinkedListMultimap.create());
        map.put("1", "1");
        map.put("1", "2");
        map.put("1", "2");
        System.out.println(map.get("1"));
    }
}
