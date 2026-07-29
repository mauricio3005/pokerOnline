package com.mauricio.pokeronline.controller;

import com.mauricio.pokeronline.dto.ActionRequest;
import com.mauricio.pokeronline.dto.HandMessage;
import com.mauricio.pokeronline.dto.StartHandRequest;
import com.mauricio.pokeronline.dto.TableStateMessage;
import com.mauricio.pokeronline.model.Player;
import com.mauricio.pokeronline.model.Round;
import com.mauricio.pokeronline.model.Table;
import com.mauricio.pokeronline.service.GameService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/**
 * Ponte entre o STOMP e o {@link GameService}: recebe ações dos jogadores em
 * {@code /app/tables/{tableId}/...} e rebroadcasta o novo estado da mesa para
 * {@code /topic/tables/{tableId}} depois de cada mudança.
 *
 * As hole cards de cada jogador viajam à parte, em {@code /topic/players/{playerId}/hand} —
 * um "canal" nomeado pelo id (UUID) do jogador, não public no {@link TableStateMessage}.
 * Isso ainda não impede um cliente mal-intencionado de assinar o canal de outro jogador se
 * descobrir o id alheio; resolver isso de verdade requer autenticação por sessão (ver tarefa
 * de identidade da sessão STOMP), que ainda não existe no projeto.
 */
@Controller
public class GameWebSocketController {

    private final GameService gameService;
    private final SimpMessagingTemplate messagingTemplate;

    public GameWebSocketController(GameService gameService, SimpMessagingTemplate messagingTemplate) {
        this.gameService = gameService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/tables/{tableId}/start")
    public void startHand(@DestinationVariable String tableId, StartHandRequest request) {
        gameService.startHand(tableId, request.smallBlind(), request.bigBlind());
        broadcastState(tableId);
    }

    @MessageMapping("/tables/{tableId}/actions")
    public void handleAction(@DestinationVariable String tableId, ActionRequest request) {
        gameService.performAction(tableId, request.playerId(), request.action(), request.amount());
        broadcastState(tableId);
    }

    private void broadcastState(String tableId) {
        Table table = gameService.getTable(tableId);
        Round round = gameService.findRound(tableId);

        messagingTemplate.convertAndSend(
                "/topic/tables/" + tableId,
                TableStateMessage.from(table, round));

        for (Player player : table.getPlayers()) {
            if (!player.getHoleCards().isEmpty()) {
                messagingTemplate.convertAndSend(
                        "/topic/players/" + player.getId() + "/hand",
                        HandMessage.from(tableId, player));
            }
        }
    }
}
