package io.graphoenix.java.generator.implementer.grpc;

import com.google.common.base.CaseFormat;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.dto.type.OperationType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.stream.Stream;

import static io.graphoenix.core.error.GraphQLErrorType.ARGUMENT_NOT_EXIST;
import static io.graphoenix.core.error.GraphQLErrorType.UNSUPPORTED_OPERATION_TYPE;
import static io.graphoenix.core.utils.DocumentUtil.DOCUMENT_UTIL;
import static io.graphoenix.spi.constant.Hammurabi.FETCH_DIRECTIVE_NAME;
import static io.graphoenix.spi.constant.Hammurabi.INTROSPECTION_PREFIX;

@ApplicationScoped
public class GrpcNameUtil {

    private final IGraphQLDocumentManager manager;

    @Inject
    public GrpcNameUtil(IGraphQLDocumentManager manager) {
        this.manager = manager;
    }

    public String getGrpcTypeName(String name) {
        return name.replaceFirst(INTROSPECTION_PREFIX, "Intro");
    }

    public String getGrpcFieldName(String name) {
        if (name.startsWith(INTROSPECTION_PREFIX)) {
            return "intro".concat(getUpperCamelName(name.replaceFirst(INTROSPECTION_PREFIX, "")));
        }
        return name;
    }

