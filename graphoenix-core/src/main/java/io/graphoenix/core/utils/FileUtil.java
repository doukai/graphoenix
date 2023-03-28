package io.graphoenix.core.utils;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import org.tinylog.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public enum FileUtil {
    FILE_UTIL;

    public <T> String fileToString(Class<T> beanClass, String fileName) {
        InputStream resourceAsStream = beanClass.getResourceAsStream(fileName);
        assert resourceAsStream != null;
        try {
            return CharStreams.toString(new InputStreamReader(resourceAsStream, Charsets.UTF_8));
        } catch (IOException e) {
            Logger.error(e);
        }
        return null;
    }
}
