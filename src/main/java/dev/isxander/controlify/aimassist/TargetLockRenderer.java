/*
 * Copyright (C) 2026 isXander
 * This file is part of Controlify.
 *
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */
package dev.isxander.controlify.aimassist;

import dev.isxander.controlify.Controlify;
import dev.isxander.controlify.config.settings.TargetLockSettings;
import dev.isxander.controlify.utils.MinecraftUtil;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;

/**
 * Draws a marker over whatever target lock is holding.
 * <p>
 * Done as a HUD layer with the projection worked out here, rather than as a world renderer: the
 * marker wants to be a constant size on screen whatever the range, and the arithmetic below is
 * stable across versions in a way that hooking the world render pipeline is not.
 * <p>
 * Everything it projects with comes off the camera the frame was actually drawn from — its basis
 * vectors and its effective field of view. Taking any of it from settings or from the player
 * instead is what put the marker in the wrong place: view bob and third person move the camera
 * without moving the eyes, and the game widens the field of view as the player speeds up or zooms,
 * so a marker placed with the base setting drifts further out the faster you go.
 */
public final class TargetLockRenderer {
	/** Radius of the pyramid's square top, in GUI pixels. Wider than tall on purpose. */
	private static final float BASE_RADIUS = 9.5f;
	/** How far the point hangs below that top face. */
	private static final float SOLID_HEIGHT = 8.5f;

	/**
	 * How far above the horizontal the marker is viewed from. Shallow on purpose: steepen it and
	 * you end up looking down onto the top face, which then fills the silhouette and reads as a
	 * flat diamond tipped towards you rather than something hanging point down. Kept low, the top
	 * is a thin horizontal sliver and the sides and the point carry the shape.
	 */
	private static final double TILT = Math.toRadians(14);
	/** One full turn in this many milliseconds. Slow enough to read as shape, not motion. */
	private static final double SPIN_PERIOD_MS = 5200;
	/** Direction the imaginary light comes from, for shading the four sides differently. */
	private static final double LIGHT_ANGLE = Math.toRadians(-50);

	/** How far above the mob's head the point of the marker sits, in blocks. */
	private static final double HEAD_CLEARANCE = 0.05;

	/** Solid when the shot is clear, faded when something is in the way. */
	private static final float ALPHA_VISIBLE = 1.0f;
	private static final float ALPHA_OCCLUDED = 0.42f;

	private TargetLockRenderer() {
	}

	public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		Entity target = TargetLock.locked();

		if (player == null || target == null || !TargetLock.active() || MinecraftUtil.getScreen() != null) {
			return;
		}
		TargetLockSettings settings = Controlify.instance().config().getSettings().aimAssistSettings().targetLock;
		if (!settings.arrowEnabled) {
			return;
		}

		float partial = deltaTracker.getGameTimeDeltaPartialTick(false);
		Vec3 markerPos = new Vec3(
				Mth.lerp(partial, target.xOld, target.getX()),
				Mth.lerp(partial, target.yOld, target.getY()) + target.getBbHeight() + HEAD_CLEARANCE,
				Mth.lerp(partial, target.zOld, target.getZ()));

		Camera camera = minecraft.gameRenderer.mainCamera();
		Vec3 relative = markerPos.subtract(camera.position());

		// The camera hands out its own basis, so there is no trig here to get a sign wrong in.
		Vector3fc forward = camera.forwardVector();
		Vector3fc up = camera.upVector();
		Vector3fc left = camera.leftVector();

		double depth = dot(relative, forward);
		if (depth <= 0.05) {
			// Behind the camera. The compass bar is the place to say where it went, not this.
			return;
		}

		int width = graphics.guiWidth();
		int height = graphics.guiHeight();
		if (width <= 0 || height <= 0) {
			return;
		}

		// The camera's own field of view, which already carries the widening the game applies for
		// speed and any zoom in effect. The value in the options menu is only where that starts.
		double tanHalfFov = Math.tan(Math.toRadians(camera.getFov()) / 2);
		double aspect = (double) width / height;
		double screenX = width / 2.0 * (1 - (dot(relative, left) / depth) / (tanHalfFov * aspect));
		double screenY = height / 2.0 * (1 - (dot(relative, up) / depth) / tanHalfFov);

