package com.are;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.mojang.brigadier.arguments.IntegerArgumentType;

import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Decides when and how the world tries to scare you. Gets worse the longer you survive. */
public final class FearDirector {
    private FearDirector() {}

    public static int stageOverride = -1;
    private static boolean wasNight = false;

    private record Task(int at, Runnable run) {}
    private record Opt(int w, Runnable run) {}

    private static final List<Task> TASKS = new ArrayList<>();
    private static final Map<UUID, Integer> NEXT = new HashMap<>();
    private static final Set<UUID> SLEEPING = new HashSet<>();

    /* ---------------------------------------------------------------- scheduling */

    public static void later(MinecraftServer s, int ticks, Runnable r) {
        TASKS.add(new Task(s.getTickCount() + ticks, r));
    }

    public static void onServerTick(ServerTickEvent.Post e) {
        MinecraftServer server = e.getServer();
        int now = server.getTickCount();
        List<Runnable> due = new ArrayList<>();
        TASKS.removeIf(task -> {
            if (task.at() <= now) {
                due.add(task.run());
                return true;
            }
            return false;
        });
        for (Runnable r : due) {
            try {
                r.run();
            } catch (Exception ex) {
                Are.LOGGER.warn("Are: scheduled task failed", ex);
            }
        }
        // count a survived night every dawn
        ServerLevel ow = server.overworld();
        long t = ow.getDayTime() % 24000L;
        boolean night = t >= 13000L && t < 23000L;
        if (wasNight && !night) {
            dawn(server);
        }
        wasNight = night;
    }

