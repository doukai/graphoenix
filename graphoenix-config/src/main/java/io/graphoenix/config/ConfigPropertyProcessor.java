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
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import static spoon.reflect.path.CtRole.DEFAULT_EXPRESSION;

public class ConfigPropertyProcessor extends AbstractAnnotationProcessor<ConfigProperty, CtField<?>> {

    private final static String[] configNames = {"application.conf", "application.json", "application.properties", "reference.conf"};
    private final String resourcesPath = System.getProperty("user.dir").concat(File.separator).concat("src").concat(File.separator).concat("main").concat(File.separator).concat("resources").concat(File.separator);

    @Override
    public void init() {
        super.init();
    }

    @Override
    public void process(ConfigProperty annotation, CtField<?> element) {
        getEnvironment().setAutoImports(true);

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
        CtTypeReference<?> type = element.getType();

        element.setDefaultExpression(typeToExpression(type, config, configKey));
    }

    private CtExpression<? extends CtVariable<?>> typeToExpression(CtTypeReference<?> type, Config config, String configKey) {
        if (type.isPrimitive()) {
            return getFactory().Code().createCodeSnippetExpression(config.getValue(configKey).render());
        } else if (type.isArray()) {
            CtNewArray<Object> newArray = getFactory().createNewArray();
            newArray.setElements(
                    config.getList(configKey).stream()
                            .map(configValue -> getFactory().Code().createCodeSnippetExpression(configValue.render()))
                            .collect(Collectors.toList())
            );
            return newArray.getValueByRole(DEFAULT_EXPRESSION);
        }
        return null;
    }
}
