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

import com.ctrip.framework.apollo.core.utils.ApolloThreadFactory;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.AbstractBindHandler;
import org.springframework.boot.context.properties.bind.BindContext;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Bindable.BindRestriction;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.PropertySourcesPlaceholdersResolver;
import org.springframework.boot.context.properties.bind.handler.IgnoreErrorsBindHandler;
import org.springframework.boot.context.properties.bind.handler.IgnoreTopLevelConverterNotFoundBindHandler;
import org.springframework.boot.context.properties.bind.handler.NoUnboundElementsBindHandler;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.context.properties.source.UnboundElementsSourceFilter;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * ConfigurationProperty注册
 */
public class SpringConfigurationPropertyRegistry {

  private static final Logger logger = LoggerFactory.getLogger(
      SpringConfigurationPropertyRegistry.class);

  private static final long CLEAN_INTERVAL_IN_SECONDS = 5;
  private final Map<BeanFactory, Multimap<String, WeakReference<Object>>> registry = Maps.newConcurrentMap();
  private final Map<BeanFactory, Binder> binderMap = new ConcurrentHashMap<>();
  private final AtomicBoolean initialized = new AtomicBoolean(false);
  private final Object LOCK = new Object();

  // 注册key、value
  public void register(ConfigurableListableBeanFactory beanFactory, String prefix, Object bean,
      ConfigurableEnvironment environment) {
    if (!registry.containsKey(beanFactory)) {
      synchronized (LOCK) {
        // init binder, 参考org.springframework.boot.context.properties.ConfigurationPropertiesBinder、ConfigurationPropertiesBindingPostProcessor的binder创建
        if (!binderMap.containsKey(beanFactory)) {
          Binder binder = new Binder(
              ConfigurationPropertySources.from(environment.getPropertySources()),
              new PropertySourcesPlaceholdersResolver(environment.getPropertySources()),
              beanFactory.getConversionService(), beanFactory::copyRegisteredEditorsTo, null);
          binderMap.put(beanFactory, binder);
        }
        if (!registry.containsKey(beanFactory)) {
          registry.put(beanFactory,
              Multimaps.synchronizedListMultimap(LinkedListMultimap.create()));
        }
      }
    }
    registry.get(beanFactory).put(prefix, new WeakReference<>(bean));
    if (initialized.compareAndSet(false, true)) {
      initialize();
    }
  }

  private void initialize() {
    Executors.newSingleThreadScheduledExecutor(
            ApolloThreadFactory.create("SpringConfigurationPropertyRegistry", true))
        .scheduleAtFixedRate(() -> {
          try {
            scanAndClean();
          } catch (Throwable ex) {
            logger.error(ex.getMessage(), ex);
          }
        }, CLEAN_INTERVAL_IN_SECONDS, CLEAN_INTERVAL_IN_SECONDS, TimeUnit.SECONDS);
  }

  // 遍历所有不可用的value（虚引用为空的）
  private void scanAndClean() {
    Iterator<Multimap<String, WeakReference<Object>>> iterator = registry.values().iterator();
    while (!Thread.currentThread().isInterrupted() && iterator.hasNext()) {
      Multimap<String, WeakReference<Object>> springConfigurationProperties = iterator.next();
      Iterator<Map.Entry<String, WeakReference<Object>>> springConfigurationPropertyIterator = springConfigurationProperties.entries()
          .iterator();
      while (springConfigurationPropertyIterator.hasNext()) {
        Map.Entry<String, WeakReference<Object>> springValue = springConfigurationPropertyIterator.next();
        if (springValue.getValue() == null) {
          springConfigurationPropertyIterator.remove();
        }
      }
    }
  }

  public Map<String, WeakReference<Object>> get(BeanFactory beanFactory, String key) {
    Multimap<String, WeakReference<Object>> prefixMap = registry.get(beanFactory);
    if (prefixMap == null) {
      return null;
    }
    Map<String, WeakReference<Object>> targetBeans = new HashMap<>();
    for (Map.Entry<String, WeakReference<Object>> entry : prefixMap.entries()) {
      if (key.startsWith(entry.getKey())) {
        targetBeans.put(entry.getKey(), entry.getValue());
      }
    }
    return targetBeans;
  }

  public void refresh(BeanFactory beanFactory, Object bean) {
    ConfigurationProperties configurationProperties = bean.getClass()
        .getAnnotation(ConfigurationProperties.class);
    Binder binder = binderMap.get(beanFactory);
    // 暂不支持Validator
    BindHandler handler = getBindHandler(configurationProperties);
    binder.bind(configurationProperties.prefix(), Bindable.ofInstance(bean), handler);
  }

  private <T> BindHandler getBindHandler(ConfigurationProperties annotation) {
    BindHandler handler = new IgnoreTopLevelConverterNotFoundBindHandler();
    handler = new ConfigurationPropertiesBindHandler(handler);
    if (annotation.ignoreInvalidFields()) {
      handler = new IgnoreErrorsBindHandler(handler);
    }
    if (!annotation.ignoreUnknownFields()) {
      UnboundElementsSourceFilter filter = new UnboundElementsSourceFilter();
      handler = new NoUnboundElementsBindHandler(handler, filter);
    }
    return handler;
  }

  private static class ConfigurationPropertiesBindHandler extends AbstractBindHandler {

    ConfigurationPropertiesBindHandler(BindHandler handler) {
      super(handler);
    }

    @Override
    public <T> Bindable<T> onStart(ConfigurationPropertyName name, Bindable<T> target,
        BindContext context) {
      return isConfigurationProperties(target.getType().resolve()) ? target.withBindRestrictions(
          BindRestriction.NO_DIRECT_PROPERTY) : target;
    }

    private boolean isConfigurationProperties(Class<?> target) {
      return target != null && MergedAnnotations.from(target)
          .isPresent(ConfigurationProperties.class);
    }

  }

}
