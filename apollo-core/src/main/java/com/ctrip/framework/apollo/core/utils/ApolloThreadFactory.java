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
package com.ctrip.framework.apollo.core.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

// 常见的线程通常
public class ApolloThreadFactory implements ThreadFactory {

  private final AtomicLong threadNumber = new AtomicLong(1);

  private final String namePrefix;

  private final boolean daemon;

  private static final ThreadGroup threadGroup = new ThreadGroup("Apollo");

  public static ThreadFactory create(String namePrefix, boolean daemon) {
    return new ApolloThreadFactory(namePrefix, daemon);
  }

  private ApolloThreadFactory(String namePrefix, boolean daemon) {
    this.namePrefix = namePrefix;
    this.daemon = daemon;
  }

  public Thread newThread(Runnable runnable) {
    Thread thread = new Thread(threadGroup, runnable,//
        threadGroup.getName() + "-" + namePrefix + "-" + threadNumber.getAndIncrement());
    thread.setDaemon(daemon);
    if (thread.getPriority() != Thread.NORM_PRIORITY) {
      thread.setPriority(Thread.NORM_PRIORITY);
    }
    return thread;
  }
}
