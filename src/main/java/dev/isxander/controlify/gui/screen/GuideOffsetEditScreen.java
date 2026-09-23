/*
 * Copyright (C) 2026 isXander
 * This file is part of Controlify.
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package dev.isxander.controlify.gui.screen;

import dev.isxander.controlify.Controlify;
import dev.isxander.controlify.api.bind.InputBinding;
import dev.isxander.controlify.api.bind.InputBindingSupplier;
import dev.isxander.controlify.bindings.ControlifyBindings;
import dev.isxander.controlify.config.settings.profile.GenericControllerSettings;
import dev.isxander.controlify.controller.ControllerEntity;
import dev.isxander.controlify.gui.guide.GuideRenderer;
import dev.isxander.controlify.gui.guide.PrecomputedLines;
import dev.isxander.controlify.utils.MinecraftUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.function.IntConsumer;

/**
 * Lets the player nudge the left/right ingame button guide columns independently, with a
 * live preview rendered using the player's actual bound inputs (real glyph icons and names
 * for a representative set of bindings), at the exact position the real HUD overlay would use.
 */
public class GuideOffsetEditScreen extends Screen {
	private static final int STEP = 1;
	private static final int BUTTON_SIZE = 20;
	private static final int FOOTER_BUTTON_WIDTH = 150;
	// The real overlay only ever shows a couple of contextually-relevant lines at once, so its
	// tight betweenLines gap never gets stressed. Here every sample binding is shown together,
	// and glyph icons commonly render taller than plain text, so give lines extra breathing
	// room purely for this preview - the real in-game renderer/gap is untouched.
	private static final int PREVIEW_LINE_SPACING = 14;

	private static final int OFFSET_BOX_WIDTH = 50;
	private static final int OFFSET_BOX_HEIGHT = 16;
	private static final int OFFSET_BOX_GAP = 6;
	private static final int OFFSET_ROW_WIDTH = OFFSET_BOX_WIDTH * 2 + OFFSET_BOX_GAP;

	private static final int CORNER_BUTTON_WIDTH = 22;
	private static final int CORNER_BUTTON_HEIGHT = 13;
	private static final int CORNER_BUTTON_GAP = 2;
	private static final int CORNER_GRID_WIDTH = CORNER_BUTTON_WIDTH * 2 + CORNER_BUTTON_GAP;

	/** Height reserved above each cluster for its "Left Guides" / "Right Guides" label. */
	private static final int LABEL_HEIGHT = 14;
	/** Horizontal gap between the two clusters, centred on the screen. */
	private static final int CLUSTER_GAP = 40;

	// Mirrors the private layout constants in GuideRenderer#extractLines, so the corner-snap
	// math below lands on exactly the same pixel the real HUD overlay would use.
	private static final int SAFE_AREA_X = 2;
	private static final int SAFE_AREA_Y = 5;
	private static final int BETWEEN_LINES = 2;

	// mirrors the default out-of-the-box guide layout: jump/sneak/inventory/radial on the
	// left, attack/use on the right - see the reference screenshot for the intended look.
	private static final List<InputBindingSupplier> LEFT_BINDINGS = List.of(
			ControlifyBindings.JUMP,
			ControlifyBindings.SNEAK,
			ControlifyBindings.INVENTORY,
			ControlifyBindings.RADIAL_MENU
	);
	private static final List<InputBindingSupplier> RIGHT_BINDINGS = List.of(
			ControlifyBindings.ATTACK,
			ControlifyBindings.USE
	);

	private final Screen parent;
	private final GenericControllerSettings.GuideSettings guideSettings;
	private final ControllerEntity controller;
	private final boolean bottomAligned;

	private int leftOffsetX;
	private int leftOffsetY;
	private int rightOffsetX;
	private int rightOffsetY;

	private EditBox leftXBox;
	private EditBox leftYBox;
	private EditBox rightXBox;
	private EditBox rightYBox;

	public GuideOffsetEditScreen(Screen parent, GenericControllerSettings.GuideSettings guideSettings, ControllerEntity controller) {
		super(Component.translatable("controlify.gui.glyph_editor.title"));
		this.parent = parent;
		this.guideSettings = guideSettings;
		this.controller = controller;
		this.bottomAligned = guideSettings.ingameGuideBottom;
		this.leftOffsetX = guideSettings.ingameGuideOffsetLeftX;
		this.leftOffsetY = guideSettings.ingameGuideOffsetLeftY;
		this.rightOffsetX = guideSettings.ingameGuideOffsetRightX;
		this.rightOffsetY = guideSettings.ingameGuideOffsetRightY;
	}

