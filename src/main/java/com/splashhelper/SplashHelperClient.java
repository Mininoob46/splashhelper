package com.splashhelper;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Splash Helper
 *
 * Keeps a plain text template file at
 * .minecraft/config/splashhelper/splashmessage.txt and watches for the
 * SkyBlock/Dungeon Hub Selector GUI. The /splashhub command copies a
 * selected hub's splash message.
 *
 * (Hub Selector behaviour is ported/adapted from a 1.8.9 ChatTriggers module.)
 */
public class SplashHelperClient implements ClientModInitializer {

	private static final String MOD_ID = "splashhelper";

	private Path configDir;
	private Path splashFile;

	@Override
	public void onInitializeClient() {
		configDir = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);
		splashFile = configDir.resolve("splashmessage.txt");

		ensureSplashFileExists();
		HubSelectorHandler hubSelectorHandler = new HubSelectorHandler(splashFile);

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
			dispatcher.register(ClientCommands.literal("splashhub")
				.then(ClientCommands.argument("number", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
					.executes(context -> hubSelectorHandler.copySplashMessage(
						Integer.toString(com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "number")))))));

		hubSelectorHandler.register();
	}

	/**
	 * Creates config/splashhelper/ and splashmessage.txt if either does not
	 * already exist.
	 */
	private void ensureSplashFileExists() {
		try {
			if (!Files.isDirectory(configDir)) {
				Files.createDirectories(configDir);
			}
			if (!Files.exists(splashFile)) {
				Files.writeString(splashFile, SplashMessageTemplate.DEFAULT_TEMPLATE, StandardCharsets.UTF_8);
			}
		} catch (IOException e) {
			throw new RuntimeException("Splash Helper: could not create " + splashFile, e);
		}
	}
}
