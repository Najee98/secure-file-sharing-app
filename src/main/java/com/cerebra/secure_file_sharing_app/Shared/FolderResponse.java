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
@Schema(description = "Folder information response")
public class FolderResponse {
    
    @Schema(description = "Folder ID", example = "1")
    private Long id;
    
    @Schema(description = "Folder name", example = "My Documents")
    private String name;
    
    @Schema(description = "Parent folder ID (null if root level)")
    private Long parentFolderId;
    
    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;
    
    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;
}