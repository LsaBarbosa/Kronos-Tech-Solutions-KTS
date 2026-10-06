#!/usr/bin/env python3
import io, os
import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import matplotlib.patches as mpatches
from reportlab.lib.enums import TA_CENTER, TA_LEFT, TA_RIGHT, TA_JUSTIFY
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.units import cm
from reportlab.platypus import (Paragraph, Spacer, Table, TableStyle,
    HRFlowable, Image, PageBreak, KeepTogether, SimpleDocTemplate)
from reportlab.lib.colors import HexColor, white

CRITICA  = HexColor("#B91C1C"); ALTA    = HexColor("#EA580C")
MEDIA    = HexColor("#D97706"); BAIXA   = HexColor("#2563EB")
INFO     = HexColor("#6B7280"); FORTE   = HexColor("#059669")
HEADER   = HexColor("#1E3A5F"); SUBHDR  = HexColor("#2563EB")
LIGHT_BG = HexColor("#F8FAFC"); BORDER  = HexColor("#E2E8F0")
TEXT     = HexColor("#374151"); DARK    = HexColor("#111827")
SEV_COLOR = {"CRITICA":CRITICA,"ALTA":ALTA,"MEDIA":MEDIA,"BAIXA":BAIXA,"INFORMATIVA":INFO}

DATA = "29 de agosto de 2026"
OUTPUT = "/home/deploy/apps/Kronos-Tech-Solutions-KTS/docs/security-audit/relatorio-auditoria-seguranca.pdf"

