package io.graphoenix.showcase.user.api;

import io.graphoenix.core.error.GraphQLErrors;
import io.graphoenix.showcase.user.dao.UserOperationDAO;
import io.graphoenix.showcase.user.dto.inputObjectType.UserListQueryTypeArguments;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.NonNull;
import org.eclipse.microprofile.graphql.Query;
import org.eclipse.microprofile.graphql.Source;
import reactor.core.publisher.Mono;

@GraphQLApi
@ApplicationScoped
public class UserApi {

    private final UserOperationDAO operationDAO;

    @Inject
    public UserApi(UserOperationDAO operationDAO) {
        this.operationDAO = operationDAO;
    }

    @Query
    public Mono<String> login(@NonNull String login, @NonNull String password) {
        return operationDAO.getUserByLogin(login)
                .map(user -> {
                            if (user.getPassword().equals(password)) {
                                return user.getId();
                            }
                            throw new GraphQLErrors("authentication failed");
                        }
                );
    }

    public Mono<Void> userTest(@Source UserListQueryTypeArguments userListQueryTypeArguments) {
        return Mono.empty();
    }

    public Mono<Void> userTest2(@Source UserListQueryTypeArguments userListQueryTypeArguments) {
        return Mono.empty();
    }
}
