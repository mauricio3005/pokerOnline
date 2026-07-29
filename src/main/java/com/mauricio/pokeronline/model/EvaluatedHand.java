package com.mauricio.pokeronline.model;

import java.util.List;

/**
 * Resultado da avaliação de uma mão de 5 cartas: a categoria ({@link HandRank})
 * e os valores de desempate em ordem de importância (ex.: valor do par, depois kickers).
 */
public record EvaluatedHand(HandRank rank, List<Integer> tiebreakers) implements Comparable<EvaluatedHand> {

    @Override
    public int compareTo(EvaluatedHand other) {
        int rankComparison = this.rank.compareTo(other.rank);
        if (rankComparison != 0) {
            return rankComparison;
        }
        int length = Math.min(this.tiebreakers.size(), other.tiebreakers.size());
        for (int i = 0; i < length; i++) {
            int comparison = Integer.compare(this.tiebreakers.get(i), other.tiebreakers.get(i));
            if (comparison != 0) {
                return comparison;
            }
        }
        return 0;
    }
}
