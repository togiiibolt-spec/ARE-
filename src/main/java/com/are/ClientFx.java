package com.are;

import java.util.Random;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/** Everything that happens on YOUR screen: jumpscares, static, fear vignette, glitchy main menu. */
public final class ClientFx {
    private ClientFx() {}

    private static final ResourceLocation STATIC = Are.id("textures/gui/static.png");
    private static final ResourceLocation VIGNETTE = Are.id("textures/gui/vignette.png");
    private static final ResourceLocation FACE = Are.id("textures/gui/face.png");
    private static final Random R = new Random();

    private static int kind = -1;
    private static int ticks = 0;
    private static int total = 1;
    private static int shake = 0;
    private static float fear = 0F;
    private static boolean titleSoundPlayed = false;

    /* ------------------------------------------------------------ packet from the server */

    public static void handle(JumpscarePayload p) {
        kind = p.kind();
        switch (kind) {
            case JumpscarePayload.GLITCH -> {
                total = 22;
                ticks = 22;
                shake = 12;
            }
            case JumpscarePayload.FACE -> {
                total = 34;
                ticks = 34;
                shake = 30;
                play(ModSounds.SCREAM.get(), 1.0F);
            }
            case JumpscarePayload.BLACKOUT -> {
                total = 70;
                ticks = 70;
                play(ModSounds.STATIC.get(), 0.6F);
            }
            default -> {
                total = 50;
                ticks = 50;
            }
        }
    }

