package io.graphoenix.dagger;

import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.api.loader.LoaderException;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class DecompilerLoader implements Loader {

    private final HashMap<String, byte[]> map = new HashMap<>();

    @Override
    public boolean canLoad(String className) {
        try {
            return map.containsKey(className + ".class") || loadAndCache(className);
        } catch (LoaderException e) {
            return false;
        }
    }

    @Override
    public byte[] load(String className) throws LoaderException {
        if (map.containsKey(className + ".class") || loadAndCache(className)) {
            return map.get(className + ".class");
        }
        return null;
    }

    private boolean loadAndCache(String className) throws LoaderException {

        byte[] buffer = new byte[1024 * 2];

        try {
            InputStream is = new FileInputStream(Paths.get(Class.forName(className, false, getClass().getClassLoader()).getProtectionDomain().getCodeSource().getLocation().toURI()).toFile());

            ZipInputStream zis = new ZipInputStream(is);
            ZipEntry ze = zis.getNextEntry();

            while (ze != null) {
                if (!ze.isDirectory()) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    int read = zis.read(buffer);

                    while (read > 0) {
                        out.write(buffer, 0, read);
                        read = zis.read(buffer);
                    }
                    map.put(ze.getName().replace("/", "."), out.toByteArray());
                }
                ze = zis.getNextEntry();
            }
            zis.closeEntry();
            if (map.containsKey(className + ".class")) {
                return true;
            }
        } catch (IOException | ClassNotFoundException | URISyntaxException e) {
            throw new LoaderException(e);
        }
        return false;
    }
}
