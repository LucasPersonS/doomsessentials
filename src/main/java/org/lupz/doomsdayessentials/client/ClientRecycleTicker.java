package org.lupz.doomsdayessentials.client;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.lupz.doomsdayessentials.block.RecycleBlock;
import org.lupz.doomsdayessentials.block.RecycleBlockEntity;
import net.minecraft.client.Minecraft;
import java.util.Map;
import java.util.WeakHashMap;

public class ClientRecycleTicker {

    private static class SoundState {
        RecyclerLoopSound currentSound;
        long lastStartTick;

        SoundState(RecyclerLoopSound sound, long tick) {
            this.currentSound = sound;
            this.lastStartTick = tick;
        }
    }

    private static final Map<RecycleBlockEntity, SoundState> SOUND_STATES = new WeakHashMap<>();

    public static void tick(Level level, BlockPos pos, BlockState state, RecycleBlockEntity be) {
        boolean isRunning = state.getValue(RecycleBlock.RUNNING);
        SoundState soundState = SOUND_STATES.get(be);

        if (isRunning) {
            long currentTick = level.getGameTime();
            // Re-trigger sound every 38 ticks (approx 1.9s) to overlap the 2s clip slightly
            if (soundState == null || (currentTick - soundState.lastStartTick >= 38)) {
                RecyclerLoopSound newSound = new RecyclerLoopSound(pos);
                Minecraft.getInstance().getSoundManager().play(newSound);

                if (soundState == null) {
                    soundState = new SoundState(newSound, currentTick);
                    SOUND_STATES.put(be, soundState);
                } else {
                    soundState.currentSound = newSound;
                    soundState.lastStartTick = currentTick;
                }
            }
        } else {
            if (soundState != null) {
                if (soundState.currentSound != null) {
                    soundState.currentSound.stopPlaying();
                }
                SOUND_STATES.remove(be);
            }
        }
    }
}