    private static void play(SoundEvent s, float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(s, pitch, 1.0F));
    }

    /* ------------------------------------------------------------ ticking */

    public static void onTick(ClientTickEvent.Post e) {
        if (ticks > 0) {
            ticks--;
        } else {
            kind = -1;
        }
        if (shake > 0) shake--;

        Minecraft mc = Minecraft.getInstance();
        float target = 0F;
        if (mc.level != null && mc.player != null) {
            double best = 9999;
            for (Entity en : mc.level.entitiesForRendering()) {
                if (en instanceof WatcherEntity || en instanceof HollowEntity) {
                    best = Math.min(best, en.distanceTo(mc.player));
                }
            }
            if (best < 48) target = (float) (1.0 - best / 48.0);
        }
        fear += (target - fear) * 0.06F;
    }

    public static void onCamera(ViewportEvent.ComputeCameraAngles e) {
        if (shake > 0) {
            float s = shake * 0.07F;
            e.setYaw(e.getYaw() + (R.nextFloat() - 0.5F) * s * 2F);
            e.setPitch(e.getPitch() + (R.nextFloat() - 0.5F) * s * 2F);
            e.setRoll(e.getRoll() + (R.nextFloat() - 0.5F) * s);
        }
    }

    /* ------------------------------------------------------------ in-game overlay */

    public static void onGui(RenderGuiEvent.Post e) {
        GuiGraphics g = e.getGuiGraphics();
        int w = g.guiWidth();
        int h = g.guiHeight();

        if (fear > 0.02F) {
            float pulse = 0.75F + 0.25F * Mth.sin(System.currentTimeMillis() / 160.0F);
            tex(g, VIGNETTE, 0, 0, w, h, 256, 256, 0, 0, Math.min(1F, fear * 1.1F * pulse));
        }
        if (kind < 0 || ticks <= 0) return;
        float f = ticks / (float) total; // 1 -> 0

        switch (kind) {
            case JumpscarePayload.GLITCH -> {
                tex(g, STATIC, 0, 0, w, h, 128, 128, R.nextInt(64), R.nextInt(64), 0.4F * f);
                bars(g, w, h, 7);
            }
            case JumpscarePayload.FACE -> {
                if (f > 0.9F) g.fill(0, 0, w, h, 0x99FF0000);
                int a = (int) (255 * Math.min(1F, f * 2.2F));
                g.fill(0, 0, w, h, (a << 24));
                int size = h;
                int ox = (int) ((R.nextFloat() - 0.5F) * 16 * f);
                int oy = (int) ((R.nextFloat() - 0.5F) * 16 * f);
                tex(g, FACE, (w - size) / 2 + ox, oy, size, size, 256, 256, 0, 0, Math.min(1F, f * 2F));
                tex(g, STATIC, 0, 0, w, h, 128, 128, R.nextInt(64), R.nextInt(64), 0.25F * f);
            }
            case JumpscarePayload.BLACKOUT -> {
                float a = ((ticks / 3) % 2 == 0) ? 0.97F : 0.55F;
                if (ticks < 15) a *= ticks / 15F;
                g.fill(0, 0, w, h, ((int) (255 * a) << 24));
            }
            default -> {
                int a = (int) (110 * f * (0.6F + 0.4F * Mth.sin(ticks * 0.6F)));
                g.fill(0, 0, w, h, (a << 24) | 0xAA0000);
            }
        }
    }

    private static void bars(GuiGraphics g, int w, int h, int n) {
        for (int i = 0; i < n; i++) {
            int y = R.nextInt(h);
            int bh = 2 + R.nextInt(10);
            int x = R.nextInt(w / 2);
            g.fill(x, y, Math.min(w, x + w / 3 + R.nextInt(w / 2)), y + bh, R.nextBoolean() ? 0xAAFF0000 : 0xCC000000);
        }
    }

    /** Draws a region (u,v,uw=vh... of texW x texH) stretched to a w x h rectangle with transparency. */
    private static void tex(GuiGraphics g, ResourceLocation t, int x, int y, int w, int h, int texW, int texH, int u, int v, float alpha) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.setColor(1F, 1F, 1F, Mth.clamp(alpha, 0F, 1F));
        int uw = texW - u;
        int vh = texH - v;
        g.blit(t, x, y, w, h, (float) u, (float) v, uw, vh, texW, texH);
        g.setColor(1F, 1F, 1F, 1F);
        RenderSystem.disableBlend();
    }

    /* ------------------------------------------------------------ main menu */

    public static void onScreenOpening(ScreenEvent.Opening e) {
        if (e.getNewScreen() instanceof TitleScreen && !titleSoundPlayed) {
            titleSoundPlayed = true;
            play(ModSounds.DRONE.get(), 0.8F);
        }
    }

    public static void onTitleRender(ScreenEvent.Render.Post e) {
        if (!(e.getScreen() instanceof TitleScreen)) return;
        GuiGraphics g = e.getGuiGraphics();
        int w = g.guiWidth();
        int h = g.guiHeight();
        long now = System.currentTimeMillis();
        Random r = new Random(now / 110L);          // changes ~9x per second => stuttery glitch

        tex(g, VIGNETTE, 0, 0, w, h, 256, 256, 0, 0, 0.55F);

        int n = 3 + r.nextInt(7);
        for (int i = 0; i < n; i++) {
            int y = r.nextInt(h);
            int bh = 1 + r.nextInt(8);
            int x = r.nextInt(w);
            int len = 30 + r.nextInt(Math.max(1, w / 2));
            g.fill(x, y, Math.min(w, x + len), y + bh, r.nextBoolean() ? 0xAAB00000 : 0xBB000000);
        }
        if (r.nextInt(4) == 0) {                      // eyes blinking in the dark
            int ex = r.nextInt(w);
            int ey = r.nextInt(h);
            g.fill(ex, ey, ex + 3, ey + 2, 0xFFFF0000);
            g.fill(ex + 8, ey, ex + 11, ey + 2, 0xFFFF0000);
        }
        if (r.nextInt(30) == 0) {
            tex(g, STATIC, 0, 0, w, h, 128, 128, r.nextInt(64), r.nextInt(64), 0.18F);
        }

        Font font = Minecraft.getInstance().font;
        PoseStack ps = g.pose();
        ps.pushPose();
        ps.translate(w - 84, h - 62, 0);
        ps.scale(4F, 4F, 1F);
        int jx = (r.nextInt(5) == 0) ? r.nextInt(3) - 1 : 0;
        g.drawString(font, "ARE", jx + 1, 1, 0xFF330000, false);
        g.drawString(font, "ARE", jx, 0, 0xFFE00000, false);
        ps.popPose();
        String sub = (now / 1500L) % 4 == 3 ? "it is watching" : "survive";
        g.drawString(font, sub, w - 84, h - 18, 0xFF880000, false);
    }
}
