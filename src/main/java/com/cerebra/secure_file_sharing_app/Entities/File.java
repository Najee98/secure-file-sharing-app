package com.cerebra.secure_file_sharing_app.Entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "files")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class File {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String displayName;
    
    @Column(nullable = false, unique = true)
    private String physicalName;
    
    @Column(nullable = false)
    private String physicalPath;
    
    @Column(nullable = false)
    private Long size;
    
    private String mimeType;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "storage_path_id", nullable = false)
    @ToString.Exclude
    @JsonIgnore
    private StoragePath storagePath;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id")
    @ToString.Exclude
    @JsonIgnore
    private Folder folder;
    
    @OneToMany(mappedBy = "file", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    @JsonIgnore
    private List<SharedLink> sharedLinks;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public File(String displayName, String physicalName, String physicalPath, Long size, String mimeType, StoragePath storagePath) {
        this.displayName = displayName;
        this.physicalName = physicalName;
        this.physicalPath = physicalPath;
        this.size = size;
        this.mimeType = mimeType;
        this.storagePath = storagePath;
    }
}
