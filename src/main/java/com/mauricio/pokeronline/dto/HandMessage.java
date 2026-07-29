package com.mauricio.pokeronline.dto;

import com.mauricio.pokeronline.model.Card;
import com.mauricio.pokeronline.model.Player;

import java.util.List;

/**
 * Mensagem privada enviada apenas ao dono das cartas, via
 * {@code convertAndSendToUser(..., "/queue/hand", ...)}. Nunca é broadcastada.
 */
public record HandMessage(String tableId, List<Card> holeCards) {

    public static HandMessage from(String tableId, Player player) {
        return new HandMessage(tableId, player.getHoleCards());
    }
}
