/*
 * Copyright (C) 2026 isXander
 * This file is part of Controlify.
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package dev.isxander.controlify.config.dto;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.isxander.controlify.aimassist.TargetLockMode;

/**
 * Serialised target lock settings. A section of its own rather than more fields on
 * {@link AimAssistConfig}, which would otherwise run into the 16-field limit a single record
 * codec allows.
 */
public record TargetLockConfig(
		boolean enabled,
		TargetLockMode mode,
		boolean autoDrop,
		int groundRange,
		int flyingRange,
		int resetPercent,
		int dropSeconds,
		boolean overrideCone,
		int lockedStrengthPercent,
		int lockedRangeBlocks,
		int lockedSpeedPercent,
		boolean arrowEnabled,
		int arrowColour
) {
	/** Widest a range slider goes. Well past any sensible value, but it costs nothing to allow. */
	public static final int MAX_RANGE = 500;
	public static final int MAX_LOCKED_RANGE = 160;
	/** Default marker colour, as plain RGB. */
	public static final int DEFAULT_ARROW_COLOUR = 0xFF3B30;

	public static final TargetLockConfig DEFAULT = new TargetLockConfig(
			false,
			TargetLockMode.KEYBIND,
			true,
			25,
			50,
			40,
			15,
			false,
			50,
			32,
			50,
			true,
			DEFAULT_ARROW_COLOUR
	);

	public static final Codec<TargetLockConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.BOOL.optionalFieldOf("enabled", DEFAULT.enabled()).forGetter(TargetLockConfig::enabled),
			TargetLockMode.CODEC.optionalFieldOf("mode", DEFAULT.mode()).forGetter(TargetLockConfig::mode),
			Codec.BOOL.optionalFieldOf("auto_drop", DEFAULT.autoDrop()).forGetter(TargetLockConfig::autoDrop),
			Codec.intRange(0, MAX_RANGE).optionalFieldOf("ground_range", DEFAULT.groundRange()).forGetter(TargetLockConfig::groundRange),
			Codec.intRange(0, MAX_RANGE).optionalFieldOf("flying_range", DEFAULT.flyingRange()).forGetter(TargetLockConfig::flyingRange),
			Codec.intRange(0, 100).optionalFieldOf("reset_percent", DEFAULT.resetPercent()).forGetter(TargetLockConfig::resetPercent),
			Codec.intRange(1, 300).optionalFieldOf("drop_seconds", DEFAULT.dropSeconds()).forGetter(TargetLockConfig::dropSeconds),
			Codec.BOOL.optionalFieldOf("override_cone", DEFAULT.overrideCone()).forGetter(TargetLockConfig::overrideCone),
			Codec.intRange(0, 100).optionalFieldOf("locked_strength_percent", DEFAULT.lockedStrengthPercent()).forGetter(TargetLockConfig::lockedStrengthPercent),
			Codec.intRange(1, MAX_LOCKED_RANGE).optionalFieldOf("locked_range_blocks", DEFAULT.lockedRangeBlocks()).forGetter(TargetLockConfig::lockedRangeBlocks),
			Codec.intRange(0, 100).optionalFieldOf("locked_speed_percent", DEFAULT.lockedSpeedPercent()).forGetter(TargetLockConfig::lockedSpeedPercent),
			Codec.BOOL.optionalFieldOf("arrow_enabled", DEFAULT.arrowEnabled()).forGetter(TargetLockConfig::arrowEnabled),
			Codec.intRange(0, 0xFFFFFF).optionalFieldOf("arrow_colour", DEFAULT.arrowColour()).forGetter(TargetLockConfig::arrowColour)
	).apply(instance, TargetLockConfig::new));
}
