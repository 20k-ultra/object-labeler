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
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.concurrent.ScheduledExecutorService;

import net.runelite.client.callback.ClientThread;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.input.KeyManager;

import static com.objectlabeler.OpenCv.*;
import static net.runelite.client.RuneLite.SCREENSHOT_DIR;
import net.runelite.client.util.HotkeyListener;
import net.runelite.api.events.GameTick;
import net.runelite.client.util.ImageCapture;
import net.runelite.client.util.ImageUploadStyle;
import net.runelite.client.ui.DrawManager;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;

import java.io.FileWriter;

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
		takeScreenshot("", config.targetObject() + "_dataset");
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
			executor.submit(() -> {
				try {
					takeScreenshot(fileName, subDir, img);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
		};

		drawManager.requestNextFrameListener(imageCallback);
	}

	private void takeScreenshot(String fileName, String subDir, Image image) throws IOException {
		BufferedImage screenshot = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
		Graphics graphics = screenshot.getGraphics();

		int gameOffsetX = 0;
		int gameOffsetY = 0;

		graphics.drawImage(image, gameOffsetX, gameOffsetY, null);
		imageCapture.takeScreenshot(screenshot, fileName, subDir, false, ImageUploadStyle.NEITHER);

		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

		// Convert BufferImage to Mat for opencv to process
		Mat screenshotImg = img2Mat(screenshot);
		// Get bounding boxes from image
		List<Rect> boxes = BoundingBoxes(screenshotImg, new Scalar(89, 130, 189), new Scalar(108, 255, 255));

		String objectId = "0"; // TODO support more than just 1 object labelling at a time
		StringBuilder detectionCoordinates = new StringBuilder();
		double imageWidth = screenshot.getWidth();
		double imageHeight = screenshot.getHeight();

		// LABEL FORMAT: https://github.com/AlexeyAB/Yolo_mark/issues/60
		// Calculate annotation format for each box
		for (Rect rect: boxes) {
			double width = rect.br().x - rect.tl().x;
			double height = rect.br().y - rect.tl().y;
			if (width > 25 && height > 25) {
				double x_center = width / 2;
				double y_center = height / 2;

				double x_center_norm = x_center / imageWidth;
				double y_center_norm = y_center / imageHeight;
				double width_norm = width / imageWidth;
				double height_norm = height / imageHeight;

				String annotation = String.format("%s %f %f %f %f\n", objectId, x_center_norm, y_center_norm, width_norm, height_norm);
				detectionCoordinates.append(annotation);
			} else {
				System.out.println("Ignoring this detection because it's small!");
			}
		}

		// Write lines to file if detections were found
		if (detectionCoordinates.length() > 0) {
			// TODO the name of the text file to write to is the name of the image!!!
			writeImageLabels("/home/mig/.runelite/screenshots/kiiller689x/Cows_dataset/pos.txt", detectionCoordinates);
			System.out.println("Image Labeled");
		}
	}

	private void writeImageLabels(String filename, StringBuilder labels) {
		try {
			if (!new File(filename).createNewFile()) {
				System.out.println("File exists");
				throw new IOException("File already exists, not overwriting!");
			} else {
				System.out.println("Created file!");
			}
			FileWriter myWriter = new FileWriter(filename);
			myWriter.write(String.valueOf(labels));
			myWriter.close();
			System.out.println("Successfully wrote to the file.");
		} catch (IOException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		}
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
