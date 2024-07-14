package org.example;

import com.ctrip.framework.apollo.spring.annotation.ApolloConfigurationPropertiesRefresh;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@ApolloConfigurationPropertiesRefresh
@ConfigurationProperties(prefix = "refresh.common")
public class RefreshScopeTest1 {
    private static final Logger logger = LoggerFactory.getLogger(RefreshScopeTest1.class);

    private String node;
    private int commandTimeout;
    private Map<String, String> someMap = Maps.newLinkedHashMap();
    private List<String> someList = Lists.newLinkedList();

    public String getNode() {
        return node;
    }

    public void setNode(String node) {
        this.node = node;
    }

    public int getCommandTimeout() {
        return commandTimeout;
    }

    public void setCommandTimeout(int commandTimeout) {
        this.commandTimeout = commandTimeout;
    }

    public Map<String, String> getSomeMap() {
        return someMap;
    }

    public void setSomeMap(Map<String, String> someMap) {
        this.someMap = someMap;
    }

    public List<String> getSomeList() {
        return someList;
    }

    public void setSomeList(List<String> someList) {
        this.someList = someList;
    }

    public String str() {
        return String.format("%s，%s，%s，%s", node, commandTimeout, someMap, someList);
    }
}
