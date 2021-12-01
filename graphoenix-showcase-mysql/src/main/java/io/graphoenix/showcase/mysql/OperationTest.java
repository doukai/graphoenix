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
        executeHandlers = OperationSQLExecuteHandler.class
)
public interface OperationTest {

//    @QueryOperation(value = "userList", layers = 1)
//    @UserExpression($name = "name")
//    User queryUser(List[] name, String name2, Sex sex);

//    @QueryOperation("userList")
//    @UserExpressions(
//            value = {
//                    @UserExpression(name = {"name", "name2"}),
//                    @UserExpression(opr = Operator.NEQ, sex = Sex.MAN)
//            },
//            roles = {
//                    @RoleExpression(opr = Operator.NEQ, name = "role1"),
//            }
//    )
//    Mono<User> queryUserAsync(String name, Sex sex, Sex sex2, int version);

//    @MutationOperation("user")
//    @UserInput(
//            $name = "name",
//            $sex = "sex",
//            login = "login1", password = "password1",
//            organization = @OrganizationInnerInput($name = "orgName", version = 2),
//            $roles = "roles"
//    )
//    User mutationUser(Sex sex, String name, String orgName, List<Role> roles);


    @MutationOperation("user")
    User mutationUser(io.graphoenix.showcase.mysql.generated.inputObjectType.UserInput userInput);

}
