package com.objectlabeler;

import java.awt.event.KeyEvent;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Keybind;

@ConfigGroup("objectlabeler")
public interface ObjectLabelerConfig extends Config
{
	@ConfigItem(
			keyName = "targetObject",
			name = "Target",
			description = "Object you want to label",
			position = 0
	)
	default String targetObject()
	{
		return "";
	}

	@ConfigItem(
			keyName = "hotkey",
			name = "Record hotkey",
			description = "When you press this key the plugin will start to label objects",
			position = 1
	)
	default Keybind hotkey()
	{
		return new Keybind(KeyEvent.CTRL_DOWN_MASK, 1);
	}

}