ACHADOS = [
    {"id":"ACHADO-001","sev":"MEDIA","cat":"Cat 4 - Chaves expostas","cat_key":"Cat 4",
     "arquivo":"constants/LegalCompanyData.java","linhas":"7-8",
     "titulo":"CPF e CNPJ reais hardcoded em constante de producao",
     "descricao":"DEV_RAZAO_SOCIAL e DEV_CNPJ contem CPF e CNPJ reais compilados no JAR. O comentario da classe instrui mover para application.yml, mas nao foi feito. O dado persiste em historico git, imagens Docker e no binario distribuido.",
     "trecho":'public static final String DEV_CNPJ = "66.899.562/0001-27";',
     "impacto":"Exposicao de CPF/CNPJ. Risco de violacao de LGPD Art. 46.",
     "explorabilidade":"Qualquer pessoa com acesso ao repositorio ou JAR pode extrair via strings ou bytecode."},
    {"id":"ACHADO-002","sev":"BAIXA","cat":"Cat 4 - Chaves expostas","cat_key":"Cat 4",
     "arquivo":"src/main/resources/application.yml","linhas":"165, 218, 232",
     "titulo":"Defaults fracos para HMAC Redis, hash LGPD e sandbox-key",
     "descricao":"Tres propriedades tem fallbacks fracos via ${VAR:-default}: REDIS_KEY_HMAC_SECRET, LGPD_LOG_HASH_SECRET e KRONOS_DEMO_SANDBOX_KEY. Em staging ou CI sem vars definidas, os valores fracos sao usados silenciosamente.",
     "trecho":"key-hmac-secret: ${REDIS_KEY_HMAC_SECRET:local-dev-redis-key-secret}",
     "impacto":"HMAC de chaves Redis forjavel; hash LGPD previsivel em staging.",
     "explorabilidade":"Requer sistema rodando sem as env vars definidas (staging, CI mal configurado)."},
    {"id":"ACHADO-003","sev":"BAIXA","cat":"Cat 4 - Chaves expostas","cat_key":"Cat 4",
     "arquivo":"src/main/resources/application-local.yml","linhas":"73, 77, 93",
     "titulo":"Perfil local expoe Actuator endpoints e JWT secret fraco",
     "descricao":"O perfil 'local' inclui endpoints Actuator env e configprops, exibe health details=always e usa JWT secret com fallback fraco. Se ativado em ambiente nao-isolado, expoe todas as variaveis de ambiente.",
     "trecho":'include: "env,configprops"  show-details: always  secret: ${JWT_SECRET:local-dev-secret...}',
     "impacto":"Exposicao de configuracao sensivel via /actuator/env se perfil local ativado em producao.",
     "explorabilidade":"Requer ativacao acidental do perfil 'local' em producao ou staging acessivel."},
    {"id":"ACHADO-004","sev":"MEDIA","cat":"Cat 2 - Permissao insuficiente","cat_key":"Cat 2",
     "arquivo":"adapter/in/web/http/ServiceContractController.java","linhas":"87, 93-94, 105-106, 117-118",
     "titulo":"Role TERMINAL pode acessar endpoints de contratos de servico",
     "descricao":"Quatro endpoints (findMyPending, preview, sign, downloadSignedDocument) usam @PreAuthorize('isAuthenticated()') em vez de hasAnyRole('MANAGER','PARTNER','CTO'). Role TERMINAL e autenticada mas representa quiosque - nao deve acessar contratos. findMyPending() e preview() nao tem verificacao biometrica compensatoria.",
     "trecho":'@PreAuthorize("isAuthenticated()")\n@GetMapping(SERVICE_CONTRACT_ME_PENDING)\npublic ResponseEntity findMyPending() {...}',
     "impacto":"Token TERMINAL pode listar contratos pendentes e baixar PDFs de contrato.",
     "explorabilidade":"Requer token JWT com role TERMINAL (acesso ao quiosque ou credencial comprometida)."},
    {"id":"ACHADO-005","sev":"MEDIA","cat":"Cat 2 - Permissao insuficiente","cat_key":"Cat 2",
     "arquivo":"adapter/in/web/http/TimesheetSignatureController.java","linhas":"43-44, 52-53, 71-72, 82-83",
     "titulo":"Role TERMINAL pode acessar endpoints de assinatura de espelho de ponto",
     "descricao":"Quatro endpoints (status, preview, sign, downloadDocument) usam isAuthenticated(). status() e preview() nao tem verificacao biometrica compensatoria, permitindo que token TERMINAL consulte status e baixe PDFs mensais.",
     "trecho":'@PreAuthorize("isAuthenticated()")\n@GetMapping(TIMESHEET_SIGNATURE_STATUS)\npublic ResponseEntity status(...) {...}',
     "impacto":"Token TERMINAL pode consultar status de assinatura e baixar espelhos de ponto mensais.",
     "explorabilidade":"Mesma condicao que ACHADO-004."},
    {"id":"ACHADO-006","sev":"INFORMATIVA","cat":"Cat 1 - Isolamento de tenant","cat_key":"Cat 1",
     "arquivo":"application/security/DomainAuthorizationService.java","linhas":"103-108",
     "titulo":"Fallback silencioso de activeCompanyId para empresa primaria em tokens legados",
     "descricao":"Quando activeCompanyId e null no token, o fallback usa employee.companyId() - a empresa primaria. Em cenario multi-company, tokens legados sem activeCompanyId sempre resolvem para empresa primaria sem indicar o problema.",
     "trecho":"if (activeCompanyId == null) {\n    activeCompanyId = getAuthenticatedEmployee().companyId();\n}",
     "impacto":"Autorizacao silenciosamente incorreta para usuarios multi-empresa com tokens legados.",
     "explorabilidade":"Requer token emitido via overload sem activeCompanyId. Nao permite cross-tenant."},
    {"id":"ACHADO-007","sev":"INFORMATIVA","cat":"Cat 5 - Inputs sem tratamento","cat_key":"Cat 5",
     "arquivo":"application/service/MessageService.java","linhas":"140-146",
     "titulo":"Conteudo de mensagens armazenado sem sanitizacao HTML no backend",
     "descricao":"title e messageText sao armazenados sem sanitizacao HTML/XSS. A seguranca depende inteiramente do frontend escapar corretamente. Risco de XSS stored se frontend renderizar via innerHTML ou equivalente.",
     "trecho":"var message = new Message(\n    sender.employeeId(), senderCompanyId,\n    request.title(),       // sem sanitizacao\n    request.messageText(), // sem sanitizacao\n    ...\n);",
     "impacto":"XSS stored se frontend renderizar sem escape. Severidade real depende do frontend.",
     "explorabilidade":"Requer: (a) MANAGER/CTO criar mensagem com payload XSS, E (b) frontend renderizar sem escape."},
    {"id":"ACHADO-008","sev":"INFORMATIVA","cat":"Cat 4 - Chaves expostas","cat_key":"Cat 4",
     "arquivo":"constants/LegalCompanyData.java + service/ReceiptPdfService.java","linhas":"11 / 27",
     "titulo":"Numero INPI placeholder '999999999' em documentos com validade juridica",
     "descricao":"Numero de registro INPI ficticio (999999999) hardcoded em dois lugares. PDFs de recibo emitidos em producao incluem esse numero, o que pode configurar uso indevido sob Portaria 671/2021.",
     "trecho":'public static final String INPI_NUMBER = "999999999";',
     "impacto":"Risco de conformidade legal: documentos fiscais com numero INPI invalido.",
     "explorabilidade":"Nao e exploravel por atacante externo; e problema de conformidade interna."},
]

FORTES = [
    ("Autorizacao centralizada via DomainAuthorizationService","Unico ponto de verificacao de tenant/ownership. Nenhum service bypassou nos caminhos criticos."),
    ("Isolamento de tenant em todos os recursos criticos","TimeRecord, Document, LGPD, Schedule e PointMirror verificam tenant antes de qualquer leitura/escrita."),
    ("IDOR inexistente nos recursos de dados","Services realizam verificacao explicita de assignment e tenant, eliminando IDOR cruzado."),
    ("Producao sem secrets hardcoded","application-prod.yml exige todas as env vars sem fallback. ProductionConfigValidator invalida startup com JWT < 64 chars."),
    ("Session invalidation e token blacklist","JwtAuthenticationFilter verifica session_version. Troca de senha invalida todos os tokens. Blacklist para logout."),
    ("Sanitizacao de dados sensiveis em observabilidade","FrontendObservabilitySanitizer, SensitiveDataSanitizer e SensitiveDataMasker removem CPF, email, JWT de todos os logs."),
    ("PDFs gerados via API binaria (sem HTML injection)","ReceiptPdfService, PointMirrorPdfService usam API Java do iTextPDF, eliminando risco de XSS em PDFs."),
    ("Verificacao biometrica em operacoes criticas","sign() em contratos e espelhos exigem reconhecimento facial via AWS Rekognition com verificacao de identidade."),
    ("Rate limiting em endpoints sensiveis","Login, recovery e checkin biometrico tem rate limits com cooldown progressivo via Redis."),
    ("CSRF configurado","CookieCsrfTokenRepository com excecoes apenas para endpoints de autenticacao stateless."),
    ("Actuator restrito em producao","Limitado a health, info, prometheus; vinculado a 127.0.0.1:8081, inacessivel externamente."),
]

