package io.graphoenix.spi.dao;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import io.vavr.CheckedFunction0;

import java.io.InputStream;
import java.io.InputStreamReader;

public abstract class BaseOperationDAO implements OperationDAO {

    protected static <T> String fileToString(Class<T> beanClass, String fileName) {
        InputStream resourceAsStream = beanClass.getResourceAsStream(fileName);
        assert resourceAsStream != null;
        return CheckedFunction0.of(() -> CharStreams.toString(new InputStreamReader(resourceAsStream, Charsets.UTF_8))).unchecked().get();
    }
}
