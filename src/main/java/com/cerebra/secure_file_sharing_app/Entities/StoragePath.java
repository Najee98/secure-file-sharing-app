package com.cerebra.secure_file_sharing_app.Entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "storage_paths")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoragePath {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String basePath;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "app_user_id", nullable = false)
    @ToString.Exclude
    private AppUser appUser;

    @OneToMany(mappedBy = "storagePath", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Folder> folders;

    @OneToMany(mappedBy = "storagePath", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<File> files;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public StoragePath(String basePath, AppUser appUser) {
        this.basePath = basePath;
        this.appUser = appUser;
    }
}