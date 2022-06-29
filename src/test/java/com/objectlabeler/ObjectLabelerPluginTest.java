package com.objectlabeler;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;
import org.opencv.core.Core;

public class ObjectLabelerPluginTest
{
	public static void main(String[] args) throws Exception
	{
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		ExternalPluginManager.loadBuiltin(ObjectLabelerPlugin.class);
		RuneLite.main(args);
	}
}