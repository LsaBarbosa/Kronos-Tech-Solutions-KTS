# Subagent: Frontend Mobile — CustomDaysMobile

## Arquivo a criar

`src/features/collaborators/create/components/CustomDaysMobile.tsx`

## Dependências do projeto

- `cn` de `@/lib/utils`
- `COLLABORATOR_DAY_OPTIONS` de `../constants`
- Tokens Tailwind do design system

## Lógica de seleção automática

**Ordem de seleção** (ao aumentar N): MONDAY → TUESDAY → WEDNESDAY → THURSDAY → FRIDAY → SATURDAY → SUNDAY

**Ordem de remoção** (ao diminuir N): SUNDAY → SATURDAY → FRIDAY → THURSDAY → WEDNESDAY → TUESDAY → MONDAY

```typescript
const DAY_ORDER = ['MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY'] as const;

function selectNext(current: string[]): string[] {
  const next = DAY_ORDER.find((d) => !current.includes(d));
  return next ? [...current, next] : current;
}

function removeLast(current: string[]): string[] {
  if (current.length <= 1) return current;
  const lastSelected = [...DAY_ORDER].reverse().find((d) => current.includes(d));
  return lastSelected ? current.filter((d) => d !== lastSelected) : current;
}
```

## Implementação completa

```tsx
import { cn } from "@/lib/utils";
import { COLLABORATOR_DAY_OPTIONS } from "../constants";

const DAY_ORDER = ['MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY'] as const;

interface CustomDaysMobileProps {
  value: string[];
  onChange: (days: string[]) => void;
}

export function CustomDaysMobile({ value, onChange }: CustomDaysMobileProps) {
  const count = value.length;

  const handleIncrease = () => {
    if (count >= 7) return;
    const next = DAY_ORDER.find((d) => !value.includes(d));
    if (next) onChange([...value, next]);
  };

  const handleDecrease = () => {
    if (count <= 1) return;
    const last = [...DAY_ORDER].reverse().find((d) => value.includes(d));
    if (last) onChange(value.filter((d) => d !== last));
  };

  const toggleDay = (day: string) => {
    const isSelected = value.includes(day);
    if (isSelected && value.length === 1) return;
    const next = isSelected ? value.filter((d) => d !== day) : [...value, day];
    onChange(next);
  };

  const offDays = COLLABORATOR_DAY_OPTIONS
    .filter((d) => !value.includes(d.value))
    .map((d) => d.label);

  return (
    <div className="space-y-4">
      {/* Passo 1: Seletor numérico */}
      <div>
        <p className="mb-2 text-sm font-medium text-slate-700">Dias por semana</p>
        <div className="flex items-center gap-4">
          <button
            type="button"
            onClick={handleDecrease}
            disabled={count <= 1}
            className={cn(
              "flex h-10 w-10 items-center justify-center rounded-full border text-lg font-semibold transition-all",
              count <= 1
                ? "cursor-not-allowed border-slate-200 bg-slate-50 text-slate-300"
                : "border-blue-300 bg-blue-50 text-blue-700 hover:bg-blue-100"
            )}
          >
            −
          </button>
          <span className="w-6 text-center text-2xl font-semibold text-slate-900">{count}</span>
          <button
            type="button"
            onClick={handleIncrease}
            disabled={count >= 7}
            className={cn(
              "flex h-10 w-10 items-center justify-center rounded-full border text-lg font-semibold transition-all",
              count >= 7
                ? "cursor-not-allowed border-slate-200 bg-slate-50 text-slate-300"
                : "border-blue-300 bg-blue-50 text-blue-700 hover:bg-blue-100"
            )}
          >
            +
          </button>
        </div>
      </div>

      {/* Passo 2: Strip de dias tocáveis */}
      <div>
        <p className="mb-2 text-sm font-medium text-slate-700">Quais dias?</p>
        <div className="flex flex-wrap gap-2">
          {COLLABORATOR_DAY_OPTIONS.map((day) => {
            const isSelected = value.includes(day.value);
            const isOnlyOne = value.length === 1 && isSelected;

            return (
              <button
                key={day.value}
                type="button"
                onClick={() => toggleDay(day.value)}
                disabled={isOnlyOne}
                className={cn(
                  "rounded-full border px-3 py-2 text-sm font-semibold transition-all",
                  isSelected
                    ? "border-blue-500 bg-blue-50 text-blue-700"
                    : "border-slate-200 bg-white text-slate-500",
                  isOnlyOne && "opacity-50"
                )}
              >
                {day.label}
              </button>
            );
          })}
        </div>
      </div>

      {/* Footer dinâmico */}
      <p className="text-xs text-slate-500">
        {offDays.length === 0
          ? "Trabalha todos os dias da semana"
          : `Folgas automáticas: ${offDays.join(", ")}`}
      </p>
    </div>
  );
}
```

## Critério de aceite

- Botão `+` seleciona o próximo dia na ordem Seg→Dom
- Botão `−` remove o último dia na ordem Dom→Seg
- Toque nas pills alterna individualmente e atualiza o contador
- Não permite desmarcar o último dia
- Footer exibe folgas automáticas dinamicamente
- Contador sincronizado com o número real de dias selecionados
