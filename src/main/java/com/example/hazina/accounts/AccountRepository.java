package com.example.hazina.accounts;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {
    boolean existsByCode(String code);
    boolean existsByCodeAndIdNot(String code, UUID id);
    Optional<Account> findByCode(String code);
    List<Account> findByTypeOrderByCodeAsc(Account.AccountType type);
    List<Account> findByParentIdOrderByCodeAsc(UUID parentId);
    List<Account> findAllByOrderByCodeAsc();
}
