/*
 * Copyright (C) 2026 isXander
 * This file is part of Controlify.
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package dev.isxander.controlify.mixins.feature.targetlock;

import net.minecraft.client.renderer.LevelRenderer;
//? if >=26.3 {
import dev.isxander.controlify.aimassist.TargetLockRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//?}
import org.spongepowered.asm.mixin.Mixin;

/// Puts the target lock marker into the world.
///
/// This is the one place a mod can hand geometry to the level renderer: everything drawn with the
/// world - mobs, block entities, particles, the block outline - is submitted from here, and the
/// collector passed in is what decides which pass each piece lands in. A render type that does not
/// blend goes into the solid pass, which is the pass that writes depth.
///
/// That is the whole reason for hooking it. The marker used to go through the gizmo API, which
/// needs no mixin at all - but every gizmo is drawn through `debug_filled_box`, and that pipeline
/// blends and leaves depth writes off. So the marker came out slightly transparent, picked up the
/// color of whatever was behind it, and had the clouds drawn straight over the top of it.
///
/// Empty on the versions before 26.3, which have no submission pass to hook; there the marker is
/// still drawn as a HUD layer over the finished picture.
@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
	//? if >=26.3 {
	@Inject(method = "submitFeatures", at = @At("HEAD"))
	private void controlify$submitTargetLockMarker(LevelRenderState levelRenderState,
			SubmitNodeCollector collector, boolean blockOutline, CallbackInfo ci) {
		TargetLockRenderer.submitWorldMarker(collector, levelRenderState.cameraRenderState.pos);
	}
	//?}
}
