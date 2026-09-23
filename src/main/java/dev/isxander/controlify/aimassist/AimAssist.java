/*
 * Copyright (C) 2026 isXander
 * This file is part of Controlify.
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package dev.isxander.controlify.aimassist;

import dev.isxander.controlify.Controlify;
import dev.isxander.controlify.config.settings.AimAssistSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector2d;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Controller aim assist. Slows the look input down while the crosshair is near a valid target,
 * which is what fixes most controller misses: overshooting, rather than being wildly off.
 * <p>
 * It only ever scales the input the player is already giving. It never moves the camera on its
 * own, never widens a hitbox and never changes where an attack lands, so the player's own aim
 * still decides the outcome.
 */
public final class AimAssist {
	/** Look input multiplier at the exact centre of the cone. Lower pulls the input down harder. */
	private static final double MELEE_SLOWDOWN_LOW = 0.60;
	private static final double MELEE_SLOWDOWN_MEDIUM = 0.50;
	private static final double MELEE_SLOWDOWN_HIGH = 0.40;

	/** Bow slowdown is gentler at every level: drawing a bow is already a slow, fine adjustment. */
	private static final double BOW_SLOWDOWN_LOW = 0.64;
	private static final double BOW_SLOWDOWN_MEDIUM = 0.54;
	private static final double BOW_SLOWDOWN_HIGH = 0.44;

	private static final double MELEE_CONE_LOW = 3.0;
	private static final double MELEE_CONE_MEDIUM = 6.0;
	private static final double MELEE_CONE_HIGH = 10.0;

	private static final double BOW_CONE_LOW = 1.5;
	private static final double BOW_CONE_MEDIUM = 3.0;
	private static final double BOW_CONE_HIGH = 5.0;

	private static final double MELEE_DISTANCE_LOW = 8.0;
	private static final double MELEE_DISTANCE_MEDIUM = 16.0;
	private static final double MELEE_DISTANCE_HIGH = 24.0;

	private static final double BOW_DISTANCE_LOW = 20.0;
	private static final double BOW_DISTANCE_MEDIUM = 35.0;
	private static final double BOW_DISTANCE_HIGH = 50.0;

	/** Strongest pull towards the target, in degrees per tick, while the stick is being pushed. */
	private static final double MELEE_PULL_LOW = 0.70;
	private static final double MELEE_PULL_MEDIUM = 0.97;
	private static final double MELEE_PULL_HIGH = 1.30;

	private static final double BOW_PULL_LOW = 0.45;
	private static final double BOW_PULL_MEDIUM = 0.70;
	private static final double BOW_PULL_HIGH = 1.00;

	/** Fraction of the remaining angle the pull tries to close each tick, before the cap applies. */
	private static final double PULL_GAIN = 0.45;

	/** Look impulse (degrees this tick) at which the stick alone drives the pull at full strength. */
	private static final double FULL_PULL_INPUT = 0.25;

	/**
	 * Fraction of the shortfall the assist covers for you when a target is sliding across the view
	 * faster than the pull's cap can follow. It is not a second pull: where the cap already keeps
	 * up there is no shortfall and this does nothing at all.
	 */
	private static final double MELEE_FOLLOW_LOW = 0.50;
	private static final double MELEE_FOLLOW_MEDIUM = 0.70;
	private static final double MELEE_FOLLOW_HIGH = 0.90;

	/**
	 * Bows deliberately get none of it. They tested well as they are, they are rarely fired at the
	 * ranges where the shortfall appears, and a drawn bow wants the player's own fine control.
	 */
	private static final double BOW_FOLLOW = 0.0;

	/** Ceiling on that, so sprinting past a mob at arm's length cannot whip the camera round. */
	private static final double MAX_FOLLOW_RATE = 6.0;

	/**
	 * Rate at which a target sliding across the view, in degrees per tick, drives the pull at full
	 * strength on its own. Any deliberate strafe past a mob clears this comfortably, so tracking
	 * works with the look stick untouched, while a standstill still leaves the camera alone.
	 */
	private static final double FULL_PULL_SWING = 0.5;

