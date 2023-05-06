package io.graphoenix.core.handler;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.core.operation.Field;
import io.graphoenix.core.operation.Operation;
import io.graphoenix.core.utils.DocumentUtil;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.graphoenix.core.error.GraphQLErrorType.*;

@ApplicationScoped
public class GraphQLFetchFieldProcessor {

    private final IGraphQLDocumentManager manager;

    @Inject
    public GraphQLFetchFieldProcessor(IGraphQLDocumentManager manager) {
        this.manager = manager;
    }

    public GraphqlParser.OperationDefinitionContext buildFetchFields(GraphqlParser.OperationDefinitionContext operationDefinitionContext) {
        return buildFetchFields(new Operation(operationDefinitionContext));
    }

    public GraphqlParser.OperationDefinitionContext buildFetchFields(Operation operation) {
        GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext;
        if (operation.getOperationType() == null || operation.getOperationType().equals("query")) {
            objectTypeDefinitionContext = manager.getQueryOperationTypeName().flatMap(manager::getObject).orElseThrow(() -> new GraphQLErrors(QUERY_TYPE_NOT_EXIST));
        } else if (operation.getOperationType().equals("mutation")) {
            objectTypeDefinitionContext = manager.getMutationOperationTypeName().flatMap(manager::getObject).orElseThrow(() -> new GraphQLErrors(MUTATION_TYPE_NOT_EXIST));
        } else {
            throw new GraphQLErrors(UNSUPPORTED_OPERATION_TYPE.bind(operation.getOperationType()));
        }
        operation.getFields()
                .forEach(field -> {
                            GraphqlParser.FieldDefinitionContext fieldDefinitionContext = objectTypeDefinitionContext
                                    .fieldsDefinition().fieldDefinition().stream()
                                    .filter(subFieldDefinitionContext -> subFieldDefinitionContext.name().getText().equals(field.getName()))
                                    .findFirst()
                                    .orElseThrow(() -> new GraphQLErrors(FIELD_NOT_EXIST.bind(objectTypeDefinitionContext.name().getText(), field.getName())));
                            processFetchSelection(fieldDefinitionContext, field);
                            if (operation.getOperationType().equals("mutation")) {
                                processFetchArgument(fieldDefinitionContext, field);
                            }
                        }
                );
        return DocumentUtil.DOCUMENT_UTIL.graphqlToOperation(operation.toString());
    }

    private void processFetchSelection(GraphqlParser.FieldDefinitionContext fieldDefinitionContext, Field field) {
        String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
        if (field != null && field.getFields() != null) {
            List<Field> fromFieldSelectionList = field.getFields().stream()
                    .map(subField -> manager.getField(fieldTypeName, subField.getName()))
                    .flatMap(Optional::stream)
                    .filter(manager::isFetchField)
                    .map(manager::getFetchFrom)
                    .map(fromFieldName -> manager.getField(fieldTypeName, fromFieldName))
                    .flatMap(Optional::stream)
                    .filter(fromFieldDefinitionContext -> field.getFields().stream().noneMatch(subField -> subField.getName().equals(fromFieldDefinitionContext.name().getText())))
                    .map(fromFieldDefinitionContext -> new Field(fromFieldDefinitionContext.name().getText()))
                    .collect(Collectors.toList());

            List<Field> withObjectFieldSelectionList = field.getFields().stream()
                    .flatMap(subField ->
                            manager.getField(fieldTypeName, subField.getName())
                                    .filter(manager::isFetchField)
                                    .filter(manager::hasFetchWith)
                                    .flatMap(fetchFieldDefinitionContext ->
                                            manager.getObject(fieldTypeName)
                                                    .map(objectTypeDefinitionContext -> manager.getFetchWithObjectField(objectTypeDefinitionContext, fetchFieldDefinitionContext))
                                                    .map(withObjectFieldDefinitionContext ->
                                                            new Field(withObjectFieldDefinitionContext.name().getText())
                                                                    .addField(
                                                                            new Field(manager.getFetchWithToObjectField(fetchFieldDefinitionContext).name().getText())
                                                                                    .setFields(subField.getFields())
                                                                    )
                                                    )
                                    )
                                    .stream()
                    )
                    .collect(Collectors.toList());

            if (fromFieldSelectionList.size() > 0) {
                field.addFields(fromFieldSelectionList);
            }

            if (withObjectFieldSelectionList.size() > 0) {
                field.addFields(withObjectFieldSelectionList);
            }

            field.getFields()
                    .forEach(subField ->
                            processFetchSelection(
                                    manager.getField(manager.getFieldTypeName(fieldDefinitionContext.type()), subField.getName())
                                            .orElseThrow(() -> new GraphQLErrors(FIELD_NOT_EXIST.bind(manager.getFieldTypeName(fieldDefinitionContext.type()), subField.getName()))),
                                    subField
                            )
                    );
        }
    }

    private void processFetchArgument(GraphqlParser.FieldDefinitionContext fieldDefinitionContext, Field field) {
        String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
        if (field != null && field.getFields() != null && field.getArguments() != null) {
            List<Field> fromFieldSelectionList = field.getArguments().keySet().stream()
                    .map(argumentName -> manager.getField(fieldTypeName, argumentName))
                    .flatMap(Optional::stream)
                    .filter(manager::isFetchField)
                    .filter(fetchFieldDefinitionContext -> !manager.getFetchAnchor(fetchFieldDefinitionContext))
                    .map(manager::getFetchFrom)
                    .map(fromFieldName -> manager.getField(fieldTypeName, fromFieldName))
                    .flatMap(Optional::stream)
                    .filter(fromFieldDefinitionContext -> field.getFields().stream().noneMatch(subField -> subField.getName().equals(fromFieldDefinitionContext.name().getText())))
                    .map(fromFieldDefinitionContext -> new Field(fromFieldDefinitionContext.name().getText()))
                    .collect(Collectors.toList());

            if (fromFieldSelectionList.size() > 0) {
                field.addFields(fromFieldSelectionList);
            }

            field.getFields()
                    .forEach(subField ->
                            processFetchArgument(
                                    manager.getField(manager.getFieldTypeName(fieldDefinitionContext.type()), subField.getName())
                                            .orElseThrow(() -> new GraphQLErrors(FIELD_NOT_EXIST.bind(manager.getFieldTypeName(fieldDefinitionContext.type()), subField.getName()))),
                                    subField
                            )
                    );
        }
    }
}
