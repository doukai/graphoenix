package io.graphoenix.showcase.mysql;

import io.graphoenix.showcase.mysql.generated.objectType.User;
import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;
import org.eclipse.microprofile.graphql.Source;

@GraphQLApi
public class ApiTest {

    @Query
    public String getUserLocation(@Source User user) {
        return user.getName();
    }
}
