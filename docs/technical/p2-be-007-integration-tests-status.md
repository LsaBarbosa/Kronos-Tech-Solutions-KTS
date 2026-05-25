# P2-BE-007: LGPD Retention Integration Tests Status

**Data:** 2026-05-25  
**Branch:** feature/lgpd-compliance  
**Status:** 🔄 IN PROGRESS

## Resumo Executivo

Criação de testes integrados para validar o fluxo de execução de retenção LGPD em modo DRY_RUN e APPLY. O arquivo de teste foi criado com cobertura completa de cenários e mapeamentos de entidades corrigidos.

**Progresso:** ✅ Estrutura de teste + Mapeamentos de entidade  
**Bloqueador:** 🔧 Configuração do contexto Spring para testes

---

## Arquivo Criado

### `src/test/java/com/kts/kronos/integration/LgpdRetentionIntegrationTest.java`

**Estrutura:**
```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class LgpdRetentionIntegrationTest {
    // 7 test scenarios
}
```

**Anotações Utilizadas:**
- `@SpringBootTest` — Carrega contexto completo da aplicação
- `@AutoConfigureMockMvc` — Configura MockMvc para testes web
- `@ActiveProfiles("test")` — Usa perfil de teste com H2 database
- `@Transactional` — Rollback automático após cada teste
- `@MockitoBean` — Mock de repositórios não usados em retenção

---

## Cenários de Teste Implementados

### ✅ 1. `testDryRunModeSupported()`
**Objetivo:** Validar que DRY_RUN não deleta documentos

```java
// Cria DocumentEntity expirado (100 dias atrás)
// Executa policy em modo DRY_RUN
// Verifica: Documento ainda existe após execução
```

**Status:** ✅ Lógica correta
**Bloqueador:** Bean initialization

---

### ✅ 2. `testPasswordResetTokensSupported()`
**Objetivo:** Validar processamento de tokens de reset de senha

```java
// Cria PasswordResetTokenEntity expirado (10 dias atrás)
// Executa policy em modo DRY_RUN
// Verifica: Token ainda existe após DRY_RUN
```

**Status:** ✅ Lógica correta

---

### ✅ 3. `testMessagesSupported()`
**Objetivo:** Validar processamento de mensagens internas

```java
// Cria MessageEntity antiga (800 dias)
// Executa policy em modo DRY_RUN
// Verifica: Mensagem ainda existe (não deletada)
```

**Status:** ✅ Lógica correta

---

### ✅ 4. `testApplyModeRequiresFlag()`
**Objetivo:** Validar que APPLY mode respeitasinalizador de habilitação

```java
// Cria DocumentEntity expirado
// Executa policy em modo APPLY (com flag default = false)
// Verifica: Documento NÃO foi deletado (flag blocks execution)
```

**Status:** ✅ Lógica correta

---

### ✅ 5. `testEmptyDataSetHandling()`
**Objetivo:** Validar edge case com dataset vazio

```java
// Deleta todos os documentos/mensagens/tokens
// Executa policies
// Verifica: Execução completa sem erros
```

**Status:** ✅ Lógica correta

---

### ✅ 6. `testMultiplePoliciesSequential()`
**Objetivo:** Validar execução sequencial de múltiplas políticas

```java
// Cria DocumentEntity + PasswordResetTokenEntity
// Executa policy1 (DOCUMENT) e policy2 (PASSWORD_RESET_TOKEN)
// Verifica: Ambas entidades existem após DRY_RUN
```

**Status:** ✅ Lógica correta

---

### ✅ 7. `testRecentDataNotAffected()`
**Objetivo:** Validar que dados recentes (não expirados) são preservados

```java
// Cria DocumentEntity recente (10 dias - dentro do período de retenção)
// Executa policy com período de 90 dias
// Verifica: Documento não foi afetado
```

**Status:** ✅ Lógica correta

---

## Correções de Mapeamento de Entidades

### DocumentEntity
**Antes (incorreto):**
```java
.userId(employeeId)        // WRONG
.companyId(companyId)      // WRONG - campo não existe
.type(DocumentType.PASSPORT)    // WRONG - enum não existe
.contentHash("hash")       // WRONG - campo é checksumSha256
.createdAt(expiredDate)    // WRONG - campo é uploadedAt
```

**Depois (correto):**
```java
.documentId(documentId)
.employeeId(employeeId)    // Correto
.fileName("doc.pdf")
.type(DocumentType.DOCUMENTS)    // Enum válido
.contentType("application/pdf")
.storagePath("s3://bucket/path")
.checksumSha256("hash")    // Campo correto
.uploadedAt(expiredDate)   // Campo correto
```

### MessageEntity
**Antes (incorreto):**
```java
.content("message")        // WRONG - campo é messageText
.createdAt(...)            // Sem @CreationTimestamp - precisa de MockitoBean
```

