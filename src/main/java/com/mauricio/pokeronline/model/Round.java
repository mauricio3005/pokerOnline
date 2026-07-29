package com.mauricio.pokeronline.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Round {

    private final Table table;
    private final int smallBlind;
    private final int bigBlind;

    private List<Player> order;
    private final Set<String> actedThisPhase;
    private RoundPhase phase;
    private int actingIndex;
    private int currentBet;

    public Round(Table table, int smallBlind, int bigBlind) {
        this.table = table;
        this.smallBlind = smallBlind;
        this.bigBlind = bigBlind;
        this.actedThisPhase = new HashSet<>();
    }

    public void start() {
        table.startNewHand();

        List<Player> seated = table.getPlayers();
        int n = seated.size();
        int firstToAct = (table.getDealerPosition() + 1) % n;
        this.order = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            order.add(seated.get((firstToAct + i) % n));
        }

        this.phase = RoundPhase.PRE_FLOP;
        this.currentBet = 0;
        actedThisPhase.clear();
        postBlinds();
    }

    private void postBlinds() {
        int n = order.size();
        Player smallBlindPlayer = order.get(0);
        Player bigBlindPlayer = order.get(1 % n);

        int smallBlindAmount = Math.min(smallBlind, smallBlindPlayer.getChips());
        smallBlindPlayer.bet(smallBlindAmount);
        table.addToPot(smallBlindAmount);

        int bigBlindAmount = Math.min(bigBlind, bigBlindPlayer.getChips());
        bigBlindPlayer.bet(bigBlindAmount);
        table.addToPot(bigBlindAmount);

        currentBet = Math.max(smallBlindAmount, bigBlindAmount);
        actingIndex = findActableFrom(2 % n);
    }

    public void handleAction(String playerId, PlayerAction action, int amount) {
        Player player = currentPlayer();
        if (player == null || !player.getId().equals(playerId)) {
            throw new IllegalStateException("Não é a vez desse jogador.");
        }

        switch (action) {
            case FOLD -> player.fold();
            case CHECK -> {
                if (player.getCurrentBet() != currentBet) {
                    throw new IllegalStateException("Não é possível dar check havendo aposta pendente.");
                }
            }
            case CALL -> {
                int needed = currentBet - player.getCurrentBet();
                int callAmount = Math.min(needed, player.getChips());
                if (callAmount > 0) {
                    player.bet(callAmount);
                    table.addToPot(callAmount);
                }
            }
            case RAISE -> {
                if (amount <= currentBet) {
                    throw new IllegalArgumentException("O valor do raise deve ser maior que a aposta atual.");
                }
                int delta = amount - player.getCurrentBet();
                player.bet(delta);
                table.addToPot(delta);
                currentBet = amount;
                actedThisPhase.clear();
            }
            case ALL_IN -> {
                int delta = player.getChips();
                player.bet(delta);
                table.addToPot(delta);
                if (player.getCurrentBet() > currentBet) {
                    currentBet = player.getCurrentBet();
                    actedThisPhase.clear();
                }
            }
        }

        if (action != PlayerAction.FOLD) {
            actedThisPhase.add(player.getId());
        }
        advanceActingIndex();
    }

    public void advancePhase() {
        if (!isBettingRoundComplete()) {
            throw new IllegalStateException("A rodada de apostas ainda não terminou.");
        }
        if (phase == RoundPhase.SHOWDOWN) {
            throw new IllegalStateException("A mão já chegou ao showdown.");
        }

        currentBet = 0;
        actedThisPhase.clear();
        for (Player player : order) {
            player.resetBetForNewPhase();
        }

        switch (phase) {
            case PRE_FLOP -> {
                table.dealFlop();
                phase = RoundPhase.FLOP;
            }
            case FLOP -> {
                table.dealTurn();
                phase = RoundPhase.TURN;
            }
            case TURN -> {
                table.dealRiver();
                phase = RoundPhase.RIVER;
            }
            case RIVER -> phase = RoundPhase.SHOWDOWN;
            case SHOWDOWN -> throw new IllegalStateException("A mão já chegou ao showdown.");
        }

        if (phase != RoundPhase.SHOWDOWN) {
            actingIndex = findActableFrom(0);
        }
    }

    public boolean isBettingRoundComplete() {
        if (isHandOver()) {
            return true;
        }
        List<Player> canAct = order.stream().filter(p -> p.getStatus() == PlayerStatus.ATIVO).toList();
        if (canAct.isEmpty()) {
            return true;
        }
        return canAct.stream().allMatch(p -> actedThisPhase.contains(p.getId()) && p.getCurrentBet() == currentBet);
    }

    public boolean isHandOver() {
        long stillIn = order.stream()
                .filter(p -> p.getStatus() == PlayerStatus.ATIVO || p.getStatus() == PlayerStatus.ALL_IN)
                .count();
        return stillIn <= 1;
    }

    public Player currentPlayer() {
        if (actingIndex < 0) {
            return null;
        }
        return order.get(actingIndex);
    }

    private void advanceActingIndex() {
        actingIndex = findActableFrom((actingIndex + 1) % order.size());
    }

    private int findActableFrom(int startIndex) {
        int n = order.size();
        for (int step = 0; step < n; step++) {
            int idx = (startIndex + step) % n;
            if (order.get(idx).getStatus() == PlayerStatus.ATIVO) {
                return idx;
            }
        }
        return -1;
    }

    public RoundPhase getPhase() {
        return phase;
    }

    public int getCurrentBet() {
        return currentBet;
    }

    public List<Player> getOrder() {
        return List.copyOf(order);
    }
}
