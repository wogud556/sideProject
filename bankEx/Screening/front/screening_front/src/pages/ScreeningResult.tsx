import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { getScreeningResult, ScreeningResponse } from '../api/screening_api'

const STATUS_LABEL: Record<string, string> = {
  APPROVED: '승인',
  CONDITIONAL: '조건부 승인',
  REJECTED: '반려',
}

const STATUS_COLOR: Record<string, string> = {
  APPROVED: '#34a853',
  CONDITIONAL: '#fbbc05',
  REJECTED: '#ea4335',
}

export default function ScreeningResult() {
  const { applicationId } = useParams<{ applicationId: string }>()
  const navigate = useNavigate()
  const [result, setResult] = useState<ScreeningResponse | null>(null)

  useEffect(() => {
    if (applicationId) {
      getScreeningResult(Number(applicationId)).then(res => setResult(res.data))
    }
  }, [applicationId])

  if (!result) return <p style={{ textAlign: 'center', marginTop: 80 }}>결과 불러오는 중...</p>

  const color = STATUS_COLOR[result.resultStatus] ?? '#666'

  return (
    <div style={{ maxWidth: 520, margin: '60px auto', padding: '0 20px', textAlign: 'center' }}>
      <h2>심사 결과</h2>
      <div style={{ ...resultBox, borderTop: `4px solid ${color}` }}>
        <p style={{ fontSize: 28, fontWeight: 700, color, margin: '0 0 8px' }}>
          {STATUS_LABEL[result.resultStatus] ?? result.resultStatus}
        </p>
        {result.rejectReason && (
          <p style={{ color: '#ea4335', marginBottom: 16 }}>사유: {result.rejectReason}</p>
        )}
        <table style={tableStyle}>
          <tbody>
            <Row label="CSS 점수" value={`${result.cssScore}점`} />
            <Row label="DSR" value={`${result.dsrRate}%`} />
            {result.resultStatus !== 'REJECTED' && (
              <>
                <Row label="승인 금액" value={`${result.approvedAmount.toLocaleString()}원`} />
                <Row label="적용 금리" value={`${result.interestRate}%`} />
              </>
            )}
          </tbody>
        </table>
      </div>
      <div style={{ display: 'flex', gap: 12, justifyContent: 'center', marginTop: 24 }}>
        <button style={btnStyle('#1a73e8')} onClick={() => navigate('/products')}>상품 목록으로</button>
        <button style={btnStyle('#666')} onClick={() => navigate('/my-applications')}>내 신청 목록</button>
      </div>
    </div>
  )
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <tr>
      <td style={{ color: '#666', padding: '6px 0', textAlign: 'left', width: '50%' }}>{label}</td>
      <td style={{ fontWeight: 600, textAlign: 'right' }}>{value}</td>
    </tr>
  )
}

const resultBox: React.CSSProperties = {
  background: '#fff', borderRadius: 12, padding: '28px 32px',
  boxShadow: '0 2px 12px rgba(0,0,0,0.1)', marginTop: 24,
}
const tableStyle: React.CSSProperties = { width: '100%', borderCollapse: 'collapse', marginTop: 16 }
function btnStyle(bg: string): React.CSSProperties {
  return { padding: '11px 22px', background: bg, color: '#fff', border: 'none', borderRadius: 8, cursor: 'pointer', fontSize: 15 }
}
