package io.graphoenix.graphql.generator.translator;

import io.graphoenix.core.error.ElementProblem;
import io.graphoenix.graphql.generator.document.Directive;
import io.graphoenix.graphql.generator.document.Field;
import io.graphoenix.graphql.generator.document.InputValue;
import io.graphoenix.graphql.generator.operation.Argument;
import io.graphoenix.graphql.generator.operation.StringValue;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.graphql.Source;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.util.Types;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.graphoenix.core.error.ElementErrorType.SOURCE_ANNOTATION_NOT_EXIST;

@ApplicationScoped
public class GraphQLApiBuilder {

    private final ElementManager elementManager;

    @Inject
    public GraphQLApiBuilder(ElementManager elementManager) {
        this.elementManager = elementManager;
    }

    public Field variableElementToField(ExecutableElement executableElement, Types typeUtils) {
        return new Field()
                .setName(elementManager.getNameFromElement(executableElement))
                .setDescription(elementManager.getDescriptionFromElement(executableElement))
                .setTypeName(elementManager.executableElementToTypeName(executableElement, typeUtils))
                .setArguments(elementManager.executableElementParametersToInputValues(executableElement, typeUtils))
                .setArguments(
                        executableElement.getParameters().stream()
                                .map(variableElement ->
                                        new InputValue()
                                                .setName(elementManager.getNameFromElement(variableElement))
                                                .setDescription(elementManager.getDescriptionFromElement(variableElement))
                                                .setDefaultValue(elementManager.getDefaultValueFromElement(variableElement))
                                                .setTypeName(elementManager.variableElementToInputTypeName(variableElement, typeUtils))
                                )
                                .collect(Collectors.toCollection(LinkedHashSet::new))
                )
                .addDirective(
                        new Directive()
                                .setName("invoke")
                                .addArgument(
                                        new Argument()
                                                .setName("className")
                                                .setValueWithVariable(new StringValue(executableElement.getEnclosingElement().toString()))
                                )
                                .addArgument(
                                        new Argument()
                                                .setName("methodName")
                                                .setValueWithVariable(new StringValue(executableElement.getSimpleName().toString()))
                                )
                );
    }

    public Tuple2<String, Field> variableElementToObjectField(ExecutableElement executableElement, Types typeUtils) {
        return Tuple.of(
                elementManager.variableElementToTypeName(
                        executableElement.getParameters().stream()
                                .filter(variableElement -> variableElement.getAnnotation(Source.class) != null)
                                .findFirst()
                                .orElseThrow(() -> new ElementProblem(SOURCE_ANNOTATION_NOT_EXIST.bind(executableElement.getSimpleName()))),
                        typeUtils
                ),
                new Field()
                        .setName(elementManager.getNameFromElement(executableElement))
                        .setDescription(elementManager.getDescriptionFromElement(executableElement))
                        .setTypeName(elementManager.executableElementToTypeName(executableElement, typeUtils))
                        .setArguments(elementManager.executableElementParametersToInputValues(executableElement, typeUtils))
                        .setStringDirectives(
                                Stream.of(new Directive()
                                                .setName("invoke")
                                                .addArgument(
                                                        new Argument()
                                                                .setName("className")
                                                                .setValueWithVariable(new StringValue(executableElement.getEnclosingElement().toString()))
                                                )
                                                .addArgument(
                                                        new Argument()
                                                                .setName("methodName")
                                                                .setValueWithVariable(new StringValue(executableElement.getSimpleName().toString()))
                                                )
                                        )
                                        .map(Directive::toString)
                                        .collect(Collectors.toCollection(LinkedHashSet::new))
                        )
        );
    }
}
