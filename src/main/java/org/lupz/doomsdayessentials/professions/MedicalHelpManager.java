package org.lupz.doomsdayessentials.professions;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;

/**
 * Tracks open medical help requests.
 */
public final class MedicalHelpManager {

    private MedicalHelpManager() {}

    private static final Map<UUID, HelpRequest> requests = new ConcurrentHashMap<>();

    public static boolean createRequest(ServerPlayer requester) {
        if (requests.containsKey(requester.getUUID())) {
            return false; // already open
        }
        requests.put(requester.getUUID(), new HelpRequest(requester.getUUID()));
        return true;
    }

    public static boolean acceptRequest(ServerPlayer medico, UUID requesterUuid) {
        HelpRequest req = requests.get(requesterUuid);
        if (req == null || req.getAssignedMedico() != null) {
            return false;
        }
        req.setAssignedMedico(medico.getUUID());
        medico.getPersistentData().putUUID("medicoHelpTarget", requesterUuid);
        return true;
    }

    public static UUID completeRequest(UUID requesterUuid) {
        HelpRequest req = requests.remove(requesterUuid);
        return req != null ? req.getAssignedMedico() : null;
    }

    public static HelpRequest getRequest(UUID requesterUuid) { return requests.get(requesterUuid); }

    public static class HelpRequest {
        private final UUID requester;
        private UUID assignedMedico;

        public HelpRequest(UUID requester) {
            this.requester = requester;
        }

        public UUID getRequester() { return requester; }
        public UUID getAssignedMedico() { return assignedMedico; }
        public void setAssignedMedico(UUID medico) { this.assignedMedico = medico; }
    }
} 