	/**
	 * Where on a mob the assist aims, as a fraction of its eye height. Aiming at the centre of the
	 * bounding box put the crosshair around a zombie's waist, and dragged it back down whenever the
	 * player was already lined up on the head. Measuring from the eyes rather than the box keeps
	 * this at roughly the same spot on mobs of very different proportions.
	 */
	private static final double AIM_HEIGHT_FACTOR = 0.80;

	/** How far outside the cone a locked bow target may drift before it is given up. */
	private static final double BOW_LOCK_TOLERANCE = 1.5;

	/** Entity hitboxes are grown by this much when testing whether the crosshair is actually on one. */
	private static final double RAY_HITBOX_PADDING = 0.3;

	/** What the assist did on the most recent look tick, for the Dev Functions readout. */
	public record Debug(@Nullable Entity target, double angle, double multiplier, boolean bowMode, boolean active,
	                    Counts counts, AimAssistTargets targets, double pull) {
		public static final Debug INACTIVE = new Debug(null, 0, 1, false, false, new Counts(), AimAssistTargets.HOSTILE, 0);
	}

	/** Where candidates were lost during the last search, so a failure can be traced to one stage. */
	public static final class Counts {
		public int nearby;
		public int eligible;
		public int tooFar;
		public int outsideCone;
		public int losBlocked;
		public double bestAngle = -1;
	}

	private static Debug lastDebug = Debug.INACTIVE;
	private static @Nullable Entity lockedBowTarget;
	private static Counts lastCounts = new Counts();

	private AimAssist() {
	}

	public static Debug debug() {
		return lastDebug;
	}

	/**
	 * Scales {@code lookImpulse} down while the crosshair is near a target. Called from the look
	 * handler before Controlify's look event fires, so mods listening to that event (Zoomify's
	 * zoom sensitivity, for one) still scale the result as they always have.
	 */
	public static void apply(Vector2d lookImpulse) {
		AimAssistSettings settings = Controlify.instance().config().getSettings().aimAssistSettings();
		LocalPlayer player = Minecraft.getInstance().player;

		if (player == null || settings.mode == AimAssistMode.OFF || !settings.mode.canAimAssist()) {
			lockedBowTarget = null;
			lastDebug = Debug.INACTIVE;
			return;
		}

		boolean bowMode = isAimingProjectile(player);
		if (!bowMode) {
			lockedBowTarget = null;
		}

		double cone = bowMode ? bowCone(settings.bowCone) : meleeCone(settings.meleeCone);
		double range = bowMode ? bowDistance(settings.bowDistance) : meleeDistance(settings.meleeDistance);

		Entity target = findTarget(player, settings, cone, range, bowMode);
		if (target == null) {
			lastDebug = new Debug(null, 0, 1, bowMode, true, lastCounts, settings.targets, 0);
			return;
		}
		if (bowMode) {
			lockedBowTarget = target;
		}

		double angle = angularOffset(player, target, range);
		// 1 while the crosshair is on the target, easing to 0 at the edge of the cone. The square
		// root keeps the assist meaningful across most of the cone instead of only dead centre.
		double proximity = Math.sqrt(Mth.clamp(1 - (angle / cone), 0, 1));

		double slowdown = bowMode ? bowSlowdown(settings.bowStrength) : meleeSlowdown(settings.meleeStrength);
		double multiplier = 1 - (1 - slowdown) * proximity;

		Vec3 toTarget = aimPoint(target).subtract(player.getEyePosition());

		// Where the target sits next tick if everyone keeps moving as they are. The change in bearing
		// is how fast it is sliding across the view, which is the rate the camera has to match just
		// to stay pointed at it.
		Vec3 nextToTarget = toTarget.add(tickMotion(target)).subtract(tickMotion(player));
		double yawDrift = Mth.wrapDegrees(yawOf(nextToTarget) - yawOf(toTarget));
		double pitchDrift = pitchOf(nextToTarget) - pitchOf(toTarget);
		double swing = Math.hypot(yawDrift, pitchDrift);

		// Magnetism only ever helps a turn that is already happening, and it can be happening for
		// two reasons: the stick is being pushed, or the target is sliding across the view because
		// the player or the mob is moving. Strafing past a zombie is the second kind, which is why
		// the stick alone is not enough to gate this. Standing still with nothing moving is neither,
		// and contributes nothing, so the camera never drifts on its own.
		double stickStrength = Mth.clamp(lookImpulse.length() / FULL_PULL_INPUT, 0, 1);
		double trackingStrength = Mth.clamp(swing / FULL_PULL_SWING, 0, 1);
		double inputStrength = Math.max(stickStrength, trackingStrength);
		double pullCap = bowMode ? bowPull(settings.bowStrength) : meleePull(settings.meleeStrength);
		double pullScale = proximity * inputStrength;

		double yawError = Mth.wrapDegrees(yawOf(toTarget) - player.getYRot());
		double pitchError = pitchOf(toTarget) - player.getXRot();

		// Cap the combined pull rather than each axis: capping them separately let a diagonal
		// pull reach 1.41x the configured cap, which is why High felt heavier than its number.
		double yawPull = yawError * PULL_GAIN;
		double pitchPull = pitchError * PULL_GAIN;
		double pullLength = Math.hypot(yawPull, pitchPull);
		if (pullLength > pullCap && pullLength > 0) {
			double scale = pullCap / pullLength;
			yawPull *= scale;
			pitchPull *= scale;
		}
		yawPull *= pullScale;
		pitchPull *= pullScale;

		// The pull's cap is a fixed number of degrees per tick, but the rate a mob slides across the
		// view goes up as you close in: at two blocks a slow sidestep moves it faster than any of the
		// three levels can follow, which is why the crosshair felt anchored at range and loose in a
		// mob's face. Make up the part of that the cap cannot reach. Further out there is no
		// shortfall and this is exactly zero, so the feel at range is untouched.
		double follow = bowMode ? BOW_FOLLOW : meleeFollow(settings.meleeStrength);
		double followRate = Math.min(Math.max(0, swing - pullCap) * follow, MAX_FOLLOW_RATE) * proximity;
		double followYaw = 0;
		double followPitch = 0;
		if (followRate > 0 && swing > 1.0e-4) {
			followYaw = yawDrift / swing * followRate;
			followPitch = pitchDrift / swing * followRate;
		}

		lookImpulse.mul(multiplier);
		lookImpulse.add(yawPull + followYaw, pitchPull + followPitch);

		lastDebug = new Debug(target, angle, multiplier, bowMode, true, lastCounts, settings.targets,
				Math.hypot(yawPull + followYaw, pitchPull + followPitch));
	}

