# Autorizacao por Aceite de Termos

## Fonte de verdade

O JWT pode continuar carregando `terms_accepted` para compatibilidade e apoio de interface, mas essa claim nao e usada como fonte de verdade para autorizacao.

O `TermsValidationFilter` valida o token apenas para identificar o colaborador e consulta o estado atual em servidor via `DocumentProvider.existsByEmployeeIdAndType(employeeId, BIOMETRIC_CONSENT_TERM)`.

## Cache

Nao ha cache para o estado de aceite nesta etapa. A mudanca de aceite ou revogacao passa a valer assim que o documento de consentimento e persistido ou removido.

## Comportamento de seguranca

Se o token for valido, mas nao houver `employeeId`, nao existir termo vigente ou a consulta server-side falhar, o filtro bloqueia a requisicao com a resposta padronizada `TERMS_NOT_ACCEPTED`.

Com isso, um token antigo com claim `terms_accepted=true` nao mantem acesso indevido apos a revogacao do termo.