RECOMENDACOES = [
    ("P1","ACHADO-001","MEDIA","Remover CPF/CNPJ de LegalCompanyData.java",
     "Mover para variaveis de ambiente ou cofre de segredos. Executar git filter-repo para remover do historico. Avaliar notificacao a ANPD (LGPD Art. 48)."),
    ("P1","ACHADO-004/005","MEDIA","Substituir isAuthenticated() por ANY_EMPLOYEE nos 8 endpoints",
     "Alterar para hasAnyRole('MANAGER','PARTNER','CTO') em ServiceContractController (4 endpoints) e TimesheetSignatureController (4 endpoints). Adicionar testes com token TERMINAL para garantir HTTP 403."),
    ("P2","ACHADO-008","INFORMATIVA","Substituir numero INPI placeholder por valor real",
     "Definir numero INPI real ou criar issue de rastreamento com prazo. PDFs fiscais com numero ficticio representam risco de conformidade com Portaria 671/2021."),
    ("P2","ACHADO-007","INFORMATIVA","Sanitizar conteudo de mensagens no backend",
     "Usar Jsoup.clean() para strip de HTML em request.title() e request.messageText() antes de persistir. Independente da seguranca do frontend."),
    ("P3","ACHADO-002/003","BAIXA","Garantir env vars definidas em todos os ambientes nao-producao",
     "Revisar pipelines CI/CD e staging para garantir que REDIS_KEY_HMAC_SECRET e LGPD_LOG_HASH_SECRET estejam sempre definidas."),
]

