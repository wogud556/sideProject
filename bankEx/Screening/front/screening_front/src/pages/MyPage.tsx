import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getProfile, UserProfile } from '../api/screening_api'
import { useAuthStore } from '../stores/authStore'
import { useLoanApplicationStore } from '../stores/loanApplicationStore'
import { useMyApplicationStore } from '../stores/myApplicationStore'

export default function MyPage() {
  const navigate = useNavigate()
  const { userId, clearAuth } = useAuthStore()
  const clearApplication = useLoanApplicationStore(s => s.clear)
  const clearMyApplications = useMyApplicationStore(s => s.clear)
  const [profile, setProfile] = useState<UserProfile | null>(null)

  useEffect(() => {
    if (!userId) { navigate('/login'); return }
    getProfile(userId).then(res => setProfile(res.data))
  }, [userId, navigate])

  const handleLogout = () => {
    clearAuth()
    clearApplication()
    clearMyApplications()
    navigate('/login')
  }

  if (!profile) return <p style={{ textAlign: 'center', marginTop: 80 }}>불러오는 중...</p>

  return (
    <div style={{ maxWidth: 520, margin: '50px auto', padding: '0 20px' }}>
      <h2>내 정보</h2>
      <div style={section}>
        <h3 style={sectionTitle}>기본 정보</h3>
        <Row label="이름" value={profile.userName} />
        <Row label="아이디" value={profile.userId} />
        <Row label="연락처" value={profile.phoneNumber ?? '-'} />
      </div>
      {profile.creditScore && (
        <div style={section}>
          <h3 style={sectionTitle}>신용 / 소득 정보</h3>
          <Row label="신용점수" value={`${profile.creditScore}점`} />
          <Row label="연소득" value={`${(profile.annualIncome / 10000).toLocaleString()}만원`} />
          <Row label="고용형태" value={profile.employmentType ?? '-'} />
          <Row label="직장명" value={profile.companyName ?? '-'} />
          <Row label="재직기간" value={`${profile.employmentMonths}개월`} />
          <Row label="기존 연간 상환액" value={`${(profile.annualRepayment / 10000).toLocaleString()}만원`} />
        </div>
      )}
      <button
        style={{ padding: '10px 20px', background: '#ea4335', color: '#fff', border: 'none', borderRadius: 8, cursor: 'pointer', marginTop: 8 }}
        onClick={handleLogout}
      >
        로그아웃
      </button>
    </div>
  )
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 0', borderBottom: '1px solid #f0f0f0' }}>
      <span style={{ color: '#666' }}>{label}</span>
      <span style={{ fontWeight: 600 }}>{value}</span>
    </div>
  )
}

const section: React.CSSProperties = { background: '#fff', borderRadius: 12, padding: '20px 24px', marginBottom: 16, boxShadow: '0 1px 6px rgba(0,0,0,0.08)' }
const sectionTitle: React.CSSProperties = { margin: '0 0 12px', fontSize: 15, color: '#1a73e8' }
