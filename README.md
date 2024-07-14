Apollo Java Client配置汇总
1. apollo.autoUpdateInjectedSpringProperties（true）：是否自动更新配置
2. apollo.property.names.cache.enable（false）：默认不开启配置名缓存，缓存后修改时被重置
3. apollo.override-system-properties（true）：默认Apollo配置源优先级高于systemEnvironment


配置类：
    ConfigService：使用ConfigManager和ConfigRegistry的入口
        ConfigManager：缓存了namespace - Config
        ConfigRegistry：缓存了namespace - ConfigFactory
            ConfigFactory：创建Config、ConfigFile
                Config：对应一个namespace
                ConfigFile：本地测试??
Spring：
    SpringValueRegistry：缓存BeanFactory - key名 -  SpringValue
        SpringValue：本地测试??
    SpringValueDefinitionProcessor：处理xml占位符
        SpringValueDefinition：处理xml占位符，本地测试??
配资源类：
    ConfigPropertySourceFactory：构造、List缓存ConfigPropertySource
        ConfigPropertySource：把Apollo的一个Config对象包装成Spring的PropertySource


问题待办：
1. ApolloBootstrapPropertySources是什么
2. ConfigChangeEvent从哪发出来的
3. ApolloInjector和SpringInjector的区别
4. ConfigFile和Config的区别
5. 关于list和map类型的key怎么解析！！！
6. ConfigurationProperties的map、list需要提前存下来，判断startWith，刷新bean