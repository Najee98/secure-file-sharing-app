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
@Schema(description = "File upload response")
public class FileUploadResponse {
    
    @Schema(description = "Uploaded file ID", example = "1")
    private Long fileId;
    
    @Schema(description = "Original file name", example = "document.pdf")
    private String fileName;
    
    @Schema(description = "File size in bytes", example = "1024576")
    private Long fileSize;
    
    @Schema(description = "MIME type", example = "application/pdf")
    private String mimeType;
    
    @Schema(description = "Upload timestamp")
    private LocalDateTime uploadedAt;
    
    @Schema(description = "Success message", example = "File uploaded successfully")
    private String message;
}
