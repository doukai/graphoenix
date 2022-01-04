package graphoenix.annotation.processor;

import com.google.auto.service.AutoService;
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.graphql.generator.translator.JavaElementToOperation;
import io.graphoenix.java.generator.config.JavaGeneratorConfig;
import io.graphoenix.java.generator.implementer.OperationInterfaceImplementer;
import io.graphoenix.spi.annotation.GraphQLOperation;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.handler.BootstrapHandler;
import io.graphoenix.spi.handler.GeneratorHandler;
import io.vavr.control.Try;

import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.lang.model.util.Elements;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.File;
import java.io.Writer;
import java.net.URI;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static io.graphoenix.config.ConfigUtil.RESOURCES_CONFIG_UTIL;
import static io.graphoenix.spi.constant.Hammurabi.RESOURCES_PATH;

@SupportedAnnotationTypes("io.graphoenix.spi.annotation.GraphQLOperation")
@AutoService(Processor.class)
public class OperationAnnotationProcessor extends AbstractProcessor {

    private IGraphQLDocumentManager manager;

    private BootstrapHandler bootstrapHandler;

    private GeneratorHandler generatorHandler;

    private JavaElementToOperation javaElementToOperation;

    private OperationInterfaceImplementer operationInterfaceImplementer;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.manager = BeanContext.get(IGraphQLDocumentManager.class);
        this.bootstrapHandler = BeanContext.get(BootstrapHandler.class);
        this.generatorHandler = BeanContext.get(GeneratorHandler.class);
        this.javaElementToOperation = BeanContext.get(JavaElementToOperation.class);
        this.operationInterfaceImplementer = BeanContext.get(OperationInterfaceImplementer.class);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            Set<? extends Element> bundleClasses = roundEnv.getElementsAnnotatedWith(annotation);
            for (Element bundleClassElement : bundleClasses) {
                if (bundleClassElement.getKind().equals(ElementKind.INTERFACE)) {

                    final Elements elementUtils = this.processingEnv.getElementUtils();

                    TypeElement typeElement = (TypeElement) bundleClassElement;
                    try {
                        String graphQLOperationClassName = GraphQLOperation.class.getName();
                        AnnotationMirror graphQLOperationMirror = typeElement.getAnnotationMirrors().stream()
                                .filter(annotationMirror -> annotationMirror.getAnnotationType().toString().equals(graphQLOperationClassName))
                                .findFirst()
                                .orElseThrow();

                        Map<? extends ExecutableElement, ? extends AnnotationValue> elementValuesWithDefaults = elementUtils.getElementValuesWithDefaults(graphQLOperationMirror);

                        String executeHandler = elementValuesWithDefaults.entrySet().stream()
                                .filter(entry -> entry.getKey().getSimpleName().toString().equals("executeHandler"))
                                .filter(entry -> entry.getValue().getValue() instanceof List<?>)
                                .findFirst()
                                .map(entry -> (String) entry.getValue().getValue()
                                )
                                .orElseThrow();

                        boolean useInject = elementValuesWithDefaults.entrySet().stream()
                                .filter(entry -> entry.getKey().getSimpleName().toString().equals("useInject"))
                                .findFirst()
                                .map(entry -> (boolean) entry.getValue().getValue())
                                .orElseThrow();

                        JavaGeneratorConfig javaGeneratorConfig = RESOURCES_CONFIG_UTIL.getValue(JavaGeneratorConfig.class);

                        if (javaGeneratorConfig.getGraphQL() != null) {
                            manager.registerGraphQL(javaGeneratorConfig.getGraphQL());
                        } else if (javaGeneratorConfig.getGraphQLFileName() != null) {
                            URI graphQLFileUri = new File(RESOURCES_PATH.concat(javaGeneratorConfig.getGraphQLFileName())).toURI();
                            manager.registerInputStream(graphQLFileUri.toURL().openStream());
                        } else if (javaGeneratorConfig.getGraphQLPath() != null) {
                            URI graphQLPathUri = new File(RESOURCES_PATH.concat(javaGeneratorConfig.getGraphQLPath())).toURI();
                            manager.registerPath(Paths.get(graphQLPathUri));
                        }

                        bootstrapHandler.bootstrap();

                        PackageElement packageElement = elementUtils.getPackageOf(typeElement);
                        Map<String, String> operationResourcesContent = javaElementToOperation.buildOperationResources(packageElement, typeElement);

                        operationResourcesContent.entrySet().stream()
                                .collect(Collectors.toMap(Map.Entry::getKey, entry -> generatorHandler.query(entry.getValue())))
                                .forEach((key, value) -> Try.run(() -> {
                                            Filer filer = processingEnv.getFiler();
                                            FileObject fileObject = filer.createResource(
                                                    StandardLocation.SOURCE_OUTPUT,
                                                    packageElement.getQualifiedName(),
                                                    typeElement.getSimpleName().toString()
                                                            .concat("_")
                                                            .concat(key)
                                                            .concat(".")
                                                            .concat(generatorHandler.extension())
                                            );
                                            Writer writer = fileObject.openWriter();
                                            writer.write(value);
                                            writer.close();
                                        }
                                ));

                        operationInterfaceImplementer.writeToFiler(packageElement, typeElement, executeHandler, generatorHandler.extension(), useInject, processingEnv.getFiler());

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return true;
    }
}
