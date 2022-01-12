package graphoenix.annotation.processor;

import com.google.auto.service.AutoService;
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.graphql.generator.translator.JavaElementToInputType;
import jakarta.annotation.Generated;

import javax.annotation.processing.*;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.util.Set;

@SupportedAnnotationTypes("org.eclipse.microprofile.graphql.Input")
@AutoService(Processor.class)
public class InputProcessor extends AbstractProcessor {

    private JavaElementToInputType javaElementToInputType;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.javaElementToInputType = BeanContext.get(JavaElementToInputType.class);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            Set<? extends Element> bundleClasses = roundEnv.getElementsAnnotatedWith(annotation);
            for (Element bundleClassElement : bundleClasses) {
                if (bundleClassElement.getAnnotation(Generated.class) == null) {
                    if (bundleClassElement.getKind().equals(ElementKind.CLASS)) {
                        final Elements elementUtils = processingEnv.getElementUtils();
                        final Filer filer = processingEnv.getFiler();
                        TypeElement typeElement = (TypeElement) bundleClassElement;
                        PackageElement packageElement = elementUtils.getPackageOf(typeElement);

                        try {
                            FileObject fileObject = filer.createResource(StandardLocation.SOURCE_OUTPUT, packageElement.getQualifiedName(), typeElement.getSimpleName().toString().concat(".gql"));
                            Writer writer = fileObject.openWriter();
                            writer.write(javaElementToInputType.buildInputType(typeElement, processingEnv.getTypeUtils()));
                            writer.close();

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        return false;
    }
}
