package com.are;

import java.util.EnumSet;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * The Hollow: it hunts you at night, but it only moves while you are NOT looking at it.
 * Stare at it and it freezes. Look away and it is closer.
 */
public class HollowEntity extends Monster {
    private int lastScare = -1000;

    public HollowEntity(EntityType<? extends HollowEntity> type, Level level) {
        super(type, level);
        this.xpReward = 25;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 60.0)
                .add(Attributes.MOVEMENT_SPEED, 0.34)
                .add(Attributes.ATTACK_DAMAGE, 7.0)
                .add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new FreezeWhenWatchedGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2D, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 32.0F));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    public boolean isWatchedBy(Player p) {
        return Look.isLookedAt(p, this, 0.9);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.level().isDay() && this.random.nextInt(200) == 0) {
            if (this.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.5, getZ(), 40, 0.3, 1.2, 0.3, 0.02);
            }
            this.discard();
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof ServerPlayer sp) {
            sp.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 160, 0));
            if (this.tickCount - lastScare > 100) {
                lastScare = this.tickCount;
                Net.send(sp, JumpscarePayload.FACE);
            }
        }
        return hit;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.WHISPER.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.STATIC.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.SCREAM.get();
    }

    /** Stops all movement while any nearby player has it in view. */
    static class FreezeWhenWatchedGoal extends Goal {
        private final HollowEntity mob;

        FreezeWhenWatchedGoal(HollowEntity mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            Player p = mob.level().getNearestPlayer(mob, 48.0);
            return p != null && !p.isCreative() && !p.isSpectator() && mob.isWatchedBy(p);
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            mob.getNavigation().stop();
        }

        @Override
        public void tick() {
            mob.getNavigation().stop();
            mob.setDeltaMovement(0.0, mob.getDeltaMovement().y, 0.0);
        }
    }
}
