package com.cerebra.secure_file_sharing_app.Shared;

import com.cerebra.secure_file_sharing_app.Aspects.Sanitization.SanitizedField;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to create a new folder")
public class CreateFolderRequest {
    
    @NotBlank(message = "Folder name is required")
    @Size(max = 255, message = "Folder name must not exceed 255 characters")
    @Schema(description = "Folder name", example = "My Documents")
    @Pattern(regexp = "^[a-zA-Z0-9\\s._-]+$", message = "Folder name contains invalid characters")
    @SanitizedField
    private String name;
    
    @Schema(description = "Parent folder ID (null for root level)", example = "1")
    @SanitizedField
    private Long parentFolderId;
}