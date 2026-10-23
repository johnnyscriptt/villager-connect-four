package com.foldpaper.villagerconnectfour.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Draws discs and the holes they drop into from filled rectangles instead of a sprite sheet. A
 * disc is a scanline circle - one {@code fill} per row - which costs nothing at GUI sizes and
 * stays crisp at any board scale, so the mod ships no board or piece art at all.
 *
 * <p>Adapted from the Checkers sibling, whose kings needed a crown ring; nothing here is ever
 * promoted, so that branch is gone and this is now a pure drawing utility with no dependency on
 * the game core. {@link #ring} survives because the winning four get outlined.
 */
final class Discs {
	static final int YELLOW_FILL = 0xFFF0C64B, YELLOW_RIM = 0xFFA8811A, YELLOW_HI = 0x70FFF6D0;
	static final int RED_FILL = 0xFFD24B3A, RED_RIM = 0xFF8E2A20, RED_HI = 0x66FFC0B0;
	/** The empty cell behind a disc - a hole cut in the slab. */
	static final int HOLE_FILL = 0xFF17203A, HOLE_RIM = 0xFF0E1428;

	private Discs() {}

	/** Draws a disc centred in a {@code size}-square cell whose top-left corner is (x, y). */
	static void draw(GuiGraphicsExtractor gg, boolean yellow, int x, int y, int size, float alpha) {
		int fill = tint(yellow ? YELLOW_FILL : RED_FILL, alpha);
		int rim = tint(yellow ? YELLOW_RIM : RED_RIM, alpha);
		int highlight = tint(yellow ? YELLOW_HI : RED_HI, alpha);

		int radius = Math.max(2, (int) (size * 0.40));
		int cx = x + size / 2, cy = y + size / 2;
		circle(gg, cx, cy, radius, rim);
		circle(gg, cx, cy, Math.max(1, radius - Math.max(1, radius / 8)), fill);

		// A soft top-left highlight stops the disc reading as a flat blob.
		int hr = Math.max(1, radius / 3);
		circle(gg, cx - radius / 3, cy - radius / 3, hr, highlight);
	}

	/** The empty socket in the board slab. */
	static void hole(GuiGraphicsExtractor gg, int x, int y, int size) {
		int radius = Math.max(2, (int) (size * 0.40));
		int cx = x + size / 2, cy = y + size / 2;
		circle(gg, cx, cy, radius, HOLE_RIM);
		circle(gg, cx, cy, Math.max(1, radius - 1), HOLE_FILL);
	}

	/** Outlines a cell's disc - used to pick out the four that won. */
	static void outline(GuiGraphicsExtractor gg, int x, int y, int size, int colour) {
		int radius = Math.max(3, (int) (size * 0.44));
		ring(gg, x + size / 2, y + size / 2, radius, colour);
	}

	/** A filled circle, one horizontal span per row. */
	private static void circle(GuiGraphicsExtractor gg, int cx, int cy, int radius, int colour) {
		for (int dy = -radius; dy <= radius; dy++) {
			int half = (int) Math.sqrt((double) radius * radius - (double) dy * dy);
			if (half <= 0) continue;
			gg.fill(cx - half, cy + dy, cx + half, cy + dy + 1, colour);
		}
	}

	/** An unfilled circle roughly two pixels thick. */
	private static void ring(GuiGraphicsExtractor gg, int cx, int cy, int radius, int colour) {
		for (int dy = -radius; dy <= radius; dy++) {
			int outer = (int) Math.sqrt((double) radius * radius - (double) dy * dy);
			int innerR = Math.max(0, radius - 2);
			int inner = Math.abs(dy) > innerR ? 0
					: (int) Math.sqrt((double) innerR * innerR - (double) dy * dy);
			if (outer <= 0) continue;
			if (inner == 0) {
				gg.fill(cx - outer, cy + dy, cx + outer, cy + dy + 1, colour);
			} else {
				gg.fill(cx - outer, cy + dy, cx - inner, cy + dy + 1, colour);
				gg.fill(cx + inner, cy + dy, cx + outer, cy + dy + 1, colour);
			}
		}
	}

	/** Scales a colour's alpha channel, for the hover ghost and for dimming after a win. */
	private static int tint(int argb, float alpha) {
		if (alpha >= 1.0f) return argb;
		int a = (int) (((argb >>> 24) & 0xFF) * Math.clamp(alpha, 0.0f, 1.0f));
		return (a << 24) | (argb & 0x00FFFFFF);
	}
}
