package io.graphoenix.mysql.translator;

import com.google.common.base.CharMatcher;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.error.GraphQLProblem;
import io.graphoenix.mysql.utils.DBNameUtil;
import io.graphoenix.mysql.utils.DBValueUtil;
import io.graphoenix.mysql.expression.IsExpression;
import io.graphoenix.mysql.expression.JsonTable;
import io.graphoenix.spi.antlr.IGraphQLFieldMapManager;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.DoubleValue;
import net.sf.jsqlparser.expression.Expression;
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

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.graphoenix.spi.constant.Hammurabi.DEPRECATED_FIELD_NAME;
import static io.graphoenix.spi.constant.Hammurabi.DEPRECATED_INPUT_NAME;
import static io.graphoenix.spi.error.GraphQLErrorType.*;

public class GraphQLArgumentsToWhere {

    private final IGraphQLDocumentManager manager;
    private final IGraphQLFieldMapManager mapper;
    private final DBNameUtil dbNameUtil;
    private final DBValueUtil dbValueUtil;

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
        return expressionStreamToMultipleExpression(expressionStream, hasOrConditional(argumentsContext, fieldDefinitionContext.argumentsDefinition()));
    }

    protected Optional<Expression> objectValueWithVariableToMultipleExpression(GraphqlParser.TypeContext typeContext,
                                                                               GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                                               GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext,
                                                                               int level) {
        Optional<GraphqlParser.InputObjectTypeDefinitionContext> inputObjectTypeDefinitionContext = manager.getInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type()));
        if (inputObjectTypeDefinitionContext.isPresent()) {
            Stream<Expression> expressionStream = objectValueWithVariableToExpressionList(typeContext, inputObjectTypeDefinitionContext.get().inputObjectValueDefinitions(), objectValueWithVariableContext, level);
            return expressionStreamToMultipleExpression(expressionStream, hasOrConditional(objectValueWithVariableContext, inputObjectTypeDefinitionContext.get()));
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
            return expressionStreamToMultipleExpression(expressionStream, hasOrConditional(objectValueContext, inputObjectTypeDefinitionContext.get()));
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
        Stream<Expression> expressionStream = argumentsDefinitionContext.inputValueDefinition().stream()
                .filter(this::isNotConditional)
                .filter(inputValueDefinitionContext -> !inputValueDefinitionContext.name().getText().equals(DEPRECATED_FIELD_NAME))
                .map(inputValueDefinitionContext -> argumentsToExpression(typeContext, inputValueDefinitionContext, argumentsContext, level))
                .filter(Optional::isPresent)
                .map(Optional::get);
        Stream<Expression> notDeprecatedExpression = notDeprecatedExpression(typeContext, argumentsDefinitionContext.inputValueDefinition(), argumentsContext, level);
        Stream<Expression> conditionalExpressionStream = listTypeConditionalFieldOfArgumentsToExpressionList(typeContext, argumentsDefinitionContext, argumentsContext, level);
        return Stream.concat(Stream.concat(expressionStream, notDeprecatedExpression), conditionalExpressionStream);
    }

    protected Stream<Expression> objectValueWithVariableToExpressionList(GraphqlParser.TypeContext typeContext,
                                                                         GraphqlParser.InputObjectValueDefinitionsContext inputObjectValueDefinitionsContext,
                                                                         GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext,
                                                                         int level) {
        Stream<Expression> expressionStream = inputObjectValueDefinitionsContext.inputValueDefinition().stream()
                .filter(this::isNotConditional)
                .filter(inputValueDefinitionContext -> !inputValueDefinitionContext.name().getText().equals(DEPRECATED_FIELD_NAME))
                .map(inputValueDefinitionContext -> objectValueWithVariableToExpression(typeContext, inputValueDefinitionContext, objectValueWithVariableContext, level))
                .filter(Optional::isPresent)
                .map(Optional::get);
        Stream<Expression> notDeprecatedExpression = notDeprecatedExpression(typeContext, inputObjectValueDefinitionsContext.inputValueDefinition(), objectValueWithVariableContext, level);
        Stream<Expression> conditionalExpressionStream = listTypeConditionalFieldOfObjectValueWithVariableToExpressionList(typeContext, inputObjectValueDefinitionsContext, objectValueWithVariableContext, level);
        return Stream.concat(Stream.concat(expressionStream, notDeprecatedExpression), conditionalExpressionStream);
    }

    protected Stream<Expression> objectValueToExpressionList(GraphqlParser.TypeContext typeContext,
                                                             GraphqlParser.InputObjectValueDefinitionsContext inputObjectValueDefinitionsContext,
                                                             GraphqlParser.ObjectValueContext objectValueContext,
                                                             int level) {
        Stream<Expression> expressionStream = inputObjectValueDefinitionsContext.inputValueDefinition().stream()
                .filter(this::isNotConditional)
                .filter(inputValueDefinitionContext -> !inputValueDefinitionContext.name().getText().equals(DEPRECATED_FIELD_NAME))
                .map(inputValueDefinitionContext -> objectValueToExpression(typeContext, inputValueDefinitionContext, objectValueContext, level))
                .filter(Optional::isPresent)
                .map(Optional::get);
        Stream<Expression> notDeprecatedExpression = notDeprecatedExpression(typeContext, inputObjectValueDefinitionsContext.inputValueDefinition(), objectValueContext, level);
        Stream<Expression> conditionalExpressionStream = listTypeConditionalFieldOfObjectValueToExpression(typeContext, inputObjectValueDefinitionsContext, objectValueContext, level);
        return Stream.concat(Stream.concat(expressionStream, notDeprecatedExpression), conditionalExpressionStream);
    }

    protected Stream<Expression> notDeprecatedExpression(GraphqlParser.TypeContext typeContext,
                                                         List<GraphqlParser.InputValueDefinitionContext> inputValueDefinitionContextList,
                                                         GraphqlParser.ArgumentsContext argumentsContext,
                                                         int level) {
        Optional<GraphqlParser.ArgumentContext> argumentContext = inputValueDefinitionContextList.stream().filter(inputValueDefinitionContext -> inputValueDefinitionContext.name().getText().equals(DEPRECATED_INPUT_NAME))
                .filter(inputValueDefinitionContext -> manager.getFieldTypeName(inputValueDefinitionContext.type()).equals("Boolean"))
                .findFirst()
                .flatMap(inputValueDefinitionContext -> manager.getArgumentFromInputValueDefinition(argumentsContext, inputValueDefinitionContext));
        if (argumentContext.isPresent() && argumentContext.get().valueWithVariable().BooleanValue() != null) {
            if (argumentContext.get().valueWithVariable().BooleanValue().getText().equals("true")) {
                return Stream.empty();
            }
        }
        return Stream.of(isFalseExpression(dbNameUtil.fieldToColumn(manager.getFieldTypeName(typeContext), DEPRECATED_FIELD_NAME, level)));
    }

    protected Stream<Expression> notDeprecatedExpression(GraphqlParser.TypeContext typeContext,
                                                         List<GraphqlParser.InputValueDefinitionContext> inputValueDefinitionContextList,
                                                         GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext,
                                                         int level) {
        Optional<GraphqlParser.ObjectFieldWithVariableContext> objectFieldWithVariableContext = inputValueDefinitionContextList.stream().filter(inputValueDefinitionContext -> inputValueDefinitionContext.name().getText().equals(DEPRECATED_INPUT_NAME))
                .filter(inputValueDefinitionContext -> manager.getFieldTypeName(inputValueDefinitionContext.type()).equals("Boolean"))
                .findFirst()
                .flatMap(inputValueDefinitionContext -> manager.getObjectFieldWithVariableFromInputValueDefinition(objectValueWithVariableContext, inputValueDefinitionContext));
        if (objectFieldWithVariableContext.isPresent() && objectFieldWithVariableContext.get().valueWithVariable().BooleanValue() != null) {
            if (objectFieldWithVariableContext.get().valueWithVariable().BooleanValue().getText().equals("true")) {
                return Stream.empty();
            }
        }
        return Stream.of(isFalseExpression(dbNameUtil.fieldToColumn(manager.getFieldTypeName(typeContext), DEPRECATED_FIELD_NAME, level)));
    }

    protected Stream<Expression> notDeprecatedExpression(GraphqlParser.TypeContext typeContext,
                                                         List<GraphqlParser.InputValueDefinitionContext> inputValueDefinitionContextList,
                                                         GraphqlParser.ObjectValueContext objectValueContext,
                                                         int level) {
        Optional<GraphqlParser.ObjectFieldContext> objectFieldContext = inputValueDefinitionContextList.stream().filter(inputValueDefinitionContext -> inputValueDefinitionContext.name().getText().equals(DEPRECATED_INPUT_NAME))
                .filter(inputValueDefinitionContext -> manager.getFieldTypeName(inputValueDefinitionContext.type()).equals("Boolean"))
                .findFirst()
                .flatMap(inputValueDefinitionContext -> manager.getObjectFieldFromInputValueDefinition(objectValueContext, inputValueDefinitionContext));
        if (objectFieldContext.isPresent() && objectFieldContext.get().value().BooleanValue() != null) {
            if (objectFieldContext.get().value().BooleanValue().getText().equals("true")) {
                return Stream.empty();
            }
        }
        return Stream.of(isFalseExpression(dbNameUtil.fieldToColumn(manager.getFieldTypeName(typeContext), DEPRECATED_FIELD_NAME, level)));
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
                throw new GraphQLProblem(NON_NULL_VALUE_NOT_EXIST.bind(inputValueDefinitionContext.getText()));
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
        if (inputValueDefinitionContext.name().getText().equals(DEPRECATED_INPUT_NAME)) {
            return Optional.empty();
        }

        Optional<GraphqlParser.ObjectTypeDefinitionContext> objectTypeDefinitionContext = manager.getObject(manager.getFieldTypeName(typeContext));
        if (objectTypeDefinitionContext.isPresent()) {
            Optional<GraphqlParser.FieldDefinitionContext> fieldDefinitionContext = manager.getFieldDefinitionFromInputValueDefinition(typeContext, inputValueDefinitionContext);
            if (fieldDefinitionContext.isPresent()) {
                String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.get().type());
                if (manager.isObject(fieldTypeName)) {
                    if (isConditionalObject(inputValueDefinitionContext)) {
                        return Optional.of(objectValueWithVariableToExpression(
                                objectTypeDefinitionContext.get(),
                                fieldDefinitionContext.get(),
                                inputValueDefinitionContext,
                                argumentContext.valueWithVariable().objectValueWithVariable(),
                                level + 1)
                        );
                    } else {
                        throw new GraphQLProblem(UNSUPPORTED_FIELD_TYPE.bind(fieldTypeName));
                    }
                } else if (manager.isScalar(fieldTypeName) || manager.isEnum(fieldTypeName)) {
                    if (isOperatorObject(inputValueDefinitionContext)) {
                        return operatorArgumentToExpression(argumentToColumn(typeContext, argumentContext, level), inputValueDefinitionContext, argumentContext);
                    } else if (fieldTypeName.equals("Boolean")) {
                        return isBooleanExpression(argumentToColumn(typeContext, argumentContext, level), argumentContext.valueWithVariable(), inputValueDefinitionContext);
                    } else if (isConditionalObject(inputValueDefinitionContext)) {
                        return objectValueWithVariableToMultipleExpression(typeContext, inputValueDefinitionContext, argumentContext.valueWithVariable().objectValueWithVariable(), level);
                    } else if (manager.isScalar(fieldTypeName)) {
                        return Optional.of(scalarValueWithVariableToExpression(argumentToColumn(typeContext, argumentContext, level), argumentContext.valueWithVariable()));
                    } else if (manager.isEnum(fieldTypeName)) {
                        return Optional.of(enumValueWithVariableToExpression(argumentToColumn(typeContext, argumentContext, level), argumentContext.valueWithVariable()));
                    } else {
                        throw new GraphQLProblem(UNSUPPORTED_FIELD_TYPE.bind(fieldTypeName));
                    }
                } else {
                    throw new GraphQLProblem(UNSUPPORTED_FIELD_TYPE.bind(fieldTypeName));
                }
            } else {
                throw new GraphQLProblem(FIELD_NOT_EXIST.bind(manager.getFieldTypeName(typeContext), inputValueDefinitionContext.name().getText()));
            }
        } else {
            throw new GraphQLProblem(TYPE_NOT_EXIST.bind(manager.getFieldTypeName(typeContext)));
        }
    }

    protected Optional<Expression> singleTypeObjectFieldWithVariableToExpression(GraphqlParser.TypeContext typeContext,
                                                                                 GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                                                 GraphqlParser.ObjectFieldWithVariableContext objectFieldWithVariableContext,
                                                                                 int level) {
        if (inputValueDefinitionContext.name().getText().equals(DEPRECATED_INPUT_NAME)) {
            return Optional.empty();
        }

        Optional<GraphqlParser.ObjectTypeDefinitionContext> objectTypeDefinitionContext = manager.getObject(manager.getFieldTypeName(typeContext));
        if (objectTypeDefinitionContext.isPresent()) {

            Optional<GraphqlParser.FieldDefinitionContext> fieldDefinitionContext = manager.getFieldDefinitionFromInputValueDefinition(typeContext, inputValueDefinitionContext);
            if (fieldDefinitionContext.isPresent()) {
                String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.get().type());
                if (manager.isObject(fieldTypeName)) {
                    if (isConditionalObject(inputValueDefinitionContext)) {
                        return Optional.of(objectValueWithVariableToExpression(
                                objectTypeDefinitionContext.get(),
                                fieldDefinitionContext.get(),
                                inputValueDefinitionContext,
                                objectFieldWithVariableContext.valueWithVariable().objectValueWithVariable(),
                                level + 1)
                        );
                    } else {
                        throw new GraphQLProblem(UNSUPPORTED_FIELD_TYPE.bind(fieldTypeName));
                    }
                } else if (manager.isScalar(fieldTypeName) || manager.isEnum(fieldTypeName)) {
                    if (isOperatorObject(inputValueDefinitionContext)) {
                        return operatorObjectFieldWithVariableToExpression(objectFieldWithVariableToColumn(typeContext, objectFieldWithVariableContext, level), inputValueDefinitionContext, objectFieldWithVariableContext);
                    } else if (fieldTypeName.equals("Boolean")) {
                        return isBooleanExpression(objectFieldWithVariableToColumn(typeContext, objectFieldWithVariableContext, level), objectFieldWithVariableContext.valueWithVariable(), inputValueDefinitionContext);
                    } else if (isConditionalObject(inputValueDefinitionContext)) {
                        return objectValueWithVariableToMultipleExpression(typeContext, inputValueDefinitionContext, objectFieldWithVariableContext.valueWithVariable().objectValueWithVariable(), level);
                    } else if (manager.isScalar(fieldTypeName)) {
                        return Optional.of(scalarValueWithVariableToExpression(objectFieldWithVariableToColumn(typeContext, objectFieldWithVariableContext, level), objectFieldWithVariableContext.valueWithVariable()));
                    } else if (manager.isEnum(fieldTypeName)) {
                        return Optional.of(enumValueWithVariableToExpression(objectFieldWithVariableToColumn(typeContext, objectFieldWithVariableContext, level), objectFieldWithVariableContext.valueWithVariable()));
                    } else {
                        throw new GraphQLProblem(UNSUPPORTED_FIELD_TYPE.bind(fieldTypeName));
                    }
                } else {
                    throw new GraphQLProblem(FIELD_NOT_EXIST.bind(manager.getFieldTypeName(typeContext), inputValueDefinitionContext.name().getText()));
                }
            } else {
                throw new GraphQLProblem(FIELD_NOT_EXIST.bind(manager.getFieldTypeName(typeContext), inputValueDefinitionContext.name().getText()));
            }
        } else {
            throw new GraphQLProblem(TYPE_NOT_EXIST.bind(manager.getFieldTypeName(typeContext)));
        }
    }

    protected Optional<Expression> singleTypeObjectFieldToExpression(GraphqlParser.TypeContext typeContext,
                                                                     GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                                     GraphqlParser.ObjectFieldContext objectFieldContext,
                                                                     int level) {
        if (inputValueDefinitionContext.name().getText().equals(DEPRECATED_INPUT_NAME)) {
            return Optional.empty();
        }

        Optional<GraphqlParser.ObjectTypeDefinitionContext> objectTypeDefinitionContext = manager.getObject(manager.getFieldTypeName(typeContext));
        if (objectTypeDefinitionContext.isPresent()) {
            Optional<GraphqlParser.FieldDefinitionContext> fieldDefinitionContext = manager.getFieldDefinitionFromInputValueDefinition(typeContext, inputValueDefinitionContext);
            if (fieldDefinitionContext.isPresent()) {
                String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.get().type());
                if (manager.isObject(fieldTypeName)) {
                    if (isConditionalObject(inputValueDefinitionContext)) {
                        return Optional.of(objectValueToExpression(
                                objectTypeDefinitionContext.get(),
                                fieldDefinitionContext.get(),
                                inputValueDefinitionContext,
                                objectFieldContext.value().objectValue(),
                                level + 1)
                        );
                    } else {
                        throw new GraphQLProblem(UNSUPPORTED_FIELD_TYPE.bind(fieldTypeName));
                    }
                } else if (manager.isScalar(fieldTypeName) || manager.isEnum(fieldTypeName)) {
                    if (isOperatorObject(inputValueDefinitionContext)) {
                        return operatorObjectFieldToExpression(objectFieldToColumn(typeContext, objectFieldContext, level), inputValueDefinitionContext, objectFieldContext);
                    } else if (manager.getFieldTypeName(inputValueDefinitionContext.type()).equals("Boolean")) {
                        return isBooleanExpression(objectFieldToColumn(typeContext, objectFieldContext, level), objectFieldContext.value(), inputValueDefinitionContext);
                    } else if (isConditionalObject(inputValueDefinitionContext)) {
                        return objectValueToMultipleExpression(typeContext, inputValueDefinitionContext, objectFieldContext.value().objectValue(), level);
                    } else if (manager.isScalar(fieldTypeName)) {
                        return Optional.of(scalarValueToExpression(objectFieldToColumn(typeContext, objectFieldContext, level), objectFieldContext.value()));
                    } else if (manager.isEnum(fieldTypeName)) {
                        return Optional.of(enumValueToExpression(objectFieldToColumn(typeContext, objectFieldContext, level), objectFieldContext.value().enumValue()));
                    } else {
                        throw new GraphQLProblem(UNSUPPORTED_FIELD_TYPE.bind(fieldTypeName));
                    }
                } else {
                    throw new GraphQLProblem(FIELD_NOT_EXIST.bind(manager.getFieldTypeName(typeContext), inputValueDefinitionContext.name().getText()));
                }
            } else {
                throw new GraphQLProblem(FIELD_NOT_EXIST.bind(manager.getFieldTypeName(typeContext), inputValueDefinitionContext.name().getText()));
            }
        } else {
            throw new GraphQLProblem(TYPE_NOT_EXIST.bind(manager.getFieldTypeName(typeContext)));
        }
    }

    protected Optional<Expression> singleTypeInputValueToExpression(GraphqlParser.TypeContext typeContext,
                                                                    GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                                    int level) {
        if (inputValueDefinitionContext.name().getText().equals(DEPRECATED_INPUT_NAME)) {
            return Optional.empty();
        }

        Optional<GraphqlParser.ObjectTypeDefinitionContext> objectTypeDefinition = manager.getObject(manager.getFieldTypeName(typeContext));
        if (objectTypeDefinition.isPresent()) {
            Optional<GraphqlParser.FieldDefinitionContext> fieldDefinitionContext = manager.getFieldDefinitionFromInputValueDefinition(typeContext, inputValueDefinitionContext);
            if (fieldDefinitionContext.isPresent()) {
                String fieldTypeName = manager.getFieldTypeName(fieldDefinitionContext.get().type());
                if (manager.isObject(fieldTypeName)) {
                    if (isConditionalObject(inputValueDefinitionContext)) {
                        return Optional.of(objectValueToExpression(
                                objectTypeDefinition.get(),
                                fieldDefinitionContext.get(),
                                inputValueDefinitionContext,
                                inputValueDefinitionContext.defaultValue().value().objectValue(),
                                level + 1)
                        );
                    } else {
                        throw new GraphQLProblem(UNSUPPORTED_FIELD_TYPE.bind(fieldTypeName));
                    }
                } else if (manager.isScalar(fieldTypeName) || manager.isEnum(fieldTypeName)) {
                    if (isOperatorObject(inputValueDefinitionContext)) {
                        return operatorInputValueToExpression(inputValueToColumn(typeContext, inputValueDefinitionContext, level), inputValueDefinitionContext);
                    } else if (manager.getFieldTypeName(inputValueDefinitionContext.type()).equals("Boolean")) {
                        return isBooleanExpression(inputValueToColumn(typeContext, inputValueDefinitionContext, level), inputValueDefinitionContext);
                    } else if (isConditionalObject(inputValueDefinitionContext)) {
                        return objectValueToMultipleExpression(typeContext, inputValueDefinitionContext, inputValueDefinitionContext.defaultValue().value().objectValue(), level);
                    } else if (manager.isScalar(fieldTypeName)) {
                        return Optional.of(scalarValueToExpression(inputValueToColumn(typeContext, inputValueDefinitionContext, level), inputValueDefinitionContext.defaultValue().value()));
                    } else if (manager.isEnum(fieldTypeName)) {
                        return Optional.of(enumValueToExpression(inputValueToColumn(typeContext, inputValueDefinitionContext, level), inputValueDefinitionContext.defaultValue().value().enumValue()));
                    } else {
                        throw new GraphQLProblem(UNSUPPORTED_FIELD_TYPE.bind(fieldTypeName));
                    }
                } else {
                    throw new GraphQLProblem(FIELD_NOT_EXIST.bind(manager.getFieldTypeName(typeContext), inputValueDefinitionContext.name().getText()));
                }
            } else {
                throw new GraphQLProblem(FIELD_NOT_EXIST.bind(manager.getFieldTypeName(typeContext), inputValueDefinitionContext.name().getText()));
            }
        } else {
            throw new GraphQLProblem(TYPE_NOT_EXIST.bind(manager.getFieldTypeName(typeContext)));
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
                    .flatMap(context -> Optional.of(context.valueWithVariable().arrayValueWithVariable().valueWithVariable().stream()
                            .map(valueWithVariableContext -> objectValueWithVariableToMultipleExpression(typeContext, conditionalInputValueDefinitionContext.get(), valueWithVariableContext.objectValueWithVariable(), level))
                            .filter(Optional::isPresent)
                            .map(Optional::get)))
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
                    .flatMap(fieldWithVariableContext -> Optional.of(fieldWithVariableContext.valueWithVariable().arrayValueWithVariable().valueWithVariable().stream()
                            .map(valueWithVariableContext -> objectValueWithVariableToMultipleExpression(typeContext, conditionalInputValueDefinitionContext.get(), valueWithVariableContext.objectValueWithVariable(), level))
                            .filter(Optional::isPresent)
                            .map(Optional::get)))
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
            return objectFieldContext.flatMap(fieldContext -> Optional.of(fieldContext.value().arrayValue().value().stream()
                    .map(valueContext -> objectValueToMultipleExpression(typeContext, conditionalInputValueDefinitionContext.get(), valueContext.objectValue(), level))
                    .filter(Optional::isPresent)
                    .map(Optional::get))
            ).orElseGet(() -> listTypeConditionalFieldOfInputValueToExpression(typeContext, conditionalInputValueDefinitionContext.get(), level));
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
                        .filter(Optional::isPresent)
                        .map(Optional::get);
            } else {
                throw new GraphQLProblem(NON_NULL_VALUE_NOT_EXIST.bind(inputValueDefinitionContext.getText()));
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
                inExpression.setRightItemsList(new ExpressionList(valueWithVariableContext.arrayValueWithVariable().valueWithVariable().stream()
                        .map(dbValueUtil::scalarValueWithVariableToDBValue).collect(Collectors.toList())));
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
                inExpression.setRightItemsList(new ExpressionList(valueContext.arrayValue().value().stream().map(dbValueUtil::scalarValueToDBValue).collect(Collectors.toList())));
                return Optional.of(inExpression);
            }
        }
        return Optional.empty();
    }

    private boolean hasOrConditional(GraphqlParser.ArgumentsContext argumentsContext, GraphqlParser.ArgumentsDefinitionContext argumentsDefinitionContext) {
        return argumentsContext.argument().stream().anyMatch(argumentContext ->
                manager.getInputValueDefinitionFromArgumentsDefinitionContext(argumentsDefinitionContext, argumentContext)
                        .map(inputValueDefinitionContext ->
                                isOrConditional(inputValueDefinitionContext, argumentContext.valueWithVariable()))
                        .orElse(false));
    }

    private boolean hasOrConditional(GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext, GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext) {

        return objectValueWithVariableContext.objectFieldWithVariable().stream().anyMatch(objectFieldWithVariableContext ->
                manager.getInputValueDefinitionFromInputObjectTypeDefinitionContext(inputObjectTypeDefinitionContext, objectFieldWithVariableContext)
                        .map(inputValueDefinitionContext ->
                                isOrConditional(inputValueDefinitionContext, objectFieldWithVariableContext.valueWithVariable()))
                        .orElse(false));
    }

    private boolean hasOrConditional(GraphqlParser.ObjectValueContext objectValueContext, GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext) {
        return objectValueContext.objectField().stream().anyMatch(objectFieldContext ->
                manager.getInputValueDefinitionFromInputObjectTypeDefinitionContext(inputObjectTypeDefinitionContext, objectFieldContext)
                        .map(inputValueDefinitionContext ->
                                isOrConditional(inputValueDefinitionContext, objectFieldContext.value()))
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
        return objectTypeDefinition.map(inputObjectTypeDefinitionContext -> inputObjectTypeDefinitionContext.inputObjectValueDefinitions().inputValueDefinition().stream()
                .anyMatch(fieldInputValueDefinitionContext ->
                        manager.isEnum(fieldInputValueDefinitionContext.type().getText()) &&
                                fieldInputValueDefinitionContext.type().typeName().name().getText().equals(enumName))).orElse(false);
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

    private Optional<Expression> operatorValueWithVariableToExpression(Expression leftExpression,
                                                                       GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                                       GraphqlParser.ValueWithVariableContext valueWithVariableContext) {
        Optional<GraphqlParser.InputObjectTypeDefinitionContext> inputObjectTypeDefinition = manager.getInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type()));

        if (inputObjectTypeDefinition.isPresent()) {
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
                            !(manager.isEnum(fieldInputValueDefinitionContext.type().getText()) &&
                                    manager.getFieldTypeName(fieldInputValueDefinitionContext.type()).equals("Operator")))
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
                return operatorValueWithVariableToExpression(leftExpression, operatorEnumValueContext.get(), subInputValueDefinitionContext.get(), subValueWithVariableContext.get());
            } else if (operatorEnumValueContext.isPresent() && subDefaultValueContext.isPresent()) {
                return operatorValueToExpression(leftExpression, operatorEnumValueContext.get(), subInputValueDefinitionContext.get(), subDefaultValueContext.get());
            } else if (defaultOperatorEnumValueContext.isPresent() && subValueWithVariableContext.isPresent()) {
                return operatorValueWithVariableToExpression(leftExpression, defaultOperatorEnumValueContext.get(), subInputValueDefinitionContext.get(), subValueWithVariableContext.get());
            } else if (defaultOperatorEnumValueContext.isPresent() && subDefaultValueContext.isPresent()) {
                return operatorValueToExpression(leftExpression, defaultOperatorEnumValueContext.get(), subInputValueDefinitionContext.get(), subDefaultValueContext.get());
            } else {
                throw new GraphQLProblem(NON_NULL_VALUE_NOT_EXIST.bind(inputValueDefinitionContext.getText()));
            }
        } else {
            throw new GraphQLProblem(TYPE_NOT_EXIST.bind(manager.getFieldTypeName(inputValueDefinitionContext.type())));
        }
    }

    private Optional<Expression> operatorValueToExpression(Expression leftExpression,
                                                           GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                           GraphqlParser.ValueContext valueContext) {
        Optional<GraphqlParser.InputObjectTypeDefinitionContext> inputObjectTypeDefinition = manager.getInputObject(manager.getFieldTypeName(inputValueDefinitionContext.type()));

        if (inputObjectTypeDefinition.isPresent()) {
            Optional<GraphqlParser.EnumValueContext> operatorEnumValueContext = inputObjectTypeDefinition.get().inputObjectValueDefinitions().inputValueDefinition().stream()
                    .filter(fieldInputValueDefinitionContext ->
                            manager.isEnum(fieldInputValueDefinitionContext.type().getText()) && manager.getFieldTypeName(fieldInputValueDefinitionContext.type()).equals("Operator"))
                    .findFirst()
                    .flatMap(fieldInputValueDefinitionContext -> manager.getObjectFieldFromInputValueDefinition(valueContext.objectValue(), fieldInputValueDefinitionContext))
                    .map(objectFieldContext -> objectFieldContext.value().enumValue());

            Optional<GraphqlParser.EnumValueContext> defaultOperatorEnumValueContext = inputObjectTypeDefinition.get().inputObjectValueDefinitions().inputValueDefinition().stream()
                    .filter(fieldInputValueDefinitionContext ->
                            manager.isEnum(fieldInputValueDefinitionContext.type().getText()) && manager.getFieldTypeName(fieldInputValueDefinitionContext.type()).equals("Operator"))
                    .findFirst()
                    .flatMap(manager::getDefaultValueFromInputValueDefinition)
                    .map(GraphqlParser.ValueContext::enumValue);

            Optional<GraphqlParser.InputValueDefinitionContext> subInputValueDefinitionContext = inputObjectTypeDefinition.get().inputObjectValueDefinitions().inputValueDefinition().stream()
                    .filter(fieldInputValueDefinitionContext ->
                            !(manager.isEnum(fieldInputValueDefinitionContext.type().getText()) &&
                                    manager.getFieldTypeName(fieldInputValueDefinitionContext.type()).equals("Operator")))
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
                return operatorValueToExpression(leftExpression, operatorEnumValueContext.get(), subInputValueDefinitionContext.get(), subValueContext.get());
            } else if (operatorEnumValueContext.isPresent() && subDefaultValueContext.isPresent()) {
                return operatorValueToExpression(leftExpression, operatorEnumValueContext.get(), subInputValueDefinitionContext.get(), subDefaultValueContext.get());
            } else if (defaultOperatorEnumValueContext.isPresent() && subValueContext.isPresent()) {
                return operatorValueToExpression(leftExpression, defaultOperatorEnumValueContext.get(), subInputValueDefinitionContext.get(), subValueContext.get());
            } else if (defaultOperatorEnumValueContext.isPresent() && subDefaultValueContext.isPresent()) {
                return operatorValueToExpression(leftExpression, defaultOperatorEnumValueContext.get(), subInputValueDefinitionContext.get(), subDefaultValueContext.get());
            } else {
                throw new GraphQLProblem(NON_NULL_VALUE_NOT_EXIST.bind(inputValueDefinitionContext.getText()));
            }
        } else {
            throw new GraphQLProblem(TYPE_NOT_EXIST.bind(manager.getFieldTypeName(inputValueDefinitionContext.type())));
        }
    }

    private Optional<Expression> operatorValueWithVariableToExpression(Expression leftExpression,
                                                                       GraphqlParser.EnumValueContext enumValueContext,
                                                                       GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                                       GraphqlParser.ValueWithVariableContext valueWithVariableContext) {

        if (manager.fieldTypeIsList(inputValueDefinitionContext.type()) && valueWithVariableContext.variable() != null) {
            return Optional.of(operatorValueWithVariableToInExpression(leftExpression, enumValueContext, inputValueDefinitionContext, valueWithVariableContext));
        }
        if (valueWithVariableContext.arrayValueWithVariable() != null) {
            return Optional.of(operatorValueWithVariableToInExpression(leftExpression, enumValueContext, inputValueDefinitionContext, valueWithVariableContext));
        }
        if (valueWithVariableContext.enumValue() != null) {
            return Optional.ofNullable(operatorEnumValueWithVariableToExpression(leftExpression, enumValueContext, valueWithVariableContext));
        }
        return Optional.ofNullable(operatorScalarValueToExpression(
                leftExpression,
                enumValueContext,
                valueWithVariableContext.StringValue(),
                valueWithVariableContext.IntValue(),
                valueWithVariableContext.FloatValue(),
                valueWithVariableContext.BooleanValue(),
                valueWithVariableContext.NullValue(),
                valueWithVariableContext.variable())
        );
    }

    private Optional<Expression> operatorValueToExpression(Expression leftExpression,
                                                           GraphqlParser.EnumValueContext enumValueContext,
                                                           GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                           GraphqlParser.ValueContext valueContext) {
        if (valueContext.arrayValue() != null) {
            return Optional.of(operatorValueToInExpression(leftExpression, enumValueContext, valueContext));
        }
        if (valueContext.enumValue() != null) {
            return Optional.ofNullable(operatorEnumValueToExpression(leftExpression, enumValueContext, valueContext));
        }
        return Optional.ofNullable(operatorScalarValueToExpression(
                leftExpression,
                enumValueContext,
                valueContext.StringValue(),
                valueContext.IntValue(),
                valueContext.FloatValue(),
                valueContext.BooleanValue(),
                valueContext.NullValue(),
                null)

        );
    }

    private Expression operatorValueToInExpression(Expression leftExpression,
                                                   GraphqlParser.EnumValueContext enumValueContext,
                                                   GraphqlParser.ValueContext valueContext) {
        InExpression inExpression = new InExpression();
        inExpression.setLeftExpression(leftExpression);
        inExpression.setRightItemsList(new ExpressionList(valueContext.arrayValue().value().stream()
                .map(dbValueUtil::valueToDBValue)
                .collect(Collectors.toList())));
        if ("NIN".equals(enumValueContext.enumValueName().getText())) {
            inExpression.setNot(true);
        }
        return inExpression;
    }

    private Expression operatorValueWithVariableToInExpression(Expression leftExpression,
                                                               GraphqlParser.EnumValueContext enumValueContext,
                                                               GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                               GraphqlParser.ValueWithVariableContext valueWithVariableContext) {

        InExpression inExpression = new InExpression();
        inExpression.setLeftExpression(leftExpression);
        if (valueWithVariableContext.variable() != null) {
            inExpression.setRightExpression(selectVariablesFromJsonArray(inputValueDefinitionContext, valueWithVariableContext));
        } else {
            inExpression.setRightItemsList(new ExpressionList(valueWithVariableContext.arrayValueWithVariable().valueWithVariable().stream()
                    .map(dbValueUtil::valueWithVariableToDBValue)
                    .collect(Collectors.toList())));
        }
        if ("NIN".equals(enumValueContext.enumValueName().getText())) {
            inExpression.setNot(true);
        }
        return inExpression;
    }

    private Expression operatorEnumValueToExpression(Expression leftExpression,
                                                     GraphqlParser.EnumValueContext operator,
                                                     GraphqlParser.ValueContext valueContext) {
        switch (operator.enumValueName().getText()) {
            case "EQ":
                return enumValueToExpression(leftExpression, valueContext.enumValue());
            case "NEQ":
                return new NotExpression(enumValueToExpression(leftExpression, valueContext.enumValue()));
            case "LK":
                LikeExpression likeExpression = new LikeExpression();
                likeExpression.setLeftExpression(leftExpression);
                likeExpression.setRightExpression(new StringValue(valueContext.enumValue().enumValueName().getText()));
                return likeExpression;
            case "NLK":
                LikeExpression notLikeExpression = new LikeExpression();
                notLikeExpression.setNot(true);
                notLikeExpression.setLeftExpression(leftExpression);
                notLikeExpression.setRightExpression(new StringValue(valueContext.enumValue().enumValueName().getText()));
                return notLikeExpression;
            case "GT":
            case "NLTE":
                GreaterThan greaterThan = new GreaterThan();
                greaterThan.setLeftExpression(leftExpression);
                greaterThan.setRightExpression(new StringValue(valueContext.enumValue().enumValueName().getText()));
                return greaterThan;
            case "GTE":
            case "NLT":
                GreaterThanEquals greaterThanEquals = new GreaterThanEquals();
                greaterThanEquals.setLeftExpression(leftExpression);
                greaterThanEquals.setRightExpression(new StringValue(valueContext.enumValue().enumValueName().getText()));
                return greaterThanEquals;
            case "LT":
            case "NGTE":
                MinorThan minorThan = new MinorThan();
                minorThan.setLeftExpression(leftExpression);
                minorThan.setRightExpression(new StringValue(valueContext.enumValue().enumValueName().getText()));
                return minorThan;
            case "LTE":
            case "NGT":
                MinorThanEquals minorThanEquals = new MinorThanEquals();
                minorThanEquals.setLeftExpression(leftExpression);
                minorThanEquals.setRightExpression(new StringValue(valueContext.enumValue().enumValueName().getText()));
                return minorThanEquals;
            case "NIL":
                IsNullExpression isNullExpression = new IsNullExpression();
                isNullExpression.setLeftExpression(leftExpression);
                return isNullExpression;
            case "NNIL":
                IsNullExpression isNotNullExpression = new IsNullExpression();
                isNotNullExpression.setNot(true);
                isNotNullExpression.setLeftExpression(leftExpression);
                return isNotNullExpression;
            default:
                throw new GraphQLProblem(UNSUPPORTED_VALUE.bind(operator.enumValueName().getText()));
        }
    }

    private Expression operatorEnumValueWithVariableToExpression(Expression leftExpression,
                                                                 GraphqlParser.EnumValueContext operator,
                                                                 GraphqlParser.ValueWithVariableContext valueWithVariableContext) {
        switch (operator.enumValueName().getText()) {
            case "EQ":
                return enumValueWithVariableToExpression(leftExpression, valueWithVariableContext);
            case "NEQ":
                return new NotExpression(enumValueWithVariableToExpression(leftExpression, valueWithVariableContext));
            case "LK":
                LikeExpression likeExpression = new LikeExpression();
                likeExpression.setLeftExpression(leftExpression);
                likeExpression.setRightExpression(new StringValue(valueWithVariableContext.enumValue().enumValueName().getText()));
                return likeExpression;
            case "NLK":
                LikeExpression notLikeExpression = new LikeExpression();
                notLikeExpression.setNot(true);
                notLikeExpression.setLeftExpression(leftExpression);
                notLikeExpression.setRightExpression(new StringValue(valueWithVariableContext.enumValue().enumValueName().getText()));
                return notLikeExpression;
            case "GT":
            case "NLTE":
                GreaterThan greaterThan = new GreaterThan();
                greaterThan.setLeftExpression(leftExpression);
                greaterThan.setRightExpression(new StringValue(valueWithVariableContext.enumValue().enumValueName().getText()));
                return greaterThan;
            case "GTE":
            case "NLT":
                GreaterThanEquals greaterThanEquals = new GreaterThanEquals();
                greaterThanEquals.setLeftExpression(leftExpression);
                greaterThanEquals.setRightExpression(new StringValue(valueWithVariableContext.enumValue().enumValueName().getText()));
                return greaterThanEquals;
            case "LT":
            case "NGTE":
                MinorThan minorThan = new MinorThan();
                minorThan.setLeftExpression(leftExpression);
                minorThan.setRightExpression(new StringValue(valueWithVariableContext.enumValue().enumValueName().getText()));
                return minorThan;
            case "LTE":
            case "NGT":
                MinorThanEquals minorThanEquals = new MinorThanEquals();
                minorThanEquals.setLeftExpression(leftExpression);
                minorThanEquals.setRightExpression(new StringValue(valueWithVariableContext.enumValue().enumValueName().getText()));
                return minorThanEquals;
            case "NIL":
                IsNullExpression isNullExpression = new IsNullExpression();
                isNullExpression.setLeftExpression(leftExpression);
                return isNullExpression;
            case "NNIL":
                IsNullExpression isNotNullExpression = new IsNullExpression();
                isNotNullExpression.setNot(true);
                isNotNullExpression.setLeftExpression(leftExpression);
                return isNotNullExpression;
            default:
                throw new GraphQLProblem(UNSUPPORTED_VALUE.bind(operator.enumValueName().getText()));
        }
    }

    private Expression operatorScalarValueToExpression(Expression leftExpression,
                                                       GraphqlParser.EnumValueContext operator,
                                                       TerminalNode stringValue,
                                                       TerminalNode intValue,
                                                       TerminalNode floatValue,
                                                       TerminalNode booleanValue,
                                                       TerminalNode nullValue,
                                                       GraphqlParser.VariableContext variableContext) {
        switch (operator.enumValueName().getText()) {
            case "EQ":
                return scalarValueToExpression(leftExpression, stringValue, intValue, floatValue, booleanValue, nullValue, variableContext);
            case "NEQ":
                return new NotExpression(scalarValueToExpression(leftExpression, stringValue, intValue, floatValue, booleanValue, nullValue, variableContext));
            case "LK":
                LikeExpression likeExpression = new LikeExpression();
                likeExpression.setLeftExpression(leftExpression);
                likeExpression.setRightExpression(dbValueUtil.scalarValueToDBValue(stringValue, intValue, floatValue, booleanValue, nullValue));
                return likeExpression;
            case "NLK":
                LikeExpression notLikeExpression = new LikeExpression();
                notLikeExpression.setNot(true);
                notLikeExpression.setLeftExpression(leftExpression);
                notLikeExpression.setRightExpression(dbValueUtil.scalarValueToDBValue(stringValue, intValue, floatValue, booleanValue, nullValue));
                return notLikeExpression;
            case "GT":
            case "NLTE":
                GreaterThan greaterThan = new GreaterThan();
                greaterThan.setLeftExpression(leftExpression);
                greaterThan.setRightExpression(dbValueUtil.scalarValueToDBValue(stringValue, intValue, floatValue, booleanValue, nullValue));
                return greaterThan;
            case "GTE":
            case "NLT":
                GreaterThanEquals greaterThanEquals = new GreaterThanEquals();
                greaterThanEquals.setLeftExpression(leftExpression);
                greaterThanEquals.setRightExpression(dbValueUtil.scalarValueToDBValue(stringValue, intValue, floatValue, booleanValue, nullValue));
                return greaterThanEquals;
            case "LT":
            case "NGTE":
                MinorThan minorThan = new MinorThan();
                minorThan.setLeftExpression(leftExpression);
                minorThan.setRightExpression(dbValueUtil.scalarValueToDBValue(stringValue, intValue, floatValue, booleanValue, nullValue));
                return minorThan;
            case "LTE":
            case "NGT":
                MinorThanEquals minorThanEquals = new MinorThanEquals();
                minorThanEquals.setLeftExpression(leftExpression);
                minorThanEquals.setRightExpression(dbValueUtil.scalarValueToDBValue(stringValue, intValue, floatValue, booleanValue, nullValue));
                return minorThanEquals;
            case "NIL":
                IsNullExpression isNullExpression = new IsNullExpression();
                isNullExpression.setLeftExpression(leftExpression);
                return isNullExpression;
            case "NNIL":
                IsNullExpression isNotNullExpression = new IsNullExpression();
                isNotNullExpression.setNot(true);
                isNotNullExpression.setLeftExpression(leftExpression);
                return isNotNullExpression;
            default:
                throw new GraphQLProblem(UNSUPPORTED_VALUE.bind(operator.enumValueName().getText()));
        }
    }

    protected Expression objectValueWithVariableToExpression(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext,
                                                             GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                             GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                             GraphqlParser.ObjectValueWithVariableContext objectValueWithVariableContext,
                                                             int level) {

        PlainSelect body = mapFieldPlainSelect(objectTypeDefinitionContext, fieldDefinitionContext, level);
        Optional<Expression> subWhereExpression = objectValueWithVariableToMultipleExpression(fieldDefinitionContext.type(), inputValueDefinitionContext, objectValueWithVariableContext, level);
        subWhereExpression.ifPresent(expression -> {
            if (body.getWhere() != null) {
                body.setWhere(new MultiAndExpression(Arrays.asList(body.getWhere(), expression)));
            } else {
                body.setWhere(expression);
            }
        });
        return existsExpression(body);
    }

    protected Expression objectValueToExpression(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext,
                                                 GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                                 GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext,
                                                 GraphqlParser.ObjectValueContext objectValueContext,
                                                 int level) {

        PlainSelect body = mapFieldPlainSelect(objectTypeDefinitionContext, fieldDefinitionContext, level);
        Optional<Expression> subWhereExpression = objectValueToMultipleExpression(fieldDefinitionContext.type(), inputValueDefinitionContext, objectValueContext, level);
        subWhereExpression.ifPresent(expression -> {
            if (body.getWhere() != null) {
                body.setWhere(new MultiAndExpression(Arrays.asList(body.getWhere(), expression)));
            } else {
                body.setWhere(expression);
            }
        });
        return existsExpression(body);
    }


    protected PlainSelect mapFieldPlainSelect(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext,
                                              GraphqlParser.FieldDefinitionContext fieldDefinitionContext,
                                              int level) {
        PlainSelect body = new PlainSelect();
        body.setSelectItems(Collections.singletonList(new AllColumns()));

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
                    subPlainSelect.setSelectItems(Collections.singletonList(new SelectExpressionItem(dbNameUtil.fieldToColumn(withTable, mapWithToFieldDefinition.get()))));
                    EqualsTo equalsWithTableColumn = new EqualsTo();
                    equalsWithTableColumn.setLeftExpression(dbNameUtil.fieldToColumn(withTable, mapWithFromFieldDefinition.get()));
                    equalsWithTableColumn.setRightExpression(dbNameUtil.fieldToColumn(parentTable, fromFieldDefinition.get()));
                    subPlainSelect.setWhere(equalsWithTableColumn);
                    subPlainSelect.setFromItem(withTable);
                    selectWithTable.setSelectBody(subPlainSelect);
                    idEqualsTo.setRightExpression(selectWithTable);
                }
            } else {
                idEqualsTo.setRightExpression(dbNameUtil.fieldToColumn(parentTable, fromFieldDefinition.get()));
            }
            body.setWhere(idEqualsTo);
        }
        return body;
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
        if (valueWithVariableContext.variable() != null) {
            equalsTo.setRightExpression(dbValueUtil.variableToJdbcNamedParameter(valueWithVariableContext.variable()));
        } else {
            equalsTo.setRightExpression(new StringValue(valueWithVariableContext.enumValue().enumValueName().getText()));
        }
        return equalsTo;
    }

    protected Expression scalarValueToExpression(Expression leftExpression,
                                                 GraphqlParser.ValueContext valueContext) {
        return scalarValueToExpression(leftExpression,
                valueContext.StringValue(),
                valueContext.IntValue(),
                valueContext.FloatValue(),
                valueContext.BooleanValue(),
                valueContext.NullValue(),
                null);
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
            IsExpression isExpression = new IsExpression();
            isExpression.setLeftExpression(leftExpression);
            isExpression.setRightExpression(dbValueUtil.variableToJdbcNamedParameter(valueWithVariableContext.variable()));
            return Optional.of(isExpression);
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
                throw new GraphQLProblem(NON_NULL_VALUE_NOT_EXIST.bind(inputValueDefinitionContext.getText()));
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
                case "Int":
                    colDataType.setDataType("INT");
                    break;
                case "Boolean":
                    colDataType.setDataType("BOOL");
                    break;
                case "String":
                    colDataType.setDataType("VARCHAR");
                    colDataType.setArgumentsStringList(Collections.singletonList("255"));
                    break;
                case "Float":
                    colDataType.setDataType("FLOAT");
                    break;
            }
        } else {
            //TODO
        }
        ColumnDefinition columnDefinition = new ColumnDefinition();
        columnDefinition.setColumnName(dbNameUtil.graphqlFieldNameToColumnName(valueWithVariableContext.variable().name().getText()));
        columnDefinition.setColDataType(colDataType);
        columnDefinition.setColumnSpecs(Arrays.asList("PATH", "'$'"));
        jsonTable.setColumnDefinitions(Collections.singletonList(columnDefinition));
        jsonTable.setAlias(new Alias(valueWithVariableContext.variable().name().getText()));

        PlainSelect body = new PlainSelect();
        body.setSelectItems(Collections.singletonList(new AllColumns()));
        body.setFromItem(jsonTable);
        subSelect.setSelectBody(body);
        return subSelect;
    }
}
