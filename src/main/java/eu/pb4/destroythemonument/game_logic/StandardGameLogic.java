package eu.pb4.destroythemonument.game_logic;

import com.google.common.collect.Multimap;
import eu.pb4.destroythemonument.game.BaseGameLogic;
import eu.pb4.destroythemonument.game.GameConfig;
import eu.pb4.destroythemonument.game.PlayerData;
import eu.pb4.destroythemonument.game.Teams;
import eu.pb4.destroythemonument.map.Map;
import eu.pb4.destroythemonument.map.TeamRegions;
import eu.pb4.destroythemonument.other.DtmUtil;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.player.GameTeam;
import xyz.nucleoid.plasmid.util.PlayerRef;
import xyz.nucleoid.plasmid.widget.GlobalWidgets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class StandardGameLogic extends BaseGameLogic {
    public StandardGameLogic(GameSpace gameSpace, Map map, GlobalWidgets widgets, GameConfig config, Multimap<GameTeam, ServerPlayerEntity> players, Teams teams) {
        super(gameSpace, map, widgets, config, players, teams);

        List<Text> texts = new ArrayList<>();

        texts.add(new LiteralText("+--------------------------------------+").formatted(Formatting.DARK_GRAY));
        texts.add(new LiteralText("§6§l           Destroy The Monument").formatted(Formatting.GOLD));
        texts.add(DtmUtil.getText("message", "about").formatted(Formatting.WHITE));
        texts.add(new LiteralText("+--------------------------------------+").formatted(Formatting.DARK_GRAY));


        for (Text text : texts) {
            this.gameSpace.getPlayers().sendMessage(text);
        }
    }

    protected void maybeEliminate(GameTeam team, TeamRegions regions) {
        if (regions.getMonumentCount() <= 0) {
            for (ServerPlayerEntity player : this.gameSpace.getPlayers() ) {
                PlayerData dtmPlayer = this.participants.get(PlayerRef.of(player));
                if (dtmPlayer != null && dtmPlayer.team == team) {
                    player.setGameMode(GameMode.SPECTATOR);
                }
            }
        }
    }

    @Override
    protected boolean checkIfShouldEnd() {
        int aliveTeams = 0;

        for (GameTeam team : this.config.teams) {
            int players = 0;

            for (PlayerData dtmPlayer : this.participants.values()) {
                if (dtmPlayer.team == team) {
                    players += 1;
                }
            }
            if (this.gameMap.teamRegions.get(team).getMonumentCount() > 0 && players > 0) {
                aliveTeams += 1;
            }
        }

        return aliveTeams <= 1;
    }

    public static void open(GameSpace gameSpace, Map map, GameConfig config, Multimap<GameTeam, ServerPlayerEntity> players, Teams teams) {
        gameSpace.openGame(game -> {
            GlobalWidgets widgets = new GlobalWidgets(game);
            BaseGameLogic active = new StandardGameLogic(gameSpace, map, widgets, config, players, teams);
            active.setupGame(game, gameSpace, map, config);
        });
    }

    public WinResult checkWinResult() {
        GameTeam winners = null;
        int monumentsWinner = 0;

        for (GameTeam team : this.config.teams) {
            int monuments = this.gameMap.teamRegions.get(team).getMonumentCount();
            int players = 0;

            for (PlayerData dtmPlayer : this.participants.values()) {
                if (dtmPlayer.team == team) {
                    players += 1;
                }
            }

            if (monuments > 0 && players > 0) {
                if (winners != null) {
                    if (monuments > monumentsWinner) {
                        winners = team;
                        monumentsWinner = monuments;
                    } else if (monuments == monumentsWinner) {
                        return WinResult.no();
                    }
                } else {
                    winners = team;
                    monumentsWinner = monuments;
                }
            }
        }

        return (winners != null) ? WinResult.win(winners) : WinResult.no();
    }

    @Override
    public Collection<String> getTeamScoreboards(GameTeam team, boolean compact) {
        List<String> lines = new ArrayList<>();

        int monuments = this.gameMap.teamRegions.get(team).getMonumentCount();

        if (compact) {
            lines.add(team.getFormatting().toString() + Formatting.BOLD.toString() + (monuments == 0 ? Formatting.STRIKETHROUGH.toString() : "")  + team.getDisplay() +
                    Formatting.GRAY.toString() + " » " +
                    Formatting.WHITE.toString() + monuments +
                    Formatting.GRAY.toString() + "/" + Formatting.WHITE.toString() +
                    this.gameMap.teamRegions.get(team).monumentStartingCount +
                    Formatting.WHITE.toString());
        }
        else {
            if (monuments != 0) {
                lines.add(team.getFormatting().toString() + Formatting.BOLD.toString() + team.getDisplay() + " Team:");
                lines.add(Formatting.GRAY.toString() + "» " +
                        Formatting.WHITE.toString() + monuments +
                        Formatting.GRAY.toString() + "/" + Formatting.WHITE.toString() +
                        this.gameMap.teamRegions.get(team).monumentStartingCount +
                        Formatting.WHITE.toString() + " left"
                );
            } else {
                lines.add(team.getFormatting().toString() + Formatting.BOLD.toString()
                        + Formatting.STRIKETHROUGH.toString() + team.getDisplay() + " Team:");
                lines.add(Formatting.GRAY.toString() + "» " +
                        Formatting.WHITE.toString() + "Eliminated!"
                );
            }
            lines.add(" ");
        }

        return lines;
    }
}