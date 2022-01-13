package graphoenix.annotation.processor;

import com.google.auto.service.AutoService;
import graphql.parser.antlr.GraphqlParser;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.core.manager.GraphQLConfigRegister;
import io.graphoenix.graphql.builder.schema.DocumentBuilder;
import io.graphoenix.graphql.generator.document.ObjectType;
import io.graphoenix.graphql.generator.translator.GraphQLApiBuilder;
import io.graphoenix.graphql.generator.translator.JavaElementToEnum;
import io.graphoenix.graphql.generator.translator.JavaElementToInputType;
import io.graphoenix.graphql.generator.translator.JavaElementToInterface;
import io.graphoenix.graphql.generator.translator.JavaElementToObject;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import jakarta.annotation.Generated;
import org.eclipse.microprofile.graphql.Enum;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Input;
import org.eclipse.microprofile.graphql.Interface;
import org.eclipse.microprofile.graphql.Mutation;
import org.eclipse.microprofile.graphql.Query;
import org.eclipse.microprofile.graphql.Type;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.net.URISyntaxException;
import java.util.Set;

import static io.graphoenix.config.ConfigUtil.RESOURCES_CONFIG_UTIL;
import static io.graphoenix.spi.constant.Hammurabi.RESOURCES_PATH;

@SupportedAnnotationTypes({
        "org.eclipse.microprofile.graphql.GraphQLApi",
        "org.eclipse.microprofile.graphql.Type",
        "org.eclipse.microprofile.graphql.Enum",
        "org.eclipse.microprofile.graphql.Interface",
        "org.eclipse.microprofile.graphql.Input"
})
@AutoService(Processor.class)
public class GraphQLApiProcessor extends AbstractProcessor {

    private IGraphQLDocumentManager manager;
    private DocumentBuilder documentBuilder;
    private GraphQLConfigRegister configRegister;
    private JavaElementToEnum javaElementToEnum;
    private JavaElementToObject javaElementToObject;
    private JavaElementToInterface javaElementToInterface;
    private JavaElementToInputType javaElementToInputType;
    private GraphQLApiBuilder graphQLApiBuilder;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        BeanContext.load(GraphQLOperationProcessor.class.getClassLoader());
        this.manager = BeanContext.get(IGraphQLDocumentManager.class);
        this.documentBuilder = BeanContext.get(DocumentBuilder.class);
        this.configRegister = BeanContext.get(GraphQLConfigRegister.class);
        this.javaElementToEnum = BeanContext.get(JavaElementToEnum.class);
        this.javaElementToObject = BeanContext.get(JavaElementToObject.class);
        this.javaElementToInterface = BeanContext.get(JavaElementToInterface.class);
        this.javaElementToInputType = BeanContext.get(JavaElementToInputType.class);
        this.graphQLApiBuilder = BeanContext.get(GraphQLApiBuilder.class);

