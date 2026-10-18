import { useState } from 'react'
import { screen, errorMessage, won, AREA_COLOR } from '../../api/mortgage_api'
import { Badge, Card, ErrorText, Field } from '../ui'
import { buttonStyle, inputStyle } from '../styles'
import type { PanelProps } from './types'

const EDITABLE = ['LIMIT_CALCULATED', 'DOCUMENT_REQUIRED']

export default function ScreeningPanel({ detail, onUpdated }: PanelProps) {
  const [incomeVerified, setIncomeVerified] = useState(true)
  const [ownershipVerified, setOwnershipVerified] = useState(true)
  const [comment, setComment] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const editable = EDITABLE.includes(detail.application.status)

  const submit = async () => {
    setBusy(true)
    setError('')
    try {
      onUpdated((await screen(detail.application.applicationId, { incomeVerified, ownershipVerified, comment: comment || undefined })).data)
      setComment('')
    } catch (e) {
      setError(errorMessage(e))
    } finally {
      setBusy(false)
    }
  }

  return (
    <Card title="3. 여신 심사">
      {detail.screenings.map((s, i) => (
        <div key={s.screeningId} style={{ padding: '8px 0', borderBottom: '1px solid #f1f3f4', opacity: i === 0 ? 1 : 0.55 }}>
          <div style={{ display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap' }}>
            <strong>{s.statusLabel}</strong>
            <Badge color={AREA_COLOR[s.borrowerResult]}>차주 {s.borrowerResult}</Badge>
            <Badge color={AREA_COLOR[s.collateralResult]}>담보 {s.collateralResult}</Badge>
            <Badge color={AREA_COLOR[s.regulatoryResult]}>규제 {s.regulatoryResult}</Badge>
            <span style={{ fontSize: 12, color: '#5f6368' }}>{s.screenerId} · {new Date(s.screenedAt).toLocaleString()}</span>
          </div>
          {s.reasons.length > 0 && (
            <ul style={{ margin: '6px 0', paddingLeft: 18, fontSize: 13 }}>
              {s.reasons.map(r => <li key={r.code}>{r.message}</li>)}
            </ul>
          )}
          {s.recommendedAmount != null && <div style={{ fontSize: 13 }}>권고 금액 {won(s.recommendedAmount)}</div>}
          {s.screeningComment && <div style={{ fontSize: 13, color: '#5f6368' }}>의견: {s.screeningComment}</div>}
        </div>
      ))}

      {editable && (
        <div>
          <p style={{ fontSize: 13, color: '#5f6368' }}>심사역 담당. 최근 한도 산정 결과(최종 한도 {won(detail.limit?.finalLimit)})를 근거로 판정합니다.</p>
          <label style={{ display: 'block', fontSize: 14 }}>
            <input type="checkbox" checked={incomeVerified} onChange={e => setIncomeVerified(e.target.checked)} /> 소득 증빙 서류 확인
          </label>
          <label style={{ display: 'block', fontSize: 14, marginTop: 6 }}>
            <input type="checkbox" checked={ownershipVerified} onChange={e => setOwnershipVerified(e.target.checked)} /> 등기부등본·소유권 서류 확인
          </label>
          <Field label="심사 의견">
            <textarea rows={2} value={comment} onChange={e => setComment(e.target.value)} style={inputStyle} />
          </Field>
          <button disabled={busy} onClick={submit} style={buttonStyle('#1a73e8', busy)}>심사 실행</button>
          <ErrorText message={error} />
        </div>
      )}
    </Card>
  )
}
