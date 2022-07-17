package io.graphoenix.core.converter;

import com.dslplatform.json.JsonConverter;
import com.dslplatform.json.JsonReader;
import com.dslplatform.json.JsonWriter;
import io.graphoenix.core.context.BeanContext;
import jakarta.json.JsonValue;
import jakarta.json.spi.JsonProvider;

import java.io.StringReader;
import java.io.StringWriter;

public class JsonValueConverter {

    private static final JsonProvider jsonProvider = BeanContext.get(JsonProvider.class);

    @JsonConverter(target = JsonValue.class)
    public static abstract class Converter {
        public static final JsonReader.ReadObject<JsonValue> JSON_READER = reader -> {
            if (reader.wasNull()) return null;
            return jsonProvider.createReader(new StringReader(reader.readSimpleString())).readValue();
        };
        public static final JsonWriter.WriteObject<JsonValue> JSON_WRITER = (writer, value) -> {
            if (value == null) {
                writer.writeNull();
            } else {
                StringWriter stringWriter = new StringWriter();
                jsonProvider.createWriter(stringWriter).write(value);
                writer.writeString(stringWriter.toString());
            }
        };
    }
}
