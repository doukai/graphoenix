package io.graphoenix.showcase.order.dto.annotation;

import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface MerchantInput0 {
  String id() default "";

  String name() default "";

  boolean isDeprecated() default false;

  int version() default 0;

  String realmId() default "";

  String createUserId() default "";

  String createTime() default "";

  String updateUserId() default "";

  String updateTime() default "";

  String createGroupId() default "";

  String __typename() default "";

  int organizationId() default 0;

  String $id() default "";

  String $name() default "";

  String $organization() default "";

  String $customerServices() default "";

  String $partners() default "";

  String $director() default "";

  String $isDeprecated() default "";

  String $version() default "";

  String $realmId() default "";

  String $createUserId() default "";

  String $createTime() default "";

  String $updateUserId() default "";

  String $updateTime() default "";

  String $createGroupId() default "";

  String $__typename() default "";

  String $organizationId() default "";

  String $merchantPartners() default "";

  String $merchantPartnersAggregate() default "";

  String $merchantPartnersConnection() default "";

  String $merchantDirector() default "";
}
