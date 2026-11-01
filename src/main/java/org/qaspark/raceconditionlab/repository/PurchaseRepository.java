
package org.qaspark.raceconditionlab.repository;

import org.qaspark.raceconditionlab.entity.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    long countByProductId(String productId);
}
