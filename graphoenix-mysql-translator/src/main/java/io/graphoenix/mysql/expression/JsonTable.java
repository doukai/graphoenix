package io.graphoenix.mysql.expression;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.TableFunction;

import java.util.List;

public class JsonTable extends TableFunction {

    private Expression json;

    private StringValue path;

    private List<ColumnDefinition> columnDefinitions;

    public Expression getJson() {
        return json;
    }

    public JsonTable setJson(Expression json) {
        this.json = json;
        return this;
    }

    public StringValue getPath() {
        return path;
    }

    public JsonTable setPath(StringValue path) {
        this.path = path;
        return this;
    }

    public List<ColumnDefinition> getColumnDefinitions() {
        return columnDefinitions;
    }

    public JsonTable setColumnDefinitions(List<ColumnDefinition> columnDefinitions) {
        this.columnDefinitions = columnDefinitions;
        return this;
    }

    @Override
    public String toString() {
        return "JSON_TABLE(" +
                json + "," +
                path +
                " COLUMNS (" + PlainSelect.getStringList(columnDefinitions, true, false) + ")" +
                ") " + getAlias();
    }

}
