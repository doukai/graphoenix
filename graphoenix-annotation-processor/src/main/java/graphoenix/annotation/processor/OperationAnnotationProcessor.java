package graphoenix.annotation.processor;

import com.google.auto.service.AutoService;

import javax.annotation.processing.*;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
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
                        JavaFileObject builderFile = processingEnv.getFiler().createSourceFile("AnnotationTest");

                        try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {
                            out.println("class AnnotationTest{}");
                        }

                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }
        return true;
    }
}
