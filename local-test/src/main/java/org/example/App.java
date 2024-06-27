package org.example;

import com.ctrip.framework.apollo.spring.annotation.EnableApolloConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Hello world!
 */
@EnableApolloConfig
@SpringBootApplication
@RestController
public class App {
    @Value("${name:}")
    private String name;

    @Value("${age:0}")
    private Integer age;

    @GetMapping("/test")
    public String test(){
        return name + " " + age;
    }

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}
