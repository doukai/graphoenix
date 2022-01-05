package graphoenix.annotation.processor;

import com.google.auto.service.AutoService;
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.core.manager.GraphQLOperationRouter;
import io.graphoenix.graphql.builder.schema.DocumentBuilder;
import io.graphoenix.graphql.generator.translator.JavaElementToOperation;
import io.graphoenix.java.generator.config.JavaGeneratorConfig;
import io.graphoenix.java.generator.implementer.OperationInterfaceImplementer;
import io.graphoenix.spi.annotation.GraphQLOperation;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.antlr.IGraphQLFieldMapManager;
import io.graphoenix.spi.handler.GeneratorHandler;
import io.vavr.control.Try;
import org.apache.logging.log4j.util.Strings;
import org.openjdk.tools.javac.code.Type;

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
import java.io.Writer;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static io.graphoenix.config.ConfigUtil.RESOURCES_CONFIG_UTIL;
import static io.graphoenix.spi.constant.Hammurabi.RESOURCES_PATH;

@SupportedAnnotationTypes("io.graphoenix.spi.annotation.GraphQLOperation")
@AutoService(Processor.class)
public class OperationAnnotationProcessor extends AbstractProcessor {

    private IGraphQLDocumentManager manager;

    private IGraphQLFieldMapManager mapper;

    private GraphQLOperationRouter operationRouter;

    private DocumentBuilder documentBuilder;

    private GeneratorHandler generatorHandler;

    private JavaElementToOperation javaElementToOperation;

    private OperationInterfaceImplementer operationInterfaceImplementer;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        BeanContext.load(OperationAnnotationProcessor.class.getClassLoader());
        this.manager = BeanContext.get(IGraphQLDocumentManager.class);
        this.mapper = BeanContext.get(IGraphQLFieldMapManager.class);
        this.operationRouter = BeanContext.get(GraphQLOperationRouter.class);
        this.documentBuilder = BeanContext.get(DocumentBuilder.class);
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

                        TypeMirror operationDAO = graphQLOperationMirror.getElementValues().entrySet().stream()
                                .filter(entry -> entry.getKey().getSimpleName().toString().equals("operationDAO"))
                                .findFirst()
                                .map(entry -> ((DeclaredType) entry.getValue().getValue()).asElement().asType())
                                .orElseThrow();

                        boolean useInject = graphQLOperationMirror.getElementValues().entrySet().stream()
                                .filter(entry -> entry.getKey().getSimpleName().toString().equals("useInject"))
                                .findFirst()
                                .map(entry -> (boolean) entry.getValue().getValue())
                                .orElse((boolean) GraphQLOperation.class.getMethod("useInject").getDefaultValue());

                        JavaGeneratorConfig javaGeneratorConfig = RESOURCES_CONFIG_UTIL.getValue(JavaGeneratorConfig.class);

                        if (javaGeneratorConfig.getGraphQL() != null) {
                            manager.registerGraphQL(javaGeneratorConfig.getGraphQL());
                        } else if (javaGeneratorConfig.getGraphQLFileName() != null) {
                            manager.registerFileByName(RESOURCES_PATH.concat(javaGeneratorConfig.getGraphQLFileName()));
                        } else if (javaGeneratorConfig.getGraphQLPath() != null) {
                            manager.registerPathByName(RESOURCES_PATH.concat(javaGeneratorConfig.getGraphQLPath()));
                        }

                        manager.registerGraphQL(documentBuilder.buildDocument().toString());
                        mapper.registerFieldMaps();

                        PackageElement packageElement = elementUtils.getPackageOf(typeElement);
                        Map<String, String> operationResourcesContent = javaElementToOperation.buildOperationResources(packageElement, typeElement);

                        Filer filer = processingEnv.getFiler();

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
                                            return Strings.EMPTY;
                                        }
                                        )
                                )
                                .forEach((key, value) -> Try.run(() -> {
                                            FileObject fileObject = filer.createResource(
                                                    StandardLocation.SOURCE_OUTPUT,
                                                    packageElement.getQualifiedName(),
                                                    typeElement.getSimpleName().toString().concat("_").concat(key).concat(".").concat(generatorHandler.extension())
                                            );
                                            Writer writer = fileObject.openWriter();
                                            writer.write(value);
                                            writer.close();
                                        }
                                ));

                        operationInterfaceImplementer.writeToFiler(packageElement, typeElement, operationDAO, generatorHandler.extension(), useInject, processingEnv.getFiler());

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return true;
    }
}