	/** Bearing of a direction, in Minecraft's yaw convention. */
	private static double yawOf(Vec3 direction) {
		return Math.toDegrees(Math.atan2(-direction.x, direction.z));
	}

	/** Elevation of a direction, in Minecraft's pitch convention. */
	private static double pitchOf(Vec3 direction) {
		double horizontal = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
		return Math.toDegrees(-Math.atan2(direction.y, horizontal));
	}

	/**
	 * How far something actually moved over the last tick. Taken from the positions rather than
	 * {@code getDeltaMovement}, which the client only refreshes for other entities when the server
	 * sends a velocity packet, and so reads zero for most of a walking mob's life.
	 */
	private static Vec3 tickMotion(Entity entity) {
		return new Vec3(entity.getX() - entity.xOld, entity.getY() - entity.yOld, entity.getZ() - entity.zOld);
	}

	/**
	 * True while the player is lining up a projectile shot: drawing a bow, or holding a loaded
	 * crossbow. Charging a crossbow is a reload rather than a shot, so it keeps melee assist.
	 */
	private static boolean isAimingProjectile(LocalPlayer player) {
		if (player.isUsingItem()) {
			boolean drawingBow = switch (player.getUseItem().getUseAnimation()) {
				case BOW -> true;
				default -> false;
			};
			if (drawingBow) {
				return true;
			}
			boolean chargingCrossbow = switch (player.getUseItem().getUseAnimation()) {
				case CROSSBOW -> true;
				default -> false;
			};
			if (chargingCrossbow) {
				return false;
			}
		}
		return isLoadedCrossbow(player.getMainHandItem()) || isLoadedCrossbow(player.getOffhandItem());
	}

	private static boolean isLoadedCrossbow(ItemStack stack) {
		ChargedProjectiles charged = stack.get(DataComponents.CHARGED_PROJECTILES);
		return charged != null && !charged.isEmpty();
	}

