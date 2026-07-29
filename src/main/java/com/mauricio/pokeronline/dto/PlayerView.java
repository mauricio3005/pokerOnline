package com.mauricio.pokeronline.dto;

import com.mauricio.pokeronline.model.Player;
import com.mauricio.pokeronline.model.PlayerStatus;

/**
 * Visão pública de um {@link Player}: o que qualquer jogador sentado na mesa
 * pode ver sobre os outros. Nunca inclui {@code holeCards} — essas só viajam
 * na fila privada do próprio dono (ver {@link HandMessage}).
 */
public record PlayerView(
        String id,
        String name,
        int chips,
        int currentBet,
        int totalBetThisHand,
        PlayerStatus status) {

    public static PlayerView from(Player player) {
        return new PlayerView(
                player.getId(),
                player.getName(),
                player.getChips(),
                player.getCurrentBet(),
                player.getTotalBetThisHand(),
                player.getStatus());
    }
}
