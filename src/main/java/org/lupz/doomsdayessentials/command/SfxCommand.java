package org.lupz.doomsdayessentials.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ParticleArgument;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.DustParticleOptions;
import org.joml.Vector3f;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lupz.doomsdayessentials.EssentialsMod;
import org.lupz.doomsdayessentials.trail.TrailData;
import org.lupz.doomsdayessentials.trail.TrailManager;
import net.minecraft.world.effect.MobEffectInstance;
import org.lupz.doomsdayessentials.effect.ModEffects;
import org.lupz.doomsdayessentials.network.PacketHandler;
import net.minecraftforge.network.PacketDistributor;
import org.lupz.doomsdayessentials.network.packet.s2c.MadnessEffectPacket;
import net.minecraft.world.phys.Vec3;
import org.lupz.doomsdayessentials.utils.DelayedTeleportManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.MutableComponent;
import java.util.concurrent.CompletableFuture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import org.lupz.doomsdayessentials.sound.ModSounds;

@Mod.EventBusSubscriber(modid = EssentialsMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SfxCommand {
    private SfxCommand() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        new SfxCommand(event.getDispatcher(), event.getBuildContext());
    }

    public SfxCommand(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        dispatcher.register(
            Commands.literal("dooms").requires(src -> src.hasPermission(2))
                .then(Commands.literal("sfx")
                    // -----------------------------------------------------------------
                    // /dooms sfx trail <player> <particle> [dx dy dz speed count]
                    // -----------------------------------------------------------------
                    .then(Commands.literal("trail")
                        .then(Commands.argument("targets", EntityArgument.players())
                            .then(Commands.argument("particle", ParticleArgument.particle(context))
                                // Default invocation
                                .executes(ctx -> setTrail(
                                    ctx.getSource(),
                                    EntityArgument.getPlayers(ctx, "targets"),
                                    ParticleArgument.getParticle(ctx, "particle"),
                                    0.1f, 0.5f, 0.1f, 0.1f, 10
                                ))
                                // Full signature
                                .then(Commands.argument("dx", FloatArgumentType.floatArg())
                                    .then(Commands.argument("dy", FloatArgumentType.floatArg())
                                        .then(Commands.argument("dz", FloatArgumentType.floatArg())
                                            .then(Commands.argument("speed", FloatArgumentType.floatArg())
                                                .then(Commands.argument("count", IntegerArgumentType.integer())
                                                    .executes(ctx -> setTrail(
                                                        ctx.getSource(),
                                                        EntityArgument.getPlayers(ctx, "targets"),
                                                        ParticleArgument.getParticle(ctx, "particle"),
                                                        FloatArgumentType.getFloat(ctx, "dx"),
                                                        FloatArgumentType.getFloat(ctx, "dy"),
                                                        FloatArgumentType.getFloat(ctx, "dz"),
                                                        FloatArgumentType.getFloat(ctx, "speed"),
                                                        IntegerArgumentType.getInteger(ctx, "count")
                                                    ))
                                                )
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    )
                    // -----------------------------------------------------------------
                    // /dooms sfx frequency <player> <seconds>
                    // -----------------------------------------------------------------
                    .then(Commands.literal("frequency")
                        .then(Commands.argument("targets", EntityArgument.players())
                            .then(Commands.argument("seconds", IntegerArgumentType.integer(1))
                                .executes(ctx -> applyFrequency(
                                        ctx.getSource(),
                                        EntityArgument.getPlayers(ctx, "targets"),
                                        IntegerArgumentType.getInteger(ctx, "seconds")
                                ))
                            )
                        )
                    )
                    // -----------------------------------------------------------------
                    // /dooms sfx madness <player> <durationTicks> <intensity>
                    // -----------------------------------------------------------------
                    .then(Commands.literal("madness")
                        .then(Commands.argument("targets", EntityArgument.players())
                            .then(Commands.argument("duration", IntegerArgumentType.integer(1))
                                .then(Commands.argument("shake", FloatArgumentType.floatArg(0.0f,1.0f))
                                    .executes(ctx -> applyMadness(
                                            ctx.getSource(),
                                            EntityArgument.getPlayers(ctx, "targets"),
                                            IntegerArgumentType.getInteger(ctx, "duration"),
                                            FloatArgumentType.getFloat(ctx, "shake"),
                                            FloatArgumentType.getFloat(ctx, "shake")
                                    ))
                                    .then(Commands.argument("overlay", FloatArgumentType.floatArg(0.0f,1.0f))
                                        .executes(ctx -> applyMadness(
                                                ctx.getSource(),
                                                EntityArgument.getPlayers(ctx, "targets"),
                                                IntegerArgumentType.getInteger(ctx, "duration"),
                                                FloatArgumentType.getFloat(ctx, "shake"),
                                                FloatArgumentType.getFloat(ctx, "overlay")
                                        ))
                                    )
                                )
                            )
                        )
                    )
                    // -----------------------------------------------------------------
                    // /dooms sfx clear <player>
                    // -----------------------------------------------------------------
                    .then(Commands.literal("clear")
                        .then(Commands.argument("targets", EntityArgument.players())
                            .executes(ctx -> clearEffects(
                                    ctx.getSource(),
                                    EntityArgument.getPlayers(ctx, "targets")
                            ))
                        )
                    )
                    // -----------------------------------------------------------------
                    // /dooms sfx tphere <targetPlayer>
                    // -----------------------------------------------------------------
                    .then(Commands.literal("tphere")
                        .then(Commands.argument("targets", EntityArgument.players())
                            // base form – use defaults
                            .executes(ctx -> teleportHere(
                                    ctx.getSource(),
                                    EntityArgument.getPlayers(ctx, "targets"),
                                    new DustParticleOptions(new Vector3f(1.0f, 0.0f, 0.0f), 1.0f), // default particle
                                    10,                        // default count
                                    0.0,                       // default speed
                                    0.5, 0.0, 0.5              // default spread
                            ))
                            // optional particle argument
                            .then(Commands.literal("particle")
                                .then(Commands.argument("particle", ParticleArgument.particle(context))
                                     .executes(ctx -> teleportHere(
                                            ctx.getSource(),
                                            EntityArgument.getPlayers(ctx, "targets"),
                                            ParticleArgument.getParticle(ctx, "particle"),
                                            10, 0.0, 0.5, 0.0, 0.5
                                    ))
                                    .then(Commands.literal("count")
                                        .then(Commands.argument("count", IntegerArgumentType.integer(1))
                                            .executes(ctx -> teleportHere(
                                                    ctx.getSource(),
                                                    EntityArgument.getPlayers(ctx, "targets"),
                                                    ParticleArgument.getParticle(ctx, "particle"),
                                                    IntegerArgumentType.getInteger(ctx, "count"),
                                                    0.0, 0.5, 0.0, 0.5
                                            ))
                                            .then(Commands.literal("speed")
                                                .then(Commands.argument("speed", DoubleArgumentType.doubleArg(0.0))
                                                    .executes(ctx -> teleportHere(
                                                            ctx.getSource(),
                                                            EntityArgument.getPlayers(ctx, "targets"),
                                                            ParticleArgument.getParticle(ctx, "particle"),
                                                            IntegerArgumentType.getInteger(ctx, "count"),
                                                            DoubleArgumentType.getDouble(ctx, "speed"),
                                                            0.5, 0.0, 0.5
                                                    ))
                                                    .then(Commands.literal("spread")
                                                        .then(Commands.argument("dx", DoubleArgumentType.doubleArg(0.0))
                                                            .then(Commands.argument("dy", DoubleArgumentType.doubleArg(0.0))
                                                                .then(Commands.argument("dz", DoubleArgumentType.doubleArg(0.0))
                                                                    .executes(ctx -> teleportHere(
                                                                            ctx.getSource(),
                                                                            EntityArgument.getPlayers(ctx, "targets"),
                                                                            ParticleArgument.getParticle(ctx, "particle"),
                                                                            IntegerArgumentType.getInteger(ctx, "count"),
                                                                            DoubleArgumentType.getDouble(ctx, "speed"),
                                                                            DoubleArgumentType.getDouble(ctx, "dx"),
                                                                            DoubleArgumentType.getDouble(ctx, "dy"),
                                                                            DoubleArgumentType.getDouble(ctx, "dz")
                                                                    ))
                                                                )
                                                            )
                                                        )
                                                    )
                                                )
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    )
                    // -----------------------------------------------------------------
                    // /dooms sfx tpback <player>
                    // -----------------------------------------------------------------
                    .then(Commands.literal("tpback")
                        .then(Commands.argument("targets", EntityArgument.players())
                            .executes(ctx -> teleportBack(
                                    ctx.getSource(),
                                    EntityArgument.getPlayers(ctx, "targets")
                            ))
                        )
                    )
                    // -----------------------------------------------------------------
                    // /dooms sfx blacksmoke <x> <y> <z> <radius> <count>
                    // -----------------------------------------------------------------
                    .then(Commands.literal("blacksmoke")
                        .then(Commands.argument("x", FloatArgumentType.floatArg())
                            .then(Commands.argument("y", FloatArgumentType.floatArg())
                                .then(Commands.argument("z", FloatArgumentType.floatArg())
                                    .then(Commands.argument("radius", FloatArgumentType.floatArg(1.0f))
                                        .then(Commands.argument("count", IntegerArgumentType.integer(1))
                                            .executes(ctx -> spawnBlackSmoke(
                                                    ctx.getSource(),
                                                    new Vec3(FloatArgumentType.getFloat(ctx, "x"),
                                                             FloatArgumentType.getFloat(ctx, "y"),
                                                             FloatArgumentType.getFloat(ctx, "z")),
                                                    FloatArgumentType.getFloat(ctx, "radius"),
                                                    IntegerArgumentType.getInteger(ctx, "count")
                                            ))
                                        )
                                    )
                                )
                            )
                        )
                    )
                    // Particle subcommand removed – CustomParticleManager deprecated
                    // -----------------------------------------------------------------
                    // /dooms sfx sky <player> <hexColor|clear>
                    // -----------------------------------------------------------------

                    // persistent sky color
                    .then(Commands.literal("global")
                        .then(Commands.literal("set")
                            .then(Commands.literal("day")
                                .then(Commands.argument("color", StringArgumentType.word())
                                    .then(Commands.argument("alpha", FloatArgumentType.floatArg(0.0f,1.0f))
                                        .executes(ctx -> setSkyColorGlobal(ctx.getSource(),
                                                true,
                                                StringArgumentType.getString(ctx, "color"),
                                                FloatArgumentType.getFloat(ctx, "alpha")))
                                    )
                                    .executes(ctx -> setSkyColorGlobal(ctx.getSource(),
                                            true,
                                            StringArgumentType.getString(ctx, "color"), 1.0f))
                                )
                            )
                            .then(Commands.literal("night")
                                .then(Commands.argument("color", StringArgumentType.word())
                                    .then(Commands.argument("alpha", FloatArgumentType.floatArg(0.0f,1.0f))
                                        .executes(ctx -> setSkyColorGlobal(ctx.getSource(),
                                                false,
                                                StringArgumentType.getString(ctx, "color"),
                                                FloatArgumentType.getFloat(ctx, "alpha")))
                                    )
                                    .executes(ctx -> setSkyColorGlobal(ctx.getSource(),
                                            false,
                                            StringArgumentType.getString(ctx, "color"), 1.0f))
                                )
                            )
                        )
                        .then(Commands.literal("clear")
                            .executes(ctx -> clearSkyGlobal(ctx.getSource()))
                        )
                    )
                    // -----------------------------------------------------------------
                    // /dooms sfx font <player> <font> [style] [color] <text>
                    // -----------------------------------------------------------------
                    .then(Commands.literal("font")
                        .then(Commands.argument("targets", EntityArgument.players())
                            .then(Commands.argument("font", StringArgumentType.word()).suggests(FontSuggestion::suggest)
                                // variant: font style [color] text  (this branch needs to be before plain text)
                                .then(Commands.argument("style", StringArgumentType.word()).suggests(StyleSuggestion::suggest)
                                    .then(Commands.argument("color", StringArgumentType.word()).suggests(ColorSuggestion::suggest)
                                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                            .executes(ctx -> sendFontTitle(
                                                    ctx.getSource(),
                                                    EntityArgument.getPlayers(ctx, "targets"),
                                                    StringArgumentType.getString(ctx, "font"),
                                                    StringArgumentType.getString(ctx, "style"),
                                                    StringArgumentType.getString(ctx, "color"),
                                                    StringArgumentType.getString(ctx, "text")
                                            ))
                                        )
                                    )
                                    // fallback: font style text
                                    .then(Commands.argument("text", StringArgumentType.greedyString())
                                        .executes(ctx -> sendFontTitle(
                                                ctx.getSource(),
                                                EntityArgument.getPlayers(ctx, "targets"),
                                                StringArgumentType.getString(ctx, "font"),
                                                StringArgumentType.getString(ctx, "style"),
                                                "white",
                                                StringArgumentType.getString(ctx, "text")
                                        ))
                                    )
                                )
                            )
                        )
                    )
                )
        );
    }

    // ---------------------------------------------------------------------
    // Implementation helpers
    // ---------------------------------------------------------------------

    private int setTrail(CommandSourceStack source, java.util.Collection<ServerPlayer> targets, ParticleOptions particle,
                         float dx, float dy, float dz, float speed, int count) {
        for(ServerPlayer player: targets){
            TrailData trailData = new TrailData(particle, dx, dy, dz, speed, count);
            TrailManager.getInstance().setTrail(player.getUUID(), trailData);
        }
        source.sendSuccess(() -> Component.literal("Trail set for " + targets.size()+" player(s)"), true);
        return targets.size();
    }

    private int applyFrequency(CommandSourceStack source, java.util.Collection<ServerPlayer> targets, int seconds){
        int ticks = seconds * 20;
        for(ServerPlayer p: targets){
            p.addEffect(new MobEffectInstance(ModEffects.FREQUENCY.get(), ticks));
        }
        source.sendSuccess(() -> Component.literal("Frequency effect applied to "+targets.size()+" player(s)"), true);
        return targets.size();
    }

    private int applyMadness(CommandSourceStack source, java.util.Collection<ServerPlayer> targets, int durationTicks, float shake, float overlay){
        for(ServerPlayer p: targets){
            PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new MadnessEffectPacket(durationTicks, shake, overlay));
            p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DARKNESS, durationTicks, 0, false, false));
        }
        source.sendSuccess(() -> Component.literal("Madness effect applied to "+targets.size()+" player(s)"), true);
        return targets.size();
    }

    private int clearEffects(CommandSourceStack src, java.util.Collection<ServerPlayer> players) {
        for(ServerPlayer p: players){
            TrailManager.getInstance().removeTrail(p.getUUID());
            p.removeAllEffects();
            PacketHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), new MadnessEffectPacket(0,0F,0F));
        }
        src.sendSuccess(() -> Component.literal("Visual effects cleared for "+players.size()+" player(s)"), true);
        return players.size();
    }

    private int teleportHere(CommandSourceStack source, java.util.Collection<ServerPlayer> targets,
                             ParticleOptions particle, int count, double speed,
                             double dx, double dy, double dz){
        if(!(source.getEntity() instanceof ServerPlayer staff)){
            source.sendFailure(Component.literal("Only a player can run this command"));
            return 0;
        }
        ServerLevel level = staff.serverLevel();
        for(ServerPlayer t: targets){
            Vec3 origin = t.position();
            boolean smokeTrail = (particle == net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE || particle == net.minecraft.core.particles.ParticleTypes.SMOKE);
            DelayedTeleportManager.scheduleTeleport(t, origin, staff.serverLevel(), staff.position(), 60, smokeTrail);
            // Beam effect at target position (before teleport)
            spawnAbductionBeam(level, origin, particle, count, speed, dx, dy, dz);
        }
        // thunder sound to everyone
        for(ServerPlayer p: level.players()){
            p.playNotifySound(org.lupz.doomsdayessentials.sound.ModSounds.ABDUCTION_THUNDER.get(), net.minecraft.sounds.SoundSource.MASTER, 1.0f, 1.0f);
        }
        source.sendSuccess(() -> Component.literal("Teleporting "+targets.size()+" player(s) in 3 seconds"), true);
        return targets.size();
    }

    // ------------------------------------------------------------------
    // Abduction beam helper
    // ------------------------------------------------------------------
    private void spawnAbductionBeam(ServerLevel level, Vec3 pos,
                                    ParticleOptions particle, int count, double speed,
                                    double dx, double dy, double dz){
        // Adjust parameters for dense black smoke
        if (particle == net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE || particle == net.minecraft.core.particles.ParticleTypes.SMOKE) {
            count = 50;
            speed = 0.02;
            dx = dz = 0.2;
            dy = 0.1;
        }

        // play beam sound once per target location for nearby players
        level.playSound(null, pos.x, pos.y, pos.z,
                org.lupz.doomsdayessentials.sound.ModSounds.ABDUCTION_BEAM.get(),
                net.minecraft.sounds.SoundSource.MASTER, 1.0f, 1.0f);

        // Photon FX support removed – always use vanilla particle fallback.
        // -------------------------------------------------------------
        // Vanilla fallback: column of smoke particles
        // -------------------------------------------------------------
        for(double y=pos.y; y< level.getMaxBuildHeight(); y+=4){
            level.sendParticles(particle,
                    pos.x, y, pos.z,
                    count,
                    dx, dy, dz,
                    speed);
        }
    }

    private int teleportBack(CommandSourceStack source, java.util.Collection<ServerPlayer> players) {
        int successCount=0;
        for(ServerPlayer p: players){
            if(DelayedTeleportManager.teleportBack(p)) successCount++;
        }
        if(successCount>0){
            final int countFinal = successCount;
            source.sendSuccess(() -> Component.literal("Teleported back "+countFinal+" player(s)"), true);
            return countFinal;
        }
        source.sendFailure(Component.literal("No saved location for any player"));
        return 0;
    }

    private int spawnBlackSmoke(CommandSourceStack source, Vec3 center, float radius, int count){
        if(!(source.getLevel() instanceof ServerLevel)){
            source.sendFailure(Component.literal("Must be executed in a server world"));
            return 0;
        }
        ServerLevel level = (ServerLevel) source.getLevel();
        for(int i=0;i<count;i++){
            double ox = (level.random.nextDouble()-0.5)*2*radius;
            double oy = level.random.nextDouble()*radius;
            double oz = (level.random.nextDouble()-0.5)*2*radius;
            level.sendParticles(ParticleTypes.LARGE_SMOKE,
                    center.x + ox, center.y + oy, center.z + oz,
                    1, 0, 0.1, 0, 0.01);
        }
        source.sendSuccess(() -> Component.literal("Black smoke spawned"), true);
        return 1;
    }

    // spawnParticleCmd removed – deprecated.

    // ---------------------------------------------------------------------
    // Sky tint helper
    // ---------------------------------------------------------------------
    // ---------------------------- Global persistent sky color ----------------------------
    private static int setSkyColorGlobal(CommandSourceStack src, boolean dayPhase, String arg, float alpha){
        int rgb = parseColor(arg, src);
        if(rgb<0) return 0;
        if(dayPhase)
            org.lupz.doomsdayessentials.utils.SkyColorManager.get().setDay(rgb, alpha);
        else
            org.lupz.doomsdayessentials.utils.SkyColorManager.get().setNight(rgb, alpha);
        src.sendSuccess(() -> Component.literal("Sky tint updated."), true);
        return 1;
    }

    private static int clearSkyGlobal(CommandSourceStack src){
        org.lupz.doomsdayessentials.utils.SkyColorManager.get().clear();
        src.sendSuccess(() -> Component.literal("Sky tint cleared."), true);
        return 1;
    }

    private static int parseColor(String arg, CommandSourceStack src){
        String colorStr = arg;
        int rgb;
        if(colorStr.equalsIgnoreCase("clear")){
            rgb = 0;
        }else{
            if(colorStr.startsWith("#")) colorStr = colorStr.substring(1);
            try{
                rgb = Integer.parseInt(colorStr,16) & 0xFFFFFF;
            }catch(NumberFormatException e){
                src.sendFailure(Component.literal("Invalid hex color."));
                return -1;
            }
        }
        return rgb;
    }

    // ------------------------------------------------------------------
    // Extra font helpers continue below
    // ------------------------------------------------------------------

    // =====================================================================
    // Suggestions helpers
    // =====================================================================

    private static class FontSuggestion {
        static CompletableFuture<Suggestions> suggest(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder b){
            discoverFonts().forEach(b::suggest);
            return b.buildFuture();
        }
    }

    private static class StyleSuggestion {
        private static final List<String> STYLES = List.of("normal","bold","italic","bolditalic");
        static CompletableFuture<Suggestions> suggest(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder b){
            STYLES.forEach(b::suggest);
            return b.buildFuture();
        }
    }

    private static class ColorSuggestion {
        private static final List<String> COLORS;
        static {
            COLORS = new ArrayList<>();
            for(ChatFormatting cf: ChatFormatting.values()){
                if(cf.isColor()) COLORS.add(cf.getName().toLowerCase());
            }
        }
        static CompletableFuture<Suggestions> suggest(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder b){
            COLORS.forEach(b::suggest);
            return b.buildFuture();
        }
    }

    private static Set<String> discoverFonts(){
        Set<String> set = new HashSet<>();
        // 1) Dev environment: scan source resources and build resources
        List<String> resourceDirs = List.of(
                "src/main/resources/assets/"+EssentialsMod.MOD_ID+"/font",
                "build/resources/main/assets/"+EssentialsMod.MOD_ID+"/font"
        );
        for(String dirStr: resourceDirs){
            try{
                Path root = Paths.get(dirStr);
                if(Files.exists(root)){
                    try(Stream<Path> s = Files.list(root)){
                        s.filter(p->p.toString().endsWith(".json")).forEach(p->{
                            String name = p.getFileName().toString().replace(".json","");
                            addFontNameVariants(set, name);
                        });
                    }
                }
            }catch(Exception ignored){}
        }

        // 2) Runtime (jar) environment: scan entries inside our jar
        try{
            java.security.CodeSource cs = SfxCommand.class.getProtectionDomain().getCodeSource();
            if(cs != null){
                java.net.URL url = cs.getLocation();
                java.nio.file.Path path = Paths.get(url.toURI());
                if(path.toString().endsWith(".jar") && Files.exists(path)){
                    try(java.util.jar.JarFile jar = new java.util.jar.JarFile(path.toFile())){
                        java.util.Enumeration<java.util.jar.JarEntry> en = jar.entries();
                        String prefix = "assets/"+EssentialsMod.MOD_ID+"/font/";
                        while(en.hasMoreElements()){
                            java.util.jar.JarEntry e = en.nextElement();
                            String name = e.getName();
                            if(name.startsWith(prefix) && name.endsWith(".json")){
                                String fname = name.substring(prefix.length(), name.length()-5); // strip prefix and .json
                                addFontNameVariants(set, fname);
                            }
                        }
                    }
                }
            }
        }catch(Exception ignored){}

        if(set.isEmpty()) set.add("knightvision");
        return set;
    }

    private static void addFontNameVariants(Set<String> set, String name){
        set.add(name);
        set.add(EssentialsMod.MOD_ID+":"+name);
        // also add capitalized variant for better tab matching on some clients
        String cap = Character.toUpperCase(name.charAt(0))+name.substring(1);
        set.add(cap);
        set.add(EssentialsMod.MOD_ID+":"+cap);
    }

    // ---------------------------------------------------------------------
    // Font title helper
    // ---------------------------------------------------------------------

    private int sendFontTitle(CommandSourceStack src, java.util.Collection<ServerPlayer> targets, String fontId, String styleKey, String colorKey, String text){
        // Resolve font id
        if(!fontId.contains(":")) fontId = EssentialsMod.MOD_ID+":"+fontId;
        final String fontIdFinal = fontId; // capture for lambda
        ResourceLocation fontRL = ResourceLocation.tryParse(fontIdFinal);

        Style style = Style.EMPTY.withFont(fontRL);

        // apply bold/italic
        switch(styleKey.toLowerCase()){
            case "bold": style = style.withBold(true); break;
            case "italic": style = style.withItalic(true); break;
            case "bolditalic": style = style.withBold(true).withItalic(true); break;
            default: break; // normal
        }

        // color parsing
        if(colorKey != null && !colorKey.equalsIgnoreCase("white")){
            TextColor tc = null;
            if(colorKey.startsWith("#")){
                try{ tc = TextColor.fromRgb(Integer.parseInt(colorKey.substring(1),16)); }catch(NumberFormatException ignored){}
            }else{
                ChatFormatting cf = ChatFormatting.getByName(colorKey.toUpperCase());
                if(cf!=null && cf.isColor()) tc = TextColor.fromLegacyFormat(cf);
            }
            if(tc!=null) style = style.withColor(tc);
        }

        MutableComponent comp = Component.literal(text).withStyle(style);
        String json = net.minecraft.network.chat.Component.Serializer.toJson(comp);

        for(ServerPlayer t: targets){
            String cmd = "title "+t.getGameProfile().getName()+" title "+json;
            src.getServer().getCommands().performPrefixedCommand(src, cmd);
        }
        src.sendSuccess(() -> Component.literal("Title sent to "+targets.size()+" player(s)."), true);
        return targets.size();
    }

    // runPhotonFx helper removed – Photon dependency dropped.
} 