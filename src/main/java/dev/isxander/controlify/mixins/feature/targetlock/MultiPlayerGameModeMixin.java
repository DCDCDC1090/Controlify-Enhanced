/*
 * Copyright (C) 2026 isXander
 * This file is part of Controlify.
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package dev.isxander.controlify.mixins.feature.targetlock;

import dev.isxander.controlify.aimassist.TargetLock;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/// Last hit target lock, outgoing half: whatever the player swings at becomes the locked target.
///
/// Hooked here rather than on the damage the server sends back, so the lock lands on the swing
/// itself. That keeps it immediate, and keeps it working on servers that never send a damage
/// event at all.
@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {
	@Inject(method = "attack", at = @At("RETURN"))
	private void onAttackEntity(Player player, Entity target, CallbackInfo ci) {
		TargetLock.onPlayerAttack(target);
	}
}
