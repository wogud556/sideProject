import { useState } from 'react';
import { ArrowLeft, Calculator } from 'lucide-react';

interface LoanApplicationFormProps {
  productName: string;
  onBack: () => void;
  onSubmit: (data: LoanFormData) => void;
}

export interface LoanFormData {
  amount: number;
  period: number;
  name: string;
  phone: string;
  monthlyPayment: number;
}

export function LoanApplicationForm({
  productName,
  onBack,
  onSubmit,
}: LoanApplicationFormProps) {
  const [amount, setAmount] = useState(10000000);
  const [period, setPeriod] = useState(12);
  const [name, setName] = useState('');
  const [phone, setPhone] = useState('');

  const interestRate = 4.5;
  const monthlyRate = interestRate / 100 / 12;
  const monthlyPayment = Math.round(
    (amount * monthlyRate * Math.pow(1 + monthlyRate, period)) /
      (Math.pow(1 + monthlyRate, period) - 1)
  );

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSubmit({ amount, period, name, phone, monthlyPayment });
  };

  return (
    <div className="min-h-screen bg-background">
      <div className="max-w-2xl mx-auto">
        <div className="bg-card border-b border-border p-4 sticky top-0 z-10">
          <button
            onClick={onBack}
            className="flex items-center gap-2 text-foreground hover:text-primary transition-colors"
          >
            <ArrowLeft className="w-5 h-5" />
            <span>돌아가기</span>
          </button>
        </div>

        <div className="p-6">
          <div className="mb-8">
            <h2 className="text-2xl mb-2">{productName}</h2>
            <p className="text-muted-foreground">대출 신청 정보를 입력해주세요</p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-6">
            <div className="bg-accent/50 rounded-xl p-6 mb-6">
              <div className="flex items-center gap-2 mb-4 text-primary">
                <Calculator className="w-5 h-5" />
                <span>예상 월 상환금</span>
              </div>
              <p className="text-3xl text-primary">
                {monthlyPayment.toLocaleString()}원
              </p>
            </div>

            <div>
              <label className="block mb-3">
                대출 금액
                <span className="text-muted-foreground ml-2">
                  {amount.toLocaleString()}원
                </span>
              </label>
              <input
                type="range"
                min="1000000"
                max="100000000"
                step="1000000"
                value={amount}
                onChange={(e) => setAmount(Number(e.target.value))}
                className="w-full h-2 bg-secondary rounded-lg appearance-none cursor-pointer accent-primary"
              />
              <div className="flex justify-between text-xs text-muted-foreground mt-2">
                <span>100만원</span>
                <span>1억원</span>
              </div>
            </div>

            <div>
              <label className="block mb-3">
                대출 기간
                <span className="text-muted-foreground ml-2">{period}개월</span>
              </label>
              <input
                type="range"
                min="6"
                max="60"
                step="6"
                value={period}
                onChange={(e) => setPeriod(Number(e.target.value))}
                className="w-full h-2 bg-secondary rounded-lg appearance-none cursor-pointer accent-primary"
              />
              <div className="flex justify-between text-xs text-muted-foreground mt-2">
                <span>6개월</span>
                <span>60개월</span>
              </div>
            </div>

            <div className="border-t border-border pt-6">
              <div className="space-y-4">
                <div>
                  <label className="block mb-2">이름</label>
                  <input
                    type="text"
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    required
                    placeholder="홍길동"
                    className="w-full px-4 py-3 bg-input-background border border-border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
                  />
                </div>
                <div>
                  <label className="block mb-2">연락처</label>
                  <input
                    type="tel"
                    value={phone}
                    onChange={(e) => setPhone(e.target.value)}
                    required
                    placeholder="010-1234-5678"
                    className="w-full px-4 py-3 bg-input-background border border-border rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
                  />
                </div>
              </div>
            </div>

            <button
              type="submit"
              className="w-full py-4 bg-primary text-primary-foreground rounded-xl hover:opacity-90 transition-opacity"
            >
              대출 신청하기
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}