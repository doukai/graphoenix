package graphoenix.annotation.processor;

import com.google.auto.service.AutoService;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.core.document.Field;
import io.graphoenix.core.document.ObjectType;
import io.graphoenix.core.error.GraphQLErrorType;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.core.handler.GraphQLConfigRegister;
import io.graphoenix.graphql.builder.schema.DocumentBuilder;
import io.graphoenix.graphql.generator.translator.GraphQLApiBuilder;
import io.graphoenix.graphql.generator.translator.JavaElementToEnum;
import io.graphoenix.graphql.generator.translator.JavaElementToInputType;
import io.graphoenix.graphql.generator.translator.JavaElementToInterface;
import io.graphoenix.graphql.generator.translator.JavaElementToObject;
import io.graphoenix.spi.annotation.Ignore;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
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
import java.util.Set;

import static io.graphoenix.config.ConfigUtil.CONFIG_UTIL;
import static javax.lang.model.SourceVersion.RELEASE_11;

@SupportedAnnotationTypes({
        "org.eclipse.microprofile.graphql.GraphQLApi",
        "org.eclipse.microprofile.graphql.Type",
        "org.eclipse.microprofile.graphql.Enum",
        "org.eclipse.microprofile.graphql.Interface",
        "org.eclipse.microprofile.graphql.Input"
})
@SupportedSourceVersion(RELEASE_11)
@AutoService(Processor.class)
public class GraphQLApiProcessor extends AbstractProcessor {

    private IGraphQLDocumentManager manager;
    private DocumentBuilder documentBuilder;
    private JavaElementToEnum javaElementToEnum;
    private JavaElementToObject javaElementToObject;
    private JavaElementToInterface javaElementToInterface;
    private JavaElementToInputType javaElementToInputType;
    private GraphQLApiBuilder graphQLApiBuilder;
    private Types typeUtils;
    private Filer filer;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.typeUtils = processingEnv.getTypeUtils();
        this.filer = processingEnv.getFiler();
        BeanContext.load(GraphQLApiProcessor.class.getClassLoader());
        this.manager = BeanContext.get(IGraphQLDocumentManager.class);
        this.javaElementToEnum = BeanContext.get(JavaElementToEnum.class);
        this.javaElementToObject = BeanContext.get(JavaElementToObject.class);
        this.javaElementToInterface = BeanContext.get(JavaElementToInterface.class);
        this.javaElementToInputType = BeanContext.get(JavaElementToInputType.class);
        this.graphQLApiBuilder = BeanContext.get(GraphQLApiBuilder.class);
        GraphQLConfigRegister configRegister = BeanContext.get(GraphQLConfigRegister.class);
        GraphQLConfig graphQLConfig = CONFIG_UTIL.scan(filer).getOptionalValue(GraphQLConfig.class).orElseGet(GraphQLConfig::new);
        this.documentBuilder = BeanContext.get(DocumentBuilder.class).setGraphQLConfig(graphQLConfig);
        try {
            manager.clearAll();
            configRegister.registerConfig(graphQLConfig, filer);
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
                .filter(element -> element.getAnnotation(Ignore.class) == null)
                .filter(element -> element.getKind().equals(ElementKind.ENUM))
                .forEach(element -> manager.registerGraphQL(javaElementToEnum.buildEnum((TypeElement) element).toString()));

        roundEnv.getElementsAnnotatedWith(Interface.class).stream()
                .filter(element -> element.getAnnotation(Ignore.class) == null)
                .filter(element -> element.getKind().equals(ElementKind.INTERFACE))
                .forEach(element -> {
                            manager.registerGraphQL(javaElementToInterface.buildInterface((TypeElement) element, typeUtils).toString());
                            element.getEnclosedElements().stream()
                                    .filter(subElement -> subElement.getAnnotation(Ignore.class) == null)
                                    .filter(subElement -> subElement.getAnnotation(Enum.class) != null)
                                    .filter(subElement -> subElement.getKind().equals(ElementKind.ENUM))
                                    .forEach(subElement -> manager.registerGraphQL(javaElementToEnum.buildEnum((TypeElement) subElement).toString()));
                        }
                );

        roundEnv.getElementsAnnotatedWith(Type.class).stream()
                .filter(element -> element.getAnnotation(Ignore.class) == null)
                .filter(element -> element.getKind().equals(ElementKind.CLASS))
                .forEach(element -> {
                            manager.registerGraphQL(javaElementToObject.buildObject((TypeElement) element, typeUtils).toString());
                            element.getEnclosedElements().stream()
                                    .filter(subElement -> subElement.getAnnotation(Ignore.class) == null)
                                    .filter(subElement -> subElement.getAnnotation(Enum.class) != null)
                                    .filter(subElement -> subElement.getKind().equals(ElementKind.ENUM))
                                    .forEach(subElement -> manager.registerGraphQL(javaElementToEnum.buildEnum((TypeElement) subElement).toString()));
                        }
                );

        roundEnv.getElementsAnnotatedWith(Input.class).stream()
                .filter(element -> element.getAnnotation(Ignore.class) == null)
                .filter(element -> element.getKind().equals(ElementKind.CLASS))
                .forEach(element -> {
                            manager.registerGraphQL(javaElementToInputType.buildInputType((TypeElement) element, typeUtils).toString());
                            element.getEnclosedElements().stream()
                                    .filter(subElement -> subElement.getAnnotation(Ignore.class) == null)
                                    .filter(subElement -> subElement.getAnnotation(Enum.class) != null)
                                    .filter(subElement -> subElement.getKind().equals(ElementKind.ENUM))
                                    .forEach(subElement -> manager.registerGraphQL(javaElementToEnum.buildEnum((TypeElement) subElement).toString()));
                        }
                );

        roundEnv.getElementsAnnotatedWith(GraphQLApi.class).stream()
                .filter(element -> element.getKind().equals(ElementKind.CLASS))
                .forEach(element -> registerGraphQLApiElement(element, typeUtils));

        try {
            FileObject exportGraphQL = filer.createResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/graphql/export.gql");
            Writer writer = exportGraphQL.openWriter();
            writer.write(documentBuilder.getExportDocument().toString());
            writer.close();

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
                                ObjectType objectType = manager.getQueryOperationTypeName().flatMap(name -> manager.getObject(name))
                                        .map(objectTypeDefinitionContext -> documentBuilder.buildObject(objectTypeDefinitionContext))
                                        .orElseGet(() -> new ObjectType().setName("QueryType"));
                                objectType.addField(graphQLApiBuilder.variableElementToField((ExecutableElement) subElement, typeUtils));
                                manager.registerGraphQL(objectType.toString());
                            } else if (subElement.getAnnotation(Mutation.class) != null && subElement.getKind().equals(ElementKind.METHOD)) {
                                ObjectType objectType = manager.getMutationOperationTypeName().flatMap(name -> manager.getObject(name))
                                        .map(objectTypeDefinitionContext -> documentBuilder.buildObject(objectTypeDefinitionContext))
                                        .orElseGet(() -> new ObjectType().setName("MutationType"));
                                objectType.addField(graphQLApiBuilder.variableElementToField((ExecutableElement) subElement, typeUtils));
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
