package com.cerebra.secure_file_sharing_app.IntegrationTests;

import com.cerebra.secure_file_sharing_app.Repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @LocalServerPort
    protected int port;

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    protected AppUserRepository appUserRepository;

    @Autowired
    protected FileRepository fileRepository;

    @Autowired
    protected FolderRepository folderRepository;

    @Autowired
    protected SharedLinkRepository sharedLinkRepository;

    @Autowired
    protected StoragePathRepository storagePathRepository;

    @BeforeEach
    void cleanupDatabase() {
        sharedLinkRepository.deleteAll();
        fileRepository.deleteAll();
        folderRepository.deleteAll();
        storagePathRepository.deleteAll();
        appUserRepository.deleteAll();
    }

    protected String baseUrl() {
        return "http://localhost:" + port;
    }
}