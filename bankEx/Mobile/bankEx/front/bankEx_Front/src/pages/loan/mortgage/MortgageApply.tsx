import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { extractErrorMessage } from '../../../api/bank_api'
import {
  applyMortgageApi,
  getMortgageProductsApi,
  type MortgageApplication,
  type MortgageApplyRequest,
  type MortgageDebtInput,
  type MortgageProduct,
} from '../../../api/mortgage_api'
import { path } from '../../../router/path'
import { styles } from './mortgageStyles'
import MortgageRow from './MortgageRow'

type Screen = 'product' | 'apply' | 'result'

const initialForm = {
  requestedAmount: 300_000_000,
  termMonths: 360,
  repaymentMethod: 'EQUAL_INSTALLMENT' as MortgageApplyRequest['repaymentMethod'],
  loanPurpose: 'PURCHASE' as MortgageApplyRequest['loanPurpose'],
  houseCount: 0,
  firstHomeBuyer: false,
  address: '',
  regionCode: 'NON_REGULATED' as MortgageApplyRequest['property']['regionCode'],
  propertyType: 'APARTMENT' as MortgageApplyRequest['property']['propertyType'],
  exclusiveArea: 84,
  declaredPrice: 500_000_000,
  incomeType: 'EMPLOYEE' as MortgageApplyRequest['income']['incomeType'],
  annualIncome: 65_000_000,
  employerName: '',
}

const emptyDebt: MortgageDebtInput = { debtType: 'CREDIT', institution: '', balance: 0, annualPrincipal: 0, annualInterest: 0 }

