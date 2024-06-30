package com.ctrip.framework.apollo;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.MetadataReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class tt {

    public static void main(String[] args) {

    }

//    public static void main(String[] args) throws Exception {
//        List<Class<?>> annotatedClasses = findAnnotatedClasses("your.package.name", "YourAnnotation");
//
//        for (Class<?> clazz : annotatedClasses) {
//            System.out.println(clazz.getName() + " is annotated with YourAnnotation");
//        }
//    }
//
//    private static List<Class<?>> findAnnotatedClasses(String packageName, String annotationName) throws IOException {
//        List<Class<?>> annotatedClasses = new ArrayList<>();
//        String packageSearchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + org.springframework.util.ClassUtils.convertClassNameToResourcePath(packageName) + "/**/*.class";
//
//        ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
//        MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();
//
//        Resource[] resources = resourcePatternResolver.getResources(packageSearchPath);
//
//        for (Resource resource : resources) {
//            MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(resource);
//            StandardAnnotationMetadata annotationMetadata = (StandardAnnotationMetadata) metadataReader.getAnnotationMetadata();
//
//            Set<String> annotationTypes = annotationMetadata.getAnnotationTypes();
//            for (String annotationType : annotationTypes) {
//                if (annotationType.equals(annotationName)) {
//                    String className = metadataReader.getClassMetadata().getClassName();
//                    try {
//                        annotatedClasses.add(Class.forName(className));
//                    } catch (ClassNotFoundException e) {
//                        e.printStackTrace();
//                    }
//                    break;
//                }
//            }
//        }
//
//        return annotatedClasses;
//    }

}
