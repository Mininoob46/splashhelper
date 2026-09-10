package com.splashhelper;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ported (loosely) from a 1.8.9 ChatTriggers "Hub Selector" module: detects the
 * SkyBlock/Dungeon Hub Selector GUI, parses the hub list, displays the emptiest
 * hub(s), and warns about restarting/mega hubs. The /splashhub command fills
 * out the splashmessage.txt template for a parsed hub and copies it to the
 * clipboard.
 */
public class HubSelectorHandler {

	// Matches an unformatted "§a" / "§c" colour code directly followed by the hub's area + number,
	// e.g. "§aSkyBlock Hub #7". Hypixel typically bakes legacy colour codes straight into these names.
	private static final Pattern ITEM_NAME_PATTERN =
		Pattern.compile("^(?:§(a|c))?(Dungeon|SkyBlock) Hub #(\\d{1,2})$");
	private static final Pattern PLAYERS_PATTERN =
		Pattern.compile("Players: (\\d{1,2})/(\\d{1,2})");
	private static final Pattern SERVER_PATTERN =
		Pattern.compile("^(?:§[0-9a-fk-or])*Server: (.*)$");
	private static final Pattern MEGA_JOIN_PATTERN =
			Pattern.compile("^Request join for Hub (.*)\\.\\.\\.$");

	// Slot indices (into the menu's slot list) where hub entries appear in the selector GUI.
	private static final int[] HUB_SLOTS = {
		10, 11, 12, 13, 14, 15, 16,
		19, 20, 21, 22, 23, 24, 25,
		28, 29, 30, 31, 32, 33, 34,
		37, 38, 39, 40, 41, 42, 43
	};
	private static final ItemStack BEST_HUB_MARKER = new ItemStack(Items.LIME_CONCRETE);

	private final Path splashFile;

	private boolean inHubSelector = false;
	private List<Hub> hubs = new ArrayList<>();
	private List<Hub> bestHubs = new ArrayList<>();

	public HubSelectorHandler(Path splashFile) {
		this.splashFile = splashFile;
	}

