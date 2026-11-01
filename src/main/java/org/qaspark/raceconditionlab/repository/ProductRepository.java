
package org.qaspark.raceconditionlab.repository;

import org.qaspark.raceconditionlab.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductRepository
        extends JpaRepository<Product, Long> {

    Optional<Product> findByProductId(String productId);

    @Modifying
    @Query("""
        UPDATE Product p
        SET p.availableQuantity = p.availableQuantity - :quantity
        WHERE p.productId = :productId
        AND p.availableQuantity >= :quantity
        """)
    int reserveInventory(
            @Param("productId") String productId,
            @Param("quantity") int quantity
    );
}
