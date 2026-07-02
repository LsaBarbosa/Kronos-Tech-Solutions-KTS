package com.kts.kronos.domain.model.enuns;

public enum WorkScheduleType {
    // 1. Tradicional (Seg-Sex)
    TRADITIONAL_5X2,

    // 2. 6x1 com Folga Fixa na Semana (Padrão)
    SIX_BY_ONE_FIXED,

    // 3. Escala 24x72 (Plantão)
    ROTATING_24X72,

    // 4. Escala 12x36 (Plantão)
    ROTATING_12X36,

    // 5. 6x1 + 2 Folgas de Final de Semana (Sábado ou Domingo) no Mês [NOVO]
    SIX_BY_ONE_TWO_WEEKENDS,

    // 6. 6x1 + 1 Final de Semana por mês (Índice fixo)
    SIX_BY_ONE_ONE_WEEKEND,

    // 7. Dias específicos da semana definidos individualmente por colaborador.
    // Os dias de trabalho são armazenados em Employee.fixedWorkDays (coluna fixed_work_days).
    // Todos os demais dias da semana são automaticamente considerados folga.
    CUSTOM_DAYS
}