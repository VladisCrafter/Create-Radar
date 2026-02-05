package com.happysg.radar.compat.cbcwpf;

import com.happysg.radar.compat.Mods;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import rbasamoyai.createbigcannons.cannon_control.contraption.AbstractMountedCannonContraption;
import rbasamoyai.createbigcannons.cannons.autocannon.material.AutocannonMaterial;
import rbasamoyai.createbigcannons.munitions.config.components.BallisticPropertiesComponent;
import rbasamoyai.createbigcannons.cannons.ItemCannonBehavior;

import java.lang.reflect.Method;
import java.util.Deque;

public final class CBCWPFCompat {

    private CBCWPFCompat() {}

    private static final String SHUPAPIUM_BREECH_BE_CLASS = "net.ato.shupapium.blockentities.ShupapiumACBreechBlockEntity";
    private static final String SHUPAPIUM_CONTAINER_CLASS = "net.ato.shupapium.items.ShupapiumAmmoContainerItem";

    public static boolean isShupapiumAutocannon(AbstractMountedCannonContraption cannon) {
        return cannon != null
                && Mods.SHUPAPIUM.isLoaded()
                && cannon instanceof IShupapiumACContraptionAccess;
    }

    public static AutocannonMaterial resolveAutocannonMaterial(AbstractMountedCannonContraption cannon) {
        if (!isShupapiumAutocannon(cannon)) return null;
        try {
            return ((IShupapiumACContraptionAccess) cannon).getShupapiumMaterial();
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** Shupapium base speed comes from profile.getProjectileBaseSpeed() */
    public static float resolveProfileBaseSpeed(AbstractMountedCannonContraption cannon) {
        if (!isShupapiumAutocannon(cannon)) return Float.NaN;

        try {
            Object profile = ((IShupapiumACContraptionAccess) cannon).getProfile();
            if (profile == null) return Float.NaN;

            Method m = profile.getClass().getMethod("getProjectileBaseSpeed");
            return ((Number) m.invoke(profile)).floatValue();
        } catch (Throwable ignored) {
            return Float.NaN;
        }
    }

    public static BallisticPropertiesComponent getAutocannonBallistics(AbstractMountedCannonContraption cannon, Level level) {
        if (!isShupapiumAutocannon(cannon) || level == null) return null;

        final double FALLBACK_DRAG = 0.01;
        final boolean FALLBACK_QUAD = false;
        final double GRAV_ON = -0.05;

        try {
            Object breech = findShupapiumBreech(cannon);
            if (breech == null)
                return new BallisticPropertiesComponent(GRAV_ON, FALLBACK_DRAG, FALLBACK_QUAD, 0, 0, 0, 0);

            ItemStack round = peekNextRound(breech);
            if (round.isEmpty())
                return new BallisticPropertiesComponent(GRAV_ON, FALLBACK_DRAG, FALLBACK_QUAD, 0, 0, 0, 0);

            Object item = round.getItem();

            boolean affected;
            try {
                Method gravM = item.getClass().getMethod("projectileAffectedByWorldsGravity");
                affected = (boolean) gravM.invoke(item);
            } catch (Throwable ignored) {
                affected = true;
            }
            double gravity = affected ? GRAV_ON : 0.0;

            Method mMain = item.getClass().getMethod("getMainProperties", ItemStack.class);
            Object mainProps = mMain.invoke(item, round);
            if (mainProps == null)
                return new BallisticPropertiesComponent(gravity, FALLBACK_DRAG, FALLBACK_QUAD, 0, 0, 0, 0);

            Method mACProps = mainProps.getClass().getMethod("autocannonProperties");
            Object acProps = mACProps.invoke(mainProps);
            if (acProps == null)
                return new BallisticPropertiesComponent(gravity, FALLBACK_DRAG, FALLBACK_QUAD, 0, 0, 0, 0);

            Method mBall = acProps.getClass().getMethod("ballistics");
            Object bpObj = mBall.invoke(acProps);

            if (bpObj instanceof BallisticPropertiesComponent bp) {
                return new BallisticPropertiesComponent(gravity, bp.drag(), bp.isQuadraticDrag(), 0, 0, 0, 0);
            }

            return new BallisticPropertiesComponent(gravity, FALLBACK_DRAG, FALLBACK_QUAD, 0, 0, 0, 0);
        } catch (Throwable ignored) {
            return new BallisticPropertiesComponent(GRAV_ON, FALLBACK_DRAG, FALLBACK_QUAD, 0, 0, 0, 0);
        }
    }

    private static Object findShupapiumBreech(AbstractMountedCannonContraption cannon) {
        for (BlockEntity be : cannon.presentBlockEntities.values()) {
            if (be == null) continue;
            if (be.getClass().getName().equals(SHUPAPIUM_BREECH_BE_CLASS)) return be;
        }
        return null;
    }

    private static ItemStack peekNextRound(Object breechBe) throws Exception {
        @SuppressWarnings("unchecked")
        Deque<ItemStack> buf = (Deque<ItemStack>) breechBe.getClass().getMethod("getInputBuffer").invoke(breechBe);
        if (buf != null && !buf.isEmpty()) {
            ItemStack s = buf.peekFirst();
            return s != null ? s : ItemStack.EMPTY;
        }

        ItemStack mag = (ItemStack) breechBe.getClass().getMethod("getMagazine").invoke(breechBe);
        if (mag == null || mag.isEmpty()) return ItemStack.EMPTY;

        Class<?> container = Class.forName(SHUPAPIUM_CONTAINER_CLASS);
        Method getMain = container.getMethod("getMainAmmoStack", ItemStack.class);
        ItemStack main = (ItemStack) getMain.invoke(null, mag);

        return main != null ? main : ItemStack.EMPTY;
    }

    private static ItemStack peekLoadedOrNextRound(AbstractMountedCannonContraption cannon, Object breechBe) {
        ItemStack loaded = findLoadedRoundInBarrel(cannon);
        if (!loaded.isEmpty()) return loaded;

        try {
            return peekNextRound(breechBe);
        } catch (Throwable ignored) {
            return ItemStack.EMPTY;
        }
    }

    private static ItemStack findLoadedRoundInBarrel(AbstractMountedCannonContraption cannon) {
        if (cannon == null) return ItemStack.EMPTY;

        for (BlockEntity be : cannon.presentBlockEntities.values()) {
            if (be == null) continue;


            try {
                Object behavior = be.getClass().getMethod("cannonBehavior").invoke(be);
                if (behavior instanceof ItemCannonBehavior icb) {
                    ItemStack s = icb.getItem();
                    if (s != null && !s.isEmpty()) return s;
                }
            } catch (NoSuchMethodException ignored) {
            } catch (Throwable ignored) {
            }
        }

        return ItemStack.EMPTY;
    }
}