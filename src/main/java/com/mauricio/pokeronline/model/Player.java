package com.mauricio.pokeronline.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Player {

    private final String id;
    private final String name;
    private int chips;
    private int currentBet;
    private final List<Card> holeCards;
    private PlayerStatus status;

    public Player(String name, int chips) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.chips = chips;
        this.currentBet = 0;
        this.holeCards = new ArrayList<>(2);
        this.status = PlayerStatus.ATIVO;
    }

    public void receiveCard(Card card) {
        holeCards.add(card);
    }

    public void fold() {
        this.status = PlayerStatus.FOLDOU;
    }

    public void bet(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("O valor da aposta deve ser positivo.");
        }
        if (amount > chips) {
            throw new IllegalArgumentException("O jogador não possui fichas suficientes.");
        }
        chips -= amount;
        currentBet += amount;
        if (chips == 0) {
            status = PlayerStatus.ALL_IN;
        }
    }

    public void resetBetForNewPhase() {
        currentBet = 0;
    }

    public void resetHand() {
        holeCards.clear();
        currentBet = 0;
        if (status != PlayerStatus.DESCONECTADO) {
            status = PlayerStatus.ATIVO;
        }
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getChips() {
        return chips;
    }

    public int getCurrentBet() {
        return currentBet;
    }

    public List<Card> getHoleCards() {
        return List.copyOf(holeCards);
    }

    public PlayerStatus getStatus() {
        return status;
    }
}
