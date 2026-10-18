import { useState } from 'react'
import { approve, confirmPreconditions, errorMessage, won } from '../../api/mortgage_api'
import { Card, ErrorText, Field, Rows } from '../ui'
import { buttonStyle, inputStyle } from '../styles'
import type { PanelProps } from './types'

export default function ApprovalPanel({ detail, onUpdated }: PanelProps) {
  const [amount, setAmount] = useState('')
  const [comment, setComment] = useState('')
  const [conditionsFulfilled, setConditionsFulfilled] = useState(false)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const { status } = detail.application
  const { approval, agreement } = detail
  const recommended = detail.screenings[0]?.recommendedAmount

  const run = async (action: () => Promise<{ data: typeof detail }>) => {
    setBusy(true)
    setError('')
    try {
      onUpdated((await action()).data)
    } catch (e) {
      setError(errorMessage(e))
    } finally {
      setBusy(false)
    }
  }

  return (
    <Card title="4. 승인 · 약정 · 선행조건">
      {approval && (
        <Rows rows={[
          ['승인 금액 / 금리 / 기간', `${won(approval.approvedAmount)} / 연 ${approval.interestRate}% / ${approval.termMonths}개월`],
          ['승인 조건', approval.conditions ?? '없음'],
          ['조건 이행', approval.conditionsFulfilled ? `확인${approval.conditionsConfirmedBy ? ` (${approval.conditionsConfirmedBy})` : ''}` : '미확인'],
          ['승인권자 / 유효기간', `${approval.approverId} / ${approval.expiresOn}까지`],
        ]} />
      )}
      {agreement && (
        <div style={{ marginTop: 10 }}>
          <Rows rows={[
            ['약정번호 / 약정일', `${agreement.agreementNo} / ${new Date(agreement.agreedAt).toLocaleString()}`],
            ['실행금 입금 계좌', agreement.disbursementAccountNo],
            ['당행 근저당 설정', agreement.lienId ? `확인 (${agreement.lienConfirmedBy})` : '미확인'],
          ]} />
        </div>
      )}

      {(status === 'SCREENING_APPROVED' || status === 'CONDITIONALLY_APPROVED') && (
        <div>
          <p style={{ fontSize: 13, color: '#5f6368' }}>승인권자 담당. 심사역과 같은 사람이 승인할 수 없습니다.</p>
          <Field label={`승인 금액 (비우면 권고 금액 ${won(recommended)})`}>
            <input type="number" value={amount} onChange={e => setAmount(e.target.value)} style={{ ...inputStyle, width: 220 }} />
          </Field>
          <Field label="승인 의견">
            <textarea rows={2} value={comment} onChange={e => setComment(e.target.value)} style={inputStyle} />
          </Field>
          <button disabled={busy} style={buttonStyle('#188038', busy)} onClick={() => run(() => approve(
            detail.application.applicationId, { approvedAmount: amount ? Number(amount) : undefined, comment: comment || undefined }))}>
            대출 승인
          </button>
        </div>
      )}

      {status === 'APPROVED' && <p style={{ fontSize: 13, color: '#5f6368' }}>고객 약정 대기 중입니다 (bankEx 앱에서 진행).</p>}

      {status === 'AGREED' && (
        <div>
          <p style={{ fontSize: 13, color: '#5f6368' }}>
            여신 실무 담당. 당행 근저당(채권최고액 = 대출금 × 120%)을 등록하고, 조건부 승인이면 조건 이행을 확인합니다.
          </p>
          {approval && !approval.conditionsFulfilled && (
            <label style={{ display: 'block', fontSize: 14 }}>
              <input type="checkbox" checked={conditionsFulfilled} onChange={e => setConditionsFulfilled(e.target.checked)} /> 승인 조건 이행 확인
            </label>
          )}
          <button disabled={busy} style={buttonStyle('#1a73e8', busy)}
            onClick={() => run(() => confirmPreconditions(detail.application.applicationId, conditionsFulfilled))}>
            선행조건 확인
          </button>
        </div>
      )}
      <ErrorText message={error} />
    </Card>
  )
}
