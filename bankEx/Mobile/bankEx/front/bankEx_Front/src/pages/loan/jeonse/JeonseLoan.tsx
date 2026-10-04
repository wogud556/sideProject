import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  getJeonseLoanProductsApi,
  applyJeonseLoanApi,
  reviewJeonseLoanApi,
  executeJeonseLoanApi,
  extractErrorMessage,
  type JeonseLoanProductResponse,
  type JeonseLoanApplyResponse,
  type JeonseLoanReviewResponse,
  type JeonseLoanExecuteResponse,
} from '../../../api/bank_api'
import { path } from '../../../router/path'

type Screen = 'product' | 'apply' | 'result' | 'reviewed' | 'executed'

const initialForm = {
  requestAmount: 80_000_000,
  annualIncome: 65_000_000,
  existingDebtAmount: 20_000_000,
  creditScore: 820,
  homelessYn: 'Y',
  householderYn: 'Y',
  guaranteeOrg: 'HF',
  salaryTransferYn: false,
  cardUsageYn: false,
  autoTransferYn: false,
  lessorName: '',
  lessorPhone: '',
  houseAddress: '',
  houseType: 'OFFICETEL',
  capitalAreaYn: 'Y',
  depositAmount: 130_000_000,
  downPaymentAmount: 10_000_000,
  contractStartDate: '',
  contractEndDate: '',
  fixedDateYn: 'Y',
  moveInPlanYn: 'Y',
  seniorClaimAmount: 0,
}

