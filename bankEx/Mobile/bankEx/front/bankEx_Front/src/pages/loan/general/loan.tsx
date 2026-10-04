import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  getLoanProductsApi,
  getLoanProductApi,
  getAccountsApi,
  applyLoanApi,
  type LoanProductResponse,
  type LoanApplicationResponse,
} from '../../../api/bank_api'
import { path } from '../../../router/path'

type Screen = 'list' | 'apply' | 'result'

export default function Loan() {
  const navigate = useNavigate()
  const user = JSON.parse(localStorage.getItem('user') || 'null')

  const [screen, setScreen] = useState<Screen>('list')
  const [products, setProducts] = useState<LoanProductResponse[]>([])
  const [selectedProduct, setSelectedProduct] = useState<LoanProductResponse | null>(null)
  const [accountNumber, setAccountNumber] = useState('')
  const [amount, setAmount] = useState(10_000_000)
  const [period, setPeriod] = useState(12)
  const [result, setResult] = useState<LoanApplicationResponse | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!user) {
      navigate(path.login)
      return
    }
    getLoanProductsApi()
      .then(setProducts)
      .catch(() => setError('상품 목록을 불러오지 못했습니다'))

    getAccountsApi(user.userId).then((accounts) => {
      if (accounts.length > 0) setAccountNumber(accounts[0].accountNumber)
    })
  }, [])

  const handleSelectProduct = async (productId: number) => {
    try {
      const product = await getLoanProductApi(productId)
      setSelectedProduct(product)
      setAmount(Math.min(10_000_000, product.maxLimit))
      setScreen('apply')
      setError('')
    } catch {
      setError('상품 정보를 불러오지 못했습니다')
    }
  }

  const handleApply = async () => {
    if (!selectedProduct || !accountNumber) return
    setLoading(true)
    setError('')
    try {
      const res = await applyLoanApi(user.userId, accountNumber, selectedProduct.productId, amount, period)
      setResult(res)
      setScreen('result')
    } catch (err: any) {
      setError(err?.response?.data || '대출 신청에 실패했습니다')
    } finally {
      setLoading(false)
    }
  }

  const monthlyRate = selectedProduct ? selectedProduct.interestRate / 100 / 12 : 0
  const monthlyPayment = selectedProduct
    ? Math.round((amount * monthlyRate * Math.pow(1 + monthlyRate, period)) / (Math.pow(1 + monthlyRate, period) - 1))
    : 0

  if (screen === 'result' && result) {
    return (
      <div style={styles.container}>
        <div style={styles.card}>
          <div style={styles.successIcon}>✓</div>
          <h2 style={styles.successTitle}>대출 신청 완료</h2>
          <p style={styles.successSub}>신청하신 대출이 정상적으로 접수되었습니다</p>

          <div style={styles.resultBox}>
            <ResultRow label="대출 상품" value={result.productName} />
            <ResultRow label="신청 금액" value={`${result.requestAmount.toLocaleString()}원`} />
            <ResultRow label="대출 기간" value={`${result.loanPeriod}개월`} />
            <ResultRow label="처리 상태" value={result.status} highlight />
          </div>

          <p style={styles.notice}>담당자가 신청 내용 검토 후 1~2 영업일 내 연락드립니다.</p>

          <button onClick={() => navigate(path.home)} style={styles.button}>홈으로 돌아가기</button>
          <button onClick={() => navigate(path.myLoans)} style={styles.outlineButton}>내 대출 내역 보기</button>
        </div>
      </div>
    )
  }

  if (screen === 'apply' && selectedProduct) {
    return (
      <div style={styles.container}>
        <div style={{ ...styles.card, width: '360px' }}>
          <button onClick={() => setScreen('list')} style={styles.backButton}>← 돌아가기</button>
          <h2 style={styles.cardTitle}>{selectedProduct.productName}</h2>
          <p style={styles.cardSub}>대출 신청 정보를 입력해주세요</p>

          <div style={styles.calcBox}>
            <p style={styles.calcLabel}>예상 월 상환금</p>
            <p style={styles.calcAmount}>{monthlyPayment.toLocaleString()}원</p>
          </div>

          <div style={styles.fieldGroup}>
            <label style={styles.label}>
              대출 금액 <span style={styles.labelSub}>{amount.toLocaleString()}원</span>
            </label>
            <input
              type="range"
              min={1_000_000}
              max={selectedProduct.maxLimit}
              step={1_000_000}
              value={amount}
              onChange={(e) => setAmount(Number(e.target.value))}
              style={styles.range}
            />
            <div style={styles.rangeLabels}>
              <span>100만원</span>
              <span>{(selectedProduct.maxLimit / 100_000_000).toFixed(0)}억원</span>
            </div>
          </div>

          <div style={styles.fieldGroup}>
            <label style={styles.label}>
              대출 기간 <span style={styles.labelSub}>{period}개월</span>
            </label>
            <input
              type="range"
              min={6}
              max={60}
              step={6}
              value={period}
              onChange={(e) => setPeriod(Number(e.target.value))}
              style={styles.range}
            />
            <div style={styles.rangeLabels}>
              <span>6개월</span>
              <span>60개월</span>
            </div>
          </div>

          <div style={styles.fieldGroup}>
            <label style={styles.label}>연결 계좌번호</label>
            <input
              type="text"
              value={accountNumber}
              onChange={(e) => setAccountNumber(e.target.value)}
              placeholder="계좌번호"
              style={styles.input}
            />
          </div>

          {error && <p style={styles.error}>{error}</p>}

          <button
            onClick={handleApply}
            disabled={loading}
            style={{ ...styles.button, opacity: loading ? 0.7 : 1 }}
          >
            {loading ? '신청 중...' : '대출 신청하기'}
          </button>
        </div>
      </div>
    )
  }

  return (
    <div style={styles.container}>
      <div style={{ ...styles.card, width: '380px' }}>
        <div style={styles.header}>
          <button onClick={() => navigate(path.home)} style={styles.backButton}>← 홈</button>
          <h2 style={styles.cardTitle}>대출 상품</h2>
          <p style={styles.cardSub}>원하시는 대출 상품을 선택해주세요</p>
        </div>

        {error && <p style={styles.error}>{error}</p>}

        <div style={styles.productList}>
          {products.map((p) => (
            <button
              key={p.productId}
              onClick={() => handleSelectProduct(p.productId)}
              style={styles.productCard}
            >
              <div style={styles.productName}>{p.productName}</div>
              <div style={styles.productDesc}>{p.description}</div>
              <div style={styles.productInfo}>
                <span style={styles.infoChip}>연 {p.interestRate}%</span>
                <span style={styles.infoChip}>최대 {(p.maxLimit / 10_000).toLocaleString()}만원</span>
              </div>
            </button>
          ))}
        </div>
      </div>
    </div>
  )
}

