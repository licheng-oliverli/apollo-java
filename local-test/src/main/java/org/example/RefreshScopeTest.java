//package org.example;
//
//import com.google.common.collect.Lists;
//import com.google.common.collect.Maps;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.InitializingBean;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.context.properties.ConfigurationProperties;
//import org.springframework.cloud.context.config.annotation.RefreshScope;
//import org.springframework.stereotype.Component;
//
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//
//@Component
//@RefreshScope
//@ConfigurationProperties(prefix = "refresh.common")
//public class RefreshScopeTest {
//    private static final Logger logger = LoggerFactory.getLogger(RefreshScopeTest.class);
//
//    private String name;
//
//    private Integer age;
//
//    private List<String> teacher;
//
//    private Map<String, String> people;
//
//    private Set<Integer> nodes;
//
//    public String getName() {
//        return name;
//    }
//
//    public void setName(String name) {
//        this.name = name;
//    }
//
//    public Integer getAge() {
//        return age;
//    }
//
//    public void setAge(Integer age) {
//        this.age = age;
//    }
//
//    public List<String> getTeacher() {
//        return teacher;
//    }
//
//    public void setTeacher(List<String> teacher) {
//        this.teacher = teacher;
//    }
//
//    public Map<String, String> getPeople() {
//        return people;
//    }
//
//    public void setPeople(Map<String, String> people) {
//        this.people = people;
//    }
//
//    public Set<Integer> getNodes() {
//        return nodes;
//    }
//
//    public void setNodes(Set<Integer> nodes) {
//        this.nodes = nodes;
//    }
//
//    @Override
//    public String toString() {
//        return "RefreshScopeTest{" +
//                "name='" + name + '\'' +
//                ", age=" + age +
//                ", teacher=" + teacher +
//                ", people=" + people +
//                ", nodes=" + nodes +
//                '}';
//    }
//}
