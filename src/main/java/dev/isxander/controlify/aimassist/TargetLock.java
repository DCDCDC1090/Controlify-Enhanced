/*
 * Copyright (C) 2026 isXander
 * This file is part of Controlify.
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package dev.isxander.controlify.aimassist;

import dev.isxander.controlify.Controlify;
import dev.isxander.controlify.api.bind.InputBinding;
import dev.isxander.controlify.config.settings.AimAssistSettings;
import dev.isxander.controlify.config.settings.TargetLockSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Holds one mob as the player's target until they say otherwise. Aim assist on its own picks
 * whatever is nearest the crosshair every tick, which is the right behaviour when you're swinging
 * at whatever wanders in front of you and the wrong one when you're fighting a particular thing in
 * a crowd. A lock settles that argument.
 * <p>
 * Nothing here moves the camera. The lock only decides <em>which</em> mob aim assist helps with,
 * so it changes nothing about how the player's look input reaches the server.
 */
public final class TargetLock {
	/** How long the bind has to be held to drop the lock rather than cycle. */
	private static final int HOLD_TICKS = 6;

	/** Ceiling on how many candidates are kept, so cycling stays usable in a large fight. */
	private static final int MAX_CANDIDATES = 24;

	/**
	 * Vanilla flyers whose movement doesn't go through flying navigation or a flying move control,
	 * so the checks below wouldn't catch them. Modded flyers are caught by those checks instead,
	 * which is why this is a backstop rather than the whole answer.
	 */
	private static final Set<String> KNOWN_FLYERS = Set.of(
			"minecraft:ghast",
			"minecraft:happy_ghast",
			"minecraft:phantom",
			"minecraft:blaze",
			"minecraft:ender_dragon",
			"minecraft:wither",
			"minecraft:bat"
	);

	private static @Nullable Entity locked;

	/** Whether the drop timer is currently running, and when it started. */
	private static boolean outside;
	/**
	 * When the player first went outside the locked target's boundary. Monotonic rather than wall
	 * clock, and counted in real time rather than ticks, so it means fifteen seconds whatever the
	 * frame rate is doing and however the system clock moves.
	 */
	private static long outsideSinceMillis;

	private static int heldTicks;
	private static boolean holdConsumed;

	private static List<Entity> candidates = List.of();

	private TargetLock() {
	}

	/** The locked mob, or null when nothing is locked. */
	public static @Nullable Entity locked() {
		return locked;
	}

	/** The mobs the bind would cycle through, nearest first, for the compass bar to show. */
	public static List<Entity> candidates() {
		return candidates;
	}

	/**
	 * Seconds left before the lock is dropped for being out of range, or -1 while the player is
	 * inside the boundary and the timer isn't running.
	 */
	public static double dropCountdown() {
		if (locked == null || !outside) {
			return -1;
		}
		TargetLockSettings settings = settings();
		double elapsed = (millis() - outsideSinceMillis) / 1000.0;
		return Math.max(0, settings.dropSeconds - elapsed);
	}

	public static void clear() {
		locked = null;
		outside = false;
	}

	/** A plain-language account of the lock's state, for the Dev Functions readout. */
	public static String describe() {
		AimAssistSettings aimAssist = Controlify.instance().config().getSettings().aimAssistSettings();
		TargetLockSettings lock = aimAssist.targetLock;
		StringBuilder out = new StringBuilder();
		out.append(lock.enabled ? "on" : "OFF")
				.append(", ").append(lock.mode.getSerializedName())
				.append(", running=").append(active())
				.append(", ignore cone=").append(lock.overrideCone);
		if (locked == null) {
			out.append(" | NO LOCK HELD, ").append(candidates.size()).append(" candidates in range");
			return out.toString();
		}
		LocalPlayer player = Minecraft.getInstance().player;
		out.append(" | holding ").append(locked.getName().getString());
		if (player != null) {
			boolean flying = isFlying(locked);
			out.append(String.format(" at %.1fm (boundary %d%s, reset at %.1fm)",
					player.distanceTo(locked), lock.rangeFor(flying), flying ? " flying" : "",
					lock.resetRadiusFor(flying)));
		}
		double countdown = dropCountdown();
		out.append(countdown < 0 ? ", timer idle" : String.format(", DROPPING in %.1fs", countdown));
		return out.toString();
	}

