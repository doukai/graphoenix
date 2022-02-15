package graphoenix.annotation.processor;

import com.google.auto.service.AutoService;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.core.handler.GraphQLConfigRegister;
import io.graphoenix.core.manager.GraphQLOperationRouter;
import io.graphoenix.graphql.builder.schema.DocumentBuilder;
import io.graphoenix.graphql.generator.translator.JavaElementToOperation;
import io.graphoenix.java.generator.implementer.OperationInterfaceImplementer;
import io.graphoenix.spi.annotation.GraphQLOperation;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.antlr.IGraphQLFieldMapManager;
import io.graphoenix.spi.handler.GeneratorHandler;
import io.vavr.control.Try;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static io.graphoenix.config.ConfigUtil.CONFIG_UTIL;

@SupportedAnnotationTypes("io.graphoenix.spi.annotation.GraphQLOperation")
@AutoService(Processor.class)
public class GraphQLOperationProcessor extends AbstractProcessor {

    private GraphQLOperationRouter operationRouter;
    private GeneratorHandler generatorHandler;
    private JavaElementToOperation javaElementToOperation;
    private OperationInterfaceImplementer operationInterfaceImplementer;
    private Elements elementUtils;
    private Filer filer;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.elementUtils = processingEnv.getElementUtils();
        this.filer = processingEnv.getFiler();
        BeanContext.load(GraphQLOperationProcessor.class.getClassLoader());
        this.operationRouter = BeanContext.get(GraphQLOperationRouter.class);
        this.generatorHandler = BeanContext.get(GeneratorHandler.class);
        this.javaElementToOperation = BeanContext.get(JavaElementToOperation.class);
        this.operationInterfaceImplementer = BeanContext.get(OperationInterfaceImplementer.class);
        IGraphQLDocumentManager manager = BeanContext.get(IGraphQLDocumentManager.class);
        IGraphQLFieldMapManager mapper = BeanContext.get(IGraphQLFieldMapManager.class);
        GraphQLConfigRegister configRegister = BeanContext.get(GraphQLConfigRegister.class);
        DocumentBuilder documentBuilder = BeanContext.get(DocumentBuilder.class);
        GraphQLConfig graphQLConfig = CONFIG_UTIL.scan(filer).getValue(GraphQLConfig.class);
        this.javaElementToOperation.setGraphQLConfig(graphQLConfig);

        try {
            manager.clearAll();
            configRegister.registerPreset(GraphQLOperationProcessor.class.getClassLoader());
            configRegister.registerConfig(graphQLConfig, filer);
            if (graphQLConfig.getBuild()) {
                manager.registerGraphQL(documentBuilder.buildDocument().toString());
            }
            mapper.registerFieldMaps();
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        if (annotations.isEmpty()) {
            return false;
        }

        for (TypeElement annotation : annotations) {
            Set<? extends Element> bundleClasses = roundEnv.getElementsAnnotatedWith(annotation);

            for (Element bundleClassElement : bundleClasses) {
                if (bundleClassElement.getKind().equals(ElementKind.INTERFACE)) {
                    TypeElement typeElement = (TypeElement) bundleClassElement;
                    PackageElement packageElement = elementUtils.getPackageOf(typeElement);

                    AnnotationMirror graphQLOperationMirror = typeElement.getAnnotationMirrors().stream()
                            .filter(annotationMirror -> annotationMirror.getAnnotationType().toString().equals(GraphQLOperation.class.getName()))
                            .findFirst()
                            .orElseThrow();

                    TypeMirror operationDAO = graphQLOperationMirror.getElementValues().entrySet().stream()
                            .filter(entry -> entry.getKey().getSimpleName().toString().equals("operationDAO"))
                            .findFirst()
                            .map(entry -> ((DeclaredType) entry.getValue().getValue()).asElement().asType())
                            .orElseThrow();

                    try {
                        Map<String, String> operationResourcesContent = javaElementToOperation.buildOperationResources(packageElement, typeElement);
                        operationResourcesContent.entrySet().stream()
                                .collect(Collectors.toMap(
                                                Map.Entry::getKey,
                                                entry -> {
                                                    switch (operationRouter.getType(entry.getValue())) {
                                                        case QUERY:
                                                            return generatorHandler.query(entry.getValue());
                                                        case MUTATION:
                                                            return generatorHandler.mutation(entry.getValue());
                                                    }
                                                    return "";
                                                }
                                        )
                                )
                                .forEach((key, value) ->
                                        Try.run(() -> {
                                                    FileObject fileObject = filer.createResource(
                                                            StandardLocation.CLASS_OUTPUT,
                                                            packageElement.getQualifiedName(),
                                                            typeElement.getSimpleName().toString().concat("_").concat(key).concat(".").concat(generatorHandler.extension())
                                                    );
                                                    Writer writer = fileObject.openWriter();
                                                    writer.write(value);
                                                    writer.close();
                                                }
                                        )
                                );

                        operationInterfaceImplementer.writeToFiler(packageElement, typeElement, operationDAO, generatorHandler.extension(), filer);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return false;
    }
}
