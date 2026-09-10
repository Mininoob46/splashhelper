# Splash Helper

A client-side Fabric mod for creating and copying Hypixel SkyBlock and Dungeon
splash messages.

## Features

- Creates `config/splashhelper/splashmessage.txt` automatically with a starter
  template.
- Lets you write a fully custom splash message template.
- Supports template placeholders for the hub number, dungeon prefix, server
  name, Minecraft username, and ping tags.
- Detects the SkyBlock and Dungeon Hub Selector screens and parses their hub
  information.
- Highlights the single best eligible mega hub in the selector GUI, ignoring
  `0/0` entries.
- Warns about restarting hubs and mega hubs.
- Copies a hub-specific splash message with `/splashhub <number>`.

## Commands

- `/splashhub <number>` - Fill the template with the selected hub's details and
  copy it to your clipboard. Open the Hub Selector first.

## Files

- `config/splashhelper/splashmessage.txt` - Your editable splash message
  template.

## Requirements

- Minecraft `26.1`
- Fabric Loader `0.19.0` or newer
- Fabric API
- Java `25` or newer

## Building

```sh
./gradlew build
```
