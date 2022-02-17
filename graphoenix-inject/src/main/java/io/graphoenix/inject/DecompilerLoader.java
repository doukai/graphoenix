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
        try {
            return classBytesCache.containsKey(className + ".class") || loadAndCache(className);
        } catch (LoaderException e) {
            Logger.warn(e);
            return false;
        }
    }

    @Override
    public byte[] load(String className) throws LoaderException {
        if (classBytesCache.containsKey(className + ".class") || loadAndCache(className)) {
            return classBytesCache.get(className + ".class");
        }
        throw new LoaderException(className.concat(" not find"));
    }

    public boolean classExists(String className) {
        try {
            Class.forName(className, false, classLoader);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private boolean loadAndCache(String className) throws LoaderException {
        if (!classExists(className)) {
            return false;
        }

        byte[] buffer = new byte[1024 * 2];

        try {
            InputStream inputStream = new FileInputStream(Paths.get(Class.forName(className, false, classLoader).getProtectionDomain().getCodeSource().getLocation().toURI()).toFile());
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
            if (classBytesCache.containsKey(className + ".class")) {
                return true;
            }
        } catch (IOException | ClassNotFoundException | URISyntaxException e) {
            Logger.warn(e);
            throw new LoaderException(e);
        }
        return false;
    }
}
