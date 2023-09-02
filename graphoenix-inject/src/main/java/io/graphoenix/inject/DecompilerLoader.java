package io.graphoenix.inject;

import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.api.loader.LoaderException;
import org.tinylog.Logger;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class DecompilerLoader implements Loader {

    private final HashMap<String, byte[]> classBytesCache = new HashMap<>();

    private final ClassLoader classLoader;

    public DecompilerLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public boolean canLoad(String className) {
        if (!classExists(className)) {
            return false;
        }
        String compileClassName = getClassName(className);
        try {
            return classBytesCache.containsKey(compileClassName + ".class") || loadAndCache(compileClassName);
        } catch (LoaderException e) {
            Logger.warn(e);
            return false;
        }
    }

    @Override
    public byte[] load(String className) throws LoaderException {
        String compileClassName = getClassName(className);
        if (classBytesCache.containsKey(compileClassName + ".class") || loadAndCache(compileClassName)) {
            return classBytesCache.get(compileClassName + ".class");
        }
        throw new LoaderException(compileClassName.concat(" not find"));
    }

    public boolean classExists(String className) {
        try {
            Class.forName(className, false, classLoader);
            return true;
        } catch (ClassNotFoundException e) {
            try {
                int i = className.lastIndexOf(".");
                if (i != -1) {
                    String nestedClassName = className.substring(0, i) + "$" + className.substring(i + 1);
                    Class.forName(nestedClassName, false, classLoader);
                    return true;
                } else {
                    return false;
                }
            } catch (ClassNotFoundException e2) {
                return false;
            }
        }
    }

    public String getClassName(String className) {
        try {
            return Class.forName(className, false, classLoader).getName();
        } catch (ClassNotFoundException e) {
            try {
                int i = className.lastIndexOf(".");
                String nestedClassName = className.substring(0, i) + "$" + className.substring(i + 1);
                return Class.forName(nestedClassName, false, classLoader).getName();
            } catch (ClassNotFoundException e2) {
                return null;
            }
        }
    }

    private boolean loadAndCache(String compileClassName) throws LoaderException {

        byte[] buffer = new byte[1024 * 2];

        try {
            InputStream inputStream = new FileInputStream(Paths.get(Class.forName(compileClassName, false, classLoader).getProtectionDomain().getCodeSource().getLocation().toURI()).toFile());
            ZipInputStream zipInputStream = new ZipInputStream(inputStream);
            ZipEntry zipEntry = zipInputStream.getNextEntry();

            while (zipEntry != null) {
                if (!zipEntry.isDirectory()) {
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    int read = zipInputStream.read(buffer);

                    while (read > 0) {
                        byteArrayOutputStream.write(buffer, 0, read);
                        read = zipInputStream.read(buffer);
                    }
                    classBytesCache.put(zipEntry.getName().replace("/", "."), byteArrayOutputStream.toByteArray());
                }
                zipEntry = zipInputStream.getNextEntry();
            }
            zipInputStream.closeEntry();
            if (classBytesCache.containsKey(compileClassName + ".class")) {
                return true;
            }
        } catch (IOException | URISyntaxException | ClassNotFoundException e) {
            Logger.warn(e);
            throw new LoaderException(e);
        }
        return false;
    }
}
