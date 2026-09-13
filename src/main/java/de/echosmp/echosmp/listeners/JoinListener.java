package de.echosmp.echosmp.listeners;

import de.echosmp.echosmp.PlaytimeManager;
import de.echosmp.echosmp.ScoreboardManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Beim Join wird die Spielzeit gestartet und das Scoreboard angezeigt.
 */
public class JoinListener implements Listener {

    private final PlaytimeManager playtimeManager;
    private final ScoreboardManager scoreboardManager;

    public JoinListener(PlaytimeManager playtimeManager, ScoreboardManager scoreboardManager) {
        this.playtimeManager = playtimeManager;
        this.scoreboardManager = scoreboardManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        playtimeManager.handleJoin(event.getPlayer());
        scoreboardManager.setupPlayer(event.getPlayer());
    }
}
