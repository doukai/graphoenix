package io.graphoenix.showcase.user.dao;

import io.graphoenix.showcase.user.dto.annotation.StringExpression;
import io.graphoenix.showcase.user.dto.annotation.UserExpression0;
import io.graphoenix.showcase.user.dto.objectType.User;
import io.graphoenix.spi.annotation.GraphQLOperation;
import io.graphoenix.spi.annotation.QueryOperation;
import reactor.core.publisher.Mono;

@GraphQLOperation
public interface UserOperationDAO {

    @QueryOperation(value = "user", selectionSet = "{id password}")
    @UserExpression0(login = @StringExpression($val = "login"))
    Mono<User> getUserByLogin(String login);
}
