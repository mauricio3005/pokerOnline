package com.mauricio.pokeronline.dto;

import com.mauricio.pokeronline.model.PlayerAction;

/**
 * Payload enviado pelo cliente para {@code /app/tables/{tableId}/actions}.
 */
public record ActionRequest(String playerId, PlayerAction action, int amount) {
}
