package org.popcraft.lwctrust;

import com.griefcraft.model.Permission;
import com.griefcraft.scripting.JavaModule;
import com.griefcraft.scripting.event.LWCAccessEvent;

import java.util.List;
import java.util.UUID;

/**
 * Trust module to interface with the LWC plugin.
 */
public class TrustModule extends JavaModule {

    LWCTrust lwcTrust;

    public TrustModule(LWCTrust lwcTrust) {
        this.lwcTrust = lwcTrust;
    }

    @Override
    public void onAccessRequest(LWCAccessEvent event) {
        UUID owner = event.getProtection().getBukkitOwner().getUniqueId();
        UUID requester = event.getPlayer().getUniqueId();
        List<UUID> trusted = lwcTrust.getTrustCache().load(owner);
        if (trusted.contains(requester)) {
            event.setAccess(Permission.Access.PLAYER);
        }
    }

}