function ResultRow({ label, value, highlight }: { label: string; value: string; highlight?: boolean }) {
  return (
    <div style={{ display: 'flex', justifyContent: 'space-between', padding: '10px 0', borderBottom: '1px solid #f0f0f0' }}>
      <span style={{ color: '#888', fontSize: '14px' }}>{label}</span>
      <span style={{ fontWeight: highlight ? 'bold' : 'normal', color: highlight ? '#2c7be5' : '#333' }}>{value}</span>
    </div>
  )
}

const styles = {
  container: {
    minHeight: '100vh',
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    background: '#f5f6f8',
    padding: '20px',
  },
  card: {
    width: '320px',
    padding: '24px',
    borderRadius: '16px',
    background: '#fff',
    boxShadow: '0 4px 12px rgba(0,0,0,0.1)',
  },
  header: {
    marginBottom: '16px',
  },
  backButton: {
    background: 'none',
    border: 'none',
    color: '#2c7be5',
    cursor: 'pointer',
    fontSize: '14px',
    padding: '0',
    marginBottom: '8px',
  },
  cardTitle: {
    margin: '0 0 4px 0',
    fontSize: '20px',
  },
  cardSub: {
    color: '#888',
    fontSize: '13px',
    margin: '0 0 16px 0',
  },
  productList: {
    display: 'flex',
    flexDirection: 'column' as const,
    gap: '12px',
  },
  productCard: {
    width: '100%',
    padding: '16px',
    borderRadius: '12px',
    border: '1px solid #e8e8e8',
    background: '#fff',
    textAlign: 'left' as const,
    cursor: 'pointer',
  },
  productName: {
    fontSize: '16px',
    fontWeight: 'bold',
    marginBottom: '6px',
  },
  productDesc: {
    fontSize: '13px',
    color: '#666',
    marginBottom: '10px',
    lineHeight: '1.4',
  },
  productInfo: {
    display: 'flex',
    gap: '8px',
  },
  infoChip: {
    fontSize: '12px',
    color: '#2c7be5',
    background: '#e8f0fb',
    padding: '3px 8px',
    borderRadius: '20px',
  },
  calcBox: {
    background: '#f0f5ff',
    borderRadius: '10px',
    padding: '16px',
    textAlign: 'center' as const,
    marginBottom: '20px',
  },
  calcLabel: {
    fontSize: '13px',
    color: '#666',
    marginBottom: '4px',
  },
  calcAmount: {
    fontSize: '24px',
    fontWeight: 'bold',
    color: '#2c7be5',
  },
  fieldGroup: {
    marginBottom: '16px',
  },
  label: {
    display: 'block',
    fontSize: '14px',
    marginBottom: '8px',
    fontWeight: '500',
  },
  labelSub: {
    color: '#888',
    fontWeight: 'normal',
    marginLeft: '8px',
  },
  range: {
    width: '100%',
    accentColor: '#2c7be5',
  },
  rangeLabels: {
    display: 'flex',
    justifyContent: 'space-between',
    fontSize: '12px',
    color: '#999',
    marginTop: '4px',
  },
  input: {
    width: '100%',
    padding: '10px 12px',
    borderRadius: '8px',
    border: '1px solid #ddd',
    fontSize: '14px',
    boxSizing: 'border-box' as const,
  },
  button: {
    width: '100%',
    padding: '12px',
    borderRadius: '8px',
    border: 'none',
    background: '#2c7be5',
    color: '#fff',
    fontWeight: 'bold',
    cursor: 'pointer',
    marginTop: '8px',
    fontSize: '15px',
  },
  outlineButton: {
    width: '100%',
    padding: '12px',
    borderRadius: '8px',
    border: '1px solid #2c7be5',
    background: '#fff',
    color: '#2c7be5',
    fontWeight: 'bold',
    cursor: 'pointer',
    marginTop: '8px',
    fontSize: '15px',
  },
  error: {
    color: 'red',
    fontSize: '14px',
    marginBottom: '10px',
  },
  resultBox: {
    border: '1px solid #f0f0f0',
    borderRadius: '10px',
    padding: '0 16px',
    marginBottom: '16px',
  },
  successIcon: {
    width: '60px',
    height: '60px',
    borderRadius: '50%',
    background: '#e8f5e9',
    color: '#43a047',
    fontSize: '28px',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    margin: '0 auto 12px',
  },
  successTitle: {
    textAlign: 'center' as const,
    marginBottom: '4px',
  },
  successSub: {
    textAlign: 'center' as const,
    color: '#888',
    fontSize: '13px',
    marginBottom: '20px',
  },
  notice: {
    fontSize: '13px',
    color: '#666',
    background: '#f5f6f8',
    borderRadius: '8px',
    padding: '12px',
    marginBottom: '16px',
    lineHeight: '1.5',
  },
}
