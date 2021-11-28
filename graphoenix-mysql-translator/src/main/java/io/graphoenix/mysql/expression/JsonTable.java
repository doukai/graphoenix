package io.graphoenix.mysql.expression;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.TableFunction;

import java.util.List;

public class JsonTable extends TableFunction {

    private Expression json;

    private String path;

    private List<ColumnDefinition> columnDefinitions;

    public Expression getJson() {
        return json;
    }

    public void setJson(Expression json) {
        this.json = json;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<ColumnDefinition> getColumnDefinitions() {
        return columnDefinitions;
    }

    public void setColumnDefinitions(List<ColumnDefinition> columnDefinitions) {
        this.columnDefinitions = columnDefinitions;
    }

    @Override
    public String toString() {

        return "JSON_TABLE(" +
                json + "," +
                path +
                " COLUMNS (" + PlainSelect.getStringList(columnDefinitions, true, false) + ")" +
                ") AS " + getAlias();
    }

}
