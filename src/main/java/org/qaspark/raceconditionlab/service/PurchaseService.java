
package org.qaspark.raceconditionlab.service;

import org.qaspark.raceconditionlab.entity.Product;
import org.qaspark.raceconditionlab.entity.Purchase;
import org.qaspark.raceconditionlab.exception.ProductNotFoundException;
import org.qaspark.raceconditionlab.repository.ProductRepository;
import org.qaspark.raceconditionlab.repository.PurchaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PurchaseService {

    private final ProductRepository productRepository;
    private final PurchaseRepository purchaseRepository;

    public PurchaseService(
            ProductRepository productRepository,
            PurchaseRepository purchaseRepository) {
        this.productRepository = productRepository;
        this.purchaseRepository = purchaseRepository;
    }


    @Transactional
    public Purchase purchase(
            String productId,
            String customerId,
            int quantity) {

        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "Quantity must be greater than zero");
        }

        // Step 1: Validate product existence
        if (productRepository.findByProductId(productId).isEmpty()) {
            throw new ProductNotFoundException(
                    "Product not found: " + productId);
        }

        // Step 2: Atomically reserve inventory
        int updatedRows = productRepository.reserveInventory(
                productId, quantity);

        // Step 3: Check whether reservation succeeded
        if (updatedRows == 0) {
            throw new IllegalStateException(
                    "Insufficient inventory");
        }

        // Step 4: Create purchase only after reservation succeeds
        Purchase purchase = new Purchase(
                productId, customerId, quantity);

        return purchaseRepository.save(purchase);
    }
}
