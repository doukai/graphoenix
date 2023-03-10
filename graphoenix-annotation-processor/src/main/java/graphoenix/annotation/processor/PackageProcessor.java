package graphoenix.annotation.processor;

import com.google.auto.service.AutoService;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.graphql.builder.schema.DocumentBuilder;
import org.tinylog.Logger;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.util.Set;

import static io.graphoenix.config.ConfigUtil.CONFIG_UTIL;
import static javax.lang.model.SourceVersion.RELEASE_11;

@SupportedAnnotationTypes("io.graphoenix.spi.annotation.Package")
@SupportedSourceVersion(RELEASE_11)
@AutoService(Processor.class)
public class PackageProcessor extends AbstractProcessor {

    private BaseProcessor baseProcessor;
    private DocumentBuilder documentBuilder;
    private GraphQLConfig graphQLConfig;
    private Filer filer;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        filer = processingEnv.getFiler();
        graphQLConfig = CONFIG_UTIL.scan(filer).getOptionalValue(GraphQLConfig.class).orElseGet(GraphQLConfig::new);
        BeanContext.load(PackageProcessor.class.getClassLoader());
        documentBuilder = BeanContext.get(DocumentBuilder.class).setGraphQLConfig(graphQLConfig);
        baseProcessor = BeanContext.get(BaseProcessor.class);
        baseProcessor.init(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (annotations.isEmpty()) {
            return false;
        }
        if (graphQLConfig.getPackageName() == null) {
            baseProcessor.getDefaultPackageName(roundEnv).ifPresent(packageName -> graphQLConfig.setPackageName(packageName));
        }
        baseProcessor.registerElements(roundEnv);
        try {
            FileObject packageGraphQL = filer.createResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/graphql/package.gql");
            Writer writer = packageGraphQL.openWriter();
            writer.write(documentBuilder.getDocument().toString());
            writer.close();

        } catch (IOException e) {
            Logger.error(e);
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage());
        }
        return false;
    }
}
