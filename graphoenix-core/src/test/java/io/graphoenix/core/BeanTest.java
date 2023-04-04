package io.graphoenix.core;

import io.graphoenix.core.config.PackageConfig;
import io.graphoenix.core.context.BeanContext;
import org.junit.jupiter.api.Test;

public class BeanTest {

    @Test
    void testPackageConfig() {
        PackageConfig packageConfig = BeanContext.get(PackageConfig.class);
        System.out.println(packageConfig.getMembers());
    }
}
