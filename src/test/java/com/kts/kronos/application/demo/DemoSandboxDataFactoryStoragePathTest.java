package com.kts.kronos.application.demo;

import com.kts.kronos.adapter.out.persistence.*;
import com.kts.kronos.adapter.out.persistence.entity.DocumentEntity;
import com.kts.kronos.adapter.out.persistence.entity.TimeRecordEntity;
import com.kts.kronos.application.service.demo.DemoSandboxDataFactory;
import com.kts.kronos.config.demo.DemoSandboxProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DemoSandboxDataFactoryStoragePathTest {

    @TempDir Path tempDir;

    @Mock CompanyRepository            companyRepo;
    @Mock UserRepository               userRepo;
    @Mock EmployeeRepository           employeeRepo;
    @Mock UserCompanyAccessRepository  accessRepo;
    @Mock TimeRecordRepository         timeRecordRepo;
    @Mock DocumentRepository           documentRepo;
    @Mock LegalConsentRepository       consentRepo;
    @Mock TimeRecordApprovalRepository approvalRepo;
    @Mock PasswordEncoder              passwordEncoder;
    @Mock DemoSandboxProperties        props;

    @InjectMocks
    DemoSandboxDataFactory factory;

    @BeforeEach
    void setup() {
        when(props.getLocalStorageRoot()).thenReturn(tempDir.toString());
        when(props.getSandboxKey()).thenReturn("KRONOS_TESTE");
        when(props.getSandboxKey()).thenReturn("KRONOS_TESTE");
        when(props.getCompanyName()).thenReturn("Kronos Teste");
        when(props.getUsername()).thenReturn("kronos_teste");
        when(props.getInitialPassword()).thenReturn("kronos_teste#");
        when(passwordEncoder.encode(any())).thenReturn("$2a$encoded");
        // createPendingRequests does saved.get(2), so mock must return at least 3 elements
        TimeRecordEntity stub = com.kts.kronos.adapter.out.persistence.entity.TimeRecordEntity.builder()
                .employeeId(UUID.randomUUID()).build();
        when(timeRecordRepo.saveAll(any())).thenReturn(List.of(stub, stub, stub));
        when(companyRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(employeeRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(userRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(accessRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(consentRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(approvalRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(documentRepo.saveAll(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void createDocuments_shouldStoreRelativePathNotAbsolute() {
        factory.createAll();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DocumentEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(documentRepo).saveAll(captor.capture());

        List<DocumentEntity> saved = captor.getValue();
        assertThat(saved).isNotEmpty();
        saved.forEach(doc -> {
            String path = doc.getStoragePath();
            assertThat(path)
                    .as("storagePath must be relative, not absolute: %s", path)
                    .doesNotStartWith("/")
                    .doesNotContain("/opt/")
                    .doesNotContain(tempDir.toString())
                    .startsWith("company/");
        });
    }

    @Test
    void createDocuments_relativePathShouldResolveToExistingFile() {
        factory.createAll();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DocumentEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(documentRepo).saveAll(captor.capture());

        List<DocumentEntity> saved = captor.getValue();
        saved.forEach(doc -> {
            Path fullPath = Path.of(tempDir.toString(), doc.getStoragePath());
            assertThat(fullPath).exists()
                    .as("Relative path must resolve to a real file under localStorageRoot");
        });
    }

    @Test
    void createDocuments_relativePathShouldNotContainSensitiveServerInfo() {
        factory.createAll();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DocumentEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(documentRepo).saveAll(captor.capture());

        captor.getValue().forEach(doc -> {
            String path = doc.getStoragePath();
            assertThat(path).doesNotContain("kronos-teste");
            assertThat(path).doesNotContain("sandbox");
            assertThat(path).doesNotContain("opt");
        });
    }
}
