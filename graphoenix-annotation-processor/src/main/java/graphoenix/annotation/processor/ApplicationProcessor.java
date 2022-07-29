package graphoenix.annotation.processor;

import com.google.auto.service.AutoService;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.core.error.GraphQLErrorType;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.core.handler.GraphQLConfigRegister;
import io.graphoenix.core.schema.JsonSchemaTranslator;
import io.graphoenix.graphql.builder.schema.DocumentBuilder;
import io.graphoenix.core.document.Field;
import io.graphoenix.core.document.ObjectType;
import io.graphoenix.graphql.generator.translator.GraphQLApiBuilder;
import io.graphoenix.graphql.generator.translator.JavaElementToEnum;
import io.graphoenix.graphql.generator.translator.JavaElementToInputType;
import io.graphoenix.graphql.generator.translator.JavaElementToInterface;
import io.graphoenix.graphql.generator.translator.JavaElementToObject;
import io.graphoenix.java.generator.implementer.ConnectionHandlerBuilder;
import io.graphoenix.java.generator.implementer.InvokeHandlerBuilder;
import io.graphoenix.java.generator.implementer.OperationHandlerImplementer;
import io.graphoenix.java.generator.implementer.SelectionFilterBuilder;
import io.graphoenix.spi.annotation.Skip;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.antlr.IGraphQLFieldMapManager;
import io.vavr.Tuple2;
import org.eclipse.microprofile.graphql.Enum;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Input;
import org.eclipse.microprofile.graphql.Interface;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Query;
import org.eclipse.microprofile.graphql.Source;
import org.eclipse.microprofile.graphql.Type;
import org.tinylog.Logger;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static io.graphoenix.config.ConfigUtil.CONFIG_UTIL;
import static javax.lang.model.SourceVersion.RELEASE_11;

@SupportedAnnotationTypes("io.graphoenix.spi.annotation.GraphoenixApplication")
@SupportedSourceVersion(RELEASE_11)
@AutoService(Processor.class)
public class ApplicationProcessor extends AbstractProcessor {

    private IGraphQLDocumentManager manager;
    private DocumentBuilder documentBuilder;
    private JavaElementToEnum javaElementToEnum;
    private JavaElementToObject javaElementToObject;
    private JavaElementToInterface javaElementToInterface;
    private JavaElementToInputType javaElementToInputType;
    private GraphQLApiBuilder graphQLApiBuilder;
    private InvokeHandlerBuilder invokeHandlerBuilder;
    private ConnectionHandlerBuilder connectionHandlerBuilder;
    private SelectionFilterBuilder selectionFilterBuilder;
    private OperationHandlerImplementer operationHandlerImplementer;
    private JsonSchemaTranslator jsonSchemaTranslator;
    private GraphQLConfig graphQLConfig;
    private Types typeUtils;
    private Filer filer;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.typeUtils = processingEnv.getTypeUtils();
        this.filer = processingEnv.getFiler();
        BeanContext.load(ApplicationProcessor.class.getClassLoader());
        this.manager = BeanContext.get(IGraphQLDocumentManager.class);
        this.documentBuilder = BeanContext.get(DocumentBuilder.class);
        this.javaElementToEnum = BeanContext.get(JavaElementToEnum.class);
        this.javaElementToObject = BeanContext.get(JavaElementToObject.class);
        this.javaElementToInterface = BeanContext.get(JavaElementToInterface.class);
        this.javaElementToInputType = BeanContext.get(JavaElementToInputType.class);
        this.graphQLApiBuilder = BeanContext.get(GraphQLApiBuilder.class);
        this.invokeHandlerBuilder = BeanContext.get(InvokeHandlerBuilder.class);
        this.connectionHandlerBuilder = BeanContext.get(ConnectionHandlerBuilder.class);
        this.selectionFilterBuilder = BeanContext.get(SelectionFilterBuilder.class);
        this.operationHandlerImplementer = BeanContext.get(OperationHandlerImplementer.class);
        this.jsonSchemaTranslator = BeanContext.get(JsonSchemaTranslator.class);
        GraphQLConfigRegister configRegister = BeanContext.get(GraphQLConfigRegister.class);
        IGraphQLFieldMapManager mapper = BeanContext.get(IGraphQLFieldMapManager.class);
        graphQLConfig = CONFIG_UTIL.scan(filer).getOptionalValue(GraphQLConfig.class).orElseGet(GraphQLConfig::new);

        try {
            manager.clearAll();
            configRegister.registerPreset(ApplicationProcessor.class.getClassLoader());
            configRegister.registerConfig(graphQLConfig, filer);
            mapper.registerFieldMaps();
            if (graphQLConfig.getBuild()) {
                manager.registerGraphQL(documentBuilder.buildDocument().toString());
            }
        } catch (IOException | URISyntaxException e) {
            Logger.error(e);
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage());
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (annotations.isEmpty()) {
            return false;
        }
        roundEnv.getElementsAnnotatedWith(Enum.class).stream()
                .filter(element -> element.getAnnotation(Skip.class) == null)
                .filter(element -> element.getKind().equals(ElementKind.ENUM))
                .forEach(element -> manager.registerGraphQL(javaElementToEnum.buildEnum((TypeElement) element).toString()));

        roundEnv.getElementsAnnotatedWith(Interface.class).stream()
                .filter(element -> element.getAnnotation(Skip.class) == null)
                .filter(element -> element.getKind().equals(ElementKind.INTERFACE))
                .forEach(element -> {
                            manager.registerGraphQL(javaElementToInterface.buildInterface((TypeElement) element, typeUtils).toString());
                            element.getEnclosedElements().stream()
                                    .filter(subElement -> subElement.getAnnotation(Skip.class) == null)
                                    .filter(subElement -> subElement.getAnnotation(Enum.class) != null)
                                    .filter(subElement -> subElement.getKind().equals(ElementKind.ENUM))
                                    .forEach(subElement -> manager.registerGraphQL(javaElementToEnum.buildEnum((TypeElement) subElement).toString()));
                        }
                );

