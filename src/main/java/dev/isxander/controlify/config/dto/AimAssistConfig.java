/*
 * Copyright (C) 2026 isXander
 * This file is part of Controlify.
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package dev.isxander.controlify.config.dto;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.isxander.controlify.aimassist.AimAssistLevel;
import dev.isxander.controlify.aimassist.AimAssistMode;
import dev.isxander.controlify.aimassist.AimAssistTargets;

import java.util.List;

/**
 * Serialised aim assist settings. Kept as its own section of the config rather than more fields
 * on {@link GlobalConfig}, which is already at the 16-field limit a single record codec allows.
 */
public record AimAssistConfig(
		AimAssistMode mode,
		AimAssistTargets targets,
		AimAssistLevel meleeStrength,
		AimAssistLevel meleeCone,
		AimAssistLevel meleeDistance,
		AimAssistLevel bowStrength,
		AimAssistLevel bowCone,
		AimAssistLevel bowDistance,
		List<String> customTargets
) {
	public static final AimAssistConfig DEFAULT = new AimAssistConfig(
			AimAssistMode.OFF,
			AimAssistTargets.HOSTILE,
			AimAssistLevel.MEDIUM,
			AimAssistLevel.MEDIUM,
			AimAssistLevel.MEDIUM,
			AimAssistLevel.MEDIUM,
			AimAssistLevel.MEDIUM,
			AimAssistLevel.MEDIUM,
			List.of()
	);

	public static final Codec<AimAssistConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			AimAssistMode.CODEC.optionalFieldOf("mode", DEFAULT.mode()).forGetter(AimAssistConfig::mode),
			AimAssistTargets.CODEC.optionalFieldOf("targets", DEFAULT.targets()).forGetter(AimAssistConfig::targets),
			AimAssistLevel.CODEC.optionalFieldOf("melee_strength", DEFAULT.meleeStrength()).forGetter(AimAssistConfig::meleeStrength),
			AimAssistLevel.CODEC.optionalFieldOf("melee_cone", DEFAULT.meleeCone()).forGetter(AimAssistConfig::meleeCone),
			AimAssistLevel.CODEC.optionalFieldOf("melee_distance", DEFAULT.meleeDistance()).forGetter(AimAssistConfig::meleeDistance),
			AimAssistLevel.CODEC.optionalFieldOf("bow_strength", DEFAULT.bowStrength()).forGetter(AimAssistConfig::bowStrength),
			AimAssistLevel.CODEC.optionalFieldOf("bow_cone", DEFAULT.bowCone()).forGetter(AimAssistConfig::bowCone),
			AimAssistLevel.CODEC.optionalFieldOf("bow_distance", DEFAULT.bowDistance()).forGetter(AimAssistConfig::bowDistance),
			Codec.list(Codec.STRING).optionalFieldOf("custom_targets", DEFAULT.customTargets()).forGetter(AimAssistConfig::customTargets)
	).apply(instance, AimAssistConfig::new));
}
