package graphoenix.annotation.processor;

import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.core.document.Field;
import io.graphoenix.core.document.InputObjectType;
import io.graphoenix.core.document.ObjectType;
import io.graphoenix.core.error.GraphQLErrorType;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.core.handler.GraphQLConfigRegister;
import io.graphoenix.core.operation.ArrayValueWithVariable;
import io.graphoenix.core.operation.Directive;
import io.graphoenix.core.operation.ObjectValueWithVariable;
import io.graphoenix.graphql.builder.schema.DocumentBuilder;
import io.graphoenix.graphql.generator.translator.GraphQLApiBuilder;
import io.graphoenix.graphql.generator.translator.JavaElementToEnum;
import io.graphoenix.graphql.generator.translator.JavaElementToInputType;
import io.graphoenix.graphql.generator.translator.JavaElementToInterface;
import io.graphoenix.graphql.generator.translator.JavaElementToObject;
import io.graphoenix.graphql.generator.translator.JavaElementToOperation;
import io.graphoenix.spi.annotation.GraphQLOperation;
import io.graphoenix.spi.annotation.Ignore;
import io.graphoenix.spi.annotation.Package;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.vavr.Tuple2;
import jakarta.json.JsonValue;
import jakarta.json.spi.JsonProvider;
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
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.graphoenix.config.ConfigUtil.CONFIG_UTIL;
import static io.graphoenix.core.utils.ElementUtil.ELEMENT_UTIL;
import static io.graphoenix.spi.constant.Hammurabi.INVOKES_DIRECTIVE_NAME;
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
    private JavaElementToOperation javaElementToOperation;
    private JsonProvider jsonProvider;
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
        javaElementToOperation = BeanContext.get(JavaElementToOperation.class);
        jsonProvider = BeanContext.get(JsonProvider.class);
        GraphQLConfigRegister configRegister = BeanContext.get(GraphQLConfigRegister.class);

        try {
            manager.clearAll();
            configRegister.registerConfig(filer);
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
                .forEach(element -> manager.mergeDocument(javaElementToEnum.buildEnum((TypeElement) element).toString()));

        roundEnv.getElementsAnnotatedWith(Interface.class).stream()
                .filter(element -> element.getAnnotation(Ignore.class) == null)
                .filter(element -> element.getKind().equals(ElementKind.INTERFACE))
                .forEach(element -> {
                            manager.mergeDocument(javaElementToInterface.buildInterface((TypeElement) element, typeUtils).toString());
                            element.getEnclosedElements().stream()
                                    .filter(subElement -> subElement.getAnnotation(Ignore.class) == null)
                                    .filter(subElement -> subElement.getAnnotation(Enum.class) != null)
                                    .filter(subElement -> subElement.getKind().equals(ElementKind.ENUM))
                                    .forEach(subElement -> manager.mergeDocument(javaElementToEnum.buildEnum((TypeElement) subElement).toString()));
                        }
                );

        roundEnv.getElementsAnnotatedWith(Type.class).stream()
                .filter(element -> element.getAnnotation(Ignore.class) == null)
                .filter(element -> element.getKind().equals(ElementKind.CLASS))
                .forEach(element -> {
                            manager.mergeDocument(javaElementToObject.buildObject((TypeElement) element, typeUtils).toString());
                            element.getEnclosedElements().stream()
                                    .filter(subElement -> subElement.getAnnotation(Ignore.class) == null)
                                    .filter(subElement -> subElement.getAnnotation(Enum.class) != null)
                                    .filter(subElement -> subElement.getKind().equals(ElementKind.ENUM))
                                    .forEach(subElement -> manager.mergeDocument(javaElementToEnum.buildEnum((TypeElement) subElement).toString()));
                        }
                );

        roundEnv.getElementsAnnotatedWith(Input.class).stream()
                .filter(element -> element.getAnnotation(Ignore.class) == null)
                .filter(element -> element.getKind().equals(ElementKind.CLASS))
                .forEach(element -> {
                            manager.mergeDocument(javaElementToInputType.buildInputType((TypeElement) element, typeUtils).toString());
                            element.getEnclosedElements().stream()
                                    .filter(subElement -> subElement.getAnnotation(Ignore.class) == null)
                                    .filter(subElement -> subElement.getAnnotation(Enum.class) != null)
                                    .filter(subElement -> subElement.getKind().equals(ElementKind.ENUM))
                                    .forEach(subElement -> manager.mergeDocument(javaElementToEnum.buildEnum((TypeElement) subElement).toString()));
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
                                ObjectType objectType = manager.getObject(manager.getQueryOperationTypeName().orElse(QUERY_TYPE_NAME))
                                        .map(objectTypeDefinitionContext -> documentBuilder.buildObject(objectTypeDefinitionContext))
                                        .orElseGet(() -> new ObjectType(QUERY_TYPE_NAME));
                                objectType.addField(graphQLApiBuilder.variableElementToField((ExecutableElement) subElement, typeUtils));
                                manager.mergeDocument(objectType.toString());
                            } else if (subElement.getAnnotation(Mutation.class) != null && subElement.getKind().equals(ElementKind.METHOD)) {
                                ObjectType objectType = manager.getObject(manager.getMutationOperationTypeName().orElse(MUTATION_TYPE_NAME))
                                        .map(objectTypeDefinitionContext -> documentBuilder.buildObject(objectTypeDefinitionContext))
                                        .orElseGet(() -> new ObjectType(MUTATION_TYPE_NAME));
                                objectType.addField(graphQLApiBuilder.variableElementToField((ExecutableElement) subElement, typeUtils));
                                manager.mergeDocument(objectType.toString());
                            } else if (subElement.getKind().equals(ElementKind.METHOD) &&
                                    ((ExecutableElement) subElement).getParameters().stream()
                                            .anyMatch(variableElement ->
                                                    variableElement.getAnnotation(Source.class) != null &&
                                                            typeUtils.asElement(variableElement.asType()).getAnnotation(Type.class) != null
                                            )
                            ) {
                                Tuple2<String, Field> objectField = graphQLApiBuilder.variableElementToObjectField((ExecutableElement) subElement, typeUtils);
                                GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext = manager.getObject(objectField._1()).orElseThrow(() -> new GraphQLErrors(GraphQLErrorType.TYPE_NOT_EXIST.bind(objectField._1())));
                                ObjectType objectType = documentBuilder.buildObject(objectTypeDefinitionContext).addField(objectField._2());
                                manager.mergeDocument(objectType.toString());
                            } else if (subElement.getKind().equals(ElementKind.METHOD) &&
                                    ((ExecutableElement) subElement).getParameters().stream()
                                            .anyMatch(variableElement ->
                                                    variableElement.getAnnotation(Source.class) != null &&
                                                            typeUtils.asElement(variableElement.asType()).getAnnotation(Interface.class) != null
                                            )
                            ) {
                                Tuple2<String, Field> objectField = graphQLApiBuilder.variableElementToObjectField((ExecutableElement) subElement, typeUtils);
                                manager.getImplementsObjectType(objectField._1())
                                        .forEach(objectTypeDefinitionContext -> {
                                                    ObjectType objectType = documentBuilder.buildObject(objectTypeDefinitionContext).addField(objectField._2());
                                                    manager.mergeDocument(objectType.toString());
                                                }
                                        );
                            } else if (subElement.getKind().equals(ElementKind.METHOD) &&
                                    ((ExecutableElement) subElement).getParameters().stream()
                                            .anyMatch(variableElement ->
                                                    variableElement.getAnnotation(Source.class) != null &&
                                                            typeUtils.asElement(variableElement.asType()).getAnnotation(Input.class) != null
                                            )
                            ) {
                                ExecutableElement executableElement = (ExecutableElement) subElement;
                                executableElement.getParameters().stream()
                                        .filter(variableElement ->
                                                variableElement.getAnnotation(Source.class) != null &&
                                                        typeUtils.asElement(variableElement.asType()).getAnnotation(Input.class) != null
                                        )
                                        .map(variableElement -> typeUtils.asElement(variableElement.asType()).getSimpleName().toString())
                                        .map(inputName ->
                                                manager.getInputObject(inputName)
                                                        .map(inputObjectTypeDefinitionContext ->
                                                                documentBuilder.buildInputObjectType(inputObjectTypeDefinitionContext))
                                                        .orElseGet(() -> new InputObjectType(inputName))
                                        )
                                        .findFirst()
                                        .ifPresent(inputObjectType -> {
                                                    Map<String, Object> invoke = Map.of(
                                                            "className", executableElement.getEnclosingElement().toString(),
                                                            "methodName", executableElement.getSimpleName().toString(),
                                                            "parameters",
                                                            new ArrayValueWithVariable(
                                                                    executableElement.getParameters().stream()
                                                                            .map(parameter -> Map.of("name", parameter.getSimpleName().toString(), "className", ELEMENT_UTIL.getTypeMirrorName(parameter.asType(), typeUtils)))
                                                                            .collect(Collectors.toList())
                                                            ),
                                                            "returnClassName", executableElement.getReturnType().toString()
                                                    );
                                                    Optional<Directive> invokes = Stream.ofNullable(inputObjectType.getDirectives())
                                                            .flatMap(Collection::stream)
                                                            .filter(directive -> directive.getName().equals(INVOKES_DIRECTIVE_NAME))
                                                            .findFirst();
                                                    if (invokes.isPresent() && invokes.get().getArguments().get("list") != null && invokes.get().getArguments().get("list").getValueType().equals(JsonValue.ValueType.ARRAY)) {
                                                        invokes.get().getArguments().put("list", jsonProvider.createArrayBuilder(invokes.get().getArguments().get("list").asJsonArray()).add(new ObjectValueWithVariable(invoke)).build());
                                                    } else {
                                                        inputObjectType.addDirective(
                                                                new Directive()
                                                                        .setName(INVOKES_DIRECTIVE_NAME)
                                                                        .addArgument("list",
                                                                                new ArrayValueWithVariable(
                                                                                        Collections.singleton(invoke)
                                                                                )
                                                                        )
                                                        );
                                                    }
                                                    manager.registerGraphQL(inputObjectType.toString());
                                                }
                                        );
                            }
                        }
                );
    }

    public void registerOperations(RoundEnvironment roundEnv) {
        roundEnv.getElementsAnnotatedWith(GraphQLOperation.class).stream()
                .filter(element -> element.getKind().equals(ElementKind.INTERFACE))
                .forEach(this::registerGraphQLOperationElement);
    }

    private void registerGraphQLOperationElement(Element element) {
        javaElementToOperation.buildOperations((TypeElement) element, typeUtils)
                .forEach(operation -> manager.registerGraphQL(operation));
    }
}
