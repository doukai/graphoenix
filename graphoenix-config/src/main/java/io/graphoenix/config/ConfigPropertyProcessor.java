package io.graphoenix.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import spoon.pattern.internal.node.StringNode;
import spoon.processing.AbstractAnnotationProcessor;
import spoon.reflect.code.CtAssignment;
import spoon.reflect.code.CtCodeSnippetStatement;
import spoon.reflect.code.CtExpression;
import spoon.reflect.declaration.*;
import spoon.reflect.path.CtRole;
import spoon.support.reflect.declaration.CtFieldImpl;

import java.io.File;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Objects;

import static spoon.reflect.path.CtRole.FIELD;

public class ConfigPropertyProcessor extends AbstractAnnotationProcessor<ConfigProperty, CtElement> {

    private final static String[] configNames = {"application.conf", "application.json", "application.properties", "reference.conf"};
    private final String resourcesPath = System.getProperty("user.dir").concat(File.separator).concat("src").concat(File.separator).concat("main").concat(File.separator).concat("resources").concat(File.separator);

    @Override
    public void init() {
        super.init();
    }

    @Override
    public void process(ConfigProperty annotation, CtElement element) {
        Config config = Arrays.stream(Objects.requireNonNull(new File(resourcesPath).listFiles()))
                .filter(file -> Arrays.asList(configNames).contains(file.getName()))
                .map(ConfigFactory::parseFile)
                .findFirst()
                .orElseThrow();


        CtAnnotation<? extends Annotation> configProperty = element.getAnnotations().stream()
                .filter(ctAnnotation -> ctAnnotation.getName().equals(ConfigProperty.class.getSimpleName()))
                .findFirst()
                .orElseThrow();

        String configKeyName = configProperty.getValueAsString("name");
        String configString = config.getString(configKeyName);

//        String value = String.format("%s = \"%s\"", ((CtFieldImpl<?>) element).getSimpleName(), new StringNode(configString));
//        CtCodeSnippetStatement ctCodeSnippet = getFactory().Core().createCodeSnippetStatement().setValue(value);
//        CtAssignment<Object, Object> assignment = getFactory().Core().createAssignment();
        ((CtField<?>) element).setDefaultExpression(getFactory().Code().createCodeSnippetExpression("test"));


    }
}
