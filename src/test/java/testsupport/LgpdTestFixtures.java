package testsupport;

import com.kts.kronos.adapter.out.persistence.CompanyRepository;
import com.kts.kronos.adapter.out.persistence.EmployeeRepository;
import com.kts.kronos.adapter.out.persistence.LegalTextRepository;
import com.kts.kronos.adapter.out.persistence.entity.AddressEmbeddable;
import com.kts.kronos.adapter.out.persistence.entity.CompanyEntity;
import com.kts.kronos.adapter.out.persistence.entity.EmployeeEntity;
import com.kts.kronos.adapter.out.persistence.entity.LegalTextEntity;
import com.kts.kronos.domain.model.enuns.DocumentType;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

@Component
public class LgpdTestFixtures {

    private final LegalTextRepository legalTextRepository;
    private final EmployeeRepository employeeRepository;
    private final CompanyRepository companyRepository;

    public LgpdTestFixtures(
            LegalTextRepository legalTextRepository,
            EmployeeRepository employeeRepository,
            CompanyRepository companyRepository
    ) {
        this.legalTextRepository = legalTextRepository;
        this.employeeRepository = employeeRepository;
        this.companyRepository = companyRepository;
    }

    public UUID seedTestEmployee() {
        var companyId = UUID.randomUUID();

        var address = new AddressEmbeddable();
        address.setPostalCode("01310-100");
        address.setCity("São Paulo");
        address.setState("SP");
        address.setStreet("Avenida Paulista");
        address.setNumber("1578");

        var company = new CompanyEntity();
        company.setId(companyId);
        company.setName("Test Company");
        company.setCnpj("00000000000100");
        company.setEmail("company@example.com");
        company.setActive(true);
        company.setAddress(address);
        companyRepository.save(company);

        var employeeAddress = new AddressEmbeddable();
        employeeAddress.setPostalCode("01310-100");
        employeeAddress.setCity("São Paulo");
        employeeAddress.setState("SP");
        employeeAddress.setStreet("Rua das Flores");
        employeeAddress.setNumber("100");

        var employeeId = UUID.randomUUID();
        var employee = new EmployeeEntity();
        employee.setEmployeeId(employeeId);
        employee.setFullName("Test Employee");
        employee.setCpf("000.000.000-00");
        employee.setJobPosition("Manager");
        employee.setEmail("test@example.com");
        employee.setSalary(5000.0);
        employee.setCompanyId(companyId);
        employee.setActive(true);
        employee.setWorkStartTime(LocalTime.of(8, 0));
        employee.setWorkEndTime(LocalTime.of(17, 0));
        employee.setAddress(employeeAddress);

        employeeRepository.save(employee);
        return employeeId;
    }

    public String seedBiometricTermAndReturnHash() {
        var existingTerm = legalTextRepository.findByDocumentTypeAndActiveTrue(DocumentType.BIOMETRIC_CONSENT_TERM);
        if (existingTerm.isPresent()) {
            return existingTerm.get().getContentHashSha256();
        }

        String biometricContent = "TERMO DE CONSENTIMENTO PARA TRATAMENTO DE DADOS BIOMÉTRICOS\n\n" +
                "O TITULAR autoriza, de forma livre, informada e inequívoca, o tratamento de seus dados pessoais sensíveis, " +
                "especificamente sua IMAGEM FACIAL (Biometria), para a finalidade exclusiva de REGISTRO E CONTROLE DE JORNADA DE TRABALHO, " +
                "em conformidade com a Lei Geral de Proteção de Dados (Lei nº 13.709/2018) e a Portaria 671/2021 do Ministério do Trabalho e Previdência.\n\n" +
                "FINALIDADE: Autenticação segura da identidade no momento do registro de ponto eletrônico, prevenindo fraudes.\n\n" +
                "ARMAZENAMENTO: Os dados serão armazenados em ambiente seguro de computação em nuvem (SaaS) provido pela KRONOS TECH SOLUTIONS.\n\n" +
                "REVOGAÇÃO: Este consentimento poderá ser revogado a qualquer momento pelo Titular, mediante solicitação expressa ao departamento de Recursos Humanos.\n\n" +
                "RETENÇÃO: A imagem facial, os templates biométricos e o termo de consentimento serão mantidos apenas enquanto houver consentimento biométrico ativo.";

        String contentHash = computeSha256(biometricContent);

        LegalTextEntity legalText = new LegalTextEntity();
        legalText.setLegalTextId(UUID.randomUUID());
        legalText.setDocumentType(DocumentType.BIOMETRIC_CONSENT_TERM);
        legalText.setVersion("1.0");
        legalText.setTitle("TERMO DE CONSENTIMENTO PARA TRATAMENTO DE DADOS BIOMÉTRICOS");
        legalText.setContent(biometricContent);
        legalText.setContentHashSha256(contentHash);
        legalText.setActive(true);
        legalText.setCreatedAt(Instant.now());
        legalText.setPublishedAt(Instant.now());

        legalTextRepository.save(legalText);
        return contentHash;
    }

    public void seedBiometricTerm() {
        seedBiometricTermAndReturnHash();
    }


    private String computeSha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return UUID.randomUUID().toString();
        }
    }
}
