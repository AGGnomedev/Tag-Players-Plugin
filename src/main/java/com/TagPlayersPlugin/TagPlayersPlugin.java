package com.TagPlayersPlugin;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.MenuEntry;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import net.runelite.client.input.KeyListener;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.api.MenuAction;
import net.runelite.client.util.Text;
import net.runelite.client.game.chatbox.ChatboxPanelManager;
import javax.inject.Inject;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@PluginDescriptor(
		name = "Tag Players Plugin"
)
@Slf4j
public class TagPlayersPlugin extends Plugin {
	private static final String TAG_PLAYER = "Tag Player";

	@Inject
	private Client client;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private TagPlayersOverlay overlay;

	@Inject
	private TagPlayersConfig config;

	@Inject
	private KeyManager keyManager;

	@Inject
	private ChatboxPanelManager chatboxPanelManager;

	private boolean hotkeyPressed = false;

	private final Map<String, String> taggedPlayers = new HashMap<>();
	private static final Pattern LEVEL_PATTERN = Pattern.compile(" \\(level-\\d+\\)$");
	private static final String SAVE_FILE = "tagged_players.txt";
	private boolean tagsVisible = false; // Only used in toggle mode


	@Provides
	TagPlayersConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(TagPlayersConfig.class);
	}

	private final KeyListener inputListener = new KeyListener() {
		@Override
		public void keyTyped(KeyEvent e) {
		}

		@Override
		public void keyPressed(KeyEvent e) {
			if (config.hotkey().matches(e)) {
				if (config.toggleMode()) {
					tagsVisible = !tagsVisible;
					log.debug("Toggle mode - tagsVisible: {}", tagsVisible);
				} else {
					hotkeyPressed = true;
					log.debug("Hold mode - hotkey pressed");
				}
			}
		}


		@Override
		public void keyReleased(KeyEvent e) {
			if (!config.toggleMode() && config.hotkey().matches(e)) {
				hotkeyPressed = false;
				log.debug("Hold mode - hotkey released");
			}
		}

	};

	@Override
	protected void startUp() throws Exception {
		overlayManager.add(overlay);
		keyManager.registerKeyListener(inputListener);
		loadTags();
		log.info("Tag Players Plugin started!");
	}

	@Override
	protected void shutDown() throws Exception {
		overlayManager.remove(overlay);
		keyManager.unregisterKeyListener(inputListener);
		saveTags();
		log.info("Tag Players Plugin stopped!");
	}

	@Subscribe
	public void onMenuOpened(MenuOpened event) {
		if (!shouldShowTags()) {
			return; // Don’t add the "Tag Player" option unless tags are visible
		}

		log.debug("Menu opened");

		MenuEntry[] menuEntries = client.getMenuEntries();
		List<MenuEntry> newMenuEntries = new ArrayList<>(Arrays.asList(menuEntries));
		Set<String> processedPlayers = new HashSet<>();

		for (MenuEntry entry : menuEntries) {
			MenuAction action = entry.getType();

			if ((action == MenuAction.PLAYER_FIRST_OPTION || action == MenuAction.PLAYER_SECOND_OPTION || action == MenuAction.PLAYER_THIRD_OPTION || action == MenuAction.PLAYER_FOURTH_OPTION || action == MenuAction.PLAYER_FIFTH_OPTION || action == MenuAction.PLAYER_SIXTH_OPTION || action == MenuAction.PLAYER_SEVENTH_OPTION || action == MenuAction.PLAYER_EIGHTH_OPTION)
					&& !processedPlayers.contains(entry.getTarget())) {

				log.debug("Adding 'Tag Player' option for player: {}", entry.getTarget());

				MenuEntry newEntry = client.createMenuEntry(-1)
						.setOption(TAG_PLAYER)
						.setTarget(entry.getTarget())
						.setType(MenuAction.RUNELITE)
						.onClick(e -> log.debug("Tag Player menu entry clicked"));

				newMenuEntries.add(newEntry);
				processedPlayers.add(entry.getTarget());
			}
		}

		client.setMenuEntries(newMenuEntries.toArray(new MenuEntry[0]));
	}



	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event) {
		log.debug("Menu option clicked: option={} target={}", event.getMenuOption(), event.getMenuTarget());

		if (TAG_PLAYER.equals(event.getMenuOption())) {
			String playerName = extractPlayerName(event.getMenuTarget());
			log.debug("Tagging player: {}", playerName);
			promptForTag(playerName);
			event.consume(); // Prevents other plugins from processing this event
		}
	}

	private void promptForTag(String playerName) {
		chatboxPanelManager.openTextInput("Enter tag for " + playerName)
				.value(Optional.ofNullable(taggedPlayers.get(playerName)).orElse(""))
				.onDone((input) -> {
					log.debug("it gets here");
					input = input.trim().isEmpty() ? null : input;
					if (input != null) {
						tagPlayer(playerName, input);
						saveTags(); // Save tags immediately after tagging a player
						log.debug("Tagged player: name={} nickname={}", playerName, input);
					} else {
						untagPlayer(playerName);
						saveTags();
						log.debug("Removed tag for player: {}", playerName);
						log.debug(taggedPlayers.toString());
					}
				})
				.build();
	}

	public void tagPlayer(String playerName, String nickname) {
		playerName = extractPlayerName(playerName);
		log.debug("Normalizing player name to {}", playerName);
		taggedPlayers.put(playerName, nickname);
		log.debug("Player {} tagged as {}", playerName, nickname);
	}

	public void untagPlayer(String playerName) {
		playerName = extractPlayerName(playerName);
		taggedPlayers.remove(playerName);
	}

	public String getNickname(String playerName) {
		playerName = extractPlayerName(playerName);
		log.debug("Retrieving nickname for normalized player name: {}", playerName);
		String nickname = taggedPlayers.get(playerName);
		log.debug("Retrieved nickname: {}", nickname);
		return nickname;
	}

	private String extractPlayerName(String playerName) {
		String normalizedPlayerName = Text.removeTags(playerName).replace('\u00A0', ' ').trim();
		Matcher matcher = LEVEL_PATTERN.matcher(normalizedPlayerName);
		if (matcher.find()) {
			return matcher.replaceAll("");
		}
		return normalizedPlayerName;
	}

	public boolean isHotkeyPressed() {
		return hotkeyPressed;
	}

	private void saveTags() {
		try {
			Path path = Paths.get(SAVE_FILE);
			Files.write(path, taggedPlayers.entrySet().stream()
					.map(entry -> entry.getKey().replace(' ', '\u00A0') + ":" + entry.getValue())
					.collect(Collectors.toList()));
			log.info("Tags saved to {}", SAVE_FILE);
		} catch (IOException e) {
			log.error("Error saving tags to file", e);
		}
	}

	private void loadTags() {
		try {
			Path path = Paths.get(SAVE_FILE);
			if (Files.exists(path)) {
				List<String> lines = Files.readAllLines(path);
				for (String line : lines) {
					String[] parts = line.split(":", 2);
					if (parts.length == 2) {
						taggedPlayers.put(parts[0].replace('\u00A0', ' '), parts[1]);
					}
				}
				log.info("Tags loaded from {}", SAVE_FILE);
			}
		} catch (IOException e) {
			log.error("Error loading tags from file", e);
		}

	}
	public boolean shouldShowTags() {
		return config.toggleMode() ? tagsVisible : hotkeyPressed;
	}

}
