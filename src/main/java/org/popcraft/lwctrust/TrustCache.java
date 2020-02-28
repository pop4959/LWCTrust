package org.popcraft.lwctrust;

import com.google.gson.Gson;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The trust cache is used to provide efficient access to trusts.
 */
public class TrustCache extends Cache<UUID, List<UUID>> {

    private LWCTrust lwcTrust;
    private Gson gson;

    public TrustCache(LWCTrust lwcTrust, int max) {
        super(max);
        this.lwcTrust = lwcTrust;
        this.gson = new Gson();
    }

    public List<UUID> load(UUID key) {
        if (containsKey(key)) {
            return get(key);
        }
        File trustFile = new File(lwcTrust.getDataFolder() + File.separator + "trusts"
                + File.separator + key.toString() + ".json");
        try {
            Trust trust = gson.fromJson(new FileReader(trustFile), Trust.class);
            put(key, trust.getTrusted());
        } catch (FileNotFoundException e) {
            put(key, new ArrayList<>());
        }
        return get(key);
    }

    public void save(UUID key) {
        if (!containsKey(key)) {
            return;
        }
        File trustFile = new File(lwcTrust.getDataFolder() + File.separator + "trusts"
                + File.separator + key.toString() + ".json");
        try {
            if (get(key).isEmpty()) {
                if (trustFile.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    trustFile.delete();
                }
            } else {
                String trust = gson.toJson(new Trust(key, get(key)));
                FileWriter fileWriter = new FileWriter(trustFile);
                fileWriter.write(trust);
                fileWriter.close();
            }
        } catch (IOException e) {
            lwcTrust.getLogger().warning("Unable to save file " + trustFile.toString());
        }
    }

}