    public String getLowerCamelName(String name) {
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, name);
    }

    public String getUpperCamelName(String name) {
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, name);
    }

    public String getGrpcTypeName(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext) {
        return getGrpcTypeName(inputObjectTypeDefinitionContext.name().getText());
    }

    public String getGrpcTypeName(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        return getGrpcTypeName(objectTypeDefinitionContext.name().getText());
    }

    public String getGrpcTypeName(GraphqlParser.TypeContext typeContext) {
        return getGrpcTypeName(manager.getFieldTypeName(typeContext));
    }

    public String getGrpcFieldName(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return getGrpcFieldName(inputValueDefinitionContext.name().getText());
    }

    public String getGrpcFieldName(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return getGrpcFieldName(fieldDefinitionContext.name().getText());
    }

    public String getLowerCamelName(GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext) {
        return getLowerCamelName(getGrpcTypeName(inputObjectTypeDefinitionContext));
    }

    public String getLowerCamelName(GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext) {
        return getLowerCamelName(getGrpcTypeName(objectTypeDefinitionContext));
    }

    public String getLowerCamelName(GraphqlParser.TypeContext typeContext) {
        return getLowerCamelName(getGrpcTypeName(typeContext));
    }

    public String getGrpcEnumValueSuffixName(GraphqlParser.TypeContext typeContext) {
        return "_".concat(CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, getGrpcTypeName(typeContext)));
    }

    public String getPackageName(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return Stream.ofNullable(fieldDefinitionContext.directives())
                .flatMap(directivesContext -> directivesContext.directive().stream())
                .filter(directiveContext -> directiveContext.name().getText().equals(FETCH_DIRECTIVE_NAME))
                .flatMap(directiveContext -> directiveContext.arguments().argument().stream())
                .filter(argumentContext -> argumentContext.name().getText().equals("packageName"))
                .filter(argumentContext -> argumentContext.valueWithVariable().StringValue() != null)
                .map(argumentContext -> DOCUMENT_UTIL.getStringValue(argumentContext.valueWithVariable().StringValue()))
                .findFirst()
                .orElseThrow(() -> new GraphQLErrors(ARGUMENT_NOT_EXIST.bind("packageName")));
    }

    public String getKey(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return Stream.ofNullable(fieldDefinitionContext.directives())
                .flatMap(directivesContext -> directivesContext.directive().stream())
                .filter(directiveContext -> directiveContext.name().getText().equals(FETCH_DIRECTIVE_NAME))
                .flatMap(directiveContext -> directiveContext.arguments().argument().stream())
                .filter(argumentContext -> argumentContext.name().getText().equals("key"))
                .filter(argumentContext -> argumentContext.valueWithVariable().StringValue() != null)
                .map(argumentContext -> DOCUMENT_UTIL.getStringValue(argumentContext.valueWithVariable().StringValue()))
                .findFirst()
                .orElseThrow(() -> new GraphQLErrors(ARGUMENT_NOT_EXIST.bind("key")));
    }

    public String getFrom(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return Stream.ofNullable(fieldDefinitionContext.directives())
                .flatMap(directivesContext -> directivesContext.directive().stream())
                .filter(directiveContext -> directiveContext.name().getText().equals(FETCH_DIRECTIVE_NAME))
                .flatMap(directiveContext -> directiveContext.arguments().argument().stream())
                .filter(argumentContext -> argumentContext.name().getText().equals("from"))
                .filter(argumentContext -> argumentContext.valueWithVariable().StringValue() != null)
                .map(argumentContext -> DOCUMENT_UTIL.getStringValue(argumentContext.valueWithVariable().StringValue()))
                .findFirst()
                .orElseThrow(() -> new GraphQLErrors(ARGUMENT_NOT_EXIST.bind("from")));
    }

    public String getTo(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return Stream.ofNullable(fieldDefinitionContext.directives())
                .flatMap(directivesContext -> directivesContext.directive().stream())
                .filter(directiveContext -> directiveContext.name().getText().equals(FETCH_DIRECTIVE_NAME))
                .flatMap(directiveContext -> directiveContext.arguments().argument().stream())
                .filter(argumentContext -> argumentContext.name().getText().equals("to"))
                .filter(argumentContext -> argumentContext.valueWithVariable().StringValue() != null)
                .map(argumentContext -> DOCUMENT_UTIL.getStringValue(argumentContext.valueWithVariable().StringValue()))
                .findFirst()
                .orElseThrow(() -> new GraphQLErrors(ARGUMENT_NOT_EXIST.bind("to")));
    }

    public boolean getAnchor(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return Stream.ofNullable(fieldDefinitionContext.directives())
                .flatMap(directivesContext -> directivesContext.directive().stream())
                .filter(directiveContext -> directiveContext.name().getText().equals(FETCH_DIRECTIVE_NAME))
                .flatMap(directiveContext -> directiveContext.arguments().argument().stream())
                .filter(argumentContext -> argumentContext.name().getText().equals("anchor"))
                .filter(argumentContext -> argumentContext.valueWithVariable().BooleanValue() != null)
                .map(argumentContext -> Boolean.parseBoolean(argumentContext.valueWithVariable().BooleanValue().getText()))
                .findFirst()
                .orElse(false);
    }

    public String getGrpcGetMethodName(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return "get".concat(getUpperCamelName(getGrpcFieldName(inputValueDefinitionContext)));
    }

    public String getGrpcHasMethodName(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return "has".concat(getUpperCamelName(getGrpcFieldName(inputValueDefinitionContext)));
    }

    public String getGrpcGetListMethodName(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return "get".concat(getUpperCamelName(getGrpcFieldName(inputValueDefinitionContext))).concat("List");
    }

    public String getGrpcGetCountMethodName(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return "get".concat(getUpperCamelName(getGrpcFieldName(inputValueDefinitionContext))).concat("Count");
    }

    public String getGetMethodName(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        String name = fieldDefinitionContext.name().getText();
        if (name.startsWith(INTROSPECTION_PREFIX)) {
            return "get".concat(name);
        }
        return "get".concat(getUpperCamelName(name));
    }

    public String getGrpcSetMethodName(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return "set".concat(getUpperCamelName(getGrpcFieldName(fieldDefinitionContext)));
    }

    public String getGrpcAddAllMethodName(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return "addAll".concat(getUpperCamelName(getGrpcFieldName(fieldDefinitionContext)));
    }

    public String packageNameToUnderline(String packageName) {
        return String.join("_", packageName.split("\\."));
    }

    public String getGraphQLServiceStubParameterName(String packageName) {
        return packageNameToUnderline(packageName).concat("_GraphQLServiceStub");
    }

    public String getGrpcRequestClassName(GraphqlParser.FieldDefinitionContext fieldDefinitionContext, OperationType operationType) {
        switch (operationType) {
            case QUERY:
                return "Query".concat(getUpperCamelName(getGrpcFieldName(fieldDefinitionContext))).concat("Request");
            case MUTATION:
                return "Mutation".concat(getUpperCamelName(getGrpcFieldName(fieldDefinitionContext))).concat("Request");
            default:
                throw new GraphQLErrors(UNSUPPORTED_OPERATION_TYPE);
        }
    }

    public String getGrpcResponseClassName(GraphqlParser.FieldDefinitionContext fieldDefinitionContext, OperationType operationType) {
        switch (operationType) {
            case QUERY:
                return "Query".concat(getUpperCamelName(getGrpcFieldName(fieldDefinitionContext))).concat("Response");
            case MUTATION:
                return "Mutation".concat(getUpperCamelName(getGrpcFieldName(fieldDefinitionContext))).concat("Response");
            default:
                throw new GraphQLErrors(UNSUPPORTED_OPERATION_TYPE);
        }
    }

    public String getTypeInvokeMethodName(GraphqlParser.TypeContext typeContext) {
        String name = manager.getFieldTypeName(typeContext);
        if (name.startsWith(INTROSPECTION_PREFIX)) {
            return INTROSPECTION_PREFIX.concat(getLowerCamelName(name.replaceFirst(INTROSPECTION_PREFIX, "")));
        }
        return getLowerCamelName(name);
    }
}
