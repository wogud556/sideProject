import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  checkEligibilityApi,
  repaymentInquiryApi,
  applyRefinanceApi,
  getCustomerLoansApi,
  type EligibilityResponse,
  type RepaymentAmountResponse,
  type ExistingLoanResponse,
  type RefinanceApplicationResponse,
} from '../api/refinance_api'
import { useRefinanceWizardStore, type NewLoanCondition } from '../stores/refinanceWizardStore'
import { PATH } from '../router/path'

type Step = 3 | 4 | 5 | 6 | 7

const STEP_LABELS: Record<Step, string> = {
  3: '대환가능여부',
  4: '신규대출조건',
  5: '상환금액확인',
  6: '신청내용확인',
  7: '신청완료',
}

export default function RefinanceWizard() {
  const navigate = useNavigate()
  const { customerId, selectedLoanIds, reset } = useRefinanceWizardStore()

  const [step, setStep] = useState<Step>(3)
  const [loans, setLoans] = useState<ExistingLoanResponse[]>([])
  const [eligibility, setEligibility] = useState<EligibilityResponse | null>(null)
  const [repayments, setRepayments] = useState<RepaymentAmountResponse[]>([])
  const [loading, setLoading] = useState(customerId != null && selectedLoanIds.length > 0)
  const [condition, setCondition] = useState<NewLoanCondition>({
    newLoanProductName: '가계대환신용대출',
    newLoanAmount: 0,
    newLoanRate: 5.5,
    newLoanRateType: '고정금리',
    newLoanPeriodMonths: 36,
    newLoanRepaymentMethod: '원리금균등분할상환',
    newLoanExecutionScheduledDate: new Date().toISOString().slice(0, 10),
    newLoanAccountNo: '',
    refinancePurposeYn: 'Y',
  })
  const [result, setResult] = useState<RefinanceApplicationResponse | null>(null)

  useEffect(() => {
    if (!customerId || selectedLoanIds.length === 0) return
    Promise.all([
      getCustomerLoansApi(customerId),
      checkEligibilityApi(customerId, selectedLoanIds),
    ])
      .then(([allLoans, eligibilityRes]) => {
        setLoans(allLoans.filter((l) => selectedLoanIds.includes(l.loanId)))
        setEligibility(eligibilityRes)
      })
      .finally(() => setLoading(false))
  }, [customerId, selectedLoanIds])

  if (!customerId || selectedLoanIds.length === 0) {
    return (
      <div style={{ padding: 24 }}>
        <p>선택된 고객/대출 정보가 없습니다. 고객 대출현황 화면에서 다시 시작해 주세요.</p>
        <button onClick={() => navigate(PATH.CUSTOMER_SEARCH)} style={buttonStyle}>고객 검색으로 이동</button>
      </div>
    )
  }

  const totalRepayment = repayments.reduce((sum, r) => sum + r.finalRepaymentAmount, 0)

  const goToRepaymentStep = async () => {
    setLoading(true)
    try {
      const res = await repaymentInquiryApi(selectedLoanIds)
      setRepayments(res)
      const total = res.reduce((sum, r) => sum + r.finalRepaymentAmount, 0)
      if (condition.newLoanAmount < total) {
        setCondition((prev) => ({ ...prev, newLoanAmount: total }))
      }
      setStep(5)
    } finally {
      setLoading(false)
    }
  }

  const submit = async () => {
    if (!confirm(`신규대출 ${condition.newLoanAmount.toLocaleString()}원을 실행하고\n기존 대출 ${selectedLoanIds.length}건을 상환합니다.\n\n계속 진행하시겠습니까?`)) {
      return
    }
    setLoading(true)
    try {
      const res = await applyRefinanceApi({
        customerId,
        loanIds: selectedLoanIds,
        ...condition,
      })
      setResult(res)
      setStep(7)
    } catch {
      alert('대환 신청 등록 중 오류가 발생했습니다.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{ maxWidth: 760, margin: '40px auto', padding: 24 }}>
      <h1 style={{ fontSize: 20, marginBottom: 8 }}>대환 신청</h1>
      <StepIndicator current={step} />

      {loading && <p style={{ color: '#888' }}>처리 중...</p>}

      {!loading && step === 3 && eligibility && (
        <div>
          <h2 style={sectionTitle}>STEP 3. 대환가능여부</h2>
          <ul style={{ listStyle: 'none', padding: 0 }}>
            {eligibility.results.map((r, i) => (
              <li key={i} style={{ padding: '6px 0', color: r.passed ? '#1a7d3a' : '#c0392b' }}>
                {r.passed ? '✓' : '✗'} {r.message}
              </li>
            ))}
          </ul>
          <p style={{ fontWeight: 'bold', marginTop: 12 }}>
            종합 판정: {eligibility.eligible ? '대환 가능' : '대환 불가'}
          </p>
          <button disabled={!eligibility.eligible} onClick={() => setStep(4)} style={buttonStyle}>다음</button>
        </div>
      )}

      {!loading && step === 4 && (
        <div>
          <h2 style={sectionTitle}>STEP 4. 신규대출조건</h2>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
            <label style={labelStyle}>신규대출상품
              <input value={condition.newLoanProductName} onChange={(e) => setCondition({ ...condition, newLoanProductName: e.target.value })} style={inputStyle} />
            </label>
            <label style={labelStyle}>대출금액
              <input type="number" value={condition.newLoanAmount} onChange={(e) => setCondition({ ...condition, newLoanAmount: Number(e.target.value) })} style={inputStyle} />
            </label>
            <label style={labelStyle}>금리(%)
              <input type="number" step="0.01" value={condition.newLoanRate} onChange={(e) => setCondition({ ...condition, newLoanRate: Number(e.target.value) })} style={inputStyle} />
            </label>
            <label style={labelStyle}>금리유형
              <select value={condition.newLoanRateType} onChange={(e) => setCondition({ ...condition, newLoanRateType: e.target.value })} style={inputStyle}>
                <option>고정금리</option>
                <option>변동금리</option>
              </select>
            </label>
            <label style={labelStyle}>대출기간(개월)
              <input type="number" value={condition.newLoanPeriodMonths} onChange={(e) => setCondition({ ...condition, newLoanPeriodMonths: Number(e.target.value) })} style={inputStyle} />
            </label>
            <label style={labelStyle}>상환방법
              <select value={condition.newLoanRepaymentMethod} onChange={(e) => setCondition({ ...condition, newLoanRepaymentMethod: e.target.value })} style={inputStyle}>
                <option>원리금균등분할상환</option>
                <option>원금균등분할상환</option>
                <option>만기일시상환</option>
              </select>
            </label>
            <label style={labelStyle}>대출실행예정일
              <input type="date" value={condition.newLoanExecutionScheduledDate} onChange={(e) => setCondition({ ...condition, newLoanExecutionScheduledDate: e.target.value })} style={inputStyle} />
            </label>
            <label style={labelStyle}>대출계좌
              <input placeholder="000-000000-00-000" value={condition.newLoanAccountNo} onChange={(e) => setCondition({ ...condition, newLoanAccountNo: e.target.value })} style={inputStyle} />
            </label>
          </div>
          <div style={{ marginTop: 16, display: 'flex', gap: 8 }}>
            <button onClick={() => setStep(3)} style={secondaryButtonStyle}>이전</button>
            <button
              onClick={goToRepaymentStep}
              disabled={!condition.newLoanAccountNo || condition.newLoanAmount <= 0}
              style={buttonStyle}
            >
              다음
            </button>
          </div>
        </div>
      )}

      {!loading && step === 5 && (
        <div>
          <h2 style={sectionTitle}>STEP 5. 상환금액확인</h2>
          <table style={{ width: '100%', borderCollapse: 'collapse', marginBottom: 12 }}>
            <thead>
              <tr style={{ borderBottom: '2px solid #d8dce3', textAlign: 'left' }}>
                <th style={thStyle}>대출</th>
                <th style={thStyle}>원금잔액</th>
                <th style={thStyle}>발생이자</th>
                <th style={thStyle}>중도상환수수료</th>
                <th style={thStyle}>최종상환예정금액</th>
              </tr>
            </thead>
            <tbody>
              {repayments.map((r) => (
                <tr key={r.loanId} style={{ borderBottom: '1px solid #eee' }}>
                  <td style={tdStyle}>{loans.find((l) => l.loanId === r.loanId)?.loanProductName}</td>
                  <td style={tdStyle}>{r.principalBalance.toLocaleString()}원</td>
                  <td style={tdStyle}>{r.accruedInterest.toLocaleString()}원</td>
                  <td style={tdStyle}>{r.prepaymentFee.toLocaleString()}원</td>
                  <td style={{ ...tdStyle, fontWeight: 'bold' }}>{r.finalRepaymentAmount.toLocaleString()}원</td>
                </tr>
              ))}
            </tbody>
          </table>
          <p style={{ fontWeight: 'bold' }}>상환예정 합계: {totalRepayment.toLocaleString()}원</p>
          <p style={{ color: condition.newLoanAmount >= totalRepayment ? '#1a7d3a' : '#c0392b' }}>
            신규대출금액 {condition.newLoanAmount.toLocaleString()}원 {condition.newLoanAmount >= totalRepayment ? '(충분)' : '(부족 — STEP 4에서 금액을 조정하세요)'}
          </p>
          <div style={{ marginTop: 16, display: 'flex', gap: 8 }}>
            <button onClick={() => setStep(4)} style={secondaryButtonStyle}>이전</button>
            <button disabled={condition.newLoanAmount < totalRepayment} onClick={() => setStep(6)} style={buttonStyle}>다음</button>
          </div>
        </div>
      )}

      {!loading && step === 6 && (
        <div>
          <h2 style={sectionTitle}>STEP 6. 신청내용확인</h2>
          <div style={summaryBox}>
            <p><strong>대환 대상 대출</strong>: {loans.length}건, 상환예정 합계 {totalRepayment.toLocaleString()}원</p>
            <p><strong>신규대출</strong>: {condition.newLoanProductName} / {condition.newLoanAmount.toLocaleString()}원 / {condition.newLoanRate}% ({condition.newLoanRateType}) / {condition.newLoanPeriodMonths}개월 / {condition.newLoanRepaymentMethod}</p>
            <p><strong>대출실행예정일</strong>: {condition.newLoanExecutionScheduledDate}</p>
            <p><strong>입금계좌</strong>: {condition.newLoanAccountNo}</p>
          </div>
          <div style={{ marginTop: 16, display: 'flex', gap: 8 }}>
            <button onClick={() => setStep(5)} style={secondaryButtonStyle}>이전</button>
            <button onClick={submit} style={buttonStyle}>신청 등록</button>
          </div>
        </div>
      )}

      {!loading && step === 7 && result && (
        <div>
          <h2 style={sectionTitle}>STEP 7. 신청완료</h2>
          <p>대환 신청이 접수되었습니다.</p>
          <p style={{ fontSize: 18, fontWeight: 'bold', margin: '12px 0' }}>신청번호: {result.applicationNo}</p>
          <button
            onClick={() => {
              reset()
              navigate(PATH.DASHBOARD)
            }}
            style={buttonStyle}
          >
            대시보드로 이동
          </button>
        </div>
      )}
    </div>
  )
}

function StepIndicator({ current }: { current: Step }) {
  const steps: Step[] = [3, 4, 5, 6, 7]
  return (
    <div style={{ display: 'flex', gap: 4, marginBottom: 20, fontSize: 12, color: '#888' }}>
      <span style={{ color: '#1a7d3a' }}>✓ STEP1 고객확인</span>
      <span>›</span>
      <span style={{ color: '#1a7d3a' }}>✓ STEP2 기존대출선택</span>
      <span>›</span>
      {steps.map((s) => (
        <span key={s} style={{ color: s === current ? '#1a3d8f' : s < current ? '#1a7d3a' : '#aaa', fontWeight: s === current ? 'bold' : 'normal' }}>
          {s < current ? '✓ ' : ''}STEP{s} {STEP_LABELS[s]} {s !== 7 ? '›' : ''}
        </span>
      ))}
    </div>
  )
}

const sectionTitle: React.CSSProperties = { fontSize: 16, marginBottom: 12 }
const labelStyle: React.CSSProperties = { display: 'flex', flexDirection: 'column', fontSize: 13, color: '#555', gap: 4 }
const inputStyle: React.CSSProperties = { padding: '8px 10px', border: '1px solid #d8dce3', borderRadius: 4, fontSize: 14 }
const buttonStyle: React.CSSProperties = { padding: '10px 24px', background: '#1a3d8f', color: '#fff', border: 'none', borderRadius: 4, cursor: 'pointer', fontSize: 14 }
const secondaryButtonStyle: React.CSSProperties = { ...buttonStyle, background: '#fff', color: '#1a3d8f', border: '1px solid #1a3d8f' }
const thStyle: React.CSSProperties = { padding: '8px 6px', fontSize: 13, color: '#555' }
const tdStyle: React.CSSProperties = { padding: '8px 6px', fontSize: 14 }
const summaryBox: React.CSSProperties = { background: '#fff', border: '1px solid #d8dce3', borderRadius: 6, padding: 16, display: 'flex', flexDirection: 'column', gap: 8, fontSize: 14 }
