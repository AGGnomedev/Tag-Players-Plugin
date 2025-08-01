package com.TagPlayersPlugin;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.TextComponent;
import net.runelite.client.util.Text;

import javax.inject.Inject;
import java.awt.*;

@Slf4j
public class TagPlayersOverlay extends Overlay {
    private final Client client;
    private final TagPlayersPlugin plugin;
    private final TagPlayersConfig config;

    @Inject
    private TagPlayersOverlay(Client client, TagPlayersPlugin plugin, TagPlayersConfig config) {
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        setPriority(OverlayPriority.HIGH);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!plugin.shouldShowTags()) {
            return null;
        }




        for (Player player : client.getPlayers()) {
            if (player != null) {
                String playerName = Text.removeTags(player.getName());
                log.debug("Processing player: {}", playerName);
                String nickname = plugin.getNickname(playerName);
                if (nickname != null) {
                    log.debug("Rendering nickname for player: {} as {}", player.getName(), nickname);
                    net.runelite.api.Point rlPoint = player.getCanvasTextLocation(graphics, nickname, player.getLogicalHeight() + 40);
                    if (rlPoint != null) {
                        Point canvasPoint = new Point(rlPoint.getX(), rlPoint.getY());
                        TextComponent textComponent = new TextComponent();
                        textComponent.setText(nickname);
                        textComponent.setPosition(canvasPoint);
                        textComponent.render(graphics);
                    } else {
                        log.debug("Player {} is off-screen or canvas point is null.", player.getName());
                    }
                }
            }
        }
        return null;
    }
}