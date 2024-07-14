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
package com.ctrip.framework.apollo.spring.property;

import com.ctrip.framework.apollo.ConfigChangeListener;
import com.ctrip.framework.apollo.build.ApolloInjector;
import com.ctrip.framework.apollo.core.utils.StringUtils;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import com.ctrip.framework.apollo.spring.annotation.ApolloJsonValue;
import com.ctrip.framework.apollo.spring.events.ApolloConfigChangeEvent;
import com.ctrip.framework.apollo.spring.util.SpringInjector;
import com.ctrip.framework.apollo.util.ConfigUtil;
import com.google.gson.Gson;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.gson.GsonBuilder;

import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.CollectionUtils;

/**
 * （完结） 1. 实现配置修改监听接口，最后调用onChange() 2. 监听配置修改事件，最后调用onChange()
 * 解析@Value的占位符，通过反射写回去，实现@Value的热更新（在哪里存进去的??）
 */
public class AutoUpdateConfigChangeListener implements ConfigChangeListener,
    ApplicationListener<ApolloConfigChangeEvent>, ApplicationContextAware {

  private static final Logger logger = LoggerFactory.getLogger(
      AutoUpdateConfigChangeListener.class);

  // 转换方法兼容spring版本的开关
  private final boolean typeConverterHasConvertIfNecessaryWithFieldParameter;
  // 配置文件读取字符串时的类型转换器
  private TypeConverter typeConverter;
  private ConfigurableBeanFactory beanFactory;
  private final PlaceholderHelper placeholderHelper;
  private final SpringValueRegistry springValueRegistry;
  private final SpringConfigurationPropertyRegistry springConfigurationPropertyRegistry;
  private final Map<String, Gson> datePatternGsonMap;
  private final ConfigUtil configUtil;

  public AutoUpdateConfigChangeListener() {
    this.typeConverterHasConvertIfNecessaryWithFieldParameter = testTypeConverterHasConvertIfNecessaryWithFieldParameter();
    this.placeholderHelper = SpringInjector.getInstance(PlaceholderHelper.class);
    this.springValueRegistry = SpringInjector.getInstance(SpringValueRegistry.class);
    this.springConfigurationPropertyRegistry = SpringInjector.getInstance(
        SpringConfigurationPropertyRegistry.class);
    // ?? 数据模式
    this.datePatternGsonMap = new ConcurrentHashMap<>();
    this.configUtil = ApolloInjector.getInstance(ConfigUtil.class);
  }

  /**
   * 解析@Value标记key的占位符，通过反射写回去，实现@Value的热更新
   */
  @Override
  public void onChange(ConfigChangeEvent changeEvent) {
    Set<String> keys = changeEvent.changedKeys();
    if (CollectionUtils.isEmpty(keys)) {
      return;
    }
    Set<String> refreshedPrefix = new HashSet<>();
    for (String key : keys) {
      // 1. check whether the changed key is relevant
      Collection<SpringValue> targetValues = springValueRegistry.get(beanFactory, key);
      if (targetValues != null && !targetValues.isEmpty()) {
        // 2. update the value
        for (SpringValue val : targetValues) {
          updateSpringValue(val);
        }
      }

      // 1. check whether the changed key is relevant
      Map<String, WeakReference<Object>> targetConfigurationProperties = springConfigurationPropertyRegistry.get(
          beanFactory, key);
      if (targetConfigurationProperties != null && !targetConfigurationProperties.isEmpty()) {
        // 2. update the configuration properties
        for (Map.Entry<String, WeakReference<Object>> val : targetConfigurationProperties.entrySet()) {
          String prefix = val.getKey();
          if (!refreshedPrefix.contains(prefix)) {
            refreshConfigurationProperties(beanFactory, val.getValue().get());
          }
          refreshedPrefix.add(prefix);
        }
      }
    }
  }

  private void updateSpringValue(SpringValue springValue) {
    try {
      Object value = resolvePropertyValue(springValue);
      springValue.update(value);
      logger.info("Auto update apollo changed value successfully, new value: {}, {}", value,
          springValue);
    } catch (Throwable ex) {
      logger.error("Auto update apollo changed value failed, {}", springValue.toString(), ex);
    }
  }

  private void refreshConfigurationProperties(BeanFactory beanFactory, Object bean) {
    try {
      springConfigurationPropertyRegistry.refresh(beanFactory, bean);
      logger.info("Auto update apollo changed configuration properties successfully, bean: {}",
          bean.getClass().getName());
    } catch (Throwable ex) {
      logger.error("Auto update apollo changed configuration properties failed, {}",
          bean.getClass().getName(), ex);
    }
  }

  /**
   * 解析@Value的占位符，包括字段、方法等各个地方标注的
   */
  private Object resolvePropertyValue(SpringValue springValue) {
    // 解析占位符value
    Object value = placeholderHelper.resolvePropertyValue(beanFactory, springValue.getBeanName(),
        springValue.getPlaceholder());

    if (springValue.isJson()) {
      // 解析过程 略
      ApolloJsonValue apolloJsonValue =
          springValue.isField() ? springValue.getField().getAnnotation(ApolloJsonValue.class)
              : springValue.getMethodParameter().getMethodAnnotation(ApolloJsonValue.class);
      String datePattern =
          apolloJsonValue != null ? apolloJsonValue.datePattern() : StringUtils.EMPTY;
      value = parseJsonValue((String) value, springValue.getGenericType(), datePattern);
    } else {
      if (springValue.isField()) {
        // 根据spring版本兼容convertIfNecessary()
        if (typeConverterHasConvertIfNecessaryWithFieldParameter) {
          value = this.typeConverter.convertIfNecessary(value, springValue.getTargetType(),
              springValue.getField());
        } else {
          value = this.typeConverter.convertIfNecessary(value, springValue.getTargetType());
        }
      } else {
        value = this.typeConverter.convertIfNecessary(value, springValue.getTargetType(),
            springValue.getMethodParameter());
      }
    }

    return value;
  }

  private Object parseJsonValue(String json, Type targetType, String datePattern) {
    try {
      return datePatternGsonMap.computeIfAbsent(datePattern, this::buildGson)
          .fromJson(json, targetType);
    } catch (Throwable ex) {
      logger.error("Parsing json '{}' to type {} failed!", json, targetType, ex);
      throw ex;
    }
  }

  private Gson buildGson(String datePattern) {
    if (StringUtils.isBlank(datePattern)) {
      return new Gson();
    }
    return new GsonBuilder().setDateFormat(datePattern).create();
  }

  private boolean testTypeConverterHasConvertIfNecessaryWithFieldParameter() {
    try {
      TypeConverter.class.getMethod("convertIfNecessary", Object.class, Class.class, Field.class);
    } catch (Throwable ex) {
      return false;
    }

    return true;
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.beanFactory = ((ConfigurableApplicationContext) applicationContext).getBeanFactory();
    this.typeConverter = this.beanFactory.getTypeConverter();
  }

  @Override
  public void onApplicationEvent(ApolloConfigChangeEvent event) {
    // 是否自动更新配置
    if (!configUtil.isAutoUpdateInjectedSpringPropertiesEnabled()) {
      return;
    }
    this.onChange(event.getConfigChangeEvent());
  }
}
