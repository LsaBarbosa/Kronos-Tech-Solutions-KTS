package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.document.DocumentWithData;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.ForbiddenException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.DocumentUseCase;
import com.kts.kronos.application.port.out.provider.DocumentProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.BucketStorageProvider;
import com.kts.kronos.domain.model.Document;
import com.kts.kronos.domain.model.enuns.DocumentType;
import com.kts.kronos.domain.model.Employee;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.kts.kronos.application.exceptions.BadRequestException;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.kts.kronos.constants.Messages.*;

@Service
@RequiredArgsConstructor
@Transactional
public class DocumentService implements DocumentUseCase {

    private final DocumentProvider documentProvider;
    private final EmployeeProvider employeeProvider;
    private final JwtAuthenticatedUser jwtAuthenticatedUser;
    private final BucketStorageProvider bucketStorageProvider;

     
    @Override
    public void uploadDocument(DocumentType type, UUID employeeId, MultipartFile file) throws IOException {

        var fileMimeType = file.getContentType();
        // 3. Verifique se o tipo está na lista permitida
        if (!ALLOWED_MIME_TYPES.contains(fileMimeType)) {
            // Se não estiver, lança a exceção
            throw new BadRequestException(INVALID_DOCUMENT_TYPE);
        }
        try {
            var employee = getEmployee(employeeId);

            var bytes = file.getBytes();
            var uniqueObjectName = employee.employeeId() + "/" + UUID.randomUUID() + "-" + file.getOriginalFilename();
            var storagePath = bucketStorageProvider.uploadFile(uniqueObjectName, bytes, file.getContentType());
            var doc = new Document(
                    employee.employeeId(),
                    type,
                    file.getOriginalFilename(),
                    file.getContentType(),
                    storagePath, // USANDO O CAMINHO DO GCS
                    TIME_ZONE_BRAZIL,null,false,false
            );
            documentProvider.save(doc);
        } catch (Exception e) {
            throw new BadRequestException(NOT_ABLE_TO_READ_FILE + ": " + e.getMessage());
        }
    }

    @Override
    public DocumentWithData downloadDocument(UUID employeeId, UUID documentId) throws IOException {
        getEmployee(employeeId);

        var doc = documentProvider.findById(documentId);

        try {
            byte[] fileData = bucketStorageProvider.downloadFile(doc.storagePath());

            return new DocumentWithData(
                    doc.documentId(),
                    doc.employeeId(),
                    doc.type(),
                    doc.fileName(),
                    doc.contentType(),
                    fileData,
                    doc.uploadedAt()
            );

        } catch (ResourceNotFoundException e) {
            throw new ResourceNotFoundException(DOCUMENT_NOT_FOUND);
        } catch (RuntimeException e) {
            throw new BadRequestException(ERROR_GET_FILE + e.getMessage());
        }
    }


    @Override
    public List<Document> listDocuments(DocumentType type, UUID employeeId, LocalDate date) {
        // 1. Identifica a Role de quem está logado
        String currentUserRole = jwtAuthenticatedUser.getRoleFromToken();

        // 2. Define se é uma "Visão de Gestor"
        boolean isManagerView = "MANAGER".equals(currentUserRole) || "CTO".equals(currentUserRole);

        // 3. Define o alvo (de quem são os documentos?)
        // O método 'isWithEmployeeId' já garante que um PARTNER só veja os seus próprios docs
        var targetEmployeeId = jwtAuthenticatedUser.isWithEmployeeId(employeeId);

        // 4. Chama o Provider passando a flag de visão
        // O Provider decidirá qual query do Repository executar baseada no booleano
        if (date == null) {
            return documentProvider.findByEmployeeAndType(targetEmployeeId, type, isManagerView);
        } else {
            return documentProvider.findByEmployeeAndDateAndType(targetEmployeeId, date, type, isManagerView);
        }
    }

    @Override
    public void uploadDocumentForTimeRecord(DocumentType type, UUID employeeId, Long timeRecordId, MultipartFile file) throws IOException {
        uploadDocumentInternal(type, employeeId, timeRecordId, file);
    }

