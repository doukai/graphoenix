package graphoenix.annotation.processor;

import com.google.auto.service.AutoService;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.core.handler.GraphQLConfigRegister;
import io.graphoenix.core.schema.JsonSchemaTranslator;
import io.graphoenix.graphql.builder.schema.DocumentBuilder;
import io.graphoenix.java.generator.implementer.*;
import io.graphoenix.java.generator.implementer.grpc.*;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.antlr.IGraphQLFieldMapManager;
import org.tinylog.Logger;

import javax.annotation.processing.*;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        ReactorGrpcServiceImplementer reactorGrpcServiceImplementer = BeanContext.get(ReactorGrpcServiceImplementer.class);
        GrpcServerProducerBuilder grpcServerProducerBuilder = BeanContext.get(GrpcServerProducerBuilder.class);
        GrpcFetchHandlerBuilder grpcFetchHandlerBuilder = BeanContext.get(GrpcFetchHandlerBuilder.class);
        roundInit(roundEnv);

        try {
            GraphQLConfigRegister configRegister = BeanContext.get(GraphQLConfigRegister.class);
            configRegister.registerPackage(ApplicationProcessor.class.getClassLoader());
            if (graphQLConfig.getBuild()) {
                manager.registerGraphQL(documentBuilder.buildDocument().toString());
            } else {
                mapper.registerFieldMaps();
            }
            registerElements(roundEnv);
            registerOperations(roundEnv);
            FileObject mainGraphQL = filer.createResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/graphql/main.gql");
            Writer writer = mainGraphQL.openWriter();
            writer.write(documentBuilder.getDocument().toString());
            writer.close();

            List<GraphqlParser.InputObjectTypeDefinitionContext> inputObjectTypeDefinitionContextList = manager.getInputObjects()
                    .filter(inputObjectTypeDefinitionContext -> !manager.isInterface(inputObjectTypeDefinitionContext))
                    .collect(Collectors.toList());
            for (GraphqlParser.InputObjectTypeDefinitionContext inputObjectTypeDefinitionContext : inputObjectTypeDefinitionContextList) {
                for (Map.Entry<String, String> resultEntry : jsonSchemaTranslator.objectToJsonSchemaStringStream(inputObjectTypeDefinitionContext).collect(Collectors.toList())) {
                    FileObject schema = filer.createResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/schema/" + resultEntry.getKey());
                    writer = schema.openWriter();
                    writer.write(resultEntry.getValue());
                    writer.close();
                }
            }

            List<GraphqlParser.OperationTypeDefinitionContext> operationTypeDefinitionContextList = manager.getOperationTypeDefinition().collect(Collectors.toList());
            for (GraphqlParser.OperationTypeDefinitionContext operationTypeDefinitionContext : operationTypeDefinitionContextList) {
                Map.Entry<String, String> resultEntry = jsonSchemaTranslator.operationObjectToJsonSchemaString(operationTypeDefinitionContext);
                FileObject schema = filer.createResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/schema/" + resultEntry.getKey());
                writer = schema.openWriter();
                writer.write(resultEntry.getValue());
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
            reactorGrpcServiceImplementer.writeToFiler(filer);
            grpcServerProducerBuilder.writeToFiler(filer);
            grpcFetchHandlerBuilder.writeToFiler(filer);

        } catch (IOException | URISyntaxException e) {
            Logger.error(e);
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage());
        }
        return false;
    }
}
