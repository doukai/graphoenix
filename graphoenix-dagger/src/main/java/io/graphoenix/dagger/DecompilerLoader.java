package io.graphoenix.dagger;


import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.api.loader.LoaderException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class DecompilerLoader implements Loader {

    @Override
    public boolean canLoad(String internalName) {
        return true;
    }

    @Override
    public byte[] load(String internalName) throws LoaderException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            ObjectOutputStream out = null;
            out = new ObjectOutputStream(bos);
            out.writeObject(Class.forName(internalName, true, DecompilerLoader.class.getClassLoader()));
            out.flush();
            return bos.toByteArray();

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        // ignore close exception
        return null;
    }
}
