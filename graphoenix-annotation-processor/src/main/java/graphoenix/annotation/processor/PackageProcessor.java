package graphoenix.annotation.processor;

import com.google.auto.service.AutoService;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.core.handler.GraphQLConfigRegister;
import io.graphoenix.graphql.builder.schema.DocumentBuilder;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import io.graphoenix.spi.antlr.IGraphQLFieldMapManager;
import org.tinylog.Logger;

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
import java.net.URISyntaxException;
import java.util.Set;

import static javax.lang.model.SourceVersion.RELEASE_11;

@SupportedAnnotationTypes("io.graphoenix.spi.annotation.Package")
@SupportedSourceVersion(RELEASE_11)
@AutoService(Processor.class)
public class PackageProcessor extends BaseProcessor {

    private Filer filer;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        filer = processingEnv.getFiler();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (annotations.isEmpty()) {
            return false;
        }
        GraphQLConfig graphQLConfig = BeanContext.get(GraphQLConfig.class);
        IGraphQLDocumentManager manager = BeanContext.get(IGraphQLDocumentManager.class);
        IGraphQLFieldMapManager mapper = BeanContext.get(IGraphQLFieldMapManager.class);
        DocumentBuilder documentBuilder = BeanContext.get(DocumentBuilder.class);
        roundInit(roundEnv);
        try {
            GraphQLConfigRegister configRegister = BeanContext.get(GraphQLConfigRegister.class);
            configRegister.registerPackage(PackageProcessor.class.getClassLoader());
            if (graphQLConfig.getBuild()) {
                manager.registerGraphQL(documentBuilder.buildDocument(true).toString());
            } else {
                mapper.registerFieldMaps();
            }
            registerElements(roundEnv);
            documentBuilder.buildInvokeDocument();
            registerOperations(roundEnv);
            FileObject packageGraphQL = filer.createResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/graphql/" + graphQLConfig.getPackageName() + ".gql");
            Writer writer = packageGraphQL.openWriter();
            writer.write(documentBuilder.getPackageDocument().toString());
            writer.close();

        } catch (IOException | URISyntaxException e) {
            Logger.error(e);
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage());
        }
        return false;
    }
}
