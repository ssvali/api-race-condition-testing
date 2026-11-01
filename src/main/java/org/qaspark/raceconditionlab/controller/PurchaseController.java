
package org.qaspark.raceconditionlab.controller;

import org.qaspark.raceconditionlab.entity.Purchase;
import org.qaspark.raceconditionlab.service.PurchaseService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/purchases")
public class PurchaseController {

    private final PurchaseService purchaseService;

    public PurchaseController(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Purchase createPurchase(@RequestBody PurchaseRequest request) {

        return purchaseService.purchase(
                request.productId(),
                request.customerId(),
                request.quantity()
        );
    }

    public record PurchaseRequest(
            String productId,
            String customerId,
            int quantity
    ) {}
}
