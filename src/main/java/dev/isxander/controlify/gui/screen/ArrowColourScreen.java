/*
 * Copyright (C) 2026 isXander
 * This file is part of Controlify.
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package dev.isxander.controlify.gui.screen;

import dev.isxander.controlify.Controlify;
import dev.isxander.controlify.config.settings.TargetLockSettings;
import dev.isxander.controlify.utils.MinecraftUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jspecify.annotations.NonNull;

/**
 * Picks the target marker's colour off a hue/saturation wheel with a brightness column beside it.
 * <p>
 * The wheel is drawn as a grid of small squares rather than uploaded as a texture: it costs a few
 * thousand rectangles a frame on a settings screen, which is nothing, and it needs no more of the
 * renderer than drawing a rectangle — the same bet the marker itself makes.
 */
public class ArrowColourScreen extends Screen {
	private static final int WHEEL_RADIUS = 62;
	/** Size of each square the wheel is built from. Smaller is smoother and costs more. */
	private static final int CELL = 2;

	private static final int BAR_WIDTH = 18;
	private static final int BAR_GAP = 26;
	private static final int SWATCH_HEIGHT = 22;
	private static final int FOOTER_BUTTON_WIDTH = 150;

	private final Screen parent;
	private final TargetLockSettings settings;
	private final int originalColour;

	private float hue;
	private float saturation;
	private float value;

	private int wheelX;
	private int wheelY;
	private int barX;
	private int barTop;
	private int barBottom;

	public ArrowColourScreen(Screen parent, TargetLockSettings settings) {
		super(Component.translatable("controlify.gui.target_lock.arrow_colour.title"));
		this.parent = parent;
		this.settings = settings;
		this.originalColour = settings.arrowColour;
		setFromRgb(settings.arrowColour);
	}

