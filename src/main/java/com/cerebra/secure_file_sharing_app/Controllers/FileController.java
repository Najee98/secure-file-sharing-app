package com.cerebra.secure_file_sharing_app.Controllers;

import com.cerebra.secure_file_sharing_app.Entities.*;

import com.cerebra.secure_file_sharing_app.Security.JWT.JwtService;
import com.cerebra.secure_file_sharing_app.Services.FileService;
import com.cerebra.secure_file_sharing_app.Services.StoragePathService;
import com.cerebra.secure_file_sharing_app.Shared.FileUploadResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "File Management", description = "File upload, download, and management operations")
public class FileController {

    private final FileService fileService;
    private final StoragePathService storagePathService;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload a file",
            description = "Upload a file to user's storage. Optionally specify a folder."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File uploaded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid file or request"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied to folder"),
            @ApiResponse(responseCode = "413", description = "File too large")
    })
    public ResponseEntity<FileUploadResponse> uploadFile(
            @Parameter(description = "File to upload", required = true)
            @RequestParam("file") MultipartFile file,

            @Parameter(description = "Folder ID (optional - null for root)")
            @RequestParam(value = "folderId", required = false) Long folderId,

            Authentication authentication) {

        log.info("File upload request: {} by user: {}", file.getOriginalFilename(), authentication.getName());

        Long userId = getCurrentUserId(authentication);
        File uploadedFile = fileService.uploadFile(file, folderId, userId);

        FileUploadResponse response = FileUploadResponse.builder()
                .fileId(uploadedFile.getId())
                .fileName(uploadedFile.getDisplayName())
                .fileSize(uploadedFile.getSize())
                .mimeType(uploadedFile.getMimeType())
                .uploadedAt(uploadedFile.getCreatedAt())
                .message("File uploaded successfully")
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{fileId}/download")
    @Operation(
            summary = "Download a file",
            description = "Download a file by its ID. User must own the file."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File downloaded successfully"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied to file"),
            @ApiResponse(responseCode = "404", description = "File not found")
    })
    public ResponseEntity<Resource> downloadFile(
            @Parameter(description = "File ID", required = true)
            @PathVariable Long fileId,

            Authentication authentication) {

        log.info("File download request: {} by user: {}", fileId, authentication.getName());

        Long userId = getCurrentUserId(authentication);
        Resource resource = fileService.downloadFile(fileId, userId);

        // Get file info for headers
        File fileEntity = fileService.findById(fileId).orElseThrow();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fileEntity.getMimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileEntity.getDisplayName() + "\"")
                .body(resource);
    }

    @DeleteMapping("/{fileId}")
    @Operation(
            summary = "Delete a file",
            description = "Delete a file by its ID. User must own the file."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File deleted successfully"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied to file"),
            @ApiResponse(responseCode = "404", description = "File not found")
    })
    public ResponseEntity<String> deleteFile(
            @Parameter(description = "File ID", required = true)
            @PathVariable Long fileId,

            Authentication authentication) {

        log.info("File delete request: {} by user: {}", fileId, authentication.getName());

        Long userId = getCurrentUserId(authentication);
        fileService.deleteFile(fileId, userId);

        return ResponseEntity.ok("File deleted successfully");
    }

    @GetMapping("/my-files")
    @Operation(
            summary = "Get all user files",
            description = "Retrieve all files belonging to the authenticated user"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Files retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "User not authenticated")
    })
    public ResponseEntity<List<File>> getMyFiles(Authentication authentication) {

        log.info("Get my files request by user: {}", authentication.getName());

        Long userId = getCurrentUserId(authentication);
        List<File> files = fileService.getUserFiles(userId);

        return ResponseEntity.ok(files);
    }

    @GetMapping("/folder/{folderId}")
    @Operation(
            summary = "Get files in folder",
            description = "Retrieve all files in a specific folder"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Files retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied to folder"),
            @ApiResponse(responseCode = "404", description = "Folder not found")
    })
    public ResponseEntity<List<File>> getFolderFiles(
            @Parameter(description = "Folder ID", required = true)
            @PathVariable Long folderId,

            Authentication authentication) {

        log.info("Get folder files request: {} by user: {}", folderId, authentication.getName());

        Long userId = getCurrentUserId(authentication);
        List<File> files = fileService.getFolderFiles(folderId, userId);

        return ResponseEntity.ok(files);
    }

    @GetMapping("/root")
    @Operation(
            summary = "Get root files",
            description = "Retrieve files in the user's root directory (not in any folder)"
    )
    public ResponseEntity<List<File>> getRootFiles(Authentication authentication) {

        log.info("Get root files request by user: {}", authentication.getName());

        Long userId = getCurrentUserId(authentication);
        Long storagePathId = getCurrentUserStoragePathId(userId);
        List<File> files = fileService.findRootFiles(storagePathId);

        return ResponseEntity.ok(files);
    }

    @GetMapping("/{fileId}/preview")
    @Operation(
            summary = "Preview a file",
            description = "Stream file content for preview (inline display)"
    )
    public ResponseEntity<Resource> previewFile(
            @PathVariable Long fileId,
            @RequestParam(value = "token", required = false) String tokenParam,
            Authentication authentication) {

        log.info("File preview request: {} by user: {}", fileId, authentication != null ? authentication.getName() : "token-based");

        Long userId;

        // If token is provided as query param (for media elements that can't send headers)
        if (tokenParam != null && authentication == null) {
            // Validate token and extract user
            String phoneNumber = jwtService.extractUsername(tokenParam);
            UserDetails userDetails = userDetailsService.loadUserByUsername(phoneNumber);

            if (!jwtService.isTokenValid(tokenParam, userDetails)) {
                throw new RuntimeException("Invalid or expired token");
            }

            // Get user ID from phone number
            AppUser user = (AppUser) userDetails;
            userId = user.getId();
        } else if (authentication != null) {
            userId = getCurrentUserId(authentication);
        } else {
            throw new RuntimeException("Authentication required");
        }

        Resource resource = fileService.downloadFile(fileId, userId);
        File fileEntity = fileService.findById(fileId).orElseThrow();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fileEntity.getMimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileEntity.getDisplayName() + "\"")
                .body(resource);
    }

    // Fixed helper methods
    private Long getCurrentUserId(Authentication authentication) {
        AppUser user = (AppUser) authentication.getPrincipal();
        return user.getId();
    }

    private Long getCurrentUserStoragePathId(Long userId) {
        return storagePathService.findByAppUserId(userId)
                .orElseThrow(() -> new RuntimeException("User storage path not found"))
                .getId();
    }
}