        roundEnv.getElementsAnnotatedWith(Type.class).stream()
                .filter(element -> element.getAnnotation(Skip.class) == null)
                .filter(element -> element.getKind().equals(ElementKind.CLASS))
                .forEach(element -> {
                            manager.registerGraphQL(javaElementToObject.buildObject((TypeElement) element, typeUtils).toString());
                            element.getEnclosedElements().stream()
                                    .filter(subElement -> subElement.getAnnotation(Skip.class) == null)
                                    .filter(subElement -> subElement.getAnnotation(Enum.class) != null)
                                    .filter(subElement -> subElement.getKind().equals(ElementKind.ENUM))
                                    .forEach(subElement -> manager.registerGraphQL(javaElementToEnum.buildEnum((TypeElement) subElement).toString()));
                        }
                );

        roundEnv.getElementsAnnotatedWith(Input.class).stream()
                .filter(element -> element.getAnnotation(Skip.class) == null)
                .filter(element -> element.getKind().equals(ElementKind.CLASS))
                .forEach(element -> {
                            manager.registerGraphQL(javaElementToInputType.buildInputType((TypeElement) element, typeUtils).toString());
                            element.getEnclosedElements().stream()
                                    .filter(subElement -> subElement.getAnnotation(Skip.class) == null)
                                    .filter(subElement -> subElement.getAnnotation(Enum.class) != null)
                                    .filter(subElement -> subElement.getKind().equals(ElementKind.ENUM))
                                    .forEach(subElement -> manager.registerGraphQL(javaElementToEnum.buildEnum((TypeElement) subElement).toString()));
                        }
                );

        roundEnv.getElementsAnnotatedWith(GraphQLApi.class).stream()
                .filter(element -> element.getKind().equals(ElementKind.CLASS))
                .forEach(element -> registerGraphQLApiElement(element, typeUtils));

        try {
            FileObject mainGraphQL = filer.createResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/graphql/main.gql");
            Writer writer = mainGraphQL.openWriter();
            writer.write(documentBuilder.getDocument().toString());
            writer.close();

            List<GraphqlParser.ObjectTypeDefinitionContext> schemaObjectList = manager.getObjects()
                    .filter(objectTypeDefinitionContext -> manager.isNotContainerType(objectTypeDefinitionContext.name().getText()))
                    .collect(Collectors.toList());

            for (GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext : schemaObjectList) {
                FileObject schema = filer.createResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/schema/".concat(objectTypeDefinitionContext.name().getText()).concat(".json"));
                writer = schema.openWriter();
                writer.write(jsonSchemaTranslator.objectToJsonSchemaString(objectTypeDefinitionContext));
                writer.close();
            }

            invokeHandlerBuilder.setConfiguration(graphQLConfig).writeToFiler(filer);
            connectionHandlerBuilder.setConfiguration(graphQLConfig).writeToFiler(filer);
            operationHandlerImplementer.setConfiguration(graphQLConfig).writeToFiler(filer);
            selectionFilterBuilder.setConfiguration(graphQLConfig).writeToFiler(filer);

        } catch (IOException e) {
            Logger.error(e);
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage());
        }
        return false;
    }

    private void registerGraphQLApiElement(Element element, Types typeUtils) {
        element.getEnclosedElements()
                .forEach(subElement -> {
                            if (subElement.getAnnotation(Query.class) != null && subElement.getKind().equals(ElementKind.METHOD)) {
                                GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext = manager.getQueryOperationTypeName().flatMap(name -> manager.getObject(name)).orElseThrow(() -> new GraphQLErrors(GraphQLErrorType.QUERY_TYPE_NOT_EXIST));
                                ObjectType objectType = documentBuilder.buildObject(objectTypeDefinitionContext).addField(graphQLApiBuilder.variableElementToField((ExecutableElement) subElement, typeUtils));
                                manager.registerGraphQL(objectType.toString());
                            } else if (subElement.getAnnotation(Mutation.class) != null && subElement.getKind().equals(ElementKind.METHOD)) {
                                GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext = manager.getMutationOperationTypeName().flatMap(name -> manager.getObject(name)).orElseThrow(() -> new GraphQLErrors(GraphQLErrorType.MUTATION_TYPE_NOT_EXIST));
                                ObjectType objectType = documentBuilder.buildObject(objectTypeDefinitionContext).addField(graphQLApiBuilder.variableElementToField((ExecutableElement) subElement, typeUtils));
                                manager.registerGraphQL(objectType.toString());
                            } else if (subElement.getKind().equals(ElementKind.METHOD) && ((ExecutableElement) subElement).getParameters().stream().anyMatch(variableElement -> variableElement.getAnnotation(Source.class) != null)) {
                                Tuple2<String, Field> objectField = graphQLApiBuilder.variableElementToObjectField((ExecutableElement) subElement, typeUtils);
                                GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext = manager.getObject(objectField._1()).orElseThrow(() -> new GraphQLErrors(GraphQLErrorType.TYPE_NOT_EXIST.bind(objectField._1())));
                                ObjectType objectType = documentBuilder.buildObject(objectTypeDefinitionContext).addField(objectField._2());
                                manager.registerGraphQL(objectType.toString());
                            }
                        }
                );
    }
}