package io.graphoenix.showcase.mysql.api;

import io.graphoenix.showcase.mysql.annotation.Aspect;
import io.graphoenix.showcase.mysql.dto.enumType.Sex;
import io.graphoenix.showcase.mysql.dto.objectType.Organization;
import io.graphoenix.showcase.mysql.dto.objectType.Role;
import io.graphoenix.showcase.mysql.dto.objectType.User;
import io.graphoenix.showcase.mysql.spi.OperationTest;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;
import org.eclipse.microprofile.graphql.Source;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Collection;
import java.util.List;

@GraphQLApi
public class ApiTest {

    protected Provider<OperationTest> operationTest;

    @Inject
    public ApiTest(Provider<OperationTest> operationTest) {
        this.operationTest = operationTest;
    }

    @Query
    public String getUserDetail(@Source User user) {
        return "";
    }

    @Query
    public List<String> getUserDetail6(@Source User user) {
        return null;
    }

    @Query
    public Integer getOrgLevel(@Source Organization organization) {
        Sex.FEMALE.name();
        return 1;
    }

    @Query
    @Aspect(value = "test")
    public Boolean getRoleDisable2(@Source Organization organization) {
        return false;
    }

    public String getUserDetail2(@Source User user) {
        return "";
    }

    public Integer getOrgLevel2(@Source Organization organization) {
        return 1;
    }

    public List<Integer> getOrgLevel3(@Source Organization organization) {
        return null;
    }

    public Boolean getRoleDisable(@Source Organization organization) {
        return false;
    }

    public Collection<User> getUserByOrg(@Source Organization organization) {
        return organization.getUsers();
    }

    public Organization getParent(@Source Organization organization) {
        return organization.getAbove();
    }

    public List<Role> findRole(String name, Integer type, Boolean disable) {
        return null;
    }

    @Query
    public Collection<User> getUserByOrg2(@Source Organization organization) {
        return organization.getUsers();
    }

    @Query
    public Organization getParent2(@Source Organization organization) {
        return organization.getAbove();
    }

    @Query
    public List<Role> findRole2(String name, Integer type, Boolean disable) {
        return null;
    }
}
