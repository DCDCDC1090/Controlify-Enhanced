/*
 * Copyright (C) 2026 isXander
 * This file is part of Controlify.
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package dev.isxander.controlify.gui.devfunctions;

import dev.isxander.controlify.Controlify;
import dev.isxander.controlify.config.settings.GlobalSettings;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionGroup;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The "Dev Functions" panel in the lower part of the right-hand pane of Controlify's Global
 * Settings screen, plus the small toggle that shows or hides it.
 * <p>
 * YACL generates that screen, so the panel is attached by
 * {@code YACLScreenCategoryTabMixin}, which calls {@link #create} when the tab is built
 * (every time the screen opens or is resized) and adds {@link #visitWidgets} to the screen.
 * <p>
 * The buttons themselves come from {@link DevFunctions}.
 */
public final class DevFunctionsPanel {
	/** Only the Controlify Global Settings tab gets the panel. */
	private static final String HOST_CATEGORY_KEY = "controlify.gui.global_settings.title";

	/**
	 * The panel starts right below this option's description, which is the tallest one on the
	 * screen (an image plus several lines of text), so no description is covered by the panel.
	 */
	private static final String REFERENCE_OPTION_KEY = "controlify.gui.reach_around";
	/** Height / width of the reference option's description image (reach-around-placement.webp is 320x180). */
	private static final float REFERENCE_IMAGE_ASPECT = 180f / 320f;

	private static final int GAP = 6;
	private static final int INNER_PADDING = 4;
	private static final int BUTTON_HEIGHT = 20;
	private static final int BUTTON_SPACING = 2;
	private static final int TOGGLE_SIZE = 20;

	private final int top;
	private final Frame frame;
	private final List<Button> buttons = new ArrayList<>();
	private final List<Boolean> buttonAvailable = new ArrayList<>();
	private final Button toggle;
	private final Label toggleLabel;

	public static boolean isHost(ConfigCategory category) {
		return category.name().getContents() instanceof TranslatableContents tc
				&& HOST_CATEGORY_KEY.equals(tc.getKey());
	}

	/**
	 * @param screenWidth  width of the YACL screen
	 * @param category     the tab's category, used to measure the reference description
	 * @param tabArea      the tab area YACL gave the tab
	 * @param searchFieldY y of YACL's search box in the right pane; the toggle sits just above it
	 */
	public static DevFunctionsPanel create(int screenWidth, ConfigCategory category, ScreenRectangle tabArea, int searchFieldY) {
		// Same maths YACL uses (YACLScreen.CategoryTab) for the right pane's description area.
		int columnWidth = screenWidth / 3;
		int padding = columnWidth / 20;
		columnWidth = Math.min(columnWidth, 400);
		int paddedWidth = columnWidth - padding * 2;
		int left = screenWidth / 3 * 2 + padding;
		int descriptionTop = tabArea.top() + padding;

		Font font = Minecraft.getInstance().font;
		int top = descriptionTop + referenceDescriptionHeight(category, font, paddedWidth) + GAP;

		int toggleY = searchFieldY - 2 - TOGGLE_SIZE;
		int maxBottom = Math.max(toggleY - GAP, top);

		// The frame is only as tall as its contents, so adding or removing a button resizes the
		// panel instead of leaving a half-empty box stretched down to the toggle.
		int count = DevFunctions.all().size();
		int contentHeight = INNER_PADDING + font.lineHeight + INNER_PADDING
				+ count * BUTTON_HEIGHT + Math.max(0, count - 1) * BUTTON_SPACING
				+ INNER_PADDING;
		int bottom = Math.min(top + contentHeight, maxBottom);

		return new DevFunctionsPanel(left, top, paddedWidth, bottom, toggleY, font);
	}