		int margin = Math.round(BASE_RADIUS * 3);
		if (screenX < -margin || screenX > width + margin || screenY < -margin || screenY > height + margin) {
			return;
		}

		float alpha = player.hasLineOfSight(target) ? ALPHA_VISIBLE : ALPHA_OCCLUDED;

		// Drawn around a local origin with the real position carried in the transform, because
		// fill only takes whole pixels. Rounding the position instead pinned the marker to the
		// pixel grid, so a mob drifting smoothly across the screen made it hop a whole GUI pixel
		// at a time in whichever axis crossed a boundary - three or four screen pixels at a
		// typical GUI scale, which is the twitching in all four directions while strafing.
		// Everything above is in GUI pixels, which at a GUI scale of 4 are four real pixels each -
		// so a shape this small ends up built out of very fat blocks. Shrinking the coordinate
		// system by that scale and building the solid the same amount larger puts one unit back on
		// one real pixel, and the edges come out as fine as the display can draw them.
		float detail = (float) Math.max(1, (double) minecraft.getWindow().getWidth() / width);

		graphics.pose().pushMatrix();
		graphics.pose().translate((float) screenX, (float) screenY - SOLID_HEIGHT);
		graphics.pose().scale(1 / detail, 1 / detail);
		draw(graphics, settings.arrowColour, alpha, detail);
		graphics.pose().popMatrix();
	}

	/**
	 * A four sided pyramid hanging point down over the mob, turning slowly so its shape reads.
	 * <p>
	 * This is real geometry rather than a drawing of some: the five corners are placed in three
	 * dimensions, spun, projected, ordered back to front and filled a scanline at a time, with each
	 * face lit by which way it happens to be facing. Doing it here rather than handing a model to
	 * the world renderer keeps the marker the same size at any range, which is the thing worth
	 * protecting — a world-space model would shrink into nothing at forty blocks.
	 */
	private static void draw(GuiGraphicsExtractor graphics, int rgb, float alpha, float detail) {
		double spin = (System.nanoTime() / 1_000_000L % (long) SPIN_PERIOD_MS) / SPIN_PERIOD_MS * Math.PI * 2;
		double cosTilt = Math.cos(TILT);
		double sinTilt = Math.sin(TILT);

		// Top corners, going round the square, plus the point underneath them.
		float radius = BASE_RADIUS * detail;
		float[] cornerX = new float[4];
		float[] cornerY = new float[4];
		float[] cornerDepth = new float[4];
		for (int i = 0; i < 4; i++) {
			double angle = spin + i * Math.PI / 2;
			double x = Math.cos(angle) * radius;
			double z = Math.sin(angle) * radius;
			cornerX[i] = (float) x;
			// Viewed from above, so depth leans into how high up the corner lands on screen.
			cornerY[i] = (float) (-z * sinTilt);
			cornerDepth[i] = (float) z;
		}
		float apexX = 0;
		float apexY = (float) (SOLID_HEIGHT * detail * cosTilt);

		int outline = argb(shade(rgb, 0.12f), alpha * 0.9f);
		drawSolid(graphics, cornerX, cornerY, cornerDepth, apexX, apexY, rgb, alpha, outline, 1.12f, true);
		drawSolid(graphics, cornerX, cornerY, cornerDepth, apexX, apexY, rgb, alpha, outline, 1.0f, false);
	}

	/**
	 * @param scale     drawn slightly large and flat dark first, which is how the whole shape gets
	 *                  an outline without working out its silhouette
	 * @param asOutline whether this pass is that dark copy
	 */
	private static void drawSolid(GuiGraphicsExtractor graphics, float[] cornerX, float[] cornerY,
	                              float[] cornerDepth, float apexX, float apexY,
	                              int rgb, float alpha, int outline, float scale, boolean asOutline) {
		// Back to front, so the near faces simply paint over the far ones and no depth test is
		// needed. A pyramid is convex, so ordering alone is enough to get it right.
		Integer[] faces = {0, 1, 2, 3};
		java.util.Arrays.sort(faces, (a, b) -> Float.compare(
				cornerDepth[b] + cornerDepth[(b + 1) % 4],
				cornerDepth[a] + cornerDepth[(a + 1) % 4]));

		for (int face : faces) {
			int next = (face + 1) % 4;
			double facing = Math.atan2(cornerY[face] + cornerY[next], cornerX[face] + cornerX[next]);
			float light = (float) (0.42 + 0.46 * Math.max(0, Math.cos(facing - LIGHT_ANGLE)));
			fillTriangle(graphics,
					new float[]{cornerX[face] * scale, cornerX[next] * scale, apexX},
					new float[]{cornerY[face] * scale, cornerY[next] * scale, apexY * scale},
					asOutline ? outline : argb(shade(rgb, light), alpha));
		}

		// The top face last: from above it is always the nearest thing on the shape.
		fillQuad(graphics, cornerX, cornerY, scale,
				asOutline ? outline : argb(lighten(rgb, 0.55f), alpha));
	}

	private static void fillQuad(GuiGraphicsExtractor graphics, float[] xs, float[] ys, float scale, int colour) {
		fillTriangle(graphics,
				new float[]{xs[0] * scale, xs[1] * scale, xs[2] * scale},
				new float[]{ys[0] * scale, ys[1] * scale, ys[2] * scale}, colour);
		fillTriangle(graphics,
				new float[]{xs[0] * scale, xs[2] * scale, xs[3] * scale},
				new float[]{ys[0] * scale, ys[2] * scale, ys[3] * scale}, colour);
	}

	/** Scanline fill, so the whole solid still needs nothing of the renderer beyond rectangles. */
	private static void fillTriangle(GuiGraphicsExtractor graphics, float[] xs, float[] ys, int colour) {
		int top = (int) Math.floor(Math.min(ys[0], Math.min(ys[1], ys[2])));
		int bottom = (int) Math.ceil(Math.max(ys[0], Math.max(ys[1], ys[2])));

		for (int y = top; y <= bottom; y++) {
			float centre = y + 0.5f;
			float left = Float.MAX_VALUE;
			float right = -Float.MAX_VALUE;

			for (int i = 0; i < 3; i++) {
				int j = (i + 1) % 3;
				float y0 = ys[i];
				float y1 = ys[j];
				if ((centre >= y0 && centre < y1) || (centre >= y1 && centre < y0)) {
					float along = (centre - y0) / (y1 - y0);
					float x = xs[i] + along * (xs[j] - xs[i]);
					left = Math.min(left, x);
					right = Math.max(right, x);
				}
			}
			if (right > left) {
				graphics.fill(Math.round(left), y, Math.round(right), y + 1, colour);
			}
		}
	}

	private static double dot(Vec3 a, Vector3fc b) {
		return a.x * b.x() + a.y * b.y() + a.z * b.z();
	}

	private static int argb(int rgb, float alpha) {
		return (Mth.clamp(Math.round(alpha * 255), 0, 255) << 24) | (rgb & 0xFFFFFF);
	}

	private static int shade(int rgb, float factor) {
		return channel(rgb, 16, factor) << 16 | channel(rgb, 8, factor) << 8 | channel(rgb, 0, factor);
	}

	private static int channel(int rgb, int shift, float factor) {
		return Mth.clamp(Math.round(((rgb >> shift) & 0xFF) * factor), 0, 255);
	}

	private static int lighten(int rgb, float amount) {
		int r = (rgb >> 16) & 0xFF;
		int g = (rgb >> 8) & 0xFF;
		int b = rgb & 0xFF;
		r = Mth.clamp(Math.round(r + (255 - r) * amount), 0, 255);
		g = Mth.clamp(Math.round(g + (255 - g) * amount), 0, 255);
		b = Mth.clamp(Math.round(b + (255 - b) * amount), 0, 255);
		return r << 16 | g << 8 | b;
	}
}