	private static @Nullable Entity findTarget(LocalPlayer player, AimAssistSettings settings, double cone, double range, boolean bowMode) {
		// A target chosen while drawing stays chosen, so a mob wandering across the view can't
		// steal the assist halfway through a shot.
		if (bowMode && lockedBowTarget != null
				&& isEligible(player, lockedBowTarget, settings)
				&& angleTo(player, lockedBowTarget) <= cone * BOW_LOCK_TOLERANCE
				&& player.hasLineOfSight(lockedBowTarget)) {
			return lockedBowTarget;
		}

		Vec3 eye = player.getEyePosition();
		Vec3 view = player.getViewVector(1.0f);
		AABB searchBox = player.getBoundingBox().inflate(range);
		Counts counts = new Counts();
		lastCounts = counts;
		counts.nearby = player.level().getEntities(player, searchBox, entity -> true).size();
		List<Entity> candidates = player.level().getEntities(player, searchBox, entity -> isEligible(player, entity, settings));
		counts.eligible = candidates.size();

		Entity bestByAngle = null;
		double bestAngle = Double.MAX_VALUE;
		Entity underCrosshair = null;
		double underCrosshairDistance = Double.MAX_VALUE;

		for (Entity candidate : candidates) {
			Vec3 point = aimPoint(candidate);
			double distance = eye.distanceTo(point);
			if (distance > range) {
				counts.tooFar++;
				continue;
			}

			double angle = angularOffset(player, candidate, range);
			if (counts.bestAngle < 0 || angle < counts.bestAngle) {
				counts.bestAngle = angle;
			}
			if (angle > cone) {
				counts.outsideCone++;
				continue;
			}
			if (!player.hasLineOfSight(candidate)) {
				counts.losBlocked++;
				continue;
			}

			// Whether the look ray actually passes through this hitbox. For bows this beats
			// "nearest to the crosshair", so a zombie at your elbow can't outrank the skeleton
			// you are lined up on just by being closer in angle.
			boolean onTarget = candidate.getBoundingBox()
					.inflate(RAY_HITBOX_PADDING)
					.clip(eye, eye.add(view.scale(range)))
					.isPresent();
			if (onTarget && distance < underCrosshairDistance) {
				underCrosshair = candidate;
				underCrosshairDistance = distance;
			}

			if (angle < bestAngle) {
				bestAngle = angle;
				bestByAngle = candidate;
			}
		}

		if (bowMode && underCrosshair != null) {
			return underCrosshair;
		}
		return bestByAngle;
	}

	private static boolean isEligible(LocalPlayer player, Entity entity, AimAssistSettings settings) {
		if (entity == player || entity == player.getVehicle() || !entity.isAlive()) {
			return false;
		}
		if (entity.isSpectator() || entity.isInvisibleTo(player)) {
			return false;
		}
		// Players are deliberately never targeted, and armour stands, boats and the like only
		// count if the player has explicitly listed them.
		if (entity instanceof Player) {
			return false;
		}

		boolean isMob = entity instanceof LivingEntity && !(entity instanceof ArmorStand);

		return switch (settings.targets) {
			case HOSTILE -> isMob && (entity.getType().getCategory() == MobCategory.MONSTER || isAngry(entity));
			case ALL_MOBS -> isMob;
			case CUSTOM -> settings.customTargets.contains(typeId(entity));
		};
	}

	/**
	 * Whether a normally peaceful mob is currently fighting, so an angry wolf pack or a swarm of
	 * bees counts as hostile. The client only sees the mob's synced aggressive flag, not its
	 * actual AI target, so this catches the mobs that bother to sync it and no more.
	 */
	private static boolean isAngry(Entity entity) {
		return entity instanceof Mob mob && mob.isAggressive();
	}

	private static String typeId(Entity entity) {
		return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
	}

	/** The point the magnetism pulls towards: just below the head, around the top of the chest. */
	private static Vec3 aimPoint(Entity entity) {
		AABB box = entity.getBoundingBox();
		// Clamped into the hitbox, since a few entities sit their eyes at or above their own box.
		double y = Mth.clamp(box.minY + entity.getEyeHeight() * AIM_HEIGHT_FACTOR, box.minY, box.maxY);
		return new Vec3((box.minX + box.maxX) / 2, y, (box.minZ + box.maxZ) / 2);
	}

