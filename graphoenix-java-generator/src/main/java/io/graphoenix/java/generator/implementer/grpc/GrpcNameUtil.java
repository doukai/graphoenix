package io.graphoenix.java.generator.implementer.grpc;

import com.google.common.base.CaseFormat;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.dto.type.OperationType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import static io.graphoenix.core.error.GraphQLErrorType.UNSUPPORTED_OPERATION_TYPE;
import static io.graphoenix.spi.constant.Hammurabi.INTROSPECTION_PREFIX;
import static io.graphoenix.spi.constant.Hammurabi.LIST_SUFFIX;

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
            return "intro" + getUpperCamelName(name.replaceFirst(INTROSPECTION_PREFIX, ""));
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
        return "_" + CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, getGrpcTypeName(typeContext));
    }

    public String getGrpcGetMethodName(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return "get" + getUpperCamelName(getGrpcFieldName(inputValueDefinitionContext));
    }

    public String getGrpcHasMethodName(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return "has" + getUpperCamelName(getGrpcFieldName(inputValueDefinitionContext));
    }

    public String getGrpcGetListMethodName(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return "get" + getUpperCamelName(getGrpcFieldName(inputValueDefinitionContext)) + LIST_SUFFIX;
    }

    public String getGrpcGetCountMethodName(GraphqlParser.InputValueDefinitionContext inputValueDefinitionContext) {
        return "get" + getUpperCamelName(getGrpcFieldName(inputValueDefinitionContext)) + "Count";
    }

    public String getGetMethodName(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        String name = fieldDefinitionContext.name().getText();
        if (name.startsWith(INTROSPECTION_PREFIX)) {
            return "get" + name;
        }
        return "get" + getUpperCamelName(name);
    }

    public String getGrpcSetMethodName(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return "set" + getUpperCamelName(getGrpcFieldName(fieldDefinitionContext));
    }

    public String getGrpcAddAllMethodName(GraphqlParser.FieldDefinitionContext fieldDefinitionContext) {
        return "addAll" + getUpperCamelName(getGrpcFieldName(fieldDefinitionContext));
    }

    public String packageNameToUnderline(String packageName) {
        return String.join("_", packageName.split("\\."));
    }

    public String getGraphQLServiceStubParameterName(String packageName) {
        return packageNameToUnderline(packageName) + "_GraphQLServiceStub";
    }

    public String getGrpcRequestClassName(GraphqlParser.FieldDefinitionContext fieldDefinitionContext, OperationType operationType) {
        switch (operationType) {
            case QUERY:
                return "Query" + getUpperCamelName(getGrpcFieldName(fieldDefinitionContext)) + "Request";
            case MUTATION:
                return "Mutation" + getUpperCamelName(getGrpcFieldName(fieldDefinitionContext)) + "Request";
            default:
                throw new GraphQLErrors(UNSUPPORTED_OPERATION_TYPE);
        }
    }

    public String getGrpcResponseClassName(GraphqlParser.FieldDefinitionContext fieldDefinitionContext, OperationType operationType) {
        switch (operationType) {
            case QUERY:
                return "Query" + getUpperCamelName(getGrpcFieldName(fieldDefinitionContext)) + "Response";
            case MUTATION:
                return "Mutation" + getUpperCamelName(getGrpcFieldName(fieldDefinitionContext)) + "Response";
            default:
                throw new GraphQLErrors(UNSUPPORTED_OPERATION_TYPE);
        }
    }

    public String getTypeInvokeMethodName(GraphqlParser.TypeContext typeContext) {
        String name = manager.getFieldTypeName(typeContext);
        if (name.startsWith(INTROSPECTION_PREFIX)) {
            return INTROSPECTION_PREFIX + getLowerCamelName(name.replaceFirst(INTROSPECTION_PREFIX, ""));
        }
        return getLowerCamelName(name);
    }
}