GITHUB_ISSUES = [
    {"n":1,"titulo":"[Seguranca] CPF e CNPJ reais hardcoded em LegalCompanyData.java","labels":"security, media",
     "descricao":"## Problema\nCPF e CNPJ reais estao hardcoded nas constantes DEV_RAZAO_SOCIAL e DEV_CNPJ em LegalCompanyData.java, compilados no JAR e no historico git.\n\n## Por que e exploravel\nQualquer pessoa com acesso ao repositorio ou ao JAR pode extrair CPF/CNPJ via `strings` ou inspecao de bytecode, sem autenticacao no sistema.\n\n## Evidencia\n```\nconstants/LegalCompanyData.java linhas 7-8:\npublic static final String DEV_CNPJ = \"66.899.562/0001-27\";\n```\n(O comentario na linha 6 ja instrui mover para application.yml -- nao foi feito.)\n\n## Impacto\nViolacao potencial do Art. 46 da LGPD. Risco de notificacao obrigatoria a ANPD se repositorio acessivel externamente (Art. 48).\n\n## Sugestao de correcao\n1. Mover para variaveis de ambiente ou cofre de segredos\n2. Injetar via @Value com validacao no ProductionConfigValidator\n3. Executar git filter-repo para remover o dado do historico\n4. Avaliar comunicacao a ANPD\n\n## Criterios de aceite\n- [ ] Constantes removidas de LegalCompanyData.java\n- [ ] Valores injetados via env var com validacao de startup\n- [ ] Historico git reescrito sem os dados\n- [ ] Testes de startup falham se env vars ausentes em producao"},
    {"n":2,"titulo":"[Seguranca] Role TERMINAL pode acessar contratos e espelhos de ponto","labels":"security, media",
     "descricao":"## Problema\n8 endpoints em ServiceContractController e TimesheetSignatureController usam @PreAuthorize(\"isAuthenticated()\") em vez de hasAnyRole('MANAGER','PARTNER','CTO'). A role TERMINAL representa quiosque biometrico e nao deve ter acesso a esses recursos.\n\n## Evidencia\n```\nServiceContractController.java linha 87:\n@PreAuthorize(\"isAuthenticated()\")\npublic ResponseEntity findMyPending() {...}\n\nTimesheetSignatureController.java linha 43:\n@PreAuthorize(\"isAuthenticated()\")\npublic ResponseEntity status(...) {...}\n```\n\n## Impacto\nToken TERMINAL pode: listar contratos pendentes, baixar PDFs de contrato, consultar status de assinatura e baixar espelhos mensais.\n\n## Sugestao de correcao\nSubstituir isAuthenticated() por hasAnyRole('MANAGER', 'PARTNER', 'CTO') nos 8 endpoints:\n- ServiceContractController: findMyPending, preview, sign, downloadSignedDocument\n- TimesheetSignatureController: status, preview, sign, downloadDocument\n\n## Criterios de aceite\n- [ ] Todos os 8 endpoints retornam HTTP 403 para tokens com role TERMINAL\n- [ ] Testes de integracao cobrindo cenarios com token TERMINAL\n- [ ] Fluxo de assinatura biometrica mantido via caminho correto"},
    {"n":3,"titulo":"[Seguranca] Numero INPI placeholder + defaults fracos em ambientes nao-prod","labels":"security, baixa, informativa",
     "descricao":"Este issue agrupa dois achados de baixa/informativa.\n\n## 3a - Numero INPI ficticio em documentos fiscais\n\n```\nLegalCompanyData.java linha 11:\npublic static final String INPI_NUMBER = \"999999999\";\n\nReceiptPdfService.java linha 27:\nprivate static final String INPI_REGISTRATION_NUMBER = \"999999999\";\n```\n\nPDFs de recibo emitidos em producao incluem numero ficticio. Documentos sob Portaria 671/2021 com INPI invalido representam risco de conformidade.\n\nAcao: Definir numero INPI real ou criar issue de rastreamento com prazo.\n\n## 3b - Defaults fracos em ambientes nao-producao\n\n```\napplication.yml linhas 165, 218, 232:\nkey-hmac-secret: ${REDIS_KEY_HMAC_SECRET:local-dev-redis-key-secret}\nhash-secret: ${LGPD_LOG_HASH_SECRET:local-dev-lgpd-log-secret}\n```\n\nEm staging ou CI sem as variaveis definidas, valores fracos sao usados silenciosamente.\n\nAcao: Garantir que as env vars estejam definidas em todos os pipelines.\n\n## Criterios de aceite\n- [ ] Numero INPI real definido ou issue de rastreamento criada\n- [ ] Pipeline CI/CD valida presenca de REDIS_KEY_HMAC_SECRET e LGPD_LOG_HASH_SECRET\n- [ ] Documentacao de perfis por ambiente atualizada"},
    {"n":4,"titulo":"[Seguranca] Conteudo de mensagens sem sanitizacao HTML (defesa em profundidade)","labels":"security, informativa",
     "descricao":"## Problema\ntitle e messageText em MessageService.java sao armazenados sem sanitizacao HTML/XSS. A seguranca depende do frontend escapar corretamente.\n\n## Evidencia\n```\nMessageService.java linhas 140-146:\nvar message = new Message(\n    sender.employeeId(), senderCompanyId,\n    request.title(),        // sem sanitizacao\n    request.messageText(),  // sem sanitizacao\n    ...\n);\n```\n\n## Impacto\nXSS stored para todos os destinatarios se frontend renderizar sem escape. Severidade depende da implementacao do frontend (nao auditado).\n\n## Sugestao de correcao\nAdicionar sanitizacao com Jsoup como defesa em profundidade:\n```java\n// org.jsoup:jsoup\nString safeTitle = Jsoup.clean(request.title(), Safelist.none());\nString safeText  = Jsoup.clean(request.messageText(), Safelist.basic());\n```\n\n## Criterios de aceite\n- [ ] Sanitizacao implementada em MessageService\n- [ ] Testes unitarios validando strip de tags HTML\n- [ ] Frontend auditado separadamente"},
]

def S(name, **kw):
    d = dict(fontName="Helvetica", fontSize=9.5, textColor=TEXT, leading=14)
    d.update(kw); return ParagraphStyle(name, **d)

ST = {
    "ct": S("ct", fontName="Helvetica-Bold", fontSize=22, textColor=white, leading=28, alignment=TA_CENTER),
    "cs": S("cs", fontSize=13, textColor=HexColor("#CBD5E1"), leading=18, alignment=TA_CENTER),
    "cd": S("cd", fontSize=10, textColor=HexColor("#94A3B8"), leading=14, alignment=TA_CENTER),
    "h1": S("h1", fontName="Helvetica-Bold", fontSize=16, textColor=HEADER, leading=22, spaceAfter=6),
    "h2": S("h2", fontName="Helvetica-Bold", fontSize=13, textColor=SUBHDR, leading=18, spaceAfter=4),
    "bo": S("bo", leading=14, spaceAfter=4, alignment=TA_JUSTIFY),
    "bs": S("bs", fontSize=8.5, leading=12, spaceAfter=2, alignment=TA_JUSTIFY),
    "co": S("co", fontName="Courier", fontSize=7.5, textColor=HexColor("#1E3A5F"), backColor=HexColor("#F0F4F8"), leading=11, leftIndent=6, rightIndent=6),
    "th": S("th", fontName="Helvetica-Bold", fontSize=9, textColor=white, leading=12, alignment=TA_CENTER),
    "tc": S("tc", fontSize=8.5, leading=12),
    "ib": S("ib", fontName="Courier", fontSize=7.5, leading=11, backColor=HexColor("#F8FAFC"), leftIndent=6, rightIndent=6),
}

