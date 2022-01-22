package io.graphoenix.showcase.mysql.spi;

import io.graphoenix.r2dbc.connector.dao.R2DBCOperationDAO;
import io.graphoenix.showcase.mysql.dto.annotation.RoleExpression;
import io.graphoenix.showcase.mysql.dto.annotation.UserExpression;
import io.graphoenix.showcase.mysql.dto.annotation.UserExpressions;
import io.graphoenix.showcase.mysql.dto.annotation.UserInput;
import io.graphoenix.showcase.mysql.dto.enumType.Sex;
import io.graphoenix.showcase.mysql.dto.objectType.User;
import io.graphoenix.spi.annotation.GraphQLOperation;
import io.graphoenix.spi.annotation.MutationOperation;
import io.graphoenix.spi.annotation.QueryOperation;
import reactor.core.publisher.Mono;

import javax.inject.Singleton;
import java.util.List;

@Singleton
@GraphQLOperation(operationDAO = R2DBCOperationDAO.class)
public interface OperationTest {

    @QueryOperation(value = "userList", layers = 1)
    @UserExpression($name = "name")
    User queryUser(String name) throws Exception;

    @QueryOperation(value = "userList", layers = 1)
    @UserExpression($sex = "sex")
    List<User> queryUserList(Sex sex) throws Exception;

    @QueryOperation("userList")
    @UserExpressions(
            value = {
                    @UserExpression($name = "name"),
                    @UserExpression($sex = "sex")
            },
            roles = @RoleExpression($name = "roleName")
    )
    Mono<User> queryUserAsync(String name, Sex sex, String roleName) throws Exception;

    @QueryOperation("userList")
    @UserExpressions(
            value = {
                    @UserExpression($name = "name"),
                    @UserExpression($sex = "sex")
            },
            roles = @RoleExpression($name = "roleName")
    )
    Mono<List<User>> queryUserListAsync(String name, Sex sex, String roleName) throws Exception;

    @MutationOperation("user")
    @UserInput(
            $name = "name",
            $sex = "sex",
            login = "login1",
            password = "password1",
            $organization = "organization",
            $roles = "roles",
            $phones = "phones"
    )
    User mutationUser(Sex sex, String name, String orgName, List<io.graphoenix.showcase.mysql.dto.inputObjectType.RoleInput> roles, io.graphoenix.showcase.mysql.dto.inputObjectType.OrganizationInput organization, List<String> phones) throws Exception;
}
