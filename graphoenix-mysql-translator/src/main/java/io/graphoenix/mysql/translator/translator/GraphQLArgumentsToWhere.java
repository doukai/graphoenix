package io.graphoenix.mysql.translator.translator;

import com.google.common.base.CharMatcher;
import com.google.common.collect.Lists;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.mysql.translator.expression.JsonTable;
import io.graphoenix.mysql.translator.utils.DBNameUtil;
import io.graphoenix.mysql.translator.utils.DBValueUtil;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.antlr.IGraphQLFieldMapManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.DoubleValue;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.JdbcNamedParameter;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NotExpression;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExistsExpression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.IsBooleanExpression;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.expression.operators.relational.LikeExpression;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.create.table.ColDataType;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SubSelect;
import net.sf.jsqlparser.util.cnfexpression.MultiAndExpression;
import net.sf.jsqlparser.util.cnfexpression.MultiOrExpression;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.graphoenix.core.error.GraphQLErrorType.FIELD_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.MAP_FROM_FIELD_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.MAP_TO_FIELD_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.MAP_WITH_FROM_FIELD_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.MAP_WITH_TO_FIELD_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.MAP_WITH_TYPE_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.NON_NULL_VALUE_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.TYPE_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.UNSUPPORTED_FIELD_TYPE;
import static io.graphoenix.core.error.GraphQLErrorType.UNSUPPORTED_OPERATOR;
import static io.graphoenix.core.error.GraphQLErrorType.UNSUPPORTED_VALUE;
import static io.graphoenix.spi.constant.Hammurabi.AFTER_INPUT_NAME;
import static io.graphoenix.spi.constant.Hammurabi.BEFORE_INPUT_NAME;
import static io.graphoenix.spi.constant.Hammurabi.DEPRECATED_FIELD_NAME;
import static io.graphoenix.spi.constant.Hammurabi.DEPRECATED_INPUT_NAME;
import static io.graphoenix.spi.constant.Hammurabi.FIRST_INPUT_NAME;
import static io.graphoenix.spi.constant.Hammurabi.GROUP_BY_INPUT_NAME;
import static io.graphoenix.spi.constant.Hammurabi.LAST_INPUT_NAME;
import static io.graphoenix.spi.constant.Hammurabi.LIST_INPUT_NAME;
import static io.graphoenix.spi.constant.Hammurabi.OFFSET_INPUT_NAME;
import static io.graphoenix.spi.constant.Hammurabi.ORDER_BY_INPUT_NAME;
import static io.graphoenix.spi.constant.Hammurabi.SORT_INPUT_NAME;
import static io.graphoenix.spi.constant.Hammurabi.WHERE_INPUT_NAME;

@ApplicationScoped
public class GraphQLArgumentsToWhere {

    private final IGraphQLDocumentManager manager;
    private final IGraphQLFieldMapManager mapper;
    private final DBNameUtil dbNameUtil;
    private final DBValueUtil dbValueUtil;
    private final String[] EXCLUDE_INPUT = {DEPRECATED_INPUT_NAME, FIRST_INPUT_NAME, LAST_INPUT_NAME, OFFSET_INPUT_NAME, AFTER_INPUT_NAME, BEFORE_INPUT_NAME, GROUP_BY_INPUT_NAME, ORDER_BY_INPUT_NAME, SORT_INPUT_NAME, LIST_INPUT_NAME};

    @Inject
    public GraphQLArgumentsToWhere(IGraphQLDocumentManager manager, IGraphQLFieldMapManager mapper, DBNameUtil dbNameUtil, DBValueUtil dbValueUtil) {
        this.manager = manager;
        this.mapper = mapper;
        this.dbNameUtil = dbNameUtil;
        this.dbValueUtil = dbValueUtil;
    }

    protected Optional<Expression> argumentsToMultipleExpression(GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                 GraphqlParser.ArgumentsContext argumentsContext,
                                                                 int level) {

        if (argumentsContext == null) {
            return Optional.of(isFalseExpression(dbNameUtil.fieldToColumn(manager.getFieldTypeName(fieldDefinitionContext.type()), DEPRECATED_FIELD_NAME, level)));
        }
        Stream<Expression> expressionStream = argumentsToExpressionList(fieldDefinitionContext.type(), fieldDefinitionContext.argumentsDefinition(), argumentsContext, level);
        Optional<Expression> multipleExpression = expressionStreamToMultipleExpression(expressionStream, hasOrConditional(argumentsContext, fieldDefinitionContext.argumentsDefinition()));
        Optional<Expression> notDeprecatedExpression = notDeprecatedExpression(fieldDefinitionContext.type(), fieldDefinitionContext.argumentsDefinition().inputValueDefinition(), argumentsContext, level);
        if (multipleExpression.isPresent() && notDeprecatedExpression.isPresent()) {
            return Optional.of(new MultiAndExpression(Arrays.asList(multipleExpression.get(), notDeprecatedExpression.get())));
        } else if (multipleExpression.isPresent()) {
            return multipleExpression;
        } else {
            return notDeprecatedExpression;
        }
    }

