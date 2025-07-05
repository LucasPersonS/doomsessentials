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
        private boolean fulfilled;

        public HelpRequest(UUID requester) {
            this.requester = requester;
        }

        public UUID getRequester() { return requester; }
        public UUID getAssignedMedico() { return assignedMedico; }
        public void setAssignedMedico(UUID medico) { this.assignedMedico = medico; }
        public boolean isFulfilled() { return fulfilled; }
        public void setFulfilled(boolean f) { this.fulfilled = f; }
    }

    /** Marks the request as fulfilled (help provided). */
    public static boolean markFulfilled(UUID requesterUuid) {
        HelpRequest req = requests.get(requesterUuid);
        if (req != null) {
            req.setFulfilled(true);
            return true;
        }
        return false;
    }

    public static boolean isAssignedMedic(UUID requesterUuid, UUID medicoUuid) {
        HelpRequest req = requests.get(requesterUuid);
        return req != null && medicoUuid.equals(req.getAssignedMedico());
    }

    /** Cancels request if present and not fulfilled, notifying involved players. */
    public static void cancelRequest(ServerPlayer requesterPlayer, String reason) {
        HelpRequest req = requests.remove(requesterPlayer.getUUID());
        if (req == null) return;

        // Notify requester
        requesterPlayer.sendSystemMessage(net.minecraft.network.chat.Component.literal("§eSeu pedido de ajuda foi cancelado: " + reason));

        // Notify medico if assigned
        if (req.getAssignedMedico() != null && !requesterPlayer.level().isClientSide) {
            var medico = requesterPlayer.getServer().getPlayerList().getPlayer(req.getAssignedMedico());
            if (medico != null) {
                medico.getPersistentData().remove("medicoHelpTarget");
                medico.sendSystemMessage(net.minecraft.network.chat.Component.literal("§eO chamado do paciente " + requesterPlayer.getName().getString() + " foi cancelado: " + reason));
            }
        }
    }
} 