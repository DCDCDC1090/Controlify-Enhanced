/*
 * Copyright (C) 2026 isXander
 * This file is part of Controlify.
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package dev.isxander.controlify.aimassist;

import com.mojang.serialization.Codec;
import dev.isxander.yacl3.api.NameableEnum;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import org.jspecify.annotations.NonNull;

import java.util.function.Predicate;

/**
 * Where aim assist is allowed to run. Mirrors {@code ReachAroundMode}: aim assist is a
 * single-player convenience by default, because plenty of servers class any aim assist as an
 * unfair advantage regardless of how it's implemented.
 */
public enum AimAssistMode implements NameableEnum, StringRepresentable {
	OFF(minecraft -> false),
	SINGLEPLAYER_AND_LAN(Minecraft::isLocalServer),
	EVERYWHERE(minecraft -> true);

	public static final Codec<AimAssistMode> CODEC = StringRepresentable.fromEnum(AimAssistMode::values);

	private final Predicate<Minecraft> canAimAssist;
	private final Component displayName;

	AimAssistMode(Predicate<Minecraft> canAimAssist) {
		this.canAimAssist = canAimAssist;
		this.displayName = Component.translatable("controlify.aim_assist.mode." + this.name().toLowerCase());
	}

	public boolean canAimAssist() {
		return canAimAssist.test(Minecraft.getInstance());
	}

	@Override
	public Component getDisplayName() {
		return displayName;
	}

	@Override
	public @NonNull String getSerializedName() {
		return this.name().toLowerCase();
	}
}
