package com.mauricio.pokeronline.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Identidade local persistida do jogador (sem autenticação): mantém o nome de usuário
 * e o saldo de fichas (bankroll) entre sessões, ao contrário do {@link com.mauricio.pokeronline.model.Player},
 * que é o estado transitório de um jogador dentro de uma mão.
 */
@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private int bankroll;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected Account() {
        // exigido pelo JPA
    }

    public Account(String username, int bankroll) {
        this.username = username;
        this.bankroll = bankroll;
        this.createdAt = LocalDateTime.now();
    }

    public void deposit(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("O valor do depósito deve ser positivo.");
        }
        bankroll += amount;
    }

    public void withdraw(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("O valor do saque deve ser positivo.");
        }
        if (amount > bankroll) {
            throw new IllegalArgumentException("Saldo insuficiente.");
        }
        bankroll -= amount;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public int getBankroll() {
        return bankroll;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
