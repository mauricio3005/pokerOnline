package com.mauricio.pokeronline.dto;

/**
 * Payload enviado pelo cliente para {@code /app/tables/{tableId}/start}.
 */
public record StartHandRequest(int smallBlind, int bigBlind) {
}
