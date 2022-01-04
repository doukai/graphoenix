package graphoenix.annotation.processor;

import com.google.auto.service.AutoService;
import io.graphoenix.java.generator.config.JavaGeneratorConfig;
import io.graphoenix.spi.annotation.GraphQLOperation;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static io.graphoenix.config.ConfigUtil.RESOURCES_CONFIG_UTIL;

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

//                        GraphQLCodeGenerator generator = DaggerGraphQLCodeGeneratorFactory.create()
//                                .buildGenerator()
//                                .addBootstrapHandlers(bootstrapHandlers)
//                                .addOperationHandlers(pretreatmentHandlers);

                        JavaGeneratorConfig javaGeneratorConfig = RESOURCES_CONFIG_UTIL.getValue(JavaGeneratorConfig.class);
//
//                        if (javaGeneratorConfig.getGraphQL() != null) {
//                            generator.registerGraphQL(javaGeneratorConfig.getGraphQL());
//                        } else if (javaGeneratorConfig.getGraphQLFileName() != null) {
//                            URI graphQLFileUri = new File(RESOURCES_PATH.concat(javaGeneratorConfig.getGraphQLFileName())).toURI();
//                            generator.registerInputStream(graphQLFileUri.toURL().openStream());
//                        } else if (javaGeneratorConfig.getGraphQLPath() != null) {
//                            URI graphQLPathUri = new File(RESOURCES_PATH.concat(javaGeneratorConfig.getGraphQLPath())).toURI();
//                            generator.registerPath(Paths.get(graphQLPathUri));
//                        }

//                        generator.bootstrap();

//                        PackageElement packageElement = elementUtils.getPackageOf(typeElement);
//                        JavaElementToOperation javaElementToOperation = DaggerJavaElementToOperationFactory.create().build();
//                        Map<String, String> operationResourcesContent = javaElementToOperation.buildOperationResources(packageElement, typeElement);
//
//                        ThrowingConsumer<Map.Entry<String, String>, IOException> createResource = (entry) -> {
//                            Filer filer = processingEnv.getFiler();
//                            FileObject fileObject = filer.createResource(
//                                    StandardLocation.SOURCE_OUTPUT,
//                                    packageElement.getQualifiedName(),
//                                    typeElement.getSimpleName().toString()
//                                            .concat("_")
//                                            .concat(entry.getKey())
//                                            .concat(".")
//                                            .concat(suffix)
//                            );
//                            Writer writer = fileObject.openWriter();
//                            writer.write(entry.getValue());
//                            writer.close();
//                        };

//                        operationResourcesContent.entrySet().stream()
//                                .collect(Collectors
//                                        .toMap(Map.Entry::getKey, entry -> generator.tryGenerate(entry.getValue())))
//                                .entrySet()
//                                .forEach(entry -> createResource.asFunction().uncheck().apply(entry));

//                        OperationInterfaceImplementer implementer = new OperationInterfaceImplementer();
//                        implementer.writeToFiler(packageElement, typeElement, executeHandlers, suffix, useInject, processingEnv.getFiler());

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return true;
    }
}
