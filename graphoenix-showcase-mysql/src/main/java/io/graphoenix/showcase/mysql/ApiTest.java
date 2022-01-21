package io.graphoenix.showcase.mysql;

import io.graphoenix.showcase.mysql.generated.enumType.Sex;
import io.graphoenix.showcase.mysql.generated.objectType.Organization;
import io.graphoenix.showcase.mysql.generated.objectType.Role;
import io.graphoenix.showcase.mysql.generated.objectType.User;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;
import org.eclipse.microprofile.graphql.Source;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@GraphQLApi
public class ApiTest {

    @Query
    public String getUserDetail(@Source User user) {
        return "";
    }

    @Query
    public Integer getOrgLevel(@Source Organization organization) {
        Sex.FEMALE.name();
        return 1;
    }

    @Query
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
