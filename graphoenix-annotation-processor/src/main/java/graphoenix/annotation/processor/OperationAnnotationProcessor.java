package graphoenix.annotation.processor;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.TypeSpec;
import io.graphoenix.common.constant.Hammurabi;
import io.graphoenix.common.manager.SimpleGraphQLDocumentManager;
import io.graphoenix.common.utils.YamlConfigUtil;
import io.graphoenix.graphql.builder.schema.DocumentBuilder;
import io.graphoenix.spi.config.JavaGeneratorConfig;
import io.graphoenix.java.generator.implementer.OperationInterfaceImplementer;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;

import javax.annotation.processing.*;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Set;

@SupportedAnnotationTypes("io.graphoenix.spi.annotation.Operation")
@AutoService(Processor.class)
public class OperationAnnotationProcessor extends AbstractProcessor {

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
    }


    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        for (TypeElement annotation : annotations) {
            Set<? extends Element> bundleClasses = roundEnv.getElementsAnnotatedWith(annotation);

            for (Element bundleClassElement : bundleClasses) {
                if (bundleClassElement.getKind().equals(ElementKind.INTERFACE)) {
                    TypeElement typeElement = (TypeElement) bundleClassElement;
                    try {
                        String resourcesPath = System.getProperty("user.dir").concat(File.separator).concat("src").concat(File.separator).concat("main").concat(File.separator).concat("resources").concat(File.separator);
                        URI configUri = new File(resourcesPath.concat(Hammurabi.CONFIG_FILE_NAME)).toURI();
                        JavaGeneratorConfig javaGeneratorConfig = YamlConfigUtil.YAML_CONFIG_UTIL.loadAs(configUri.toURL().openStream(), JavaGeneratorConfig.class);
                        IGraphQLDocumentManager manager = new SimpleGraphQLDocumentManager();
                        if (javaGeneratorConfig.getGraphQL() != null) {
                            manager.registerDocument(javaGeneratorConfig.getGraphQL());
                        } else if (javaGeneratorConfig.getGraphQLFileName() != null) {
                            URI graphQLFileUri = new File(resourcesPath.concat(javaGeneratorConfig.getGraphQLFileName())).toURI();
                            manager.registerDocument(graphQLFileUri.toURL().openStream());
                        } else if (javaGeneratorConfig.getGraphQLPath() != null) {
                            URI graphQLPathUri = new File(resourcesPath.concat(javaGeneratorConfig.getGraphQLPath())).toURI();
                            manager.registerPath(Paths.get(graphQLPathUri));
                        }
                        manager.registerDocument(new DocumentBuilder(manager).buildDocument().toString());
                        OperationInterfaceImplementer implementer = new OperationInterfaceImplementer(manager, javaGeneratorConfig);
                        TypeSpec typeSpec = implementer.buildImplementClass(typeElement);

                        JavaFileObject builderFile = processingEnv.getFiler().createSourceFile(typeSpec.name);
                        try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {
                            out.println(typeSpec);
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return true;
    }
}
