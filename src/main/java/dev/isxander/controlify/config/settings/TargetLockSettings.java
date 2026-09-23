/*
 * Copyright (C) 2026 isXander
 * This file is part of Controlify.
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package dev.isxander.controlify.config.settings;

import dev.isxander.controlify.aimassist.TargetLockMode;
import dev.isxander.controlify.config.dto.TargetLockConfig;
import net.minecraft.util.Mth;

/** Live target lock settings, shown on the Aim Assist screen and read every tick while locked. */
public class TargetLockSettings {
	public boolean enabled;
	public TargetLockMode mode;

	/** Whether a target that has been left behind is eventually let go of on its own. */
	public boolean autoDrop;

	/** How far a mob that walks may be left behind before the drop timer starts, in blocks. */
	public int groundRange;
	/** The same for a mob that flies, which covers ground far faster and is usually further off. */
	public int flyingRange;
	/**
	 * How far back inside the boundary the player has to come for the drop timer to reset, as a
	 * percentage of the boundary. Without it the timer would restart every time a mob chasing the
	 * player crossed the line. A percentage rather than a block count so it stays sensible however
	 * the ranges above are set, instead of needing to be clamped against them.
	 */
	public int resetPercent;
	/** How long the player may stay outside the boundary before the lock is let go of. */
	public int dropSeconds;

	/** Whether a locked target gets aim assist from any angle, rather than only within the cone. */
	public boolean overrideCone;

	public int lockedStrengthPercent;
	/** How far away a locked mob can be and still get aim assist, in blocks. */
	public int lockedRangeBlocks;
	public int lockedSpeedPercent;

	/** Whether the marker over the locked mob is drawn at all. */
	public boolean arrowEnabled;
	/** Marker colour as plain RGB, set on the colour wheel. */
	public int arrowColour;

	private TargetLockSettings() {
		apply(TargetLockConfig.DEFAULT);
	}

	private void apply(TargetLockConfig dto) {
		this.enabled = dto.enabled();
		this.mode = dto.mode();
		this.autoDrop = dto.autoDrop();
		this.groundRange = dto.groundRange();
		this.flyingRange = dto.flyingRange();
		this.resetPercent = dto.resetPercent();
		this.dropSeconds = dto.dropSeconds();
		this.overrideCone = dto.overrideCone();
		this.lockedStrengthPercent = dto.lockedStrengthPercent();
		this.lockedRangeBlocks = dto.lockedRangeBlocks();
		this.lockedSpeedPercent = dto.lockedSpeedPercent();
		this.arrowEnabled = dto.arrowEnabled();
		this.arrowColour = dto.arrowColour();
	}

	public static TargetLockSettings defaults() {
		return new TargetLockSettings();
	}

	public static TargetLockSettings fromDTO(TargetLockConfig dto) {
		TargetLockSettings settings = new TargetLockSettings();
		settings.apply(dto);
		return settings;
	}

	public TargetLockConfig toDTO() {
		return new TargetLockConfig(
				enabled, mode, autoDrop, groundRange, flyingRange, resetPercent, dropSeconds,
				overrideCone, lockedStrengthPercent, lockedRangeBlocks, lockedSpeedPercent,
				arrowEnabled, arrowColour
		);
	}

	/** The boundary for this target, in blocks: further for something that flies. */
	public int rangeFor(boolean flying) {
		return flying ? flyingRange : groundRange;
	}

	/**
	 * How close the player must get for the drop timer to reset. Derived from the boundary, so it
	 * can never end up larger than the boundary itself and leave the timer unresettable.
	 */
	public double resetRadiusFor(boolean flying) {
		int range = rangeFor(flying);
		return range * (1 - Mth.clamp(resetPercent, 0, 100) / 100.0);
	}
}
