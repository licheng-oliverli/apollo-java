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
package com.ctrip.framework.apollo.spring.spi;

import com.ctrip.framework.apollo.core.spi.Ordered;
import com.ctrip.framework.apollo.spring.annotation.ApolloAnnotationProcessor;
import com.ctrip.framework.apollo.spring.annotation.EnableApolloConfig;
import com.ctrip.framework.apollo.spring.annotation.SpringConfigurationPropertiesProcessor;
import com.ctrip.framework.apollo.spring.annotation.SpringValueProcessor;
import com.ctrip.framework.apollo.spring.config.PropertySourcesProcessor;
import com.ctrip.framework.apollo.spring.property.AutoUpdateConfigChangeListener;
import com.ctrip.framework.apollo.spring.property.SpringValueDefinitionProcessor;
import com.ctrip.framework.apollo.spring.util.BeanRegistrationUtil;
import com.google.common.collect.Lists;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;

public class DefaultApolloConfigRegistrarHelper implements ApolloConfigRegistrarHelper {
  private static final Logger logger = LoggerFactory.getLogger(DefaultApolloConfigRegistrarHelper.class);

  private Environment environment;

  @Override
  public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
    AnnotationAttributes attributes = AnnotationAttributes.fromMap(importingClassMetadata.getAnnotationAttributes(EnableApolloConfig.class.getName()));
    final String[] namespaces = attributes.getStringArray("value");
    final int order = attributes.getNumber("order");
    // 解析所有namespace，避免有占位符的namespace
    final String[] resolvedNamespaces = this.resolveNamespaces(namespaces);
    // 数据源处理器添加所有namespace
    PropertySourcesProcessor.addNamespaces(Lists.newArrayList(resolvedNamespaces), order);

    Map<String, Object> propertySourcesPlaceholderPropertyValues = new HashMap<>();
    propertySourcesPlaceholderPropertyValues.put("order", 0);
    // 以environment为数据源，初次启动时解析@Value和配置文件里的占位符
    BeanRegistrationUtil.registerBeanDefinitionIfNotExists(registry, PropertySourcesPlaceholderConfigurer.class, propertySourcesPlaceholderPropertyValues);
    // 初始化数据源：把注解里所有namespace初始化成PropertySource，添加到environment。config监听到配置修改时会推送changeEvent
    BeanRegistrationUtil.registerBeanDefinitionIfNotExists(registry, PropertySourcesProcessor.class);
    // 热更新：SpringValueRegistry的配置修改时的回调，解析占位符，通过反射写回去，实现@Value的热更新
    BeanRegistrationUtil.registerBeanDefinitionIfNotExists(registry, AutoUpdateConfigChangeListener.class);
    // 用反射处理@ApolloConfig、@ApolloJsonValue、@ApolloConfigChangeListener，反射回调 + 注册到SpringValueRegistry
    BeanRegistrationUtil.registerBeanDefinitionIfNotExists(registry, ApolloAnnotationProcessor.class);
    // 把标注@Value的字段和方法还有xml注册到SpringValueRegistry
    BeanRegistrationUtil.registerBeanDefinitionIfNotExists(registry, SpringValueProcessor.class);
    // 待测试
    BeanRegistrationUtil.registerBeanDefinitionIfNotExists(registry, SpringValueDefinitionProcessor.class);
    // 新增：@ConfigurationProperties配置自动更新
    BeanRegistrationUtil.registerBeanDefinitionIfNotExists(registry, SpringConfigurationPropertiesProcessor.class);
  }

  // 如果@EnableApolloConfig里namespace里有占位符，解析占位符
  private String[] resolveNamespaces(String[] namespaces) {
    // no support for Spring version prior to 3.2.x, see https://github.com/apolloconfig/apollo/issues/4178
    if (this.environment == null) {
      logNamespacePlaceholderNotSupportedMessage(namespaces);
      return namespaces;
    }
    String[] resolvedNamespaces = new String[namespaces.length];
    for (int i = 0; i < namespaces.length; i++) {
      // throw IllegalArgumentException if given text is null or if any placeholders are unresolvable
      resolvedNamespaces[i] = this.environment.resolveRequiredPlaceholders(namespaces[i]);
    }
    return resolvedNamespaces;
  }

  private void logNamespacePlaceholderNotSupportedMessage(String[] namespaces) {
    for (String namespace : namespaces) {
      if (namespace.contains("${")) {
        logger.warn("Namespace placeholder {} is not supported for Spring version prior to 3.2.x,"
                + " see https://github.com/apolloconfig/apollo/issues/4178 for more details.",
            namespace);
        break;
      }
    }
  }

  @Override
  public int getOrder() {
    return Ordered.LOWEST_PRECEDENCE;
  }

  @Override
  public void setEnvironment(Environment environment) {
    this.environment = environment;
  }
}