	/** Monotonic milliseconds, so the drop timer can't be moved by the system clock changing. */
	private static long millis() {
		return System.nanoTime() / 1_000_000L;
	}

	private static TargetLockSettings settings() {
		return Controlify.instance().config().getSettings().aimAssistSettings().targetLock;
	}

	/**
	 * Whether the lock should be running at all. Marker only is exempt from where aim assist is
	 * allowed: it hands out no aim help of any kind, so it's an overlay rather than an advantage
	 * and there's no reason to make it wait on the aim assist setting.
	 */
	public static boolean active() {
		AimAssistSettings aimAssist = Controlify.instance().config().getSettings().aimAssistSettings();
		if (!aimAssist.targetLock.enabled) {
			return false;
		}
		return !aimAssist.targetLock.mode.assistsLockedTarget() || aimAssist.mode.canAimAssist();
	}

	/**
	 * Keeps the lock honest: drops it when the target dies, leaves the world, or has been left
	 * behind for long enough. Runs every input tick, including while a screen is open, so walking
	 * away with the inventory up still counts.
	 */
	public static void tick() {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null || !active()) {
			clear();
			candidates = List.of();
			return;
		}

		candidates = findCandidates(player);

		if (locked == null) {
			outside = false;
			return;
		}
		if (!locked.isAlive() || locked.isRemoved() || locked.level() != player.level()) {
			clear();
			return;
		}
		// Something may have climbed on since the lock was taken. Moving up to it here, on the
		// tick, is what keeps the marker off a mount that has just been mounted.
		Entity onTop = rider(locked);
		if (onTop != locked) {
			locked = onTop;
		}

		TargetLockSettings settings = settings();
		if (!settings.autoDrop) {
			outside = false;
			return;
		}

		// The boundary is measured from where the target is right now, every tick, so a mob walking
		// away moves the line with it rather than leaving the player standing outside a stale one.
		boolean flying = isFlying(locked);
		double distance = player.distanceTo(locked);

		if (!outside) {
			if (distance > settings.rangeFor(flying)) {
				outside = true;
				outsideSinceMillis = millis();
			}
			return;
		}