def make_donut(counts):
    labels = ["CRITICA","ALTA","MEDIA","BAIXA","INFORMATIVA"]
    palette = ["#B91C1C","#EA580C","#D97706","#2563EB","#6B7280"]
    vals = [counts.get(l,0) for l in labels]
    nz = [(l,v,c) for l,v,c in zip(labels,vals,palette) if v>0]
    if not nz: return None
    lb,vl,cl = zip(*nz)
    fig,ax = plt.subplots(figsize=(4.5,3.4),facecolor="none")
    wedges,_,autotexts = ax.pie(vl,labels=None,colors=cl,autopct="%d",startangle=90,
        wedgeprops=dict(width=0.55,edgecolor="white",linewidth=2),pctdistance=0.75)
    for at in autotexts:
        at.set_fontsize(9); at.set_fontweight("bold"); at.set_color("white")
    ax.set_facecolor("none")
    patches = [mpatches.Patch(color=c,label=f"{l} ({v})") for l,v,c in zip(lb,vl,cl)]
    ax.legend(handles=patches,loc="center left",bbox_to_anchor=(0.92,0.5),fontsize=8,frameon=False)
    ax.set_title("Achados por Severidade",fontsize=10,fontweight="bold",color="#1E3A5F",pad=8)
    fig.tight_layout()
    buf=io.BytesIO(); fig.savefig(buf,format="png",dpi=150,bbox_inches="tight",transparent=True)
    plt.close(fig); buf.seek(0); return buf.read()

def make_bar(counts):
    cats=["Cat 1\nTenant","Cat 2\nPermissao","Cat 3\nIDOR","Cat 4\nChaves","Cat 5\nXSS"]
    keys=["Cat 1","Cat 2","Cat 3","Cat 4","Cat 5"]
    vals=[counts.get(k,0) for k in keys]
    bcs=["#2563EB","#EA580C","#B91C1C","#D97706","#6B7280"]
    fig,ax=plt.subplots(figsize=(5.8,3.2),facecolor="none")
    bars=ax.bar(cats,vals,color=bcs,edgecolor="white",linewidth=1.5,width=0.6)
    for bar,v in zip(bars,vals):
        ax.text(bar.get_x()+bar.get_width()/2,bar.get_height()+0.05,str(v),
            ha="center",va="bottom",fontsize=10,fontweight="bold",color="#374151")
    ax.set_facecolor("none")
    for sp in ["top","right"]: ax.spines[sp].set_visible(False)
    ax.spines["left"].set_color("#CBD5E1"); ax.spines["bottom"].set_color("#CBD5E1")
    ax.yaxis.set_major_locator(plt.MaxNLocator(integer=True))
    ax.tick_params(colors="#6B7280",labelsize=8)
    ax.set_ylim(0,max(vals)+1.5 if max(vals)>0 else 3)
    ax.set_title("Achados por Categoria",fontsize=10,fontweight="bold",color="#1E3A5F",pad=8)
    fig.tight_layout()
    buf=io.BytesIO(); fig.savefig(buf,format="png",dpi=150,bbox_inches="tight",transparent=True)
    plt.close(fig); buf.seek(0); return buf.read()

def count_sev():
    c={}
    for a in ACHADOS: c[a["sev"]]=c.get(a["sev"],0)+1
    return c

def count_cat():
    c={}
    for a in ACHADOS: c[a["cat_key"]]=c.get(a["cat_key"],0)+1
    return c

def tbl(rows,cws,sty=None):
    t=Table(rows,colWidths=cws)
    if sty: t.setStyle(TableStyle(sty))
    return t

def on_page(canvas,doc):
    canvas.saveState()
    w,h=A4
    if doc.page>1:
        canvas.setFillColor(HEADER); canvas.rect(0,h-1.2*cm,w,1.2*cm,fill=1,stroke=0)
        canvas.setFont("Helvetica-Bold",8); canvas.setFillColor(white)
        canvas.drawString(2*cm,h-0.75*cm,"Relatorio de Auditoria de Seguranca - Kronos")
        canvas.drawRightString(w-2*cm,h-0.75*cm,f"Pag. {doc.page}")
        canvas.setFillColor(BORDER); canvas.rect(0,0,w,0.8*cm,fill=1,stroke=0)
        canvas.setFont("Helvetica",7.5); canvas.setFillColor(HexColor("#6B7280"))
        canvas.drawCentredString(w/2,0.28*cm,f"Confidencial - {DATA} - Uso interno restrito")
    canvas.restoreState()

