package io.graphoenix.graphql.generator.translator;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.graphql.generator.operation.Field;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;

import javax.inject.Inject;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ElementManager {
    private final IGraphQLDocumentManager manager;

    @Inject
    public ElementManager(IGraphQLDocumentManager manager) {
        this.manager = manager;
    }

    public List<Field> buildFields(String typeName, int level, int layers) {
        return getFields(typeName, level, layers)
                .map(fieldDefinitionContext ->
                        {
                            Field field = new Field().setName(fieldDefinitionContext.name().getText());
                            if (level <= layers) {
                                field.setFields(buildFields(manager.getFieldTypeName(fieldDefinitionContext.type()), level + 1, layers));
                            }
                            return field;
                        }
                )
                .collect(Collectors.toList());
    }

    public Stream<GraphqlParser.FieldDefinitionContext> getFields(String typeName, int level, int layers) {
        return manager.getFields(typeName)
                .filter(fieldDefinitionContext -> level < layers ||
                        level == layers && (manager.isScaLar(manager.getFieldTypeName(fieldDefinitionContext.type())) || manager.isEnum(manager.getFieldTypeName(fieldDefinitionContext.type())))
                );
    }

    public VariableElement getParameterFromExecutableElement(ExecutableElement executableElement, String name) {
        return executableElement.getParameters().stream()
                .filter(parameter -> parameter.getSimpleName().toString().equals(name))
                .findFirst()
                .orElseThrow();
    }
}