export default function MortgageApply() {
  const navigate = useNavigate()
  const user = JSON.parse(localStorage.getItem('user') || 'null')

  const [screen, setScreen] = useState<Screen>('product')
  const [products, setProducts] = useState<MortgageProduct[]>([])
  const [product, setProduct] = useState<MortgageProduct | null>(null)
  const [form, setForm] = useState(initialForm)
  const [debts, setDebts] = useState<MortgageDebtInput[]>([])
  const [result, setResult] = useState<MortgageApplication | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!user) {
      navigate(path.login)
      return
    }
    getMortgageProductsApi()
      .then(setProducts)
      .catch((err) => setError(extractErrorMessage(err, '상품 정보를 불러오지 못했습니다')))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const update = <K extends keyof typeof initialForm>(key: K, value: (typeof initialForm)[K]) =>
    setForm((prev) => ({ ...prev, [key]: value }))

  const updateDebt = (index: number, patch: Partial<MortgageDebtInput>) =>
    setDebts((prev) => prev.map((d, i) => (i === index ? { ...d, ...patch } : d)))

  const handleApply = async () => {
    if (!product) return
    setLoading(true)
    setError('')
    try {
      const response = await applyMortgageApi({
        userId: user.userId,
        productCode: product.productCode,
        requestedAmount: Number(form.requestedAmount),
        termMonths: Number(form.termMonths),
        repaymentMethod: form.repaymentMethod,
        loanPurpose: form.loanPurpose,
        houseCount: Number(form.houseCount),
        firstHomeBuyer: form.firstHomeBuyer,
        property: {
          address: form.address,
          regionCode: form.regionCode,
          propertyType: form.propertyType,
          exclusiveArea: form.exclusiveArea ? Number(form.exclusiveArea) : null,
          declaredPrice: Number(form.declaredPrice),
        },
        income: {
          incomeType: form.incomeType,
          annualIncome: Number(form.annualIncome),
          employerName: form.employerName,
        },
        debts: debts.map((d) => ({
          ...d,
          balance: Number(d.balance),
          annualPrincipal: Number(d.annualPrincipal),
          annualInterest: Number(d.annualInterest),
        })),
      })
      setResult(response)
      setScreen('result')
    } catch (err) {
      setError(extractErrorMessage(err, '주택담보대출 신청에 실패했습니다'))
    } finally {
      setLoading(false)
    }
  }

  if (screen === 'result' && result) {
    return (
      <div style={styles.container}>
        <div style={styles.card}>
          <h2 style={styles.cardTitle}>신청 완료</h2>
          <p style={styles.cardSub}>심사 진행 상황은 '내 주담대'에서 확인할 수 있습니다.</p>
          <div style={styles.notice}>{result.notice}</div>
          <div style={styles.resultBox}>
            <MortgageRow label="신청번호" value={result.applicationNo} />
            <MortgageRow label="신청 금액" value={`${result.requestedAmount.toLocaleString()}원`} />
            <MortgageRow label="진행 상태" value={result.statusLabel} highlight />
          </div>
          <button onClick={() => navigate(path.mortgageApplications)} style={styles.button}>내 주담대 보기</button>
          <button onClick={() => navigate(path.home)} style={styles.subButton}>홈으로</button>
        </div>
      </div>
    )
  }

  if (screen === 'apply' && product) {
    return (
      <div style={styles.container}>
        <div style={styles.card}>
          <button onClick={() => setScreen('product')} style={styles.backButton}>← 상품 선택</button>
          <h2 style={styles.cardTitle}>{product.productName}</h2>
          <p style={styles.cardSub}>
            {(product.minAmount / 10_000).toLocaleString()}만 ~ {(product.maxAmount / 100_000_000).toLocaleString()}억 원 ·
            {' '}{product.minTermMonths / 12}~{product.maxTermMonths / 12}년
          </p>

          <p style={styles.sectionTitle}>대출 조건</p>
          <Field label="신청 금액 (원)">
            <input type="number" value={form.requestedAmount} onChange={(e) => update('requestedAmount', Number(e.target.value))} style={styles.input} />
          </Field>
          <Field label="대출 기간">
            <select value={form.termMonths} onChange={(e) => update('termMonths', Number(e.target.value))} style={styles.input}>
              {[120, 240, 360, 480].filter((m) => m >= product.minTermMonths && m <= product.maxTermMonths).map((m) => (
                <option key={m} value={m}>{m / 12}년</option>
              ))}
            </select>
          </Field>
          <Field label="상환 방식">
            <select value={form.repaymentMethod} onChange={(e) => update('repaymentMethod', e.target.value as typeof form.repaymentMethod)} style={styles.input}>
              <option value="EQUAL_INSTALLMENT">원리금균등</option>
              <option value="EQUAL_PRINCIPAL">원금균등</option>
              <option value="BULLET">만기일시</option>
            </select>
          </Field>
          <Field label="대출 목적">
            <select value={form.loanPurpose} onChange={(e) => update('loanPurpose', e.target.value as typeof form.loanPurpose)} style={styles.input}>
              <option value="PURCHASE">주택 구입</option>
              <option value="LIVING">생활안정</option>
            </select>
          </Field>
          <Field label="현재 보유 주택 수 (담보 주택 구입 전)">
            <input type="number" min={0} value={form.houseCount} onChange={(e) => update('houseCount', Number(e.target.value))} style={styles.input} />
          </Field>
          <label style={styles.checkboxLabel}>
            <input type="checkbox" checked={form.firstHomeBuyer} onChange={(e) => update('firstHomeBuyer', e.target.checked)} />
            생애최초 주택 구입자입니다
          </label>

          <p style={styles.sectionTitle}>담보 주택</p>
          <Field label="주소">
            <input value={form.address} onChange={(e) => update('address', e.target.value)} placeholder="서울시 ○○구 ○○로 1" style={styles.input} />
          </Field>
          <Field label="소재지 규제 여부">
            <select value={form.regionCode} onChange={(e) => update('regionCode', e.target.value as typeof form.regionCode)} style={styles.input}>
              <option value="NON_REGULATED">비규제지역</option>
              <option value="REGULATED">규제지역 (투기·조정대상지역)</option>
            </select>
          </Field>
          <Field label="주택 유형">
            <select value={form.propertyType} onChange={(e) => update('propertyType', e.target.value as typeof form.propertyType)} style={styles.input}>
              <option value="APARTMENT">아파트</option>
              <option value="VILLA">연립·다세대</option>
              <option value="DETACHED">단독주택</option>
              <option value="OFFICETEL">주거용 오피스텔</option>
            </select>
          </Field>
          <Field label="전용면적 (㎡)">
            <input type="number" value={form.exclusiveArea} onChange={(e) => update('exclusiveArea', Number(e.target.value))} style={styles.input} />
          </Field>
          <Field label="매매가 또는 시세 (원)">
            <input type="number" value={form.declaredPrice} onChange={(e) => update('declaredPrice', Number(e.target.value))} style={styles.input} />
          </Field>

          <p style={styles.sectionTitle}>소득</p>
          <Field label="소득 유형">
            <select value={form.incomeType} onChange={(e) => update('incomeType', e.target.value as typeof form.incomeType)} style={styles.input}>
              <option value="EMPLOYEE">근로소득</option>
              <option value="BUSINESS">사업소득</option>
              <option value="OTHER">기타소득</option>
            </select>
          </Field>
          <Field label="연소득 (원)">
            <input type="number" value={form.annualIncome} onChange={(e) => update('annualIncome', Number(e.target.value))} style={styles.input} />
          </Field>
          <Field label="직장명">
            <input value={form.employerName} onChange={(e) => update('employerName', e.target.value)} style={styles.input} />
          </Field>

          <p style={styles.sectionTitle}>기존 대출 (DSR 산정에 반영)</p>
          {debts.map((d, i) => (
            <div key={i} style={{ border: '1px solid #f0f0f0', borderRadius: '8px', padding: '10px', marginBottom: '8px' }}>
              <select value={d.debtType} onChange={(e) => updateDebt(i, { debtType: e.target.value as MortgageDebtInput['debtType'] })} style={styles.input}>
                <option value="MORTGAGE">주택담보대출</option>
                <option value="CREDIT">신용대출</option>
                <option value="JEONSE">전세자금대출</option>
                <option value="OTHER">기타대출</option>
              </select>
              <input placeholder="금융기관" value={d.institution} onChange={(e) => updateDebt(i, { institution: e.target.value })} style={{ ...styles.input, marginTop: '6px' }} />
              <input type="number" placeholder="잔액" value={d.balance || ''} onChange={(e) => updateDebt(i, { balance: Number(e.target.value) })} style={{ ...styles.input, marginTop: '6px' }} />
              <input type="number" placeholder="연간 원금 상환액" value={d.annualPrincipal || ''} onChange={(e) => updateDebt(i, { annualPrincipal: Number(e.target.value) })} style={{ ...styles.input, marginTop: '6px' }} />
              <input type="number" placeholder="연간 이자" value={d.annualInterest || ''} onChange={(e) => updateDebt(i, { annualInterest: Number(e.target.value) })} style={{ ...styles.input, marginTop: '6px' }} />
              <button onClick={() => setDebts((prev) => prev.filter((_, j) => j !== i))} style={styles.subButton}>삭제</button>
            </div>
          ))}
          <button onClick={() => setDebts((prev) => [...prev, emptyDebt])} style={styles.subButton}>+ 기존 대출 추가</button>

          {error && <p style={{ ...styles.error, marginTop: '12px' }}>{error}</p>}
          <button onClick={handleApply} disabled={loading} style={{ ...styles.button, opacity: loading ? 0.7 : 1 }}>
            {loading ? '신청 중...' : '신청하기'}
          </button>
        </div>
      </div>
    )
  }

  return (
    <div style={styles.container}>
      <div style={styles.card}>
        <button onClick={() => navigate(path.home)} style={styles.backButton}>← 홈</button>
        <h2 style={styles.cardTitle}>주택담보대출</h2>
        <p style={styles.cardSub}>신청 후 담보 평가·한도 산정·심사·승인을 거쳐 약정하면 대출금이 입금됩니다.</p>
        {error && <p style={styles.error}>{error}</p>}
        {products.map((p) => (
          <button key={p.productCode} onClick={() => { setProduct(p); setScreen('apply') }} style={styles.listItem}>
            <div style={{ fontWeight: 'bold', marginBottom: '6px' }}>{p.productName}</div>
            <div style={{ display: 'flex', gap: '6px', marginBottom: '6px' }}>
              <span style={styles.chip}>기준금리 연 {p.baseRate}%~</span>
              <span style={styles.chip}>최대 {(p.maxAmount / 100_000_000).toLocaleString()}억</span>
            </div>
            <div style={{ fontSize: '12px', color: '#888' }}>{p.description}</div>
          </button>
        ))}
        <button onClick={() => navigate(path.mortgageApplications)} style={styles.subButton}>내 주담대 보기</button>
      </div>
    </div>
  )
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div style={styles.fieldGroup}>
      <label style={styles.label}>{label}</label>
      {children}
    </div>
  )
}
