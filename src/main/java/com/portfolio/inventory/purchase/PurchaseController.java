package com.portfolio.inventory.purchase;

import static com.portfolio.inventory.purchase.PurchaseDtos.*;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/purchases")
public class PurchaseController {
    private final PurchaseService service;

    public PurchaseController(PurchaseService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'PURCHASING_AGENT')")
    @Operation(summary = "Create a draft purchase order")
    public PurchaseResponse create(@Valid @RequestBody CreatePurchaseRequest request,
            Authentication authentication) {
        return service.create(request, authentication.getName());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a purchase order")
    public PurchaseResponse get(@PathVariable Long id) { return service.get(id); }

    @GetMapping
    @Operation(summary = "List purchase orders")
    public Page<PurchaseResponse> list(@RequestParam(required = false) PurchaseStatus status,
            @ParameterObject @PageableDefault(size = 20, sort = "purchasedAt",
                    direction = Sort.Direction.DESC) Pageable pageable) {
        return service.list(status, pageable);
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyRole('ADMIN', 'PURCHASING_AGENT')")
    @Operation(summary = "Submit a draft purchase order")
    public PurchaseResponse submit(@PathVariable Long id) { return service.submit(id); }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PURCHASING_AGENT')")
    @Operation(summary = "Edit supplier, warehouse and items while an order is DRAFT")
    public PurchaseResponse updateDraft(@PathVariable Long id,
            @Valid @RequestBody UpdateDraftRequest request) {
        return service.updateDraft(id, request);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_MANAGER')")
    @Operation(summary = "Approve a submitted purchase order")
    public PurchaseResponse approve(@PathVariable Long id, Authentication authentication) {
        return service.approve(id, authentication.getName());
    }

    @PostMapping("/{id}/receive")
    @PreAuthorize("hasAnyRole('ADMIN', 'INVENTORY_MANAGER')")
    @Operation(summary = "Receive some or all outstanding stock")
    public PurchaseResponse receive(@PathVariable Long id,
            @Valid @RequestBody ReceivePurchaseRequest request, Authentication authentication) {
        return service.receive(id, request, authentication.getName());
    }

    @GetMapping("/{id}/receipts")
    @Operation(summary = "List the immutable stock receipt history for an order")
    public java.util.List<ReceiptResponse> receipts(@PathVariable Long id) {
        return service.receipts(id);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'PURCHASING_AGENT')")
    @Operation(summary = "Cancel a draft, submitted, or approved purchase order")
    public PurchaseResponse cancel(@PathVariable Long id) { return service.cancel(id); }
}
