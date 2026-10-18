import { useEffect, useState } from 'react'
import { createPolicy, getPolicies, errorMessage } from '../api/mortgage_api'
import type { Policy, PolicyCreateRequest, PolicyType } from '../api/mortgage_api'
import { Card, ErrorText, Field } from '../components/ui'
import { buttonStyle, inputStyle, tableStyle, tdStyle, thStyle } from '../components/styles'

const TYPES: PolicyType[] = ['LTV', 'DTI', 'DSR', 'STRESS_RATE', 'MAX_LOAN_AMOUNT']
const today = new Date().toISOString().slice(0, 10)

const emptyForm: PolicyCreateRequest = {
  policyType: 'LTV', regionCode: 'ALL', borrowerType: 'ALL', productCode: 'ALL',
  policyValue: 0, effectiveFrom: today, basis: '',
}

export default function PolicyList() {
  const [policies, setPolicies] = useState<Policy[]>([])
  const [form, setForm] = useState<PolicyCreateRequest>(emptyForm)
  const [error, setError] = useState('')

  const load = () => getPolicies().then(res => setPolicies(res.data)).catch(e => setError(errorMessage(e)))
  useEffect(() => { load() }, [])

  const submit = async () => {
    setError('')
    try {
      await createPolicy({ ...form, policyValue: Number(form.policyValue), basis: form.basis || undefined })
      setForm(emptyForm)
      load()
    } catch (e) {
      setError(errorMessage(e))
    }
  }

  const set = (patch: Partial<PolicyCreateRequest>) => setForm(prev => ({ ...prev, ...patch }))
  const isOpen = (p: Policy) => p.effectiveFrom <= today && (p.effectiveTo == null || p.effectiveTo >= today)

  return (
    <>
      <h2>규제 정책</h2>
      <p style={{ color: '#5f6368', fontSize: 14 }}>
        한도 산정은 기준일에 유효한 정책 중 가장 구체적인 행(상품 &gt; 차주 유형 &gt; 지역)을 씁니다.
        정책은 수정하지 않고 새 버전을 등록하며, 같은 조건의 이전 버전은 시작일 전날로 종료됩니다.
      </p>
      <Card>
        <table style={tableStyle}>
          <thead>
            <tr>
              <th style={thStyle}>ID</th><th style={thStyle}>유형</th><th style={thStyle}>지역</th><th style={thStyle}>차주</th>
              <th style={thStyle}>상품</th><th style={{ ...thStyle, textAlign: 'right' }}>값</th><th style={thStyle}>적용기간</th>
              <th style={thStyle}>버전</th><th style={thStyle}>근거</th>
            </tr>
          </thead>
          <tbody>
            {policies.map(p => (
              <tr key={p.policyId} style={{ color: isOpen(p) ? undefined : '#9aa0a6' }}>
                <td style={tdStyle}>#{p.policyId}</td>
                <td style={tdStyle}>{p.policyType}</td>
                <td style={tdStyle}>{p.regionCode}</td>
                <td style={tdStyle}>{p.borrowerType}</td>
                <td style={tdStyle}>{p.productCode}</td>
                <td style={{ ...tdStyle, textAlign: 'right' }}>{p.policyValue.toLocaleString()}</td>
                <td style={tdStyle}>{p.effectiveFrom} ~ {p.effectiveTo ?? ''}</td>
                <td style={tdStyle}>v{p.policyVersion}</td>
                <td style={tdStyle}>{p.basis}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </Card>

      <Card title="새 정책 버전 등록 (승인권자)">
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 12 }}>
          <Field label="유형">
            <select value={form.policyType} onChange={e => set({ policyType: e.target.value as PolicyType })} style={inputStyle}>
              {TYPES.map(t => <option key={t}>{t}</option>)}
            </select>
          </Field>
          <Field label="지역">
            <select value={form.regionCode} onChange={e => set({ regionCode: e.target.value })} style={inputStyle}>
              {['ALL', 'REGULATED', 'NON_REGULATED'].map(v => <option key={v}>{v}</option>)}
            </select>
          </Field>
          <Field label="차주 유형">
            <select value={form.borrowerType} onChange={e => set({ borrowerType: e.target.value })} style={inputStyle}>
              {['ALL', 'FIRST_HOME', 'NO_HOME', 'ONE_HOME', 'MULTI_HOME'].map(v => <option key={v}>{v}</option>)}
            </select>
          </Field>
          <Field label="상품">
            <select value={form.productCode} onChange={e => set({ productCode: e.target.value })} style={inputStyle}>
              {['ALL', 'MG-STD', 'MG-FIRST'].map(v => <option key={v}>{v}</option>)}
            </select>
          </Field>
          <Field label="값 (% · %p · 원)">
            <input type="number" value={form.policyValue} onChange={e => set({ policyValue: Number(e.target.value) })} style={inputStyle} />
          </Field>
          <Field label="적용 시작일">
            <input type="date" value={form.effectiveFrom} onChange={e => set({ effectiveFrom: e.target.value })} style={inputStyle} />
          </Field>
          <Field label="근거">
            <input value={form.basis ?? ''} onChange={e => set({ basis: e.target.value })} style={inputStyle} />
          </Field>
        </div>
        <button onClick={submit} style={buttonStyle('#188038')}>등록</button>
        <ErrorText message={error} />
      </Card>
    </>
  )
}
