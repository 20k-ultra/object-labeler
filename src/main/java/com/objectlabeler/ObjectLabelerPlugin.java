package com.objectlabeler;

import com.google.inject.Provides;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import net.runelite.client.callback.ClientThread;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.input.KeyManager;

import static net.runelite.client.RuneLite.SCREENSHOT_DIR;

import net.runelite.client.ui.DrawManager;
import net.runelite.client.util.HotkeyListener;
import net.runelite.api.events.GameTick;

import java.util.concurrent.ScheduledExecutorService;

@Slf4j
@PluginDescriptor(
        name = "Object Labeler"
)
public class ObjectLabelerPlugin extends Plugin {

    @Inject
    private Client client;

    @Inject
    private ObjectLabelerConfig config;

    @Inject
    private ClientThread clientThread;

    @Inject
    private KeyManager keyManager;

    @Inject
    private ScheduledExecutorService executor;

    @Inject
    private DrawManager drawManager;

    private boolean labelingActive = false;
    private int ticks = 0;
    private static final int tickTrigger = 1;

    private Annotator labeler;

    private final HotkeyListener hotkeyListener = new HotkeyListener(() -> config.hotkey()) {
        @Override
        public void hotkeyPressed() {
            toggleLabeling();
        }
    };

    @Override
    protected void startUp() throws Exception {
        log.info("Object Labeler started!");
        // Ensure screenshot directory exists
        SCREENSHOT_DIR.mkdirs();
        // Setup labeler
        this.labeler = new Annotator(executor, drawManager);
        // Begin listening for key to start labeling
        keyManager.registerKeyListener(hotkeyListener);
    }

    @Override
    protected void shutDown() throws Exception {
        log.info("Object Labeler stopped!");
        keyManager.unregisterKeyListener(hotkeyListener);
    }

    @Provides
    ObjectLabelerConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(ObjectLabelerConfig.class);
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged configChanged) {
        if (!configChanged.getGroup().equals("objectlabeler")) {
            return;
        }

        clientThread.invoke(this::rebuild);
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        this.ticks++;

        if (this.ticks > tickTrigger) {
            this.ticks = 0;

            // Only label when in game!
            if (this.labelingActive && client.getGameState() != GameState.LOGIN_SCREEN) {
                // Split targetObject config on comma if multiple are passed
                labeler.annotate(config.targetObject().toLowerCase().split(","));
            }
        }
    }

    void rebuild() {
        log.info("Rebuilding plugin!");
        // TODO Stop labeling
        // TODO update KeyEventListener with new key set if it changed
        // TODO Prompt user to start labelling again
    }

    private void toggleLabeling() {
        if (this.labelingActive) {
            this.labelingActive = false;
            log.info("Deactivated Labeler!");
        } else {
            log.info("Activate Labeler!");
            this.labelingActive = true;
        }
    }
}
