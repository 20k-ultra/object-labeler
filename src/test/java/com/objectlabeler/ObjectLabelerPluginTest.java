package com.objectlabeler;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class ObjectLabelerPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(ObjectLabelerPlugin.class);
		RuneLite.main(args);
	}
}