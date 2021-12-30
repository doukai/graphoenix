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

    protected HashMap<String, byte[]> map = new HashMap<>();

    public HashMap<String, byte[]> getMap() {
        return map;
    }

    public DecompilerLoader(String className) throws LoaderException {

        byte[] buffer = new byte[1024 * 2];

        try {
            InputStream is = new FileInputStream(Paths.get(Class.forName(className).getProtectionDomain().getCodeSource().getLocation().toURI()).toFile());

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
        } catch (IOException | ClassNotFoundException | URISyntaxException e) {
            throw new LoaderException(e);
        }
    }

    @Override
    public boolean canLoad(String internalName) {
        return map.containsKey(internalName + ".class");
    }

    @Override
    public byte[] load(String internalName) {
        return map.get(internalName + ".class");
    }
}
