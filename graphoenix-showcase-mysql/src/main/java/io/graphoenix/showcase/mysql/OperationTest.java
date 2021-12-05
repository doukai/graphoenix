package io.graphoenix.showcase.mysql;

import io.graphoenix.graphql.builder.handler.bootstrap.DocumentBuildHandler;
import io.graphoenix.mysql.handler.operation.OperationToSQLConvertHandler;
import io.graphoenix.mysql.handler.operation.SQLToFileConvertHandler;
import io.graphoenix.r2dbc.connector.handler.operation.OperationSQLExecuteHandler;
import io.graphoenix.showcase.mysql.generated.annotation.*;
import io.graphoenix.showcase.mysql.generated.enumType.Operator;
import io.graphoenix.showcase.mysql.generated.enumType.Sex;
import io.graphoenix.showcase.mysql.generated.objectType.Role;
import io.graphoenix.showcase.mysql.generated.objectType.User;
import io.graphoenix.spi.annotation.GraphQLOperation;
import io.graphoenix.spi.annotation.MutationOperation;
import io.graphoenix.spi.annotation.QueryOperation;
import reactor.core.publisher.Mono;

import java.util.List;

@GraphQLOperation(
        bootstrapHandlers = DocumentBuildHandler.class,
        pretreatmentHandlers = {OperationToSQLConvertHandler.class, SQLToFileConvertHandler.class},
        executeHandlers = OperationSQLExecuteHandler.class,
        suffix = "sql"
)
public interface OperationTest {

    @QueryOperation(value = "userList", layers = 1)
    User queryUser(List[] name, String name2, Sex sex) throws Exception;

    @QueryOperation(value = "userList", layers = 1)
    List<User> queryUserList(List[] name, String name2, Sex sex) throws Exception;

    @QueryOperation("userList")
    Mono<User> queryUserAsync(String name, Sex sex, Sex sex2, int version) throws Exception;

    @QueryOperation("userList")
    Mono<List<User>> queryUserListAsync(String name, Sex sex, Sex sex2, int version) throws Exception;

    @MutationOperation("user")
    @UserInput(
            $name = "name",
            $sex = "sex",
            login = "login1", password = "password1",
            $organization = "organizationInput",
            $roles = "roles",
            $phones = "phones"
    )
    User mutationUser(Sex sex, String name, String orgName, List<io.graphoenix.showcase.mysql.generated.inputObjectType.RoleInput> roles, io.graphoenix.showcase.mysql.generated.inputObjectType.OrganizationInput organizationInput, List<String> phones) throws Exception;


}
