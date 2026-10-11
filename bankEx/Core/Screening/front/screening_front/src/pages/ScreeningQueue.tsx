import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  getScreenings, errorMessage, STATUS_LABEL, STATUS_COLOR, LOAN_TYPE_LABEL,
} from '../api/screening_api'
import type { ScreeningResponse, ScreeningStatus } from '../api/screening_api'
import { useReviewerStore } from '../stores/reviewerStore'

const FILTERS: { label: string; status?: ScreeningStatus }[] = [
  { label: '수동심사 대기', status: 'MANUAL_REVIEW' },
  { label: '승인', status: 'APPROVED' },
  { label: '거절', status: 'REJECTED' },
  { label: '전체' },
]

export default function ScreeningQueue() {
  const navigate = useNavigate()
  const { reviewerId, setReviewerId } = useReviewerStore()
  const [filter, setFilter] = useState<ScreeningStatus | undefined>('MANUAL_REVIEW')
  const [screenings, setScreenings] = useState<ScreeningResponse[]>([])
  const [error, setError] = useState('')

  useEffect(() => {
    getScreenings(filter)
      .then(res => { setScreenings(res.data); setError('') })
      .catch(e => setError(errorMessage(e)))
  }, [filter])

  return (
    <div style={{ maxWidth: 960, margin: '40px auto', padding: '0 20px' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h2 style={{ margin: 0 }}>대출 심사 관리</h2>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <label style={{ fontSize: 14, color: '#666' }}>
            심사역 ID{' '}
            <input value={reviewerId} onChange={e => setReviewerId(e.target.value)} style={inputStyle} />
          </label>
          <button style={btnStyle('#1a73e8')} onClick={() => navigate('/screenings/new')}>심사 요청 등록</button>
        </div>
      </div>

      <div style={{ display: 'flex', gap: 8, margin: '24px 0 16px' }}>
        {FILTERS.map(f => (
          <button
            key={f.label}
            onClick={() => setFilter(f.status)}
            style={tabStyle(filter === f.status)}
          >
            {f.label}
          </button>
        ))}
      </div>

      {error && <p style={{ color: '#ea4335' }}>{error}</p>}

      {screenings.length === 0 ? (
        <p style={{ color: '#999' }}>해당하는 심사 건이 없습니다.</p>
      ) : (
        <table style={tableStyle}>
          <thead>
            <tr style={{ background: '#f5f7fa' }}>
              {['심사번호', '고객', '대출구분', '신청금액', '신용점수', '연소득', '상태', '사유', '접수시각'].map(h => (
                <th key={h} style={thStyle}>{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {screenings.map(s => (
              <tr key={s.screeningId} onClick={() => navigate(`/screenings/${s.screeningId}`)} style={{ cursor: 'pointer' }}>
                <td style={tdStyle}>{s.screeningId}</td>
                <td style={tdStyle}>{s.customerId}</td>
                <td style={tdStyle}>{LOAN_TYPE_LABEL[s.loanType]}</td>
                <td style={{ ...tdStyle, textAlign: 'right' }}>{s.requestedAmount.toLocaleString()}원</td>
                <td style={{ ...tdStyle, textAlign: 'right' }}>{s.creditScore}</td>
                <td style={{ ...tdStyle, textAlign: 'right' }}>{s.annualIncome.toLocaleString()}원</td>
                <td style={tdStyle}><span style={badgeStyle(s.status)}>{STATUS_LABEL[s.status]}</span></td>
                <td style={{ ...tdStyle, color: '#666', fontSize: 13 }}>{s.reasonMessage}</td>
                <td style={{ ...tdStyle, fontSize: 13 }}>{new Date(s.screenedAt).toLocaleString()}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}

const inputStyle: React.CSSProperties = { padding: '6px 10px', border: '1px solid #ddd', borderRadius: 6, width: 120 }
const tableStyle: React.CSSProperties = {
  width: '100%', borderCollapse: 'collapse', background: '#fff',
  boxShadow: '0 1px 6px rgba(0,0,0,0.08)', borderRadius: 10, overflow: 'hidden',
}
const thStyle: React.CSSProperties = { padding: '10px 12px', fontSize: 13, color: '#555', textAlign: 'left', whiteSpace: 'nowrap' }
const tdStyle: React.CSSProperties = { padding: '10px 12px', borderTop: '1px solid #eee', fontSize: 14 }
function tabStyle(active: boolean): React.CSSProperties {
  return {
    padding: '6px 16px', borderRadius: 20, cursor: 'pointer', fontSize: 14,
    border: active ? '1px solid #1a73e8' : '1px solid #ddd',
    background: active ? '#e8f0fe' : '#fff', color: active ? '#1a73e8' : '#555',
  }
}
function btnStyle(bg: string): React.CSSProperties {
  return { padding: '8px 16px', background: bg, color: '#fff', border: 'none', borderRadius: 6, cursor: 'pointer', fontSize: 14 }
}
function badgeStyle(status: ScreeningStatus): React.CSSProperties {
  return {
    padding: '3px 10px', borderRadius: 20, fontSize: 12, fontWeight: 600, whiteSpace: 'nowrap',
    background: STATUS_COLOR[status] + '22', color: STATUS_COLOR[status],
  }
}
