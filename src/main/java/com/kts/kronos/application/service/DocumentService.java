package com.kts.kronos.application.service;

import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.DocumentUseCase;
import com.kts.kronos.application.port.out.provider.DocumentProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.domain.model.Document;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DocumentService implements DocumentUseCase {
    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");
    public static final LocalDateTime TIME_ZONE_BRAZIL = LocalDateTime.now(SAO_PAULO);
    public static final String EMPLOYEE_NOT_FOUND = "Colaborador não encontrado";
    public static final String DOCUMENT_AND_EMPLOYEE_NOT_SAME = "Documento não pertence ao Colaborador";
    private final DocumentProvider documentProvider;
    private final EmployeeProvider employeeProvider;
    @Override
    public Document uploadDocument(UUID employeeId, MultipartFile file) throws IOException {
        var employee = employeeProvider.findById(employeeId).orElseThrow(
                ()-> new ResourceNotFoundException("Colaborador não encontrado"));


        var bytes = file.getBytes();
        var doc = new Document(
                null, file.getOriginalFilename(),file.getContentType(),bytes,TIME_ZONE_BRAZIL, employee.employeeId()
        );
        return documentProvider.save(doc);
    }

    @Override
    public Document downloadDocument(UUID employeeId,UUID documentId) throws IOException {
       var doc = documentProvider.findById(documentId);
       var employee = employeeProvider.findById(employeeId)
               .orElseThrow(()-> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));
       if (!doc.employeeId().equals(employee.employeeId())){
           throw new BadRequestException(DOCUMENT_AND_EMPLOYEE_NOT_SAME);
       }
       return doc;
    }

    @Override
    public List<Document> listDocuments(UUID employeeId, LocalDate date) {
        return date == null
                ? documentProvider.findAllByEmployee(employeeId)
                : documentProvider.findByEmployeeAndDate(employeeId, date);
    }

}