def build():
    story=[]
    w,h=A4

    # CAPA
    story.append(Spacer(1,2.5*cm))
    cover_box=tbl([[""]],[w-4*cm],[("BACKGROUND",(0,0),(-1,-1),HEADER),("ROWHEIGHTS",(0,0),(-1,-1),6*cm)])
    story.append(cover_box)
    story.append(Spacer(1,-5.6*cm))
    story.append(Spacer(1,0.7*cm))
    story.append(Paragraph("Relatorio de Auditoria de Seguranca",ST["ct"]))
    story.append(Spacer(1,0.3*cm))
    story.append(Paragraph("Kronos - Sistema de Controle de Ponto",ST["cs"]))
    story.append(Spacer(1,0.5*cm))
    story.append(Paragraph(f"Data: {DATA}",ST["cd"]))
    story.append(Paragraph("Escopo: Codigo-fonte backend Java (1017 arquivos)",ST["cd"]))
    story.append(Paragraph("Classificacao: Confidencial - Uso interno restrito",ST["cd"]))
    story.append(Spacer(1,3.8*cm))
    nota=tbl([
        [Paragraph("<b>Stack</b>",ST["tc"]), Paragraph("Java 21 / Spring Boot 3.5 / PostgreSQL / Redis / AWS S3+Rekognition / iTextPDF",ST["tc"])],
        [Paragraph("<b>Arquitetura</b>",ST["tc"]), Paragraph("Hexagonal: controllers em adapter/in/web/http/, services em application/service/",ST["tc"])],
        [Paragraph("<b>Tenant isolation</b>",ST["tc"]), Paragraph("activeCompanyId no JWT, validado em DomainAuthorizationService por cada operacao",ST["tc"])],
        [Paragraph("<b>Frontend</b>",ST["tc"]), Paragraph("Nao auditado (repositorio separado). Achados XSS backend anotados.",ST["tc"])],
        [Paragraph("<b>Nao auditados</b>",ST["tc"]), Paragraph(".env e infra/observability/.env (permissao negada)",ST["tc"])],
    ],[3.8*cm,12.7*cm],[
        ("ROWBACKGROUNDS",(0,0),(-1,-1),[LIGHT_BG,white]),
        ("BOX",(0,0),(-1,-1),0.5,BORDER),("INNERGRID",(0,0),(-1,-1),0.3,BORDER),
        ("VALIGN",(0,0),(-1,-1),"TOP"),("TOPPADDING",(0,0),(-1,-1),6),("BOTTOMPADDING",(0,0),(-1,-1),6),
        ("LEFTPADDING",(0,0),(-1,-1),8),
    ])
    story.append(nota)
    story.append(PageBreak())

    # RESUMO EXECUTIVO
    story.append(Paragraph("1. Resumo Executivo",ST["h1"]))
    story.append(HRFlowable(width="100%",thickness=1,color=BORDER,spaceAfter=10))
    sc=count_sev(); cc=count_cat(); total=len(ACHADOS)
    story.append(Paragraph(
        f"A auditoria analisou <b>{total} achados</b> no backend Kronos (1017 arquivos Java). "
        "Nenhum achado CRITICO ou ALTO. Os dois achados MEDIA referem-se a role TERMINAL sem restricao de role "
        "e dado pessoal (CPF/CNPJ) hardcoded. DomainAuthorizationService elimina a classe inteira de IDOR e cross-tenant.",ST["bo"]))
    story.append(Spacer(1,0.3*cm))
    crows=[[Paragraph("<b>Severidade</b>",ST["th"]),Paragraph("<b>Qtd</b>",ST["th"])]]
    for sv in ["CRITICA","ALTA","MEDIA","BAIXA","INFORMATIVA"]:
        crows.append([Paragraph(sv,ST["tc"]),Paragraph(str(sc.get(sv,0)),ST["tc"])])
    crows.append([Paragraph("<b>TOTAL</b>",ST["tc"]),Paragraph(f"<b>{total}</b>",ST["tc"])])
    ctbl=tbl(crows,[5.5*cm,2.5*cm],[
        ("BACKGROUND",(0,0),(-1,0),HEADER),("ROWBACKGROUNDS",(0,1),(-1,-2),[LIGHT_BG,white]),
        ("BACKGROUND",(0,-1),(-1,-1),HexColor("#E2E8F0")),("ALIGN",(1,0),(1,-1),"CENTER"),
        ("BOX",(0,0),(-1,-1),0.5,BORDER),("INNERGRID",(0,0),(-1,-1),0.3,BORDER),
        ("TOPPADDING",(0,0),(-1,-1),5),("BOTTOMPADDING",(0,0),(-1,-1),5),("LEFTPADDING",(0,0),(-1,-1),8),
    ])
    db=make_donut(sc); bb=make_bar(cc)
    charts=[]
    if db: charts.append(Image(io.BytesIO(db),width=7*cm,height=5.5*cm))
    if bb: charts.append(Image(io.BytesIO(bb),width=9*cm,height=5.5*cm))
    if charts:
        chs=tbl([charts],[8*cm,8.5*cm],[("VALIGN",(0,0),(-1,-1),"MIDDLE"),("ALIGN",(0,0),(-1,-1),"CENTER")])
        outer=tbl([[ctbl,chs]],[9*cm,8*cm],[("VALIGN",(0,0),(-1,-1),"TOP"),("LEFTPADDING",(1,0),(1,0),10)])
        story.append(outer)
    else:
        story.append(ctbl)
    story.append(PageBreak())

    # PONTOS FORTES E FRACOS
    story.append(Paragraph("2. Pontos Fortes e Fracos",ST["h1"]))
    story.append(HRFlowable(width="100%",thickness=1,color=BORDER,spaceAfter=10))
    story.append(Paragraph("2.1 Pontos Fortes",ST["h2"]))
    for title,desc in FORTES:
        r=tbl([[
            Paragraph("OK",S("ok",fontName="Helvetica-Bold",fontSize=8,textColor=white,alignment=TA_CENTER)),
            Paragraph(f"<b>{title}</b>  <font size=8 color='#4B5563'>{desc}</font>",ST["bs"]),
        ]],[1*cm,16*cm],[
            ("BACKGROUND",(0,0),(0,0),FORTE),("VALIGN",(0,0),(-1,-1),"MIDDLE"),
            ("TOPPADDING",(0,0),(-1,-1),5),("BOTTOMPADDING",(0,0),(-1,-1),5),
            ("LEFTPADDING",(0,0),(0,0),4),("LEFTPADDING",(1,0),(1,0),8),
        ])
        story.append(r); story.append(Spacer(1,0.15*cm))
    story.append(Spacer(1,0.4*cm))
    story.append(Paragraph("2.2 Pontos Fracos",ST["h2"]))
    fracos=[
        ("ACHADO-001","MEDIA","CPF/CNPJ reais compilados no JAR e historico git - risco LGPD."),
        ("ACHADO-004/005","MEDIA","Role TERMINAL acessa contratos e espelhos via isAuthenticated() sem restricao de role."),
        ("ACHADO-007","INFORMATIVA","Backend nao sanitiza HTML em mensagens - depende do frontend nao auditado."),
        ("ACHADO-008","INFORMATIVA","Numero INPI placeholder (999999999) em documentos fiscais com validade juridica."),
    ]
    for aid,sev,desc in fracos:
        c=SEV_COLOR.get(sev,INFO)
        r=tbl([[
            Paragraph(sev[:3],S("w",fontName="Helvetica-Bold",fontSize=8,textColor=white,alignment=TA_CENTER)),
            Paragraph(f"<b>{aid}</b> - {desc}",ST["bs"]),
        ]],[1.3*cm,15.7*cm],[
            ("BACKGROUND",(0,0),(0,0),c),("VALIGN",(0,0),(-1,-1),"MIDDLE"),
            ("TOPPADDING",(0,0),(-1,-1),5),("BOTTOMPADDING",(0,0),(-1,-1),5),
            ("LEFTPADDING",(0,0),(0,0),4),("LEFTPADDING",(1,0),(1,0),8),
        ])
        story.append(r); story.append(Spacer(1,0.15*cm))
    story.append(PageBreak())

    # ACHADOS DETALHADOS
    story.append(Paragraph("3. Achados Detalhados",ST["h1"]))
    story.append(HRFlowable(width="100%",thickness=1,color=BORDER,spaceAfter=10))
    for a in ACHADOS:
        col=SEV_COLOR.get(a["sev"],INFO)
        hdr=tbl([[
            Paragraph(f"<b>{a['id']}</b>",S("aid",fontName="Helvetica-Bold",fontSize=10,textColor=white)),
            Paragraph(a["sev"],S("as",fontName="Helvetica-Bold",fontSize=9,textColor=white,alignment=TA_CENTER)),
            Paragraph(a["cat"],S("ac",fontSize=9,textColor=HexColor("#CBD5E1"),alignment=TA_RIGHT)),
        ]],[4*cm,3*cm,10*cm],[
            ("BACKGROUND",(0,0),(-1,-1),col),("TOPPADDING",(0,0),(-1,-1),6),("BOTTOMPADDING",(0,0),(-1,-1),6),
            ("LEFTPADDING",(0,0),(0,0),10),("RIGHTPADDING",(-1,0),(-1,0),10),("VALIGN",(0,0),(-1,-1),"MIDDLE"),
        ])
        body=tbl([
            [Paragraph("<b>Titulo</b>",ST["tc"]),         Paragraph(a["titulo"],ST["tc"])],
            [Paragraph("<b>Arquivo</b>",ST["tc"]),        Paragraph(f"<font name='Courier' size=8>{a['arquivo']}</font>  linha(s) {a['linhas']}",ST["tc"])],
            [Paragraph("<b>Problema</b>",ST["tc"]),       Paragraph(a["descricao"],ST["bs"])],
            [Paragraph("<b>Trecho</b>",ST["tc"]),         Paragraph(a["trecho"].replace("\n","<br/>"),ST["co"])],
            [Paragraph("<b>Impacto</b>",ST["tc"]),        Paragraph(a["impacto"],ST["bs"])],
            [Paragraph("<b>Explorabilidade</b>",ST["tc"]),Paragraph(a["explorabilidade"],ST["bs"])],
        ],[3.2*cm,13.8*cm],[
            ("ROWBACKGROUNDS",(0,0),(-1,-1),[LIGHT_BG,white]),("VALIGN",(0,0),(-1,-1),"TOP"),
            ("TOPPADDING",(0,0),(-1,-1),5),("BOTTOMPADDING",(0,0),(-1,-1),5),
            ("LEFTPADDING",(0,0),(-1,-1),8),("BOX",(0,0),(-1,-1),0.5,BORDER),
            ("INNERGRID",(0,0),(-1,-1),0.3,BORDER),
            ("FONTNAME",(0,0),(0,-1),"Helvetica-Bold"),("FONTSIZE",(0,0),(0,-1),8.5),
            ("TEXTCOLOR",(0,0),(0,-1),HexColor("#4B5563")),
        ])
        card=tbl([[hdr],[body]],[17*cm],[
            ("BOX",(0,0),(-1,-1),1,col),("TOPPADDING",(0,0),(-1,-1),0),
            ("BOTTOMPADDING",(0,0),(-1,-1),0),("LEFTPADDING",(0,0),(-1,-1),0),("RIGHTPADDING",(0,0),(-1,-1),0),
        ])
        story.append(KeepTogether(card)); story.append(Spacer(1,0.45*cm))
    story.append(PageBreak())

    # RECOMENDACOES
    story.append(Paragraph("4. Recomendacoes Priorizadas",ST["h1"]))
    story.append(HRFlowable(width="100%",thickness=1,color=BORDER,spaceAfter=10))
    pc={"P1":CRITICA,"P2":MEDIA,"P3":BAIXA}
    for prio,ref,sev,titulo,detalhe in RECOMENDACOES:
        r=tbl([[
            Paragraph(prio,S("p",fontName="Helvetica-Bold",fontSize=11,textColor=white,alignment=TA_CENTER)),
            Paragraph(f"<b>{titulo}</b><br/><font size=7.5 color='#6B7280'>{ref}</font>",ST["bs"]),
            Paragraph(detalhe,ST["bs"]),
        ]],[1.2*cm,5.5*cm,10.3*cm],[
            ("BACKGROUND",(0,0),(0,0),pc.get(prio,BAIXA)),("BACKGROUND",(1,0),(-1,0),LIGHT_BG),
            ("BOX",(0,0),(-1,-1),0.5,BORDER),("INNERGRID",(0,0),(-1,-1),0.3,BORDER),
            ("VALIGN",(0,0),(-1,-1),"TOP"),("TOPPADDING",(0,0),(-1,-1),8),("BOTTOMPADDING",(0,0),(-1,-1),8),
            ("LEFTPADDING",(0,0),(0,0),4),("LEFTPADDING",(1,0),(-1,0),8),
        ])
        story.append(r); story.append(Spacer(1,0.3*cm))
    story.append(PageBreak())

    # ISSUES PARA O GITHUB
    story.append(Paragraph("5. Issues para o GitHub",ST["h1"]))
    story.append(HRFlowable(width="100%",thickness=1,color=BORDER,spaceAfter=6))
    story.append(Paragraph("Textos completos prontos para copiar e colar. Cada bloco delimitado por marcadores.",ST["bo"]))
    story.append(Spacer(1,0.3*cm))
    for iss in GITHUB_ISSUES:
        story.append(Paragraph(f"ISSUE {iss['n']} - {iss['titulo']}",ST["h2"]))
        story.append(Paragraph(f"Labels: <b>{iss['labels']}</b>",ST["bs"]))
        story.append(Spacer(1,0.1*cm))
        ds=tbl([[Paragraph(f"--- ISSUE {iss['n']} ---",S("d",fontName="Courier-Bold",fontSize=8,textColor=HEADER))]],[17*cm],[
            ("BACKGROUND",(0,0),(-1,-1),HexColor("#EFF6FF")),("BOX",(0,0),(-1,-1),0.5,SUBHDR),
            ("TOPPADDING",(0,0),(-1,-1),4),("BOTTOMPADDING",(0,0),(-1,-1),4),("LEFTPADDING",(0,0),(-1,-1),8),
        ])
        story.append(ds)
        safe=(iss["descricao"].strip()
              .replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")
              .replace("\n","<br/>"))
        bt=tbl([[Paragraph(safe,ST["ib"])]],[17*cm],[
            ("BACKGROUND",(0,0),(-1,-1),HexColor("#F8FAFC")),("BOX",(0,0),(-1,-1),0.5,BORDER),
            ("TOPPADDING",(0,0),(-1,-1),8),("BOTTOMPADDING",(0,0),(-1,-1),8),
            ("LEFTPADDING",(0,0),(-1,-1),10),("RIGHTPADDING",(0,0),(-1,-1),10),
        ])
        story.append(bt)
        de=tbl([[Paragraph(f"--- FIM ISSUE {iss['n']} ---",S("df",fontName="Courier-Bold",fontSize=8,textColor=HEADER))]],[17*cm],[
            ("BACKGROUND",(0,0),(-1,-1),HexColor("#EFF6FF")),("BOX",(0,0),(-1,-1),0.5,SUBHDR),
            ("TOPPADDING",(0,0),(-1,-1),4),("BOTTOMPADDING",(0,0),(-1,-1),4),("LEFTPADDING",(0,0),(-1,-1),8),
        ])
        story.append(de); story.append(Spacer(1,0.55*cm))

    doc=SimpleDocTemplate(OUTPUT,pagesize=A4,leftMargin=2*cm,rightMargin=2*cm,
        topMargin=2.2*cm,bottomMargin=1.8*cm,
        title="Relatorio de Auditoria de Seguranca - Kronos",
        author="Claude Code - Auditoria Automatizada")
    doc.build(story,onFirstPage=on_page,onLaterPages=on_page)
    return OUTPUT

if __name__=="__main__":
    out=build(); print(f"PDF gerado: {out}")
