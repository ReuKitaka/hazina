package com.example.hazina.currency;

import com.example.hazina.accounts.Account;
import com.example.hazina.accounts.AccountRepository;
import com.example.hazina.currency.dto.*;
import com.example.hazina.ledger.JournalEntryService;
import com.example.hazina.ledger.dto.CreateJournalEntryRequest;
import com.example.hazina.ledger.dto.JournalEntryLineRequest;
import com.example.hazina.ledger.dto.JournalEntryResponse;
import com.example.hazina.shared.ResourceNotFoundException;
import com.example.hazina.users.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExchangeRateService {

    private final ExchangeRateRepository rateRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final JournalEntryService journalEntryService;

    @Transactional
    public ExchangeRateResponse create(CreateExchangeRateRequest request, String userEmail) {
        if (request.baseCurrency().equalsIgnoreCase(request.quoteCurrency())) {
            throw new IllegalArgumentException("Base and quote currencies must be different");
        }
        UUID userId = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail))
                .getId();

        ExchangeRate rate = new ExchangeRate();
        rate.setBaseCurrency(request.baseCurrency().toUpperCase());
        rate.setQuoteCurrency(request.quoteCurrency().toUpperCase());
        rate.setRate(request.rate());
        rate.setEffectiveDate(request.effectiveDate());
        rate.setCreatedBy(userId);

        return toResponse(rateRepository.save(rate));
    }

    public List<ExchangeRateResponse> findAll() {
        return rateRepository.findAll().stream().map(this::toResponse).toList();
    }

    public List<ExchangeRateResponse> findByPair(String base, String quote) {
        return rateRepository
                .findByBaseCurrencyAndQuoteCurrencyOrderByEffectiveDateDesc(
                        base.toUpperCase(), quote.toUpperCase())
                .stream().map(this::toResponse).toList();
    }

    public ExchangeRateResponse findById(UUID id) {
        return toResponse(rateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exchange rate not found: " + id)));
    }

    public ExchangeRateResponse getLatestRate(String base, String quote, LocalDate asOf) {
        LocalDate date = asOf != null ? asOf : LocalDate.now();
        return rateRepository
                .findTopByBaseCurrencyAndQuoteCurrencyAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
                        base.toUpperCase(), quote.toUpperCase(), date)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("No exchange rate found for %s/%s on or before %s", base, quote, date)));
    }

    // ── Revaluation ───────────────────────────────────────────────────────────

    @Transactional
    public RevaluationResponse revalue(RevaluationRequest request, String userEmail) {
        Account account = accountRepository.findById(request.accountId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + request.accountId()));
        accountRepository.findById(request.fxGainLossAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("FX gain/loss account not found: " + request.fxGainLossAccountId()));

        // Functional balance = SUM(DR) - SUM(CR) of FX lines for this account
        BigDecimal functionalBalance = journalEntryService
                .getFunctionalBalanceForForeignCurrency(request.accountId(), request.foreignCurrency().toUpperCase());
        // Net foreign exposure
        BigDecimal netForeignAmount = journalEntryService
                .getNetForeignAmountForAccount(request.accountId(), request.foreignCurrency().toUpperCase());

        if (netForeignAmount.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalStateException(
                    String.format("No %s exposure found on account %s", request.foreignCurrency(), account.getCode()));
        }

        // Get the current rate: foreignCurrency → functional currency (KES)
        ExchangeRate currentRate = rateRepository
                .findTopByBaseCurrencyAndQuoteCurrencyAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
                        request.foreignCurrency().toUpperCase(), "KES", request.valuationDate())
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("No exchange rate for %s/KES on or before %s",
                                request.foreignCurrency(), request.valuationDate())));

        BigDecimal revalued = netForeignAmount.multiply(currentRate.getRate())
                .setScale(4, RoundingMode.HALF_UP);
        BigDecimal adjustment = revalued.subtract(functionalBalance);

        if (adjustment.abs().compareTo(new BigDecimal("0.0001")) < 0) {
            throw new IllegalStateException("No revaluation adjustment needed — balance is already current");
        }

        // Build the revaluation journal entry
        // adjustment > 0: DR account, CR fxGainLoss  (gain for debit-normal; loss for credit-normal)
        // adjustment < 0: DR fxGainLoss, CR account  (loss for debit-normal; gain for credit-normal)
        BigDecimal absAdj = adjustment.abs();
        JournalEntryLineRequest accountLine;
        JournalEntryLineRequest fxLine;
        if (adjustment.compareTo(BigDecimal.ZERO) > 0) {
            accountLine = new JournalEntryLineRequest(request.accountId(),
                    "FX Revaluation - " + request.foreignCurrency(), absAdj, null);
            fxLine = new JournalEntryLineRequest(request.fxGainLossAccountId(),
                    "FX Revaluation - " + request.foreignCurrency(), null, absAdj);
        } else {
            fxLine = new JournalEntryLineRequest(request.fxGainLossAccountId(),
                    "FX Revaluation - " + request.foreignCurrency(), absAdj, null);
            accountLine = new JournalEntryLineRequest(request.accountId(),
                    "FX Revaluation - " + request.foreignCurrency(), null, absAdj);
        }

        String description = String.format("FX Revaluation: %s exposure on %s at rate %.6f",
                request.foreignCurrency(), account.getCode(), currentRate.getRate());
        JournalEntryResponse entry = journalEntryService.create(
                new CreateJournalEntryRequest(request.valuationDate(), description, null,
                        List.of(accountLine, fxLine)),
                userEmail);
        journalEntryService.post(entry.id(), userEmail);

        return new RevaluationResponse(
                entry.id(), entry.entryNumber(),
                account.getId(), account.getCode(),
                request.foreignCurrency().toUpperCase(),
                netForeignAmount, currentRate.getRate(),
                functionalBalance, revalued, adjustment,
                adjustment.compareTo(BigDecimal.ZERO) > 0,
                request.valuationDate());
    }

    // ── helper ────────────────────────────────────────────────────────────────

    private ExchangeRateResponse toResponse(ExchangeRate r) {
        return new ExchangeRateResponse(r.getId(), r.getBaseCurrency(), r.getQuoteCurrency(),
                r.getRate(), r.getEffectiveDate(), r.getCreatedAt());
    }
}
