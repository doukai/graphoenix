package io.graphoenix.showcase.mysql;

import io.graphoenix.showcase.mysql.generated.annotation.UserExpression;
import io.graphoenix.showcase.mysql.generated.annotation.UserExpressions;
import io.graphoenix.showcase.mysql.generated.enumType.Sex;
import io.graphoenix.showcase.mysql.generated.objectType.User;
import io.graphoenix.spi.annotation.Mutation;
import io.graphoenix.spi.annotation.Operation;
import io.graphoenix.spi.annotation.Query;
import reactor.core.publisher.Mono;

@Operation
public interface OperationTest {

    @Query("user")
    @UserExpressions({
            @UserExpression(name = "name"),
            @UserExpression(sex = "sex")
    })
    User queryUser(String name, Sex sex);

    @Query("user")
    @UserExpressions({
            @UserExpression(name = "name"),
            @UserExpression(sex = "sex")
    })
    Mono<User> queryUserAsync();

    @Mutation("user")
    User mutationUser();

    @Mutation("user")
    Mono<User> mutationUserAsync();

}
