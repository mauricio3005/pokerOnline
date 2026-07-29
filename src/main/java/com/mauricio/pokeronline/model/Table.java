package com.mauricio.pokeronline.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Table {

    private static final int HOLE_CARDS_PER_PLAYER = 2;

    private final String id;
    private final String name;
    private final int maxSeats;
    private final List<Player> players;
    private final List<Card> communityCards;
    private Deck deck;
    private int pot;
    private int dealerPosition;

    public Table(String name, int maxSeats) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.maxSeats = maxSeats;
        this.players = new ArrayList<>(maxSeats);
        this.communityCards = new ArrayList<>(5);
        this.pot = 0;
        this.dealerPosition = 0;
    }

    public void addPlayer(Player player) {
        if (players.size() >= maxSeats) {
            throw new IllegalStateException("A mesa já está cheia.");
        }
        players.add(player);
    }

    public void removePlayer(String playerId) {
        players.removeIf(player -> player.getId().equals(playerId));
    }

    public void startNewHand() {
        if (players.size() < 2) {
            throw new IllegalStateException("São necessários ao menos 2 jogadores para iniciar uma mão.");
        }
        this.deck = new Deck();
        deck.shuffle();
        communityCards.clear();
        pot = 0;

        for (Player player : players) {
            player.resetHand();
        }

        for (int i = 0; i < HOLE_CARDS_PER_PLAYER; i++) {
            for (Player player : players) {
                if (player.getStatus() != PlayerStatus.DESCONECTADO) {
                    player.receiveCard(deck.draw());
                }
            }
        }
    }

    public void dealFlop() {
        requireCommunityCardCount(0);
        for (int i = 0; i < 3; i++) {
            communityCards.add(deck.draw());
        }
    }

    public void dealTurn() {
        requireCommunityCardCount(3);
        communityCards.add(deck.draw());
    }

    public void dealRiver() {
        requireCommunityCardCount(4);
        communityCards.add(deck.draw());
    }

    private void requireCommunityCardCount(int expected) {
        if (deck == null) {
            throw new IllegalStateException("A mão ainda não foi iniciada.");
        }
        if (communityCards.size() != expected) {
            throw new IllegalStateException("Etapa de distribuição de cartas fora de ordem.");
        }
    }

    public void addToPot(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("O valor adicionado ao pote deve ser positivo.");
        }
        pot += amount;
    }

    public void clearPot() {
        pot = 0;
    }

    public void advanceDealerPosition() {
        if (players.isEmpty()) {
            return;
        }
        dealerPosition = (dealerPosition + 1) % players.size();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getMaxSeats() {
        return maxSeats;
    }

    public List<Player> getPlayers() {
        return List.copyOf(players);
    }

    public List<Card> getCommunityCards() {
        return List.copyOf(communityCards);
    }

    public int getPot() {
        return pot;
    }

    public int getDealerPosition() {
        return dealerPosition;
    }
}
