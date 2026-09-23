/*
 * Copyright (C) 2026 isXander
 * This file is part of Controlify.
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package dev.isxander.controlify.gui.devfunctions;

import dev.isxander.controlify.Controlify;
import dev.isxander.controlify.aimassist.AimAssist;
import dev.isxander.controlify.aimassist.TargetLock;
import dev.isxander.controlify.config.settings.GlobalSettings;
import dev.isxander.controlify.utils.MinecraftUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Registry of the test buttons shown in the "Dev Functions" panel on the right-hand side of
 * Controlify's Global Settings screen (see {@link DevFunctionsPanel}).
 * <p>
 * To add a new test button, add one more {@link #register} call to the static block below:
 * give it a name, a tooltip, when it can be used, and what it does. Buttons appear in the
 * panel in the order they are registered.
 */
public final class DevFunctions {
	/**
	 * @param name      button label
	 * @param tooltip   shown when hovering the button
	 * @param available whether the button can currently be pressed (checked each time the screen opens)
	 * @param action    what happens when the button is pressed
	 */
	public record DevFunction(Component name, Component tooltip, BooleanSupplier available, Runnable action) {
	}

	private static final List<DevFunction> FUNCTIONS = new ArrayList<>();

	static {
		register(new DevFunction(
				Component.translatable("controlify.gui.dev_functions.new_server_toast"),
				Component.translatable("controlify.gui.dev_functions.new_server_toast.tooltip"),
				() -> true,
				DevFunctions::showNewServerToast
		));

		register(new DevFunction(
				Component.translatable("controlify.gui.dev_functions.aim_assist_target"),
				Component.translatable("controlify.gui.dev_functions.aim_assist_target.tooltip"),
				() -> Minecraft.getInstance().player != null,
				DevFunctions::showAimAssistToast
		));

		register(new DevFunction(
				Component.translatable("controlify.gui.dev_functions.target_lock"),
				Component.translatable("controlify.gui.dev_functions.target_lock.tooltip"),
				() -> Minecraft.getInstance().player != null,
				DevFunctions::showTargetLockToast
		));

		register(new DevFunction(
				Component.translatable("controlify.gui.check_movement_type"),
				Component.translatable("controlify.gui.check_movement_type.tooltip"),
				() -> Minecraft.getInstance().player != null,
				DevFunctions::showMovementTypeToast
		));
	}

	private DevFunctions() {
	}

	public static void register(DevFunction function) {
		FUNCTIONS.add(function);
	}

	public static List<DevFunction> all() {
		return Collections.unmodifiableList(FUNCTIONS);
	}

	/** Shows the exact "New server detected" toast players get on a server that isn't whitelisted. */
	private static void showNewServerToast() {
		ServerData server = Minecraft.getInstance().getCurrentServer();
		Controlify.instance().sendNewServerToast(server != null ? server.name : "Test Server");
	}

	/** Reports what aim assist is doing right now, for tuning the strength and range levels. */
	private static void showAimAssistToast() {
		AimAssist.Debug debug = AimAssist.debug();
		Component description;
		if (!debug.active()) {
			description = Component.translatable("controlify.toast.aim_assist.inactive");
		} else if (debug.target() == null) {
			description = Component.translatable(
					"controlify.toast.aim_assist.no_target",
					debug.targets().getDisplayName(),
					Component.translatable(debug.bowMode()
							? "controlify.gui.aim_assist.bow"
							: "controlify.gui.aim_assist.melee"),
					String.valueOf(debug.counts().nearby),
					String.format("%d eligible, %d far, %d outside cone, %d blocked, best %.1f°",
							debug.counts().eligible,
							debug.counts().tooFar,
							debug.counts().outsideCone,
							debug.counts().losBlocked,
							debug.counts().bestAngle)
			);
		} else {
			description = Component.translatable(
					"controlify.toast.aim_assist.target",
					debug.target().getDisplayName(),
					String.format("%.1f", debug.angle()),
					String.format("%.0f", debug.multiplier() * 100),
					String.format("%.2f", debug.pull()),
					Component.translatable(debug.bowMode()
							? "controlify.gui.aim_assist.bow"
							: "controlify.gui.aim_assist.melee")
			);
		}
		if (debug.locked()) {
			description = description.copy().append(Component.translatable("controlify.toast.aim_assist.locked"));
		}
		MinecraftUtil.sendToast(
				Component.translatable("controlify.toast.aim_assist.title"),
				description,
				false
		);
	}

	/** Reports what target lock is holding, and why it would let go. */
	private static void showTargetLockToast() {
		MinecraftUtil.sendToast(
				Component.translatable("controlify.toast.target_lock.title"),
				Component.literal(TargetLock.describe()),
				false
		);
	}

	/** Shows whether analog or keyboard-like movement is active right now. */
	private static void showMovementTypeToast() {
		GlobalSettings globalSettings = Controlify.instance().config().getSettings().globalSettings();
		boolean keyboardLike = globalSettings.shouldUseKeyboardMovement();
		MinecraftUtil.sendToast(
				Component.translatable(keyboardLike
						? "controlify.toast.movement_type.keyboard.title"
						: "controlify.toast.movement_type.analogue.title"),
				Component.translatable(keyboardLike
						? "controlify.toast.movement_type.keyboard.description"
						: "controlify.toast.movement_type.analogue.description"),
				false
		);
	}
}