    private static void dawn(MinecraftServer server) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            CompoundTag d = p.getPersistentData();
            int n = d.getInt("are_nights") + 1;
            d.putInt("are_nights", n);
            if (n >= 10 && !d.getBoolean("are_finale")) {
                d.putBoolean("are_finale", true);
                title(p, "§a§lYOU SURVIVED", "§7Ten nights. ...Didn't you?", 120);
                Net.send(p, JumpscarePayload.PULSE);
                later(server, 160, () -> p.sendSystemMessage(Component.literal("§e" + p.getName().getString() + " left the game")));
                later(server, 220, () -> p.sendSystemMessage(Component.literal("§e" + p.getName().getString() + " joined the game")));
            } else if (n < 10) {
                p.sendSystemMessage(Component.literal("§7You survived night " + n + "."));
            }
        }
    }

    /* ---------------------------------------------------------------- helpers */

    static int stage(ServerLevel lvl) {
        if (stageOverride >= 0) return stageOverride;
        long day = lvl.getDayTime() / 24000L;
        return day >= 6 ? 3 : day >= 3 ? 2 : day >= 1 ? 1 : 0;
    }

    static boolean isNight(ServerLevel lvl) {
        long t = lvl.getDayTime() % 24000L;
        return t >= 13000L && t < 23000L;
    }

    public static void playTo(ServerPlayer p, SoundEvent s, double x, double y, double z, float vol, float pitch) {
        p.connection.send(new ClientboundSoundPacket(Holder.direct(s), SoundSource.AMBIENT, x, y, z, vol, pitch,
                p.level().getRandom().nextLong()));
    }

    static void title(ServerPlayer p, String main, String sub, int stay) {
        p.connection.send(new ClientboundSetTitlesAnimationPacket(10, stay, 10));
        p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(sub)));
        p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(main)));
    }

    static <T extends Entity> T spawn(EntityType<T> type, ServerLevel lvl, Vec3 pos) {
        T e = type.create(lvl);
        if (e == null) return null;
        e.moveTo(pos.x, pos.y, pos.z, lvl.random.nextFloat() * 360F, 0F);
        lvl.addFreshEntity(e);
        return e;
    }

    /* ---------------------------------------------------------------- player tick */

    public static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        if (!(p.level() instanceof ServerLevel lvl)) return;
        handleSleep(p, lvl);
        if (p.tickCount % 20 != 0 || p.isSpectator() || lvl.dimension() != Level.OVERWORLD) return;

        int now = lvl.getServer().getTickCount();
        if (now < NEXT.getOrDefault(p.getUUID(), 0)) return;
        int st = stage(lvl);
        boolean night = isNight(lvl);
        double[] base = {0.05, 0.08, 0.12, 0.18};
        double chance = base[st] * (night ? 1.8 : 1.0) * AreConfig.INTENSITY.get();
        if (lvl.random.nextDouble() > chance) return;
        runRandomEvent(p, lvl, st, night);
        NEXT.put(p.getUUID(), now + (int) ((260 - st * 40) / Math.max(0.2, AreConfig.INTENSITY.get())));
    }

    private static void runRandomEvent(ServerPlayer p, ServerLevel lvl, int st, boolean night) {
        List<Opt> o = new ArrayList<>();
        o.add(new Opt(30, () -> ambient(p, lvl)));
        o.add(new Opt(20, () -> footsteps(p, lvl)));
        o.add(new Opt(night ? 26 : 10, () -> peek(p, lvl)));
        if (st >= 1) {
            if (AreConfig.SNUFF_TORCHES.get()) o.add(new Opt(10, () -> snuff(p, lvl)));
            if (AreConfig.OPEN_DOORS.get()) o.add(new Opt(8, () -> doors(p, lvl)));
            if (AreConfig.FAKE_CHAT.get()) o.add(new Opt(12, () -> fakeChat(p, lvl)));
            o.add(new Opt(8, () -> Net.send(p, JumpscarePayload.GLITCH)));
            if (night) o.add(new Opt(12, () -> behindYou(p, lvl)));
        }
        if (st >= 2) {
            if (night) o.add(new Opt(16, () -> spawnHollow(p, lvl, st)));
            o.add(new Opt(8, () -> {
                playTo(p, ModSounds.HEARTBEAT.get(), p.getX(), p.getY(), p.getZ(), 1F, 1F);
                later(lvl.getServer(), 60, () -> Net.send(p, JumpscarePayload.FACE));
            }));
        }
        if (st >= 3) {
            o.add(new Opt(10, () -> {
                p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 140, 0, false, false));
                Net.send(p, JumpscarePayload.BLACKOUT);
            }));
            if (night) o.add(new Opt(14, () -> spawnHollow(p, lvl, st)));
        }
        int total = 0;
        for (Opt x : o) total += x.w();
        int roll = lvl.random.nextInt(total);
        for (Opt x : o) {
            roll -= x.w();
            if (roll < 0) {
                x.run().run();
                return;
            }
        }
    }

    /* ---------------------------------------------------------------- events */

    private static void ambient(ServerPlayer p, ServerLevel lvl) {
        SoundEvent[] pool = {ModSounds.WHISPER.get(), ModSounds.DRONE.get(), ModSounds.KNOCK.get(), ModSounds.HEARTBEAT.get()};
        SoundEvent s = pool[lvl.random.nextInt(pool.length)];
        double a = lvl.random.nextDouble() * Math.PI * 2;
        double d = 4 + lvl.random.nextDouble() * 8;
        playTo(p, s, p.getX() + Math.cos(a) * d, p.getY(), p.getZ() + Math.sin(a) * d, 1.2F, 0.8F + lvl.random.nextFloat() * 0.4F);
    }

    private static void footsteps(ServerPlayer p, ServerLevel lvl) {
        Vec3 look = p.getLookAngle();
        Vec3 back = new Vec3(-look.x, 0, -look.z).normalize();
        for (int i = 0; i < 7; i++) {
            final double d = 8.0 - i;
            later(lvl.getServer(), 8 + i * 10, () -> {
                if (p.isAlive() && !p.hasDisconnected()) {
                    playTo(p, ModSounds.STEP.get(), p.getX() + back.x * d, p.getY(), p.getZ() + back.z * d, 1.0F, 0.7F);
                }
            });
        }
    }

    private static void peek(ServerPlayer p, ServerLevel lvl) {
        if (!lvl.getEntitiesOfClass(WatcherEntity.class, p.getBoundingBox().inflate(70)).isEmpty()) return;
        Vec3 pos = findSpot(lvl, p, 22, 40, true);
        if (pos != null) spawn(ModEntities.WATCHER.get(), lvl, pos);
    }

    public static void spawnWatcherNear(ServerPlayer p, ServerLevel lvl) {
        Vec3 pos = findSpot(lvl, p, 18, 30, true);
        if (pos != null) spawn(ModEntities.WATCHER.get(), lvl, pos);
    }

    public static void spawnHollow(ServerPlayer p, ServerLevel lvl, int st) {
        int max = st >= 3 ? 3 : 1;
        if (lvl.getEntitiesOfClass(HollowEntity.class, p.getBoundingBox().inflate(90)).size() >= max) return;
        Vec3 pos = findSpot(lvl, p, 28, 42, false);
        if (pos == null) return;
        spawn(ModEntities.HOLLOW.get(), lvl, pos);
        playTo(p, ModSounds.SCREAM.get(), pos.x, pos.y, pos.z, 1.6F, 0.6F);
    }

    private static void behindYou(ServerPlayer p, ServerLevel lvl) {
        if (!lvl.getEntitiesOfClass(WatcherEntity.class, p.getBoundingBox().inflate(40)).isEmpty()) return;
        Vec3 look = p.getLookAngle();
        Vec3 back = new Vec3(-look.x, 0, -look.z).normalize();
        double x = p.getX() + back.x * 6;
        double z = p.getZ() + back.z * 6;
        int bx = Mth.floor(x);
        int bz = Mth.floor(z);
        if (!lvl.hasChunk(bx >> 4, bz >> 4)) return;
        int y = lvl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz);
        if (Math.abs(y - p.getY()) > 3) return;
        playTo(p, ModSounds.HEARTBEAT.get(), x, y, z, 1F, 1F);
        spawn(ModEntities.WATCHER.get(), lvl, new Vec3(x, y, z));
    }

    private static void snuff(ServerPlayer p, ServerLevel lvl) {
        BlockPos c = p.blockPosition();
        int done = 0;
        for (BlockPos pos : BlockPos.betweenClosed(c.offset(-9, -4, -9), c.offset(9, 4, 9))) {
            BlockState s = lvl.getBlockState(pos);
            if (s.is(Blocks.TORCH) || s.is(Blocks.WALL_TORCH) || s.is(Blocks.SOUL_TORCH) || s.is(Blocks.SOUL_WALL_TORCH)) {
                lvl.removeBlock(pos, false);
                lvl.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8, 0.1, 0.1, 0.1, 0.01);
                if (++done >= 3) break;
            }
        }
        if (done > 0) {
            playTo(p, ModSounds.WHISPER.get(), p.getX(), p.getY(), p.getZ(), 1.0F, 1.0F);
        }
    }

    private static void doors(ServerPlayer p, ServerLevel lvl) {
        BlockPos c = p.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(c.offset(-12, -3, -12), c.offset(12, 3, 12))) {
            BlockState s = lvl.getBlockState(pos);
            if (s.getBlock() instanceof DoorBlock door && s.is(BlockTags.WOODEN_DOORS) && !s.getValue(DoorBlock.OPEN)) {
                door.setOpen(null, lvl, s, pos, true);
                return;
            }
        }
    }

    private static void fakeChat(ServerPlayer p, ServerLevel lvl) {
        String n = p.getName().getString();
        String sys = AreConfig.SYSTEM_INFO.get() ? AreBook.systemUser() : n;
        String[] lines = {
                "<" + n + "> i'm right behind you",
                "<" + n + "> don't turn around",
                "<Are> hello, " + sys,
                "<???> " + n + ", look at me",
                "§e" + n + " left the game",
                "<" + n + "> why is it so dark"
        };
        String m = lines[lvl.random.nextInt(lines.length)];
        p.sendSystemMessage(Component.literal(m));
        if (m.contains("left the game")) {
            later(lvl.getServer(), 60, () -> p.sendSystemMessage(Component.literal("§e" + n + " joined the game")));
        }
    }

    /* ---------------------------------------------------------------- spots */

    private static Vec3 findSpot(ServerLevel lvl, ServerPlayer p, int min, int max, boolean cover) {
        RandomSource r = lvl.random;
        Vec3 fallback = null;
        for (int i = 0; i < 30; i++) {
            double a = r.nextDouble() * Math.PI * 2;
            double d = min + r.nextDouble() * (max - min);
            int x = Mth.floor(p.getX() + Math.cos(a) * d);
            int z = Mth.floor(p.getZ() + Math.sin(a) * d);
            if (!lvl.hasChunk(x >> 4, z >> 4)) continue;
            int y = lvl.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos ground = new BlockPos(x, y - 1, z);
            BlockState gs = lvl.getBlockState(ground);
            if (!gs.blocksMotion() || gs.is(BlockTags.LOGS) || gs.is(BlockTags.LEAVES)) continue;
            if (!lvl.getFluidState(ground.above()).isEmpty()) continue;
            if (Math.abs(y - p.getY()) > 12) continue;
            Vec3 spot = new Vec3(x + 0.5, y, z + 0.5);
            if (fallback == null) fallback = spot;
            if (!cover) return spot;
            Vec3 c = coverSpot(lvl, p, x, y, z);
            if (c != null) return c;
        }
        return fallback;
    }

    /** Looks for a tree trunk / rock near (x,y,z) and returns a spot just behind it, as seen from the player. */
    private static Vec3 coverSpot(ServerLevel lvl, ServerPlayer p, int x, int y, int z) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                m.set(x + dx, y + 1, z + dz);
                BlockState s = lvl.getBlockState(m);
                if (!(s.is(BlockTags.LOGS) || s.is(BlockTags.BASE_STONE_OVERWORLD))) continue;
                Vec3 away = new Vec3(m.getX() + 0.5 - p.getX(), 0, m.getZ() + 0.5 - p.getZ()).normalize();
                Vec3 side = new Vec3(-away.z, 0, away.x);
                double sx = m.getX() + 0.5 + away.x * 1.1 + side.x * 0.55;
                double sz = m.getZ() + 0.5 + away.z * 1.1 + side.z * 0.55;
                BlockPos sp = BlockPos.containing(sx, y, sz);
                if (lvl.getBlockState(sp).isAir() && lvl.getBlockState(sp.above()).isAir()
                        && lvl.getBlockState(sp.above(2)).isAir() && lvl.getBlockState(sp.below()).blocksMotion()) {
                    return new Vec3(sx, y, sz);
                }
            }
        }
        return null;
    }

    /* ---------------------------------------------------------------- sleeping */

    private static void handleSleep(ServerPlayer p, ServerLevel lvl) {
        boolean sleeping = p.isSleeping();
        UUID id = p.getUUID();
        if (sleeping && SLEEPING.add(id)) {
            dream(p, lvl);
        } else if (!sleeping && SLEEPING.remove(id)) {
            wake(p, lvl);
        }
    }

    private static void dream(ServerPlayer p, ServerLevel lvl) {
        MinecraftServer s = lvl.getServer();
        later(s, 5, () -> title(p, "§8You drift off to sleep...", "§7and start playing a game.", 30));
        later(s, 30, () -> title(p, "§7In the game, a player sleeps in a bed", "§8...just like you.", 30));
        later(s, 55, () -> title(p, "§cThe player on screen turns around", "§4it has your face.", 30));
        later(s, 80, () -> title(p, "§4Footsteps outside", "§7something is walking to your house.", 40));
        for (int i = 1; i <= 9; i++) {
            final double d = 20 - 2.0 * i;
            later(s, i * 10, () -> {
                if (p.isSleeping()) {
                    playTo(p, ModSounds.STEP.get(), p.getX() + d, p.getY(), p.getZ() + 0.5, 0.5F + 0.1F * (float) d / 4F, 0.8F);
                }
            });
        }
    }

    private static void wake(ServerPlayer p, ServerLevel lvl) {
        if (lvl.random.nextFloat() > 0.7F) return;
        Vec3 look = p.getLookAngle();
        spawn(ModEntities.WATCHER.get(), lvl, new Vec3(p.getX() + look.x * 3, p.getY(), p.getZ() + look.z * 3));
        title(p, "§4It watched you sleep", "§7it is still here.", 60);
        Net.send(p, JumpscarePayload.FACE);
    }

    /* ---------------------------------------------------------------- first join: the book */

    public static void giveBook(ServerPlayer p) {
        ItemStack book = AreBook.make(p);
        if (!p.getInventory().add(book)) {
            p.drop(book, false);
        }
    }

    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        CompoundTag data = p.getPersistentData();
        if (data.getBoolean("are_intro")) return;
        data.putBoolean("are_intro", true);
        giveBook(p);
        MinecraftServer s = p.getServer();
        if (s == null) return;
        String name = p.getName().getString();
        later(s, 100, () -> {
            title(p, "§4§lARE", "§7Hello, " + name + ".", 80);
            Net.send(p, JumpscarePayload.GLITCH);
        });
        later(s, 240, () -> p.sendSystemMessage(Component.literal("§8A book has appeared in your inventory. Read it.")));
    }

    /* ---------------------------------------------------------------- commands (testing) */

    public static void onCommands(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("are").requires(src -> src.hasPermission(2))
                .then(Commands.literal("scare").executes(c -> {
                    Net.send(c.getSource().getPlayerOrException(), JumpscarePayload.FACE);
                    return 1;
                }))
                .then(Commands.literal("glitch").executes(c -> {
                    Net.send(c.getSource().getPlayerOrException(), JumpscarePayload.GLITCH);
                    return 1;
                }))
                .then(Commands.literal("blackout").executes(c -> {
                    Net.send(c.getSource().getPlayerOrException(), JumpscarePayload.BLACKOUT);
                    return 1;
                }))
                .then(Commands.literal("watcher").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    spawnWatcherNear(p, p.serverLevel());
                    return 1;
                }))
                .then(Commands.literal("hollow").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    spawnHollow(p, p.serverLevel(), 3);
                    return 1;
                }))
                .then(Commands.literal("book").executes(c -> {
                    giveBook(c.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("stage").then(Commands.argument("n", IntegerArgumentType.integer(-1, 3)).executes(c -> {
                    stageOverride = IntegerArgumentType.getInteger(c, "n");
                    c.getSource().sendSuccess(() -> Component.literal("Are: fear stage override = " + stageOverride + " (-1 = automatic)"), false);
                    return 1;
                }))));
    }
}
