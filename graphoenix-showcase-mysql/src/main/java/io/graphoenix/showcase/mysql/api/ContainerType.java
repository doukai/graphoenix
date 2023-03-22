package io.graphoenix.showcase.mysql.api;

import org.eclipse.microprofile.graphql.Type;

@Type("ContainerType1")
public class ContainerType {

    private String name;

    private Float height;

    private int age;

    private boolean sex;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Float getHeight() {
        return height;
    }

    public void setHeight(Float height) {
        this.height = height;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public boolean getSex() {
        return sex;
    }

    public void setSex(boolean sex) {
        this.sex = sex;
    }
}
