package com.objectlabeler;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("objectlabeler")
public interface ObjectLabelerConfig extends Config
{
	@ConfigItem(
		keyName = "targetObject",
		name = "Target",
		description = "Object you want to label"
	)
	default String targetObject()
	{
		return "";
	}
}
