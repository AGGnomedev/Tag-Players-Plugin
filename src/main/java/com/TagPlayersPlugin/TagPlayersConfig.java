package com.TagPlayersPlugin;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Keybind;

@ConfigGroup("tagPlayers")
public interface TagPlayersConfig extends Config {
	@ConfigItem(
			keyName = "hotkey",
			name = "Tag Hotkey",
			description = "The hotkey to show tagged player nicknames"
	)
	default Keybind hotkey() {
		return Keybind.NOT_SET;
	}
	@ConfigItem(
			keyName = "toggleMode",
			name = "Toggle Mode",
			description = "If enabled, pressing the hotkey will toggle tag visibility instead of requiring it to be held"
	)
	default boolean toggleMode() {
		return false;
	}

}
