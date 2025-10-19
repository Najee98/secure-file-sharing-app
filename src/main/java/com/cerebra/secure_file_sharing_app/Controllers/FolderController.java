package com.cerebra.secure_file_sharing_app.Controllers;

import com.cerebra.secure_file_sharing_app.Entities.*;
import com.cerebra.secure_file_sharing_app.Services.*;
import com.cerebra.secure_file_sharing_app.Shared.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/folders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Folder Management", description = "Folder creation, deletion, and navigation operations")
public class FolderController {

    private final FolderService folderService;
    private final StoragePathService storagePathService;

    @PostMapping
    @Operation(
            summary = "Create a new folder",
            description = "Create a new folder in user's storage. Can be nested under a parent folder."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Folder created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid folder name or duplicate name"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied to parent folder"),
            @ApiResponse(responseCode = "404", description = "Parent folder not found")
    })
    public ResponseEntity<FolderResponse> createFolder(
            @Valid @RequestBody CreateFolderRequest request,
            Authentication authentication) {

        log.info("Create folder request: '{}' by user: {}", request.getName(), authentication.getName());

        Long userId = getCurrentUserId(authentication);
        Folder folder = folderService.createFolder(request.getName(), request.getParentFolderId(), userId);

        FolderResponse response = mapToFolderResponse(folder);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{folderId}")
    @Operation(
            summary = "Delete a folder",
            description = "Delete a folder. Folder must be empty (no subfolders or files)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Folder deleted successfully"),
            @ApiResponse(responseCode = "400", description = "Folder contains files or subfolders"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied to folder"),
            @ApiResponse(responseCode = "404", description = "Folder not found")
    })
    public ResponseEntity<String> deleteFolder(
            @Parameter(description = "Folder ID", required = true)
            @PathVariable Long folderId,
            Authentication authentication) {

        log.info("Delete folder request: {} by user: {}", folderId, authentication.getName());

        Long userId = getCurrentUserId(authentication);
        folderService.deleteFolder(folderId, userId);

        return ResponseEntity.ok("Folder deleted successfully");
    }

    @GetMapping("/my-folders")
    @Operation(
            summary = "Get all user folders",
            description = "Retrieve all folders belonging to the authenticated user"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Folders retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "User not authenticated")
    })
    public ResponseEntity<List<FolderResponse>> getMyFolders(Authentication authentication) {

        log.info("Get my folders request by user: {}", authentication.getName());

        Long userId = getCurrentUserId(authentication);
        // Get user's storage path ID
        Long storagePathId = getCurrentUserStoragePathId(userId);
        List<Folder> folders = folderService.findByStoragePathId(storagePathId);

        List<FolderResponse> response = folders.stream()
                .map(this::mapToFolderResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/root")
    @Operation(
            summary = "Get root folders",
            description = "Retrieve folders at the root level (no parent folder)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Root folders retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "User not authenticated")
    })
    public ResponseEntity<List<FolderResponse>> getRootFolders(Authentication authentication) {

        log.info("Get root folders request by user: {}", authentication.getName());

        Long userId = getCurrentUserId(authentication);
        Long storagePathId = getCurrentUserStoragePathId(userId);
        List<Folder> folders = folderService.findRootFolders(storagePathId);

        List<FolderResponse> response = folders.stream()
                .map(this::mapToFolderResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{folderId}/subfolders")
    @Operation(
            summary = "Get subfolders",
            description = "Retrieve all subfolders within a specific folder"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Subfolders retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied to folder"),
            @ApiResponse(responseCode = "404", description = "Folder not found")
    })
    public ResponseEntity<List<FolderResponse>> getSubfolders(
            @Parameter(description = "Parent folder ID", required = true)
            @PathVariable Long folderId,
            Authentication authentication) {

        log.info("Get subfolders request: {} by user: {}", folderId, authentication.getName());

        Long userId = getCurrentUserId(authentication);

        // Validate access to parent folder
        if (!folderService.hasAccess(folderId, userId)) {
            throw new RuntimeException("Access denied to folder: " + folderId);
        }

        List<Folder> subfolders = folderService.findByParentFolderId(folderId);

        List<FolderResponse> response = subfolders.stream()
                .map(this::mapToFolderResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{folderId}")
    @Operation(
            summary = "Get folder details",
            description = "Retrieve details of a specific folder"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Folder details retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied to folder"),
            @ApiResponse(responseCode = "404", description = "Folder not found")
    })
    public ResponseEntity<FolderResponse> getFolderDetails(
            @Parameter(description = "Folder ID", required = true)
            @PathVariable Long folderId,
            Authentication authentication) {

        log.info("Get folder details request: {} by user: {}", folderId, authentication.getName());

        Long userId = getCurrentUserId(authentication);

        if (!folderService.hasAccess(folderId, userId)) {
            throw new RuntimeException("Access denied to folder: " + folderId);
        }

        Folder folder = folderService.findById(folderId)
                .orElseThrow(() -> new RuntimeException("Folder not found: " + folderId));

        FolderResponse response = mapToFolderResponse(folder);
        return ResponseEntity.ok(response);
    }

    // Helper methods
    private Long getCurrentUserId(Authentication authentication) {
        AppUser user = (AppUser) authentication.getPrincipal();
        return user.getId();
    }

    private Long getCurrentUserStoragePathId(Long userId) {
        return storagePathService.findByAppUserId(userId)
                .orElseThrow(() -> new RuntimeException("User storage path not found"))
                .getId();
    }

    private FolderResponse mapToFolderResponse(Folder folder) {
        return FolderResponse.builder()
                .id(folder.getId())
                .name(folder.getName())
                .parentFolderId(folder.getParentFolder() != null ? folder.getParentFolder().getId() : null)
                .createdAt(folder.getCreatedAt())
                .updatedAt(folder.getUpdatedAt())
                .build();
    }
}