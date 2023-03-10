package graphoenix.annotation.processor;

import com.google.auto.service.AutoService;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.context.BeanContext;
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

import javax.annotation.processing.AbstractProcessor;
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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static io.graphoenix.config.ConfigUtil.CONFIG_UTIL;
import static javax.lang.model.SourceVersion.RELEASE_11;

@SupportedAnnotationTypes("io.graphoenix.spi.annotation.Application")
@SupportedSourceVersion(RELEASE_11)
@AutoService(Processor.class)
public class ApplicationProcessor extends AbstractProcessor {

    private BaseProcessor baseProcessor;
    private IGraphQLDocumentManager manager;
    private DocumentBuilder documentBuilder;
    private InvokeHandlerBuilder invokeHandlerBuilder;
    private ConnectionHandlerBuilder connectionHandlerBuilder;
    private SelectionFilterBuilder selectionFilterBuilder;
    private OperationHandlerImplementer operationHandlerImplementer;
    private QueryDataLoaderBuilder queryDataLoaderBuilder;
    private MutationDataLoaderBuilder mutationDataLoaderBuilder;
    private QueryHandlerBuilder queryHandlerBuilder;
    private MutationHandlerBuilder mutationHandlerBuilder;
    private JsonSchemaTranslator jsonSchemaTranslator;
    private GraphQLConfig graphQLConfig;
    private Filer filer;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        filer = processingEnv.getFiler();
        graphQLConfig = CONFIG_UTIL.scan(filer).getOptionalValue(GraphQLConfig.class).orElseGet(GraphQLConfig::new);
        BeanContext.load(PackageProcessor.class.getClassLoader());
        manager = BeanContext.get(IGraphQLDocumentManager.class);
        documentBuilder = BeanContext.get(DocumentBuilder.class).setGraphQLConfig(graphQLConfig);
        jsonSchemaTranslator = BeanContext.get(JsonSchemaTranslator.class);
        invokeHandlerBuilder = BeanContext.get(InvokeHandlerBuilder.class);
        connectionHandlerBuilder = BeanContext.get(ConnectionHandlerBuilder.class);
        selectionFilterBuilder = BeanContext.get(SelectionFilterBuilder.class);
        operationHandlerImplementer = BeanContext.get(OperationHandlerImplementer.class);
        queryDataLoaderBuilder = BeanContext.get(QueryDataLoaderBuilder.class);
        mutationDataLoaderBuilder = BeanContext.get(MutationDataLoaderBuilder.class);
        queryHandlerBuilder = BeanContext.get(QueryHandlerBuilder.class);
        mutationHandlerBuilder = BeanContext.get(MutationHandlerBuilder.class);
        baseProcessor = BeanContext.get(BaseProcessor.class);
        baseProcessor.init(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (annotations.isEmpty()) {
            return false;
        }
        if (graphQLConfig.getPackageName() == null) {
            baseProcessor.getDefaultPackageName(roundEnv).ifPresent(packageName -> graphQLConfig.setPackageName(packageName));
        }
        baseProcessor.registerElements(roundEnv);
        try {
            if (graphQLConfig.getBuild()) {
                manager.registerGraphQL(documentBuilder.buildDocument().toString());
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

            invokeHandlerBuilder.setConfiguration(graphQLConfig).writeToFiler(filer);
            connectionHandlerBuilder.setConfiguration(graphQLConfig).writeToFiler(filer);
            operationHandlerImplementer.setConfiguration(graphQLConfig).writeToFiler(filer);
            selectionFilterBuilder.setConfiguration(graphQLConfig).writeToFiler(filer);
            queryDataLoaderBuilder.setConfiguration(graphQLConfig).writeToFiler(filer);
            mutationDataLoaderBuilder.setConfiguration(graphQLConfig).writeToFiler(filer);
            queryHandlerBuilder.setConfiguration(graphQLConfig).writeToFiler(filer);
            mutationHandlerBuilder.setConfiguration(graphQLConfig).writeToFiler(filer);

        } catch (IOException e) {
            Logger.error(e);
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage());
        }
        return false;
    }
}
