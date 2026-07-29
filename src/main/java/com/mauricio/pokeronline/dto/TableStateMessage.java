package com.mauricio.pokeronline.dto;

import com.mauricio.pokeronline.model.Card;
import com.mauricio.pokeronline.model.Round;
import com.mauricio.pokeronline.model.RoundPhase;
import com.mauricio.pokeronline.model.Table;

import java.util.List;

/**
 * Estado público de uma mesa, transmitido via broadcast em {@code /topic/tables/{tableId}}
 * a todos os inscritos. {@code round} pode ser {@code null} quando nenhuma mão está em
 * andamento (mesa aguardando jogadores).
 */
public record TableStateMessage(
        String tableId,
        List<PlayerView> players,
        List<Card> communityCards,
        int pot,
        int dealerPosition,
        RoundPhase phase,
        String currentPlayerId,
        int currentBet) {

    public static TableStateMessage from(Table table, Round round) {
        List<PlayerView> players = table.getPlayers().stream()
                .map(PlayerView::from)
                .toList();

        if (round == null) {
            return new TableStateMessage(
                    table.getId(), players, table.getCommunityCards(), table.getPot(),
                    table.getDealerPosition(), null, null, 0);
        }

        var current = round.currentPlayer();
        return new TableStateMessage(
                table.getId(), players, table.getCommunityCards(), table.getPot(),
                table.getDealerPosition(), round.getPhase(),
                current == null ? null : current.getId(), round.getCurrentBet());
    }
}
