package graphoenix.annotation.processor;

import com.google.auto.service.AutoService;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.core.handler.GraphQLConfigRegister;
import io.graphoenix.core.schema.JsonSchemaTranslator;
import io.graphoenix.graphql.builder.schema.DocumentBuilder;
import io.graphoenix.java.generator.implementer.ConnectionHandlerBuilder;
import io.graphoenix.java.generator.implementer.InvokeHandlerBuilder;
import io.graphoenix.java.generator.implementer.MutationDataLoaderBuilder;
import io.graphoenix.java.generator.implementer.MutationHandlerBuilder;
import io.graphoenix.java.generator.implementer.OperationHandlerImplementer;
import io.graphoenix.java.generator.implementer.QueryDataLoaderBuilder;
import io.graphoenix.java.generator.implementer.QueryHandlerBuilder;
import io.graphoenix.java.generator.implementer.SelectionFilterBuilder;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
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
import java.util.List;
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
        DocumentBuilder documentBuilder = BeanContext.get(DocumentBuilder.class);
        JsonSchemaTranslator jsonSchemaTranslator = BeanContext.get(JsonSchemaTranslator.class);
        InvokeHandlerBuilder invokeHandlerBuilder = BeanContext.get(InvokeHandlerBuilder.class);
        ConnectionHandlerBuilder connectionHandlerBuilder = BeanContext.get(ConnectionHandlerBuilder.class);
        SelectionFilterBuilder selectionFilterBuilder = BeanContext.get(SelectionFilterBuilder.class);
        OperationHandlerImplementer operationHandlerImplementer = BeanContext.get(OperationHandlerImplementer.class);
        QueryDataLoaderBuilder queryDataLoaderBuilder = BeanContext.get(QueryDataLoaderBuilder.class);
        MutationDataLoaderBuilder mutationDataLoaderBuilder = BeanContext.get(MutationDataLoaderBuilder.class);
        QueryHandlerBuilder queryHandlerBuilder = BeanContext.get(QueryHandlerBuilder.class);
        MutationHandlerBuilder mutationHandlerBuilder = BeanContext.get(MutationHandlerBuilder.class);
        registerElements(roundEnv);
        try {
            GraphQLConfigRegister configRegister = BeanContext.get(GraphQLConfigRegister.class);
            configRegister.registerPreset(ApplicationProcessor.class.getClassLoader());
            if (graphQLConfig.getBuild()) {
                manager.registerGraphQL(documentBuilder.buildDocumentWithClassName().toString());
            }
            FileObject mainGraphQL = filer.createResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/graphql/main.gql");
            Writer writer = mainGraphQL.openWriter();
            writer.write(documentBuilder.getDocument().toString());
            writer.close();

            List<GraphqlParser.ObjectTypeDefinitionContext> schemaObjectList = manager.getObjects()
                    .filter(manager::isNotContainerType)
                    .collect(Collectors.toList());

            for (GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext : schemaObjectList) {
                FileObject schema = filer.createResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/schema/".concat(objectTypeDefinitionContext.name().getText()));
                writer = schema.openWriter();
                writer.write(jsonSchemaTranslator.objectToJsonSchemaString(objectTypeDefinitionContext));
                writer.close();
                schema = filer.createResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/schema/".concat(objectTypeDefinitionContext.name().getText().concat("List")));
                writer = schema.openWriter();
                writer.write(jsonSchemaTranslator.objectListToJsonSchemaString(objectTypeDefinitionContext));
                writer.close();
                schema = filer.createResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/schema/".concat(objectTypeDefinitionContext.name().getText().concat("Update")));
                writer = schema.openWriter();
                writer.write(jsonSchemaTranslator.objectToJsonSchemaString(objectTypeDefinitionContext, true));
                writer.close();
                schema = filer.createResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/schema/".concat(objectTypeDefinitionContext.name().getText().concat("ListUpdate")));
                writer = schema.openWriter();
                writer.write(jsonSchemaTranslator.objectListToJsonSchemaString(objectTypeDefinitionContext, true));
                writer.close();
            }
            invokeHandlerBuilder.writeToFiler(filer);
            connectionHandlerBuilder.writeToFiler(filer);
            operationHandlerImplementer.writeToFiler(filer);
            selectionFilterBuilder.writeToFiler(filer);
            queryDataLoaderBuilder.writeToFiler(filer);
            mutationDataLoaderBuilder.writeToFiler(filer);
            queryHandlerBuilder.writeToFiler(filer);
            mutationHandlerBuilder.writeToFiler(filer);
        } catch (IOException | URISyntaxException e) {
            Logger.error(e);
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage());
        }
        return false;
    }
}