		// Coming back over the line isn't enough to reset the timer: the player has to get properly
		// back inside it. Otherwise a mob chasing them across the boundary restarts the count every
		// few steps and the lock never drops at all.
		if (distance <= settings.resetRadiusFor(flying)) {
			outside = false;
			return;
		}
		if (millis() - outsideSinceMillis >= settings.dropSeconds * 1000L) {
			clear();
		}
	}

	/**
	 * Tap to lock the nearest target or move to the next one, hold to let go. Cycling wraps, so
	 * holding is the way out rather than pressing through every mob in the room.
	 */
	public static void handleBind(InputBinding bind) {
		if (!active()) {
			heldTicks = 0;
			holdConsumed = false;
			return;
		}

		if (bind.digitalNow()) {
			heldTicks++;
			if (heldTicks >= HOLD_TICKS && !holdConsumed) {
				holdConsumed = true;
				clear();
			}
			return;
		}

		if (bind.justReleased()) {
			if (!holdConsumed) {
				cycle();
			}
			heldTicks = 0;
			holdConsumed = false;
		}
	}

	/** Moves to the next candidate, or locks the first one when nothing is locked yet. */
	private static void cycle() {
		if (candidates.isEmpty()) {
			clear();
			return;
		}
		int next = 0;
		if (locked != null) {
			int current = candidates.indexOf(locked);
			if (current >= 0) {
				next = (current + 1) % candidates.size();
			}
		}
		lockTo(candidates.get(next));
	}

	/** Locks a specific mob, used by the bind and by last hit mode. */
	public static void lockTo(@Nullable Entity entity) {
		Entity target = entity == null ? null : rider(entity);
		if (target == locked) {
			return;
		}
		locked = target;
		outside = false;
	}

	/**
	 * Whatever is sitting on top of this one, which is the thing actually worth tracking.
	 * <p>
	 * A zombie on a horse is two entities that move as one, and the two were separately lockable:
	 * lock the horse and the marker would sit at the horse's head height, which is inside the
	 * rider's chest. Redirecting here rather than at the point of drawing matters - it means the
	 * lock is on the rider from the moment it is taken, so nothing is ever drawn on the mount and
	 * then seen to jump off it.
	 * <p>
	 * Walks rather than taking one step, because mounts stack: a zombie on a horse in a boat is
	 * three deep. Counted rather than looped on a condition, since a passenger cycle would
	 * otherwise hang the game, and steered by whoever is driving where there is a choice, so a
	 * mount carrying two comes out as the one at the reins.
	 */
	private static Entity rider(Entity entity) {
		Entity top = entity;
		for (int step = 0; step < 8 && top.isVehicle(); step++) {
			Entity next = top.getControllingPassenger();
			if (next == null) {
				next = top.getFirstPassenger();
			}
			// Never redirect onto the player: their own mount stays the thing that was locked.
			if (next == null || next == top || next == Minecraft.getInstance().player) {
				break;
			}
			top = next;
		}
		return top;
	}

	/**
	 * The player hit something, by hand or by shot. In last hit mode that takes the lock, which is
	 * the whole point of the mode: you end up fighting whatever you're actually hitting without
	 * ever pressing the bind. It takes the lock whether or not one was held, so letting go with
	 * the bind and then swinging or shooting picks a new target straight away.
	 * <p>
	 * Melee comes in on the swing rather than on the damage the server reports back, so it lands
	 * immediately rather than a round trip later, and still works on the servers that never send
	 * a damage event at all. A shot has no such shortcut: an arrow in flight belongs to the
	 * server, so hits by bow, crossbow and trident arrive with the damage instead.
	 */
	public static void onPlayerAttack(@Nullable Entity victim) {
		if (!followsLastHit() || victim == null || !isLockable(victim)) {
			return;
		}
		lockTo(victim);
	}

	/**
	 * The player took a hit. A melee hit takes the lock, on the grounds that whatever got close
	 * enough to hit you is what you're fighting.
	 * <p>
	 * Being shot, though, is not a decision to start a fight. An incoming projectile only takes
	 * the lock when there is nothing else worth locking: anything else in reach, or anything
	 * already locked, outranks a shooter somewhere off in the trees. So an archer can only ever
	 * claim the lock by being the last thing standing, and holding the bind to let go is never
	 * undone by the next arrow.
	 */
	public static void onPlayerHurt(@Nullable Entity attacker, boolean projectile) {
		if (!followsLastHit() || attacker == null || !isLockable(attacker)) {
			return;
		}
		if (projectile && hasTargetOtherThan(attacker)) {
			return;
		}
		lockTo(attacker);
	}

	/** Whether anything but this entity is worth locking: something already held, or in reach. */
	private static boolean hasTargetOtherThan(Entity entity) {
		if (locked != null && locked != entity) {
			return true;
		}
		return candidates.stream().anyMatch(candidate -> candidate != entity);
	}

	private static boolean followsLastHit() {
		return active() && settings().mode.followsLastHit();
	}

	/**
	 * Whether this is something the bind would have offered anyway. Reusing the bind's own
	 * eligibility keeps the two routes to a lock agreeing: last hit mode can't hand you a target
	 * the bind would have refused, such as one beyond Locked Range or one the target filter
	 * excludes.
	 */
	private static boolean isLockable(Entity entity) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null || entity.isRemoved() || entity.level() != player.level()) {
			return false;
		}
		AimAssistSettings aimAssist = Controlify.instance().config().getSettings().aimAssistSettings();
		if (player.distanceTo(entity) > aimAssist.targetLock.lockedRangeBlocks) {
			return false;
		}
		return AimAssist.isEligible(player, entity, aimAssist);
	}

	/**
	 * Every mob the bind could lock, nearest first. Mobs on screen come first, because locking
	 * something behind you when there's a perfectly good target in front is never what was meant.
	 * If nothing at all is on screen the list falls back to everything in range, so the bind still
	 * does something when a mob is at your back.
	 */
	private static List<Entity> findCandidates(LocalPlayer player) {
		AimAssistSettings aimAssist = Controlify.instance().config().getSettings().aimAssistSettings();
		TargetLockSettings settings = aimAssist.targetLock;

		// How far the bind can reach is Locked Range: the distance a locked mob still gets help at,
		// so being able to lock exactly that far is the only version that makes sense. The Letting
		// Go ranges are about giving a target up, not about picking one, and using them here quietly
		// made a short drop boundary a short reach as well — worse, it did it even with dropping
		// switched off, where distance is meant to stop mattering at all.
		int searchRange = settings.lockedRangeBlocks;
		if (searchRange <= 0) {
			return List.of();
		}

		AABB searchBox = player.getBoundingBox().inflate(searchRange);
		List<Entity> onScreen = new ArrayList<>();
		List<Entity> offScreen = new ArrayList<>();

		for (Entity entity : player.level().getEntities(player, searchBox,
				entity -> AimAssist.isEligible(player, entity, aimAssist))) {
			if (player.distanceTo(entity) > searchRange) {
				continue;
			}
			// A mount is not offered separately from what is riding it. The rider is found by
			// this same scan if it is a target at all, so leaving the mount out is all it takes
			// for the pair to cycle as one thing rather than two in the same place.
			if (entity.isVehicle()) {
				continue;
			}
			(isOnScreen(player, entity) ? onScreen : offScreen).add(entity);
		}

		List<Entity> chosen = onScreen.isEmpty() ? offScreen : onScreen;
		chosen.sort(Comparator.comparingDouble(player::distanceTo));
		return chosen.size() > MAX_CANDIDATES ? List.copyOf(chosen.subList(0, MAX_CANDIDATES)) : List.copyOf(chosen);
	}

	/** Whether the mob is inside the player's actual field of view, horizontally and vertically. */
	private static boolean isOnScreen(LocalPlayer player, Entity entity) {
		Minecraft minecraft = Minecraft.getInstance();
		double verticalFov = minecraft.options.fov().get();
		double aspect = Math.max(1.0, (double) minecraft.getWindow().getWidth() / minecraft.getWindow().getHeight());
		double horizontalFov = 2 * Math.toDegrees(Math.atan(Math.tan(Math.toRadians(verticalFov / 2)) * aspect));

		Vec3 toTarget = entity.getBoundingBox().getCenter().subtract(player.getEyePosition());
		double horizontal = Math.sqrt(toTarget.x * toTarget.x + toTarget.z * toTarget.z);
		double yawOffset = Math.abs(Mth.wrapDegrees(Math.toDegrees(Math.atan2(-toTarget.x, toTarget.z)) - player.getYRot()));
		double pitchOffset = Math.abs(Math.toDegrees(-Math.atan2(toTarget.y, horizontal)) - player.getXRot());

		return yawOffset <= horizontalFov / 2 && pitchOffset <= verticalFov / 2;
	}

	/**
	 * Whether this mob flies, and so gets the longer boundary. A heuristic rather than a list:
	 * navigation and move control between them catch almost everything, vanilla or modded.
	 * <p>
	 * Worth knowing that a Breeze doesn't count. It jumps rather than flies, so it keeps the
	 * ground boundary.
	 */
	public static boolean isFlying(Entity entity) {
		if (entity instanceof Mob mob) {
			if (mob.getNavigation() instanceof FlyingPathNavigation
					|| mob.getMoveControl() instanceof FlyingMoveControl) {
				return true;
			}
		}
		if (KNOWN_FLYERS.contains(AimAssist.typeId(entity))) {
			return true;
		}
		return entity.isNoGravity() && !entity.onGround();
	}
}
