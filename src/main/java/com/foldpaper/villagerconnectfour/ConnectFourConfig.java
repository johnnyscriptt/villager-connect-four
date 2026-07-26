package com.foldpaper.villagerconnectfour;

import com.foldpaper.villagerconnectfour.connectfour.Geometry;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The mod's settings, in {@code config/villagerconnectfour.properties}. Written with its
 * documentation on first run so it explains itself without anyone reading the source.
 *
 * <p>Read once at startup: a game's rules should not change underneath a board that is already
 * open.
 *
 * <p>Deliberately absent: search depth, time budget, and any direct difficulty override. The
 * ladder is meant to be five opponents that think differently, and exposing its knobs would
 * flatten it back into a single number.
 */
public final class ConnectFourConfig {
	private static final String FILE_NAME = "villagerconnectfour.properties";
	private static final String FIRST_PLAYER = "first-player";
	private static final String BOARD_SIZE = "board-size";

	private static final String TEMPLATE = """
			# Villager Connect Four configuration.

			# Who drops the first disc.
			#
			#   villager   The default. Connect Four is a solved game: on the standard 7x6 board
			#              the player who moves first can force a win, opening in the centre.
			#              Giving the villager that side is what makes a Master genuinely
			#              unbeatable rather than merely strong, and what the difficulty ladder
			#              was built against.
			#
			#   player     You move first, and so you get the theoretically winning side. Play
			#              accurately enough and even a Master cannot stop you. Friendlier, and
			#              the sanctioned way past the top of the ladder.
			#
			# Whoever moves first plays yellow, so this also decides your colour.
			%s=%s

			# Board size, as WIDTHxHEIGHT. The standard game is 7x6.
			#
			# Other sizes change the game outright rather than just its scale: 7x6 is a win for
			# whoever moves first, while several smaller boards are draws with perfect play. The
			# villagers adapt, but the difficulty ladder was tuned on 7x6.
			#
			# Width and height must each be at least 4, and width * (height + 1) must not exceed
			# 64 - the board is stored in a single 64-bit word, one column per height+1 bits.
			%s=%s
			""";

	private static boolean villagerMovesFirst = true;
	private static Geometry geometry = Geometry.STANDARD;

	private ConnectFourConfig() {}

	/** True when the villager drops first, which is the default. */
	public static boolean villagerMovesFirst() {
		return villagerMovesFirst;
	}

	public static Geometry geometry() {
		return geometry;
	}

	/** Loads the config, writing a documented default file if none exists. */
	public static void load() {
		Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
		try {
			if (Files.notExists(path)) {
				write(path);
			} else {
				Map<String, String> values = read(path);
				villagerMovesFirst = parseFirstPlayer(values.getOrDefault(FIRST_PLAYER, ""));
				geometry = parseBoardSize(values.getOrDefault(BOARD_SIZE, ""));
			}
		} catch (IOException e) {
			VillagerConnectFour.LOGGER.warn("could not read {}, using defaults", path, e);
			villagerMovesFirst = true;
			geometry = Geometry.STANDARD;
		}
		VillagerConnectFour.LOGGER.info("board {}, {} moves first", geometry,
				villagerMovesFirst ? "villager" : "player");
	}

	private static Map<String, String> read(Path path) throws IOException {
		Map<String, String> values = new HashMap<>();
		for (String line : Files.readAllLines(path)) {
			String trimmed = line.trim();
			if (trimmed.startsWith("#")) continue;
			int eq = trimmed.indexOf('=');
			if (eq > 0) values.put(trimmed.substring(0, eq).trim(), trimmed.substring(eq + 1).trim());
		}
		return values;
	}

	private static boolean parseFirstPlayer(String value) {
		return switch (value.toLowerCase(Locale.ROOT)) {
			case "player" -> false;
			case "villager", "" -> true;
			default -> {
				VillagerConnectFour.LOGGER.warn("unknown {} '{}', using villager", FIRST_PLAYER, value);
				yield true;
			}
		};
	}

	private static Geometry parseBoardSize(String value) {
		if (value.isEmpty()) return Geometry.STANDARD;
		try {
			return Geometry.parse(value);
		} catch (IllegalArgumentException e) {
			VillagerConnectFour.LOGGER.warn("unusable {} '{}' ({}), using 7x6",
					BOARD_SIZE, value, e.getMessage());
			return Geometry.STANDARD;
		}
	}

	private static void write(Path path) throws IOException {
		Files.createDirectories(path.getParent());
		Files.writeString(path, TEMPLATE.formatted(
				FIRST_PLAYER, "villager", BOARD_SIZE, Geometry.STANDARD.toString()));
	}
}
