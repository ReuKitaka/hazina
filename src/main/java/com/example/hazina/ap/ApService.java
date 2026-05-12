package com.example.hazina.ap;

import com.example.hazina.accounts.Account;
import com.example.hazina.accounts.AccountRepository;
import com.example.hazina.ap.dto.*;
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
public class ApService {

    private final SupplierRepository supplierRepository;
    private final BillRepository billRepository;
    private final ApPaymentRepository paymentRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final JournalEntryService journalEntryService;

    // ── Suppliers ─────────────────────────────────────────────────────────────

    @Transactional
    public SupplierResponse createSupplier(CreateSupplierRequest request) {
        if (supplierRepository.existsBySupplierCode(request.supplierCode())) {
            throw new IllegalArgumentException("Supplier code already exists: " + request.supplierCode());
        }
        Supplier supplier = new Supplier();
        supplier.setSupplierCode(request.supplierCode());
        supplier.setName(request.name());
        supplier.setEmail(request.email());
        supplier.setPhone(request.phone());
        supplier.setAddress(request.address());
        supplier.setTaxPin(request.taxPin());
        return SupplierResponse.from(supplierRepository.save(supplier), BigDecimal.ZERO);
    }

    public List<SupplierResponse> findAllSuppliers() {
        return supplierRepository.findAll().stream()
                .map(s -> SupplierResponse.from(s, billRepository.getOutstandingBalanceForSupplier(s.getId())))
                .toList();
    }

