package com.cerebra.secure_file_sharing_app.Controllers;

import com.cerebra.secure_file_sharing_app.Entities.AppUser;
import com.cerebra.secure_file_sharing_app.Entities.SharedLink;
import com.cerebra.secure_file_sharing_app.Services.SharedLinkService;
import com.cerebra.secure_file_sharing_app.Shared.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "File Sharing", description = "Create and manage shareable links for files and folders")
public class SharedLinkController {

    private final SharedLinkService sharedLinkService;

    @PostMapping("/api/files/{fileId}/share")
    @Operation(
            summary = "Share a file",
            description = "Create a shareable link for a file. Optionally send SMS notification to recipient."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File shared successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied to file"),
            @ApiResponse(responseCode = "404", description = "File not found")
    })
    public ResponseEntity<ShareResponse> shareFile(
            @Parameter(description = "File ID", required = true)
            @PathVariable Long fileId,

            @Valid @RequestBody CreateShareRequest request,
            Authentication authentication) {

        log.info("Share file request: {} by user: {}", fileId, authentication.getName());

        Long userId = getCurrentUserId(authentication);
        SharedLink sharedLink = sharedLinkService.createFileShare(fileId, userId, request.getRecipientPhone());

        ShareResponse response = mapToShareResponse(sharedLink, "file");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/folders/{folderId}/share")
    @Operation(
            summary = "Share a folder",
            description = "Create a shareable link for a folder. Optionally send SMS notification to recipient."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Folder shared successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied to folder"),
            @ApiResponse(responseCode = "404", description = "Folder not found")
    })
    public ResponseEntity<ShareResponse> shareFolder(
            @Parameter(description = "Folder ID", required = true)
            @PathVariable Long folderId,

            @Valid @RequestBody CreateShareRequest request,
            Authentication authentication) {

        log.info("Share folder request: {} by user: {}", folderId, authentication.getName());

        Long userId = getCurrentUserId(authentication);
        SharedLink sharedLink = sharedLinkService.createFolderShare(folderId, userId, request.getRecipientPhone());

        ShareResponse response = mapToShareResponse(sharedLink, "folder");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/public/shared/{linkToken}")
    @Operation(
            summary = "Download shared file",
            description = "Download a file using a shared link. No authentication required."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "File downloaded successfully"),
            @ApiResponse(responseCode = "404", description = "Invalid or expired share link"),
            @ApiResponse(responseCode = "410", description = "Share link has expired")
    })
    public ResponseEntity<Resource> downloadSharedFile(
            @Parameter(description = "Share link token", required = true)
            @PathVariable String linkToken) {

        log.info("Public download request for token: {}", linkToken);

        Resource resource = sharedLinkService.downloadSharedFile(linkToken);

        // Get share details for filename
        SharedLink sharedLink = sharedLinkService.findByLinkToken(linkToken)
                .orElseThrow(() -> new RuntimeException("Share not found"));

        String filename = sharedLink.getFile() != null ?
                sharedLink.getFile().getDisplayName() : "shared-file";

        String mimeType = sharedLink.getFile() != null ?
                sharedLink.getFile().getMimeType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mimeType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }

    @DeleteMapping("/api/shared/{shareId}")
    @Operation(
            summary = "Revoke shared link",
            description = "Revoke a shared link. User must own the shared file/folder."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Share revoked successfully"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Share not found")
    })
    public ResponseEntity<String> revokeShare(
            @Parameter(description = "Share ID", required = true)
            @PathVariable Long shareId,

            Authentication authentication) {

        log.info("Revoke share request: {} by user: {}", shareId, authentication.getName());

        Long userId = getCurrentUserId(authentication);
        sharedLinkService.revokeShare(shareId, userId);

        return ResponseEntity.ok("Share revoked successfully");
    }

    @GetMapping("/api/files/{fileId}/shares")
    @Operation(
            summary = "Get file shares",
            description = "Get all active shared links for a specific file"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Shares retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied to file"),
            @ApiResponse(responseCode = "404", description = "File not found")
    })
    public ResponseEntity<List<ShareResponse>> getFileShares(
            @Parameter(description = "File ID", required = true)
            @PathVariable Long fileId,

            Authentication authentication) {

        log.info("Get file shares request: {} by user: {}", fileId, authentication.getName());

        List<SharedLink> shares = sharedLinkService.findByFileId(fileId);

        List<ShareResponse> response = shares.stream()
                .map(share -> mapToShareResponse(share, "file"))
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/shared/my-shares")
    @Operation(
            summary = "Get my shares",
            description = "Get all shared links created by the authenticated user"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Shares retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "User not authenticated")
    })
    public ResponseEntity<List<ShareResponse>> getMyShares(Authentication authentication) {

        log.info("Get my shares request by user: {}", authentication.getName());

        Long userId = getCurrentUserId(authentication);
        List<SharedLink> shares = sharedLinkService.getUserShares(userId);

        List<ShareResponse> response = shares.stream()
                .map(share -> mapToShareResponse(share, share.getFile() != null ? "file" : "folder"))
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/public/shared/{linkToken}/info")
    @Operation(
            summary = "Get share info",
            description = "Get information about a shared link without downloading. No authentication required."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Share info retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Invalid or expired share link")
    })
    public ResponseEntity<ShareResponse> getShareInfo(
            @Parameter(description = "Share link token", required = true)
            @PathVariable String linkToken) {

        log.info("Get share info request for token: {}", linkToken);

        SharedLink sharedLink = sharedLinkService.findByLinkToken(linkToken)
                .orElseThrow(() -> new RuntimeException("Invalid or expired share link"));

        ShareResponse response = mapToShareResponse(sharedLink, sharedLink.getFile() != null ? "file" : "folder");
        return ResponseEntity.ok(response);
    }

    // Helper methods
    private Long getCurrentUserId(Authentication authentication) {
        AppUser user = (AppUser) authentication.getPrincipal();
        return user.getId();
    }

    private ShareResponse mapToShareResponse(SharedLink sharedLink, String type) {
        String itemName = "";
        Long itemId = null;

        if (sharedLink.getFile() != null) {
            itemName = sharedLink.getFile().getDisplayName();
            itemId = sharedLink.getFile().getId();
        }

        return ShareResponse.builder()
                .shareId(sharedLink.getId())
                .linkToken(sharedLink.getLinkToken())
                .shareUrl("http://localhost:8080/public/shared/" + sharedLink.getLinkToken())
                .itemType(type)
                .itemName(itemName)
                .itemId(itemId)
                .expiresAt(sharedLink.getExpiresAt())
                .createdAt(sharedLink.getCreatedAt())
                .build();
    }
}