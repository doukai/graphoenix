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
import io.graphoenix.spi.handler.GeneratorHandler;
import org.tinylog.Logger;

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
import java.io.Writer;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static javax.lang.model.SourceVersion.RELEASE_11;

@SupportedAnnotationTypes("io.graphoenix.spi.annotation.GraphQLOperation")
@SupportedSourceVersion(RELEASE_11)
@AutoService(Processor.class)
public class GraphQLOperationProcessor extends BaseProcessor {

    private Elements elementUtils;
    private Types typeUtils;
    private Filer filer;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        elementUtils = processingEnv.getElementUtils();
        typeUtils = processingEnv.getTypeUtils();
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
        GraphQLOperationRouter operationRouter = BeanContext.get(GraphQLOperationRouter.class);
        GeneratorHandler generatorHandler = BeanContext.get(GeneratorHandler.class);
        JavaElementToOperation javaElementToOperation = BeanContext.get(JavaElementToOperation.class);
        OperationInterfaceImplementer operationInterfaceImplementer = BeanContext.get(OperationInterfaceImplementer.class);
        try {
            GraphQLConfigRegister configRegister = BeanContext.get(GraphQLConfigRegister.class);
            configRegister.registerPreset(GraphQLOperationProcessor.class.getClassLoader());
            if (graphQLConfig.getBuild()) {
                manager.registerGraphQL(documentBuilder.buildDocument().toString());
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

                        Logger.info("start build operation resource for interface {}", typeElement.getQualifiedName().toString());
                        Map<String, String> operationResourcesContent = javaElementToOperation.buildOperationResources(typeElement, typeUtils)
                                .entrySet().stream()
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
                                );

                        for (Map.Entry<String, String> resource : operationResourcesContent.entrySet()) {
                            String key = resource.getKey();
                            String value = resource.getValue();
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
                        operationInterfaceImplementer.writeToFiler(packageElement, typeElement, operationDAO, generatorHandler.extension(), filer);
                    }
                }
            }
        } catch (Exception e) {
            Logger.error(e);
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage());
        }
        return false;
    }
}
