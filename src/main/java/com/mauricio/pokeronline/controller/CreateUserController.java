package com.mauricio.pokeronline.controller;

import com.mauricio.pokeronline.entity.Account;
import com.mauricio.pokeronline.repository.AccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/accounts")
public class CreateUserController {

    private final AccountRepository accountRepository;

    public CreateUserController(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @PostMapping
    public ResponseEntity<Account> createUser(@RequestBody CreateUserRequest request) {
        if (accountRepository.findByUsername(request.username()).isPresent()) {
            throw new IllegalArgumentException("Já existe uma conta com esse username: " + request.username());
        }

        Account account = new Account(request.username(), request.initialBankroll());
        Account saved = accountRepository.save(account);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    public record CreateUserRequest(String username, int initialBankroll) {
    }
}
