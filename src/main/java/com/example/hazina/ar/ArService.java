package com.example.hazina.ar;

import com.example.hazina.accounts.Account;
import com.example.hazina.accounts.AccountRepository;
import com.example.hazina.ar.dto.*;
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
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArService {

    private final CustomerRepository customerRepository;
    private final InvoiceRepository invoiceRepository;
    private final ArReceiptRepository receiptRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final JournalEntryService journalEntryService;

    // ── Customers ─────────────────────────────────────────────────────────────

    @Transactional
    public CustomerResponse createCustomer(CreateCustomerRequest request) {
        if (customerRepository.existsByCustomerCode(request.customerCode())) {
            throw new IllegalArgumentException("Customer code already exists: " + request.customerCode());
        }
        Customer customer = new Customer();
        customer.setCustomerCode(request.customerCode());
        customer.setName(request.name());
        customer.setEmail(request.email());
        customer.setPhone(request.phone());
        customer.setAddress(request.address());
        customer.setCreditLimit(request.creditLimit());
        return CustomerResponse.from(customerRepository.save(customer), BigDecimal.ZERO);
    }

    public List<CustomerResponse> findAllCustomers() {
        return customerRepository.findAll().stream()
                .map(c -> CustomerResponse.from(c, invoiceRepository.getOutstandingBalanceForCustomer(c.getId())))
                .toList();
    }

    public CustomerResponse findCustomerById(UUID id) {
        Customer c = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + id));
        return CustomerResponse.from(c, invoiceRepository.getOutstandingBalanceForCustomer(id));
    }

    // ── Invoices ──────────────────────────────────────────────────────────────

    @Transactional
    public InvoiceResponse createInvoice(CreateInvoiceRequest request, String userEmail) {
        customerRepository.findById(request.customerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + request.customerId()));

        Account arAccount = accountRepository.findById(request.arAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("AR account not found: " + request.arAccountId()));
        if (arAccount.getType() != Account.AccountType.ASSET) {
            throw new IllegalArgumentException("AR account must be an ASSET type account");
        }

        if (!request.dueDate().isAfter(request.issueDate()) && !request.dueDate().isEqual(request.issueDate())) {
            throw new IllegalArgumentException("Due date must be on or after issue date");
        }

        Long seq = invoiceRepository.nextVal();
        String invoiceNumber = String.format("INV-%d-%06d", LocalDate.now().getYear(), seq);
        UUID userId = resolveUserId(userEmail);

        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setCustomerId(request.customerId());
        invoice.setArAccountId(request.arAccountId());
        invoice.setIssueDate(request.issueDate());
        invoice.setDueDate(request.dueDate());
        invoice.setNotes(request.notes());
        invoice.setCreatedBy(userId);

        BigDecimal total = BigDecimal.ZERO;
        List<InvoiceLine> lines = new ArrayList<>();
        for (int i = 0; i < request.lines().size(); i++) {
            InvoiceLineRequest lr = request.lines().get(i);
            Account revAccount = accountRepository.findById(lr.revenueAccountId())
                    .orElseThrow(() -> new ResourceNotFoundException("Revenue account not found: " + lr.revenueAccountId()));
            if (revAccount.getType() != Account.AccountType.REVENUE) {
                throw new IllegalArgumentException("Revenue account must be REVENUE type: " + revAccount.getCode());
            }
            BigDecimal amount = lr.quantity().multiply(lr.unitPrice());
            InvoiceLine line = new InvoiceLine();
            line.setInvoice(invoice);
            line.setDescription(lr.description());
            line.setQuantity(lr.quantity());
            line.setUnitPrice(lr.unitPrice());
            line.setAmount(amount);
            line.setRevenueAccountId(lr.revenueAccountId());
            line.setLineOrder(i);
            lines.add(line);
            total = total.add(amount);
        }
        invoice.setLines(lines);
        invoice.setTotalAmount(total);

        return toInvoiceResponse(invoiceRepository.save(invoice));
    }

    @Transactional
    public InvoiceResponse approveInvoice(UUID id, String userEmail) {
        Invoice invoice = findInvoiceEntity(id);
        if (invoice.getStatus() != Invoice.InvoiceStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT invoices can be approved");
        }

        // GL entry: DR AR account (total), CR each revenue account (per line)
        List<JournalEntryLineRequest> jeLines = new ArrayList<>();
        jeLines.add(new JournalEntryLineRequest(invoice.getArAccountId(),
                "AR - " + invoice.getInvoiceNumber(), invoice.getTotalAmount(), null));
        for (InvoiceLine line : invoice.getLines()) {
            jeLines.add(new JournalEntryLineRequest(line.getRevenueAccountId(),
                    line.getDescription(), null, line.getAmount()));
        }

        JournalEntryResponse entry = journalEntryService.create(
                new CreateJournalEntryRequest(invoice.getIssueDate(),
                        "Invoice " + invoice.getInvoiceNumber(), invoice.getInvoiceNumber(), jeLines),
                userEmail);
        journalEntryService.post(entry.id(), userEmail);

        invoice.setStatus(Invoice.InvoiceStatus.APPROVED);
        invoice.setJournalEntryId(entry.id());
        return toInvoiceResponse(invoiceRepository.save(invoice));
    }

    @Transactional
    public InvoiceResponse cancelInvoice(UUID id) {
        Invoice invoice = findInvoiceEntity(id);
        if (invoice.getStatus() != Invoice.InvoiceStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT invoices can be cancelled");
        }
        invoice.setStatus(Invoice.InvoiceStatus.CANCELLED);
        return toInvoiceResponse(invoiceRepository.save(invoice));
    }

    public InvoiceResponse findInvoiceById(UUID id) {
        return toInvoiceResponse(findInvoiceEntity(id));
    }

    public List<InvoiceResponse> findAllInvoices(Invoice.InvoiceStatus status) {
        List<Invoice> invoices = status != null
                ? invoiceRepository.findAllWithLinesByStatus(status)
                : invoiceRepository.findAllWithLines();
        return invoices.stream().map(this::toInvoiceResponse).toList();
    }

    public List<InvoiceResponse> findInvoicesByCustomer(UUID customerId) {
        customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerId));
        return invoiceRepository.findByCustomerIdWithLines(customerId).stream()
                .map(this::toInvoiceResponse).toList();
    }

    // ── Receipts ──────────────────────────────────────────────────────────────

    @Transactional
    public ArReceiptResponse recordReceipt(RecordReceiptRequest request, String userEmail) {
        Invoice invoice = findInvoiceEntity(request.invoiceId());

        if (invoice.getStatus() == Invoice.InvoiceStatus.DRAFT
                || invoice.getStatus() == Invoice.InvoiceStatus.CANCELLED
                || invoice.getStatus() == Invoice.InvoiceStatus.PAID) {
            throw new IllegalStateException("Receipt can only be applied to APPROVED or PARTIALLY_PAID invoices");
        }

        BigDecimal outstanding = invoice.getOutstandingAmount();
        if (request.amountReceived().compareTo(outstanding) > 0) {
            throw new IllegalArgumentException(String.format(
                    "Amount received %s exceeds outstanding balance %s", request.amountReceived(), outstanding));
        }

        accountRepository.findById(request.paymentAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment account not found: " + request.paymentAccountId()));

        // GL entry: DR payment account (cash/bank), CR AR account
        List<JournalEntryLineRequest> jeLines = List.of(
                new JournalEntryLineRequest(request.paymentAccountId(),
                        "Receipt - " + invoice.getInvoiceNumber(), request.amountReceived(), null),
                new JournalEntryLineRequest(invoice.getArAccountId(),
                        "Receipt - " + invoice.getInvoiceNumber(), null, request.amountReceived())
        );

        String method = request.paymentMethod() != null ? request.paymentMethod() : "BANK_TRANSFER";
        JournalEntryResponse entry = journalEntryService.create(
                new CreateJournalEntryRequest(request.receiptDate(),
                        "Receipt for " + invoice.getInvoiceNumber(), invoice.getInvoiceNumber(), jeLines),
                userEmail);
        journalEntryService.post(entry.id(), userEmail);

        UUID userId = resolveUserId(userEmail);
        ArReceipt receipt = new ArReceipt();
        receipt.setInvoiceId(request.invoiceId());
        receipt.setCustomerId(invoice.getCustomerId());
        receipt.setReceiptDate(request.receiptDate());
        receipt.setAmountReceived(request.amountReceived());
        receipt.setPaymentMethod(method);
        receipt.setPaymentAccountId(request.paymentAccountId());
        receipt.setJournalEntryId(entry.id());
        receipt.setNotes(request.notes());
        receipt.setCreatedBy(userId);
        receiptRepository.save(receipt);

        // Update invoice paid amount and status
        BigDecimal newPaid = invoice.getPaidAmount().add(request.amountReceived());
        invoice.setPaidAmount(newPaid);
        invoice.setStatus(newPaid.compareTo(invoice.getTotalAmount()) >= 0
                ? Invoice.InvoiceStatus.PAID
                : Invoice.InvoiceStatus.PARTIALLY_PAID);
        invoiceRepository.save(invoice);

        return toReceiptResponse(receipt, invoice, entry.entryNumber());
    }

    public List<ArReceiptResponse> findReceiptsByInvoice(UUID invoiceId) {
        Invoice invoice = findInvoiceEntity(invoiceId);
        return receiptRepository.findByInvoiceIdOrderByReceiptDateDesc(invoiceId).stream()
                .map(r -> toReceiptResponse(r, invoice,
                        journalEntryService.findById(r.getJournalEntryId()).entryNumber()))
                .toList();
    }

    public ArReceiptResponse findReceiptById(UUID id) {
        ArReceipt receipt = receiptRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Receipt not found: " + id));
        Invoice invoice = findInvoiceEntity(receipt.getInvoiceId());
        return toReceiptResponse(receipt, invoice,
                journalEntryService.findById(receipt.getJournalEntryId()).entryNumber());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Invoice findInvoiceEntity(UUID id) {
        return invoiceRepository.findByIdWithLines(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + id));
    }

    private UUID resolveUserId(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email))
                .getId();
    }

    private InvoiceResponse toInvoiceResponse(Invoice invoice) {
        Set<UUID> accountIds = new HashSet<>();
        accountIds.add(invoice.getArAccountId());
        invoice.getLines().forEach(l -> accountIds.add(l.getRevenueAccountId()));
        Map<UUID, Account> accountMap = accountRepository.findAllById(accountIds).stream()
                .collect(Collectors.toMap(Account::getId, a -> a));

        Account arAccount = accountMap.get(invoice.getArAccountId());
        Customer customer = customerRepository.findById(invoice.getCustomerId()).orElse(null);

        List<InvoiceLineResponse> lineResponses = invoice.getLines().stream()
                .map(l -> {
                    Account rev = accountMap.get(l.getRevenueAccountId());
                    return InvoiceLineResponse.from(l,
                            rev != null ? rev.getCode() : null,
                            rev != null ? rev.getName() : null);
                }).toList();

        return new InvoiceResponse(
                invoice.getId(), invoice.getInvoiceNumber(),
                invoice.getCustomerId(), customer != null ? customer.getName() : null,
                invoice.getArAccountId(), arAccount != null ? arAccount.getCode() : null,
                invoice.getIssueDate(), invoice.getDueDate(),
                invoice.getStatus().name(),
                invoice.getTotalAmount(), invoice.getPaidAmount(), invoice.getOutstandingAmount(),
                invoice.getNotes(), lineResponses,
                invoice.getCreatedAt(), invoice.getUpdatedAt());
    }

    private ArReceiptResponse toReceiptResponse(ArReceipt r, Invoice invoice, String jeNumber) {
        Customer customer = customerRepository.findById(r.getCustomerId()).orElse(null);
        Account payAccount = accountRepository.findById(r.getPaymentAccountId()).orElse(null);
        return new ArReceiptResponse(
                r.getId(), r.getInvoiceId(), invoice.getInvoiceNumber(),
                r.getCustomerId(), customer != null ? customer.getName() : null,
                r.getReceiptDate(), r.getAmountReceived(), r.getPaymentMethod(),
                r.getPaymentAccountId(), payAccount != null ? payAccount.getCode() : null,
                jeNumber, r.getNotes(), r.getCreatedAt());
    }
}
