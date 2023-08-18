package io.graphoenix.showcase.order.dto.annotation;

import java.lang.String;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface GoodInput0 {
  String id() default "";

  int quantity() default 0;

  boolean isDeprecated() default false;

  int version() default 0;

  int realmId() default 0;

  String createUserId() default "";

  String createTime() default "";

  String updateUserId() default "";

  String updateTime() default "";

  String createGroupId() default "";

  String __typename() default "";

  int orderId() default 0;

  int productId() default 0;

  int merchantId() default 0;

  String $id() default "";

  String $quantity() default "";

  String $order() default "";

  String $product() default "";

  String $merchant() default "";

  String $isDeprecated() default "";

  String $version() default "";

  String $realmId() default "";

  String $createUserId() default "";

  String $createTime() default "";

  String $updateUserId() default "";

  String $updateTime() default "";

  String $createGroupId() default "";

  String $__typename() default "";

  String $orderId() default "";

  String $productId() default "";

  String $merchantId() default "";
}
