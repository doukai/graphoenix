package io.graphoenix.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValue;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import spoon.processing.AbstractAnnotationProcessor;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtNewArray;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.CtTypeReference;

import java.io.File;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ConfigPropertyProcessor extends AbstractAnnotationProcessor<ConfigProperty, CtField<Object>> {

    private final static String[] configNames = {"application.conf", "application.json", "application.properties", "reference.conf"};
    private final String resourcesPath = System.getProperty("user.dir").concat(File.separator).concat("src").concat(File.separator).concat("main").concat(File.separator).concat("resources").concat(File.separator);
    private CtTypeReference<?> collectionTypeReference;
    private CtTypeReference<?> setTypeReference;
    private CtTypeReference<?> mapTypeReference;
    private CtTypeReference<?> stringTypeReference;

    @Override
    public void init() {
        super.init();
        getEnvironment().setAutoImports(true);
        collectionTypeReference = getFactory().Type().createReference(Collection.class);
        setTypeReference = getFactory().Type().createReference(Set.class);
        mapTypeReference = getFactory().Type().createReference(Map.class);
        stringTypeReference = getFactory().Type().createReference(String.class);
    }

    @Override
    public void process(ConfigProperty annotation, CtField<Object> element) {

        Config config = Arrays.stream(Objects.requireNonNull(new File(resourcesPath).listFiles()))
                .filter(file -> Arrays.asList(configNames).contains(file.getName()))
                .map(ConfigFactory::parseFile)
                .findFirst()
                .orElseThrow();

        CtAnnotation<? extends Annotation> configProperty = element.getAnnotations().stream()
                .filter(ctAnnotation -> ctAnnotation.getName().equals(ConfigProperty.class.getSimpleName()))
                .findFirst()
                .orElseThrow();

        String configKey = configProperty.getValueAsString("name");
        CtTypeReference<Object> type = element.getType();

        element.setDefaultExpression(typeToExpression(type, config, configKey));
    }

    @SuppressWarnings("unchecked")
    private CtExpression<Object> typeToExpression(CtTypeReference<Object> type, Config config, String configKey) {
        if (type.isPrimitive() || type.isSubtypeOf(stringTypeReference)) {
            return getFactory().createCodeSnippetExpression(config.hasPath(configKey) ? config.getValue(configKey).render() : "null");
        } else if (type.isArray()) {
            CtNewArray<Object> newArray = getFactory().createNewArray();
            return newArray.setElements(
                    config.getList(configKey).stream()
                            .map(configValue -> getFactory().createCodeSnippetExpression(configValue.render()))
                            .collect(Collectors.toList())
            );
        } else if (type.isSubtypeOf(collectionTypeReference)) {
            String valueList = config.getList(configKey).stream()
                    .map(ConfigValue::render)
                    .collect(Collectors.joining(","));
            if (type.isSubtypeOf(setTypeReference)) {
                return getFactory().createCodeSnippetExpression("Set.of(" + valueList + ")");
            } else {
                return getFactory().createCodeSnippetExpression("List.of(" + valueList + ")");
            }
        } else if (type.isSubtypeOf(mapTypeReference)) {
            String valueList = config.getObject(configKey).entrySet().stream()
                    .flatMap(entry -> Stream.of("\"" + entry.getKey() + "\"", entry.getValue().render()))
                    .collect(Collectors.joining(","));
            return getFactory().createCodeSnippetExpression("Map.of(" + valueList + ")");
        } else if (type.isClass()) {
            CtExpression<Object>[] ctExpressions = type.getAllFields().stream()
                    .map(ctFieldReference -> typeToExpression((CtTypeReference<Object>) ctFieldReference.getType(), config, configKey.concat(".").concat(ctFieldReference.getSimpleName())))
                    .toArray(CtExpression[]::new);
            return getFactory().createConstructorCall(type, ctExpressions);
        }
        return null;
    }
}
