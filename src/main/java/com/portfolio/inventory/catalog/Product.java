package com.portfolio.inventory.catalog;

import jakarta.persistence.*;

@Entity
@Table(name = "products")
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 50)
    private String sku;
    @Column(nullable = false, length = 120)
    private String name;
    @Column(nullable = false)
    private boolean active;

    protected Product() {}
    public Long getId() { return id; }
    public String getSku() { return sku; }
    public String getName() { return name; }
    public boolean isActive() { return active; }
}
