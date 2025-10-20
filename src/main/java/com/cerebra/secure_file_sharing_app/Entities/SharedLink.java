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
    @JoinColumn(name = "file_id", nullable = true)
    @ToString.Exclude
    private File file;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id", nullable = true)
    @ToString.Exclude
    private Folder folder;

    // Single @PrePersist method combining both responsibilities
    @PrePersist
    protected void onCreate() {
        // Set creation timestamp
        createdAt = LocalDateTime.now();

        // Validate either file or folder is set, but not both
        if ((file == null && folder == null) || (file != null && folder != null)) {
            throw new IllegalStateException("SharedLink must reference either a file or a folder, but not both");
        }
    }

    // Keep @PreUpdate separate for updates
    @PreUpdate
    protected void onUpdate() {
        // Validate either file or folder is set, but not both
        if ((file == null && folder == null) || (file != null && folder != null)) {
            throw new IllegalStateException("SharedLink must reference either a file or a folder, but not both");
        }
    }
}