	@Override
	protected void init() {
		wheelX = width / 2 - (BAR_WIDTH + BAR_GAP) / 2;
		wheelY = height / 2 - 14;
		barX = wheelX + WHEEL_RADIUS + BAR_GAP;
		barTop = wheelY - WHEEL_RADIUS;
		barBottom = wheelY + WHEEL_RADIUS;

		addRenderableWidget(Button.builder(Component.translatable("controlify.gui.target_lock.arrow_colour.reset"),
						b -> setFromRgb(dev.isxander.controlify.config.dto.TargetLockConfig.DEFAULT_ARROW_COLOUR))
				.bounds(width / 2 - FOOTER_BUTTON_WIDTH - 2, height - 28, FOOTER_BUTTON_WIDTH, 20)
				.build());
		addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
				.bounds(width / 2 + 2, height - 28, FOOTER_BUTTON_WIDTH, 20)
				.build());
	}

	@Override
	public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractRenderState(graphics, mouseX, mouseY, a);

		graphics.text(font, title, width / 2 - font.width(title) / 2, 18, 0xFFFFFFFF);

		drawWheel(graphics);
		drawBrightnessBar(graphics);

		int colour = currentRgb();
		int swatchWidth = WHEEL_RADIUS * 2 + BAR_GAP + BAR_WIDTH;
		int swatchX = wheelX - WHEEL_RADIUS;
		int swatchY = barBottom + 14;
		graphics.fill(swatchX - 1, swatchY - 1, swatchX + swatchWidth + 1, swatchY + SWATCH_HEIGHT + 1, 0xFF000000);
		graphics.fill(swatchX, swatchY, swatchX + swatchWidth, swatchY + SWATCH_HEIGHT, 0xFF000000 | colour);

		String hex = String.format("#%06X", colour);
		graphics.text(font, hex, width / 2 - font.width(hex) / 2, swatchY + SWATCH_HEIGHT + 6, 0xFFFFFFFF);
	}

	private void drawWheel(GuiGraphicsExtractor graphics) {
		for (int dy = -WHEEL_RADIUS; dy <= WHEEL_RADIUS; dy += CELL) {
			for (int dx = -WHEEL_RADIUS; dx <= WHEEL_RADIUS; dx += CELL) {
				double distance = Math.sqrt(dx * dx + dy * dy);
				if (distance > WHEEL_RADIUS) {
					continue;
				}
				float cellHue = (float) ((Math.toDegrees(Math.atan2(dy, dx)) + 360) % 360 / 360.0);
				float cellSaturation = (float) (distance / WHEEL_RADIUS);
				graphics.fill(wheelX + dx, wheelY + dy, wheelX + dx + CELL, wheelY + dy + CELL,
						0xFF000000 | hsvToRgb(cellHue, cellSaturation, value));
			}
		}

		double angle = Math.toRadians(hue * 360);
		int markerX = wheelX + (int) Math.round(Math.cos(angle) * saturation * WHEEL_RADIUS);
		int markerY = wheelY + (int) Math.round(Math.sin(angle) * saturation * WHEEL_RADIUS);
		drawRing(graphics, markerX, markerY, 4);
	}

	private void drawBrightnessBar(GuiGraphicsExtractor graphics) {
		graphics.fill(barX - 1, barTop - 1, barX + BAR_WIDTH + 1, barBottom + 1, 0xFF000000);
		int steps = barBottom - barTop;
		for (int i = 0; i < steps; i++) {
			float barValue = 1 - (float) i / steps;
			graphics.fill(barX, barTop + i, barX + BAR_WIDTH, barTop + i + 1,
					0xFF000000 | hsvToRgb(hue, saturation, barValue));
		}
		int handleY = barTop + Math.round((1 - value) * steps);
		graphics.fill(barX - 3, handleY - 1, barX + BAR_WIDTH + 3, handleY + 1, 0xFFFFFFFF);
	}

	/** A small hollow square, so the marker stays visible over any colour underneath it. */
	private void drawRing(GuiGraphicsExtractor graphics, int x, int y, int radius) {
		graphics.fill(x - radius, y - radius, x + radius, y - radius + 1, 0xFF000000);
		graphics.fill(x - radius, y + radius - 1, x + radius, y + radius, 0xFF000000);
		graphics.fill(x - radius, y - radius, x - radius + 1, y + radius, 0xFF000000);
		graphics.fill(x + radius - 1, y - radius, x + radius, y + radius, 0xFF000000);
		graphics.fill(x - radius + 1, y - radius + 1, x + radius - 1, y - radius + 2, 0xFFFFFFFF);
		graphics.fill(x - radius + 1, y + radius - 2, x + radius - 1, y + radius - 1, 0xFFFFFFFF);
		graphics.fill(x - radius + 1, y - radius + 1, x - radius + 2, y + radius - 1, 0xFFFFFFFF);
		graphics.fill(x + radius - 2, y - radius + 1, x + radius - 1, y + radius - 1, 0xFFFFFFFF);
	}

	@Override
	public boolean mouseClicked(@NonNull MouseButtonEvent mouseButtonEvent, boolean doubleClick) {
		if (super.mouseClicked(mouseButtonEvent, doubleClick)) {
			return true;
		}
		return handleDrag(mouseButtonEvent.x(), mouseButtonEvent.y());
	}

	@Override
	public boolean mouseDragged(@NonNull MouseButtonEvent mouseButtonEvent, double dx, double dy) {
		if (super.mouseDragged(mouseButtonEvent, dx, dy)) {
			return true;
		}
		return handleDrag(mouseButtonEvent.x(), mouseButtonEvent.y());
	}

	private boolean handleDrag(double mouseX, double mouseY) {
		double dx = mouseX - wheelX;
		double dy = mouseY - wheelY;
		double distance = Math.sqrt(dx * dx + dy * dy);
		if (distance <= WHEEL_RADIUS + 6) {
			hue = (float) ((Math.toDegrees(Math.atan2(dy, dx)) + 360) % 360 / 360.0);
			saturation = (float) Mth.clamp(distance / WHEEL_RADIUS, 0, 1);
			apply();
			return true;
		}
		if (mouseX >= barX - 4 && mouseX <= barX + BAR_WIDTH + 4 && mouseY >= barTop - 4 && mouseY <= barBottom + 4) {
			value = (float) Mth.clamp(1 - (mouseY - barTop) / (barBottom - barTop), 0, 1);
			apply();
			return true;
		}
		return false;
	}

	private void apply() {
		settings.arrowColour = currentRgb();
	}

	private int currentRgb() {
		return hsvToRgb(hue, saturation, value);
	}

	@Override
	public void onClose() {
		if (settings.arrowColour != originalColour) {
			Controlify.instance().config().saveSafely();
		}
		MinecraftUtil.setScreen(parent);
	}

	private void setFromRgb(int rgb) {
		float r = ((rgb >> 16) & 0xFF) / 255f;
		float g = ((rgb >> 8) & 0xFF) / 255f;
		float b = (rgb & 0xFF) / 255f;
		float max = Math.max(r, Math.max(g, b));
		float min = Math.min(r, Math.min(g, b));
		float delta = max - min;

		if (delta <= 0.0001f) {
			hue = 0;
		} else if (max == r) {
			hue = ((g - b) / delta % 6) / 6f;
		} else if (max == g) {
			hue = ((b - r) / delta + 2) / 6f;
		} else {
			hue = ((r - g) / delta + 4) / 6f;
		}
		if (hue < 0) {
			hue += 1;
		}
		saturation = max <= 0 ? 0 : delta / max;
		value = max;
		settings.arrowColour = rgb;
	}

	private static int hsvToRgb(float h, float s, float v) {
		int sector = (int) (h * 6) % 6;
		float f = h * 6 - (int) (h * 6);
		float p = v * (1 - s);
		float q = v * (1 - f * s);
		float t = v * (1 - (1 - f) * s);
		float r;
		float g;
		float b;
		switch (sector) {
			case 0 -> { r = v; g = t; b = p; }
			case 1 -> { r = q; g = v; b = p; }
			case 2 -> { r = p; g = v; b = t; }
			case 3 -> { r = p; g = q; b = v; }
			case 4 -> { r = t; g = p; b = v; }
			default -> { r = v; g = p; b = q; }
		}
		return Math.round(r * 255) << 16 | Math.round(g * 255) << 8 | Math.round(b * 255);
	}
}
