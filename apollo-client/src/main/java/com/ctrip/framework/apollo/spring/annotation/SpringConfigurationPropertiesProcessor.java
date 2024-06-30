/*
 * Copyright 2022 Apollo Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.ctrip.framework.apollo.spring.annotation;

import com.ctrip.framework.apollo.build.ApolloInjector;
import com.ctrip.framework.apollo.spring.property.SpringValueRegistry;
import com.ctrip.framework.apollo.spring.util.SpringInjector;
import com.ctrip.framework.apollo.util.ConfigUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 处理字段和方法上的@Value和xml
 * 把标注@Value的字段和方法还有xml注册到SpringValueRegistry
 */
public class SpringConfigurationPropertiesProcessor extends ApolloProcessor implements BeanFactoryAware {

    private static final Logger logger = LoggerFactory.getLogger(SpringConfigurationPropertiesProcessor.class);

    private final ConfigUtil configUtil;
    private final SpringValueRegistry springValueRegistry;
    private BeanFactory beanFactory;

    public SpringConfigurationPropertiesProcessor() {
        springValueRegistry = SpringInjector.getInstance(SpringValueRegistry.class);
        configUtil = ApolloInjector.getInstance(ConfigUtil.class);
    }

    @Override
    protected void processField(Object bean, String beanName, Field field) {
    }

    @Override
    protected void processMethod(Object bean, String beanName, Method method) {
    }

    /**
     * 1. 要开启自动更新
     * 2. 是@ConfigurationProperties
     * 3. 是@RefreshScope
     * 4. beanFactory里有xxx.xxx.RefreshScope的Bean
     * 5. 回调调用RefreshScope的refresh()
     */
    @Override
    protected void processClass(Object bean, String beanName, Class<?> clazz) {
        try {
            ConfigurationProperties configurationPropertiesAnnotation = clazz.getDeclaredAnnotation(ConfigurationProperties.class);
            if (configUtil.isAutoUpdateInjectedSpringPropertiesEnabled() && configurationPropertiesAnnotation != null) {
                for(Annotation annotation : clazz.getDeclaredAnnotations()){
                    if (annotation.getClass() == Class.forName("xxx.xxx.RefreshScope")) {
                        // todo beanFactory里有xxx.xxx.RefreshScope的Bean
                        // todo 回调调用RefreshScope的refresh()
                    }
                }

            }
        } catch (ClassNotFoundException e) {
            logger.warn("RefreshScope class not found!", e);
        }
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }
}