	public void register() {
		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (!(screen instanceof AbstractContainerScreen<?> containerScreen)) {
				inHubSelector = false;
				return;
			}

			String title = screen.getTitle().getString();
			inHubSelector = title.equals("SkyBlock Hub Selector") || title.equals("Dungeon Hub Selector");
			if (!inHubSelector) {
				return;
			}

			refreshHubs(containerScreen);
			ScreenEvents.afterExtract(screen).register((renderedScreen, graphics, mouseX, mouseY, tickProgress) -> {
				if (renderedScreen instanceof AbstractContainerScreen<?> updatedScreen && inHubSelector) {
					highlightBestHub(updatedScreen, graphics);
				}
			});
			ScreenEvents.afterTick(screen).register(tickedScreen -> {
				if (inHubSelector && hubs.isEmpty() && tickedScreen instanceof AbstractContainerScreen<?> updatedScreen) {
					refreshHubs(updatedScreen);
				}
			});

			ScreenEvents.remove(screen).register(s -> inHubSelector = false);
		});

		ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
			Matcher match = MEGA_JOIN_PATTERN.matcher(message.getString());
			if (match.matches() && match.group(1).toLowerCase(Locale.ROOT).startsWith("mega")) {
				chat("§aYou're warping into a mega hub! (" + match.group(1) + ")");
			}
		});
	}

	// ------------------------------------------------------------------
	// Parsing the hub list
	// ------------------------------------------------------------------

	private void refreshHubs(AbstractContainerScreen<?> screen) {
		AbstractContainerMenu menu = screen.getMenu();
		List<Hub> parsed = new ArrayList<>();

		for (int slotIndex : HUB_SLOTS) {
			if (slotIndex >= menu.slots.size()) {
				continue;
			}
			ItemStack stack = menu.getSlot(slotIndex).getItem();
			if (stack.isEmpty()) {
				continue;
			}
			Hub hub = parseHub(stack, slotIndex);
			if (hub != null) {
				parsed.add(hub);
			}
		}

		hubs = parsed;
		List<Hub> eligibleHubs = parsed.stream()
			.filter(hub -> hub.maxPlayers > 0 && hub.serverName.toLowerCase(Locale.ROOT).contains("mega"))
			.toList();
		bestHubs = new ArrayList<>();
		if (eligibleHubs.isEmpty()) {
			return;
		}

		Hub bestHub = eligibleHubs.stream().max((a, b) -> Integer.compare(a.freeSlots, b.freeSlots)).orElseThrow();
		bestHubs.add(bestHub);

		long restartingCount = hubs.stream().filter(Hub::isRestarting).count();

		if (restartingCount > 0) {
			chat("§cDetected " + restartingCount + " restarting hub(s) - they may shift, "
				+ "wait before picking a hub to splash in.");
		}
	}

	private void highlightBestHub(AbstractContainerScreen<?> screen, GuiGraphicsExtractor graphics) {
		if (bestHubs.isEmpty()) {
			return;
		}
		Hub bestHub = bestHubs.get(0);
		if (bestHub.slotIndex >= screen.getMenu().slots.size()) {
			return;
		}
		var slot = screen.getMenu().getSlot(bestHub.slotIndex);
		int left = (screen.width - 176) / 2;
		int top = (screen.height - 166) / 2;
		int itemX = left + slot.x + 1;
		int itemY = top + slot.y + 1;
		graphics.fill(itemX, itemY, itemX + 16, itemY + 16, 0xFF202020);
		graphics.item(BEST_HUB_MARKER, itemX, itemY);
	}

	private Hub parseHub(ItemStack stack, int slotIndex) {
		Matcher nameMatch = ITEM_NAME_PATTERN.matcher(stack.getHoverName().getString());
		if (!nameMatch.matches()) {
			return null;
		}
		String colour = nameMatch.group(1) == null ? "a" : nameMatch.group(1);
		String area = nameMatch.group(2);
		String number = nameMatch.group(3);

		Integer players = null;
		Integer maxPlayers = null;
		String server = null;

		ItemLore lore = stack.getOrDefault(DataComponents.LORE, ItemLore.EMPTY);
		for (Component line : lore.lines()) {
			String text = line.getString();

			Matcher playersMatch = PLAYERS_PATTERN.matcher(text);
			if (playersMatch.find()) {
				players = Integer.parseInt(playersMatch.group(1));
				maxPlayers = Integer.parseInt(playersMatch.group(2));
			}

			Matcher serverMatch = SERVER_PATTERN.matcher(text);
			if (serverMatch.matches()) {
				server = serverMatch.group(1);
			}
		}

		if (players == null || maxPlayers == null || server == null) {
			return null;
		}
		return new Hub(colour, area, number, server, players, maxPlayers, slotIndex);
	}

	public int copySplashMessage(String number) {
		Minecraft client = Minecraft.getInstance();
		if (client.player == null) {
			return 0;
		}

		if (client.screen instanceof AbstractContainerScreen<?> containerScreen
			&& isHubSelector(containerScreen)) {
			refreshHubs(containerScreen);
		}

		Hub hub = hubs.stream().filter(candidate -> candidate.number.equals(number)).findFirst().orElse(null);
		if (hub == null) {
			chat("§cSplash Helper doesn't know hub " + number + ". Open the Hub Selector first.");
			return 0;
		}

		String template;
		try {
			ensureSplashFileExists();
			template = Files.readString(splashFile, StandardCharsets.UTF_8);
		} catch (IOException e) {
			chat("§cSplash Helper couldn't read splashmessage.txt: " + e.getMessage());
			return 0;
		}

		String message = SplashMessageTemplate.build(template, hub, client);
		String hubText = (hub.isDungeon() ? "Dungeon " : "") + "Hub " + hub.number + " (" + hub.serverName + ")";

		client.keyboardHandler.setClipboard(message);
		chat("§aCopied splash message for §f" + hubText + " §ato your clipboard.");

		return 1;
	}

	private boolean isHubSelector(AbstractContainerScreen<?> screen) {
		String title = screen.getTitle().getString();
		return title.equals("SkyBlock Hub Selector") || title.equals("Dungeon Hub Selector");
	}

	private void ensureSplashFileExists() throws IOException {
		if (splashFile.getParent() != null && !Files.isDirectory(splashFile.getParent())) {
			Files.createDirectories(splashFile.getParent());
		}
		if (!Files.exists(splashFile)) {
			Files.writeString(splashFile, SplashMessageTemplate.DEFAULT_TEMPLATE, StandardCharsets.UTF_8);
		}
	}

	private void chat(String legacyText) {
		Minecraft client = Minecraft.getInstance();
		if (client.gui != null) {
			client.gui.getChat().addClientSystemMessage(Component.literal(legacyText));
		}
	}

}
