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
import com.ctrip.framework.apollo.core.utils.StringUtils;
import com.ctrip.framework.apollo.spring.property.SpringConfigurationPropertyRegistry;
import com.ctrip.framework.apollo.spring.util.SpringInjector;
import com.ctrip.framework.apollo.util.ConfigUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.ConfigurableEnvironment;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * 处理@ConfigurationProperties + @RefreshScope的类
 */
public class SpringConfigurationPropertiesProcessor implements ApplicationContextAware, BeanPostProcessor {

  private static final Logger logger = LoggerFactory.getLogger(SpringConfigurationPropertiesProcessor.class);

  private final ConfigUtil configUtil;
  private final SpringConfigurationPropertyRegistry springConfigurationPropertyRegistry;
  private ConfigurableBeanFactory beanFactory;
  private ConfigurableEnvironment environment;

  public SpringConfigurationPropertiesProcessor() {
    springConfigurationPropertyRegistry = SpringInjector.getInstance(SpringConfigurationPropertyRegistry.class);
    configUtil = ApolloInjector.getInstance(ConfigUtil.class);
  }

  /**
   * 1. 要开启自动更新 2. 是@ConfigurationProperties 3. 是@RefreshScope 4.
   * beanFactory里有xxx.xxx.RefreshScope的Bean 5. 回调调用RefreshScope的refresh()
   */
  @Override
  public Object postProcessBeforeInitialization(Object bean, String beanName) {
    Class<?> clazz = bean.getClass();
    ConfigurationProperties configurationPropertiesAnnotation = clazz.getDeclaredAnnotation(ConfigurationProperties.class);
    ApolloConfigurationPropertiesRefresh apolloConfigurationPropertiesRefreshAnnotation = clazz.getDeclaredAnnotation(ApolloConfigurationPropertiesRefresh.class);
    if (configUtil.isAutoUpdateInjectedSpringPropertiesEnabled() && configurationPropertiesAnnotation != null && (
        apolloConfigurationPropertiesRefreshAnnotation != null || isRefreshScope(clazz.getDeclaredAnnotations()))) {
      String prefix = configurationPropertiesAnnotation.prefix();
      springConfigurationPropertyRegistry.register(this.beanFactory, prefix, bean, environment);
      logger.info("Monitoring bean {}", beanName);
    }
    return bean;
  }

  private boolean isRefreshScope(Annotation[] annotations) {
    for (Annotation annotation : annotations) {
      if (annotation.annotationType().getName().equals("org.springframework.cloud.context.config.annotation.RefreshScope")) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    ConfigurableApplicationContext context = ((ConfigurableApplicationContext) applicationContext);
    this.beanFactory = context.getBeanFactory();
    this.environment = context.getEnvironment();
  }

}
