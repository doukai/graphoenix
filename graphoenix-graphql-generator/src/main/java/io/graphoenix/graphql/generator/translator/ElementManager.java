package io.graphoenix.graphql.generator.translator;

import io.graphoenix.core.operation.Field;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.graphoenix.core.utils.DocumentUtil.DOCUMENT_UTIL;
import static io.graphoenix.spi.constant.Hammurabi.AGGREGATE_SUFFIX;

@ApplicationScoped
public class ElementManager {
    private final IGraphQLDocumentManager manager;

    @Inject
    public ElementManager(IGraphQLDocumentManager manager) {
        this.manager = manager;
    }

    public Set<Field> buildFields(String typeName, int level, int layers) {
        return manager.getFields(typeName)
                .filter(manager::isNotInvokeField)
                .filter(manager::isNotFetchField)
                .filter(manager::isNotFunctionField)
                .filter(manager::isNotConnectionField)
                .filter(fieldDefinitionContext -> !fieldDefinitionContext.name().getText().endsWith(AGGREGATE_SUFFIX))
                .flatMap(fieldDefinitionContext -> {
                            String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
                            if (manager.isObject(fieldTypeName)) {
                                if (level < layers) {
                                    return Stream.of(
                                            new Field(fieldDefinitionContext.name().getText())
                                                    .setFields(buildFields(fieldTypeName, level + 1, layers))
                                    );
                                } else {
                                    return Stream.empty();
                                }
                            } else {
                                return Stream.of(new Field(fieldDefinitionContext.name().getText()));
                            }
                        }
                )
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public Set<Field> buildFields(String selectionSet) {
        return DOCUMENT_UTIL.graphqlToSelectionSet(selectionSet).selection().stream().map(Field::new).collect(Collectors.toSet());
    }
}
