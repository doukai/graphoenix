package io.graphoenix.showcase.mysql.api;

import io.graphoenix.showcase.mysql.dto.enumType.Sex;
import io.graphoenix.showcase.mysql.dto.objectType.Organization;
import io.graphoenix.showcase.mysql.dto.objectType.Role;
import io.graphoenix.showcase.mysql.dto.objectType.User;
import io.graphoenix.showcase.mysql.spi.OperationTest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;
import org.eclipse.microprofile.graphql.Source;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;

@GraphQLApi
@ApplicationScoped
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
    public Flux<Boolean> getRoleDisable2(@Source Organization organization) {
        return Flux.just(false);
    }

    public String getUserDetail2(@Source User user) {
        return "";
    }

    public PublisherBuilder<Integer> getOrgLevel2(@Source Organization organization) {
        return null;
    }

    public Mono<List<Integer>> getOrgLevel3(@Source Organization organization) {
        return null;
    }

    @Query
    public List<Integer> getOrgLevel5(@Source Organization organization) {
        return null;
    }

    public Flux<Boolean> getRoleDisable(@Source Organization organization) {
        return Flux.just(false);
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
    public Mono<Collection<User>> getUserByOrg2(@Source Organization organization) {
        return Mono.just(organization.getUsers());
    }

    @Query
    public Organization getParent2(@Source Organization organization) {
        return organization.getAbove();
    }

    @Query
    public PublisherBuilder<List<Role>> findRole2(String name, Integer type, Boolean disable) {
        return null;
    }
}
