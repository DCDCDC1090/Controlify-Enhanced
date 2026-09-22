/*
 * Copyright (C) 2026 isXander
 * This file is part of Controlify.
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package dev.isxander.controlify.gui.devfunctions;

import dev.isxander.controlify.Controlify;
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
