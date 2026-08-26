package com.lockelite.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "banks")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Bank {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String code;          // LOCKELITE, SBI, HDFC, ICICI, AXIS, KOTAK

    @Column(nullable = false, length = 100)
    private String name;          // "State Bank of India"

    @Column(name = "primary_color", nullable = false, length = 10)
    private String primaryColor;  // e.g. #2D6BB5

    @Column(name = "sidebar_color", nullable = false, length = 10)
    private String sidebarColor;  // e.g. #1A3A6B

    @Column(name = "bg_color", nullable = false, length = 10)
    private String bgColor;       // e.g. #F0F4F8

    @Column(name = "accent_color", length = 10)
    private String accentColor;   // e.g. #E6EEF8

    @Column(nullable = false, length = 20)
    private String layout;        // sidebar | top-nav | panel | tab-nav | icon-sidebar

    @Column(name = "logo_text", length = 10)
    private String logoText;      // LE, SBI, HDFC, etc.

    @Column(name = "is_active")
    private Boolean isActive = true;
}
