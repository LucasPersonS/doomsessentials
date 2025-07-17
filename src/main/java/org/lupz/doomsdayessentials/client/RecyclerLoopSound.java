package org.lupz.doomsdayessentials.client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import org.lupz.doomsdayessentials.block.RecycleBlockEntity;
import org.lupz.doomsdayessentials.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.client.Minecraft;

/**
 * Looping sound that plays while the recycler is processing items. Automatically stops when the block stops running or is removed.
 */
public class RecyclerLoopSound extends AbstractTickableSoundInstance {

    private final BlockPos pos;

    public RecyclerLoopSound(BlockPos pos) {
        super(ModSounds.RECYCLER_LOOP.get(), SoundSource.AMBIENT, SoundInstance.createUnseededRandom());
        this.pos = pos;
        this.looping = true;
        this.delay = 0;
        this.x = pos.getX() + 0.5;
        this.y = pos.getY() + 0.5;
        this.z = pos.getZ() + 0.5;
        this.volume = 1.0f;
    }

    @Override
    public void tick() {
        var level = Minecraft.getInstance().level;
        var player = Minecraft.getInstance().player;
        if (player == null || player.isRemoved() || level == null) {
            this.stop();
            return;
        }
        if(!level.isLoaded(pos)){
            this.stop();
            return;
        }
        if(level.getBlockEntity(pos) instanceof RecycleBlockEntity be){
            if(be.isRemoved()){
                this.stop();
            }
        } else {
            this.stop();
        }
    }
} 