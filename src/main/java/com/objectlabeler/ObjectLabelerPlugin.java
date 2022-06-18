package com.objectlabeler;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
	name = "Object Labeler"
)
public class ObjectLabelerPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ObjectLabelerConfig config;

	@Override
	protected void startUp() throws Exception
	{
		log.info("Object Labeler started!");
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("Object Labeler stopped!");
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		log.info(String.valueOf(gameStateChanged.getGameState()));
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Object Labeler says " + config.targetObject(), null);
		}
	}

	@Provides
	ObjectLabelerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ObjectLabelerConfig.class);
	}
}
