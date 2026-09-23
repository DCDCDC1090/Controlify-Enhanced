/*
 * Copyright (C) 2026 isXander
 * This file is part of Controlify.
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package dev.isxander.controlify.aimassist;

import com.mojang.serialization.Codec;
import dev.isxander.yacl3.api.NameableEnum;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import org.jspecify.annotations.NonNull;

/**
 * The three-step scale every aim assist strength and range setting uses. The numbers each level
 * maps to live in {@link AimAssist}, so all the tuning sits in one place.
 */
public enum AimAssistLevel implements NameableEnum, StringRepresentable {
	LOW,
	MEDIUM,
	HIGH;

	public static final Codec<AimAssistLevel> CODEC = StringRepresentable.fromEnum(AimAssistLevel::values);

	private final Component displayName =
			Component.translatable("controlify.aim_assist.level." + this.name().toLowerCase());

	@Override
	public Component getDisplayName() {
		return displayName;
	}

	@Override
	public @NonNull String getSerializedName() {
		return this.name().toLowerCase();
	}
}
