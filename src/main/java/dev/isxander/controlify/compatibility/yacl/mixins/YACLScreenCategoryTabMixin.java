/*
 * Copyright (C) 2026 isXander
 * This file is part of Controlify.
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package dev.isxander.controlify.compatibility.yacl.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.isxander.controlify.api.buttonguide.ButtonGuideApi;
import dev.isxander.controlify.api.buttonguide.ButtonGuidePredicate;
import dev.isxander.controlify.bindings.ControlifyBindings;
import dev.isxander.controlify.gui.devfunctions.DevFunctionsPanel;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.gui.DescriptionWithName;
import dev.isxander.yacl3.gui.OptionDescriptionWidget;
import dev.isxander.yacl3.gui.YACLScreen;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;
import java.util.function.Supplier;

@Mixin(YACLScreen.CategoryTab.class)
public class YACLScreenCategoryTabMixin {
	@Shadow @Final
	public Button saveFinishedButton;

	@Shadow @Final
	public Button undoButton;

	/** The Dev Functions panel; only present on Controlify's Global Settings tab. */
	@Unique @Nullable private DevFunctionsPanel controlify$devFunctionsPanel;

	@Inject(method = "<init>", at = @At("RETURN"), require = 0)
	private void onConstructCategory(CallbackInfo ci) {
		ButtonGuideApi.addGuideToButton(saveFinishedButton, ControlifyBindings.GUI_ABSTRACT_ACTION_1, ButtonGuidePredicate.always());
	}

	@Inject(method = "<init>", at = @At("RETURN"), require = 0)
	private void controlify$addDevFunctionsPanel(YACLScreen screen, ConfigCategory category, ScreenRectangle tabArea, CallbackInfo ci) {
		if (DevFunctionsPanel.isHost(category)) {
			// YACL puts its search box 22px above the Cancel/Undo row.
			int searchFieldY = undoButton.getY() - 22;
			controlify$devFunctionsPanel = DevFunctionsPanel.create(screen.width, category, tabArea, searchFieldY);
		}
	}

	/**
	 * While the Dev Functions panel is shown, keep the option description area above it, so
	 * long descriptions scroll instead of being drawn underneath the panel.
	 */
	@WrapOperation(method = "<init>", at = @At(value = "NEW", target = "dev/isxander/yacl3/gui/OptionDescriptionWidget"), require = 0)
	private OptionDescriptionWidget controlify$limitDescriptionArea(Supplier<ScreenRectangle> dimensions, DescriptionWithName description, Operation<OptionDescriptionWidget> original) {
		Supplier<ScreenRectangle> limited = () -> {
			ScreenRectangle rect = dimensions.get();
			DevFunctionsPanel panel = controlify$devFunctionsPanel;
			if (panel != null && panel.isShown()) {
				int maxHeight = Math.max(0, panel.top() - 3 - rect.top());
				if (maxHeight < rect.height()) {
					return new ScreenRectangle(rect.left(), rect.top(), rect.width(), maxHeight);
				}
			}
			return rect;
		};
		return original.call(limited, description);
	}

	@Inject(method = "visitChildren", at = @At("TAIL"), require = 0)
	private void controlify$visitDevFunctionsPanel(Consumer<AbstractWidget> consumer, CallbackInfo ci) {
		if (controlify$devFunctionsPanel != null) {
			controlify$devFunctionsPanel.visitWidgets(consumer);
		}
	}
}