export default function JeonseLoan() {
  const navigate = useNavigate()
  const user = JSON.parse(localStorage.getItem('user') || 'null')

  const [screen, setScreen] = useState<Screen>('product')
  const [product, setProduct] = useState<JeonseLoanProductResponse | null>(null)
  const [form, setForm] = useState(initialForm)
  const [applyResult, setApplyResult] = useState<JeonseLoanApplyResponse | null>(null)
  const [reviewResult, setReviewResult] = useState<JeonseLoanReviewResponse | null>(null)
  const [executeResult, setExecuteResult] = useState<JeonseLoanExecuteResponse | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!user) {
      navigate(path.login)
      return
    }
    getJeonseLoanProductsApi()
      .then((products) => setProduct(products[0] ?? null))
      .catch(() => setError('상품 정보를 불러오지 못했습니다'))
  }, [])

  const updateForm = (key: keyof typeof initialForm, value: string | number | boolean) => {
    setForm((prev) => ({ ...prev, [key]: value }))
  }

  const handleApply = async () => {
    if (!product) return
    setLoading(true)
    setError('')
    try {
      const response = await applyJeonseLoanApi({
        userId: user.userId,
        productId: product.productId,
        requestAmount: Number(form.requestAmount),
        annualIncome: Number(form.annualIncome),
        existingDebtAmount: Number(form.existingDebtAmount),
        creditScore: Number(form.creditScore),
        homelessYn: form.homelessYn,
        householderYn: form.householderYn,
        guaranteeOrg: form.guaranteeOrg,
        repaymentType: 'MATURITY',
        salaryTransferYn: form.salaryTransferYn,
        cardUsageYn: form.cardUsageYn,
        autoTransferYn: form.autoTransferYn,
        contract: {
          lessorName: form.lessorName,
          lessorPhone: form.lessorPhone,
          houseAddress: form.houseAddress,
          houseType: form.houseType,
          capitalAreaYn: form.capitalAreaYn,
          depositAmount: Number(form.depositAmount),
          downPaymentAmount: Number(form.downPaymentAmount),
          contractStartDate: form.contractStartDate,
          contractEndDate: form.contractEndDate,
          fixedDateYn: form.fixedDateYn,
          moveInPlanYn: form.moveInPlanYn,
          seniorClaimAmount: Number(form.seniorClaimAmount),
        },
      })
      setApplyResult(response)
      setScreen('result')
    } catch (err: any) {
      setError(extractErrorMessage(err, '전세대출 신청에 실패했습니다'))
    } finally {
      setLoading(false)
    }
  }

  const handleReview = async () => {
    if (!applyResult) return
    setLoading(true)
    setError('')
    try {
      const response = await reviewJeonseLoanApi(applyResult.applicationId)
      setReviewResult(response)
      setScreen('reviewed')
    } catch (err: any) {
      setError(extractErrorMessage(err, '심사 실행에 실패했습니다'))
    } finally {
      setLoading(false)
    }
  }

  const handleExecute = async () => {
    if (!reviewResult) return
    setLoading(true)
    setError('')
    try {
      const response = await executeJeonseLoanApi(reviewResult.applicationId)
      setExecuteResult(response)
      setScreen('executed')
    } catch (err: any) {
      setError(extractErrorMessage(err, '대출 실행에 실패했습니다'))
    } finally {
      setLoading(false)
    }
  }

  if (screen === 'executed' && executeResult) {
    return (
      <div style={styles.container}>
        <div style={styles.card}>
          <div style={styles.successIcon}>✓</div>
          <h2 style={styles.successTitle}>전세대출 실행 완료</h2>
          <p style={styles.successSub}>{executeResult.message}</p>

          <div style={styles.resultBox}>
            <ResultRow label="신청번호" value={executeResult.applicationId} />
            <ResultRow label="실행 금액" value={`${executeResult.executedAmount.toLocaleString()}원`} />
            <ResultRow label="첫 상환일" value={executeResult.firstPaymentDate} highlight />
          </div>

          <button onClick={() => navigate(path.home)} style={styles.button}>홈으로 돌아가기</button>
        </div>
      </div>
    )
  }

  if (screen === 'reviewed' && reviewResult) {
    return (
      <div style={styles.container}>
        <div style={styles.card}>
          <h2 style={styles.cardTitle}>심사 결과</h2>
          <p style={styles.cardSub}>{reviewResult.message}</p>

          <div style={styles.resultBox}>
            <ResultRow label="신청번호" value={reviewResult.applicationId} />
            <ResultRow label="심사 상태" value={reviewResult.status} highlight />
            {reviewResult.approvedAmount != null && (
              <ResultRow label="승인 금액" value={`${reviewResult.approvedAmount.toLocaleString()}원`} />
            )}
            {reviewResult.loanRate != null && (
              <ResultRow label="적용 금리" value={`연 ${reviewResult.loanRate.toFixed(2)}%`} />
            )}
          </div>

          {error && <p style={styles.error}>{error}</p>}

          {reviewResult.status === 'APPROVED' ? (
            <button
              onClick={handleExecute}
              disabled={loading}
              style={{ ...styles.button, opacity: loading ? 0.7 : 1 }}
            >
              {loading ? '실행 중...' : '대출 실행'}
            </button>
          ) : (
            <button onClick={() => navigate(path.home)} style={styles.button}>홈으로 돌아가기</button>
          )}
        </div>
      </div>
    )
  }

  if (screen === 'result' && applyResult) {
    return (
      <div style={styles.container}>
        <div style={styles.card}>
          <h2 style={styles.cardTitle}>신청 결과</h2>
          <p style={styles.cardSub}>{applyResult.message}</p>

          <div style={styles.resultBox}>
            <ResultRow label="신청번호" value={applyResult.applicationId} />
            <ResultRow label="처리 상태" value={applyResult.status} highlight />
            <ResultRow label="신청 금액" value={`${applyResult.requestAmount.toLocaleString()}원`} />
            {applyResult.availableLimitAmount != null && (
              <ResultRow label="산출 한도" value={`${applyResult.availableLimitAmount.toLocaleString()}원`} />
            )}
            {applyResult.estimatedRate != null && (
              <ResultRow label="예상 금리" value={`연 ${applyResult.estimatedRate.toFixed(2)}%`} />
            )}
          </div>

          {error && <p style={styles.error}>{error}</p>}

          {applyResult.status === 'LIMIT_CALCULATED' ? (
            <button
              onClick={handleReview}
              disabled={loading}
              style={{ ...styles.button, opacity: loading ? 0.7 : 1 }}
            >
              {loading ? '심사 중...' : '심사 실행'}
            </button>
          ) : (
            <button onClick={() => navigate(path.home)} style={styles.button}>홈으로 돌아가기</button>
          )}
        </div>
      </div>
    )
  }

  if (screen === 'apply' && product) {
    return (
      <div style={styles.container}>
        <div style={{ ...styles.card, width: '420px' }}>
          <button onClick={() => setScreen('product')} style={styles.backButton}>← 돌아가기</button>
          <h2 style={styles.cardTitle}>{product.productName}</h2>
          <p style={styles.cardSub}>소득/부채 정보와 임대차계약 정보를 입력해주세요</p>

          <div style={styles.formScroll}>
            <SectionTitle>신청 정보</SectionTitle>
            <NumberField label="신청 금액" value={form.requestAmount} onChange={(v) => updateForm('requestAmount', v)} />
            <NumberField label="연소득" value={form.annualIncome} onChange={(v) => updateForm('annualIncome', v)} />
            <NumberField label="기존 부채 금액" value={form.existingDebtAmount} onChange={(v) => updateForm('existingDebtAmount', v)} />
            <NumberField label="신용점수" value={form.creditScore} onChange={(v) => updateForm('creditScore', v)} />
            <YnField label="무주택 여부" value={form.homelessYn} onChange={(v) => updateForm('homelessYn', v)} />
            <YnField label="세대주 여부" value={form.householderYn} onChange={(v) => updateForm('householderYn', v)} />
            <SelectField
              label="보증기관"
              value={form.guaranteeOrg}
              options={['HF', 'HUG', 'SGI']}
              onChange={(v) => updateForm('guaranteeOrg', v)}
            />
            <div style={styles.fieldGroup}>
              <label style={styles.checkboxLabel}>
                <input type="checkbox" checked={form.salaryTransferYn} onChange={(e) => updateForm('salaryTransferYn', e.target.checked)} />
                급여이체 실적 있음
              </label>
              <label style={styles.checkboxLabel}>
                <input type="checkbox" checked={form.cardUsageYn} onChange={(e) => updateForm('cardUsageYn', e.target.checked)} />
                카드 사용 실적 있음
              </label>
              <label style={styles.checkboxLabel}>
                <input type="checkbox" checked={form.autoTransferYn} onChange={(e) => updateForm('autoTransferYn', e.target.checked)} />
                자동이체 등록 있음
              </label>
            </div>

            <SectionTitle>임대차계약 정보</SectionTitle>
            <TextField label="임대인명" value={form.lessorName} onChange={(v) => updateForm('lessorName', v)} />
            <TextField label="임대인 연락처" value={form.lessorPhone} onChange={(v) => updateForm('lessorPhone', v)} />
            <TextField label="주택 주소" value={form.houseAddress} onChange={(v) => updateForm('houseAddress', v)} />
            <SelectField
              label="주택 유형"
              value={form.houseType}
              options={['APARTMENT', 'OFFICETEL', 'VILLA', 'HOUSE']}
              onChange={(v) => updateForm('houseType', v)}
            />
            <YnField label="수도권 여부" value={form.capitalAreaYn} onChange={(v) => updateForm('capitalAreaYn', v)} />
            <NumberField label="전세보증금" value={form.depositAmount} onChange={(v) => updateForm('depositAmount', v)} />
            <NumberField label="계약금 납입액" value={form.downPaymentAmount} onChange={(v) => updateForm('downPaymentAmount', v)} />
            <DateField label="계약 시작일" value={form.contractStartDate} onChange={(v) => updateForm('contractStartDate', v)} />
            <DateField label="계약 종료일" value={form.contractEndDate} onChange={(v) => updateForm('contractEndDate', v)} />
            <YnField label="확정일자 여부" value={form.fixedDateYn} onChange={(v) => updateForm('fixedDateYn', v)} />
            <YnField label="전입 예정 여부" value={form.moveInPlanYn} onChange={(v) => updateForm('moveInPlanYn', v)} />
            <NumberField label="선순위채권 금액" value={form.seniorClaimAmount} onChange={(v) => updateForm('seniorClaimAmount', v)} />
          </div>

          {error && <p style={styles.error}>{error}</p>}

          <button
            onClick={handleApply}
            disabled={loading}
            style={{ ...styles.button, opacity: loading ? 0.7 : 1 }}
          >
            {loading ? '신청 중...' : '전세대출 신청하기'}
          </button>
        </div>
      </div>
    )
  }

  return (
    <div style={styles.container}>
      <div style={{ ...styles.card, width: '360px' }}>
        <button onClick={() => navigate(path.home)} style={styles.backButton}>← 홈</button>
        <h2 style={styles.cardTitle}>전세대출</h2>
        <p style={styles.cardSub}>임차보증금을 담보로 전세자금을 대출받을 수 있습니다</p>

        {error && <p style={styles.error}>{error}</p>}

        {product && (
          <div style={styles.productCard}>
            <div style={styles.productName}>{product.productName}</div>
            <div style={styles.productInfo}>
              <span style={styles.infoChip}>금리 {product.minRate}% ~ {product.maxRate}%</span>
              <span style={styles.infoChip}>최대 {(product.maxLimitAmount / 100_000_000).toFixed(1)}억원</span>
            </div>
            <button onClick={() => setScreen('apply')} style={styles.button}>신청하기</button>
          </div>
        )}
      </div>
    </div>
  )
}

