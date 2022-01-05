package io.graphoenix.core.utils;

import io.vavr.CheckedFunction1;

public enum TryUtil {
    TRY_UTIL;

    public final CheckedFunction1<String, Class<?>> classForName = Class::forName;

}
