package io.graphoenix.dagger;

import org.jd.core.v1.api.loader.Loader;

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
        return map.containsKey(className + ".class") || loadAndCache(className);
    }

    @Override
    public byte[] load(String className) {
        if (map.containsKey(className + ".class") || loadAndCache(className)) {
            return map.get(className + ".class");
        }
        return null;
    }

    private boolean loadAndCache(String className) {

        byte[] buffer = new byte[1024 * 2];

        try {
            InputStream is = new FileInputStream(Paths.get(Class.forName(className, false, DecompilerLoader.class.getClassLoader()).getProtectionDomain().getCodeSource().getLocation().toURI()).toFile());
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
            e.printStackTrace();
        }
        return false;
    }
}
