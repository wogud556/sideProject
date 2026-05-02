import { useState } from 'react';
import { LoanProductCard } from './components/LoanProductCard';
import { LoanApplicationForm, LoanFormData } from './components/LoanApplicationForm';
import { LoanResult } from './components/LoanResult';
import { Home, CreditCard, Building2, Briefcase } from 'lucide-react';

type Screen = 'home' | 'application' | 'result';

export default function App() {
  const [currentScreen, setCurrentScreen] = useState<Screen>('home');
  const [selectedProduct, setSelectedProduct] = useState('');
  const [formData, setFormData] = useState<LoanFormData | null>(null);

  const handleProductSelect = (productName: string) => {
    setSelectedProduct(productName);
    setCurrentScreen('application');
  };

  const handleFormSubmit = (data: LoanFormData) => {
    setFormData(data);
    setCurrentScreen('result');
  };

  const handleGoHome = () => {
    setCurrentScreen('home');
    setSelectedProduct('');
    setFormData(null);
  };

  if (currentScreen === 'application') {
    return (
      <LoanApplicationForm
        productName={selectedProduct}
        onBack={handleGoHome}
        onSubmit={handleFormSubmit}
      />
    );
  }

  if (currentScreen === 'result' && formData) {
    return (
      <LoanResult
        data={formData}
        productName={selectedProduct}
        onGoHome={handleGoHome}
      />
    );
  }

  return (
    <div className="min-h-screen bg-background">
      <header className="bg-card border-b border-border sticky top-0 z-10">
        <div className="max-w-4xl mx-auto px-6 py-4">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-lg bg-primary flex items-center justify-center">
              <Building2 className="w-5 h-5 text-primary-foreground" />
            </div>
            <h1 className="text-xl">KB 국민은행</h1>
          </div>
        </div>
      </header>

      <main className="max-w-4xl mx-auto px-6 py-8">
        <div className="mb-8">
          <h2 className="text-2xl mb-2">대출 상품</h2>
          <p className="text-muted-foreground">
            고객님께 맞는 최적의 대출 상품을 선택해주세요
          </p>
        </div>

        <div className="grid gap-4 mb-8">
          <LoanProductCard
            title="신용대출"
            description="별도 담보 없이 신용도를 기반으로 대출받을 수 있습니다"
            interestRate="연 4.5%"
            maxAmount="최대 1억원"
            icon={<CreditCard className="w-6 h-6" />}
            onClick={() => handleProductSelect('신용대출')}
          />
          <LoanProductCard
            title="주택담보대출"
            description="주택을 담보로 저금리 대출을 받을 수 있습니다"
            interestRate="연 3.2%"
            maxAmount="최대 5억원"
            icon={<Home className="w-6 h-6" />}
            onClick={() => handleProductSelect('주택담보대출')}
          />
          <LoanProductCard
            title="사업자대출"
            description="사업 운영자금이 필요한 사업자를 위한 대출 상품입니다"
            interestRate="연 3.8%"
            maxAmount="최대 3억원"
            icon={<Briefcase className="w-6 h-6" />}
            onClick={() => handleProductSelect('사업자대출')}
          />
        </div>

        <div className="bg-card border border-border rounded-xl p-6">
          <h3 className="mb-4">대출 신청 안내</h3>
          <ul className="space-y-3 text-sm text-muted-foreground">
            <li className="flex gap-2">
              <span className="text-primary">•</span>
              <span>신용대출은 재직증명서 및 소득증빙 서류가 필요합니다</span>
            </li>
            <li className="flex gap-2">
              <span className="text-primary">•</span>
              <span>주택담보대출은 등기부등본 및 건물등기사항전부증명서가 필요합니다</span>
            </li>
            <li className="flex gap-2">
              <span className="text-primary">•</span>
              <span>대출 승인까지 1~2 영업일이 소요됩니다</span>
            </li>
            <li className="flex gap-2">
              <span className="text-primary">•</span>
              <span>중도상환 수수료는 대출 후 3년 이내 면제됩니다</span>
            </li>
          </ul>
        </div>
      </main>

      <footer className="bg-card border-t border-border mt-12">
        <div className="max-w-4xl mx-auto px-6 py-6">
          <p className="text-sm text-muted-foreground text-center">
            KB국민은행 고객센터: 1588-9999 | 평일 09:00-18:00
          </p>
        </div>
      </footer>
    </div>
  );
}