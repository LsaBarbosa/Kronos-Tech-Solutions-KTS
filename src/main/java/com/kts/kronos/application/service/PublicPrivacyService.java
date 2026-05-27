package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.public_privacy.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PublicPrivacyService {

    public PublicProcessingCatalogResponse getPublicProcessingCatalog() {
        return new PublicProcessingCatalogResponse(
                "2026.05.1",
                LocalDate.of(2026, 5, 27),
                buildActivities()
        );
    }

    public PublicPrivacyPolicyResponse getPublicPrivacyPolicy() {
        return new PublicPrivacyPolicyResponse(
                "2026.05.1",
                LocalDate.of(2026, 5, 27),
                "Política de Privacidade e Proteção de Dados - Kronos",
                buildPolicySections()
        );
    }

    public PublicBiometricTermResponse getPublicBiometricTerm() {
        return new PublicBiometricTermResponse(
                "2026.05.1",
                LocalDate.of(2026, 5, 27),
                "Termo de Consentimento e Uso de Biometria Facial",
                buildBiometricSections()
        );
    }

    private List<PublicProcessingActivityResponse> buildActivities() {
        return List.of(
                new PublicProcessingActivityResponse(
                        "COMPANY_REGISTRATION",
                        "Cadastro de Empresa",
                        "Operação de cadastro e administração da conta empresarial na plataforma Kronos.",
                        List.of("Dados cadastrais da empresa", "Dados de contato corporativo", "Dados de endereço"),
                        List.of("Criação e administração da conta empresarial", "Execução do contrato de uso", "Suporte operacional"),
                        List.of("Execução de contrato", "Legítimo interesse", "Cumprimento de obrigação legal"),
                        "Enquanto a conta estiver ativa; posteriormente pelo prazo necessário para exercício regular de direitos e cumprimento de obrigações legais",
                        List.of("Acesso aos dados", "Correção de dados", "Oposição ao tratamento", "Portabilidade")
                ),
                new PublicProcessingActivityResponse(
                        "EMPLOYEE_REGISTRATION",
                        "Cadastro de Colaborador",
                        "Operação de cadastro e manutenção de dados profissionais do colaborador na plataforma.",
                        List.of("Identificação civil", "Dados de contato", "Dados profissionais", "Dados de jornada", "Dados de vínculo empresarial"),
                        List.of("Gestão de jornada", "Controle operacional do ponto", "Emissão de relatórios", "Atendimento a obrigações trabalhistas"),
                        List.of("Execução de contrato", "Cumprimento de obrigação legal", "Exercício regular de direitos"),
                        "Durante vínculo operacional com a empresa; após encerramento, pelo prazo necessário para obrigações legais e defesa de direitos",
                        List.of("Acesso aos dados", "Correção de dados", "Exclusão de dados", "Portabilidade")
                ),
                new PublicProcessingActivityResponse(
                        "TIME_RECORD",
                        "Registro Eletrônico de Ponto",
                        "Operação de marcação eletrônica de ponto e geração de comprovantes e documentos legais.",
                        List.of("Data e hora de marcação", "Geolocalização (quando aplicável)", "Status do registro", "Evidências técnicas"),
                        List.of("Controle de jornada", "Geração de comprovantes", "Emissão de relatórios", "Geração de documentos legais (AFD, AEJ, espelho)"),
                        List.of("Cumprimento de obrigação legal", "Execução de contrato", "Exercício regular de direitos"),
                        "Conforme necessidade legal, fiscal, trabalhista e defesa de direitos",
                        List.of("Acesso aos dados", "Correção de dados", "Portabilidade", "Oposição ao tratamento")
                ),
                new PublicProcessingActivityResponse(
                        "FACIAL_BIOMETRY",
                        "Biometria Facial",
                        "Operação de captura, armazenamento e processamento de dados biométricos faciais para autenticação e controle de ponto.",
                        List.of("Imagem facial", "Identificadores biométricos derivados", "Evidência de consentimento", "Logs técnicos mínimos"),
                        List.of("Autenticação do colaborador", "Prevenção de fraude", "Segurança da marcação de ponto"),
                        List.of("Consentimento específico", "Cumprimento de obrigação legal", "Exercício regular de direitos"),
                        "Enquanto houver consentimento válido e necessidade operacional; até revogação ou término da finalidade",
                        List.of("Revogação de consentimento", "Exclusão de dados", "Acesso aos dados")
                ),
                new PublicProcessingActivityResponse(
                        "DOCUMENTS",
                        "Documentos Enviados ou Gerados",
                        "Armazenamento e processamento de documentos anexados, comprovantes e documentos legais gerados pela plataforma.",
                        List.of("Documentos anexados", "Comprovantes", "Documentos de ponto", "Documentos legais gerados"),
                        List.of("Comprovação de solicitações", "Auditoria", "Geração de evidências", "Suporte a obrigações trabalhistas"),
                        List.of("Cumprimento de obrigação legal", "Execução de contrato", "Exercício regular de direitos"),
                        "Conforme tipo documental e necessidade legal/contratual",
                        List.of("Acesso aos dados", "Exclusão de dados", "Portabilidade")
                ),
                new PublicProcessingActivityResponse(
                        "LGPD_REQUESTS",
                        "Solicitações LGPD",
                        "Operação de recebimento, processamento e atendimento de solicitações dos titulares de direitos LGPD.",
                        List.of("Identificação do solicitante", "Tipo de solicitação", "Histórico de atendimento", "Evidências de conclusão"),
                        List.of("Atender direitos do titular", "Manter registro de atendimento", "Demonstrar conformidade"),
                        List.of("Cumprimento de obrigação legal", "Exercício regular de direitos"),
                        "Pelo prazo necessário para demonstrar atendimento e conformidade",
                        List.of("Acesso aos dados", "Qualquer direito LGPD aplicável")
                )
        );
    }

    private List<PrivacyPolicySectionResponse> buildPolicySections() {
        return List.of(
                new PrivacyPolicySectionResponse(
                        "Quem Opera a Plataforma",
                        "A plataforma Kronos é operada por empresa fornecedora de soluções de controle de jornada eletrônico. " +
                                "Esta política aplica-se ao tratamento de dados realizado pela plataforma e seus operadores autorizados."
                ),
                new PrivacyPolicySectionResponse(
                        "Dados Que Podem Ser Tratados",
                        "A plataforma processa dados de empresas, colaboradores, usuários e titulares LGPD, incluindo: " +
                                "identificação, contato, endereço, dados profissionais, jornada, ponto eletrônico, geolocalização (quando aplicável), " +
                                "biometria facial, documentos, e registros de solicitações LGPD."
                ),
                new PrivacyPolicySectionResponse(
                        "Finalidades do Tratamento",
                        "Os dados são tratados para: execução de contrato de uso da plataforma, gestão de jornada, " +
                                "emissão de comprovantes e documentos legais, prevenção de fraude, atendimento a obrigações trabalhistas e legais, " +
                                "e atendimento a direitos dos titulares."
                ),
                new PrivacyPolicySectionResponse(
                        "Bases Legais",
                        "O tratamento é fundamentado em: execução de contrato, cumprimento de obrigação legal ou regulatória, " +
                                "exercício regular de direitos, legítimo interesse, e consentimento específico quando necessário (especialmente para biometria)."
                ),
                new PrivacyPolicySectionResponse(
                        "Uso de Biometria Facial",
                        "A plataforma pode utilizar dados biométricos faciais para autenticação do colaborador e segurança da marcação de ponto. " +
                                "O uso de biometria exige consentimento específico do titular, que pode ser revogado a qualquer momento. " +
                                "A revogação pode exigir método alternativo de autenticação definido pela empresa. " +
                                "Consulte o Termo de Biometria Facial para detalhes."
                ),
                new PrivacyPolicySectionResponse(
                        "Uso de Geolocalização",
                        "A plataforma pode registrar geolocalização durante marcação de ponto, quando configurado pela empresa. " +
                                "A geolocalização é tratada com base em contrato, obrigação legal, e consentimento do titular. " +
                                "Os dados de localização são armazenados com restrição de acesso e usados apenas para fins autorizados."
                ),
                new PrivacyPolicySectionResponse(
                        "Compartilhamento com Operadores e Infraestrutura",
                        "Os dados podem ser compartilhados com provedores de infraestrutura e serviços que atuam como processadores de dados, " +
                                "incluindo provedores de armazenamento em nuvem, segurança, auditoria e backup. " +
                                "Todos os processadores são contratualmente obrigados a manter confidencialidade e cumprir LGPD. " +
                                "A plataforma não compartilha dados com terceiros fora do escopo do contrato sem consentimento explícito."
                ),
                new PrivacyPolicySectionResponse(
                        "Retenção e Descarte",
                        "Os dados são retidos pelo tempo necessário para cumprir as finalidades declaradas e obrigações legais. " +
                                "Após esse período, os dados são descartados de forma segura. " +
                                "Períodos específicos variam por tipo de dado: dados de ponto podem ser retidos por anos por obrigação fiscal, " +
                                "enquanto dados temporários são descartados em prazos menores. Consulte o Catálogo de Tratamento de Dados para detalhes."
                ),
                new PrivacyPolicySectionResponse(
                        "Direitos do Titular",
                        "Você possui direitos LGPD, incluindo: acesso aos dados que mantemos sobre você, correção de dados imprecisos, " +
                                "exclusão (direito ao esquecimento) quando aplicável, portabilidade dos dados para outro prestador, " +
                                "oposição ao tratamento em alguns casos, e revogação de consentimento. " +
                                "Para exercer direitos, abra uma solicitação LGPD na plataforma ou entre em contato pelo canal de privacidade."
                ),
                new PrivacyPolicySectionResponse(
                        "Canal de Contato",
                        "Dúvidas sobre esta política ou para exercer direitos LGPD, entre em contato através da plataforma " +
                                "ou envie mensagem ao encarregado de proteção de dados. A resposta será fornecida no prazo legal (30 dias)."
                ),
                new PrivacyPolicySectionResponse(
                        "Segurança da Informação",
                        "A plataforma implementa medidas técnicas e organizacionais apropriadas para proteger dados contra acesso não autorizado, " +
                                "alteração, destruição ou divulgação. Essas medidas incluem criptografia, controle de acesso, auditoria, " +
                                "e treinamento de equipes. Porém, nenhuma transmissão de dados é 100% segura; o seu uso da plataforma é por sua conta e risco."
                ),
                new PrivacyPolicySectionResponse(
                        "Alterações desta Política",
                        "Esta política pode ser atualizada para refletir mudanças legais, operacionais ou de infraestrutura. " +
                                "Alterações significativas serão comunicadas com antecedência. O uso contínuo da plataforma após alterações implica aceitação das novas condições."
                )
        );
    }

    private List<BiometricTermSectionResponse> buildBiometricSections() {
        return List.of(
                new BiometricTermSectionResponse(
                        "O Que é Biometria Facial no Kronos",
                        "Biometria facial é um método de autenticação que usa características únicas do seu rosto para verificar sua identidade. " +
                                "A plataforma Kronos captura uma imagem facial durante o cadastro de biometria e processa dados biométricos derivados (características faciais) " +
                                "para comparação em futuras marcações de ponto ou acessos. A imagem original pode ser descartada após o processamento."
                ),
                new BiometricTermSectionResponse(
                        "Para Que a Biometria é Usada",
                        "A biometria facial no Kronos é usada para: (1) autenticar você ao fazer login na plataforma; " +
                                "(2) permitir marcação de ponto sem necessidade de senha; (3) prevenção de fraude e acesso não autorizado; " +
                                "(4) segurança dos registros de ponto. A biometria facilita o seu uso do sistema e protege a integridade dos registros."
                ),
                new BiometricTermSectionResponse(
                        "Quais Dados São Tratados",
                        "Durante o cadastro de biometria, você autoriza o tratamento de: (1) imagem facial capturada no momento do cadastro; " +
                                "(2) dados biométricos extraídos (características faciais processadas); (3) timestamp do cadastro e alterações; " +
                                "(4) logs técnicos mínimos de aceites. A plataforma não armazena propósitos pessoais identificáveis fora do contexto da biometria."
                ),
                new BiometricTermSectionResponse(
                        "Base Legal e Necessidade de Consentimento",
                        "O uso de biometria facial exige seu consentimento explícito, conforme LGPD. " +
                                "Você consente especificamente ao clicar \"aceito biometria\" nesta tela. " +
                                "O consentimento é voluntário e independente do uso da plataforma, exceto se a empresa exigir biometria como único método de ponto. " +
                                "Nesse caso, método alternativo deve ser oferecido."
                ),
                new BiometricTermSectionResponse(
                        "Consequências da Recusa ou Revogação",
                        "Se recusar ou revogar consentimento: (1) você não poderá usar biometria para login ou marcação de ponto; " +
                                "(2) você poderá usar senha ou método alternativo (se disponível) definido pela empresa; " +
                                "(3) a recusa não impede o acesso à plataforma, desde que haja método alternativo. " +
                                "Revogação é sempre possível sem justificativa prévia."
                ),
                new BiometricTermSectionResponse(
                        "Revogação do Consentimento",
                        "Você pode revogar consentimento de biometria a qualquer momento pela plataforma, selecionando a opção de revogar biometria. " +
                                "Após revogação, os dados biométricos serão deletados. Você poderá usar método alternativo de autenticação ou cadastrar nova biometria futuramente."
                ),
                new BiometricTermSectionResponse(
                        "Retenção de Dados Biométricos",
                        "Os dados biométricos são retidos enquanto você consentir e enquanto forem necessários para autenticação e marcação de ponto. " +
                                "Após revogação de consentimento, os dados são descartados em até 30 dias. " +
                                "Se você deixar de usar a plataforma, dados biométricos podem ser retidos conforme política de retenção geral, " +
                                "respeitando prazos legais e de defesa de direitos."
                ),
                new BiometricTermSectionResponse(
                        "Segurança de Dados Biométricos",
                        "Os dados biométricos são armazenados de forma criptografada e com acesso restrito. " +
                                "Apenas sistemas autenticados podem comparar sua biometria futura com o registro de cadastro. " +
                                "Imagens originais (fotos) podem não ser retidas; apenas dados biométricos derivados. " +
                                "Você pode solicitar acesso aos dados biométricos que mantemos sobre você."
                ),
                new BiometricTermSectionResponse(
                        "Canal para Dúvidas e Solicitações",
                        "Se tiver dúvidas sobre biometria ou deseja exercer direitos (acesso, exclusão, portabilidade), " +
                                "entre em contato pela plataforma ou com o encarregado de proteção de dados. " +
                                "Sua solicitação será processada no prazo legal (30 dias)."
                )
        );
    }
}
