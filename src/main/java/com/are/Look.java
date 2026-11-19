package com.are;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** "Is this player staring at that entity?" helper. */
public final class Look {
    private Look() {}

    /** cone: 1.0 = exactly crosshair, 0.965 ~ 15 degrees, 0.9 ~ 25 degrees. */
    public static boolean isLookedAt(LivingEntity viewer, Entity target, double cone) {
        Vec3 view = viewer.getViewVector(1.0F).normalize();
        Vec3 to = new Vec3(target.getX() - viewer.getX(), target.getEyeY() - viewer.getEyeY(), target.getZ() - viewer.getZ());
        double len = to.length();
        if (len < 1.0E-4) {
            return true;
        }
        return view.dot(to.scale(1.0 / len)) > cone && viewer.hasLineOfSight(target);
    }
}