function SectionTitle({ children }: { children: React.ReactNode }) {
  return <p style={styles.sectionTitle}>{children}</p>
}

function TextField({ label, value, onChange }: { label: string; value: string; onChange: (v: string) => void }) {
  return (
    <div style={styles.fieldGroup}>
      <label style={styles.label}>{label}</label>
      <input type="text" value={value} onChange={(e) => onChange(e.target.value)} style={styles.input} />
    </div>
  )
}

function NumberField({ label, value, onChange }: { label: string; value: number; onChange: (v: number) => void }) {
  return (
    <div style={styles.fieldGroup}>
      <label style={styles.label}>{label}</label>
      <input type="number" value={value} onChange={(e) => onChange(Number(e.target.value))} style={styles.input} />
    </div>
  )
}

function DateField({ label, value, onChange }: { label: string; value: string; onChange: (v: string) => void }) {
  return (
    <div style={styles.fieldGroup}>
      <label style={styles.label}>{label}</label>
      <input type="date" value={value} onChange={(e) => onChange(e.target.value)} style={styles.input} />
    </div>
  )
}

function YnField({ label, value, onChange }: { label: string; value: string; onChange: (v: string) => void }) {
  return <SelectField label={label} value={value} options={['Y', 'N']} onChange={onChange} />
}

