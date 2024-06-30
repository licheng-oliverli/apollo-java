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
package com.ctrip.framework.apollo.spring.config;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigChangeListener;
import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.apollo.build.ApolloInjector;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import com.ctrip.framework.apollo.spring.events.ApolloConfigChangeEvent;
import com.ctrip.framework.apollo.spring.util.PropertySourcesUtil;
import com.ctrip.framework.apollo.spring.util.SpringInjector;
import com.ctrip.framework.apollo.util.ConfigUtil;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.*;

/**
 * （完结）
 * 基于注解的配置源处理器
 * 1. 把注解里所有namespace初始化成PropertySource加入CompositePropertySource，并添加到environment
 * 2. 给所有Config添加修改事件监听器
 */
public class PropertySourcesProcessor implements BeanFactoryPostProcessor, EnvironmentAware, ApplicationEventPublisherAware, PriorityOrdered {
  // 注解启动的所有namespace
  private static final Multimap<Integer, String> NAMESPACE_NAMES = LinkedHashMultimap.create();
  private static final Set<BeanFactory> AUTO_UPDATE_INITIALIZED_BEAN_FACTORIES = Sets.newConcurrentHashSet();

  private final ConfigPropertySourceFactory configPropertySourceFactory = SpringInjector.getInstance(ConfigPropertySourceFactory.class);
  private ConfigUtil configUtil;
  private ConfigurableEnvironment environment;
  private ApplicationEventPublisher applicationEventPublisher;

  public static boolean addNamespaces(Collection<String> namespaces, int order) {
    return NAMESPACE_NAMES.putAll(order, namespaces);
  }

  @Override
  public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
    this.configUtil = ApolloInjector.getInstance(ConfigUtil.class);
    // 初始化所有namespace转成PropertySource加入CompositePropertySource，并添加到environment
    initializePropertySources();
    initializeAutoUpdatePropertiesFeature(beanFactory);
  }

  /**
   * 初始化所有namespace转成PropertySource加入CompositePropertySource，并添加到environment
   */
  private void initializePropertySources() {
    if (environment.getPropertySources().contains(PropertySourcesConstants.APOLLO_PROPERTY_SOURCE_NAME)) {
      return;
    }
    // CompositePropertySource可以加入多个数据源
    CompositePropertySource composite;
    if (configUtil.isPropertyNamesCacheEnabled()) {
      composite = new CachedCompositePropertySource(PropertySourcesConstants.APOLLO_PROPERTY_SOURCE_NAME);
    } else {
      composite = new CompositePropertySource(PropertySourcesConstants.APOLLO_PROPERTY_SOURCE_NAME);
    }

    // 依次构建PropertySource加入CompositePropertySource
    ImmutableSortedSet<Integer> orders = ImmutableSortedSet.copyOf(NAMESPACE_NAMES.keySet());
    for (int order : orders) {
      for (String namespace : NAMESPACE_NAMES.get(order)) {
        Config config = ConfigService.getConfig(namespace);
        composite.addPropertySource(configPropertySourceFactory.getConfigPropertySource(namespace, config));
      }
    }
    NAMESPACE_NAMES.clear();

    // 添加CompositePropertySource，确保Apollo辅助数据源高于CompositePropertySource
    if (environment.getPropertySources().contains(PropertySourcesConstants.APOLLO_BOOTSTRAP_PROPERTY_SOURCE_NAME)) {
      if (configUtil.isOverrideSystemProperties()) {
        // 确保辅助数据源优先级最高
        PropertySourcesUtil.ensureBootstrapPropertyPrecedence(environment);
      }
      // CompositePropertySource加到辅助数据源之后
      environment.getPropertySources().addAfter(PropertySourcesConstants.APOLLO_BOOTSTRAP_PROPERTY_SOURCE_NAME, composite);
    } else {
      if (!configUtil.isOverrideSystemProperties()) {
        if (environment.getPropertySources().contains(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME)) {
          environment.getPropertySources().addAfter(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, composite);
          return;
        }
      }
      environment.getPropertySources().addFirst(composite);
    }
  }

  /**
   * 给所有Config添加修改事件监听器
   */
  private void initializeAutoUpdatePropertiesFeature(ConfigurableListableBeanFactory beanFactory) {
    if (!AUTO_UPDATE_INITIALIZED_BEAN_FACTORIES.add(beanFactory)) {
      return;
    }
    // 配置修改时会推送changeEvent
    ConfigChangeListener configChangeEventPublisher = changeEvent -> applicationEventPublisher.publishEvent(new ApolloConfigChangeEvent(changeEvent));
    List<ConfigPropertySource> configPropertySources = configPropertySourceFactory.getAllConfigPropertySources();
    for (ConfigPropertySource configPropertySource : configPropertySources) {
      configPropertySource.addChangeListener(configChangeEventPublisher);
    }
  }

  @Override
  public void setEnvironment(Environment environment) {
    //it is safe enough to cast as all known environment is derived from ConfigurableEnvironment
    this.environment = (ConfigurableEnvironment) environment;
  }

  @Override
  public int getOrder() {
    //make it as early as possible
    return Ordered.HIGHEST_PRECEDENCE;
  }

  // for test only
  static void reset() {
    NAMESPACE_NAMES.clear();
    AUTO_UPDATE_INITIALIZED_BEAN_FACTORIES.clear();
  }

  @Override
  public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
    this.applicationEventPublisher = applicationEventPublisher;
  }
}
