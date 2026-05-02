import { ChevronRight } from 'lucide-react';

interface LoanProductCardProps {
  title: string;
  description: string;
  interestRate: string;
  maxAmount: string;
  icon: React.ReactNode;
  onClick: () => void;
}

export function LoanProductCard({
  title,
  description,
  interestRate,
  maxAmount,
  icon,
  onClick,
}: LoanProductCardProps) {
  return (
    <button
      onClick={onClick}
      className="w-full bg-card border border-border rounded-xl p-6 text-left hover:shadow-lg transition-all duration-200 hover:border-primary/30"
    >
      <div className="flex items-start justify-between gap-4">
        <div className="flex-1">
          <div className="flex items-center gap-3 mb-3">
            <div className="w-12 h-12 rounded-lg bg-accent flex items-center justify-center text-primary">
              {icon}
            </div>
            <h3 className="text-lg">{title}</h3>
          </div>
          <p className="text-sm text-muted-foreground mb-4">{description}</p>
          <div className="flex gap-4">
            <div>
              <p className="text-xs text-muted-foreground">금리</p>
              <p className="text-primary">{interestRate}</p>
            </div>
            <div>
              <p className="text-xs text-muted-foreground">최대 한도</p>
              <p className="text-primary">{maxAmount}</p>
            </div>
          </div>
        </div>
        <ChevronRight className="w-5 h-5 text-muted-foreground flex-shrink-0" />
      </div>
    </button>
  );
}