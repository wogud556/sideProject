import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { requestScreening, errorMessage } from '../api/screening_api'
import type { LoanType } from '../api/screening_api'

// bankEx 연동 전까지 심사 요청을 직접 넣어보기 위한 화면
export default function NewScreening() {
  const navigate = useNavigate()
  const [form, setForm] = useState({
    customerId: '',
    loanProductId: '1',
    loanType: 'GENERAL' as LoanType,
    requestedAmount: '',
    annualIncome: '',
    creditScore: '',
    existingLoanAmount: '0',
  })
  const [error, setError] = useState('')

  const set = (key: keyof typeof form) =>
    (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => setForm({ ...form, [key]: e.target.value })

  const submit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    try {
      const res = await requestScreening({
        ...form,
        requestedAmount: Number(form.requestedAmount),
        annualIncome: Number(form.annualIncome),
        creditScore: Number(form.creditScore),
        existingLoanAmount: Number(form.existingLoanAmount),
      })
      navigate(`/screenings/${res.data.screeningId}`)
    } catch (e) {
      setError(errorMessage(e))
    }
  }

  return (
    <div style={{ maxWidth: 480, margin: '40px auto', padding: '0 20px' }}>
      <button style={linkBtn} onClick={() => navigate('/')}>← 목록으로</button>
      <h2>심사 요청 등록</h2>
      <form onSubmit={submit} style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
        <Field label="고객 ID"><input required value={form.customerId} onChange={set('customerId')} style={inputStyle} /></Field>
        <Field label="대출 구분">
          <select value={form.loanType} onChange={set('loanType')} style={inputStyle}>
            <option value="GENERAL">일반대출</option>
            <option value="JEONSE">전세대출</option>
          </select>
        </Field>
        <Field label="상품 ID"><input required value={form.loanProductId} onChange={set('loanProductId')} style={inputStyle} /></Field>
        <Field label="신청 금액 (원)"><input required type="number" min={1} value={form.requestedAmount} onChange={set('requestedAmount')} style={inputStyle} /></Field>
        <Field label="연소득 (원)"><input required type="number" min={0} value={form.annualIncome} onChange={set('annualIncome')} style={inputStyle} /></Field>
        <Field label="신용점수 (0~1000)"><input required type="number" min={0} max={1000} value={form.creditScore} onChange={set('creditScore')} style={inputStyle} /></Field>
        <Field label="기존 대출금 (원)"><input required type="number" min={0} value={form.existingLoanAmount} onChange={set('existingLoanAmount')} style={inputStyle} /></Field>
        {error && <p style={{ color: '#ea4335', margin: 0 }}>{error}</p>}
        <button type="submit" style={submitBtn}>심사 실행</button>
      </form>
    </div>
  )
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <label style={{ display: 'flex', flexDirection: 'column', gap: 4, fontSize: 14, color: '#555' }}>
      {label}
      {children}
    </label>
  )
}

const inputStyle: React.CSSProperties = { padding: '9px 12px', border: '1px solid #ddd', borderRadius: 6, fontSize: 14 }
const linkBtn: React.CSSProperties = { background: 'none', border: 'none', color: '#1a73e8', cursor: 'pointer', padding: 0, fontSize: 14 }
const submitBtn: React.CSSProperties = {
  padding: '12px', background: '#1a73e8', color: '#fff', border: 'none',
  borderRadius: 8, cursor: 'pointer', fontSize: 15, marginTop: 8,
}
