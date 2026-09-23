/*
 * Copyright (C) 2026 isXander
 * This file is part of Controlify.
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package dev.isxander.controlify.gui.screen;

import dev.isxander.controlify.Controlify;
import dev.isxander.controlify.aimassist.AimAssistLevel;
import dev.isxander.controlify.aimassist.AimAssistMode;
import dev.isxander.controlify.aimassist.AimAssistTargets;
import dev.isxander.controlify.config.settings.AimAssistSettings;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Util;
import net.minecraft.network.chat.Component;

import java.util.concurrent.atomic.AtomicReference;

/**
 * The Aim Assist screen, opened from Global Settings. Strength and range are split into melee
 * and bow groups, because a bow wants a gentler pull in a much tighter cone than a sword does.
 */
public class AimAssistScreenFactory {
	public static Screen createAimAssistScreen(Screen parent) {
		AimAssistSettings settings = Controlify.instance().config().getSettings().aimAssistSettings();
		AimAssistSettings defaults = AimAssistSettings.defaults();
		AtomicReference<ButtonOption> customListOptRef = new AtomicReference<>();

		return YetAnotherConfigLib.createBuilder()
				.title(Component.translatable("controlify.gui.aim_assist.title"))
				.save(() -> Controlify.instance().config().saveSafely())
				.category(ConfigCategory.createBuilder()
						.name(Component.translatable("controlify.gui.aim_assist.title"))
						.option(Option.<AimAssistMode>createBuilder()
								.name(Component.translatable("controlify.gui.aim_assist.mode"))
								.description(state -> OptionDescription.createBuilder()
										.text(Component.translatable("controlify.gui.aim_assist.mode.tooltip"))
										.text(state == AimAssistMode.EVERYWHERE
												? Component.translatable("controlify.gui.aim_assist.mode.tooltip.warning").withStyle(ChatFormatting.RED)
												: Component.empty())
										.build())
								.binding(defaults.mode, () -> settings.mode, v -> settings.mode = v)
								.controller(opt -> EnumControllerBuilder.create(opt).enumClass(AimAssistMode.class))
								.build())
						.option(Util.make(() -> {
							Option<AimAssistTargets> targets = Option.<AimAssistTargets>createBuilder()
									.name(Component.translatable("controlify.gui.aim_assist.targets"))
									.description(OptionDescription.of(Component.translatable("controlify.gui.aim_assist.targets.tooltip")))
									.binding(defaults.targets, () -> settings.targets, v -> settings.targets = v)
									.controller(opt -> EnumControllerBuilder.create(opt).enumClass(AimAssistTargets.class))
									.build();
							targets.addListener((opt, event) -> {
								ButtonOption customList = customListOptRef.get();
								if (customList != null) {
									customList.setAvailable(opt.pendingValue() == AimAssistTargets.CUSTOM);
								}
							});
							return targets;
						}))
						.option(Util.make(() -> {
							ButtonOption customList = ButtonOption.createBuilder()
									.name(Component.translatable("controlify.gui.aim_assist.custom_list"))
									.description(OptionDescription.of(Component.translatable("controlify.gui.aim_assist.custom_list.tooltip")))
									.action((screen, button) -> {
										// The entity picker lands in the next build; until then the
										// custom list is edited in the config file.
									})
									.available(false)
									.build();
							customListOptRef.set(customList);
							return customList;
						}))
						.group(OptionGroup.createBuilder()
								.name(Component.translatable("controlify.gui.aim_assist.melee"))
								.description(OptionDescription.of(Component.translatable("controlify.gui.aim_assist.melee.tooltip")))
								.option(levelOption(
										"controlify.gui.aim_assist.strength",
										defaults.meleeStrength, () -> settings.meleeStrength, v -> settings.meleeStrength = v))
								.option(levelOption(
										"controlify.gui.aim_assist.cone",
										defaults.meleeCone, () -> settings.meleeCone, v -> settings.meleeCone = v))
								.option(levelOption(
										"controlify.gui.aim_assist.distance",
										defaults.meleeDistance, () -> settings.meleeDistance, v -> settings.meleeDistance = v))
								.build())
						.group(OptionGroup.createBuilder()
								.name(Component.translatable("controlify.gui.aim_assist.bow"))
								.description(OptionDescription.of(Component.translatable("controlify.gui.aim_assist.bow.tooltip")))
								.option(levelOption(
										"controlify.gui.aim_assist.strength",
										defaults.bowStrength, () -> settings.bowStrength, v -> settings.bowStrength = v))
								.option(levelOption(
										"controlify.gui.aim_assist.cone",
										defaults.bowCone, () -> settings.bowCone, v -> settings.bowCone = v))
								.option(levelOption(
										"controlify.gui.aim_assist.distance",
										defaults.bowDistance, () -> settings.bowDistance, v -> settings.bowDistance = v))
								.build())
						.build())
				.build().generateScreen(parent);
	}

	private static Option<AimAssistLevel> levelOption(
			String translationKey,
			AimAssistLevel defaultValue,
			java.util.function.Supplier<AimAssistLevel> getter,
			java.util.function.Consumer<AimAssistLevel> setter
	) {
		return Option.<AimAssistLevel>createBuilder()
				.name(Component.translatable(translationKey))
				.description(OptionDescription.of(Component.translatable(translationKey + ".tooltip")))
				.binding(defaultValue, getter, setter)
				.controller(opt -> EnumControllerBuilder.create(opt).enumClass(AimAssistLevel.class))
				.build();
	}
}