	private static double angleTo(LocalPlayer player, Entity entity) {
		return angleBetween(player.getViewVector(1.0f), aimPoint(entity).subtract(player.getEyePosition()));
	}

	/**
	 * How far off the crosshair an entity is, measured to the nearest part of its hitbox rather
	 * than its centre. A zombie three blocks away is over a metre wide, so measuring to the centre
	 * reports several degrees off even when the crosshair is plainly on it.
	 *
	 * @return 0 when the look ray passes through the hitbox
	 */
	private static double angularOffset(LocalPlayer player, Entity entity, double range) {
		Vec3 eye = player.getEyePosition();
		Vec3 view = player.getViewVector(1.0f);
		AABB box = entity.getBoundingBox().inflate(RAY_HITBOX_PADDING);

		if (box.clip(eye, eye.add(view.scale(range))).isPresent()) {
			return 0;
		}

		double best = angleBetween(view, box.getCenter().subtract(eye));
		for (double x : new double[]{box.minX, box.maxX}) {
			for (double y : new double[]{box.minY, (box.minY + box.maxY) / 2, box.maxY}) {
				for (double z : new double[]{box.minZ, box.maxZ}) {
					best = Math.min(best, angleBetween(view, new Vec3(x, y, z).subtract(eye)));
				}
			}
		}
		return best;
	}

	/** Angle between two vectors, in degrees. */
	private static double angleBetween(Vec3 a, Vec3 b) {
		double lengths = a.length() * b.length();
		if (lengths == 0) {
			return 180;
		}
		return Math.toDegrees(Math.acos(Mth.clamp(a.dot(b) / lengths, -1, 1)));
	}

	private static double meleeSlowdown(AimAssistLevel level) {
		return switch (level) {
			case LOW -> MELEE_SLOWDOWN_LOW;
			case MEDIUM -> MELEE_SLOWDOWN_MEDIUM;
			case HIGH -> MELEE_SLOWDOWN_HIGH;
		};
	}

	private static double bowSlowdown(AimAssistLevel level) {
		return switch (level) {
			case LOW -> BOW_SLOWDOWN_LOW;
			case MEDIUM -> BOW_SLOWDOWN_MEDIUM;
			case HIGH -> BOW_SLOWDOWN_HIGH;
		};
	}

	private static double meleePull(AimAssistLevel level) {
		return switch (level) {
			case LOW -> MELEE_PULL_LOW;
			case MEDIUM -> MELEE_PULL_MEDIUM;
			case HIGH -> MELEE_PULL_HIGH;
		};
	}

	private static double bowPull(AimAssistLevel level) {
		return switch (level) {
			case LOW -> BOW_PULL_LOW;
			case MEDIUM -> BOW_PULL_MEDIUM;
			case HIGH -> BOW_PULL_HIGH;
		};
	}

	private static double meleeFollow(AimAssistLevel level) {
		return switch (level) {
			case LOW -> MELEE_FOLLOW_LOW;
			case MEDIUM -> MELEE_FOLLOW_MEDIUM;
			case HIGH -> MELEE_FOLLOW_HIGH;
		};
	}

	private static double meleeCone(AimAssistLevel level) {
		return switch (level) {
			case LOW -> MELEE_CONE_LOW;
			case MEDIUM -> MELEE_CONE_MEDIUM;
			case HIGH -> MELEE_CONE_HIGH;
		};
	}

	private static double bowCone(AimAssistLevel level) {
		return switch (level) {
			case LOW -> BOW_CONE_LOW;
			case MEDIUM -> BOW_CONE_MEDIUM;
			case HIGH -> BOW_CONE_HIGH;
		};
	}

	private static double meleeDistance(AimAssistLevel level) {
		return switch (level) {
			case LOW -> MELEE_DISTANCE_LOW;
			case MEDIUM -> MELEE_DISTANCE_MEDIUM;
			case HIGH -> MELEE_DISTANCE_HIGH;
		};
	}

	private static double bowDistance(AimAssistLevel level) {
		return switch (level) {
			case LOW -> BOW_DISTANCE_LOW;
			case MEDIUM -> BOW_DISTANCE_MEDIUM;
			case HIGH -> BOW_DISTANCE_HIGH;
		};
	}
}
