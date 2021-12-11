package graphoenix.annotation.processor;

import com.google.auto.service.AutoService;
import com.pivovarit.function.ThrowingBiConsumer;
import com.pivovarit.function.ThrowingBiFunction;
import io.graphoenix.common.pipeline.DaggerGraphQLCodeGeneratorFactory;
import io.graphoenix.common.pipeline.GraphQLCodeGenerator;
import io.graphoenix.graphql.generator.translator.DaggerJavaElementToOperationFactory;
import io.graphoenix.graphql.generator.translator.JavaElementToOperation;
import io.graphoenix.spi.annotation.GraphQLOperation;
import io.graphoenix.spi.config.JavaGeneratorConfig;
import io.graphoenix.java.generator.implementer.OperationInterfaceImplementer;

import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.lang.model.util.Elements;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static io.graphoenix.config.CompileConfig.COMPILE_CONFIG;

@SupportedAnnotationTypes("io.graphoenix.spi.annotation.GraphQLOperation")
@AutoService(Processor.class)
public class OperationAnnotationProcessor extends AbstractProcessor {

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
                        Set<String> bootstrapHandlers = elementValuesWithDefaults.entrySet().stream()
                                .filter(entry -> entry.getKey().getSimpleName().toString().equals("bootstrapHandlers"))
                                .filter(entry -> entry.getValue().getValue() instanceof List<?>)
                                .findFirst()
                                .map(entry -> ((List<?>) entry.getValue().getValue()).stream()
                                        .map(item -> ((AnnotationValue) item).getValue().toString())
                                        .collect(Collectors.toSet())
                                )
                                .orElseThrow();
                        Set<String> pretreatmentHandlers = elementValuesWithDefaults.entrySet().stream()
                                .filter(entry -> entry.getKey().getSimpleName().toString().equals("pretreatmentHandlers"))
                                .filter(entry -> entry.getValue().getValue() instanceof List<?>)
                                .findFirst()
                                .map(entry -> ((List<?>) entry.getValue().getValue()).stream()
                                        .map(item -> ((AnnotationValue) item).getValue().toString())
                                        .collect(Collectors.toSet())
                                )
                                .orElseThrow();
                        Set<String> executeHandlers = elementValuesWithDefaults.entrySet().stream()
                                .filter(entry -> entry.getKey().getSimpleName().toString().equals("executeHandlers"))
                                .filter(entry -> entry.getValue().getValue() instanceof List<?>)
                                .findFirst()
                                .map(entry -> ((List<?>) entry.getValue().getValue()).stream()
                                        .map(item -> ((AnnotationValue) item).getValue().toString())
                                        .collect(Collectors.toSet())
                                )
                                .orElseThrow();

                        String suffix = elementValuesWithDefaults.entrySet().stream()
                                .filter(entry -> entry.getKey().getSimpleName().toString().equals("suffix"))
                                .findFirst()
                                .map(entry -> (String) entry.getValue().getValue())
                                .orElseThrow();

                        boolean useInject = elementValuesWithDefaults.entrySet().stream()
                                .filter(entry -> entry.getKey().getSimpleName().toString().equals("useInject"))
                                .findFirst()
                                .map(entry -> (boolean) entry.getValue().getValue())
                                .orElseThrow();

                        GraphQLCodeGenerator generator = DaggerGraphQLCodeGeneratorFactory.create()
                                .buildGenerator()
                                .addBootstrapHandlers(bootstrapHandlers)
                                .addOperationHandlers(pretreatmentHandlers);

                        String resourcesPath = System.getProperty("user.dir").concat(File.separator).concat("src").concat(File.separator).concat("main").concat(File.separator).concat("resources").concat(File.separator);
                        JavaGeneratorConfig javaGeneratorConfig = COMPILE_CONFIG.getValue("generator", JavaGeneratorConfig.class);

                        if (javaGeneratorConfig.getGraphQL() != null) {
                            generator.registerDocument(javaGeneratorConfig.getGraphQL());
                        } else if (javaGeneratorConfig.getGraphQLFileName() != null) {
                            URI graphQLFileUri = new File(resourcesPath.concat(javaGeneratorConfig.getGraphQLFileName())).toURI();
                            generator.registerDocument(graphQLFileUri.toURL().openStream());
                        } else if (javaGeneratorConfig.getGraphQLPath() != null) {
                            URI graphQLPathUri = new File(resourcesPath.concat(javaGeneratorConfig.getGraphQLPath())).toURI();
                            generator.registerPath(Paths.get(graphQLPathUri));
                        }

                        generator.bootstrap();

                        PackageElement packageElement = elementUtils.getPackageOf(typeElement);
                        JavaElementToOperation javaElementToOperation = DaggerJavaElementToOperationFactory.create().build();
                        Map<String, String> operationResourcesContent = javaElementToOperation.buildOperationResources(packageElement, typeElement);

                        ThrowingBiFunction<GraphQLCodeGenerator, String, String, Exception> generatorPretreatment = GraphQLCodeGenerator::generate;
                        ThrowingBiConsumer<Filer, Map.Entry<String, String>, IOException> createResource = (filer, entry) -> {
                            FileObject fileObject = filer.createResource(
                                    StandardLocation.SOURCE_OUTPUT,
                                    packageElement.getQualifiedName(),
                                    typeElement.getSimpleName().toString()
                                            .concat("_")
                                            .concat(entry.getKey())
                                            .concat(".")
                                            .concat(suffix)
                            );
                            Writer writer = fileObject.openWriter();
                            writer.write(entry.getValue());
                            writer.close();
                        };

                        operationResourcesContent.entrySet().stream()
                                .collect(Collectors
                                        .toMap(Map.Entry::getKey, entry -> generatorPretreatment.unchecked().apply(generator, entry.getValue())))
                                .entrySet()
                                .forEach(entry -> createResource.asFunction().unchecked().apply(processingEnv.getFiler(), entry));

                        OperationInterfaceImplementer implementer = new OperationInterfaceImplementer();
                        implementer.writeToFiler(packageElement, typeElement, executeHandlers, suffix, useInject, processingEnv.getFiler());

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return true;
    }
}
