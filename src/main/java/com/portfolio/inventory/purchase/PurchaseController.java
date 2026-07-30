package com.portfolio.inventory.purchase;

import static com.portfolio.inventory.purchase.PurchaseDtos.*;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/purchases")
public class PurchaseController {
    private final PurchaseService service;

    public PurchaseController(PurchaseService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'PURCHASING_AGENT')")
    @Operation(summary = "Purchase and receive stock from a supplier",
            description = "ADMIN or PURCHASING_AGENT. The received quantities immediately increase warehouse stock.")
    public PurchaseResponse create(
            @Valid @RequestBody CreatePurchaseRequest request, Authentication authentication) {
        return service.create(request, authentication.getName());
    }
}
