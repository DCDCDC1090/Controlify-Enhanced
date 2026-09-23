/*
 * Copyright (C) 2026 isXander
 * This file is part of Controlify.
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package dev.isxander.controlify.gui.screen;

import dev.isxander.controlify.Controlify;
import dev.isxander.controlify.aimassist.AimAssistMode;
import dev.isxander.controlify.aimassist.AimAssistTargets;
import dev.isxander.controlify.aimassist.TargetLockMode;
import dev.isxander.controlify.config.dto.AimAssistConfig;
import dev.isxander.controlify.config.dto.TargetLockConfig;
import dev.isxander.controlify.config.settings.AimAssistSettings;
import dev.isxander.controlify.config.settings.TargetLockSettings;
import dev.isxander.controlify.utils.MinecraftUtil;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.TickBoxControllerBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Util;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Supplier;

/**
 * The Aim Assist screen, opened from Global Settings. Strength and range are split into melee
 * and bow groups, because a bow wants a gentler pull in a much tighter cone than a sword does,
 * and every setting is a slider in its own units rather than a three-step scale: the difference
 * between two levels was always a number, and hiding it only made tuning by feel harder.
 */
public class AimAssistScreenFactory {
	public static Screen createAimAssistScreen(Screen parent) {
		AimAssistSettings settings = Controlify.instance().config().getSettings().aimAssistSettings();
		AimAssistSettings defaults = AimAssistSettings.defaults();
		TargetLockSettings lock = settings.targetLock;
		TargetLockSettings lockDefaults = defaults.targetLock;
		AtomicReference<ButtonOption> customListOptRef = new AtomicReference<>();

		// Held as locals so the toggle below can grey them out. A slider that is still live while
		// the thing it configures is switched off is exactly what sent us hunting a phantom bug.
		Option<Integer> groundRange = slider("controlify.gui.target_lock.ground_range", 0, TargetLockConfig.MAX_RANGE, 1, BLOCKS,
				lockDefaults.groundRange, () -> lock.groundRange, v -> lock.groundRange = v);
		Option<Integer> flyingRange = slider("controlify.gui.target_lock.flying_range", 0, TargetLockConfig.MAX_RANGE, 1, BLOCKS,
				lockDefaults.flyingRange, () -> lock.flyingRange, v -> lock.flyingRange = v);
		Option<Integer> resetPercent = slider("controlify.gui.target_lock.reset_percent", 0, 100, 1, PERCENT,
				lockDefaults.resetPercent, () -> lock.resetPercent, v -> lock.resetPercent = v);
		Option<Integer> dropSeconds = slider("controlify.gui.target_lock.drop_seconds", 1, 300, 1, SECONDS,
				lockDefaults.dropSeconds, () -> lock.dropSeconds, v -> lock.dropSeconds = v);
		List<Option<Integer>> dropoutOptions = List.of(groundRange, flyingRange, resetPercent, dropSeconds);
		dropoutOptions.forEach(option -> option.setAvailable(lock.autoDrop));

		Option<Boolean> autoDrop = Option.<Boolean>createBuilder()
				.name(Component.translatable("controlify.gui.target_lock.auto_drop"))
				.description(OptionDescription.of(Component.translatable("controlify.gui.target_lock.auto_drop.tooltip")))
				.binding(lockDefaults.autoDrop, () -> lock.autoDrop, v -> lock.autoDrop = v)
				.controller(TickBoxControllerBuilder::create)
				.build();
		autoDrop.addListener((opt, event) -> dropoutOptions.forEach(option -> option.setAvailable(opt.pendingValue())));

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
								.option(slider("controlify.gui.aim_assist.strength", 0, 100, 1, PERCENT,
										defaults.meleeStrengthPercent, () -> settings.meleeStrengthPercent, v -> settings.meleeStrengthPercent = v))
								.option(slider("controlify.gui.aim_assist.cone",
										AimAssistConfig.MIN_CONE_TENTHS, AimAssistConfig.MAX_MELEE_CONE_TENTHS, 5, DEGREES,
										defaults.meleeConeTenths, () -> settings.meleeConeTenths, v -> settings.meleeConeTenths = v))
								.option(slider("controlify.gui.aim_assist.distance", 1, AimAssistConfig.MAX_MELEE_DISTANCE, 1, BLOCKS,
										defaults.meleeDistanceBlocks, () -> settings.meleeDistanceBlocks, v -> settings.meleeDistanceBlocks = v))
								.build())
						.group(OptionGroup.createBuilder()
								.name(Component.translatable("controlify.gui.aim_assist.bow"))
								.description(OptionDescription.of(Component.translatable("controlify.gui.aim_assist.bow.tooltip")))
								.option(slider("controlify.gui.aim_assist.strength", 0, 100, 1, PERCENT,
										defaults.bowStrengthPercent, () -> settings.bowStrengthPercent, v -> settings.bowStrengthPercent = v))
								.option(slider("controlify.gui.aim_assist.cone",
										AimAssistConfig.MIN_CONE_TENTHS, AimAssistConfig.MAX_BOW_CONE_TENTHS, 5, DEGREES,
										defaults.bowConeTenths, () -> settings.bowConeTenths, v -> settings.bowConeTenths = v))
								.option(slider("controlify.gui.aim_assist.distance", 1, AimAssistConfig.MAX_BOW_DISTANCE, 1, BLOCKS,
										defaults.bowDistanceBlocks, () -> settings.bowDistanceBlocks, v -> settings.bowDistanceBlocks = v))
								.build())
						.group(OptionGroup.createBuilder()
								.name(Component.translatable("controlify.gui.target_lock"))
								.description(OptionDescription.of(Component.translatable("controlify.gui.target_lock.tooltip")))
								.option(Option.<Boolean>createBuilder()
										.name(Component.translatable("controlify.gui.target_lock.enabled"))
										.description(OptionDescription.of(Component.translatable("controlify.gui.target_lock.enabled.tooltip")))
										.binding(lockDefaults.enabled, () -> lock.enabled, v -> lock.enabled = v)
										.controller(TickBoxControllerBuilder::create)
										.build())
								.option(Option.<TargetLockMode>createBuilder()
										.name(Component.translatable("controlify.gui.target_lock.mode"))
										// Described one mode at a time. All three at once needed line
										// breaks inside a single translation string, which the lang
										// loader hands over as literal backslash-n.
										.description(state -> OptionDescription.createBuilder()
												.text(Component.translatable("controlify.gui.target_lock.mode.tooltip"))
												.text(Component.empty())
												.text(Component.translatable(
														"controlify.gui.target_lock.mode." + state.getSerializedName() + ".desc"))
												.build())
										.binding(lockDefaults.mode, () -> lock.mode, v -> lock.mode = v)
										.controller(opt -> EnumControllerBuilder.create(opt).enumClass(TargetLockMode.class))
										.build())
								.option(slider("controlify.gui.target_lock.strength", 0, 100, 1, PERCENT,
										lockDefaults.lockedStrengthPercent, () -> lock.lockedStrengthPercent, v -> lock.lockedStrengthPercent = v))
								.option(slider("controlify.gui.target_lock.range", 1, TargetLockConfig.MAX_LOCKED_RANGE, 1, BLOCKS,
										lockDefaults.lockedRangeBlocks, () -> lock.lockedRangeBlocks, v -> lock.lockedRangeBlocks = v))
								.option(slider("controlify.gui.target_lock.speed", 0, 100, 1, PERCENT,
										lockDefaults.lockedSpeedPercent, () -> lock.lockedSpeedPercent, v -> lock.lockedSpeedPercent = v))
								.option(Option.<Boolean>createBuilder()
										.name(Component.translatable("controlify.gui.target_lock.override_cone"))
										.description(state -> OptionDescription.createBuilder()
												.text(Component.translatable("controlify.gui.target_lock.override_cone.tooltip"))
												.text(state
														? Component.translatable("controlify.gui.target_lock.override_cone.tooltip.warning").withStyle(ChatFormatting.RED)
														: Component.empty())
												.build())
										.binding(lockDefaults.overrideCone, () -> lock.overrideCone, v -> lock.overrideCone = v)
										.controller(TickBoxControllerBuilder::create)
										.build())
								.option(Option.<Boolean>createBuilder()
										.name(Component.translatable("controlify.gui.target_lock.arrow"))
										.description(OptionDescription.of(Component.translatable("controlify.gui.target_lock.arrow.tooltip")))
										.binding(lockDefaults.arrowEnabled, () -> lock.arrowEnabled, v -> lock.arrowEnabled = v)
										.controller(TickBoxControllerBuilder::create)
										.build())
								.option(ButtonOption.createBuilder()
										.name(Component.translatable("controlify.gui.target_lock.arrow_colour"))
										.description(OptionDescription.of(Component.translatable("controlify.gui.target_lock.arrow_colour.tooltip")))
										.action((screen, button) -> MinecraftUtil.setScreen(new ArrowColourScreen(screen, lock)))
										.build())
								.build())
						.group(OptionGroup.createBuilder()
								.name(Component.translatable("controlify.gui.target_lock.dropout"))
								.description(OptionDescription.of(Component.translatable("controlify.gui.target_lock.dropout.tooltip")))
								.option(autoDrop)
								.option(groundRange)
								.option(flyingRange)
								.option(resetPercent)
								.option(dropSeconds)
								.build())
						.build())
				.build().generateScreen(parent);
	}

	private static final IntFunction<Component> PERCENT =
			v -> Component.translatable("controlify.gui.aim_assist.percent_format", v);
	private static final IntFunction<Component> BLOCKS =
			v -> Component.translatable("controlify.gui.aim_assist.blocks_format", v);
	private static final IntFunction<Component> SECONDS =
			v -> Component.translatable("controlify.gui.aim_assist.seconds_format", v);
	/** Cones are stored in tenths of a degree so half-degree steps survive as whole numbers. */
	private static final IntFunction<Component> DEGREES =
			v -> Component.translatable("controlify.gui.aim_assist.degrees_format", String.format("%.1f", v / 10.0));

	private static Option<Integer> slider(
			String translationKey,
			int min,
			int max,
			int step,
			IntFunction<Component> format,
			int defaultValue,
			Supplier<Integer> getter,
			Consumer<Integer> setter
	) {
		return Option.<Integer>createBuilder()
				.name(Component.translatable(translationKey))
				.description(OptionDescription.of(Component.translatable(translationKey + ".tooltip")))
				.binding(defaultValue, getter, setter)
				.controller(opt -> IntegerSliderControllerBuilder.create(opt)
						.range(min, max)
						.step(step)
						.formatValue(format::apply))
				.build();
	}
}
