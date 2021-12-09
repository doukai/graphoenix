package graphoenix.annotation.processor;

import com.google.auto.service.AutoService;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.graphoenix.config.TypesafeConfigProviderResolver;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.*;
import javax.lang.model.util.Elements;
import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

@SupportedAnnotationTypes("org.eclipse.microprofile.config.inject.ConfigProperty")
@AutoService(Processor.class)
public class ConfigPropertyAnnotationProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        for (TypeElement annotation : annotations) {
            Set<? extends Element> bundleClasses = roundEnv.getElementsAnnotatedWith(annotation);
            for (Element bundleClassElement : bundleClasses) {

                final Elements elementUtils = this.processingEnv.getElementUtils();


                String configPropertyClassName = ConfigProperty.class.getName();
                AnnotationMirror configPropertyMirror = bundleClassElement.getAnnotationMirrors().stream()
                        .filter(annotationMirror -> annotationMirror.getAnnotationType().toString().equals(configPropertyClassName))
                        .findFirst()
                        .orElseThrow();

                Map<? extends ExecutableElement, ? extends AnnotationValue> elementValuesWithDefaults = elementUtils.getElementValuesWithDefaults(configPropertyMirror);

                String name = elementValuesWithDefaults.entrySet().stream()
                        .filter(entry -> entry.getKey().getSimpleName().toString().equals("name"))
                        .findFirst()
                        .map(entry -> (String) entry.getValue().getValue())
                        .orElseThrow();

                String resourcesPath = System.getProperty("user.dir")
                        .concat(File.separator)
                        .concat("src")
                        .concat(File.separator)
                        .concat("main")
                        .concat(File.separator)
                        .concat("resources")
                        .concat(File.separator)
                        .concat("application.conf");

                Config config = ConfigFactory.parseFile(new File(resourcesPath));
                config.getString("generator.basePackageName");


            }
        }

        return true;
    }
}
