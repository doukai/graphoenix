package io.graphoenix.graphql.generator.translator;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.graphql.generator.operation.Field;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ElementManager {
    private final IGraphQLDocumentManager manager;

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

    public Optional<? extends VariableElement> getParameterFromExecutableElement(ExecutableElement executableElement, String name) {
        return executableElement.getParameters().stream()
                .filter(parameter -> parameter.getSimpleName().toString().equals(name))
                .findFirst();
    }

    public Optional<String> getVariableName(ExecutableElement executableElement, String name) {
        return executableElement.getParameters().stream()
                .filter(parameter -> parameter.getSimpleName().toString().equals(name))
                .findFirst()
                .map(parameter -> parameter.asType().getKind().equals(TypeKind.ARRAY) ? "in" : "val");
    }
}
