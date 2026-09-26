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
 * The developer panel's own settings: whether it is shown, and the numbers being tuned from it.
 * <p>
 * Split out of {@link GlobalConfig} rather than added to it, because that record was already at
 * the sixteen fields a single record codec allows and there will be more of these. Whether the
 * panel is shown moved in here with them: it belongs with them, and moving it is what made room.
 * <p>
 * The whole block is optional and every field inside it is too, so a config written before this
 * existed still loads. The one visible cost is that whether the panel is shown goes back to its
 * default the first time, since the old setting sat at the top level under a different name.
 */
public record DevConfig(
		boolean showFunctions,
		int colorPointerSpeed,
		String wiredPaths,
		String wirelessPaths
) {
	/**
	 * How fast the color wheels' pointer moves at full stick, in GUI pixels a second.
	 * <p>
	 * Deliberately far slower than the virtual mouse, which covers about four hundred screen
	 * pixels a second at its default sensitivity. A color disc is a hundred and twelve pixels
	 * across, so at that rate the whole of it would go by in under a second and picking a
	 * particular color would be luck. At this rate crossing it takes nearly two.
	 */
	public static final int DEFAULT_COLOR_POINTER_SPEED = 60;
	/** Slow enough to be a crawl, and still enough to cross the disc inside twenty seconds. */
	public static final int MIN_COLOR_POINTER_SPEED = 10;
	/** Past this it is faster than the virtual mouse, which is the thing it is meant to be under. */
	public static final int MAX_COLOR_POINTER_SPEED = 300;

	/**
	 * Which device paths mean a cable and which mean a receiver, learned from the dev panel rather
	 * than worked out.
	 * <p>
	 * SDL will not say. Its connection state is unknown for pads handled by XInput or GameInput,
	 * its battery reading can say "charging" on a pad running off a dongle, and a pad's GUID is
	 * the same either way. Only the device path differs, and nothing in it reveals which is which -
	 * that is a fact about someone's desk, not about the hardware. So it is recorded when told, and
	 * nothing is claimed until then. Stored as the paths seen at that moment, separated by a
	 * newline, so a connection that presents more than one is kept whole.
	 */
	public static final String PATH_SEPARATOR = "\n";

	public static final DevConfig DEFAULT = new DevConfig(true, DEFAULT_COLOR_POINTER_SPEED, "", "");

	public static final Codec<DevConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.BOOL.optionalFieldOf("show_functions", DEFAULT.showFunctions()).forGetter(DevConfig::showFunctions),
			Codec.intRange(MIN_COLOR_POINTER_SPEED, MAX_COLOR_POINTER_SPEED)
					.optionalFieldOf("colour_pointer_speed", DEFAULT.colorPointerSpeed())
					.forGetter(DevConfig::colorPointerSpeed),
			Codec.STRING.optionalFieldOf("wired_paths", DEFAULT.wiredPaths()).forGetter(DevConfig::wiredPaths),
			Codec.STRING.optionalFieldOf("wireless_paths", DEFAULT.wirelessPaths()).forGetter(DevConfig::wirelessPaths)
	).apply(instance, DevConfig::new));
}
