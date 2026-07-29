package com.mauricio.pokeronline.service;

import com.mauricio.pokeronline.model.EvaluatedHand;
import com.mauricio.pokeronline.model.HandEvaluator;
import com.mauricio.pokeronline.model.Player;
import com.mauricio.pokeronline.model.PlayerAction;
import com.mauricio.pokeronline.model.PlayerStatus;
import com.mauricio.pokeronline.model.Round;
import com.mauricio.pokeronline.model.RoundPhase;
import com.mauricio.pokeronline.model.Table;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Orquestra o ciclo de uma mesa de poker: cria mesas, adiciona jogadores, inicia mãos,
 * repassa ações dos jogadores ao {@link Round} correspondente e conduz a mão até o fim
 * (fold-out ou showdown), distribuindo o pote ao vencedor.
 */
@Service
public class GameService {

    private final Map<String, Table> tables = new ConcurrentHashMap<>();
    private final Map<String, Round> rounds = new ConcurrentHashMap<>();

    public Table createTable(String name, int maxSeats) {
        Table table = new Table(name, maxSeats);
        tables.put(table.getId(), table);
        return table;
    }

    public Player joinTable(String tableId, String playerName, int startingChips) {
        Table table = getTable(tableId);
        Player player = new Player(playerName, startingChips);
        table.addPlayer(player);
        return player;
    }

    public void startHand(String tableId, int smallBlind, int bigBlind) {
        Table table = getTable(tableId);
        Round round = new Round(table, smallBlind, bigBlind);
        round.start();
        rounds.put(tableId, round);
    }

    public void performAction(String tableId, String playerId, PlayerAction action, int amount) {
        Table table = getTable(tableId);
        Round round = getRound(tableId);

        round.handleAction(playerId, action, amount);

        while (!round.isHandOver() && round.isBettingRoundComplete() && round.getPhase() != RoundPhase.RIVER) {
            round.advancePhase();
        }

        if (round.isHandOver()) {
            finishHand(tableId, table, round);
        } else if (round.getPhase() == RoundPhase.RIVER && round.isBettingRoundComplete()) {
            round.advancePhase();
            finishHand(tableId, table, round);
        }
    }

    private void finishHand(String tableId, Table table, Round round) {
        List<Player> allPlayers = table.getPlayers();

        List<Integer> contributionLevels = allPlayers.stream()
                .map(Player::getTotalBetThisHand)
                .filter(amount -> amount > 0)
                .distinct()
                .sorted()
                .toList();

        int previousLevel = 0;
        for (int level : contributionLevels) {
            int contributorsAtThisLevel = (int) allPlayers.stream()
                    .filter(player -> player.getTotalBetThisHand() >= level)
                    .count();
            int potLayerAmount = (level - previousLevel) * contributorsAtThisLevel;

            List<Player> eligiblePlayers = allPlayers.stream()
                    .filter(player -> player.getTotalBetThisHand() >= level)
                    .filter(player -> player.getStatus() == PlayerStatus.ATIVO || player.getStatus() == PlayerStatus.ALL_IN)
                    .toList();

            awardPotLayer(potLayerAmount, eligiblePlayers, table, round);
            previousLevel = level;
        }

        table.clearPot();
        table.advanceDealerPosition();
        rounds.remove(tableId);
    }

    private void awardPotLayer(int amount, List<Player> eligiblePlayers, Table table, Round round) {
        if (amount <= 0 || eligiblePlayers.isEmpty()) {
            return;
        }

        List<Player> winners;
        if (eligiblePlayers.size() == 1) {
            winners = eligiblePlayers;
        } else {
            Map<Player, EvaluatedHand> evaluations = eligiblePlayers.stream()
                    .collect(Collectors.toMap(
                            player -> player,
                            player -> HandEvaluator.evaluate(player, table.getCommunityCards())));
            EvaluatedHand bestHand = evaluations.values().stream()
                    .max(Comparator.naturalOrder())
                    .orElseThrow();
            winners = eligiblePlayers.stream()
                    .filter(player -> evaluations.get(player).compareTo(bestHand) == 0)
                    .toList();
        }

        int share = amount / winners.size();
        int remainder = amount % winners.size();

        for (Player winner : winners) {
            winner.winChips(share);
        }

        List<Player> remainderOrder = round.getOrder().stream()
                .filter(winners::contains)
                .toList();
        for (int i = 0; i < remainder; i++) {
            remainderOrder.get(i % remainderOrder.size()).winChips(1);
        }
    }

    public Table getTable(String tableId) {
        Table table = tables.get(tableId);
        if (table == null) {
            throw new IllegalArgumentException("Mesa não encontrada: " + tableId);
        }
        return table;
    }

    public Round getRound(String tableId) {
        Round round = rounds.get(tableId);
        if (round == null) {
            throw new IllegalArgumentException("Não há mão em andamento na mesa: " + tableId);
        }
        return round;
    }
}
