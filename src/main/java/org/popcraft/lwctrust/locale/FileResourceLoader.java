package org.popcraft.lwctrust.locale;

import org.popcraft.lwctrust.LWCTrust;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

public class FileResourceLoader extends ClassLoader {

    private LWCTrust lwcTrust;

    public FileResourceLoader(LWCTrust lwcTrust) {
        super();
        this.lwcTrust = lwcTrust;
    }

    @Override
    protected URL findResource(String name) {
        File file = new File(lwcTrust.getDataFolder() + File.separator + name);
        try {
            return file.toURI().toURL();
        } catch (MalformedURLException e) {
            return null;
        }
    }

}
