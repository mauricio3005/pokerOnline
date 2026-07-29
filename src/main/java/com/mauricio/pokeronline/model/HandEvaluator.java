package com.mauricio.pokeronline.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Avalia a melhor mão de poker de 5 cartas possível a partir de um conjunto de 5 a 7 cartas
 * (hole cards + cartas comunitárias).
 */
public final class HandEvaluator {

    private static final List<Integer> WHEEL_STRAIGHT = List.of(14, 5, 4, 3, 2);

    private HandEvaluator() {
    }

    public static EvaluatedHand evaluate(Player player, List<Card> communityCards) {
        List<Card> allCards = new ArrayList<>(player.getHoleCards());
        allCards.addAll(communityCards);
        return evaluate(allCards);
    }

    public static EvaluatedHand evaluate(List<Card> cards) {
        if (cards.size() < 5) {
            throw new IllegalArgumentException("São necessárias ao menos 5 cartas para avaliar uma mão.");
        }

        EvaluatedHand best = null;
        for (List<Card> combination : combinationsOfFive(cards)) {
            EvaluatedHand evaluated = evaluateFiveCards(combination);
            if (best == null || evaluated.compareTo(best) > 0) {
                best = evaluated;
            }
        }
        return best;
    }

    private static EvaluatedHand evaluateFiveCards(List<Card> fiveCards) {
        List<Integer> valuesDesc = fiveCards.stream()
                .map(card -> card.rank().getValue())
                .sorted(Comparator.reverseOrder())
                .toList();

        boolean isFlush = fiveCards.stream().map(Card::suit).distinct().count() == 1;
        Integer straightHighCard = detectStraightHighCard(valuesDesc);

        Map<Integer, Long> countByValue = fiveCards.stream()
                .collect(Collectors.groupingBy(card -> card.rank().getValue(), Collectors.counting()));

        List<Map.Entry<Integer, Long>> groups = new ArrayList<>(countByValue.entrySet());
        groups.sort(Comparator
                .comparing((Map.Entry<Integer, Long> entry) -> entry.getValue()).reversed()
                .thenComparing(Comparator.comparing((Map.Entry<Integer, Long> entry) -> entry.getKey()).reversed()));

        long topGroupSize = groups.get(0).getValue();
        long secondGroupSize = groups.size() > 1 ? groups.get(1).getValue() : 0;

        if (straightHighCard != null && isFlush) {
            HandRank rank = straightHighCard == 14 ? HandRank.ROYAL_FLUSH : HandRank.STRAIGHT_FLUSH;
            return new EvaluatedHand(rank, List.of(straightHighCard));
        }
        if (topGroupSize == 4) {
            int quadValue = groups.get(0).getKey();
            int kicker = groups.get(1).getKey();
            return new EvaluatedHand(HandRank.QUADRA, List.of(quadValue, kicker));
        }
        if (topGroupSize == 3 && secondGroupSize == 2) {
            return new EvaluatedHand(HandRank.FULL_HOUSE, List.of(groups.get(0).getKey(), groups.get(1).getKey()));
        }
        if (isFlush) {
            return new EvaluatedHand(HandRank.FLUSH, valuesDesc);
        }
        if (straightHighCard != null) {
            return new EvaluatedHand(HandRank.SEQUENCIA, List.of(straightHighCard));
        }
        if (topGroupSize == 3) {
            List<Integer> tiebreakers = new ArrayList<>();
            tiebreakers.add(groups.get(0).getKey());
            tiebreakers.addAll(remainingGroupKeysDesc(groups, 1));
            return new EvaluatedHand(HandRank.TRINCA, tiebreakers);
        }
        if (topGroupSize == 2 && secondGroupSize == 2) {
            int highPair = groups.get(0).getKey();
            int lowPair = groups.get(1).getKey();
            int kicker = groups.get(2).getKey();
            return new EvaluatedHand(HandRank.DOIS_PARES, List.of(highPair, lowPair, kicker));
        }
        if (topGroupSize == 2) {
            List<Integer> tiebreakers = new ArrayList<>();
            tiebreakers.add(groups.get(0).getKey());
            tiebreakers.addAll(remainingGroupKeysDesc(groups, 1));
            return new EvaluatedHand(HandRank.PAR, tiebreakers);
        }
        return new EvaluatedHand(HandRank.CARTA_ALTA, valuesDesc);
    }

    private static List<Integer> remainingGroupKeysDesc(List<Map.Entry<Integer, Long>> groups, int fromIndex) {
        return groups.subList(fromIndex, groups.size()).stream()
                .map(Map.Entry::getKey)
                .sorted(Comparator.reverseOrder())
                .toList();
    }

    private static Integer detectStraightHighCard(List<Integer> valuesDesc) {
        List<Integer> distinctDesc = valuesDesc.stream().distinct().toList();
        if (distinctDesc.size() != 5) {
            return null;
        }
        boolean consecutive = true;
        for (int i = 0; i < distinctDesc.size() - 1; i++) {
            if (distinctDesc.get(i) - distinctDesc.get(i + 1) != 1) {
                consecutive = false;
                break;
            }
        }
        if (consecutive) {
            return distinctDesc.get(0);
        }
        if (distinctDesc.equals(WHEEL_STRAIGHT)) {
            return 5;
        }
        return null;
    }

    private static List<List<Card>> combinationsOfFive(List<Card> cards) {
        List<List<Card>> combinations = new ArrayList<>();
        combine(cards, 5, 0, new ArrayList<>(), combinations);
        return combinations;
    }

    private static void combine(List<Card> cards, int size, int startIndex, List<Card> current, List<List<Card>> result) {
        if (current.size() == size) {
            result.add(new ArrayList<>(current));
            return;
        }
        for (int i = startIndex; i < cards.size(); i++) {
            current.add(cards.get(i));
            combine(cards, size, i + 1, current, result);
            current.remove(current.size() - 1);
        }
    }
}
