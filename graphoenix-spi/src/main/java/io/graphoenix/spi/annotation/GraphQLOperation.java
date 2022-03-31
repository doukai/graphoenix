package io.graphoenix.spi.annotation;

import io.graphoenix.spi.dao.OperationDAO;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface GraphQLOperation {

    Class<? extends OperationDAO> operationDAO();
}
