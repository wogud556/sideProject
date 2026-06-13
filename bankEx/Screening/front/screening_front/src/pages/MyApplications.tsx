import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { getMyApplications } from '../api/screening_api'
import { useAuthStore } from '../stores/authStore'
import { useMyApplicationStore } from '../stores/myApplicationStore'

const STATUS_KO: Record<string, string> = {
  APPLIED: '신청',
  SCREENING: '심사중',
  APPROVED: '승인',
  CONDITIONAL: '조건부승인',
  REJECTED: '반려',
  EXECUTED: '실행완료',
}

const STATUS_COLOR: Record<string, string> = {
  APPROVED: '#34a853', CONDITIONAL: '#fbbc05', REJECTED: '#ea4335',
  APPLIED: '#999', SCREENING: '#1a73e8', EXECUTED: '#0f9d58',
}

export default function MyApplications() {
  const navigate = useNavigate()
  const { userId, isLoggedIn } = useAuthStore()
  const { applications, setApplications } = useMyApplicationStore()

  useEffect(() => {
    if (!isLoggedIn) { navigate('/login'); return }
    getMyApplications(userId!).then(res => setApplications(res.data))
  }, [isLoggedIn, userId, navigate, setApplications])

  return (
    <div style={{ maxWidth: 700, margin: '40px auto', padding: '0 20px' }}>
      <h2>내 대출 신청 목록</h2>
      {applications.length === 0 ? (
        <p style={{ color: '#999' }}>신청 내역이 없습니다.</p>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          {applications.map(a => (
            <div key={a.applicationId} style={cardStyle}>
              <div>
                <p style={{ margin: 0, fontWeight: 600 }}>신청번호: {a.applicationId}</p>
                <p style={{ margin: '4px 0 0', color: '#666' }}>
                  신청금액: {a.requestAmount.toLocaleString()}원 &nbsp;|&nbsp;
                  {new Date(a.createdAt).toLocaleDateString()}
                </p>
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                <span style={badgeStyle(a.applicationStatus)}>
                  {STATUS_KO[a.applicationStatus] ?? a.applicationStatus}
                </span>
                {['APPROVED', 'CONDITIONAL', 'REJECTED'].includes(a.applicationStatus) && (
                  <button
                    style={resultBtn}
                    onClick={() => navigate(`/result/${a.applicationId}`)}
                  >
                    결과보기
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

const cardStyle: React.CSSProperties = {
  background: '#fff', borderRadius: 10, padding: '16px 20px',
  boxShadow: '0 1px 6px rgba(0,0,0,0.08)', display: 'flex',
  justifyContent: 'space-between', alignItems: 'center',
}
const resultBtn: React.CSSProperties = {
  padding: '6px 14px', background: '#1a73e8', color: '#fff',
  border: 'none', borderRadius: 6, cursor: 'pointer', fontSize: 13,
}
function badgeStyle(status: string): React.CSSProperties {
  return {
    padding: '3px 12px', borderRadius: 20, fontSize: 13, fontWeight: 600,
    background: STATUS_COLOR[status] + '22', color: STATUS_COLOR[status] ?? '#666',
  }
}
