package io.graphoenix.graphql.generator.translator;

import io.graphoenix.core.error.ElementProcessException;
import io.graphoenix.core.document.Directive;
import io.graphoenix.core.document.Field;
import io.graphoenix.core.document.InputValue;
import io.graphoenix.core.operation.Argument;
import io.graphoenix.core.operation.ArrayValueWithVariable;
import io.graphoenix.core.operation.StringValue;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.graphql.Source;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.util.Types;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.graphoenix.core.error.ElementProcessErrorType.SOURCE_ANNOTATION_NOT_EXIST;

@ApplicationScoped
public class GraphQLApiBuilder {

    private final ElementManager elementManager;

    @Inject
    public GraphQLApiBuilder(ElementManager elementManager) {
        this.elementManager = elementManager;
    }

    public Field variableElementToField(ExecutableElement executableElement, Types typeUtils) {
        Field field = new Field()
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
                                .addArgument(
                                        new Argument()
                                                .setName("parameters")
                                                .setValueWithVariable(
                                                        new ArrayValueWithVariable(
                                                                executableElement.getParameters().stream()
                                                                        .map(parameter -> Map.of("name", parameter.getSimpleName().toString(), "className", parameter.asType().toString()))
                                                                        .collect(Collectors.toList()),
                                                                true
                                                        )
                                                )
                                )
                                .addArgument(
                                        new Argument()
                                                .setName("returnClassName")
                                                .setValueWithVariable(new StringValue(executableElement.getReturnType().toString()))
                                )
                );
        if (executableElement.getAnnotation(PermitAll.class) != null) {
            field.addStringDirective("permitAll");
        }
        if (executableElement.getAnnotation(DenyAll.class) != null) {
            field.addStringDirective("denyAll");
        }
        if (executableElement.getAnnotation(RolesAllowed.class) != null) {
            Directive directive = new Directive()
                    .setName("rolesAllowed")
                    .addArgument(new Argument().setName("roles").setValueWithVariable(executableElement.getAnnotation(RolesAllowed.class).value()));
            field.addDirective(directive);
        }
        return field;
    }

    public Tuple2<String, Field> variableElementToObjectField(ExecutableElement executableElement, Types typeUtils) {
        return Tuple.of(
                elementManager.variableElementToTypeName(
                        executableElement.getParameters().stream()
                                .filter(variableElement -> variableElement.getAnnotation(Source.class) != null)
                                .findFirst()
                                .orElseThrow(() -> new ElementProcessException(SOURCE_ANNOTATION_NOT_EXIST.bind(executableElement.getSimpleName()))),
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
                                                .addArgument(
                                                        new Argument()
                                                                .setName("parameters")
                                                                .setValueWithVariable(
                                                                        new ArrayValueWithVariable(
                                                                                executableElement.getParameters().stream()
                                                                                        .map(parameter -> Map.of("name", parameter.getSimpleName().toString(), "className", parameter.asType().toString()))
                                                                                        .collect(Collectors.toList()),
                                                                                true
                                                                        )
                                                                )
                                                )
                                                .addArgument(
                                                        new Argument()
                                                                .setName("returnClassName")
                                                                .setValueWithVariable(new StringValue(executableElement.getReturnType().toString()))
                                                )
                                        )
                                        .map(Directive::toString)
                                        .collect(Collectors.toCollection(LinkedHashSet::new))
                        )
        );
    }
}
