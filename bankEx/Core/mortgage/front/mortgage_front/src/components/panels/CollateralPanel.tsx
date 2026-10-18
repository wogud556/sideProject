import { useState } from 'react'
import { evaluateCollateral, errorMessage, won, LIEN_LABEL } from '../../api/mortgage_api'
import type { LienType, ValuationMethod } from '../../api/mortgage_api'
import { Card, ErrorText, Field } from '../ui'
import { buttonStyle, inputStyle, tableStyle, tdStyle, thStyle } from '../styles'
import type { PanelProps } from './types'

const EDITABLE = ['APPLIED', 'COLLATERAL_EVALUATED', 'LIMIT_CALCULATED', 'DOCUMENT_REQUIRED']

interface ClaimRow { lienType: LienType; creditor: string; amount: string }

export default function CollateralPanel({ detail, onUpdated }: PanelProps) {
  const [method, setMethod] = useState<ValuationMethod>('MARKET_PRICE')
  const [value, setValue] = useState(String(detail.property.declaredPrice))
  const [claims, setClaims] = useState<ClaimRow[]>([])
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const editable = EDITABLE.includes(detail.application.status)

  const submit = async () => {
    setBusy(true)
    setError('')
    try {
      const res = await evaluateCollateral(detail.application.applicationId, {
        valuationMethod: method,
        appraisedValue: Number(value),
        seniorClaims: claims.map((c, i) => ({ lienType: c.lienType, creditor: c.creditor, amount: Number(c.amount), lienRank: i + 1 })),
      })
      onUpdated(res.data)
      setClaims([])
    } catch (e) {
      setError(errorMessage(e))
    } finally {
      setBusy(false)
    }
  }

  const updateClaim = (i: number, patch: Partial<ClaimRow>) =>
    setClaims(prev => prev.map((c, j) => (j === i ? { ...c, ...patch } : c)))

  return (
    <Card title="1. 담보 평가 · 권리관계">
      {detail.valuations.length > 0 && (
        <table style={tableStyle}>
          <thead><tr><th style={thStyle}>평가일</th><th style={thStyle}>방법</th><th style={{ ...thStyle, textAlign: 'right' }}>평가액</th><th style={thStyle}>담당</th></tr></thead>
          <tbody>
            {detail.valuations.map((v, i) => (
              <tr key={v.valuationId} style={{ color: i === 0 ? undefined : '#9aa0a6' }}>
                <td style={tdStyle}>{v.valuationDate}{i === 0 && ' (적용)'}</td>
                <td style={tdStyle}>{v.valuationMethod === 'APPRAISAL' ? '감정평가' : '시세 조회'}</td>
                <td style={{ ...tdStyle, textAlign: 'right' }}>{won(v.appraisedValue)}</td>
                <td style={tdStyle}>{v.valuedBy}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
      {detail.liens.length > 0 && (
        <table style={{ ...tableStyle, marginTop: 12 }}>
          <thead><tr><th style={thStyle}>순위</th><th style={thStyle}>구분</th><th style={thStyle}>권리자</th><th style={{ ...thStyle, textAlign: 'right' }}>금액</th></tr></thead>
          <tbody>
            {detail.liens.map(l => (
              <tr key={l.lienId}>
                <td style={tdStyle}>{l.lienRank}</td>
                <td style={tdStyle}>{LIEN_LABEL[l.lienType]}</td>
                <td style={tdStyle}>{l.creditor}</td>
                <td style={{ ...tdStyle, textAlign: 'right' }}>{won(l.amount)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {editable && (
        <div style={{ borderTop: '1px dashed #dadce0', marginTop: 14, paddingTop: 4 }}>
          <p style={{ fontSize: 13, color: '#5f6368' }}>
            여신 실무 담당. {detail.valuations.length > 0 && '재평가하면 한도를 다시 산정해야 합니다. '}선순위 목록은 입력한 내용으로 통째로 바뀝니다.
          </p>
          <div style={{ display: 'flex', gap: 12 }}>
            <Field label="평가 방법">
              <select value={method} onChange={e => setMethod(e.target.value as ValuationMethod)} style={inputStyle}>
                <option value="MARKET_PRICE">시세 조회</option>
                <option value="APPRAISAL">감정평가</option>
              </select>
            </Field>
            <Field label="평가액 (원)">
              <input type="number" value={value} onChange={e => setValue(e.target.value)} style={inputStyle} />
            </Field>
          </div>
          {claims.map((c, i) => (
            <div key={i} style={{ display: 'flex', gap: 8, alignItems: 'flex-end' }}>
              <Field label={`선순위 ${i + 1}`}>
                <select value={c.lienType} onChange={e => updateClaim(i, { lienType: e.target.value as LienType })} style={inputStyle}>
                  <option value="SENIOR_MORTGAGE">선순위 근저당</option>
                  <option value="LEASE_DEPOSIT">임차보증금</option>
                </select>
              </Field>
              <Field label="권리자"><input value={c.creditor} onChange={e => updateClaim(i, { creditor: e.target.value })} style={inputStyle} /></Field>
              <Field label="채권최고액·보증금"><input type="number" value={c.amount} onChange={e => updateClaim(i, { amount: e.target.value })} style={inputStyle} /></Field>
              <button onClick={() => setClaims(prev => prev.filter((_, j) => j !== i))} style={{ ...inputStyle, cursor: 'pointer' }}>삭제</button>
            </div>
          ))}
          <div style={{ display: 'flex', gap: 8 }}>
            <button onClick={() => setClaims(prev => [...prev, { lienType: 'SENIOR_MORTGAGE', creditor: '', amount: '' }])}
              style={buttonStyle('#5f6368')}>선순위 추가</button>
            <button disabled={busy} onClick={submit} style={buttonStyle('#1a73e8', busy)}>담보 평가 등록</button>
          </div>
          <ErrorText message={error} />
        </div>
      )}
    </Card>
  )
}
