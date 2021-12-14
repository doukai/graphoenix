package io.graphoenix.showcase.mysql.inputObjectType;

import java.lang.Integer;
import java.lang.String;
import org.eclipse.microprofile.graphql.Input;

@Input
public class UserPhonesInput {
  private Integer id;

  private Integer userId;

  private String phone;

  public Integer getId() {
    return this.id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Integer getUserId() {
    return this.userId;
  }

  public void setUserId(Integer userId) {
    this.userId = userId;
  }

  public String getPhone() {
    return this.phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }
}
