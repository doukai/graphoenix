package graphoenix.annotation.processor;

import com.google.auto.service.AutoService;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.graphql.builder.schema.DocumentBuilder;
import io.graphoenix.java.generator.implementer.grpc.GrpcFetchHandlerBuilder;
import io.graphoenix.java.generator.implementer.grpc.GrpcInputObjectHandlerBuilder;
import io.graphoenix.java.generator.implementer.grpc.GrpcObjectHandlerBuilder;
import io.graphoenix.java.generator.implementer.grpc.GrpcRequestHandlerBuilder;
import io.graphoenix.java.generator.implementer.grpc.GrpcServiceImplementer;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
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
import java.io.IOException;
import java.util.Set;

import static io.graphoenix.config.ConfigUtil.CONFIG_UTIL;
import static javax.lang.model.SourceVersion.RELEASE_11;

@SupportedAnnotationTypes("io.grpc.stub.annotations.GrpcGenerated")
@SupportedSourceVersion(RELEASE_11)
@AutoService(Processor.class)
public class GrpcServiceProcessor extends AbstractProcessor {

    private BaseProcessor baseProcessor;
    private GrpcInputObjectHandlerBuilder grpcInputObjectHandlerBuilder;
    private GrpcObjectHandlerBuilder grpcObjectHandlerBuilder;
    private GrpcRequestHandlerBuilder grpcRequestHandlerBuilder;
    private GrpcServiceImplementer grpcServiceImplementer;
    private GrpcFetchHandlerBuilder grpcFetchHandlerBuilder;
    private GraphQLConfig graphQLConfig;
    private Filer filer;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.filer = processingEnv.getFiler();
        this.graphQLConfig = CONFIG_UTIL.scan(filer).getOptionalValue(GraphQLConfig.class).orElseGet(GraphQLConfig::new);
        BeanContext.load(GrpcServiceProcessor.class.getClassLoader());
        IGraphQLDocumentManager manager = BeanContext.get(IGraphQLDocumentManager.class);
        DocumentBuilder documentBuilder = BeanContext.get(DocumentBuilder.class).setGraphQLConfig(graphQLConfig);
        this.grpcInputObjectHandlerBuilder = BeanContext.get(GrpcInputObjectHandlerBuilder.class);
        this.grpcObjectHandlerBuilder = BeanContext.get(GrpcObjectHandlerBuilder.class);
        this.grpcRequestHandlerBuilder = BeanContext.get(GrpcRequestHandlerBuilder.class);
        this.grpcServiceImplementer = BeanContext.get(GrpcServiceImplementer.class);
        this.grpcFetchHandlerBuilder = BeanContext.get(GrpcFetchHandlerBuilder.class);
        this.baseProcessor = BeanContext.get(BaseProcessor.class);
        this.baseProcessor.init(processingEnv);

        try {
            if (graphQLConfig.getBuild()) {
                manager.registerGraphQL(documentBuilder.buildDocument().toString());
            }
        } catch (IOException e) {
            Logger.error(e);
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage());
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (annotations.isEmpty()) {
            return false;
        }
        baseProcessor.registerElements(roundEnv);

        try {
            grpcInputObjectHandlerBuilder.setConfiguration(graphQLConfig).writeToFiler(filer);
            grpcObjectHandlerBuilder.setConfiguration(graphQLConfig).writeToFiler(filer);
            grpcRequestHandlerBuilder.setConfiguration(graphQLConfig).writeToFiler(filer);
            grpcServiceImplementer.setConfiguration(graphQLConfig).writeToFiler(filer);
            grpcFetchHandlerBuilder.setConfiguration(graphQLConfig).writeToFiler(filer);
        } catch (IOException e) {
            Logger.error(e);
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage());
        }
        return false;
    }
}