	/**
	 * Geometry for the two control clusters. Both clusters sit side by side in the middle of the
	 * screen, rather than against the left and right edges, so they don't cover the guide preview
	 * in the places the guides normally sit.
	 */
	private record ClusterLayout(int leftGridX, int rightGridX, int gridY, int rowY,
								int leftRowX, int rightRowX, int cornerY,
								int leftCornerX, int rightCornerX, int gridSize) {
	}

	private ClusterLayout clusterLayout() {
		int gridSize = BUTTON_SIZE * 3;
		int clusterWidth = Math.max(gridSize, Math.max(OFFSET_ROW_WIDTH, CORNER_GRID_WIDTH));
		int cornerGridHeight = CORNER_BUTTON_HEIGHT * 2 + CORNER_BUTTON_GAP;
		int clusterHeight = LABEL_HEIGHT + gridSize + 10 + OFFSET_BOX_HEIGHT + 10 + cornerGridHeight;

		int clusterTop = height / 2 - clusterHeight / 2;
		int gridY = clusterTop + LABEL_HEIGHT;
		int leftClusterX = width / 2 - CLUSTER_GAP / 2 - clusterWidth;
		int rightClusterX = width / 2 + CLUSTER_GAP / 2;

		int rowY = gridY + gridSize + 10;
		int cornerY = rowY + OFFSET_BOX_HEIGHT + 10;

		return new ClusterLayout(
				leftClusterX + (clusterWidth - gridSize) / 2,
				rightClusterX + (clusterWidth - gridSize) / 2,
				gridY, rowY,
				leftClusterX + (clusterWidth - OFFSET_ROW_WIDTH) / 2,
				rightClusterX + (clusterWidth - OFFSET_ROW_WIDTH) / 2,
				cornerY,
				leftClusterX + (clusterWidth - CORNER_GRID_WIDTH) / 2,
				rightClusterX + (clusterWidth - CORNER_GRID_WIDTH) / 2,
				gridSize
		);
	}

