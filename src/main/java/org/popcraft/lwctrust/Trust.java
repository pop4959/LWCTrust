package org.popcraft.lwctrust;

import java.util.List;
import java.util.UUID;

/**
 * Trust POJO.
 */
public class Trust {

    private UUID owner;
    private List<UUID> trusted;

    public Trust(UUID owner, List<UUID> trusted) {
        this.owner = owner;
        this.trusted = trusted;
    }

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
    }

    public List<UUID> getTrusted() {
        return trusted;
    }

    public void setTrusted(List<UUID> trusted) {
        this.trusted = trusted;
    }

}
