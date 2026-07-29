# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

A Texas Hold'em poker server built with Spring Boot, written to build Java/Spring skills. Spring acts as the game server: it coordinates turns, evaluates winning hands, and manages table state. Base package: `com.mauricio.pokeronline`.

## Commands

Windows environment — use the `mvnw.cmd` wrapper (or `mvnw` from Bash/Git Bash).

```
mvnw.cmd spring-boot:run          # run the app
mvnw.cmd test                     # run all tests
mvnw.cmd test -Dtest=ClassName    # run a single test class
mvnw.cmd compile                  # compile only
```

Do not run build/compile commands proactively — the user prefers to compile/test themselves; only do it if explicitly asked.

## Architecture

The game engine is plain, mutable Java objects in `model/` — no JPA entities. This is intentional: a hand mutates state many times per second (bets, folds, streets), and driving that through Hibernate-managed entities would add overhead with no benefit since this state is transient for the duration of a hand.

JPA/H2 (already wired in `pom.xml` and `application.properties`, in-memory `jdbc:h2:mem:pokeronline`, console at `/h2-console`) is reserved for state that must outlive a hand/session — e.g. a future `Account`/`User` entity holding login and bankroll, and hand-history logging. No such entities exist yet. A game-domain `Player` (in-memory, per-table) is deliberately kept separate from a persisted account — a `Player` would reference an account id, not be one.

### Model layer (`model/`)

Core flow: `Deck` → `Table` → `Round` → `HandEvaluator`.

- **`Card`** — record of `Rank` (enum, numeric `value` 2–14, Ace high) + `Suit` (enum).
- **`Deck`** — constructor builds all 52 `Card`s; `shuffle()`, `draw()` (removes from the end of the list), `remaining()`.
- **`Player`** — mutable per-hand game state: `chips`, `currentBet` (amount committed *in the current betting phase*, reset via `resetBetForNewPhase()`), `totalBetThisHand` (cumulative across the whole hand, used for side-pot math, reset only in `resetHand()`), `holeCards`, `status` (`PlayerStatus`: `ATIVO`, `FOLDOU`, `ALL_IN`, `DESCONECTADO`). `getHoleCards()` returns a defensive copy.
- **`Table`** — holds seated `Player`s, the active `Deck`, `communityCards`, `pot`, `dealerPosition`. `startNewHand()` deals hole cards; `dealFlop()/dealTurn()/dealRiver()` enforce street order via `requireCommunityCardCount`. `clearPot()`/`advanceDealerPosition()` are called by `GameService` after a hand resolves.
- **`Round`** — orchestrates one hand's betting: turn order (`order`, rotated to start at the small blind, i.e. the seat after the dealer), blind posting, and phase progression (`RoundPhase`: `PRE_FLOP` → `FLOP` → `TURN` → `RIVER` → `SHOWDOWN`). `handleAction(playerId, PlayerAction, amount)` applies `FOLD`/`CHECK`/`CALL`/`RAISE`/`ALL_IN`; a raise (or an all-in that exceeds the current bet) clears `actedThisPhase` so action reopens for everyone. `isBettingRoundComplete()` is true once only one player remains in the hand, or everyone who can still act has matched the current bet. `advancePhase()` refuses to run until the betting round is closed.
  - Known simplification: heads-up (2-player) post-flop action always starts from the small blind seat; the special heads-up rule (dealer acts first post-flop) is not implemented.
- **`HandEvaluator`** — pure static utility (private constructor, all methods `static`) that evaluates the best 5-card hand out of 5–7 cards by brute-forcing all `C(n,5)` combinations and keeping the max `EvaluatedHand`. Per 5-card combination it checks, in priority order: straight+flush → four of a kind → full house → flush → straight → three of a kind → two pair → pair → high card. Handles the wheel straight (A-2-3-4-5, where the Ace counts low and 5 is the effective high card) as a special case since `Rank.AS` has `value = 14`.
- **`EvaluatedHand`** — record of `HandRank` (enum ordered weakest→strongest, so `compareTo` on the enum itself expresses hand strength) + `tiebreakers` (a `List<Integer>` whose contents depend on the hand rank — e.g. `[pairValue, kicker, kicker, kicker]` for a pair, just `[highCard]` for a straight, all 5 ranks for high-card/flush). Implements `Comparable` by comparing `rank` first, then `tiebreakers` position by position.

### Service layer (`service/`)

- **`GameService`** (`@Service`) — in-memory `Map<String, Table>` / `Map<String, Round>` keyed by table id (`ConcurrentHashMap`, no persistence yet). `createTable`/`joinTable`/`startHand` set things up; `performAction` forwards to `Round.handleAction`, then loops `advancePhase()` while nobody can act and the river hasn't been reached — this is what auto-runs remaining streets when all contesting players are all-in, without waiting for actions that will never come.
- Hand resolution (`finishHand`) builds side pots from each player's `totalBetThisHand`: distinct positive contribution levels become pot layers, each layer's eligible winners are non-folded players who reached that contribution level, evaluated with `HandEvaluator`. Ties within a layer split the pot evenly; an odd remainder chip is handed out one-by-one following `Round.getOrder()`. This generalizes "smallest all-in caps the pot, the excess forms a side pot" to any number of differing all-in amounts, without special-casing.

### Not yet implemented

No REST/WebSocket layer exposing `GameService` yet — everything is plain Java callable in-process. No persisted `Account`/`User` entity, no hand-history logging, no blind-level/tournament structure (blinds are passed as fixed ints to `Round`'s constructor by the caller).