    protected Optional<Expression> objectValueWithVariableToMultipleExpression(GraphqlParser.TypeContext typeContext,
                                                                               GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                                               GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext,
                                                                               int level) {
        Optional<GraphqlParser.InputObjectTypeDefinitionContext> inputObjectTypeDefinitionContext = manager.getInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type()));
        if (inputObjectTypeDefinitionContext.isPresent()) {
            Stream<Expression> expressionStream = objectValueWithVariableToExpressionList(typeContext, inputObjectTypeDefinitionContext.get().inputObjectValueDefinitions(), objectValueWithVariableContext, level);
            Optional<Expression> multipleExpression = expressionStreamToMultipleExpression(expressionStream, hasOrConditional(objectValueWithVariableContext, inputObjectTypeDefinitionContext.get()));
            Optional<Expression> notDeprecatedExpression = notDeprecatedExpression(typeContext, inputObjectTypeDefinitionContext.get().inputObjectValueDefinitions().inputValueDefinition(), objectValueWithVariableContext, level);
            if (multipleExpression.isPresent() && notDeprecatedExpression.isPresent()) {
                return Optional.of(new MultiAndExpression(Arrays.asList(multipleExpression.get(), notDeprecatedExpression.get())));
            } else if (multipleExpression.isPresent()) {
                return multipleExpression;
            } else {
                return notDeprecatedExpression;
            }
        } else {
            return Optional.of(isFalseExpression(dbNameUtil.fieldToColumn(manager.getFieldTypeName(typeContext), DEPRECATED_FIELD_NAME, level)));
        }
    }

    protected Optional<Expression> objectValueToMultipleExpression(GraphqlParser.TypeContext typeContext,
                                                                   GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                                   GraphqlParser.ObjectValueContext objectValueContext,
                                                                   int level) {
        Optional<GraphqlParser.InputObjectTypeDefinitionContext> inputObjectTypeDefinitionContext = manager.getInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type()));
        if (inputObjectTypeDefinitionContext.isPresent()) {
            Stream<Expression> expressionStream = objectValueToExpressionList(typeContext, inputObjectTypeDefinitionContext.get().inputObjectValueDefinitions(), objectValueContext, level);
            Optional<Expression> multipleExpression = expressionStreamToMultipleExpression(expressionStream, hasOrConditional(objectValueContext, inputObjectTypeDefinitionContext.get()));
            Optional<Expression> notDeprecatedExpression = notDeprecatedExpression(typeContext, inputObjectTypeDefinitionContext.get().inputObjectValueDefinitions().inputValueDefinition(), objectValueContext, level);
            if (multipleExpression.isPresent() && notDeprecatedExpression.isPresent()) {
                return Optional.of(new MultiAndExpression(Arrays.asList(multipleExpression.get(), notDeprecatedExpression.get())));
            } else if (multipleExpression.isPresent()) {
                return multipleExpression;
            } else {
                return notDeprecatedExpression;
            }
        } else {
            return Optional.of(isFalseExpression(dbNameUtil.fieldToColumn(manager.getFieldTypeName(typeContext), DEPRECATED_FIELD_NAME, level)));
        }
    }

    protected Optional<Expression> expressionStreamToMultipleExpression(Stream<Expression> expressionStream, boolean hasOrConditional) {
        List<Expression> expressionList = expressionStream.collect(Collectors.toList());
        if (expressionList.size() == 0) {
            return Optional.empty();
        } else if (expressionList.size() == 1) {
            return Optional.of(expressionList.get(0));
        } else {
            if (hasOrConditional) {
                return Optional.of(new MultiOrExpression(expressionList));
            } else {
                return Optional.of(new MultiAndExpression(expressionList));
            }
        }
    }

    protected Stream<Expression> argumentsToExpressionList(GraphqlParser.TypeContext typeContext,
                                                           GraphqlParser.ArgumentsDefinitionContext argumentsDefinitionContext,
                                                           GraphqlParser.ArgumentsContext argumentsContext,
                                                           int level) {
        Stream<Expression> expressionStream = Stream.ofNullable(argumentsDefinitionContext).map(GraphqlParser.ArgumentsDefinitionContext::inputValueDefinition).flatMap(Collection::stream)
                .filter(this::isNotConditional)
                .filter(inputValueDefinitionContext -> !inputValueDefinitionContext.name().getText().equals(DEPRECATED_FIELD_NAME))
                .filter(inputValueDefinitionContext -> Arrays.stream(EXCLUDE_INPUT).noneMatch(inputName -> inputName.equals(inputValueDefinitionContext.name().getText())))
                .map(inputValueDefinitionContext -> argumentsToExpression(typeContext, inputValueDefinitionContext, argumentsContext, level))
                .flatMap(Optional::stream);
        Stream<Expression> conditionalExpressionStream = listTypeConditionalFieldOfArgumentsToExpressionList(typeContext, argumentsDefinitionContext, argumentsContext, level);
        return Stream.concat(expressionStream, conditionalExpressionStream);
    }

    protected Stream<Expression> objectValueWithVariableToExpressionList(GraphqlParser.TypeContext typeContext,
                                                                         GraphqlParser.InputObjectValueDefinitionsContext inputObjectValueDefinitionsContext,
                                                                         GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext,
                                                                         int level) {
        Stream<Expression> expressionStream = inputObjectValueDefinitionsContext.inputValueDefinition().stream()
                .filter(this::isNotConditional)
                .filter(inputValueDefinitionContext -> !inputValueDefinitionContext.name().getText().equals(DEPRECATED_FIELD_NAME))
                .filter(inputValueDefinitionContext -> Arrays.stream(EXCLUDE_INPUT).noneMatch(inputName -> inputName.equals(inputValueDefinitionContext.name().getText())))
                .map(inputValueDefinitionContext -> objectValueWithVariableToExpression(typeContext, inputValueDefinitionContext, objectValueWithVariableContext, level))
                .flatMap(Optional::stream);
        Stream<Expression> conditionalExpressionStream = listTypeConditionalFieldOfObjectValueWithVariableToExpressionList(typeContext, inputObjectValueDefinitionsContext, objectValueWithVariableContext, level);
        return Stream.concat(expressionStream, conditionalExpressionStream);
    }

    protected Stream<Expression> objectValueToExpressionList(GraphqlParser.TypeContext typeContext,
                                                             GraphqlParser.InputObjectValueDefinitionsContext inputObjectValueDefinitionsContext,
                                                             GraphqlParser.ObjectValueContext objectValueContext,
                                                             int level) {
        Stream<Expression> expressionStream = inputObjectValueDefinitionsContext.inputValueDefinition().stream()
                .filter(this::isNotConditional)
                .filter(inputValueDefinitionContext -> !inputValueDefinitionContext.name().getText().equals(DEPRECATED_FIELD_NAME))
                .filter(inputValueDefinitionContext -> Arrays.stream(EXCLUDE_INPUT).noneMatch(inputName -> inputName.equals(inputValueDefinitionContext.name().getText())))
                .map(inputValueDefinitionContext -> objectValueToExpression(typeContext, inputValueDefinitionContext, objectValueContext, level))
                .flatMap(Optional::stream);
        Stream<Expression> conditionalExpressionStream = listTypeConditionalFieldOfObjectValueToExpression(typeContext, inputObjectValueDefinitionsContext, objectValueContext, level);
        return Stream.concat(expressionStream, conditionalExpressionStream);
    }

    protected Optional<Expression> notDeprecatedExpression(GraphqlParser.TypeContext typeContext,
                                                           List<GraphqlParser.InputValueDefinitionContext> inputValueDefinitionContextList,
                                                           GraphqlParser.ArgumentsContext argumentsContext,
                                                           int level) {
        Optional<GraphqlParser.ArgumentContext> argumentContext = inputValueDefinitionContextList.stream()
                .filter(inputValueDefinitionContext -> inputValueDefinitionContext.name().getText().equals(DEPRECATED_INPUT_NAME))
                .filter(inputValueDefinitionContext -> manager.getFieldTypeName(inputValueDefinitionContext.type()).equals("Boolean"))
                .findFirst()
                .flatMap(inputValueDefinitionContext -> manager.getArgumentFromInputValueDefinition(argumentsContext, inputValueDefinitionContext));
        if (argumentContext.isPresent() && argumentContext.get().valueWithVariable().BooleanValue() != null) {
            if (argumentContext.get().valueWithVariable().BooleanValue().getText().equals("true")) {
                return Optional.empty();
            }
        }
        return Optional.of(isFalseExpression(dbNameUtil.fieldToColumn(manager.getFieldTypeName(typeContext), DEPRECATED_FIELD_NAME, level)));
    }

    protected Optional<Expression> notDeprecatedExpression(GraphqlParser.TypeContext typeContext,
                                                           List<GraphqlParser.InputValueDefinitionContext> inputValueDefinitionContextList,
                                                           GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext,
                                                           int level) {
        Optional<GraphqlParser.ObjectFieldWithVariableContext> objectFieldWithVariableContext = inputValueDefinitionContextList.stream()
                .filter(inputValueDefinitionContext -> inputValueDefinitionContext.name().getText().equals(DEPRECATED_INPUT_NAME))
                .filter(inputValueDefinitionContext -> manager.getFieldTypeName(inputValueDefinitionContext.type()).equals("Boolean"))
                .findFirst()
                .flatMap(inputValueDefinitionContext -> manager.getObjectFieldWithVariableFromInputValueDefinition(objectValueWithVariableContext, inputValueDefinitionContext));
        if (objectFieldWithVariableContext.isPresent() && objectFieldWithVariableContext.get().valueWithVariable().BooleanValue() != null) {
            if (objectFieldWithVariableContext.get().valueWithVariable().BooleanValue().getText().equals("true")) {
                return Optional.empty();
            }
        }
        return Optional.of(isFalseExpression(dbNameUtil.fieldToColumn(manager.getFieldTypeName(typeContext), DEPRECATED_FIELD_NAME, level)));
    }

    protected Optional<Expression> notDeprecatedExpression(GraphqlParser.TypeContext typeContext,
                                                           List<GraphqlParser.InputValueDefinitionContext> inputValueDefinitionContextList,
                                                           GraphqlParser.ObjectValueContext objectValueContext,
                                                           int level) {
        Optional<GraphqlParser.ObjectFieldContext> objectFieldContext = inputValueDefinitionContextList.stream()
                .filter(inputValueDefinitionContext -> inputValueDefinitionContext.name().getText().equals(DEPRECATED_INPUT_NAME))
                .filter(inputValueDefinitionContext -> manager.getFieldTypeName(inputValueDefinitionContext.type()).equals("Boolean"))
                .findFirst()
                .flatMap(inputValueDefinitionContext -> manager.getObjectFieldFromInputValueDefinition(objectValueContext, inputValueDefinitionContext));
        if (objectFieldContext.isPresent() && objectFieldContext.get().value().BooleanValue() != null) {
            if (objectFieldContext.get().value().BooleanValue().getText().equals("true")) {
                return Optional.empty();
            }
        }
        return Optional.of(isFalseExpression(dbNameUtil.fieldToColumn(manager.getFieldTypeName(typeContext), DEPRECATED_FIELD_NAME, level)));
    }


    protected Optional<Expression> argumentsToExpression(GraphqlParser.TypeContext typeContext,
                                                         GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                         GraphqlParser.ArgumentsContext argumentsContext,
                                                         int level) {
        Optional<GraphqlParser.ArgumentContext> argumentContext = manager.getArgumentFromInputValueDefinition(argumentsContext, inputValueDefinitionContext);
        if (argumentContext.isPresent()) {
            return argumentToExpression(typeContext, inputValueDefinitionContext, argumentContext.get(), level);
        } else {
            return defaultValueToExpression(typeContext, inputValueDefinitionContext, level);
        }
    }

    protected Optional<Expression> objectValueWithVariableToExpression(GraphqlParser.TypeContext typeContext,
                                                                       GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                                       GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext,
                                                                       int level) {
        Optional<GraphqlParser.ObjectFieldWithVariableContext> objectFieldWithVariableContext = manager.getObjectFieldWithVariableFromInputValueDefinition(objectValueWithVariableContext, inputValueDefinitionContext);
        if (objectFieldWithVariableContext.isPresent()) {
            return objectFieldWithVariableToExpression(typeContext, inputValueDefinitionContext, objectFieldWithVariableContext.get(), level);
        } else {
            return defaultValueToExpression(typeContext, inputValueDefinitionContext, level);
        }
    }

    protected Optional<Expression> objectValueToExpression(GraphqlParser.TypeContext typeContext,
                                                           GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                           GraphqlParser.ObjectValueContext objectValueContext,
                                                           int level) {
        Optional<GraphqlParser.ObjectFieldContext> objectFieldContext = manager.getObjectFieldFromInputValueDefinition(objectValueContext, inputValueDefinitionContext);
        if (objectFieldContext.isPresent()) {
            return objectFieldToExpression(typeContext, inputValueDefinitionContext, objectFieldContext.get(), level);
        } else {
            return defaultValueToExpression(typeContext, inputValueDefinitionContext, level);
        }
    }

    protected Optional<Expression> defaultValueToExpression(GraphqlParser.TypeContext typeContext,
                                                            GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                            int level) {
        if (inputValueDefinitionContext.type().nonNullType() != null) {
            if (inputValueDefinitionContext.defaultValue() != null) {
                return inputValueToExpression(typeContext, inputValueDefinitionContext, level);
            } else {
                throw new GraphQLErrors(NON_NULL_VALUE_NOT_EXIST.bind(inputValueDefinitionContext.getText()));
            }
        }
        return Optional.empty();
    }

    protected Optional<Expression> argumentToExpression(GraphqlParser.TypeContext typeContext,
                                                        GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                        GraphqlParser.ArgumentContext argumentContext,
                                                        int level) {
        if (manager.fieldTypeIsList(inputValueDefinitionContext.type())) {
            return argumentContext == null ?
                    listTypeInputValueToInExpression(typeContext, inputValueDefinitionContext, level) : listTypeArgumentToExpression(typeContext, inputValueDefinitionContext, argumentContext, level);
        } else {
            return argumentContext == null ?
                    singleTypeInputValueToExpression(typeContext, inputValueDefinitionContext, level) : singleTypeArgumentToExpression(typeContext, inputValueDefinitionContext, argumentContext, level);
        }
    }

    protected Optional<Expression> inputValueToExpression(GraphqlParser.TypeContext typeContext,
                                                          GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                          int level) {
        if (manager.fieldTypeIsList(inputValueDefinitionContext.type())) {
            return listTypeInputValueToInExpression(typeContext, inputValueDefinitionContext, level);
        } else {
            return singleTypeInputValueToExpression(typeContext, inputValueDefinitionContext, level);
        }
    }

    protected Optional<Expression> objectFieldWithVariableToExpression(GraphqlParser.TypeContext typeContext,
                                                                       GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                                       GraphqlParser.ObjectFieldWithVariableContext objectFieldWithVariableContext,
                                                                       int level) {
        if (manager.fieldTypeIsList(inputValueDefinitionContext.type())) {
            return objectFieldWithVariableContext == null ?
                    listTypeInputValueToInExpression(typeContext, inputValueDefinitionContext, level) : listTypeObjectFieldWithVariableToExpression(typeContext, inputValueDefinitionContext, objectFieldWithVariableContext, level);
        } else {
            return objectFieldWithVariableContext == null ?
                    singleTypeInputValueToExpression(typeContext, inputValueDefinitionContext, level) : singleTypeObjectFieldWithVariableToExpression(typeContext, inputValueDefinitionContext, objectFieldWithVariableContext, level);
        }
    }

    protected Optional<Expression> objectFieldToExpression(GraphqlParser.TypeContext typeContext,
                                                           GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                           GraphqlParser.ObjectFieldContext objectFieldContext,
                                                           int level) {
        if (manager.fieldTypeIsList(inputValueDefinitionContext.type())) {
            return objectFieldContext == null ?
                    listTypeInputValueToInExpression(typeContext, inputValueDefinitionContext, level) : listTypeObjectFieldToExpression(typeContext, inputValueDefinitionContext, objectFieldContext, level);
        } else {
            return objectFieldContext == null ?
                    singleTypeInputValueToExpression(typeContext, inputValueDefinitionContext, level) : singleTypeObjectFieldToExpression(typeContext, inputValueDefinitionContext, objectFieldContext, level);
        }
    }

    protected Optional<Expression> singleTypeArgumentToExpression(GraphqlParser.TypeContext typeContext,
                                                                  GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                                  GraphqlParser.ArgumentContext argumentContext,
                                                                  int level) {
        Optional<GraphqlParser.ObjectTypeDefinitionContext> objectTypeDefinitionContext = manager.getObject(manager.getFieldTypeName(typeContext));
        if (objectTypeDefinitionContext.isPresent()) {
            Optional<GraphqlParser.FieldDefinitionContext> fieldDefinitionContext = manager.getFieldDefinitionFromInputValueDefinition(typeContext, inputValueDefinitionContext);
            if (fieldDefinitionContext.isPresent()) {
                String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.get().type());
                if (manager.isObject(fieldTypeName)) {
                    if (isConditionalObject(inputValueDefinitionContext)) {
                        return Optional.of(
                                objectValueWithVariableToExpression(
                                        objectTypeDefinitionContext.get(),
                                        fieldDefinitionContext.get(),
                                        inputValueDefinitionContext,
                                        argumentContext.valueWithVariable().objectValueWithVariable(),
                                        level + 1
                                )
                        );
                    } else {
                        throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE.bind(fieldTypeName));
                    }
                } else if (manager.isScalar(fieldTypeName) || manager.isEnum(fieldTypeName)) {
                    if (manager.fieldTypeIsList(fieldDefinitionContext.get().type())) {
                        if (isOperatorObject(inputValueDefinitionContext)) {
                            return operatorArgumentToMapWithToExpression(objectTypeDefinitionContext.get(), fieldDefinitionContext.get(), inputValueDefinitionContext, argumentContext, level + 1);
                        } else if (isConditionalObject(inputValueDefinitionContext)) {
                            return objectValueWithVariableToMultipleExpression(typeContext, inputValueDefinitionContext, argumentContext.valueWithVariable().objectValueWithVariable(), level);
                        } else if (manager.isScalar(fieldTypeName)) {
                            return Optional.of(scalarValueWithVariableToExpression(mapWithToColumn(objectTypeDefinitionContext.get(), fieldDefinitionContext.get(), level + 1), argumentContext.valueWithVariable()))
                                    .map(expression -> existsExpression(mapWithTypeToFieldPlainSelect(objectTypeDefinitionContext.get(), fieldDefinitionContext.get(), expression, level + 1)));
                        } else if (manager.isEnum(fieldTypeName)) {
                            return Optional.of(enumValueWithVariableToExpression(mapWithToColumn(objectTypeDefinitionContext.get(), fieldDefinitionContext.get(), level + 1), argumentContext.valueWithVariable()))
                                    .map(expression -> existsExpression(mapWithTypeToFieldPlainSelect(objectTypeDefinitionContext.get(), fieldDefinitionContext.get(), expression, level + 1)));
                        } else {
                            throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE.bind(fieldTypeName));
                        }
                    } else {
                        if (isOperatorObject(inputValueDefinitionContext)) {
                            return operatorArgumentToExpression(argumentToColumn(typeContext, argumentContext, level), inputValueDefinitionContext, argumentContext);
                        } else if (isConditionalObject(inputValueDefinitionContext)) {
                            return objectValueWithVariableToMultipleExpression(typeContext, inputValueDefinitionContext, argumentContext.valueWithVariable().objectValueWithVariable(), level);
                        } else if (manager.isScalar(fieldTypeName)) {
                            return Optional.of(scalarValueWithVariableToExpression(argumentToColumn(typeContext, argumentContext, level), argumentContext.valueWithVariable()));
                        } else if (manager.isEnum(fieldTypeName)) {
                            return Optional.of(enumValueWithVariableToExpression(argumentToColumn(typeContext, argumentContext, level), argumentContext.valueWithVariable()));
                        } else {
                            throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE.bind(fieldTypeName));
                        }
                    }
                } else {
                    throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE.bind(fieldTypeName));
                }
            } else {
                throw new GraphQLErrors(FIELD_NOT_EXIST.bind(manager.getFieldTypeName(typeContext), inputValueDefinitionContext.name().getText()));
            }
        } else {
            throw new GraphQLErrors(TYPE_NOT_EXIST.bind(manager.getFieldTypeName(typeContext)));
        }
    }

    protected Optional<Expression> singleTypeObjectFieldWithVariableToExpression(GraphqlParser.TypeContext typeContext,
                                                                                 GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                                                 GraphqlParser.ObjectFieldWithVariableContext objectFieldWithVariableContext,
                                                                                 int level) {
        Optional<GraphqlParser.ObjectTypeDefinitionContext> objectTypeDefinitionContext = manager.getObject(manager.getFieldTypeName(typeContext));
        if (objectTypeDefinitionContext.isPresent()) {

            Optional<GraphqlParser.FieldDefinitionContext> fieldDefinitionContext = manager.getFieldDefinitionFromInputValueDefinition(typeContext, inputValueDefinitionContext);
            if (fieldDefinitionContext.isPresent()) {
                String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.get().type());
                if (manager.isObject(fieldTypeName)) {
                    if (isConditionalObject(inputValueDefinitionContext)) {
                        return Optional.of(
                                objectValueWithVariableToExpression(
                                        objectTypeDefinitionContext.get(),
                                        fieldDefinitionContext.get(),
                                        inputValueDefinitionContext,
                                        objectFieldWithVariableContext.valueWithVariable().objectValueWithVariable(),
                                        level + 1
                                )
                        );
                    } else {
                        throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE.bind(fieldTypeName));
                    }
                } else if (manager.isScalar(fieldTypeName) || manager.isEnum(fieldTypeName)) {
                    if (manager.fieldTypeIsList(fieldDefinitionContext.get().type())) {
                        if (isOperatorObject(inputValueDefinitionContext)) {
                            return operatorObjectFieldWithVariableToMapWithToExpression(objectTypeDefinitionContext.get(), fieldDefinitionContext.get(), inputValueDefinitionContext, objectFieldWithVariableContext, level + 1);
                        } else if (isConditionalObject(inputValueDefinitionContext)) {
                            return objectValueWithVariableToMultipleExpression(typeContext, inputValueDefinitionContext, objectFieldWithVariableContext.valueWithVariable().objectValueWithVariable(), level);
                        } else if (manager.isScalar(fieldTypeName)) {
                            return Optional.of(scalarValueWithVariableToExpression(mapWithToColumn(objectTypeDefinitionContext.get(), fieldDefinitionContext.get(), level + 1), objectFieldWithVariableContext.valueWithVariable()))
                                    .map(expression -> existsExpression(mapWithTypeToFieldPlainSelect(objectTypeDefinitionContext.get(), fieldDefinitionContext.get(), expression, level + 1)));
                        } else if (manager.isEnum(fieldTypeName)) {
                            return Optional.of(enumValueWithVariableToExpression(mapWithToColumn(objectTypeDefinitionContext.get(), fieldDefinitionContext.get(), level + 1), objectFieldWithVariableContext.valueWithVariable()))
                                    .map(expression -> existsExpression(mapWithTypeToFieldPlainSelect(objectTypeDefinitionContext.get(), fieldDefinitionContext.get(), expression, level + 1)));
                        } else {
                            throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE.bind(fieldTypeName));
                        }
                    } else {
                        if (isOperatorObject(inputValueDefinitionContext)) {
                            return operatorObjectFieldWithVariableToExpression(objectFieldWithVariableToColumn(typeContext, objectFieldWithVariableContext, level), inputValueDefinitionContext, objectFieldWithVariableContext);
                        } else if (isConditionalObject(inputValueDefinitionContext)) {
                            return objectValueWithVariableToMultipleExpression(typeContext, inputValueDefinitionContext, objectFieldWithVariableContext.valueWithVariable().objectValueWithVariable(), level);
                        } else if (manager.isScalar(fieldTypeName)) {
                            return Optional.of(scalarValueWithVariableToExpression(objectFieldWithVariableToColumn(typeContext, objectFieldWithVariableContext, level), objectFieldWithVariableContext.valueWithVariable()));
                        } else if (manager.isEnum(fieldTypeName)) {
                            return Optional.of(enumValueWithVariableToExpression(objectFieldWithVariableToColumn(typeContext, objectFieldWithVariableContext, level), objectFieldWithVariableContext.valueWithVariable()));
                        } else {
                            throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE.bind(fieldTypeName));
                        }
                    }
                } else {
                    throw new GraphQLErrors(FIELD_NOT_EXIST.bind(manager.getFieldTypeName(typeContext), inputValueDefinitionContext.name().getText()));
                }
            } else {
                throw new GraphQLErrors(FIELD_NOT_EXIST.bind(manager.getFieldTypeName(typeContext), inputValueDefinitionContext.name().getText()));
            }
        } else {
            throw new GraphQLErrors(TYPE_NOT_EXIST.bind(manager.getFieldTypeName(typeContext)));
        }
    }

    protected Optional<Expression> singleTypeObjectFieldToExpression(GraphqlParser.TypeContext typeContext,
                                                                     GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                                     GraphqlParser.ObjectFieldContext objectFieldContext,
                                                                     int level) {
        Optional<GraphqlParser.ObjectTypeDefinitionContext> objectTypeDefinitionContext = manager.getObject(manager.getFieldTypeName(typeContext));
        if (objectTypeDefinitionContext.isPresent()) {
            Optional<GraphqlParser.FieldDefinitionContext> fieldDefinitionContext = manager.getFieldDefinitionFromInputValueDefinition(typeContext, inputValueDefinitionContext);
            if (fieldDefinitionContext.isPresent()) {
                String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.get().type());
                if (manager.isObject(fieldTypeName)) {
                    if (isConditionalObject(inputValueDefinitionContext)) {
                        return Optional.of(
                                objectValueToExpression(
                                        objectTypeDefinitionContext.get(),
                                        fieldDefinitionContext.get(),
                                        inputValueDefinitionContext,
                                        objectFieldContext.value().objectValue(),
                                        level + 1
                                )
                        );
                    } else {
                        throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE.bind(fieldTypeName));
                    }
                } else if (manager.isScalar(fieldTypeName) || manager.isEnum(fieldTypeName)) {
                    if (manager.fieldTypeIsList(fieldDefinitionContext.get().type())) {
                        if (isOperatorObject(inputValueDefinitionContext)) {
                            return operatorObjectFieldToMapWithToExpression(objectTypeDefinitionContext.get(), fieldDefinitionContext.get(), inputValueDefinitionContext, objectFieldContext, level + 1);
                        } else if (isConditionalObject(inputValueDefinitionContext)) {
                            return objectValueToMultipleExpression(typeContext, inputValueDefinitionContext, objectFieldContext.value().objectValue(), level);
                        } else if (manager.isScalar(fieldTypeName)) {
                            return Optional.of(scalarValueToExpression(mapWithToColumn(objectTypeDefinitionContext.get(), fieldDefinitionContext.get(), level + 1), objectFieldContext.value()))
                                    .map(expression -> existsExpression(mapWithTypeToFieldPlainSelect(objectTypeDefinitionContext.get(), fieldDefinitionContext.get(), expression, level + 1)));
                        } else if (manager.isEnum(fieldTypeName)) {
                            return Optional.of(enumValueToExpression(mapWithToColumn(objectTypeDefinitionContext.get(), fieldDefinitionContext.get(), level + 1), objectFieldContext.value().enumValue()))
                                    .map(expression -> existsExpression(mapWithTypeToFieldPlainSelect(objectTypeDefinitionContext.get(), fieldDefinitionContext.get(), expression, level + 1)));
                        } else {
                            throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE.bind(fieldTypeName));
                        }
                    } else {
                        if (isOperatorObject(inputValueDefinitionContext)) {
                            return operatorObjectFieldToExpression(objectFieldToColumn(typeContext, objectFieldContext, level), inputValueDefinitionContext, objectFieldContext);
                        } else if (isConditionalObject(inputValueDefinitionContext)) {
                            return objectValueToMultipleExpression(typeContext, inputValueDefinitionContext, objectFieldContext.value().objectValue(), level);
                        } else if (manager.isScalar(fieldTypeName)) {
                            return Optional.of(scalarValueToExpression(objectFieldToColumn(typeContext, objectFieldContext, level), objectFieldContext.value()));
                        } else if (manager.isEnum(fieldTypeName)) {
                            return Optional.of(enumValueToExpression(objectFieldToColumn(typeContext, objectFieldContext, level), objectFieldContext.value().enumValue()));
                        } else {
                            throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE.bind(fieldTypeName));
                        }
                    }
                } else {
                    throw new GraphQLErrors(FIELD_NOT_EXIST.bind(manager.getFieldTypeName(typeContext), inputValueDefinitionContext.name().getText()));
                }
            } else {
                throw new GraphQLErrors(FIELD_NOT_EXIST.bind(manager.getFieldTypeName(typeContext), inputValueDefinitionContext.name().getText()));
            }
        } else {
            throw new GraphQLErrors(TYPE_NOT_EXIST.bind(manager.getFieldTypeName(typeContext)));
        }
    }

    protected Optional<Expression> singleTypeInputValueToExpression(GraphqlParser.TypeContext typeContext,
                                                                    GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                                    int level) {
        Optional<GraphqlParser.ObjectTypeDefinitionContext> objectTypeDefinitionContext = manager.getObject(manager.getFieldTypeName(typeContext));
        if (objectTypeDefinitionContext.isPresent()) {
            Optional<GraphqlParser.FieldDefinitionContext> fieldDefinitionContext = manager.getFieldDefinitionFromInputValueDefinition(typeContext, inputValueDefinitionContext);
            if (fieldDefinitionContext.isPresent()) {
                String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.get().type());
                if (manager.isObject(fieldTypeName)) {
                    if (isConditionalObject(inputValueDefinitionContext)) {
                        return Optional.of(
                                objectValueToExpression(
                                        objectTypeDefinitionContext.get(),
                                        fieldDefinitionContext.get(),
                                        inputValueDefinitionContext,
                                        inputValueDefinitionContext.defaultValue().value().objectValue(),
                                        level + 1
                                )
                        );
                    } else {
                        throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE.bind(fieldTypeName));
                    }
                } else if (manager.isScalar(fieldTypeName) || manager.isEnum(fieldTypeName)) {
                    if (manager.fieldTypeIsList(fieldDefinitionContext.get().type())) {
                        if (isOperatorObject(inputValueDefinitionContext)) {
                            return operatorInputValueToMapWithToExpression(objectTypeDefinitionContext.get(), fieldDefinitionContext.get(), inputValueDefinitionContext, level + 1);
                        } else if (isConditionalObject(inputValueDefinitionContext)) {
                            return objectValueToMultipleExpression(typeContext, inputValueDefinitionContext, inputValueDefinitionContext.defaultValue().value().objectValue(), level);
                        } else if (manager.isScalar(fieldTypeName)) {
                            return Optional.of(scalarValueToExpression(mapWithToColumn(objectTypeDefinitionContext.get(), fieldDefinitionContext.get(), level + 1), inputValueDefinitionContext.defaultValue().value()))
                                    .map(expression -> existsExpression(mapWithTypeToFieldPlainSelect(objectTypeDefinitionContext.get(), fieldDefinitionContext.get(), expression, level + 1)));
                        } else if (manager.isEnum(fieldTypeName)) {
                            return Optional.of(enumValueToExpression(mapWithToColumn(objectTypeDefinitionContext.get(), fieldDefinitionContext.get(), level + 1), inputValueDefinitionContext.defaultValue().value().enumValue()))
                                    .map(expression -> existsExpression(mapWithTypeToFieldPlainSelect(objectTypeDefinitionContext.get(), fieldDefinitionContext.get(), expression, level + 1)));
                        } else {
                            throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE.bind(fieldTypeName));
                        }
                    } else {
                        if (isOperatorObject(inputValueDefinitionContext)) {
                            return operatorInputValueToExpression(inputValueToColumn(typeContext, inputValueDefinitionContext, level), inputValueDefinitionContext);
                        } else if (isConditionalObject(inputValueDefinitionContext)) {
                            return objectValueToMultipleExpression(typeContext, inputValueDefinitionContext, inputValueDefinitionContext.defaultValue().value().objectValue(), level);
                        } else if (manager.isScalar(fieldTypeName)) {
                            return Optional.of(scalarValueToExpression(inputValueToColumn(typeContext, inputValueDefinitionContext, level), inputValueDefinitionContext.defaultValue().value()));
                        } else if (manager.isEnum(fieldTypeName)) {
                            return Optional.of(enumValueToExpression(inputValueToColumn(typeContext, inputValueDefinitionContext, level), inputValueDefinitionContext.defaultValue().value().enumValue()));
                        } else {
                            throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE.bind(fieldTypeName));
                        }
                    }
                } else {
                    throw new GraphQLErrors(FIELD_NOT_EXIST.bind(manager.getFieldTypeName(typeContext), inputValueDefinitionContext.name().getText()));
                }
            } else {
                throw new GraphQLErrors(FIELD_NOT_EXIST.bind(manager.getFieldTypeName(typeContext), inputValueDefinitionContext.name().getText()));
            }
        } else {
            throw new GraphQLErrors(TYPE_NOT_EXIST.bind(manager.getFieldTypeName(typeContext)));
        }
    }

    protected Optional<Expression> listTypeArgumentToExpression(GraphqlParser.TypeContext typeContext,
                                                                GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                                GraphqlParser.ArgumentContext argumentContext,
                                                                int level) {
        if (argumentContext == null) {
            return listTypeInputValueToInExpression(typeContext, inputValueDefinitionContext, level);
        } else {
            return listTypeArgumentToInExpression(typeContext, inputValueDefinitionContext, argumentContext, level);
        }
    }

    protected Optional<Expression> listTypeObjectFieldWithVariableToExpression(GraphqlParser.TypeContext typeContext,
                                                                               GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                                               GraphqlParser.ObjectFieldWithVariableContext objectFieldWithVariableContext,
                                                                               int level) {
        if (objectFieldWithVariableContext == null) {
            return listTypeInputValueToInExpression(typeContext, inputValueDefinitionContext, level);
        } else {
            return listTypeObjectFieldWithVariableToInExpression(typeContext, inputValueDefinitionContext, objectFieldWithVariableContext, level);
        }
    }

    protected Optional<Expression> listTypeObjectFieldToExpression(GraphqlParser.TypeContext typeContext,
                                                                   GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                                   GraphqlParser.ObjectFieldContext objectFieldContext,
                                                                   int level) {
        if (objectFieldContext == null) {
            return listTypeInputValueToInExpression(typeContext, inputValueDefinitionContext, level);
        } else {
            return listTypeObjectFieldToInExpression(typeContext, inputValueDefinitionContext, objectFieldContext, level);
        }
    }

    protected Stream<Expression> listTypeConditionalFieldOfArgumentsToExpressionList(GraphqlParser.TypeContext typeContext,
                                                                                     GraphqlParser.ArgumentsDefinitionContext argumentsDefinitionContext,
                                                                                     GraphqlParser.ArgumentsContext argumentsContext,
                                                                                     int level) {
        Optional<GraphqlParser.InputValueDefinitionContext> conditionalInputValueDefinitionContext = argumentsDefinitionContext.inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> manager.fieldTypeIsList(inputValueDefinitionContext.type()) && isConditionalObject(inputValueDefinitionContext))
                .findFirst();
        if (conditionalInputValueDefinitionContext.isPresent()) {
            Optional<GraphqlParser.ArgumentContext> argumentContext = manager.getArgumentFromInputValueDefinition(argumentsContext, conditionalInputValueDefinitionContext.get());
            return argumentContext
                    .flatMap(context ->
                            Optional.of(
                                    context.valueWithVariable().arrayValueWithVariable().valueWithVariable().stream()
                                            .map(valueWithVariableContext -> objectValueWithVariableToMultipleExpression(typeContext, conditionalInputValueDefinitionContext.get(), valueWithVariableContext.objectValueWithVariable(), level))
                                            .flatMap(Optional::stream)
                            )
                    )
                    .orElseGet(() -> listTypeConditionalFieldOfInputValueToExpression(typeContext, conditionalInputValueDefinitionContext.get(), level));
        }
        return Stream.empty();
    }

    protected Stream<Expression> listTypeConditionalFieldOfObjectValueWithVariableToExpressionList(GraphqlParser.TypeContext typeContext,
                                                                                                   GraphqlParser.InputObjectValueDefinitionsContext inputObjectValueDefinitionsContext,
                                                                                                   GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext,
                                                                                                   int level) {
        Optional<GraphqlParser.InputValueDefinitionContext> conditionalInputValueDefinitionContext = inputObjectValueDefinitionsContext.inputValueDefinition().stream()
                .filter(fieldInputValueDefinitionContext -> manager.fieldTypeIsList(fieldInputValueDefinitionContext.type()) && isConditionalObject(fieldInputValueDefinitionContext))
                .findFirst();
        if (conditionalInputValueDefinitionContext.isPresent()) {
            Optional<GraphqlParser.ObjectFieldWithVariableContext> objectFieldWithVariableContext = manager.getObjectFieldWithVariableFromInputValueDefinition(objectValueWithVariableContext, conditionalInputValueDefinitionContext.get());
            return objectFieldWithVariableContext
                    .flatMap(fieldWithVariableContext ->
                            Optional.of(
                                    fieldWithVariableContext.valueWithVariable().arrayValueWithVariable().valueWithVariable().stream()
                                            .map(valueWithVariableContext -> objectValueWithVariableToMultipleExpression(typeContext, conditionalInputValueDefinitionContext.get(), valueWithVariableContext.objectValueWithVariable(), level))
                                            .flatMap(Optional::stream)
                            )
                    )
                    .orElseGet(() -> listTypeConditionalFieldOfInputValueToExpression(typeContext, conditionalInputValueDefinitionContext.get(), level));
        }
        return Stream.empty();
    }

    protected Stream<Expression> listTypeConditionalFieldOfObjectValueToExpression(GraphqlParser.TypeContext typeContext,
                                                                                   GraphqlParser.InputObjectValueDefinitionsContext inputObjectValueDefinitionsContext,
                                                                                   GraphqlParser.ObjectValueContext objectValueContext,
                                                                                   int level) {
        Optional<GraphqlParser.InputValueDefinitionContext> conditionalInputValueDefinitionContext = inputObjectValueDefinitionsContext.inputValueDefinition().stream()
                .filter(fieldInputValueDefinitionContext -> manager.fieldTypeIsList(fieldInputValueDefinitionContext.type()) && isConditionalObject(fieldInputValueDefinitionContext))
                .findFirst();
        if (conditionalInputValueDefinitionContext.isPresent()) {
            Optional<GraphqlParser.ObjectFieldContext> objectFieldContext = manager.getObjectFieldFromInputValueDefinition(objectValueContext, conditionalInputValueDefinitionContext.get());
            return objectFieldContext
                    .flatMap(fieldContext ->
                            Optional.of(
                                    fieldContext.value().arrayValue().value().stream()
                                            .map(valueContext -> objectValueToMultipleExpression(typeContext, conditionalInputValueDefinitionContext.get(), valueContext.objectValue(), level))
                                            .flatMap(Optional::stream)
                            )
                    )
                    .orElseGet(() -> listTypeConditionalFieldOfInputValueToExpression(typeContext, conditionalInputValueDefinitionContext.get(), level));
        }
        return Stream.empty();
    }

    protected Stream<Expression> listTypeConditionalFieldOfInputValueToExpression(GraphqlParser.TypeContext typeContext,
                                                                                  GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                                                  int level) {
        if (inputValueDefinitionContext.type().nonNullType() != null) {
            if (inputValueDefinitionContext.defaultValue() != null) {
                return inputValueDefinitionContext.defaultValue().value().arrayValue().value().stream()
                        .map(valueContext -> objectValueToMultipleExpression(typeContext, inputValueDefinitionContext, valueContext.objectValue(), level))
                        .flatMap(Optional::stream);
            } else {
                throw new GraphQLErrors(NON_NULL_VALUE_NOT_EXIST.bind(inputValueDefinitionContext.getText()));
            }
        }
        return Stream.empty();
    }

    private Optional<Expression> listTypeArgumentToInExpression(GraphqlParser.TypeContext typeContext,
                                                                GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                                GraphqlParser.ArgumentContext argumentContext,
                                                                int level) {
        return valueWithVariableToInExpression(typeContext, inputValueDefinitionContext, argumentContext.valueWithVariable(), level);
    }

    private Optional<Expression> listTypeInputValueToInExpression(GraphqlParser.TypeContext typeContext,
                                                                  GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                                  int level) {
        return valueToInExpression(typeContext, inputValueDefinitionContext, inputValueDefinitionContext.defaultValue().value(), level);
    }

    private Optional<Expression> listTypeObjectFieldWithVariableToInExpression(GraphqlParser.TypeContext typeContext,
                                                                               GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                                               GraphqlParser.ObjectFieldWithVariableContext objectFieldWithVariableContext,
                                                                               int level) {
        return valueWithVariableToInExpression(typeContext, inputValueDefinitionContext, objectFieldWithVariableContext.valueWithVariable(), level);
    }

    private Optional<Expression> listTypeObjectFieldToInExpression(GraphqlParser.TypeContext typeContext,
                                                                   GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                                   GraphqlParser.ObjectFieldContext objectFieldContext,
                                                                   int level) {
        return valueToInExpression(typeContext, inputValueDefinitionContext, objectFieldContext.value(), level);
    }

    protected Optional<Expression> valueWithVariableToInExpression(GraphqlParser.TypeContext typeContext,
                                                                   GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                                   GraphqlParser.ValueWithVariableContext valueWithVariableContext,
                                                                   int level) {
        if (manager.isScalar(manager.getFieldTypeName(inputValueDefinitionContext.type()))) {
            if (valueWithVariableContext.arrayValueWithVariable() != null) {
                InExpression inExpression = new InExpression();
                inExpression.setLeftExpression(inputValueToColumn(typeContext, inputValueDefinitionContext, level));
                inExpression.setRightItemsList(
                        new ExpressionList(valueWithVariableContext.arrayValueWithVariable().valueWithVariable().stream()
                                .map(dbValueUtil::scalarValueWithVariableToDBValue).collect(Collectors.toList()))
                );
                return Optional.of(inExpression);
            }
        }
        return Optional.empty();
    }

    protected Optional<Expression> valueToInExpression(GraphqlParser.TypeContext typeContext,
                                                       GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                       GraphqlParser.ValueContext valueContext,
                                                       int level) {
        if (manager.isScalar(manager.getFieldTypeName(inputValueDefinitionContext.type()))) {
            if (valueContext.arrayValue() != null) {
                InExpression inExpression = new InExpression();
                inExpression.setLeftExpression(inputValueToColumn(typeContext, inputValueDefinitionContext, level));
                inExpression.setRightItemsList(
                        new ExpressionList(valueContext.arrayValue().value().stream()
                                .map(dbValueUtil::scalarValueToDBValue).collect(Collectors.toList()))
                );
                return Optional.of(inExpression);
            }
        }
        return Optional.empty();
    }

    private boolean hasOrConditional(GraphqlParser.ArgumentsContext argumentsContext, GraphqlParser.ArgumentsDefinitionContext argumentsDefinitionContext) {
        return argumentsContext.argument().stream()
                .anyMatch(argumentContext ->
                        manager.getInputValueDefinitionFromArgumentsDefinitionContext(argumentsDefinitionContext, argumentContext)
                                .map(inputValueDefinitionContext -> isOrConditional(inputValueDefinitionContext, argumentContext.valueWithVariable()))
                                .orElse(false)
                );
    }

    private boolean hasOrConditional(GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext, GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext) {

        return objectValueWithVariableContext.objectFieldWithVariable().stream()
                .anyMatch(objectFieldWithVariableContext ->
                        manager.getInputValueDefinitionFromInputObjectTypeDefinitionContext(inputObjectTypeDefinitionContext, objectFieldWithVariableContext)
                                .map(inputValueDefinitionContext -> isOrConditional(inputValueDefinitionContext, objectFieldWithVariableContext.valueWithVariable()))
                                .orElse(false)
                );
    }

    private boolean hasOrConditional(GraphqlParser.ObjectValueContext objectValueContext, GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext) {
        return objectValueContext.objectField().stream().anyMatch(objectFieldContext ->
                manager.getInputValueDefinitionFromInputObjectTypeDefinitionContext(inputObjectTypeDefinitionContext, objectFieldContext)
                        .map(inputValueDefinitionContext -> isOrConditional(inputValueDefinitionContext, objectFieldContext.value()))
                        .orElse(false));
    }

    private boolean isOrConditional(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ValueWithVariableContext valueWithVariableContext) {
        if (isConditional(inputValueDefinitionContext)) {
            return conditionalIsOr(valueWithVariableContext.enumValue());
        }
        return false;
    }

    private boolean isOrConditional(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, GraphqlParser.ValueContext valueContext) {
        if (isConditional(inputValueDefinitionContext)) {
            return conditionalIsOr(valueContext.enumValue());
        }
        return false;
    }

    private boolean conditionalIsOr(GraphqlParser.EnumValueContext enumValueContext) {
        return enumValueContext != null && enumValueContext.enumValueName().getText().equals("OR");
    }

    private boolean isConditional(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return inputValueDefinitionContext.type().typeName() != null && isConditional(inputValueDefinitionContext.type().typeName().name().getText());
    }

    private boolean isNotConditional(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return !isConditional(inputValueDefinitionContext);
    }

    private boolean isConditional(String typeName) {
        return typeName != null && manager.isEnum(typeName) && typeName.equals("Conditional");
    }

    private boolean isOperatorObject(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return containsEnum(inputValueDefinitionContext, "Operator");
    }

    private boolean isConditionalObject(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return containsEnum(inputValueDefinitionContext, "Conditional");
    }

    private boolean containsEnum(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, String enumName) {
        String fieldTypeName = manager.getFieldTypeName(inputValueDefinitionContext.type());
        Optional<GraphqlParser.InputObjectTypeDefinitionContext> objectTypeDefinition = manager.getInputObject(fieldTypeName);
        return objectTypeDefinition
                .map(inputObjectTypeDefinitionContext ->
                        inputObjectTypeDefinitionContext.inputObjectValueDefinitions().inputValueDefinition().stream()
                                .anyMatch(fieldInputValueDefinitionContext ->
                                        manager.isEnum(fieldInputValueDefinitionContext.type().getText()) &&
                                                fieldInputValueDefinitionContext.type().typeName().name().getText().equals(enumName))
                )
                .orElse(false);
    }

    private Optional<Expression> operatorArgumentToExpression(Expression leftExpression,
                                                              GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                              GraphqlParser.ArgumentContext argumentContext) {
        return operatorValueWithVariableToExpression(leftExpression, inputValueDefinitionContext, argumentContext.valueWithVariable());
    }

    private Optional<Expression> operatorInputValueToExpression(Expression leftExpression, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return operatorValueToExpression(leftExpression, inputValueDefinitionContext, inputValueDefinitionContext.defaultValue().value());
    }

    private Optional<Expression> operatorObjectFieldWithVariableToExpression(Expression leftExpression,
                                                                             GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                                             GraphqlParser.ObjectFieldWithVariableContext objectFieldWithVariableContext) {
        return operatorValueWithVariableToExpression(leftExpression, inputValueDefinitionContext, objectFieldWithVariableContext.valueWithVariable());
    }

    private Optional<Expression> operatorObjectFieldToExpression(Expression leftExpression,
                                                                 GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                                 GraphqlParser.ObjectFieldContext objectFieldContext) {
        return operatorValueToExpression(leftExpression, inputValueDefinitionContext, objectFieldContext.value());
    }

    private Optional<Expression> operatorArgumentToMapWithToExpression(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext,
                                                                       GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                       GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                                       GraphqlParser.ArgumentContext argumentContext,
                                                                       int level) {
        return operatorValueWithVariableToExpression(mapWithToColumn(objectTypeDefinitionContext, fieldDefinitionContext, level), inputValueDefinitionContext, argumentContext.valueWithVariable())
                .map(expression -> existsExpression(mapWithTypeToFieldPlainSelect(objectTypeDefinitionContext, fieldDefinitionContext, expression, level)));
    }

    private Optional<Expression> operatorInputValueToMapWithToExpression(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext,
                                                                         GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                         GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                                         int level) {
        return operatorValueToExpression(mapWithToColumn(objectTypeDefinitionContext, fieldDefinitionContext, level), inputValueDefinitionContext, inputValueDefinitionContext.defaultValue().value())
                .map(expression -> existsExpression(mapWithTypeToFieldPlainSelect(objectTypeDefinitionContext, fieldDefinitionContext, expression, level)));
    }

    private Optional<Expression> operatorObjectFieldWithVariableToMapWithToExpression(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext,
                                                                                      GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                                      GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                                                      GraphqlParser.ObjectFieldWithVariableContext objectFieldWithVariableContext,
                                                                                      int level) {
        return operatorValueWithVariableToExpression(mapWithToColumn(objectTypeDefinitionContext, fieldDefinitionContext, level), inputValueDefinitionContext, objectFieldWithVariableContext.valueWithVariable())
                .map(expression -> existsExpression(mapWithTypeToFieldPlainSelect(objectTypeDefinitionContext, fieldDefinitionContext, expression, level)));
    }

    private Optional<Expression> operatorObjectFieldToMapWithToExpression(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext,
                                                                          GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                          GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                                          GraphqlParser.ObjectFieldContext objectFieldContext,
                                                                          int level) {
        return operatorValueToExpression(mapWithToColumn(objectTypeDefinitionContext, fieldDefinitionContext, level), inputValueDefinitionContext, objectFieldContext.value())
                .map(expression -> existsExpression(mapWithTypeToFieldPlainSelect(objectTypeDefinitionContext, fieldDefinitionContext, expression, level)));
    }

    public Optional<Expression> operatorArgumentsToExpression(Expression leftExpression,
                                                              GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                              GraphqlParser.ArgumentsContext argumentsContext) {

        if (argumentsContext == null) {
            return Optional.empty();
        }

        boolean skipNull = argumentsContext.argument().stream()
                .filter(argumentContext -> argumentContext.name().getText().equals("skipNull"))
                .findFirst()
                .filter(argumentContext -> argumentContext.valueWithVariable().BooleanValue() != null)
                .map(argumentContext -> Boolean.parseBoolean(argumentContext.valueWithVariable().BooleanValue().getText()))
                .orElse(false);

        fieldDefinitionContext.argumentsDefinition().inputValueDefinition().stream()
                .filter(fieldInputValueDefinitionContext ->
                        manager.isEnum(fieldInputValueDefinitionContext.type().getText()) && manager.getFieldTypeName(fieldInputValueDefinitionContext.type()).equals("Operator"))
                .findFirst()
                .flatMap(fieldInputValueDefinitionContext -> manager.getArgumentFromInputValueDefinition(argumentsContext, fieldInputValueDefinitionContext))
                .map(objectFieldWithVariableContext -> objectFieldWithVariableContext.valueWithVariable().enumValue());

        Optional<GraphqlParser.EnumValueContext> operatorEnumValueContext = fieldDefinitionContext.argumentsDefinition().inputValueDefinition().stream()
                .filter(fieldInputValueDefinitionContext ->
                        manager.isEnum(fieldInputValueDefinitionContext.type().getText()) && manager.getFieldTypeName(fieldInputValueDefinitionContext.type()).equals("Operator"))
                .findFirst()
                .flatMap(fieldInputValueDefinitionContext -> manager.getArgumentFromInputValueDefinition(argumentsContext, fieldInputValueDefinitionContext))
                .map(objectFieldWithVariableContext -> objectFieldWithVariableContext.valueWithVariable().enumValue());

        Optional<GraphqlParser.EnumValueContext> defaultOperatorEnumValueContext = fieldDefinitionContext.argumentsDefinition().inputValueDefinition().stream()
                .filter(fieldInputValueDefinitionContext ->
                        manager.isEnum(fieldInputValueDefinitionContext.type().getText()) && manager.getFieldTypeName(fieldInputValueDefinitionContext.type()).equals("Operator"))
                .findFirst()
                .flatMap(manager::getDefaultValueFromInputValueDefinition)
                .map(GraphqlParser.ValueContext::enumValue);

        Optional<GraphqlParser.InputValueDefinitionContext> subInputValueDefinitionContext = fieldDefinitionContext.argumentsDefinition().inputValueDefinition().stream()
                .filter(fieldInputValueDefinitionContext ->
                        !(manager.isEnum(fieldInputValueDefinitionContext.type().getText()) && manager.getFieldTypeName(fieldInputValueDefinitionContext.type()).equals("Operator")))
                .filter(fieldInputValueDefinitionContext ->
                        argumentsContext.argument().stream()
                                .anyMatch(argumentContext -> argumentContext.name().getText().equals(fieldInputValueDefinitionContext.name().getText()))
                )
                .findFirst();

        Optional<GraphqlParser.ValueWithVariableContext> subValueWithVariableContext = subInputValueDefinitionContext
                .flatMap(fieldInputValueDefinitionContext -> manager.getArgumentFromInputValueDefinition(argumentsContext, fieldInputValueDefinitionContext))
                .map(GraphqlParser.ArgumentContext::valueWithVariable);

        Optional<GraphqlParser.ValueContext> subDefaultValueContext = subInputValueDefinitionContext
                .flatMap(manager::getDefaultValueFromInputValueDefinition);

        if (operatorEnumValueContext.isPresent() && subValueWithVariableContext.isPresent()) {
            return operatorValueWithVariableToExpression(leftExpression, operatorEnumValueContext.get(), subInputValueDefinitionContext.get(), subValueWithVariableContext.get(), skipNull);
        } else if (operatorEnumValueContext.isPresent() && subDefaultValueContext.isPresent()) {
            return operatorValueToExpression(leftExpression, operatorEnumValueContext.get(), subInputValueDefinitionContext.get(), subDefaultValueContext.get(), skipNull);
        } else if (defaultOperatorEnumValueContext.isPresent() && subValueWithVariableContext.isPresent()) {
            return operatorValueWithVariableToExpression(leftExpression, defaultOperatorEnumValueContext.get(), subInputValueDefinitionContext.get(), subValueWithVariableContext.get(), skipNull);
        } else if (defaultOperatorEnumValueContext.isPresent() && subDefaultValueContext.isPresent()) {
            return operatorValueToExpression(leftExpression, defaultOperatorEnumValueContext.get(), subInputValueDefinitionContext.get(), subDefaultValueContext.get(), skipNull);
        } else {
            return Optional.empty();
        }
    }

    private Optional<Expression> operatorValueWithVariableToExpression(Expression leftExpression,
                                                                       GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                                       GraphqlParser.ValueWithVariableContext valueWithVariableContext) {
        Optional<GraphqlParser.InputObjectTypeDefinitionContext> inputObjectTypeDefinition = manager.getInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type()));

        if (valueWithVariableContext.objectValueWithVariable() == null) {
            throw new GraphQLErrors(UNSUPPORTED_OPERATOR.bind(valueWithVariableContext.getText()));
        }

        if (inputObjectTypeDefinition.isPresent()) {

            boolean skipNull = valueWithVariableContext.objectValueWithVariable().objectFieldWithVariable().stream()
                    .filter(fieldInputValueDefinitionContext -> fieldInputValueDefinitionContext.name().getText().equals("skipNull"))
                    .findFirst()
                    .filter(objectFieldWithVariableContext -> objectFieldWithVariableContext.valueWithVariable().BooleanValue() != null)
                    .map(objectFieldWithVariableContext -> Boolean.parseBoolean(objectFieldWithVariableContext.valueWithVariable().BooleanValue().getText()))
                    .orElse(false);

            Optional<GraphqlParser.EnumValueContext> operatorEnumValueContext = inputObjectTypeDefinition.get().inputObjectValueDefinitions().inputValueDefinition().stream()
                    .filter(fieldInputValueDefinitionContext ->
                            manager.isEnum(fieldInputValueDefinitionContext.type().getText()) && manager.getFieldTypeName(fieldInputValueDefinitionContext.type()).equals("Operator"))
                    .findFirst()
                    .flatMap(fieldInputValueDefinitionContext -> manager.getObjectFieldWithVariableFromInputValueDefinition(valueWithVariableContext.objectValueWithVariable(), fieldInputValueDefinitionContext))
                    .map(objectFieldWithVariableContext -> objectFieldWithVariableContext.valueWithVariable().enumValue());

            Optional<GraphqlParser.EnumValueContext> defaultOperatorEnumValueContext = inputObjectTypeDefinition.get().inputObjectValueDefinitions().inputValueDefinition().stream()
                    .filter(fieldInputValueDefinitionContext ->
                            manager.isEnum(fieldInputValueDefinitionContext.type().getText()) && manager.getFieldTypeName(fieldInputValueDefinitionContext.type()).equals("Operator"))
                    .findFirst()
                    .flatMap(manager::getDefaultValueFromInputValueDefinition)
                    .map(GraphqlParser.ValueContext::enumValue);

            Optional<GraphqlParser.InputValueDefinitionContext> subInputValueDefinitionContext = inputObjectTypeDefinition.get().inputObjectValueDefinitions().inputValueDefinition().stream()
                    .filter(fieldInputValueDefinitionContext ->
                            !(manager.isEnum(fieldInputValueDefinitionContext.type().getText()) && manager.getFieldTypeName(fieldInputValueDefinitionContext.type()).equals("Operator")))
                    .filter(fieldInputValueDefinitionContext ->
                            valueWithVariableContext.objectValueWithVariable().objectFieldWithVariable().stream()
                                    .anyMatch(objectFieldWithVariableContext -> objectFieldWithVariableContext.name().getText().equals(fieldInputValueDefinitionContext.name().getText()))
                    )
                    .findFirst();

            Optional<GraphqlParser.ValueWithVariableContext> subValueWithVariableContext = subInputValueDefinitionContext
                    .flatMap(fieldInputValueDefinitionContext -> manager.getObjectFieldWithVariableFromInputValueDefinition(valueWithVariableContext.objectValueWithVariable(), fieldInputValueDefinitionContext))
                    .map(GraphqlParser.ObjectFieldWithVariableContext::valueWithVariable);

            Optional<GraphqlParser.ValueContext> subDefaultValueContext = subInputValueDefinitionContext
                    .flatMap(manager::getDefaultValueFromInputValueDefinition);

            if (operatorEnumValueContext.isPresent() && subValueWithVariableContext.isPresent()) {
                return operatorValueWithVariableToExpression(leftExpression, operatorEnumValueContext.get(), subInputValueDefinitionContext.get(), subValueWithVariableContext.get(), skipNull);
            } else if (operatorEnumValueContext.isPresent() && subDefaultValueContext.isPresent()) {
                return operatorValueToExpression(leftExpression, operatorEnumValueContext.get(), subInputValueDefinitionContext.get(), subDefaultValueContext.get(), skipNull);
            } else if (defaultOperatorEnumValueContext.isPresent() && subValueWithVariableContext.isPresent()) {
                return operatorValueWithVariableToExpression(leftExpression, defaultOperatorEnumValueContext.get(), subInputValueDefinitionContext.get(), subValueWithVariableContext.get(), skipNull);
            } else if (defaultOperatorEnumValueContext.isPresent() && subDefaultValueContext.isPresent()) {
                return operatorValueToExpression(leftExpression, defaultOperatorEnumValueContext.get(), subInputValueDefinitionContext.get(), subDefaultValueContext.get(), skipNull);
            } else {
                throw new GraphQLErrors(NON_NULL_VALUE_NOT_EXIST.bind(inputValueDefinitionContext.getText()));
            }
        } else {
            throw new GraphQLErrors(TYPE_NOT_EXIST.bind(manager.getFieldTypeName(inputValueDefinitionContext.type())));
        }
    }

    private Optional<Expression> operatorValueToExpression(Expression leftExpression,
                                                           GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                           GraphqlParser.ValueContext valueContext) {

        if (valueContext.objectValue() == null) {
            throw new GraphQLErrors(UNSUPPORTED_OPERATOR.bind(valueContext.getText()));
        }

        Optional<GraphqlParser.InputObjectTypeDefinitionContext> inputObjectTypeDefinition = manager.getInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type()));

        if (inputObjectTypeDefinition.isPresent()) {

            boolean skipNull = valueContext.objectValue().objectField().stream()
                    .filter(fieldContext -> fieldContext.name().getText().equals("skipNull"))
                    .findFirst()
                    .filter(fieldContext -> fieldContext.value().BooleanValue() != null)
                    .map(fieldContext -> Boolean.parseBoolean(fieldContext.value().BooleanValue().getText()))
                    .orElse(false);

            Optional<GraphqlParser.EnumValueContext> operatorEnumValueContext = inputObjectTypeDefinition.get().inputObjectValueDefinitions().inputValueDefinition().stream()
                    .filter(fieldInputValueDefinitionContext ->
                            manager.isEnum(fieldInputValueDefinitionContext.type().getText()) && manager.getFieldTypeName(fieldInputValueDefinitionContext.type()).equals("Operator"))
                    .findFirst()
                    .flatMap(fieldInputValueDefinitionContext -> manager.getObjectFieldFromInputValueDefinition(valueContext.objectValue(), fieldInputValueDefinitionContext))
                    .map(objectFieldContext -> objectFieldContext.value().enumValue());

            Optional<GraphqlParser.EnumValueContext> defaultOperatorEnumValueContext = inputObjectTypeDefinition.get().inputObjectValueDefinitions().inputValueDefinition().stream()
                    .filter(fieldInputValueDefinitionContext ->
                            manager.isEnum(fieldInputValueDefinitionContext.type().getText()) && manager.getFieldTypeName(fieldInputValueDefinitionContext.type()).equals("Operator")
                    )
                    .findFirst()
                    .flatMap(manager::getDefaultValueFromInputValueDefinition)
                    .map(GraphqlParser.ValueContext::enumValue);

            Optional<GraphqlParser.InputValueDefinitionContext> subInputValueDefinitionContext = inputObjectTypeDefinition.get().inputObjectValueDefinitions().inputValueDefinition().stream()
                    .filter(fieldInputValueDefinitionContext ->
                            !(manager.isEnum(fieldInputValueDefinitionContext.type().getText()) && manager.getFieldTypeName(fieldInputValueDefinitionContext.type()).equals("Operator"))
                    )
                    .filter(fieldInputValueDefinitionContext ->
                            valueContext.objectValue().objectField().stream()
                                    .anyMatch(objectFieldContext -> objectFieldContext.name().getText().equals(fieldInputValueDefinitionContext.name().getText()))
                    )
                    .findFirst();

            Optional<GraphqlParser.ValueContext> subValueContext = subInputValueDefinitionContext
                    .flatMap(fieldInputValueDefinitionContext -> manager.getObjectFieldFromInputValueDefinition(valueContext.objectValue(), fieldInputValueDefinitionContext))
                    .map(GraphqlParser.ObjectFieldContext::value);

            Optional<GraphqlParser.ValueContext> subDefaultValueContext = subInputValueDefinitionContext
                    .flatMap(manager::getDefaultValueFromInputValueDefinition);

            if (operatorEnumValueContext.isPresent() && subValueContext.isPresent()) {
                return operatorValueToExpression(leftExpression, operatorEnumValueContext.get(), subInputValueDefinitionContext.get(), subValueContext.get(), skipNull);
            } else if (operatorEnumValueContext.isPresent() && subDefaultValueContext.isPresent()) {
                return operatorValueToExpression(leftExpression, operatorEnumValueContext.get(), subInputValueDefinitionContext.get(), subDefaultValueContext.get(), skipNull);
            } else if (defaultOperatorEnumValueContext.isPresent() && subValueContext.isPresent()) {
                return operatorValueToExpression(leftExpression, defaultOperatorEnumValueContext.get(), subInputValueDefinitionContext.get(), subValueContext.get(), skipNull);
            } else if (defaultOperatorEnumValueContext.isPresent() && subDefaultValueContext.isPresent()) {
                return operatorValueToExpression(leftExpression, defaultOperatorEnumValueContext.get(), subInputValueDefinitionContext.get(), subDefaultValueContext.get(), skipNull);
            } else {
                throw new GraphQLErrors(NON_NULL_VALUE_NOT_EXIST.bind(inputValueDefinitionContext.getText()));
            }
        } else {
            throw new GraphQLErrors(TYPE_NOT_EXIST.bind(manager.getFieldTypeName(inputValueDefinitionContext.type())));
        }
    }

    private Optional<Expression> operatorValueWithVariableToExpression(Expression leftExpression,
                                                                       GraphqlParser.EnumValueContext enumValueContext,
                                                                       GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                                       GraphqlParser.ValueWithVariableContext valueWithVariableContext,
                                                                       boolean skipNull) {

        if (manager.fieldTypeIsList(inputValueDefinitionContext.type()) && valueWithVariableContext.variable() != null) {
            return Optional.of(operatorValueWithVariableToInExpression(leftExpression, enumValueContext, inputValueDefinitionContext, valueWithVariableContext, skipNull));
        }
        if (valueWithVariableContext.arrayValueWithVariable() != null) {
            return Optional.of(operatorValueWithVariableToInExpression(leftExpression, enumValueContext, inputValueDefinitionContext, valueWithVariableContext, skipNull));
        }
        if (valueWithVariableContext.enumValue() != null) {
            return operatorEnumValueWithVariableToExpression(leftExpression, enumValueContext, valueWithVariableContext, skipNull);
        }
        return operatorScalarValueToExpression(
                leftExpression,
                enumValueContext,
                valueWithVariableContext.StringValue(),
                valueWithVariableContext.IntValue(),
                valueWithVariableContext.FloatValue(),
                valueWithVariableContext.BooleanValue(),
                valueWithVariableContext.NullValue(),
                valueWithVariableContext.variable(),
                skipNull
        );
    }

    private Optional<Expression> operatorValueToExpression(Expression leftExpression,
                                                           GraphqlParser.EnumValueContext enumValueContext,
                                                           GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                           GraphqlParser.ValueContext valueContext,
                                                           boolean skipNull) {
        if (valueContext.arrayValue() != null) {
            return Optional.of(operatorValueToInExpression(leftExpression, enumValueContext, valueContext, skipNull));
        }
        if (valueContext.enumValue() != null) {
            return operatorEnumValueToExpression(leftExpression, enumValueContext, valueContext, skipNull);
        }
        return operatorScalarValueToExpression(
                leftExpression,
                enumValueContext,
                valueContext.StringValue(),
                valueContext.IntValue(),
                valueContext.FloatValue(),
                valueContext.BooleanValue(),
                valueContext.NullValue(),
                null,
                skipNull
        );
    }

    private Expression operatorValueToInExpression(Expression leftExpression,
                                                   GraphqlParser.EnumValueContext enumValueContext,
                                                   GraphqlParser.ValueContext valueContext,
                                                   boolean skipNull) {

        if ("BT".equals(enumValueContext.enumValueName().getText()) || "NBT".equals(enumValueContext.enumValueName().getText())) {
            List<List<GraphqlParser.ValueContext>> betweenGroup = Lists.partition(valueContext.arrayValue().value(), 2);
            return new MultiOrExpression(
                    betweenGroup.stream()
                            .map(valueContextList -> {
                                        if (valueContextList.size() == 2) {
                                            if ("BT".equals(enumValueContext.enumValueName().getText())) {
                                                return new MultiAndExpression(
                                                        Arrays.asList(
                                                                new GreaterThanEquals()
                                                                        .withLeftExpression(leftExpression)
                                                                        .withRightExpression(dbValueUtil.valueToDBValue(valueContextList.get(0))),
                                                                new MinorThanEquals()
                                                                        .withLeftExpression(leftExpression)
                                                                        .withRightExpression(dbValueUtil.valueToDBValue(valueContextList.get(1)))
                                                        )
                                                );
                                            } else {
                                                return new MultiAndExpression(
                                                        Arrays.asList(
                                                                new MinorThanEquals()
                                                                        .withLeftExpression(leftExpression)
                                                                        .withRightExpression(dbValueUtil.valueToDBValue(valueContextList.get(0))),
                                                                new GreaterThanEquals()
                                                                        .withLeftExpression(leftExpression)
                                                                        .withRightExpression(dbValueUtil.valueToDBValue(valueContextList.get(1)))
                                                        )
                                                );
                                            }
                                        } else {
                                            if ("BT".equals(enumValueContext.enumValueName().getText())) {
                                                return new GreaterThanEquals()
                                                        .withLeftExpression(leftExpression)
                                                        .withRightExpression(dbValueUtil.valueToDBValue(valueContextList.get(0)));
                                            } else {
                                                return new MinorThanEquals()
                                                        .withLeftExpression(leftExpression)
                                                        .withRightExpression(dbValueUtil.valueToDBValue(valueContextList.get(0)));
                                            }
                                        }
                                    }
                            )
                            .collect(Collectors.toList())
            );
        } else {
            InExpression inExpression = new InExpression();
            inExpression.setLeftExpression(leftExpression);
            inExpression.setRightItemsList(
                    skipNull ?
                            new ExpressionList(
                                    valueContext.arrayValue().value().stream()
                                            .filter(item -> item.NullValue() == null)
                                            .map(dbValueUtil::valueToDBValue)
                                            .collect(Collectors.toList())
                            ) :
                            new ExpressionList(
                                    valueContext.arrayValue().value().stream()
                                            .map(dbValueUtil::valueToDBValue)
                                            .collect(Collectors.toList())
                            )
            );
            if ("NIN".equals(enumValueContext.enumValueName().getText())) {
                inExpression.setNot(true);
            }
            return inExpression;
        }
    }

    private Expression operatorValueWithVariableToInExpression(Expression leftExpression,
                                                               GraphqlParser.EnumValueContext enumValueContext,
                                                               GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                               GraphqlParser.ValueWithVariableContext valueWithVariableContext,
                                                               boolean skipNull) {
        if ("BT".equals(enumValueContext.enumValueName().getText()) || "NBT".equals(enumValueContext.enumValueName().getText())) {
            if (valueWithVariableContext.variable() != null) {
                if ("BT".equals(enumValueContext.enumValueName().getText())) {
                    MultiAndExpression multiAndExpression = new MultiAndExpression(
                            Arrays.asList(
                                    new GreaterThanEquals()
                                            .withLeftExpression(leftExpression)
                                            .withRightExpression(dbValueUtil.arrayFieldVariableToDBValue(0, valueWithVariableContext)),
                                    new MinorThanEquals()
                                            .withLeftExpression(leftExpression)
                                            .withRightExpression(dbValueUtil.arrayFieldVariableToDBValue(1, valueWithVariableContext))
                            )
                    );
                    return skipNull ? skipNullExpression(dbValueUtil.variableToJdbcNamedParameter(valueWithVariableContext.variable()), multiAndExpression) : multiAndExpression;
                } else {
                    MultiAndExpression multiAndExpression = new MultiAndExpression(
                            Arrays.asList(
                                    new MinorThanEquals()
                                            .withLeftExpression(leftExpression)
                                            .withRightExpression(dbValueUtil.arrayFieldVariableToDBValue(0, valueWithVariableContext)),
                                    new GreaterThanEquals()
                                            .withLeftExpression(leftExpression)
                                            .withRightExpression(dbValueUtil.arrayFieldVariableToDBValue(1, valueWithVariableContext))
                            )
                    );
                    return skipNull ? skipNullExpression(dbValueUtil.variableToJdbcNamedParameter(valueWithVariableContext.variable()), multiAndExpression) : multiAndExpression;
                }
            } else {
                List<List<GraphqlParser.ValueWithVariableContext>> betweenGroup = Lists.partition(valueWithVariableContext.arrayValueWithVariable().valueWithVariable(), 2);
                return new MultiOrExpression(
                        betweenGroup.stream()
                                .map(valueWithVariableContextList -> {
                                            if (valueWithVariableContextList.size() == 2) {
                                                if ("BT".equals(enumValueContext.enumValueName().getText())) {
                                                    return new MultiAndExpression(
                                                            Arrays.asList(
                                                                    new GreaterThanEquals()
                                                                            .withLeftExpression(leftExpression)
                                                                            .withRightExpression(dbValueUtil.valueWithVariableToDBValue(valueWithVariableContextList.get(0))),
                                                                    new MinorThanEquals()
                                                                            .withLeftExpression(leftExpression)
                                                                            .withRightExpression(dbValueUtil.valueWithVariableToDBValue(valueWithVariableContextList.get(1)))
                                                            )
                                                    );
                                                } else {
                                                    return new MultiAndExpression(
                                                            Arrays.asList(
                                                                    new MinorThanEquals()
                                                                            .withLeftExpression(leftExpression)
                                                                            .withRightExpression(dbValueUtil.valueWithVariableToDBValue(valueWithVariableContextList.get(0))),
                                                                    new GreaterThanEquals()
                                                                            .withLeftExpression(leftExpression)
                                                                            .withRightExpression(dbValueUtil.valueWithVariableToDBValue(valueWithVariableContextList.get(1)))
                                                            )
                                                    );
                                                }
                                            } else {
                                                if ("BT".equals(enumValueContext.enumValueName().getText())) {
                                                    return new GreaterThanEquals()
                                                            .withLeftExpression(leftExpression)
                                                            .withRightExpression(dbValueUtil.valueWithVariableToDBValue(valueWithVariableContextList.get(0)));
                                                } else {
                                                    return new MinorThanEquals()
                                                            .withLeftExpression(leftExpression)
                                                            .withRightExpression(dbValueUtil.valueWithVariableToDBValue(valueWithVariableContextList.get(0)));
                                                }
                                            }
                                        }
                                )
                                .collect(Collectors.toList())
                );
            }
        } else {
            InExpression inExpression = new InExpression();
            inExpression.setLeftExpression(leftExpression);
            if ("NIN".equals(enumValueContext.enumValueName().getText())) {
                inExpression.setNot(true);
            }
            if (valueWithVariableContext.variable() != null) {
                inExpression.setRightExpression(selectVariablesFromJsonArray(inputValueDefinitionContext, valueWithVariableContext));
                return skipNull ? skipNullExpression(dbValueUtil.variableToJdbcNamedParameter(valueWithVariableContext.variable()), inExpression) : inExpression;
            } else {
                inExpression.setRightItemsList(
                        skipNull ?
                                new ExpressionList(
                                        valueWithVariableContext.arrayValueWithVariable().valueWithVariable().stream()
                                                .filter(item -> item.NullValue() == null)
                                                .map(dbValueUtil::valueWithVariableToDBValue)
                                                .collect(Collectors.toList())
                                ) :
                                new ExpressionList(
                                        valueWithVariableContext.arrayValueWithVariable().valueWithVariable().stream()
                                                .map(dbValueUtil::valueWithVariableToDBValue)
                                                .collect(Collectors.toList())
                                )
                );
                return inExpression;
            }
        }
    }

    private Optional<Expression> operatorEnumValueToExpression(Expression leftExpression,
                                                               GraphqlParser.EnumValueContext operator,
                                                               GraphqlParser.ValueContext valueContext,
                                                               boolean skipNull) {
        if (skipNull && valueContext.NullValue() != null) {
            return Optional.empty();
        }
        Expression expression;
        switch (operator.enumValueName().getText()) {
            case "EQ":
                expression = enumValueToExpression(leftExpression, valueContext.enumValue());
                break;
            case "NEQ":
                expression = new NotExpression(enumValueToExpression(leftExpression, valueContext.enumValue()));
                break;
            case "LK":
                LikeExpression likeExpression = new LikeExpression();
                likeExpression.setLeftExpression(leftExpression);
                likeExpression.setRightExpression(dbValueUtil.enumValueToDBValue(valueContext));
                expression = likeExpression;
                break;
            case "NLK":
                LikeExpression notLikeExpression = new LikeExpression();
                notLikeExpression.setNot(true);
                notLikeExpression.setLeftExpression(leftExpression);
                notLikeExpression.setRightExpression(dbValueUtil.enumValueToDBValue(valueContext));
                expression = notLikeExpression;
                break;
            case "GT":
            case "NLTE":
                GreaterThan greaterThan = new GreaterThan();
                greaterThan.setLeftExpression(leftExpression);
                greaterThan.setRightExpression(dbValueUtil.enumValueToDBValue(valueContext));
                expression = greaterThan;
                break;
            case "GTE":
            case "NLT":
                GreaterThanEquals greaterThanEquals = new GreaterThanEquals();
                greaterThanEquals.setLeftExpression(leftExpression);
                greaterThanEquals.setRightExpression(dbValueUtil.enumValueToDBValue(valueContext));
                expression = greaterThanEquals;
                break;
            case "LT":
            case "NGTE":
                MinorThan minorThan = new MinorThan();
                minorThan.setLeftExpression(leftExpression);
                minorThan.setRightExpression(dbValueUtil.enumValueToDBValue(valueContext));
                expression = minorThan;
                break;
            case "LTE":
            case "NGT":
                MinorThanEquals minorThanEquals = new MinorThanEquals();
                minorThanEquals.setLeftExpression(leftExpression);
                minorThanEquals.setRightExpression(dbValueUtil.enumValueToDBValue(valueContext));
                expression = minorThanEquals;
                break;
            case "NIL":
                IsNullExpression isNullExpression = new IsNullExpression();
                isNullExpression.setLeftExpression(leftExpression);
                expression = isNullExpression;
                break;
            case "NNIL":
                IsNullExpression isNotNullExpression = new IsNullExpression();
                isNotNullExpression.setNot(true);
                isNotNullExpression.setLeftExpression(leftExpression);
                expression = isNotNullExpression;
                break;
            default:
                throw new GraphQLErrors(UNSUPPORTED_VALUE.bind(operator.enumValueName().getText()));
        }
        return Optional.of(expression);
    }

    private Optional<Expression> operatorEnumValueWithVariableToExpression(Expression leftExpression,
                                                                           GraphqlParser.EnumValueContext operator,
                                                                           GraphqlParser.ValueWithVariableContext valueWithVariableContext,
                                                                           boolean skipNull) {
        if (skipNull && valueWithVariableContext.NullValue() != null) {
            return Optional.empty();
        }
        Expression expression;
        switch (operator.enumValueName().getText()) {
            case "EQ":
                expression = enumValueWithVariableToExpression(leftExpression, valueWithVariableContext);
                break;
            case "NEQ":
                expression = new NotExpression(enumValueWithVariableToExpression(leftExpression, valueWithVariableContext));
                break;
            case "LK":
                LikeExpression likeExpression = new LikeExpression();
                likeExpression.setLeftExpression(leftExpression);
                likeExpression.setRightExpression(dbValueUtil.enumValueWithVariableToDBValue(valueWithVariableContext));
                expression = likeExpression;
                break;
            case "NLK":
                LikeExpression notLikeExpression = new LikeExpression();
                notLikeExpression.setNot(true);
                notLikeExpression.setLeftExpression(leftExpression);
                notLikeExpression.setRightExpression(dbValueUtil.enumValueWithVariableToDBValue(valueWithVariableContext));
                expression = notLikeExpression;
                break;
            case "GT":
            case "NLTE":
                GreaterThan greaterThan = new GreaterThan();
                greaterThan.setLeftExpression(leftExpression);
                greaterThan.setRightExpression(dbValueUtil.enumValueWithVariableToDBValue(valueWithVariableContext));
                expression = greaterThan;
                break;
            case "GTE":
            case "NLT":
                GreaterThanEquals greaterThanEquals = new GreaterThanEquals();
                greaterThanEquals.setLeftExpression(leftExpression);
                greaterThanEquals.setRightExpression(dbValueUtil.enumValueWithVariableToDBValue(valueWithVariableContext));
                expression = greaterThanEquals;
                break;
            case "LT":
            case "NGTE":
                MinorThan minorThan = new MinorThan();
                minorThan.setLeftExpression(leftExpression);
                minorThan.setRightExpression(dbValueUtil.enumValueWithVariableToDBValue(valueWithVariableContext));
                expression = minorThan;
                break;
            case "LTE":
            case "NGT":
                MinorThanEquals minorThanEquals = new MinorThanEquals();
                minorThanEquals.setLeftExpression(leftExpression);
                minorThanEquals.setRightExpression(dbValueUtil.enumValueWithVariableToDBValue(valueWithVariableContext));
                expression = minorThanEquals;
                break;
            case "NIL":
                IsNullExpression isNullExpression = new IsNullExpression();
                isNullExpression.setLeftExpression(leftExpression);
                expression = isNullExpression;
                break;
            case "NNIL":
                IsNullExpression isNotNullExpression = new IsNullExpression();
                isNotNullExpression.setNot(true);
                isNotNullExpression.setLeftExpression(leftExpression);
                expression = isNotNullExpression;
                break;
            default:
                throw new GraphQLErrors(UNSUPPORTED_VALUE.bind(operator.enumValueName().getText()));
        }
        if (skipNull) {
            return Optional.of(skipNullExpression(dbValueUtil.enumValueWithVariableToDBValue(valueWithVariableContext), expression));
        } else {
            return Optional.of(expression);
        }
    }

    private Optional<Expression> operatorScalarValueToExpression(Expression leftExpression,
                                                                 GraphqlParser.EnumValueContext operator,
                                                                 TerminalNode stringValue,
                                                                 TerminalNode intValue,
                                                                 TerminalNode floatValue,
                                                                 TerminalNode booleanValue,
                                                                 TerminalNode nullValue,
                                                                 GraphqlParser.VariableContext variableContext,
                                                                 boolean skipNull) {
        if (skipNull && nullValue != null) {
            return Optional.empty();
        }
        Expression expression;
        switch (operator.enumValueName().getText()) {
            case "EQ":
                expression = scalarValueToExpression(leftExpression, stringValue, intValue, floatValue, booleanValue, nullValue, variableContext);
                break;
            case "NEQ":
                expression = new NotExpression(scalarValueToExpression(leftExpression, stringValue, intValue, floatValue, booleanValue, nullValue, variableContext));
                break;
            case "LK":
                LikeExpression likeExpression = new LikeExpression();
                likeExpression.setLeftExpression(leftExpression);
                likeExpression.setRightExpression(dbValueUtil.scalarValueToDBValue(stringValue, intValue, floatValue, booleanValue, nullValue, variableContext));
                expression = likeExpression;
                break;
            case "NLK":
                LikeExpression notLikeExpression = new LikeExpression();
                notLikeExpression.setNot(true);
                notLikeExpression.setLeftExpression(leftExpression);
                notLikeExpression.setRightExpression(dbValueUtil.scalarValueToDBValue(stringValue, intValue, floatValue, booleanValue, nullValue, variableContext));
                expression = notLikeExpression;
                break;
            case "GT":
            case "NLTE":
                GreaterThan greaterThan = new GreaterThan();
                greaterThan.setLeftExpression(leftExpression);
                greaterThan.setRightExpression(dbValueUtil.scalarValueToDBValue(stringValue, intValue, floatValue, booleanValue, nullValue, variableContext));
                expression = greaterThan;
                break;
            case "GTE":
            case "NLT":
                GreaterThanEquals greaterThanEquals = new GreaterThanEquals();
                greaterThanEquals.setLeftExpression(leftExpression);
                greaterThanEquals.setRightExpression(dbValueUtil.scalarValueToDBValue(stringValue, intValue, floatValue, booleanValue, nullValue, variableContext));
                expression = greaterThanEquals;
                break;
            case "LT":
            case "NGTE":
                MinorThan minorThan = new MinorThan();
                minorThan.setLeftExpression(leftExpression);
                minorThan.setRightExpression(dbValueUtil.scalarValueToDBValue(stringValue, intValue, floatValue, booleanValue, nullValue, variableContext));
                expression = minorThan;
                break;
            case "LTE":
            case "NGT":
                MinorThanEquals minorThanEquals = new MinorThanEquals();
                minorThanEquals.setLeftExpression(leftExpression);
                minorThanEquals.setRightExpression(dbValueUtil.scalarValueToDBValue(stringValue, intValue, floatValue, booleanValue, nullValue, variableContext));
                expression = minorThanEquals;
                break;
            case "NIL":
                IsNullExpression isNullExpression = new IsNullExpression();
                isNullExpression.setLeftExpression(leftExpression);
                expression = isNullExpression;
                break;
            case "NNIL":
                IsNullExpression isNotNullExpression = new IsNullExpression();
                isNotNullExpression.setNot(true);
                isNotNullExpression.setLeftExpression(leftExpression);
                expression = isNotNullExpression;
                break;
            default:
                throw new GraphQLErrors(UNSUPPORTED_VALUE.bind(operator.enumValueName().getText()));
        }
        if (skipNull) {
            return Optional.of(skipNullExpression(dbValueUtil.scalarValueToDBValue(stringValue, intValue, floatValue, booleanValue, nullValue, variableContext), expression));
        } else {
            return Optional.of(expression);
        }
    }

    protected Expression objectValueWithVariableToExpression(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext,
                                                             GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                             GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                             GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext,
                                                             int level) {

        PlainSelect body = mapFieldPlainSelect(objectTypeDefinitionContext, fieldDefinitionContext, level);
        Optional<Expression> subWhereExpression = objectValueWithVariableToMultipleExpression(fieldDefinitionContext.type(), inputValueDefinitionContext, objectValueWithVariableContext, level);
        subWhereExpression
                .ifPresent(expression -> {
                            if (body.getWhere() != null) {
                                body.setWhere(new MultiAndExpression(Arrays.asList(body.getWhere(), expression)));
                            } else {
                                body.setWhere(expression);
                            }
                        }
                );
        return existsExpression(body);
    }

    protected Expression objectValueToExpression(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext,
                                                 GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                 GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                 GraphqlParser.ObjectValueContext objectValueContext,
                                                 int level) {

        PlainSelect body = mapFieldPlainSelect(objectTypeDefinitionContext, fieldDefinitionContext, level);
        Optional<Expression> subWhereExpression = objectValueToMultipleExpression(fieldDefinitionContext.type(), inputValueDefinitionContext, objectValueContext, level);
        subWhereExpression
                .ifPresent(expression -> {
                            if (body.getWhere() != null) {
                                body.setWhere(new MultiAndExpression(Arrays.asList(body.getWhere(), expression)));
                            } else {
                                body.setWhere(expression);
                            }
                        }
                );
        return existsExpression(body);
    }

    protected Optional<Expression> objectValueWithVariableToWhereExpression(GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                                            GraphqlParser.ArgumentsContext argumentsContext) {

        return fieldDefinitionContext.argumentsDefinition().inputValueDefinition().stream()
                .filter(inputValueDefinitionContext -> inputValueDefinitionContext.name().getText().equals(WHERE_INPUT_NAME))
                .findFirst()
                .flatMap(inputValueDefinitionContext ->
                        Stream.ofNullable(argumentsContext)
                                .map(GraphqlParser.ArgumentsContext::argument)
                                .flatMap(Collection::stream)
                                .filter(argumentContext -> argumentContext.name().getText().equals(inputValueDefinitionContext.name().getText()))
                                .filter(argumentContext -> argumentContext.valueWithVariable().objectValueWithVariable() != null)
                                .findFirst()
                                .flatMap(argumentContext ->
                                        objectValueWithVariableToMultipleExpression(
                                                fieldDefinitionContext.type(),
                                                inputValueDefinitionContext,
                                                argumentContext.valueWithVariable().objectValueWithVariable(),
                                                1
                                        )
                                )
                );
    }

    protected PlainSelect mapFieldPlainSelect(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext,
                                              GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                              int level) {
        PlainSelect body = new PlainSelect();
        body.addSelectItems(new AllColumns());

        String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.type());
        Table table = dbNameUtil.typeToTable(fieldTypeName, level);
        body.setFromItem(table);

        String parentTypeName = objectTypeDefinitionContext.name().getText();
        String fieldName = fieldDefinitionContext.name().getText();

        Optional<GraphqlParser.FieldDefinitionContext> fromFieldDefinition = mapper.getFromFieldDefinition(parentTypeName, fieldName);
        Optional<GraphqlParser.FieldDefinitionContext> toFieldDefinition = mapper.getToFieldDefinition(parentTypeName, fieldName);

        if (fromFieldDefinition.isPresent() && toFieldDefinition.isPresent()) {
            Table parentTable = dbNameUtil.typeToTable(objectTypeDefinitionContext, level - 1);
            EqualsTo idEqualsTo = new EqualsTo();
            idEqualsTo.setLeftExpression(dbNameUtil.fieldToColumn(table, toFieldDefinition.get()));
            boolean mapWithType = mapper.mapWithType(parentTypeName, fieldName);

            if (mapWithType) {
                Optional<GraphqlParser.ObjectTypeDefinitionContext> mapWithObjectDefinition = mapper.getWithObjectTypeDefinition(parentTypeName, fieldName);
                Optional<GraphqlParser.FieldDefinitionContext> mapWithFromFieldDefinition = mapper.getWithFromFieldDefinition(parentTypeName, fieldName);
                Optional<GraphqlParser.FieldDefinitionContext> mapWithToFieldDefinition = mapper.getWithToFieldDefinition(parentTypeName, fieldName);

                if (mapWithObjectDefinition.isPresent() && mapWithFromFieldDefinition.isPresent() && mapWithToFieldDefinition.isPresent()) {
                    Table withTable = dbNameUtil.typeToTable(mapWithObjectDefinition.get(), level);
                    SubSelect selectWithTable = new SubSelect();
                    PlainSelect subPlainSelect = new PlainSelect();
                    subPlainSelect.addSelectItems(new SelectExpressionItem(dbNameUtil.fieldToColumn(withTable, mapWithToFieldDefinition.get())));
                    EqualsTo equalsWithTableColumn = new EqualsTo();
                    equalsWithTableColumn.setLeftExpression(dbNameUtil.fieldToColumn(withTable, mapWithFromFieldDefinition.get()));
                    equalsWithTableColumn.setRightExpression(dbNameUtil.fieldToColumn(parentTable, fromFieldDefinition.get()));
                    subPlainSelect.setWhere(equalsWithTableColumn);
                    subPlainSelect.setFromItem(withTable);
                    selectWithTable.setSelectBody(subPlainSelect);
                    idEqualsTo.setRightExpression(selectWithTable);
                } else {
                    GraphQLErrors graphQLErrors = new GraphQLErrors();
                    if (mapWithObjectDefinition.isEmpty()) {
                        graphQLErrors.add(MAP_WITH_TYPE_NOT_EXIST.bind(fieldDefinitionContext.getText()));
                    }
                    if (mapWithFromFieldDefinition.isEmpty()) {
                        graphQLErrors.add(MAP_WITH_FROM_FIELD_NOT_EXIST.bind(fieldDefinitionContext.getText()));
                    }
                    if (mapWithToFieldDefinition.isEmpty()) {
                        graphQLErrors.add(MAP_WITH_TO_FIELD_NOT_EXIST.bind(fieldDefinitionContext.getText()));
                    }
                    throw graphQLErrors;
                }
            } else {
                idEqualsTo.setRightExpression(dbNameUtil.fieldToColumn(parentTable, fromFieldDefinition.get()));
            }
            body.setWhere(idEqualsTo);
        } else {
            GraphQLErrors graphQLErrors = new GraphQLErrors();
            if (fromFieldDefinition.isEmpty()) {
                graphQLErrors.add(MAP_FROM_FIELD_NOT_EXIST.bind(fieldDefinitionContext.getText()));
            }
            if (toFieldDefinition.isEmpty()) {
                graphQLErrors.add(MAP_TO_FIELD_NOT_EXIST.bind(fieldDefinitionContext.getText()));
            }
            throw graphQLErrors;
        }
        return body;
    }

    protected PlainSelect mapWithTypeToFieldPlainSelect(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext,
                                                        GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                        Expression expression,
                                                        int level) {
        String parentTypeName = objectTypeDefinitionContext.name().getText();
        String fieldName = fieldDefinitionContext.name().getText();

        Optional<GraphqlParser.FieldDefinitionContext> fromFieldDefinition = mapper.getFromFieldDefinition(parentTypeName, fieldName);

        if (fromFieldDefinition.isPresent()) {
            Table parentTable = dbNameUtil.typeToTable(objectTypeDefinitionContext, level - 1);
            boolean mapWithType = mapper.mapWithType(parentTypeName, fieldName);

            if (mapWithType) {
                Optional<GraphqlParser.ObjectTypeDefinitionContext> mapWithObjectDefinition = mapper.getWithObjectTypeDefinition(parentTypeName, fieldName);
                Optional<GraphqlParser.FieldDefinitionContext> mapWithFromFieldDefinition = mapper.getWithFromFieldDefinition(parentTypeName, fieldName);
                Optional<GraphqlParser.FieldDefinitionContext> mapWithToFieldDefinition = mapper.getWithToFieldDefinition(parentTypeName, fieldName);

                if (mapWithObjectDefinition.isPresent() && mapWithFromFieldDefinition.isPresent() && mapWithToFieldDefinition.isPresent()) {
                    Table withTable = dbNameUtil.typeToTable(mapWithObjectDefinition.get(), level);
                    EqualsTo equalsWithTableColumn = new EqualsTo();
                    equalsWithTableColumn.setLeftExpression(dbNameUtil.fieldToColumn(withTable, mapWithFromFieldDefinition.get()));
                    equalsWithTableColumn.setRightExpression(dbNameUtil.fieldToColumn(parentTable, fromFieldDefinition.get()));

                    PlainSelect body = new PlainSelect();
                    body.addSelectItems(new AllColumns());
                    body.setFromItem(withTable);
                    body.setWhere(new MultiAndExpression(Arrays.asList(equalsWithTableColumn, expression)));
                    return body;
                } else {
                    GraphQLErrors graphQLErrors = new GraphQLErrors();
                    if (mapWithObjectDefinition.isEmpty()) {
                        graphQLErrors.add(MAP_WITH_TYPE_NOT_EXIST.bind(fieldDefinitionContext.getText()));
                    }
                    if (mapWithFromFieldDefinition.isEmpty()) {
                        graphQLErrors.add(MAP_WITH_FROM_FIELD_NOT_EXIST.bind(fieldDefinitionContext.getText()));
                    }
                    if (mapWithToFieldDefinition.isEmpty()) {
                        graphQLErrors.add(MAP_WITH_TO_FIELD_NOT_EXIST.bind(fieldDefinitionContext.getText()));
                    }
                    throw graphQLErrors;
                }
            } else {
                throw new GraphQLErrors(MAP_WITH_TYPE_NOT_EXIST.bind(fieldDefinitionContext.getText()));
            }
        } else {
            GraphQLErrors graphQLErrors = new GraphQLErrors();
            graphQLErrors.add(MAP_FROM_FIELD_NOT_EXIST.bind(fieldDefinitionContext.getText()));
            throw graphQLErrors;
        }
    }

    protected Column mapWithToColumn(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext,
                                     GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                     int level) {
        String parentTypeName = objectTypeDefinitionContext.name().getText();
        String fieldName = fieldDefinitionContext.name().getText();
        boolean mapWithType = mapper.mapWithType(parentTypeName, fieldName);
        if (mapWithType) {
            Optional<GraphqlParser.ObjectTypeDefinitionContext> mapWithObjectDefinition = mapper.getWithObjectTypeDefinition(parentTypeName, fieldName);
            Optional<GraphqlParser.FieldDefinitionContext> mapWithToFieldDefinition = mapper.getWithToFieldDefinition(parentTypeName, fieldName);
            if (mapWithObjectDefinition.isPresent() && mapWithToFieldDefinition.isPresent()) {
                return dbNameUtil.fieldToColumn(mapWithObjectDefinition.get().name().getText(), mapWithToFieldDefinition.get(), level);
            } else {
                GraphQLErrors graphQLErrors = new GraphQLErrors();
                if (mapWithObjectDefinition.isEmpty()) {
                    graphQLErrors.add(MAP_WITH_TYPE_NOT_EXIST.bind(fieldDefinitionContext.getText()));
                }
                if (mapWithToFieldDefinition.isEmpty()) {
                    graphQLErrors.add(MAP_WITH_TO_FIELD_NOT_EXIST.bind(fieldDefinitionContext.getText()));
                }
                throw graphQLErrors;
            }
        } else {
            throw new GraphQLErrors(MAP_WITH_TYPE_NOT_EXIST.bind(fieldDefinitionContext.getText()));
        }
    }

    protected Column argumentToColumn(GraphqlParser.TypeContext typeContext, GraphqlParser.ArgumentContext argumentContext, int level) {
        return dbNameUtil.fieldToColumn(manager.getFieldTypeName(typeContext), argumentContext, level);
    }

    protected Column inputValueToColumn(GraphqlParser.TypeContext typeContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext, int level) {
        return dbNameUtil.fieldToColumn(manager.getFieldTypeName(typeContext), inputValueDefinitionContext, level);
    }

    protected Column objectFieldWithVariableToColumn(GraphqlParser.TypeContext typeContext, GraphqlParser.ObjectFieldWithVariableContext objectFieldWithVariableContext, int level) {
        return dbNameUtil.fieldToColumn(manager.getFieldTypeName(typeContext), objectFieldWithVariableContext, level);
    }

    protected Column objectFieldToColumn(GraphqlParser.TypeContext typeContext, GraphqlParser.ObjectFieldContext objectFieldContext, int level) {
        return dbNameUtil.fieldToColumn(manager.getFieldTypeName(typeContext), objectFieldContext, level);
    }

    protected Expression enumValueToExpression(Expression leftExpression,
                                               GraphqlParser.EnumValueContext enumValueContext) {

        EqualsTo equalsTo = new EqualsTo();
        equalsTo.setLeftExpression(leftExpression);
        equalsTo.setRightExpression(new StringValue(enumValueContext.enumValueName().getText()));
        return equalsTo;
    }

    protected Expression enumValueWithVariableToExpression(Expression leftExpression,
                                                           GraphqlParser.ValueWithVariableContext valueWithVariableContext) {

        EqualsTo equalsTo = new EqualsTo();
        equalsTo.setLeftExpression(leftExpression);
        equalsTo.setRightExpression(dbValueUtil.enumValueWithVariableToDBValue(valueWithVariableContext));
        return equalsTo;
    }

    protected Expression scalarValueToExpression(Expression leftExpression,
                                                 GraphqlParser.ValueContext valueContext) {
        return scalarValueToExpression(
                leftExpression,
                valueContext.StringValue(),
                valueContext.IntValue(),
                valueContext.FloatValue(),
                valueContext.BooleanValue(),
                valueContext.NullValue(),
                null
        );
    }

    protected Expression scalarValueWithVariableToExpression(Expression leftExpression,
                                                             GraphqlParser.ValueWithVariableContext valueWithVariableContext) {
        return scalarValueToExpression(leftExpression,
                valueWithVariableContext.StringValue(),
                valueWithVariableContext.IntValue(),
                valueWithVariableContext.FloatValue(),
                valueWithVariableContext.BooleanValue(),
                valueWithVariableContext.NullValue(),
                valueWithVariableContext.variable());
    }

    protected Expression scalarValueToExpression(Expression leftExpression,
                                                 TerminalNode stringValue,
                                                 TerminalNode intValue,
                                                 TerminalNode floatValue,
                                                 TerminalNode booleanValue,
                                                 TerminalNode nullValue,
                                                 GraphqlParser.VariableContext variableContext) {
        if (stringValue != null) {
            EqualsTo equalsTo = new EqualsTo();
            equalsTo.setLeftExpression(leftExpression);
            equalsTo.setRightExpression(new StringValue(CharMatcher.is('"').trimFrom(stringValue.getText())));
            return equalsTo;
        } else if (intValue != null) {
            EqualsTo equalsTo = new EqualsTo();
            equalsTo.setLeftExpression(leftExpression);
            equalsTo.setRightExpression(new LongValue(intValue.getText()));
            return equalsTo;
        } else if (floatValue != null) {
            EqualsTo equalsTo = new EqualsTo();
            equalsTo.setLeftExpression(leftExpression);
            equalsTo.setRightExpression(new DoubleValue(floatValue.getText()));
            return equalsTo;
        } else if (booleanValue != null) {
            IsBooleanExpression isBooleanExpression = new IsBooleanExpression();
            isBooleanExpression.setLeftExpression(leftExpression);
            isBooleanExpression.setIsTrue(Boolean.parseBoolean(booleanValue.getText()));
            return isBooleanExpression;
        } else if (nullValue != null) {
            IsNullExpression isNullExpression = new IsNullExpression();
            isNullExpression.setLeftExpression(leftExpression);
            return isNullExpression;
        } else if (variableContext != null) {
            EqualsTo equalsTo = new EqualsTo();
            equalsTo.setLeftExpression(leftExpression);
            equalsTo.setRightExpression(dbValueUtil.variableToJdbcNamedParameter(variableContext));
            return equalsTo;
        }
        return null;
    }

    protected Optional<Expression> isBooleanExpression(Expression leftExpression, GraphqlParser.ValueWithVariableContext valueWithVariableContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        if (valueWithVariableContext.variable() != null) {
            EqualsTo equalsTo = new EqualsTo();
            equalsTo.setLeftExpression(leftExpression);
            equalsTo.setRightExpression(dbValueUtil.variableToJdbcNamedParameter(valueWithVariableContext.variable()));
            return Optional.of(equalsTo);
        } else if (valueWithVariableContext.BooleanValue() != null) {
            IsBooleanExpression isBooleanExpression = new IsBooleanExpression();
            isBooleanExpression.setLeftExpression(leftExpression);
            isBooleanExpression.setIsTrue(Boolean.parseBoolean(valueWithVariableContext.BooleanValue().getText()));
            return Optional.of(isBooleanExpression);
        } else {
            return isBooleanExpression(leftExpression, inputValueDefinitionContext);
        }
    }

    protected Optional<Expression> isBooleanExpression(Expression leftExpression, GraphqlParser.ValueContext valueContext, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        if (valueContext.BooleanValue() != null) {
            IsBooleanExpression isBooleanExpression = new IsBooleanExpression();
            isBooleanExpression.setLeftExpression(leftExpression);
            isBooleanExpression.setIsTrue(Boolean.parseBoolean(valueContext.BooleanValue().getText()));
            return Optional.of(isBooleanExpression);
        } else {
            return isBooleanExpression(leftExpression, inputValueDefinitionContext);
        }
    }

    protected Optional<Expression> isBooleanExpression(Expression leftExpression, GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        if (inputValueDefinitionContext.type().nonNullType() != null) {
            if (inputValueDefinitionContext.defaultValue() != null) {
                IsBooleanExpression isBooleanExpression = new IsBooleanExpression();
                isBooleanExpression.setLeftExpression(leftExpression);
                isBooleanExpression.setIsTrue(Boolean.parseBoolean(inputValueDefinitionContext.defaultValue().value().BooleanValue().getText()));
                return Optional.of(isBooleanExpression);
            } else {
                throw new GraphQLErrors(NON_NULL_VALUE_NOT_EXIST.bind(inputValueDefinitionContext.getText()));
            }
        }
        return Optional.empty();
    }

    protected Expression isFalseExpression(Expression leftExpression) {
        IsBooleanExpression isBooleanExpression = new IsBooleanExpression();
        isBooleanExpression.setLeftExpression(leftExpression);
        isBooleanExpression.setNot(true);
        isBooleanExpression.setIsTrue(true);
        return isBooleanExpression;
    }

    protected ExistsExpression existsExpression(PlainSelect body) {
        ExistsExpression existsExpression = new ExistsExpression();
        SubSelect subSelect = new SubSelect();
        subSelect.setSelectBody(body);
        existsExpression.setRightExpression(subSelect);
        return existsExpression;
    }

    protected SubSelect selectVariablesFromJsonArray(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                     GraphqlParser.ValueWithVariableContext valueWithVariableContext) {
        SubSelect subSelect = new SubSelect();
        JsonTable jsonTable = new JsonTable();
        JdbcNamedParameter jdbcNamedParameter = new JdbcNamedParameter();
        jdbcNamedParameter.setName(valueWithVariableContext.variable().name().getText());
        jsonTable.setJson(jdbcNamedParameter);
        jsonTable.setPath(new StringValue("$[*]"));
        ColDataType colDataType = new ColDataType();
        String fieldTypeName = manager.getFieldTypeName(inputValueDefinitionContext.type());
        if (manager.isEnum(fieldTypeName)) {
            colDataType.setDataType("INT");
        } else if (manager.isScalar(fieldTypeName)) {
            switch (fieldTypeName) {
                case "ID":
                case "String":
                    colDataType.setDataType("VARCHAR");
                    colDataType.setArgumentsStringList(Collections.singletonList("255"));
                    break;
                case "Boolean":
                    colDataType.setDataType("BOOL");
                    break;
                case "Int":
                    colDataType.setDataType("INT");
                    break;
                case "Float":
                    colDataType.setDataType("FLOAT");
                    break;
                case "BigInteger":
                    colDataType.setDataType("BIGINT");
                    break;
                case "BigDecimal":
                    colDataType.setDataType("DECIMAL");
                    break;
                case "Date":
                    colDataType.setDataType("DATE");
                    break;
                case "Time":
                    colDataType.setDataType("TIME");
                    break;
                case "DateTime":
                    colDataType.setDataType("DATETIME");
                    break;
                case "Timestamp":
                    colDataType.setDataType("TIMESTAMP");
                    break;
            }
        } else {
            throw new GraphQLErrors(UNSUPPORTED_FIELD_TYPE.bind(fieldTypeName));
        }
        ColumnDefinition columnDefinition = new ColumnDefinition();
        columnDefinition.setColumnName(dbNameUtil.graphqlFieldNameToColumnName(valueWithVariableContext.variable().name().getText()));
        columnDefinition.setColDataType(colDataType);
        columnDefinition.addColumnSpecs("PATH", "'$'");
        jsonTable.setColumnDefinitions(Collections.singletonList(columnDefinition));
        jsonTable.setAlias(new Alias(valueWithVariableContext.variable().name().getText()));

        PlainSelect body = new PlainSelect();
        body.addSelectItems(new AllColumns());
        body.setFromItem(jsonTable);
        subSelect.setSelectBody(body);
        return subSelect;
    }

    protected Expression skipNullExpression(Expression leftExpression, Expression expression) {
        IsNullExpression isNullExpression = new IsNullExpression().withLeftExpression(leftExpression);
        return new Function().withName("IF").withParameters(new ExpressionList(isNullExpression, new LongValue(1), expression));
    }
}

