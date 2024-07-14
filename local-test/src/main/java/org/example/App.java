package org.example;

import com.ctrip.framework.apollo.spring.annotation.EnableApolloConfig;
import org.example.bind.BindHelper;
import org.example.bind.Data;
import org.example.bind.Data1;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.context.scope.refresh.RefreshScope;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Hello world!
 */
@EnableApolloConfig(value = {"application","test1","test.yml","test2"})
@SpringBootApplication
@RestController
@Import(ImportTest.class)
public class App {
    @Autowired
    private RefreshScopeTest1 refreshScopeTest1;

    @Autowired
    private BindHelper bindHelper;

    @Autowired
    private Data data;

    @Autowired
    private Data1 data1;

    @GetMapping("/test")
    public String test() {
        return refreshScopeTest1.str();
    }

//    @Override
//    public String toString() {
//        return "App{" +
//                "name='" + name + '\'' +
//                ", age=" + age +
//                ", teacher=" + teacher +
//                ", nodes=" + nodes +
//                ", refreshScopeTest=" + refreshScopeTest1 +
//                '}';
//    }

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}
