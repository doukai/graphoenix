package graphoenix.annotation.processor;

import com.google.auto.service.AutoService;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.*;
import javax.lang.model.util.Elements;
import java.util.Map;
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

                try {

                    Config config = ConfigProvider.getConfig();
                    config.getValue(name, Class.forName(bundleClassElement.getSimpleName().toString()));


                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }


            }
        }

        return true;
    }
}
