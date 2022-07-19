package graphoenix.annotation.processor;

import com.google.auto.service.AutoService;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.core.error.ElementProcessErrorType;
import io.graphoenix.core.error.ElementProcessException;
import io.graphoenix.core.error.GraphQLErrorType;
import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.core.handler.GraphQLConfigRegister;
import io.graphoenix.core.handler.GraphQLOperationRouter;
import io.graphoenix.graphql.builder.schema.DocumentBuilder;
import io.graphoenix.graphql.generator.translator.JavaElementToOperation;
import io.graphoenix.java.generator.implementer.OperationInterfaceImplementer;
import io.graphoenix.spi.annotation.GraphQLOperation;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.antlr.IGraphQLFieldMapManager;
import io.graphoenix.spi.handler.GeneratorHandler;
import io.vavr.control.Try;
import org.tinylog.Logger;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static io.graphoenix.config.ConfigUtil.CONFIG_UTIL;
import static javax.lang.model.SourceVersion.RELEASE_11;

@SupportedAnnotationTypes("io.graphoenix.spi.annotation.GraphQLOperation")
@SupportedSourceVersion(RELEASE_11)
@AutoService(Processor.class)
public class GraphQLOperationProcessor extends AbstractProcessor {

    private GraphQLOperationRouter operationRouter;
    private GeneratorHandler generatorHandler;
    private JavaElementToOperation javaElementToOperation;
    private OperationInterfaceImplementer operationInterfaceImplementer;
    private Elements elementUtils;
    private Types typeUtils;
    private Filer filer;
    private GraphQLConfig graphQLConfig;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.elementUtils = processingEnv.getElementUtils();
        this.typeUtils = processingEnv.getTypeUtils();
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
        graphQLConfig = CONFIG_UTIL.scan(filer).getOptionalValue(GraphQLConfig.class).orElseGet(GraphQLConfig::new);

        try {
            manager.clearAll();
            configRegister.registerPreset(GraphQLOperationProcessor.class.getClassLoader());
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
        for (TypeElement annotation : annotations) {
            Set<? extends Element> bundleClasses = roundEnv.getElementsAnnotatedWith(annotation);

            for (Element bundleClassElement : bundleClasses) {
                if (bundleClassElement.getKind().equals(ElementKind.INTERFACE)) {
                    TypeElement typeElement = (TypeElement) bundleClassElement;
                    PackageElement packageElement = elementUtils.getPackageOf(typeElement);

                    AnnotationMirror graphQLOperationMirror = typeElement.getAnnotationMirrors().stream()
                            .filter(annotationMirror -> annotationMirror.getAnnotationType().toString().equals(GraphQLOperation.class.getName()))
                            .findFirst()
                            .orElseThrow(() -> new ElementProcessException(ElementProcessErrorType.OPERATION_ANNOTATION_NOT_EXIST.bind(typeElement.getQualifiedName().toString())));

                    TypeMirror operationDAO = graphQLOperationMirror.getElementValues().entrySet().stream()
                            .filter(entry -> entry.getKey().getSimpleName().toString().equals("operationDAO"))
                            .findFirst()
                            .map(entry -> ((DeclaredType) entry.getValue().getValue()).asElement().asType())
                            .orElseThrow(() -> new ElementProcessException(ElementProcessErrorType.OPERATION_DAO_VALUE_NOT_EXIST));

                    try {
                        Logger.info("start build operation resource for interface {}", typeElement.getQualifiedName().toString());
                        Map<String, String> operationResourcesContent = javaElementToOperation.buildOperationResources(typeElement, typeUtils);
                        operationResourcesContent.entrySet().stream()
                                .collect(
                                        Collectors.toMap(
                                                Map.Entry::getKey,
                                                entry -> {
                                                    switch (operationRouter.getType(entry.getValue())) {
                                                        case QUERY:
                                                            return generatorHandler.query(entry.getValue());
                                                        case MUTATION:
                                                            return generatorHandler.mutation(entry.getValue());
                                                    }
                                                    throw new GraphQLErrors(GraphQLErrorType.UNSUPPORTED_OPERATION_TYPE);
                                                }
                                        )
                                )
                                .forEach((key, value) ->
                                        Try.run(() -> {
                                                    String resourceName = typeElement.getSimpleName().toString().concat("_").concat(key).concat(".").concat(generatorHandler.extension());
                                                    Logger.info("{} build success", resourceName);
                                                    Logger.debug("resource content:\r\n{}", value);
                                                    FileObject fileObject = filer.createResource(
                                                            StandardLocation.CLASS_OUTPUT,
                                                            packageElement.getQualifiedName(),
                                                            resourceName
                                                    );
                                                    Writer writer = fileObject.openWriter();
                                                    writer.write(value);
                                                    writer.close();
                                                }
                                        )
                                );

                        operationInterfaceImplementer.setGraphQLConfig(graphQLConfig).writeToFiler(packageElement, typeElement, operationDAO, generatorHandler.extension(), filer);

                    } catch (Exception e) {
                        Logger.error(e);
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage());
                    }
                }
            }
        }
        return false;
    }
}
