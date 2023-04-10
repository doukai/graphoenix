package io.graphoenix.mysql.translator.statement;

public class CreateDataBase {

    private String dataBaseName;

    private Boolean exist;

    public void setDataBaseName(String dataBaseName) {
        this.dataBaseName = dataBaseName;
    }

    public CreateDataBase withDataBaseName(String dataBaseName) {
        this.dataBaseName = dataBaseName;
        return this;
    }

    public Boolean getExist() {
        return exist;
    }

    public void setExist(Boolean exist) {
        this.exist = exist;
    }

    public CreateDataBase withExist(Boolean exist) {
        this.exist = exist;
        return this;
    }

    @Override
    public String toString() {
        String sql = "CREATE DATABASE";
        if (exist != null) {
            sql += " IF";
            if (exist) {
                sql += " ";
            } else {
                sql += " NOT";
            }
            sql += " EXISTS";
        }
        if (dataBaseName != null) {
            sql += " " + dataBaseName;
        }
        return sql;
    }
}
