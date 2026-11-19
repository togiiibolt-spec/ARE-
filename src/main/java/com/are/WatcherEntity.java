package com.are;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * The Watcher: it never moves and never attacks. It stands at the edge of your vision
 * (often half hidden behind a tree) and stares at you. Look straight at it, or get close,
 * and it vanishes. Then it comes back somewhere else.
 */
public class WatcherEntity extends PathfinderMob {
    private int watched = 0;
    private int age = 0;

    public WatcherEntity(EntityType<? extends WatcherEntity> type, Level level) {
        super(type, level);
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.FOLLOW_RANGE, 64.0);
    }

    @Override
    protected void registerGoals() {
        // no goals: it only stands and stares
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurt(source, amount);
        }
        if (!this.level().isClientSide) {
            Player p = source.getEntity() instanceof Player pl ? pl : null;
            vanish(p, true);
        }
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        age++;
        Player p = this.level().getNearestPlayer(this, 80.0);
        if (p == null || age > 20 * 45) {
            vanish(null, false);
            return;
        }
        faceTowards(p);
        boolean seen = Look.isLookedAt(p, this, 0.965);
        watched = seen ? watched + 1 : Math.max(0, watched - 1);
        if (age > 15 && (this.distanceTo(p) < 7.0F || watched > 28)) {
            vanish(p, true);
        }
    }

    private void faceTowards(Entity t) {
        double dx = t.getX() - getX();
        double dz = t.getZ() - getZ();
        float yaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
        this.setYRot(yaw);
        this.setYBodyRot(yaw);
        this.setYHeadRot(yaw);
        double dy = t.getEyeY() - getEyeY();
        double h = Math.sqrt(dx * dx + dz * dz);
        this.setXRot((float) (-(Mth.atan2(dy, h) * (180.0 / Math.PI))));
    }

    private void vanish(Player p, boolean scare) {
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.5, getZ(), 30, 0.3, 1.2, 0.3, 0.02);
            if (scare) {
                sl.playSound(null, getX(), getY(), getZ(), ModSounds.STATIC.get(), SoundSource.HOSTILE, 1.0F,
                        0.9F + this.random.nextFloat() * 0.2F);
                if (p instanceof ServerPlayer sp) {
                    float d = this.distanceTo(sp);
                    if (d < 10.0F && this.random.nextInt(3) == 0) {
                        Net.send(sp, JumpscarePayload.FACE);
                    } else if (d < 26.0F) {
                        Net.send(sp, JumpscarePayload.GLITCH);
                    }
                }
            }
        }
        this.discard();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.WHISPER.get();
    }
}