    public SupplierResponse findSupplierById(UUID id) {
        Supplier s = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + id));
        return SupplierResponse.from(s, billRepository.getOutstandingBalanceForSupplier(id));
    }

    // ── Bills ─────────────────────────────────────────────────────────────────

    @Transactional
    public BillResponse createBill(CreateBillRequest request, String userEmail) {
        supplierRepository.findById(request.supplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + request.supplierId()));

        Account apAccount = accountRepository.findById(request.apAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("AP account not found: " + request.apAccountId()));
        if (apAccount.getType() != Account.AccountType.LIABILITY) {
            throw new IllegalArgumentException("AP account must be a LIABILITY type account");
        }

        Long seq = billRepository.nextVal();
        String billNumber = String.format("BILL-%d-%06d", LocalDate.now().getYear(), seq);
        UUID userId = resolveUserId(userEmail);

        Bill bill = new Bill();
        bill.setBillNumber(billNumber);
        bill.setSupplierId(request.supplierId());
        bill.setApAccountId(request.apAccountId());
        bill.setBillDate(request.billDate());
        bill.setDueDate(request.dueDate());
        bill.setSupplierRef(request.supplierRef());
        bill.setNotes(request.notes());
        bill.setCreatedBy(userId);

        BigDecimal total = BigDecimal.ZERO;
        List<BillLine> lines = new ArrayList<>();
        for (int i = 0; i < request.lines().size(); i++) {
            BillLineRequest lr = request.lines().get(i);
            Account expAccount = accountRepository.findById(lr.expenseAccountId())
                    .orElseThrow(() -> new ResourceNotFoundException("Expense account not found: " + lr.expenseAccountId()));
            if (expAccount.getType() != Account.AccountType.EXPENSE) {
                throw new IllegalArgumentException("Expense account must be EXPENSE type: " + expAccount.getCode());
            }
            BigDecimal amount = lr.quantity().multiply(lr.unitPrice());
            BillLine line = new BillLine();
            line.setBill(bill);
            line.setDescription(lr.description());
            line.setQuantity(lr.quantity());
            line.setUnitPrice(lr.unitPrice());
            line.setAmount(amount);
            line.setExpenseAccountId(lr.expenseAccountId());
            line.setLineOrder(i);
            lines.add(line);
            total = total.add(amount);
        }
        bill.setLines(lines);
        bill.setTotalAmount(total);

        return toBillResponse(billRepository.save(bill));
    }

    @Transactional
    public BillResponse approveBill(UUID id, String userEmail) {
        Bill bill = findBillEntity(id);
        if (bill.getStatus() != Bill.BillStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT bills can be approved");
        }

        // GL entry: DR each expense account (per line), CR AP account (total)
        List<JournalEntryLineRequest> jeLines = new ArrayList<>();
        for (BillLine line : bill.getLines()) {
            jeLines.add(new JournalEntryLineRequest(line.getExpenseAccountId(),
                    line.getDescription(), line.getAmount(), null));
        }
        jeLines.add(new JournalEntryLineRequest(bill.getApAccountId(),
                "AP - " + bill.getBillNumber(), null, bill.getTotalAmount()));

        JournalEntryResponse entry = journalEntryService.create(
                new CreateJournalEntryRequest(bill.getBillDate(),
                        "Bill " + bill.getBillNumber(), bill.getBillNumber(), jeLines),
                userEmail);
        journalEntryService.post(entry.id(), userEmail);

        bill.setStatus(Bill.BillStatus.APPROVED);
        bill.setJournalEntryId(entry.id());
        return toBillResponse(billRepository.save(bill));
    }

    @Transactional
    public BillResponse cancelBill(UUID id) {
        Bill bill = findBillEntity(id);
        if (bill.getStatus() != Bill.BillStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT bills can be cancelled");
        }
        bill.setStatus(Bill.BillStatus.CANCELLED);
        return toBillResponse(billRepository.save(bill));
    }

    public BillResponse findBillById(UUID id) {
        return toBillResponse(findBillEntity(id));
    }

    public List<BillResponse> findAllBills(Bill.BillStatus status) {
        List<Bill> bills = status != null
                ? billRepository.findAllWithLinesByStatus(status)
                : billRepository.findAllWithLines();
        return bills.stream().map(this::toBillResponse).toList();
    }

    public List<BillResponse> findBillsBySupplier(UUID supplierId) {
        supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + supplierId));
        return billRepository.findBySupplierIdWithLines(supplierId).stream()
                .map(this::toBillResponse).toList();
    }

    // ── Payments ──────────────────────────────────────────────────────────────

    @Transactional
    public ApPaymentResponse recordPayment(RecordPaymentRequest request, String userEmail) {
        Bill bill = findBillEntity(request.billId());

        if (bill.getStatus() == Bill.BillStatus.DRAFT
                || bill.getStatus() == Bill.BillStatus.CANCELLED
                || bill.getStatus() == Bill.BillStatus.PAID) {
            throw new IllegalStateException("Payment can only be applied to APPROVED or PARTIALLY_PAID bills");
        }

        BigDecimal outstanding = bill.getOutstandingAmount();
        if (request.amountPaid().compareTo(outstanding) > 0) {
            throw new IllegalArgumentException(String.format(
                    "Amount paid %s exceeds outstanding balance %s", request.amountPaid(), outstanding));
        }

        accountRepository.findById(request.paymentAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment account not found: " + request.paymentAccountId()));

        // GL entry: DR AP account, CR payment account (cash/bank)
        List<JournalEntryLineRequest> jeLines = List.of(
                new JournalEntryLineRequest(bill.getApAccountId(),
                        "Payment - " + bill.getBillNumber(), request.amountPaid(), null),
                new JournalEntryLineRequest(request.paymentAccountId(),
                        "Payment - " + bill.getBillNumber(), null, request.amountPaid())
        );

        String method = request.paymentMethod() != null ? request.paymentMethod() : "BANK_TRANSFER";
        JournalEntryResponse entry = journalEntryService.create(
                new CreateJournalEntryRequest(request.paymentDate(),
                        "Payment for " + bill.getBillNumber(), bill.getBillNumber(), jeLines),
                userEmail);
        journalEntryService.post(entry.id(), userEmail);

        UUID userId = resolveUserId(userEmail);
        ApPayment payment = new ApPayment();
        payment.setBillId(request.billId());
        payment.setSupplierId(bill.getSupplierId());
        payment.setPaymentDate(request.paymentDate());
        payment.setAmountPaid(request.amountPaid());
        payment.setPaymentMethod(method);
        payment.setPaymentAccountId(request.paymentAccountId());
        payment.setJournalEntryId(entry.id());
        payment.setNotes(request.notes());
        payment.setCreatedBy(userId);
        paymentRepository.save(payment);

        BigDecimal newPaid = bill.getPaidAmount().add(request.amountPaid());
        bill.setPaidAmount(newPaid);
        bill.setStatus(newPaid.compareTo(bill.getTotalAmount()) >= 0
                ? Bill.BillStatus.PAID
                : Bill.BillStatus.PARTIALLY_PAID);
        billRepository.save(bill);

        return toPaymentResponse(payment, bill, entry.entryNumber());
    }

    public List<ApPaymentResponse> findPaymentsByBill(UUID billId) {
        Bill bill = findBillEntity(billId);
        return paymentRepository.findByBillIdOrderByPaymentDateDesc(billId).stream()
                .map(p -> toPaymentResponse(p, bill,
                        journalEntryService.findById(p.getJournalEntryId()).entryNumber()))
                .toList();
    }

    public ApPaymentResponse findPaymentById(UUID id) {
        ApPayment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + id));
        Bill bill = findBillEntity(payment.getBillId());
        return toPaymentResponse(payment, bill,
                journalEntryService.findById(payment.getJournalEntryId()).entryNumber());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Bill findBillEntity(UUID id) {
        return billRepository.findByIdWithLines(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found: " + id));
    }

    private UUID resolveUserId(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email))
                .getId();
    }

    private BillResponse toBillResponse(Bill bill) {
        Set<UUID> accountIds = new HashSet<>();
        accountIds.add(bill.getApAccountId());
        bill.getLines().forEach(l -> accountIds.add(l.getExpenseAccountId()));
        Map<UUID, Account> accountMap = accountRepository.findAllById(accountIds).stream()
                .collect(Collectors.toMap(Account::getId, a -> a));

        Account apAccount = accountMap.get(bill.getApAccountId());
        Supplier supplier = supplierRepository.findById(bill.getSupplierId()).orElse(null);

        List<BillLineResponse> lineResponses = bill.getLines().stream()
                .map(l -> {
                    Account exp = accountMap.get(l.getExpenseAccountId());
                    return BillLineResponse.from(l,
                            exp != null ? exp.getCode() : null,
                            exp != null ? exp.getName() : null);
                }).toList();

        return new BillResponse(
                bill.getId(), bill.getBillNumber(),
                bill.getSupplierId(), supplier != null ? supplier.getName() : null,
                bill.getApAccountId(), apAccount != null ? apAccount.getCode() : null,
                bill.getBillDate(), bill.getDueDate(), bill.getSupplierRef(),
                bill.getStatus().name(),
                bill.getTotalAmount(), bill.getPaidAmount(), bill.getOutstandingAmount(),
                bill.getNotes(), lineResponses,
                bill.getCreatedAt(), bill.getUpdatedAt());
    }

    private ApPaymentResponse toPaymentResponse(ApPayment p, Bill bill, String jeNumber) {
        Supplier supplier = supplierRepository.findById(p.getSupplierId()).orElse(null);
        Account payAccount = accountRepository.findById(p.getPaymentAccountId()).orElse(null);
        return new ApPaymentResponse(
                p.getId(), p.getBillId(), bill.getBillNumber(),
                p.getSupplierId(), supplier != null ? supplier.getName() : null,
                p.getPaymentDate(), p.getAmountPaid(), p.getPaymentMethod(),
                p.getPaymentAccountId(), payAccount != null ? payAccount.getCode() : null,
                jeNumber, p.getNotes(), p.getCreatedAt());
    }
}