    @Override
    public void deleteDocument(UUID employeeId, UUID documentId) {
        var currentUserRole = jwtAuthenticatedUser.getRoleFromToken();
        var currentUserId = jwtAuthenticatedUser.getEmployeeId(); // ou getUserId dependendo da sua lógica de auth
        var doc = documentProvider.findById(documentId);

        var loggedInEmployeeId = jwtAuthenticatedUser.getEmployeeId();

        if (doc.type() == DocumentType.TIME_OFF) {
            if (!doc.employeeId().equals(loggedInEmployeeId)) {
                throw new ForbiddenException(ONLY_OWNER_DELETE_TIME_OFF_DOCS);
            }
        }
        Document updatedDoc;
        boolean isManager = "MANAGER".equals(currentUserRole) || "CTO".equals(currentUserRole);

        if (isManager) {
            updatedDoc = doc.markDeletedByManager();
        } else {
            // Se for funcionário, garante que é o dono
            if (!doc.employeeId().equals(currentUserId)) {
                throw new ForbiddenException("Você não pode apagar documentos de outro funcionário.");
            }
            updatedDoc = doc.markDeletedByEmployee();
        }
        if (updatedDoc.deletedByEmployee() && updatedDoc.deletedByManager()) {

            // Remove arquivo do S3/Disco
            bucketStorageProvider.deleteFile(doc.storagePath());

            // Remove registro do Banco
            documentProvider.delete(doc.employeeId(), doc.documentId()); // Método delete físico existente

        } else {
            // 6. Caso contrário, apenas salvamos o estado atualizado (Soft Delete)
            documentProvider.save(updatedDoc);
        }
    }

    private Employee getEmployee(UUID employeeId) {
        var employeeIdWith = jwtAuthenticatedUser.isWithEmployeeId(employeeId);
        return employeeProvider.findById(employeeIdWith)
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));
    }

    private void uploadDocumentInternal(DocumentType type, UUID employeeId, Long timeRecordId, MultipartFile file) throws IOException {
        var fileMimeType = file.getContentType();
        // 3. Verifique se o tipo está na lista permitida
        if (!ALLOWED_MIME_TYPES.contains(fileMimeType)) {
            // Se não estiver, lança a exceção
            throw new BadRequestException(INVALID_DOCUMENT_TYPE);
        }
        try {
            var employee = getEmployee(employeeId);

            var bytes = file.getBytes();
            var uniqueObjectName = employee.employeeId() + "/" + UUID.randomUUID() + "-" + file.getOriginalFilename();
            var storagePath = bucketStorageProvider.uploadFile(uniqueObjectName, bytes, file.getContentType());
            var doc = new Document(
                    employee.employeeId(),
                    type,
                    file.getOriginalFilename(),
                    file.getContentType(),
                    storagePath, // USANDO O CAMINHO DO GCS
                    TIME_ZONE_BRAZIL,
                    timeRecordId,
                    false,false
            );
            documentProvider.save(doc);
        } catch (Exception e) {
            throw new BadRequestException(NOT_ABLE_TO_READ_FILE + ": " + e.getMessage());
        }
    }

    @Override
    public void uploadGeneratedDocument(DocumentType type, UUID employeeId, Long timeRecordId, byte[] content, String fileName) {
        try {
            // Validação interna básica (opcional, já que geramos o PDF confiável)
            var contentType = "application/pdf";

            var employee = getEmployee(employeeId); // Garante que funcionário existe

            // Define o caminho no Bucket
            var uniqueObjectName = employee.employeeId() + "/receipts/" + UUID.randomUUID() + "-" + fileName;

            // Upload Físico
            var storagePath = bucketStorageProvider.uploadFile(uniqueObjectName, content,contentType);

            // Salva Metadados no Banco
            var doc = new Document(
                    employee.employeeId(),
                    type,
                    fileName,
                    contentType,
                    storagePath,
                    TIME_ZONE_BRAZIL,
                    timeRecordId,false,false
            );
            documentProvider.save(doc);

        } catch (Exception e) {
            throw new BadRequestException(FAILURE_TO_SAVE_AUTO_GENERATED_DOC + e.getMessage());
        }
    }

}
