package com.kts.kronos.application.service;

import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.AcceptTermsUseCase;
import com.kts.kronos.application.port.in.usecase.DocumentUseCase;
import com.kts.kronos.application.port.out.provider.CompanyProvider;
import com.kts.kronos.application.port.out.provider.DocumentProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.S3StorageProvider;
import com.kts.kronos.domain.model.Company;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.enuns.DocumentType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AcceptTermsService implements AcceptTermsUseCase {

    private final EmployeeProvider employeeProvider;
    private final CompanyProvider companyProvider;
    private final BiometricTermPdfService pdfService;
    private final DocumentUseCase documentUseCase; // Seu serviço existente de documentos
    private final DocumentProvider documentProvider;
    private final S3StorageProvider s3StorageProvider; // <--- Aqui o Spring injeta o S3StorageProviderImpl

    @Override
    @Transactional
    public void acceptBiometricTerms(UUID employeeId, String ipAddress) {
        log.info("Iniciando processo de aceite de termos para Employee ID: {}", employeeId);

        Employee employee = employeeProvider.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Colaborador não encontrado"));

        Company company = companyProvider.findById(employee.companyId())
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada"));

        // 1. Gera o PDF assinado eletronicamente
        byte[] pdfBytes = pdfService.generateConsentTerm(employee, company, ipAddress);

        // 2. Define o nome do arquivo
        String filename = String.format("Termo_Aceite_Biometria_%s.pdf", employee.cpf());

        // 3. Salva no Storage (S3/MinIO) e no Banco (tb_documents)
        // Estamos usando o documentService existente. Precisamos garantir que ele aceite byte[]
        // Se o seu método upload aceitar apenas MultipartFile, precisaremos de um adaptador ou chamar um método de baixo nível.
        // Assumindo que você tem ou criará um método 'uploadGeneratedDocument' no DocumentService (vimos isso nos testes anteriores).

        documentUseCase.uploadGeneratedDocument(
                DocumentType.BIOMETRIC_CONSENT_TERM,
                employee.employeeId(),
                null, // Não vinculado a um TimeRecord específico
                pdfBytes,
                filename
        );

        log.info("Termo de Consentimento salvo com sucesso para {}", employee.fullName());
    }

    @Override
    public boolean hasAcceptedBiometricTerm(UUID employeeId) {
        // Regra de Negócio: O usuário aceitou se existir um documento do tipo BIOMETRIC_CONSENT_TERM vinculado a ele.
        return documentProvider.existsByEmployeeIdAndType(
                employeeId,
                DocumentType.BIOMETRIC_CONSENT_TERM
        );
    }
}