package io.graphonix.grantlr.manager.impl;

import graphql.parser.antlr.GraphqlParser;
import io.graphonix.grantlr.manager.*;

public class GraphqlAntlrManager {

    private final IGraphqlOperationManager graphqlOperationManager;

    private final IGraphqlObjectManager graphqlObjectManager;

    private final IGraphqlFieldManager graphqlFieldManager;

    private final IGraphqlInputObjectManager graphqlInputObjectManager;

    private final IGraphqlInputValueManager graphqlInputValueManager;

    private final IGraphqlEnumManager graphqlEnumManager;

    private final IGraphqlScalarManager graphqlScalarManager;

    public GraphqlAntlrManager(IGraphqlOperationManager graphqlOperationManager,
                               IGraphqlObjectManager graphqlObjectManager,
                               IGraphqlFieldManager graphqlFieldManager,
                               IGraphqlInputObjectManager graphqlInputObjectManager,
                               IGraphqlInputValueManager graphqlInputValueManager,
                               IGraphqlEnumManager graphqlEnumManager,
                               IGraphqlScalarManager graphqlScalarManager) {
        this.graphqlOperationManager = graphqlOperationManager;
        this.graphqlObjectManager = graphqlObjectManager;
        this.graphqlFieldManager = graphqlFieldManager;
        this.graphqlInputObjectManager = graphqlInputObjectManager;
        this.graphqlInputValueManager = graphqlInputValueManager;
        this.graphqlEnumManager = graphqlEnumManager;
        this.graphqlScalarManager = graphqlScalarManager;
    }

    public void registerDocument(GraphqlParser.DocumentContext documentContext) {
        documentContext.definition().forEach(this::registerDefinition);
    }

    protected void registerDefinition(GraphqlParser.DefinitionContext definitionContext) {
        if (definitionContext.typeSystemDefinition() != null) {
            registerSystemDefinition(definitionContext.typeSystemDefinition());
        }
    }

    protected void registerSystemDefinition(GraphqlParser.TypeSystemDefinitionContext typeSystemDefinitionContext) {
        if (typeSystemDefinitionContext.schemaDefinition() != null) {
            typeSystemDefinitionContext.schemaDefinition().operationTypeDefinition().forEach(this::registerOperationType);
        } else if (typeSystemDefinitionContext.typeDefinition() != null) {
            registerTypeDefinition(typeSystemDefinitionContext.typeDefinition());
        }
    }

    protected void registerOperationType(GraphqlParser.OperationTypeDefinitionContext operationTypeDefinitionContext) {
        graphqlOperationManager.register(operationTypeDefinitionContext);
    }

    protected void registerTypeDefinition(GraphqlParser.TypeDefinitionContext typeDefinitionContext) {

        if (typeDefinitionContext.scalarTypeDefinition() != null) {
            graphqlScalarManager.register(typeDefinitionContext.scalarTypeDefinition());
        } else if (typeDefinitionContext.enumTypeDefinition() != null) {
            graphqlEnumManager.register(typeDefinitionContext.enumTypeDefinition());
        } else if (typeDefinitionContext.objectTypeDefinition() != null) {
            graphqlObjectManager.register(typeDefinitionContext.objectTypeDefinition());
            graphqlFieldManager.register(typeDefinitionContext.objectTypeDefinition());

        } else if (typeDefinitionContext.inputObjectTypeDefinition() != null) {
            graphqlInputObjectManager.register(typeDefinitionContext.inputObjectTypeDefinition());
            graphqlInputValueManager.register(typeDefinitionContext.inputObjectTypeDefinition());
        }
    }
}