	private DevFunctionsPanel(int left, int top, int width, int bottom, int toggleY, Font font) {
		this.top = top;
		this.frame = new Frame(left, top, width, bottom - top, font);

		int buttonX = left + INNER_PADDING;
		int buttonWidth = width - INNER_PADDING * 2;
		int y = top + INNER_PADDING + font.lineHeight + INNER_PADDING;
		for (DevFunctions.DevFunction function : DevFunctions.all()) {
			if (y + BUTTON_HEIGHT > bottom - INNER_PADDING) {
				// Not enough room in this window size for more buttons.
				break;
			}
			Button button = Button.builder(function.name(), btn -> function.action().run())
					.pos(buttonX, y)
					.size(buttonWidth, BUTTON_HEIGHT)
					.tooltip(Tooltip.create(function.tooltip()))
					.build();
			buttons.add(button);
			buttonAvailable.add(function.available().getAsBoolean());
			y += BUTTON_HEIGHT + BUTTON_SPACING;
		}

		this.toggle = Button.builder(Component.empty(), btn -> setShown(!isShown()))
				.pos(left, toggleY)
				.size(TOGGLE_SIZE, TOGGLE_SIZE)
				.tooltip(Tooltip.create(Component.translatable("controlify.gui.dev_functions.toggle.tooltip")))
				.build();
		this.toggleLabel = new Label(left + TOGGLE_SIZE + 4, toggleY, width - TOGGLE_SIZE - 4, TOGGLE_SIZE,
				Component.translatable("controlify.gui.dev_functions.toggle"), font);

		applyVisibility();
	}

	/** Adds the panel's widgets to the screen, in draw order (frame behind the buttons). */
	public void visitWidgets(Consumer<AbstractWidget> consumer) {
		consumer.accept(frame);
		buttons.forEach(consumer);
		consumer.accept(toggle);
		consumer.accept(toggleLabel);
	}

	/** y of the panel's top edge; the description area above is kept above this while the panel is shown. */
	public int top() {
		return top;
	}

	public boolean isShown() {
		return settings().showDevFunctions;
	}

	private void setShown(boolean shown) {
		settings().showDevFunctions = shown;
		Controlify.instance().config().saveSafely();
		applyVisibility();
	}

	private void applyVisibility() {
		boolean shown = isShown();
		frame.visible = shown;
		for (int i = 0; i < buttons.size(); i++) {
			Button button = buttons.get(i);
			// Invisible and inactive: not drawn, can't be clicked, can't be reached with a controller.
			button.visible = shown;
			button.active = shown && buttonAvailable.get(i);
		}
		toggle.setMessage(Component.literal(shown ? "✔" : ""));
	}

	private static GlobalSettings settings() {
		return Controlify.instance().config().getSettings().globalSettings();
	}

	/** Height YACL uses to draw the reference option's description (name, image, wrapped text). */
	private static int referenceDescriptionHeight(ConfigCategory category, Font font, int width) {
		@Nullable Option<?> reference = null;
		for (OptionGroup group : category.groups()) {
			for (Option<?> option : group.options()) {
				if (option.name().getContents() instanceof TranslatableContents tc
						&& REFERENCE_OPTION_KEY.equals(tc.getKey())) {
					reference = option;
				}
			}
		}

		int nameHeight = font.lineHeight + 5;
		int imageHeight = (int) (width * REFERENCE_IMAGE_ASPECT) + 5;
		if (reference == null) {
			// Fall back to an image and four lines of text if the option ever goes away.
			return nameHeight + imageHeight + font.lineHeight * 4;
		}
		int textHeight = font.split(reference.description().text(), width).size() * font.lineHeight;
		return nameHeight + imageHeight + textHeight;
	}

	/** Background, border and title of the panel. Not interactive. */
	private static final class Frame extends AbstractWidget {
		private final Font font;

		Frame(int x, int y, int width, int height, Font font) {
			super(x, y, width, height, Component.translatable("controlify.gui.dev_functions.title"));
			this.font = font;
			this.active = false;
		}

		@Override
		public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
			graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 0x60000000);
			graphics.outline(getX(), getY(), getWidth(), getHeight(), 0xFF5A5A5A);
			graphics.text(font, getMessage(), getX() + INNER_PADDING, getY() + INNER_PADDING, 0xFFAAAAAA);
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput output) {
			output.add(NarratedElementType.TITLE, getMessage());
		}
	}

	/** Text shown next to the toggle. Not interactive. */
	private static final class Label extends AbstractWidget {
		private final Font font;

		Label(int x, int y, int width, int height, Component text, Font font) {
			super(x, y, width, height, text);
			this.font = font;
			this.active = false;
		}

		@Override
		public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
			graphics.text(font, getMessage(), getX(), getY() + (getHeight() - font.lineHeight) / 2 + 1, 0xFFFFFFFF);
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput output) {
			output.add(NarratedElementType.TITLE, getMessage());
		}
	}
}
