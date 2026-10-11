import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import {
  getScreening, approveScreening, rejectScreening, errorMessage,
  STATUS_LABEL, STATUS_COLOR, LOAN_TYPE_LABEL,
} from '../api/screening_api'
import type { ScreeningResponse } from '../api/screening_api'
import { useReviewerStore } from '../stores/reviewerStore'

export default function ScreeningDetail() {
  const { screeningId } = useParams<{ screeningId: string }>()
  const navigate = useNavigate()
  const { reviewerId } = useReviewerStore()
  const [screening, setScreening] = useState<ScreeningResponse | null>(null)
  const [approvedAmount, setApprovedAmount] = useState('')
  const [comment, setComment] = useState('')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    getScreening(Number(screeningId))
      .then(res => setScreening(res.data))
      .catch(e => setError(errorMessage(e)))
  }, [screeningId])

  const decide = async (action: 'approve' | 'reject') => {
    if (!screening) return
    setSubmitting(true)
    setError('')
    try {
      const body = {
        reviewerId,
        comment: comment || undefined,
        approvedAmount: action === 'approve' && approvedAmount ? Number(approvedAmount) : undefined,
      }
      const res = action === 'approve'
        ? await approveScreening(screening.screeningId, body)
        : await rejectScreening(screening.screeningId, body)
      setScreening(res.data)
    } catch (e) {
      setError(errorMessage(e))
    } finally {
      setSubmitting(false)
    }
  }

  if (!screening) {
    return <p style={{ textAlign: 'center', marginTop: 80, color: error ? '#ea4335' : undefined }}>{error || '불러오는 중...'}</p>
  }

  const color = STATUS_COLOR[screening.status]
  const won = (n: number | null) => (n == null ? '-' : `${n.toLocaleString()}원`)

  return (
    <div style={{ maxWidth: 640, margin: '40px auto', padding: '0 20px' }}>
      <button style={linkBtn} onClick={() => navigate('/')}>← 목록으로</button>
      <h2>심사 #{screening.screeningId}</h2>

      <section style={{ ...cardStyle, borderTop: `4px solid ${color}` }}>
        <p style={{ fontSize: 22, fontWeight: 700, color, margin: '0 0 4px' }}>{STATUS_LABEL[screening.status]}</p>
        <p style={{ color: '#666', margin: 0 }}>{screening.reasonMessage}</p>
      </section>

      <section style={cardStyle}>
        <h3 style={sectionTitle}>심사 요청</h3>
        <table style={tableStyle}>
          <tbody>
            <Row label="고객 ID" value={screening.customerId} />
            <Row label="대출 구분 / 상품" value={`${LOAN_TYPE_LABEL[screening.loanType]} / ${screening.loanProductId}`} />
            <Row label="신청 금액" value={won(screening.requestedAmount)} />
            <Row label="연소득" value={won(screening.annualIncome)} />
            <Row label="신용점수" value={`${screening.creditScore}점`} />
            <Row label="기존 대출금" value={won(screening.existingLoanAmount)} />
            <Row label="접수 시각" value={new Date(screening.screenedAt).toLocaleString()} />
          </tbody>
        </table>
      </section>

      {screening.status === 'APPROVED' && (
        <section style={cardStyle}>
          <h3 style={sectionTitle}>승인 조건</h3>
          <table style={tableStyle}>
            <tbody>
              <Row label="승인 금액" value={won(screening.approvedAmount)} />
              <Row label="적용 금리" value={`${screening.approvedInterestRate}%`} />
            </tbody>
          </table>
        </section>
      )}

      {screening.reviewerId && (
        <section style={cardStyle}>
          <h3 style={sectionTitle}>심사역 결정</h3>
          <table style={tableStyle}>
            <tbody>
              <Row label="심사역" value={screening.reviewerId} />
              <Row label="결정 시각" value={new Date(screening.reviewedAt!).toLocaleString()} />
              <Row label="의견" value={screening.reviewComment ?? '-'} />
            </tbody>
          </table>
        </section>
      )}

      {screening.status === 'MANUAL_REVIEW' && (
        <section style={cardStyle}>
          <h3 style={sectionTitle}>심사역 결정 ({reviewerId})</h3>
          <label style={labelStyle}>
            승인 금액 (비우면 신청 금액 전액)
            <input
              type="number" min={1} max={screening.requestedAmount}
              value={approvedAmount} onChange={e => setApprovedAmount(e.target.value)}
              placeholder={String(screening.requestedAmount)} style={inputStyle}
            />
          </label>
          <label style={labelStyle}>
            심사 의견
            <textarea value={comment} onChange={e => setComment(e.target.value)} rows={3} style={inputStyle} />
          </label>
          <div style={{ display: 'flex', gap: 12, marginTop: 12 }}>
            <button disabled={submitting || !reviewerId} style={btnStyle('#34a853')} onClick={() => decide('approve')}>승인</button>
            <button disabled={submitting || !reviewerId} style={btnStyle('#ea4335')} onClick={() => decide('reject')}>거절</button>
          </div>
          {!reviewerId && <p style={{ color: '#ea4335', fontSize: 13 }}>목록 화면에서 심사역 ID를 입력해 주세요.</p>}
        </section>
      )}

      {error && <p style={{ color: '#ea4335' }}>{error}</p>}
    </div>
  )
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <tr>
      <td style={{ color: '#666', padding: '6px 0', width: '40%' }}>{label}</td>
      <td style={{ fontWeight: 600, textAlign: 'right' }}>{value}</td>
    </tr>
  )
}

const cardStyle: React.CSSProperties = {
  background: '#fff', borderRadius: 12, padding: '20px 24px',
  boxShadow: '0 2px 12px rgba(0,0,0,0.08)', marginTop: 16,
}
const sectionTitle: React.CSSProperties = { margin: '0 0 8px', fontSize: 16 }
const tableStyle: React.CSSProperties = { width: '100%', borderCollapse: 'collapse' }
const labelStyle: React.CSSProperties = { display: 'flex', flexDirection: 'column', gap: 4, fontSize: 14, color: '#555', marginTop: 12 }
const inputStyle: React.CSSProperties = { padding: '8px 10px', border: '1px solid #ddd', borderRadius: 6, fontSize: 14 }
const linkBtn: React.CSSProperties = { background: 'none', border: 'none', color: '#1a73e8', cursor: 'pointer', padding: 0, fontSize: 14 }
function btnStyle(bg: string): React.CSSProperties {
  return { padding: '10px 24px', background: bg, color: '#fff', border: 'none', borderRadius: 8, cursor: 'pointer', fontSize: 15 }
}
