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
import io.graphoenix.graphql.generator.translator.GraphQLApiBuilder;
import io.graphoenix.graphql.generator.translator.JavaElementToEnum;
import io.graphoenix.graphql.generator.translator.JavaElementToInputType;
import io.graphoenix.graphql.generator.translator.JavaElementToInterface;
import io.graphoenix.graphql.generator.translator.JavaElementToObject;
import io.graphoenix.spi.annotation.Ignore;
import io.graphoenix.spi.annotation.Package;
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
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Optional;

import static io.graphoenix.config.ConfigUtil.CONFIG_UTIL;
import static io.graphoenix.spi.constant.Hammurabi.MUTATION_TYPE_NAME;
import static io.graphoenix.spi.constant.Hammurabi.QUERY_TYPE_NAME;

public abstract class BaseProcessor extends AbstractProcessor {

    private GraphQLConfig graphQLConfig;
    private IGraphQLDocumentManager manager;
    private DocumentBuilder documentBuilder;
    private GraphQLApiBuilder graphQLApiBuilder;
    private JavaElementToEnum javaElementToEnum;
    private JavaElementToObject javaElementToObject;
    private JavaElementToInterface javaElementToInterface;
    private JavaElementToInputType javaElementToInputType;
    private Types typeUtils;

    @Override
    public void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        typeUtils = processingEnv.getTypeUtils();
        Filer filer = processingEnv.getFiler();
        CONFIG_UTIL.load(filer);
        BeanContext.load(BaseProcessor.class.getClassLoader());
        manager = BeanContext.get(IGraphQLDocumentManager.class);
        graphQLConfig = BeanContext.get(GraphQLConfig.class);
        documentBuilder = BeanContext.get(DocumentBuilder.class);
        graphQLApiBuilder = BeanContext.get(GraphQLApiBuilder.class);
        javaElementToEnum = BeanContext.get(JavaElementToEnum.class);
        javaElementToObject = BeanContext.get(JavaElementToObject.class);
        javaElementToInterface = BeanContext.get(JavaElementToInterface.class);
        javaElementToInputType = BeanContext.get(JavaElementToInputType.class);
        GraphQLConfigRegister configRegister = BeanContext.get(GraphQLConfigRegister.class);
        IGraphQLFieldMapManager mapper = BeanContext.get(IGraphQLFieldMapManager.class);

        try {
            manager.clearAll();
            configRegister.registerPreset(ApplicationProcessor.class.getClassLoader());
            configRegister.registerConfig(graphQLConfig, filer);
            mapper.registerFieldMaps();
        } catch (IOException | URISyntaxException e) {
            Logger.error(e);
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage());
        }
    }

    public Optional<String> getDefaultPackageName(RoundEnvironment roundEnv) {
        return roundEnv.getElementsAnnotatedWith(Package.class).stream()
                .filter(element -> element.getKind().equals(ElementKind.PACKAGE))
                .findFirst()
                .map(element -> (PackageElement) element)
                .map(packageElement -> packageElement.getQualifiedName().toString());
    }

    public void registerElements(RoundEnvironment roundEnv) {
        if (graphQLConfig.getPackageName() == null) {
            getDefaultPackageName(roundEnv).ifPresent(packageName -> graphQLConfig.setPackageName(packageName));
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
                .forEach(this::registerGraphQLApiElement);
    }

    private void registerGraphQLApiElement(Element element) {
        element.getEnclosedElements()
                .forEach(subElement -> {
                            if (subElement.getAnnotation(Query.class) != null && subElement.getKind().equals(ElementKind.METHOD)) {
                                ObjectType objectType = manager.getQueryOperationTypeName().flatMap(name -> manager.getObject(name))
                                        .map(objectTypeDefinitionContext -> documentBuilder.buildObject(objectTypeDefinitionContext))
                                        .orElseGet(() -> new ObjectType().setName(QUERY_TYPE_NAME));
                                objectType.addField(graphQLApiBuilder.variableElementToField((ExecutableElement) subElement, typeUtils));
                                manager.registerGraphQL(objectType.toString());
                            } else if (subElement.getAnnotation(Mutation.class) != null && subElement.getKind().equals(ElementKind.METHOD)) {
                                ObjectType objectType = manager.getMutationOperationTypeName().flatMap(name -> manager.getObject(name))
                                        .map(objectTypeDefinitionContext -> documentBuilder.buildObject(objectTypeDefinitionContext))
                                        .orElseGet(() -> new ObjectType().setName(MUTATION_TYPE_NAME));
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