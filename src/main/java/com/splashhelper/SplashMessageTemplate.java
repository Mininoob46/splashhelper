package com.splashhelper;

import net.minecraft.client.Minecraft;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fills in a splashmessage.txt template. Supports:
 *   {hub}        - hub number, e.g. "7"
 *   {dungeon?}   - "Dungeon " if the hub is a dungeon hub, otherwise blank
 *   {server}     - hub server name, e.g. "mega12A"
 *   {username}   - your Minecraft username
 *   plus the built-in ping placeholders.
 */
public final class SplashMessageTemplate {

	public static final String DEFAULT_TEMPLATE = String.join("\n",
		"{bbping}",
		"## {dungeon?}Hub: {hub} ({server})",
		"Splasher: {username}",
		"Where: ",
		"",
		"Above is the default splash message.",
		"You can edit this file to write your own template.",
		"",
		"Placeholders you can use:",
		"  {hub}        - the hub number",
		"  {dungeon?}   - \"Dungeon \" if it's a dungeon hub, blank otherwise",
		"  {server}     - the hub's server name",
		"  {username}   - your Minecraft username",
		"  {bbping} {bscping} {spaping} {iodping} {jbaping} - ping tags",
		"",
		"These placeholders are filled in automatically by /splashhub after you",
		"open the Hub Selector.",
		"",
		"If you're happy with this default message, delete everything below the line",
		"'Where: ' above and write your own instead."
	);

	private SplashMessageTemplate() {
	}

	public static String build(String template, Hub hub, Minecraft client) {
		String result = template;

		if (hub != null) {
			result = result
				.replace("{hub}", hub.number)
				.replace("{dungeon?}", hub.isDungeon() ? "Dungeon " : "")
				.replace("{server}", hub.serverName);
		} else {
			result = result
				.replace("{hub}", "")
				.replace("{dungeon?}", "")
				.replace("{server}", "");
		}

		String username = client.getUser() != null ? client.getUser().getName() : "";
		result = result.replace("{username}", username);

		for (Map.Entry<String, String> entry : defaultPings().entrySet()) {
			result = result.replace("{" + entry.getKey() + "}", entry.getValue());
		}

		return result;
	}

	private static Map<String, String> defaultPings() {
		Map<String, String> pings = new LinkedHashMap<>();
		pings.put("bbping", "@Splash Pings");
		pings.put("bscping", "@human");
		pings.put("spaping", "@Splash Ping");
		pings.put("iodping", "@Splash");
		pings.put("jbaping", "@2mmwjba");
		return pings;
	}
}
