# Subagent: Frontend Desktop — CustomDaysDesktop

## Arquivo a criar

`src/features/collaborators/create/components/CustomDaysDesktop.tsx`

## Dependências do projeto a usar

- `cn` de `@/lib/utils`
- `COLLABORATOR_DAY_OPTIONS` de `../constants` (já tem os 7 dias)
- Tokens Tailwind do design system: `border-blue-500 bg-blue-50 text-blue-700`

## Implementação completa

```tsx
import { cn } from "@/lib/utils";
import { COLLABORATOR_DAY_OPTIONS } from "../constants";

type DayOfWeek = typeof COLLABORATOR_DAY_OPTIONS[number]["value"];

interface CustomDaysDesktopProps {
  value: string[];
  onChange: (days: string[]) => void;
}

export function CustomDaysDesktop({ value, onChange }: CustomDaysDesktopProps) {
  const toggle = (day: string) => {
    const isSelected = value.includes(day);
    if (isSelected && value.length === 1) return; // protege mínimo 1 dia
    const next = isSelected
      ? value.filter((d) => d !== day)
      : [...value, day];
    onChange(next);
  };

  const workedDays = value.length;
  const offDays = 7 - workedDays;

  return (
    <div className="space-y-3">
      <div className="flex flex-wrap gap-2">
        {COLLABORATOR_DAY_OPTIONS.map((day) => {
          const isSelected = value.includes(day.value);
          const isOnlyOne = value.length === 1 && isSelected;

          return (
            <button
              key={day.value}
              type="button"
              onClick={() => toggle(day.value)}
              disabled={isOnlyOne}
              className={cn(
                "rounded-full border px-4 py-2 text-sm font-semibold transition-all",
                isSelected
                  ? "border-blue-500 bg-blue-50 text-blue-700"
                  : "border-slate-200 bg-white text-slate-600 hover:border-blue-200 hover:bg-blue-50/60",
                isOnlyOne && "cursor-not-allowed opacity-50"
              )}
            >
              {day.label}
            </button>
          );
        })}
      </div>
      <p className="text-xs text-slate-500">
        {workedDays} {workedDays === 1 ? "dia trabalhado" : "dias trabalhados"}{" "}
        · {offDays} {offDays === 1 ? "folga" : "folgas"}
      </p>
    </div>
  );
}
```

## Critério de aceite

- 7 pills renderizadas (Seg a Dom)
- Clique alterna work↔off com feedback visual imediato
- Não permite desmarcar o último dia selecionado
- Resumo "X dias trabalhados · Y folgas" atualiza em tempo real
- Props `value` e `onChange` controlam o estado (não é componente não-controlado)
