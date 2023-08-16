package graphoenix.annotation.processor;

import com.google.auto.service.AutoService;
import com.google.common.base.CaseFormat;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.core.error.GraphQLErrorType;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.core.handler.GraphQLConfigRegister;
import io.graphoenix.core.schema.JsonSchemaTranslator;
import io.graphoenix.graphql.builder.schema.DocumentBuilder;
import io.graphoenix.java.generator.implementer.*;
import io.graphoenix.java.generator.implementer.grpc.GrpcFetchHandlerBuilder;
import io.graphoenix.java.generator.implementer.grpc.GrpcInputObjectHandlerBuilder;
import io.graphoenix.java.generator.implementer.grpc.GrpcObjectHandlerBuilder;
import io.graphoenix.java.generator.implementer.grpc.GrpcRequestHandlerBuilder;
import io.graphoenix.java.generator.implementer.grpc.GrpcServiceImplementer;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.antlr.IGraphQLFieldMapManager;
import org.tinylog.Logger;

import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

import static javax.lang.model.SourceVersion.RELEASE_11;

@SupportedAnnotationTypes("io.graphoenix.spi.annotation.Application")
@SupportedSourceVersion(RELEASE_11)
@AutoService(Processor.class)
public class ApplicationProcessor extends BaseProcessor {

    private Filer filer;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        filer = processingEnv.getFiler();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (annotations.isEmpty()) {
            return false;
        }
        GraphQLConfig graphQLConfig = BeanContext.get(GraphQLConfig.class);
        IGraphQLDocumentManager manager = BeanContext.get(IGraphQLDocumentManager.class);
        IGraphQLFieldMapManager mapper = BeanContext.get(IGraphQLFieldMapManager.class);
        DocumentBuilder documentBuilder = BeanContext.get(DocumentBuilder.class);
        JsonSchemaTranslator jsonSchemaTranslator = BeanContext.get(JsonSchemaTranslator.class);
        InvokeHandlerBuilder invokeHandlerBuilder = BeanContext.get(InvokeHandlerBuilder.class);
        InputInvokeHandlerBuilder inputInvokeHandlerBuilder = BeanContext.get(InputInvokeHandlerBuilder.class);
        ArgumentsInvokeHandlerBuilder argumentsInvokeHandlerBuilder = BeanContext.get(ArgumentsInvokeHandlerBuilder.class);
        ConnectionHandlerBuilder connectionHandlerBuilder = BeanContext.get(ConnectionHandlerBuilder.class);
        SelectionFilterBuilder selectionFilterBuilder = BeanContext.get(SelectionFilterBuilder.class);
        OperationHandlerImplementer operationHandlerImplementer = BeanContext.get(OperationHandlerImplementer.class);
        QueryDataLoaderBuilder queryDataLoaderBuilder = BeanContext.get(QueryDataLoaderBuilder.class);
        MutationDataLoaderBuilder mutationDataLoaderBuilder = BeanContext.get(MutationDataLoaderBuilder.class);
        QueryHandlerBuilder queryHandlerBuilder = BeanContext.get(QueryHandlerBuilder.class);
        MutationHandlerBuilder mutationHandlerBuilder = BeanContext.get(MutationHandlerBuilder.class);
        OperationInterfaceImplementer operationInterfaceImplementer = BeanContext.get(OperationInterfaceImplementer.class);

        GrpcInputObjectHandlerBuilder grpcInputObjectHandlerBuilder = BeanContext.get(GrpcInputObjectHandlerBuilder.class);
        GrpcObjectHandlerBuilder grpcObjectHandlerBuilder = BeanContext.get(GrpcObjectHandlerBuilder.class);
        GrpcRequestHandlerBuilder grpcRequestHandlerBuilder = BeanContext.get(GrpcRequestHandlerBuilder.class);
        GrpcServiceImplementer grpcServiceImplementer = BeanContext.get(GrpcServiceImplementer.class);
        GrpcFetchHandlerBuilder grpcFetchHandlerBuilder = BeanContext.get(GrpcFetchHandlerBuilder.class);