        GraphQLConfig graphQLConfig = RESOURCES_CONFIG_UTIL.getValue(GraphQLConfig.class);
        try {
            configRegister.registerConfig(graphQLConfig, RESOURCES_PATH);
            if (graphQLConfig.getBuild()) {
                manager.registerGraphQL(documentBuilder.buildDocument().toString());
            }
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        if (annotations.isEmpty()) {
            return true;
        }

        final Types typeUtils = processingEnv.getTypeUtils();
        final Filer filer = processingEnv.getFiler();

        roundEnv.getElementsAnnotatedWith(Enum.class).stream()
                .filter(element -> element.getAnnotation(Generated.class) == null)
                .filter(element -> element.getKind().equals(ElementKind.ENUM))
                .forEach(element -> manager.registerGraphQL(javaElementToEnum.buildEnum((TypeElement) element).toString()));

        roundEnv.getElementsAnnotatedWith(Interface.class).stream()
                .filter(element -> element.getAnnotation(Generated.class) == null)
                .filter(element -> element.getKind().equals(ElementKind.INTERFACE))
                .forEach(element -> {
                            manager.registerGraphQL(javaElementToInterface.buildInterface((TypeElement) element, typeUtils).toString());
                            element.getEnclosedElements().stream()
                                    .filter(subElement -> subElement.getAnnotation(Generated.class) == null)
                                    .filter(subElement -> subElement.getAnnotation(Enum.class) != null)
                                    .filter(subElement -> subElement.getKind().equals(ElementKind.ENUM))
                                    .forEach(subElement -> manager.registerGraphQL(javaElementToEnum.buildEnum((TypeElement) subElement).toString()));
                        }
                );

        roundEnv.getElementsAnnotatedWith(Type.class).stream()
                .filter(element -> element.getAnnotation(Generated.class) == null)
                .filter(element -> element.getKind().equals(ElementKind.CLASS))
                .forEach(element -> {
                            manager.registerGraphQL(javaElementToObject.buildObject((TypeElement) element, typeUtils).toString());
                            element.getEnclosedElements().stream()
                                    .filter(subElement -> subElement.getAnnotation(Generated.class) == null)
                                    .filter(subElement -> subElement.getAnnotation(Enum.class) != null)
                                    .filter(subElement -> subElement.getKind().equals(ElementKind.ENUM))
                                    .forEach(subElement -> manager.registerGraphQL(javaElementToEnum.buildEnum((TypeElement) subElement).toString()));
                        }
                );

        roundEnv.getElementsAnnotatedWith(Input.class).stream()
                .filter(element -> element.getAnnotation(Generated.class) == null)
                .filter(element -> element.getKind().equals(ElementKind.CLASS))
                .forEach(element -> {
                            manager.registerGraphQL(javaElementToInputType.buildInputType((TypeElement) element, typeUtils).toString());
                            element.getEnclosedElements().stream()
                                    .filter(subElement -> subElement.getAnnotation(Generated.class) == null)
                                    .filter(subElement -> subElement.getAnnotation(Enum.class) != null)
                                    .filter(subElement -> subElement.getKind().equals(ElementKind.ENUM))
                                    .forEach(subElement -> manager.registerGraphQL(javaElementToEnum.buildEnum((TypeElement) subElement).toString()));
                        }
                );

        roundEnv.getElementsAnnotatedWith(GraphQLApi.class).stream()
                .filter(element -> element.getKind().equals(ElementKind.CLASS))
                .forEach(element -> registerGraphQLApiElement(element, typeUtils));

        try {
            FileObject fileObject = filer.createResource(StandardLocation.SOURCE_OUTPUT, "", "all.gql");
            Writer writer = fileObject.openWriter();
            writer.write(documentBuilder.getDocument().toString());
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    private void registerGraphQLApiElement(Element element, Types typeUtils) {
        element.getEnclosedElements().forEach(
                subElement -> {
                    if (subElement.getAnnotation(Query.class) != null && subElement.getKind().equals(ElementKind.METHOD)) {
                        GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext = manager.getQueryOperationTypeName().flatMap(name -> manager.getObject(name)).orElseThrow();
                        ObjectType objectType = documentBuilder.getObject(objectTypeDefinitionContext).addField(graphQLApiBuilder.variableElementToField((ExecutableElement) subElement, typeUtils));
                        manager.registerGraphQL(objectType.toString());
                    } else if (subElement.getAnnotation(Mutation.class) != null && subElement.getKind().equals(ElementKind.METHOD)) {
                        GraphqlParser.ObjectTypeDefinitionContext objectTypeDefinitionContext = manager.getMutationOperationTypeName().flatMap(name -> manager.getObject(name)).orElseThrow();
                        ObjectType objectType = documentBuilder.getObject(objectTypeDefinitionContext).addField(graphQLApiBuilder.variableElementToField((ExecutableElement) subElement, typeUtils));
                        manager.registerGraphQL(objectType.toString());
                    }
                }
        );
    }
}
