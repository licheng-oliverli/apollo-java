package org.example.bind;


import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@lombok.Data
public class Data1 {
    private String hostName;

    private List<String> ports;

    private Map<String, String> map;

    public String str() {
        return "Data{" +
                "hostName='" + hostName + '\'' +
                ", ports=" + ports +
                ", map=" + map +
                '}';
    }
}