        registerElements(roundEnv);
        try {
            GraphQLConfigRegister configRegister = BeanContext.get(GraphQLConfigRegister.class);
            configRegister.registerPackage(ApplicationProcessor.class.getClassLoader());
            if (graphQLConfig.getBuild()) {
                manager.registerGraphQL(documentBuilder.buildDocument().toString());
            } else {
                mapper.registerFieldMaps();
            }
            registerOperations(roundEnv);
            FileObject mainGraphQL = filer.createResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/graphql/main.gql");
            Writer writer = mainGraphQL.openWriter();
            writer.write(documentBuilder.getDocument().toString());
            writer.close();

            List<GraphqlParser.InputObjectTypeDefinitionContext> inputObjectTypeDefinitionContextList = manager.getInputObjects().collect(Collectors.toList());

            for (GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext : inputObjectTypeDefinitionContextList) {
                FileObject schema = filer.createResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/schema/".concat(inputObjectTypeDefinitionContext.name().getText()));
                writer = schema.openWriter();
                writer.write(jsonSchemaTranslator.objectToJsonSchemaString(inputObjectTypeDefinitionContext));
                writer.close();
            }

            List<GraphqlParser.OperationTypeDefinitionContext> operationTypeDefinitionContextList = manager.getOperationTypeDefinition().collect(Collectors.toList());

            for (GraphqlParser.OperationTypeDefinitionContext operationTypeDefinitionContext : operationTypeDefinitionContextList) {
                GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext = manager.getObject(operationTypeDefinitionContext.typeName().name().getText())
                        .orElseThrow(() -> new GraphQLErrors(GraphQLErrorType.TYPE_NOT_EXIST.bind(operationTypeDefinitionContext.typeName().name().getText())));
                FileObject schema = filer.createResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/schema/".concat(objectTypeDefinitionContext.name().getText()));
                writer = schema.openWriter();
                writer.write(jsonSchemaTranslator.operationObjectToJsonSchemaString(operationTypeDefinitionContext));
                writer.close();
            }

            List<Map.Entry<GraphqlParser.ObjectTypeDefinitionContext, GraphqlParser.FieldDefinitionContext>> mutationTypeFieldDefinitionContextList = manager.getOperationTypeDefinition()
                    .filter(operationTypeDefinitionContext -> operationTypeDefinitionContext.operationType().MUTATION() != null)
                    .flatMap(operationTypeDefinitionContext -> manager.getObject(operationTypeDefinitionContext.typeName().name().getText()).stream())
                    .flatMap(objectTypeDefinitionContext ->
                            objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                                    .map(fieldDefinitionContext -> new AbstractMap.SimpleEntry<>(objectTypeDefinitionContext, fieldDefinitionContext))
                    )
                    .collect(Collectors.toList());

            for (Map.Entry<GraphqlParser.ObjectTypeDefinitionContext, GraphqlParser.FieldDefinitionContext> entry : mutationTypeFieldDefinitionContextList) {
                FileObject schema = filer.createResource(
                        StandardLocation.CLASS_OUTPUT,
                        "",
                        "META-INF/schema/"
                                .concat(entry.getKey().name().getText())
                                .concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, entry.getValue().name().getText()))
                                .concat("UpdateById")
                );
                writer = schema.openWriter();
                writer.write(jsonSchemaTranslator.operationObjectFieldUpdateByIdArgumentsToJsonSchemaString(entry.getKey(), entry.getValue()));
                writer.close();
                schema = filer.createResource(
                        StandardLocation.CLASS_OUTPUT,
                        "",
                        "META-INF/schema/"
                                .concat(entry.getKey().name().getText())
                                .concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, entry.getValue().name().getText()))
                                .concat("UpdateByWhere")
                );
                writer = schema.openWriter();
                writer.write(jsonSchemaTranslator.operationObjectFieldUpdateByWhereArgumentsToJsonSchemaString(entry.getKey(), entry.getValue()));
                writer.close();
                if (manager.fieldTypeIsList(entry.getValue().type())) {
                    schema = filer.createResource(
                            StandardLocation.CLASS_OUTPUT,
                            "",
                            "META-INF/schema/"
                                    .concat(entry.getKey().name().getText())
                                    .concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, entry.getValue().name().getText()))
                    );
                    writer = schema.openWriter();
                    writer.write(jsonSchemaTranslator.operationObjectFieldListArgumentsToJsonSchemaString(entry.getKey(), entry.getValue()));
                    writer.close();
                }
            }

            List<Map.Entry<GraphqlParser.ObjectTypeDefinitionContext, GraphqlParser.FieldDefinitionContext>> queryTypeFieldDefinitionContextList = manager.getOperationTypeDefinition()
                    .filter(operationTypeDefinitionContext -> operationTypeDefinitionContext.operationType().MUTATION() == null)
                    .flatMap(operationTypeDefinitionContext -> manager.getObject(operationTypeDefinitionContext.typeName().name().getText()).stream())
                    .flatMap(objectTypeDefinitionContext ->
                            objectTypeDefinitionContext.fieldsDefinition().fieldDefinition().stream()
                                    .map(fieldDefinitionContext -> new AbstractMap.SimpleEntry<>(objectTypeDefinitionContext, fieldDefinitionContext))
                    )
                    .collect(Collectors.toList());

            for (Map.Entry<GraphqlParser.ObjectTypeDefinitionContext, GraphqlParser.FieldDefinitionContext> entry : queryTypeFieldDefinitionContextList) {
                FileObject schema = filer.createResource(
                        StandardLocation.CLASS_OUTPUT,
                        "",
                        "META-INF/schema/"
                                .concat(entry.getKey().name().getText())
                                .concat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, entry.getValue().name().getText()))
                );
                writer = schema.openWriter();
                writer.write(jsonSchemaTranslator.operationObjectFieldArgumentsToJsonSchemaString(entry.getKey(), entry.getValue()));
                writer.close();
            }

            invokeHandlerBuilder.writeToFiler(filer);
            inputInvokeHandlerBuilder.writeToFiler(filer);
            argumentsInvokeHandlerBuilder.writeToFiler(filer);
            connectionHandlerBuilder.writeToFiler(filer);
            operationHandlerImplementer.writeToFiler(filer);
            selectionFilterBuilder.writeToFiler(filer);
            queryDataLoaderBuilder.writeToFiler(filer);
            mutationDataLoaderBuilder.writeToFiler(filer);
            queryHandlerBuilder.writeToFiler(filer);
            mutationHandlerBuilder.writeToFiler(filer);
            operationInterfaceImplementer.writeToFiler(filer);

            grpcInputObjectHandlerBuilder.writeToFiler(filer);
            grpcObjectHandlerBuilder.writeToFiler(filer);
            grpcRequestHandlerBuilder.writeToFiler(filer);
            grpcServiceImplementer.writeToFiler(filer);
            grpcFetchHandlerBuilder.writeToFiler(filer);

        } catch (IOException | URISyntaxException e) {
            Logger.error(e);
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage());
        }
        return false;
    }
}
