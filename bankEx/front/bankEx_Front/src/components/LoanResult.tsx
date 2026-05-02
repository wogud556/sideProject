import { CheckCircle2, Home } from 'lucide-react';
import type { LoanFormData } from './LoanApplicationForm';

interface LoanResultProps {
  data: LoanFormData;
  productName: string;
  onGoHome: () => void;
}

export function LoanResult({ data, productName, onGoHome }: LoanResultProps) {
  return (
    <div className="min-h-screen bg-background flex items-center justify-center p-6">
      <div className="max-w-md w-full">
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-20 h-20 rounded-full bg-success/10 mb-4">
            <CheckCircle2 className="w-12 h-12 text-success" />
          </div>
          <h2 className="text-2xl mb-2">대출 신청 완료</h2>
          <p className="text-muted-foreground">
            신청하신 대출이 정상적으로 접수되었습니다
          </p>
        </div>

        <div className="bg-card border border-border rounded-xl p-6 space-y-4 mb-6">
          <div>
            <p className="text-sm text-muted-foreground mb-1">대출 상품</p>
            <p className="text-lg">{productName}</p>
          </div>
          <div className="border-t border-border pt-4">
            <p className="text-sm text-muted-foreground mb-1">신청자</p>
            <p>{data.name}</p>
          </div>
          <div className="border-t border-border pt-4">
            <p className="text-sm text-muted-foreground mb-1">연락처</p>
            <p>{data.phone}</p>
          </div>
          <div className="border-t border-border pt-4">
            <p className="text-sm text-muted-foreground mb-1">대출 금액</p>
            <p className="text-xl text-primary">
              {data.amount.toLocaleString()}원
            </p>
          </div>
          <div className="border-t border-border pt-4">
            <p className="text-sm text-muted-foreground mb-1">대출 기간</p>
            <p>{data.period}개월</p>
          </div>
          <div className="border-t border-border pt-4 bg-accent/30 -mx-6 -mb-6 px-6 py-4 rounded-b-xl">
            <p className="text-sm text-muted-foreground mb-1">월 상환금</p>
            <p className="text-2xl text-primary">
              {data.monthlyPayment.toLocaleString()}원
            </p>
          </div>
        </div>

        <div className="bg-info/10 border border-info/20 rounded-lg p-4 mb-6">
          <p className="text-sm text-info-foreground/80">
            담당자가 신청 내용을 검토 후 1~2 영업일 이내에 연락드리겠습니다.
          </p>
        </div>

        <button
          onClick={onGoHome}
          className="w-full py-4 bg-secondary text-secondary-foreground rounded-xl hover:bg-secondary/80 transition-colors flex items-center justify-center gap-2"
        >
          <Home className="w-5 h-5" />
          <span>홈으로 돌아가기</span>
        </button>
      </div>
    </div>
  );
}