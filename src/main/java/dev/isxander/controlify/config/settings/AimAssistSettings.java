/*
 * Copyright (C) 2026 isXander
 * This file is part of Controlify.
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package dev.isxander.controlify.config.settings;

import dev.isxander.controlify.aimassist.AimAssistLevel;
import dev.isxander.controlify.aimassist.AimAssistMode;
import dev.isxander.controlify.aimassist.AimAssistTargets;
import dev.isxander.controlify.config.dto.AimAssistConfig;

import java.util.ArrayList;
import java.util.List;

/** Live aim assist settings, shown on the Aim Assist screen and read every tick while looking. */
public class AimAssistSettings {
	public AimAssistMode mode;
	public AimAssistTargets targets;

	public AimAssistLevel meleeStrength;
	public AimAssistLevel meleeCone;
	public AimAssistLevel meleeDistance;

	public AimAssistLevel bowStrength;
	public AimAssistLevel bowCone;
	public AimAssistLevel bowDistance;

	/** Entity type ids ("minecraft:silverfish") used when {@link #targets} is CUSTOM. */
	public List<String> customTargets;

	private AimAssistSettings() {
		AimAssistConfig defaults = AimAssistConfig.DEFAULT;
		this.mode = defaults.mode();
		this.targets = defaults.targets();
		this.meleeStrength = defaults.meleeStrength();
		this.meleeCone = defaults.meleeCone();
		this.meleeDistance = defaults.meleeDistance();
		this.bowStrength = defaults.bowStrength();
		this.bowCone = defaults.bowCone();
		this.bowDistance = defaults.bowDistance();
		this.customTargets = new ArrayList<>(defaults.customTargets());
	}

	public static AimAssistSettings defaults() {
		return new AimAssistSettings();
	}

	public static AimAssistSettings fromDTO(AimAssistConfig dto) {
		AimAssistSettings settings = new AimAssistSettings();
		settings.mode = dto.mode();
		settings.targets = dto.targets();
		settings.meleeStrength = dto.meleeStrength();
		settings.meleeCone = dto.meleeCone();
		settings.meleeDistance = dto.meleeDistance();
		settings.bowStrength = dto.bowStrength();
		settings.bowCone = dto.bowCone();
		settings.bowDistance = dto.bowDistance();
		settings.customTargets = new ArrayList<>(dto.customTargets());
		return settings;
	}

	public AimAssistConfig toDTO() {
		return new AimAssistConfig(
				mode,
				targets,
				meleeStrength,
				meleeCone,
				meleeDistance,
				bowStrength,
				bowCone,
				bowDistance,
				List.copyOf(customTargets)
		);
	}
}
