package org.lupz.doomsdayessentials.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.sound.ModSounds;

@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SoundCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("playsoundessentials")
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("sound", StringArgumentType.word())
                    .suggests(SOUND_PROVIDER)
                    .executes(ctx -> playSound(ctx, null))
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> playSound(ctx, EntityArgument.getPlayer(ctx, "player")))
                    )
                )
        );
    }

    private static int playSound(CommandContext<CommandSourceStack> ctx, ServerPlayer player) throws CommandSyntaxException {
        String soundName = StringArgumentType.getString(ctx, "sound");
        ServerPlayer target = player;
        if (target == null) {
            if (ctx.getSource().getEntity() instanceof ServerPlayer) {
                target = (ServerPlayer) ctx.getSource().getEntity();
            } else {
                ctx.getSource().sendFailure(Component.literal("You must specify a player to play the sound for."));
                return 0;
            }
        }

        final ServerPlayer finalTarget = target;
        // Build ResourceLocation automatically
        if(!soundName.contains(":")) soundName = EssentialsMod.MOD_ID+":"+soundName;
        net.minecraft.resources.ResourceLocation rl = net.minecraft.resources.ResourceLocation.tryParse(soundName);
        if(rl==null){
            ctx.getSource().sendFailure(Component.literal("Invalid sound id"));
            return 0;
        }

        net.minecraft.sounds.SoundEvent dynamic = net.minecraft.sounds.SoundEvent.createVariableRangeEvent(rl);
        target.playNotifySound(dynamic, SoundSource.MASTER, 1.0f, 1.0f);
        ctx.getSource().sendSuccess(() -> Component.literal("Playing " + rl + " for " + finalTarget.getName().getString()), false);
        return 1;
    }

    // ----------------------------------------------------------------
    // Suggestion provider based on sounds.json
    // ----------------------------------------------------------------

    private static final java.util.Set<String> SOUND_CACHE = loadSoundKeys();

    private static final com.mojang.brigadier.suggestion.SuggestionProvider<CommandSourceStack> SOUND_PROVIDER = (ctx,builder)->{
        java.util.Set<String> opts = SOUND_CACHE;
        String rem = builder.getRemainingLowerCase();
        opts.stream().filter(s->s.startsWith(rem)).forEach(builder::suggest);
        return builder.buildFuture();
    };

    private static java.util.Set<String> loadSoundKeys(){
        java.util.Set<String> keys = new java.util.HashSet<>();
        try{
            java.io.InputStream is = SoundCommand.class.getResourceAsStream("/assets/"+EssentialsMod.MOD_ID+"/sounds.json");
            if(is!=null){
                com.google.gson.JsonObject root = new com.google.gson.JsonParser().parse(new java.io.InputStreamReader(is)).getAsJsonObject();
                for(String k: root.keySet()) keys.add(k);
            }
        }catch(Exception ignored){}
        return keys;
    }
} 