import { useState } from 'react'
import { calculateLimit, errorMessage, won, pct, CONSTRAINT_LABEL } from '../../api/mortgage_api'
import type { LimitResult } from '../../api/mortgage_api'
import { Card, ErrorText, Field, Rows } from '../ui'
import { buttonStyle, inputStyle, tableStyle, tdStyle, thStyle } from '../styles'
import type { PanelProps } from './types'

const EDITABLE = ['COLLATERAL_EVALUATED', 'LIMIT_CALCULATED', 'DOCUMENT_REQUIRED']

export default function LimitPanel({ detail, onUpdated }: PanelProps) {
  const [creditScore, setCreditScore] = useState(String(detail.limit?.creditScore ?? 800))
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const limit = detail.limit
  const editable = EDITABLE.includes(detail.application.status)

  const submit = async () => {
    setBusy(true)
    setError('')
    try {
      onUpdated((await calculateLimit(detail.application.applicationId, Number(creditScore))).data)
    } catch (e) {
      setError(errorMessage(e))
    } finally {
      setBusy(false)
    }
  }

  return (
    <Card title="2. 한도 산정 (LTV · DTI · DSR)">
      {limit && <LimitBreakdown limit={limit} />}
      {editable && (
        <div style={{ borderTop: limit ? '1px dashed #dadce0' : undefined, marginTop: limit ? 14 : 0 }}>
          <p style={{ fontSize: 13, color: '#5f6368' }}>심사역 담당. 기준일은 오늘이며, 그날 유효한 규제 정책이 적용됩니다.</p>
          <Field label="신용점수 (신용조회 결과)">
            <input type="number" min={0} max={1000} value={creditScore} onChange={e => setCreditScore(e.target.value)} style={{ ...inputStyle, width: 160 }} />
          </Field>
          <button disabled={busy} onClick={submit} style={buttonStyle('#1a73e8', busy)}>{limit ? '한도 재산정' : '한도 산정'}</button>
          <ErrorText message={error} />
        </div>
      )}
    </Card>
  )
}

function LimitBreakdown({ limit }: { limit: LimitResult }) {
  const rows: [LimitResult['bindingConstraint'], string, number, string][] = [
    ['COLLATERAL', '담보가용액', limit.collateralLimit,
      `평가액 ${won(limit.collateralValue)} × LTV ${pct(limit.ltvPercent)} = ${won(limit.ltvLimit)} − 선순위 ${won(limit.seniorClaims)}`],
    ['DTI', 'DTI 한도', limit.dtiLimit, `연소득 × ${pct(limit.dtiPercent)} 에서 기존 부담 차감 후 역산`],
    ['STRESS_DSR', '스트레스 DSR 한도', limit.stressDsrLimit,
      `연소득 × ${pct(limit.dsrPercent)} − 기존 원리금 ${won(limit.existingDebtService)}, 금리 ${limit.appliedRate}% + ${limit.stressRate}%p (가산 전 DSR 한도 ${won(limit.dsrLimit)})`],
    ['PRODUCT', '상품 한도', limit.productLimit, ''],
    ['REGULATORY', '규제 최대 금액', limit.regulatoryLimit, ''],
  ]
  return (
    <>
      <Rows rows={[
        ['기준일 / 지역 / 차주 유형', `${limit.baseDate} / ${limit.regionCode} / ${limit.borrowerType}`],
        ['신용점수 / 적용 금리', `${limit.creditScore}점 / 연 ${limit.appliedRate}%`],
        ['최종 한도', <span style={{ color: '#174ea6', fontSize: 16 }}>{won(limit.finalLimit)}</span>],
        ['결정 제약', CONSTRAINT_LABEL[limit.bindingConstraint]],
        ['신청액 기준 LTV / DTI / DSR / 스트레스 DSR',
          `${pct(limit.requestedLtv)} / ${pct(limit.requestedDti)} / ${pct(limit.requestedDsr)} / ${pct(limit.requestedStressDsr)}`],
      ]} />
      <table style={{ ...tableStyle, marginTop: 12 }}>
        <thead><tr><th style={thStyle}>제약</th><th style={{ ...thStyle, textAlign: 'right' }}>한도</th><th style={thStyle}>근거</th></tr></thead>
        <tbody>
          {rows.map(([key, label, amount, basis]) => (
            <tr key={key} style={{ background: key === limit.bindingConstraint ? '#e8f0fe' : undefined }}>
              <td style={tdStyle}>{label}</td>
              <td style={{ ...tdStyle, textAlign: 'right', fontWeight: 600 }}>{won(amount)}</td>
              <td style={{ ...tdStyle, fontSize: 12, color: '#5f6368' }}>{basis}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <p style={{ fontSize: 12, color: '#9aa0a6' }}>
        적용 정책 ID — LTV #{limit.ltvPolicyId}, DTI #{limit.dtiPolicyId}, DSR #{limit.dsrPolicyId},
        스트레스 #{limit.stressPolicyId}, 최대 금액 #{limit.maxAmountPolicyId} · 산정 {limit.calculatedBy}
      </p>
    </>
  )
}