	@Override
	protected void init() {
		ClusterLayout layout = clusterLayout();

		addDirectionalPad(
				layout.leftGridX(), layout.gridY(),
				() -> leftOffsetY -= STEP, () -> leftOffsetY += STEP,
				() -> leftOffsetX -= STEP, () -> leftOffsetX += STEP,
				() -> { leftOffsetX = 0; leftOffsetY = 0; }
		);
		addDirectionalPad(
				layout.rightGridX(), layout.gridY(),
				() -> rightOffsetY -= STEP, () -> rightOffsetY += STEP,
				() -> rightOffsetX -= STEP, () -> rightOffsetX += STEP,
				() -> { rightOffsetX = 0; rightOffsetY = 0; }
		);

		int rowY = layout.rowY();
		int leftRowX = layout.leftRowX();
		int rightRowX = layout.rightRowX();

		leftXBox = createOffsetBox(leftRowX, rowY, leftOffsetX, v -> leftOffsetX = v);
		leftYBox = createOffsetBox(leftRowX + OFFSET_BOX_WIDTH + OFFSET_BOX_GAP, rowY, leftOffsetY, v -> leftOffsetY = v);
		rightXBox = createOffsetBox(rightRowX, rowY, rightOffsetX, v -> rightOffsetX = v);
		rightYBox = createOffsetBox(rightRowX + OFFSET_BOX_WIDTH + OFFSET_BOX_GAP, rowY, rightOffsetY, v -> rightOffsetY = v);
		addRenderableWidget(leftXBox);
		addRenderableWidget(leftYBox);
		addRenderableWidget(rightXBox);
		addRenderableWidget(rightYBox);

		addCornerButtons(layout.leftCornerX(), layout.cornerY(), false);
		addCornerButtons(layout.rightCornerX(), layout.cornerY(), true);

		int footerY = height - 28;
		addRenderableWidget(Button.builder(Component.translatable("controlify.gui.glyph_editor.reset_all"), b -> resetAll())
				.bounds(width / 2 - FOOTER_BUTTON_WIDTH - 4, footerY, FOOTER_BUTTON_WIDTH, 20)
				.build());
		addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> commitAndClose())
				.bounds(width / 2 + 4, footerY, FOOTER_BUTTON_WIDTH, 20)
				.build());
	}

	private void addDirectionalPad(int gridX, int gridY, Runnable onUp, Runnable onDown, Runnable onLeft, Runnable onRight, Runnable onReset) {
		int s = BUTTON_SIZE;
		addRenderableWidget(Button.builder(Component.literal("▲"), b -> { onUp.run(); syncEditBoxes(); })
				.bounds(gridX + s, gridY, s, s)
				.build());
		addRenderableWidget(Button.builder(Component.literal("◄"), b -> { onLeft.run(); syncEditBoxes(); })
				.bounds(gridX, gridY + s, s, s)
				.build());
		addRenderableWidget(Button.builder(Component.literal("⟲"), b -> { onReset.run(); syncEditBoxes(); })
				.bounds(gridX + s, gridY + s, s, s)
				.tooltip(Tooltip.create(Component.translatable("controlify.gui.glyph_editor.reset_side")))
				.build());
		addRenderableWidget(Button.builder(Component.literal("►"), b -> { onRight.run(); syncEditBoxes(); })
				.bounds(gridX + s * 2, gridY + s, s, s)
				.build());
		addRenderableWidget(Button.builder(Component.literal("▼"), b -> { onDown.run(); syncEditBoxes(); })
				.bounds(gridX + s, gridY + s * 2, s, s)
				.build());
	}

	/**
	 * Builds a numeric text-entry box that lets the player type an exact offset instead of
	 * repeatedly clicking the directional pad. Accepts an empty value or a lone "-" mid-typing
	 * without touching the underlying offset, only committing once a full number is entered.
	 */
	private EditBox createOffsetBox(int x, int y, int initialValue, IntConsumer onChange) {
		EditBox box = new EditBox(font, x, y, OFFSET_BOX_WIDTH, OFFSET_BOX_HEIGHT, Component.translatable("controlify.gui.glyph_editor.offset_value"));
		box.setMaxLength(6);
		box.setValue(String.valueOf(initialValue));
		box.setResponder(text -> {
			if (text.isEmpty() || text.equals("-")) {
				return;
			}
			try {
				onChange.accept(Integer.parseInt(text));
			} catch (NumberFormatException ignored) {
				// leave the underlying offset unchanged until a complete number is typed
			}
		});
		return box;
	}

	/**
	 * Adds a 2x2 grid of small buttons that instantly snap a side's offset so its preview
	 * lands flush against the chosen screen corner.
	 */
	private void addCornerButtons(int x, int y, boolean rightColumn) {
		int w = CORNER_BUTTON_WIDTH;
		int h = CORNER_BUTTON_HEIGHT;
		int gap = CORNER_BUTTON_GAP;

		addRenderableWidget(Button.builder(Component.literal("⌜"), b -> snapToCorner(rightColumn, true, false))
				.bounds(x, y, w, h)
				.tooltip(Tooltip.create(Component.translatable("controlify.gui.glyph_editor.snap_top_left")))
				.build());
		addRenderableWidget(Button.builder(Component.literal("⌝"), b -> snapToCorner(rightColumn, true, true))
				.bounds(x + w + gap, y, w, h)
				.tooltip(Tooltip.create(Component.translatable("controlify.gui.glyph_editor.snap_top_right")))
				.build());
		addRenderableWidget(Button.builder(Component.literal("⌞"), b -> snapToCorner(rightColumn, false, false))
				.bounds(x, y + h + gap, w, h)
				.tooltip(Tooltip.create(Component.translatable("controlify.gui.glyph_editor.snap_bottom_left")))
				.build());
		addRenderableWidget(Button.builder(Component.literal("⌟"), b -> snapToCorner(rightColumn, false, true))
				.bounds(x + w + gap, y + h + gap, w, h)
				.tooltip(Tooltip.create(Component.translatable("controlify.gui.glyph_editor.snap_bottom_right")))
				.build());
	}

	/**
	 * Computes and applies the offset needed to move a side's preview flush against a screen
	 * corner, using the exact same anchor math as {@link GuideRenderer}'s real extractLines:
	 * the left column's x anchors its block's left edge, the right column's x anchors its
	 * block's right edge, and the vertical anchor flips with {@link #bottomAligned} - so we
	 * measure from whichever edge is "natural" for this profile and offset from there.
	 */
	private void snapToCorner(boolean rightColumn, boolean top, boolean rightEdge) {
		PrecomputedLines preview = buildPreviewLines(rightColumn ? RIGHT_BINDINGS : LEFT_BINDINGS, !rightColumn);
		if (preview.lines().isEmpty()) {
			return;
		}

		int lineWidth = preview.width();
		int allLinesHeight = preview.height() + (preview.lines().size() - 1) * BETWEEN_LINES;

		int offsetX;
		if (rightColumn) {
			offsetX = rightEdge ? 0 : (2 * SAFE_AREA_X + lineWidth - width);
		} else {
			offsetX = rightEdge ? (width - 2 * SAFE_AREA_X - lineWidth) : 0;
		}

		int naturalY = bottomAligned ? (height - allLinesHeight - SAFE_AREA_Y) : SAFE_AREA_Y;
		int targetY = top ? SAFE_AREA_Y : (height - allLinesHeight - SAFE_AREA_Y);
		int offsetY = targetY - naturalY;

		if (rightColumn) {
			rightOffsetX = offsetX;
			rightOffsetY = offsetY;
		} else {
			leftOffsetX = offsetX;
			leftOffsetY = offsetY;
		}
		syncEditBoxes();
	}

	/**
	 * Keeps the text boxes showing the current offsets after any change made outside of typing
	 * into them directly (directional pad, per-side reset, reset all, corner snap).
	 */
	private void syncEditBoxes() {
		if (leftXBox == null) {
			return; // not yet initialised
		}
		leftXBox.setValue(String.valueOf(leftOffsetX));
		leftYBox.setValue(String.valueOf(leftOffsetY));
		rightXBox.setValue(String.valueOf(rightOffsetX));
		rightYBox.setValue(String.valueOf(rightOffsetY));
	}

	private void resetAll() {
		leftOffsetX = 0;
		leftOffsetY = 0;
		rightOffsetX = 0;
		rightOffsetY = 0;
		syncEditBoxes();
	}

	private void commitAndClose() {
		guideSettings.ingameGuideOffsetLeftX = leftOffsetX;
		guideSettings.ingameGuideOffsetLeftY = leftOffsetY;
		guideSettings.ingameGuideOffsetRightX = rightOffsetX;
		guideSettings.ingameGuideOffsetRightY = rightOffsetY;
		Controlify.instance().config().saveSafely();
		MinecraftUtil.setScreen(parent);
	}

	@Override
	public void onClose() {
		commitAndClose();
	}

	@Override
	public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
		super.extractRenderState(graphics, mouseX, mouseY, a);

		graphics.centeredText(font, title, width / 2, 12, 0xFFFFFFFF);
		graphics.centeredText(font, Component.translatable("controlify.gui.glyph_editor.subtitle"), width / 2, 24, 0xFFA0A0A0);

		ClusterLayout layout = clusterLayout();
		int gridSize = layout.gridSize();

		graphics.centeredText(font, Component.translatable("controlify.gui.glyph_editor.left_side"), layout.leftGridX() + gridSize / 2, layout.gridY() - LABEL_HEIGHT, 0xFFFFFFFF);
		graphics.centeredText(font, Component.translatable("controlify.gui.glyph_editor.right_side"), layout.rightGridX() + gridSize / 2, layout.gridY() - LABEL_HEIGHT, 0xFFFFFFFF);

		int rowY = layout.rowY();
		int leftRowX = layout.leftRowX();
		int rightRowX = layout.rightRowX();

		graphics.centeredText(font, Component.literal("X"), leftRowX + OFFSET_BOX_WIDTH / 2, rowY - 10, 0xFFAAAAAA);
		graphics.centeredText(font, Component.literal("Y"), leftRowX + OFFSET_BOX_WIDTH + OFFSET_BOX_GAP + OFFSET_BOX_WIDTH / 2, rowY - 10, 0xFFAAAAAA);
		graphics.centeredText(font, Component.literal("X"), rightRowX + OFFSET_BOX_WIDTH / 2, rowY - 10, 0xFFAAAAAA);
		graphics.centeredText(font, Component.literal("Y"), rightRowX + OFFSET_BOX_WIDTH + OFFSET_BOX_GAP + OFFSET_BOX_WIDTH / 2, rowY - 10, 0xFFAAAAAA);

		// live preview using the player's real bound inputs, rendered with the exact same
		// positioning math the real HUD overlay uses
		PrecomputedLines leftPreview = buildPreviewLines(LEFT_BINDINGS, true);
		PrecomputedLines rightPreview = buildPreviewLines(RIGHT_BINDINGS, false);
		GuideRenderer.extractPreviewLines(graphics, leftPreview, font, width, height, bottomAligned, false, true, leftOffsetX, leftOffsetY);
		GuideRenderer.extractPreviewLines(graphics, rightPreview, font, width, height, bottomAligned, true, true, rightOffsetX, rightOffsetY);
	}

	/**
	 * @param glyphFirst whether the glyph icon is shown before the name (left column style) or
	 *                   after it (right column style), matching how the real guide composes lines.
	 */
	private PrecomputedLines buildPreviewLines(List<InputBindingSupplier> bindings, boolean glyphFirst) {
		PrecomputedLines.Builder builder = new PrecomputedLines.Builder();
		for (InputBindingSupplier supplier : bindings) {
			InputBinding binding = supplier.onOrNull(controller);
			if (binding == null) {
				continue;
			}

			Component glyph = binding.inputGlyph();
			Component name = binding.name();
			Component text = glyphFirst
					? Component.empty().append(glyph).append(" ").append(name)
					: Component.empty().append(name).append(" ").append(glyph);

			int lineWidth = font.width(text);
			builder.addLine(text, lineWidth, PREVIEW_LINE_SPACING, 0, lineWidth);
		}
		return builder.build();
	}
}
