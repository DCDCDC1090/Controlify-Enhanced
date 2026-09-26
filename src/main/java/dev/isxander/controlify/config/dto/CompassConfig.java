/*
 * Copyright (C) 2026 isXander
 * This file is part of Controlify.
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package dev.isxander.controlify.config.dto;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Serialised compass bar settings. Split out of {@link TargetLockConfig} once the bar grew a
 * position and a width of its own: that record was two fields off the 16 a single record codec
 * allows, and everything here belongs together anyway.
 * <p>
 * The offsets are measured from where the bar sits by default — centred horizontally, just below
 * the top of the screen — rather than from a screen corner, so the bar stays put when the window
 * is resized or the GUI scale changes.
 */
public record CompassConfig(
		boolean enabled,
		int color,
		int offsetX,
		int offsetY,
		int width
) {
	/** Default bar color: the green the Controlify Enhanced banner is built on. */
	public static final int DEFAULT_COLOR = 0x44CA72;

	/**
	 * Default width, end caps included. Its 182 pixels of bar match the vanilla XP bar, which is
	 * what makes it look like it belongs on the screen rather than on top of it.
	 */
	public static final int DEFAULT_WIDTH = 192;
	/** Narrow enough to be a stub in the corner of the screen, still wide enough to read. */
	public static final int MIN_WIDTH = 64;
	/**
	 * Wide enough to span a 4K screen at GUI scale 2. The renderer clamps to the screen on top of
	 * this, so a wide bar set on one monitor cannot run off the edge of a smaller one.
	 */
	public static final int MAX_WIDTH = 640;

	public static final CompassConfig DEFAULT = new CompassConfig(
			true,
			DEFAULT_COLOR,
			0,
			0,
			DEFAULT_WIDTH
	);

	public static final Codec<CompassConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.BOOL.optionalFieldOf("enabled", DEFAULT.enabled()).forGetter(CompassConfig::enabled),
			Codec.intRange(0, 0xFFFFFF).optionalFieldOf("colour", DEFAULT.color()).forGetter(CompassConfig::color),
			Codec.INT.optionalFieldOf("offset_x", DEFAULT.offsetX()).forGetter(CompassConfig::offsetX),
			Codec.INT.optionalFieldOf("offset_y", DEFAULT.offsetY()).forGetter(CompassConfig::offsetY),
			Codec.intRange(MIN_WIDTH, MAX_WIDTH).optionalFieldOf("width", DEFAULT.width()).forGetter(CompassConfig::width)
	).apply(instance, CompassConfig::new));
}
