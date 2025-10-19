package com.cerebra.secure_file_sharing_app.Shared;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Shared link information")
public class ShareResponse {
    
    @Schema(description = "Share ID", example = "1")
    private Long shareId;
    
    @Schema(description = "Unique link token", example = "abc-123-def-456")
    private String linkToken;
    
    @Schema(description = "Complete share URL", example = "http://localhost:8080/public/shared/abc-123-def-456")
    private String shareUrl;
    
    @Schema(description = "Type of shared item", example = "file")
    private String itemType; // "file" or "folder"
    
    @Schema(description = "Name of shared item", example = "document.pdf")
    private String itemName;
    
    @Schema(description = "ID of shared item", example = "123")
    private Long itemId;
    
    @Schema(description = "Share expiration timestamp")
    private LocalDateTime expiresAt;
    
    @Schema(description = "Share creation timestamp")
    private LocalDateTime createdAt;
}