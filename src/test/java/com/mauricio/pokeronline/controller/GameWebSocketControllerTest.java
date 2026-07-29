package com.mauricio.pokeronline.controller;

import com.mauricio.pokeronline.dto.StartHandRequest;
import com.mauricio.pokeronline.dto.TableStateMessage;
import com.mauricio.pokeronline.model.Player;
import com.mauricio.pokeronline.model.RoundPhase;
import com.mauricio.pokeronline.model.Table;
import com.mauricio.pokeronline.service.GameService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Sobe o servidor real numa porta aleatória, conecta como cliente STOMP de verdade
 * (mesmo protocolo que um front-end usaria) e confere que iniciar uma mão dispara
 * o broadcast do estado da mesa em {@code /topic/tables/{tableId}}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GameWebSocketControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private GameService gameService;

    @Test
    void broadcastsTableStateWhenHandStarts() throws Exception {
        Table table = gameService.createTable("Mesa de teste", 6);
        gameService.joinTable(table.getId(), "Alice", 1000);
        gameService.joinTable(table.getId(), "Bob", 1000);

        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        StompSession session = stompClient
                .connectAsync("ws://localhost:" + port + "/ws", new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);

        BlockingQueue<TableStateMessage> receivedStates = new LinkedBlockingQueue<>();
        session.subscribe("/topic/tables/" + table.getId(), new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return TableStateMessage.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                receivedStates.add((TableStateMessage) payload);
            }
        });

        session.send("/app/tables/" + table.getId() + "/start", new StartHandRequest(10, 20));

        TableStateMessage state = receivedStates.poll(5, TimeUnit.SECONDS);

        assertNotNull(state, "deveria ter recebido o broadcast do estado da mesa");
        assertEquals(table.getId(), state.tableId());
        assertEquals(RoundPhase.PRE_FLOP, state.phase());
        assertEquals(2, state.players().size());
        for (Player player : gameService.getTable(table.getId()).getPlayers()) {
            assertEquals(2, player.getHoleCards().size());
        }

        session.disconnect();
    }
}