**Depois (correto):**
```java
.messageId(messageId)
.employeeId(employeeId)
.companyId(companyId)
.title("title")
.messageText("message")    // Campo correto
.priority(MessagePriority.NORMAL)    // Enum requerido
.createdAt(expiredDate)
.deletedBySystem(false)
```

### DocumentType Enum
**Valores Válidos:**
- ✅ `PAYSLIP`
- ✅ `TIME_OFF`
- ✅ `DOCUMENTS` (usado nos testes)
- ✅ `EMPLOYEE_DOCUMENTS`
- ✅ `POINT_RECORD_RECEIPT`
- ✅ `BIOMETRIC_CONSENT_TERM`
- ✅ `SERVICE_CONTRACT_TERMS`

**Valor Inválido:**
- ❌ `PASSPORT` (não existe no enum)

---

## Bloqueador Atual: Spring Context Bean Initialization

### Erro
```
org.springframework.beans.factory.UnsatisfiedDependencyException: 
Error creating bean with name 'authController':
No qualifying bean of type 'com.kts.kronos.adapter.out.persistence.PasswordResetTokenRepository'
```

### Causa
O contexto da aplicação tenta carregar `AuthController` → `AuthService` → `PasswordResetTokenProviderImpl`, que depende de `PasswordResetTokenRepository`. Porém, os repositórios Spring Data JPA não estão sendo auto-configurados.

### Soluções Possíveis

#### Opção A: MockitoBean para todos os repositórios  *(Simples, rápido)*
```java
@MockitoBean
private PasswordResetTokenRepository passwordResetTokenRepository;
// Conflita com @Autowired do campo acima
```

#### Opção B: Test Configuration Bean  *(Recomendado)*
```java
@TestConfiguration
public class LgpdRetentionTestConfig {
    @Bean
    public PasswordResetTokenRepository passwordResetTokenRepository() {
        return Mockito.mock(PasswordResetTokenRepository.class);
    }
}

@SpringBootTest
@Import(LgpdRetentionTestConfig.class)
class LgpdRetentionIntegrationTest {
    // Testes
}
```

#### Opção C: Excluir AuthController  *(Isolamento)*
```java
@SpringBootTest(
    excludeName = "com.kts.kronos.adapter.in.web.http.AuthController"
)
class LgpdRetentionIntegrationTest {
    // Testes sem autenticação
}
```

#### Opção D: @DataJpaTest + @Import seletivo  *(Foco total em dados)*
```java
@DataJpaTest
@Import({
    RetentionPolicyExecutor.class,
    AuditService.class,
    RetentionExecutionLogProvider.class,
    // ... todos os beans de retenção
})
class LgpdRetentionIntegrationTest {
    // Apenas testes de dados
}
```

---

## Próximas Ações

### Imediato
1. **Aplicar Opção B ou D para resolver bean initialization**
   - Opção B: Mais flexível para testes futuros
   - Opção D: Mais focado em dados, mais rápido

2. **Compilar e rodar testes:**
   ```bash
   ./gradlew compileTestJava
   ./gradlew test --tests "LgpdRetentionIntegrationTest"
   ```

3. **Validar resultados:**
   - ✅ 7 testes passando
   - ✅ Sem warnings de deprecation
   - ✅ Coverage de retenção completo

### Documentação Final
- [ ] Atualizar README.md com instruções de execução
- [ ] Adicionar exemplos de uso em comentários
- [ ] Documentar comportamento esperado vs. real

---

## Checklist de Competição

- [x] Estrutura de teste criada
- [x] 7 cenários de teste implementados
- [x] Mapeamentos de entidades corrigidos
- [x] Enums validados (DocumentType, MessagePriority)
- [x] Transactionalidade configurada
- [ ] Bean initialization resolvida
- [ ] Testes passando
- [ ] CI/CD validação concluída

---

## Métricas

| Métrica | Valor |
|---------|-------|
| Linhas de teste | 360 |
| Cenários cobertos | 7 |
| Entidades testadas | 3 (Document, Message, PasswordResetToken) |
| Modos de execução | 2 (DRY_RUN, APPLY) |
| Tempo estimado para fix | 15 min |

---

## Decisões

1. **Usar @SpringBootTest + Mocks seletivos**
   - Razão: Carrega contexto real, testa comportamento real de retenção
   
2. **Usar @Transactional**
   - Razão: Rollback automático, cada teste é isolado
   
3. **Mapeamentos de entidade fixos**
   - Razão: Alinhamento com definições reais de entidades

---

## Conclusão Parcial

O arquivo de teste `LgpdRetentionIntegrationTest.java` está estruturalmente correto com:
- ✅ 7 cenários de teste bem definidos
- ✅ Mapeamentos de entidade precisos
- ✅ Documentação clara
- ⏳ Aguardando resolução de bean initialization

**Status:** Pronto para próximo passo após resolver Spring context.

