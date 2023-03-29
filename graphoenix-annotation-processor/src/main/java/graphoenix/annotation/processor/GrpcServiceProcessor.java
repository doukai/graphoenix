package graphoenix.annotation.processor;

import com.google.auto.service.AutoService;
import io.graphoenix.core.config.GraphQLConfig;
import io.graphoenix.core.context.BeanContext;
import io.graphoenix.core.handler.GraphQLConfigRegister;
import io.graphoenix.graphql.builder.schema.DocumentBuilder;
import io.graphoenix.java.generator.implementer.grpc.GrpcFetchHandlerBuilder;
import io.graphoenix.java.generator.implementer.grpc.GrpcInputObjectHandlerBuilder;
import io.graphoenix.java.generator.implementer.grpc.GrpcObjectHandlerBuilder;
import io.graphoenix.java.generator.implementer.grpc.GrpcRequestHandlerBuilder;
import io.graphoenix.java.generator.implementer.grpc.GrpcServiceImplementer;
import io.graphoenix.spi.antlr.IGraphQLDocumentManager;
import org.tinylog.Logger;

import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Set;

import static javax.lang.model.SourceVersion.RELEASE_11;

@SupportedAnnotationTypes("io.grpc.stub.annotations.GrpcGenerated")
@SupportedSourceVersion(RELEASE_11)
@AutoService(Processor.class)
public class GrpcServiceProcessor extends BaseProcessor {

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
        DocumentBuilder documentBuilder = BeanContext.get(DocumentBuilder.class);
        GrpcInputObjectHandlerBuilder grpcInputObjectHandlerBuilder = BeanContext.get(GrpcInputObjectHandlerBuilder.class);
        GrpcObjectHandlerBuilder grpcObjectHandlerBuilder = BeanContext.get(GrpcObjectHandlerBuilder.class);
        GrpcRequestHandlerBuilder grpcRequestHandlerBuilder = BeanContext.get(GrpcRequestHandlerBuilder.class);
        GrpcServiceImplementer grpcServiceImplementer = BeanContext.get(GrpcServiceImplementer.class);
        GrpcFetchHandlerBuilder grpcFetchHandlerBuilder = BeanContext.get(GrpcFetchHandlerBuilder.class);
        registerElements(roundEnv);
        try {
            GraphQLConfigRegister configRegister = BeanContext.get(GraphQLConfigRegister.class);
            configRegister.registerPackage(GrpcServiceProcessor.class.getClassLoader());
            if (graphQLConfig.getBuild()) {
                manager.registerGraphQL(documentBuilder.buildDocument().toString());
            }
            grpcInputObjectHandlerBuilder.writeToFiler(filer);
            grpcObjectHandlerBuilder.writeToFiler(filer);
            grpcRequestHandlerBuilder.writeToFiler(filer);
            grpcServiceImplementer.writeToFiler(filer);
            grpcFetchHandlerBuilder.writeToFiler(filer);
        } catch (IOException | URISyntaxException e) {
            Logger.error(e);
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage());
        }
        return false;
    }
}
