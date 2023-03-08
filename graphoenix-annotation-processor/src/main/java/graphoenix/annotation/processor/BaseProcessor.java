package graphoenix.annotation.processor;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.core.document.Field;
import io.graphoenix.core.document.ObjectType;
import io.graphoenix.core.error.GraphQLErrorType;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.core.handler.GraphQLConfigRegister;
import io.graphoenix.graphql.builder.schema.DocumentBuilder;
import io.graphoenix.graphql.generator.translator.*;
import io.graphoenix.spi.annotation.Ignore;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.antlr.IGraphQLFieldMapManager;
import io.vavr.Tuple2;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.graphql.*;
import org.eclipse.microprofile.graphql.Enum;
import org.tinylog.Logger;

import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.net.URISyntaxException;

import static io.graphoenix.config.ConfigUtil.CONFIG_UTIL;

@ApplicationScoped
public class BaseProcessor {

    private IGraphQLDocumentManager manager;
    private DocumentBuilder documentBuilder;
    private GraphQLApiBuilder graphQLApiBuilder;
    private JavaElementToEnum javaElementToEnum;
    private JavaElementToObject javaElementToObject;
    private JavaElementToInterface javaElementToInterface;
    private JavaElementToInputType javaElementToInputType;
    private Types typeUtils;

    public void init(ProcessingEnvironment processingEnv) {
        Filer filer = processingEnv.getFiler();
        this.typeUtils = processingEnv.getTypeUtils();
        GraphQLConfig graphQLConfig = CONFIG_UTIL.scan(filer).getOptionalValue(GraphQLConfig.class).orElseGet(GraphQLConfig::new);
        this.manager = BeanContext.get(IGraphQLDocumentManager.class);
        this.documentBuilder = BeanContext.get(DocumentBuilder.class).setGraphQLConfig(graphQLConfig);
        this.graphQLApiBuilder = BeanContext.get(GraphQLApiBuilder.class);
        this.javaElementToEnum = BeanContext.get(JavaElementToEnum.class);
        this.javaElementToObject = BeanContext.get(JavaElementToObject.class);
        this.javaElementToInterface = BeanContext.get(JavaElementToInterface.class);
        this.javaElementToInputType = BeanContext.get(JavaElementToInputType.class);

        try {
            manager.clearAll();
            GraphQLConfigRegister configRegister = BeanContext.get(GraphQLConfigRegister.class);
            configRegister.registerPreset(ApplicationProcessor.class.getClassLoader());
            configRegister.registerConfig(graphQLConfig, filer);
            IGraphQLFieldMapManager mapper = BeanContext.get(IGraphQLFieldMapManager.class);
            mapper.registerFieldMaps();
        } catch (IOException | URISyntaxException e) {
            Logger.error(e);
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage());
        }
    }

    public void registerElements(RoundEnvironment roundEnv) {
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
                .forEach(this::registerGraphQLApiElement);
    }

    private void registerGraphQLApiElement(Element element) {
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