function SelectField({ label, value, options, onChange }: { label: string; value: string; options: string[]; onChange: (v: string) => void }) {
  return (
    <div style={styles.fieldGroup}>
      <label style={styles.label}>{label}</label>
      <select value={value} onChange={(e) => onChange(e.target.value)} style={styles.input}>
        {options.map((option) => (
          <option key={option} value={option}>{option}</option>
        ))}
      </select>
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
  sectionTitle: {
    fontSize: '13px',
    fontWeight: 'bold',
    color: '#2c7be5',
    margin: '16px 0 8px 0',
  },
  formScroll: {
    maxHeight: '55vh',
    overflowY: 'auto' as const,
    paddingRight: '4px',
    marginBottom: '12px',
  },
  productCard: {
    padding: '16px',
    borderRadius: '12px',
    border: '1px solid #e8e8e8',
  },
  productName: {
    fontSize: '16px',
    fontWeight: 'bold',
    marginBottom: '10px',
  },
  productInfo: {
    display: 'flex',
    gap: '8px',
    marginBottom: '16px',
  },
  infoChip: {
    fontSize: '12px',
    color: '#2c7be5',
    background: '#e8f0fb',
    padding: '3px 8px',
    borderRadius: '20px',
  },
  fieldGroup: {
    marginBottom: '12px',
  },
  label: {
    display: 'block',
    fontSize: '13px',
    marginBottom: '6px',
    fontWeight: '500',
  },
  checkboxLabel: {
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
    fontSize: '13px',
    marginBottom: '8px',
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
}
