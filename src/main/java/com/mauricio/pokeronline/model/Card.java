package com.mauricio.pokeronline.model;

public record Card(Rank rank, Suit suit) {

    @Override
    public String toString() {
        return rank + " de " + suit;
    }
}
