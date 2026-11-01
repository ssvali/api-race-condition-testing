package org.qaspark.raceconditionlab.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "purchases")
public class Purchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String productId;

    @Column(nullable = false)
    private String customerId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private LocalDateTime purchasedAt;

    protected Purchase() {
    }

    public Purchase(String productId, String customerId,
                    Integer quantity) {
        this.productId = productId;
        this.customerId = customerId;
        this.quantity = quantity;
        this.purchasedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getProductId() {
        return productId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public LocalDateTime getPurchasedAt() {
        return purchasedAt;
    }
}
