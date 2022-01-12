package graphoenix.annotation.processor;

import com.google.auto.service.AutoService;
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.graphql.generator.translator.JavaElementToObject;
import io.vavr.Tuple;
import io.vavr.Tuple2;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@SupportedAnnotationTypes("org.eclipse.microprofile.graphql.Type")
@AutoService(Processor.class)
public class TypeProcessor extends AbstractProcessor {

    private JavaElementToObject javaElementToObject;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.javaElementToObject = BeanContext.get(JavaElementToObject.class);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) {
            Set<? extends Element> bundleClasses = roundEnv.getElementsAnnotatedWith(annotation);
            for (Element bundleClassElement : bundleClasses) {
//                if (bundleClassElement.getAnnotation(Generated.class) == null) {
                if (bundleClassElement.getKind().equals(ElementKind.CLASS)) {
                    final Elements elementUtils = processingEnv.getElementUtils();
                    final Filer filer = processingEnv.getFiler();
                    TypeElement typeElement = (TypeElement) bundleClassElement;
                    PackageElement packageElement = elementUtils.getPackageOf(typeElement);

                    List<TypeElement> interfaceList = typeElement.getInterfaces().stream()
                            .map(typeMirror -> (TypeElement) processingEnv.getTypeUtils().asElement(typeMirror))
                            .collect(Collectors.toList());

                    List<Tuple2<String, TypeElement>> fieldList = typeElement.getEnclosedElements().stream()
                            .filter(element -> element.getKind().equals(ElementKind.FIELD))
                            .map(element -> Tuple.of(element.getSimpleName().toString(), (TypeElement) processingEnv.getTypeUtils().asElement(element.asType())))
                            .collect(Collectors.toList());

                    try {
                        FileObject fileObject = filer.createResource(StandardLocation.SOURCE_OUTPUT, packageElement.getQualifiedName(), typeElement.getSimpleName().toString().concat(".gql"));
                        Writer writer = fileObject.openWriter();
                        writer.write(javaElementToObject.buildObject(typeElement, processingEnv.getTypeUtils()));
                        writer.close();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
//                }
            }
        }
        return false;
    }
}
