/*
 * Copyright (C) 2026 isXander
 * This file is part of Controlify.
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package dev.isxander.controlify.mixins.feature.targetlock;

import dev.isxander.controlify.aimassist.TargetLock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/// Last hit target lock, for the hits the client can't see for itself: everything the player takes,
/// and the player's own shots landing.
///
/// Melee the player deals is not handled here. That goes through MultiPlayerGameMode#attack on the
/// swing, which is immediate and doesn't depend on the server sending anything back.
@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {
	@Inject(method = "handleDamageEvent", at = @At("RETURN"))
	private void onDamageEvent(ClientboundDamageEventPacket packet, CallbackInfo ci) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		ClientLevel level = minecraft.level;
		if (player == null || level == null) {
			return;
		}

		// The packet's entity ids are not plain entity ids - it needs a spare value to mean
		// "nobody" - so the decoding is left to vanilla rather than reproduced here.
		DamageSource source = packet.getSource(level);
		if (source == null) {
			return;
		}

		if (packet.entityId() == player.getId()) {
			TargetLock.onPlayerHurt(source.getEntity(), packet.sourceType().is(DamageTypeTags.IS_PROJECTILE));
			return;
		}

		// The player's own arrow, bolt or trident landing. An arrow in flight belongs to the
		// server, so unlike a swing there is nothing client side to hook: the damage is the
		// first the client hears that the shot connected at all.
		if (source.getEntity() == player && packet.sourceType().is(DamageTypeTags.IS_PROJECTILE)) {
			TargetLock.onPlayerAttack(level.getEntity(packet.entityId()));
		}
	}
}
