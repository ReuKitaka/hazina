package com.example.hazina.accounts;

import com.example.hazina.accounts.dto.AccountResponse;
import com.example.hazina.accounts.dto.CreateAccountRequest;
import com.example.hazina.accounts.dto.UpdateAccountRequest;
import com.example.hazina.shared.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountService {

    private final AccountRepository accountRepository;

    @Transactional
    public AccountResponse create(CreateAccountRequest request) {
        if (accountRepository.existsByCode(request.code())) {
            throw new IllegalArgumentException("Account code already exists: " + request.code());
        }
        if (request.parentId() != null && !accountRepository.existsById(request.parentId())) {
            throw new ResourceNotFoundException("Parent account not found: " + request.parentId());
        }

        Account account = new Account();
        account.setCode(request.code());
        account.setName(request.name());
        account.setType(request.type());
        account.setNormalBalance(deriveNormalBalance(request.type()));
        account.setParentId(request.parentId());
        account.setDescription(request.description());

        return AccountResponse.from(accountRepository.save(account));
    }

    public List<AccountResponse> findAll() {
        return accountRepository.findAllByOrderByCodeAsc().stream()
                .map(AccountResponse::from)
                .toList();
    }

    public List<AccountResponse> findByType(Account.AccountType type) {
        return accountRepository.findByTypeOrderByCodeAsc(type).stream()
                .map(AccountResponse::from)
                .toList();
    }

    public AccountResponse findById(UUID id) {
        return accountRepository.findById(id)
                .map(AccountResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + id));
    }

    @Transactional
    public AccountResponse update(UUID id, UpdateAccountRequest request) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + id));

        if (request.parentId() != null
                && !request.parentId().equals(id)
                && !accountRepository.existsById(request.parentId())) {
            throw new ResourceNotFoundException("Parent account not found: " + request.parentId());
        }

        account.setName(request.name());
        account.setDescription(request.description());
        account.setParentId(request.parentId());
        account.setActive(request.active());

        return AccountResponse.from(accountRepository.save(account));
    }

    @Transactional
    public void deactivate(UUID id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + id));
        account.setActive(false);
        accountRepository.save(account);
    }

    private Account.NormalBalance deriveNormalBalance(Account.AccountType type) {
        return switch (type) {
            case ASSET, EXPENSE -> Account.NormalBalance.DEBIT;
            case LIABILITY, EQUITY, REVENUE -> Account.NormalBalance.CREDIT;
        };
    }
}
