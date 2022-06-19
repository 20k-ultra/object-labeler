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

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.Graphics;
import java.util.function.Consumer;
import java.util.concurrent.ScheduledExecutorService;

import net.runelite.client.callback.ClientThread;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.input.KeyManager;
import static net.runelite.client.RuneLite.SCREENSHOT_DIR;
import net.runelite.client.util.HotkeyListener;
import net.runelite.api.events.GameTick;
import net.runelite.client.util.ImageCapture;
import net.runelite.client.util.ImageUploadStyle;
import net.runelite.client.ui.DrawManager;

@Slf4j
@PluginDescriptor(
	name = "Object Labeler"
)
public class ObjectLabelerPlugin extends Plugin
{

	private boolean labelingActive = false;
	private int ticks = 0;
	private static final int tickTrigger = 1;

	@Inject
	private Client client;

	@Inject
	private ObjectLabelerConfig config;

	@Inject
	private ClientThread clientThread;

	@Inject
	private KeyManager keyManager;

	@Inject
	private ImageCapture imageCapture;

	@Inject
	private ScheduledExecutorService executor;

	@Inject
	private DrawManager drawManager;

	private final HotkeyListener hotkeyListener = new HotkeyListener(() -> config.hotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			toggleLabeling();
		}
	};

	@Override
	protected void startUp() throws Exception
	{
		log.info("Object Labeler started!");
		SCREENSHOT_DIR.mkdirs();
		keyManager.registerKeyListener(hotkeyListener);
	}

	@Override
	protected void shutDown() throws Exception
	{
		log.info("Object Labeler stopped!");
		keyManager.unregisterKeyListener(hotkeyListener);
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

	@Subscribe
	public void onConfigChanged(ConfigChanged configChanged)
	{
		if (!configChanged.getGroup().equals("objectlabeler"))
		{
			return;
		}

		clientThread.invoke(this::rebuild);
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		this.ticks++;

		if (this.ticks > tickTrigger) {
			this.ticks = 0;

			if (this.labelingActive && client.getGameState() != GameState.LOGIN_SCREEN)
			{
				// Only label when in game!
				label();
			}
		}
	}

	void label() {
		// Take screenshot of UI
		takeScreenshot("", config.targetObject() + " Dataset");
		// TODO Update ${config.targetObject()}.txt with new image and coordinates of all object boxes
		updateTxt(config.targetObject());
		log.info("Took screenshot!");
	}

	void updateTxt(String object) {
		// TODO how to get the file name ?
		log.info("Updating: " + config.targetObject() + ".txt file with object coordinates");
	}

	void takeScreenshot(String fileName, String subDir)
	{
		Consumer<Image> imageCallback = (img) ->
		{
			executor.submit(() -> takeScreenshot(fileName, subDir, img));
		};

		drawManager.requestNextFrameListener(imageCallback);
	}

	private void takeScreenshot(String fileName, String subDir, Image image)
	{
		BufferedImage screenshot = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
		Graphics graphics = screenshot.getGraphics();

		int gameOffsetX = 0;
		int gameOffsetY = 0;

		graphics.drawImage(image, gameOffsetX, gameOffsetY, null);
		imageCapture.takeScreenshot(screenshot, fileName, subDir, false, ImageUploadStyle.NEITHER);
	}

	void rebuild() {
		log.info("Rebuilding plugin!");
		// TODO Stop labeling
		// TODO update KeyEventListener with new key set if it changed
		// TODO Prompt user to start labelling again
	}

	private void toggleLabeling()
	{
		if (this.labelingActive) {
			this.labelingActive = false;
			log.info("Deactivated Labeler!");
		} else {
			log.info("Activate Labeler!");
			this.labelingActive = true;
		}
	}
}
