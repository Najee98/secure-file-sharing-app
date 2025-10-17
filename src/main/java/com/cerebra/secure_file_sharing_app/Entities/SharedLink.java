package com.cerebra.secure_file_sharing_app.Entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "shared_links")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedLink {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String linkToken;
    
    @Column(nullable = false)
    private LocalDateTime expiresAt;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    @ToString.Exclude
    private File file;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    public SharedLink(String linkToken, LocalDateTime expiresAt, File file) {
        this.linkToken = linkToken;
        this.expiresAt = expiresAt;
        this.file = file;
    }
}
