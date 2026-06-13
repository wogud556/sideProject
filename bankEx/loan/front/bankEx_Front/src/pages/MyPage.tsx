import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { getUserProfileApi, type UserProfileResponse } from '../api/bank_api'
import { path } from '../router/path'

export default function MyPage() {
  const navigate = useNavigate()
  const user = JSON.parse(localStorage.getItem('user') || 'null')
  const [profile, setProfile] = useState<UserProfileResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [copiedAccount, setCopiedAccount] = useState<string | null>(null)

  useEffect(() => {
    if (!user) {
      navigate(path.login)
      return
    }
    getUserProfileApi(user.userId)
      .then(setProfile)
      .catch(() => setError('정보를 불러오지 못했습니다'))
      .finally(() => setLoading(false))
  }, [])

  const handleCopy = (accountNumber: string) => {
    navigator.clipboard.writeText(accountNumber)
    setCopiedAccount(accountNumber)
    setTimeout(() => setCopiedAccount(null), 2000)
  }

  if (loading) {
    return (
      <div style={styles.container}>
        <div style={styles.card}>
          <p style={styles.center}>불러오는 중...</p>
        </div>
      </div>
    )
  }

  return (
    <div style={styles.container}>
      <div style={styles.card}>
        <button onClick={() => navigate(path.home)} style={styles.backButton}>← 홈</button>
        <h2 style={styles.pageTitle}>마이페이지</h2>

        {error && <p style={styles.error}>{error}</p>}

        {profile && (
          <>
            {/* 개인정보 섹션 */}
            <section style={styles.section}>
              <div style={styles.avatar}>
                {profile.userName.slice(0, 1)}
              </div>
              <h3 style={styles.userName}>{profile.userName}</h3>
              <p style={styles.userId}>@{profile.userId}</p>
            </section>

            <section style={styles.section}>
              <h4 style={styles.sectionTitle}>개인정보</h4>
              <div style={styles.infoBox}>
                <InfoRow label="이름" value={profile.userName} />
                <InfoRow label="아이디" value={profile.userId} />
                <InfoRow label="전화번호" value={profile.phone || '미등록'} />
                <InfoRow label="가입일" value={profile.createdAt.slice(0, 10)} isLast />
              </div>
            </section>

            {/* 계좌 섹션 */}
            <section style={styles.section}>
              <h4 style={styles.sectionTitle}>내 계좌</h4>
              {profile.accounts.length === 0 ? (
                <p style={styles.empty}>등록된 계좌가 없습니다.</p>
              ) : (
                <div style={styles.accountList}>
                  {profile.accounts.map((account, idx) => (
                    <div key={account.accountId} style={styles.accountCard}>
                      <div style={styles.accountHeader}>
                        <span style={styles.accountLabel}>계좌 {idx + 1}</span>
                        <span style={styles.balance}>{account.balance.toLocaleString()}원</span>
                      </div>
                      <div style={styles.accountNumberRow}>
                        <span style={styles.accountNumber}>{account.accountNumber}</span>
                        <button
                          onClick={() => handleCopy(account.accountNumber)}
                          style={styles.copyButton}
                        >
                          {copiedAccount === account.accountNumber ? '복사됨 ✓' : '복사'}
                        </button>
                      </div>
                      <p style={styles.accountDate}>개설일: {account.createdAt.slice(0, 10)}</p>
                    </div>
                  ))}
                </div>
              )}
            </section>
          </>
        )}
      </div>
    </div>
  )
}

function InfoRow({ label, value, isLast }: { label: string; value: string; isLast?: boolean }) {
  return (
    <div style={{ ...infoRowStyle.row, borderBottom: isLast ? 'none' : '1px solid #f0f0f0' }}>
      <span style={infoRowStyle.label}>{label}</span>
      <span style={infoRowStyle.value}>{value}</span>
    </div>
  )
}

const infoRowStyle = {
  row: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: '12px 0',
  },
  label: {
    fontSize: '14px',
    color: '#888',
  },
  value: {
    fontSize: '14px',
    color: '#333',
    fontWeight: '500',
  },
}

const styles = {
  container: {
    minHeight: '100vh',
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'flex-start',
    background: '#f5f6f8',
    padding: '40px 20px',
  },
  card: {
    width: '360px',
    padding: '24px',
    borderRadius: '16px',
    background: '#fff',
    boxShadow: '0 4px 12px rgba(0,0,0,0.1)',
  },
  backButton: {
    background: 'none',
    border: 'none',
    color: '#2c7be5',
    cursor: 'pointer',
    fontSize: '14px',
    padding: '0',
    marginBottom: '8px',
  },
  pageTitle: {
    margin: '0 0 24px 0',
    fontSize: '20px',
  },
  section: {
    marginBottom: '24px',
  },
  avatar: {
    width: '64px',
    height: '64px',
    borderRadius: '50%',
    background: '#2c7be5',
    color: '#fff',
    fontSize: '28px',
    fontWeight: 'bold',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    margin: '0 auto 12px',
  },
  userName: {
    textAlign: 'center' as const,
    margin: '0 0 4px 0',
    fontSize: '20px',
  },
  userId: {
    textAlign: 'center' as const,
    color: '#aaa',
    fontSize: '13px',
    margin: '0 0 8px 0',
  },
  sectionTitle: {
    fontSize: '14px',
    color: '#888',
    marginBottom: '8px',
    fontWeight: '600',
    textTransform: 'uppercase' as const,
    letterSpacing: '0.5px',
  },
  infoBox: {
    border: '1px solid #f0f0f0',
    borderRadius: '12px',
    padding: '0 16px',
  },
  accountList: {
    display: 'flex',
    flexDirection: 'column' as const,
    gap: '12px',
  },
  accountCard: {
    border: '1px solid #e8e8e8',
    borderRadius: '12px',
    padding: '16px',
    background: '#fafafa',
  },
  accountHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '8px',
  },
  accountLabel: {
    fontSize: '13px',
    color: '#888',
  },
  balance: {
    fontSize: '15px',
    fontWeight: 'bold',
    color: '#333',
  },
  accountNumberRow: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '6px',
  },
  accountNumber: {
    fontSize: '16px',
    fontWeight: '600',
    color: '#2c7be5',
    letterSpacing: '1px',
  },
  copyButton: {
    fontSize: '12px',
    padding: '4px 10px',
    borderRadius: '6px',
    border: '1px solid #2c7be5',
    background: '#fff',
    color: '#2c7be5',
    cursor: 'pointer',
  },
  accountDate: {
    fontSize: '12px',
    color: '#bbb',
    margin: '0',
  },
  center: {
    textAlign: 'center' as const,
    color: '#888',
  },
  error: {
    color: 'red',
    fontSize: '14px',
  },
  empty: {
    color: '#aaa',
    fontSize: '14px',
    textAlign: 'center' as const,
    padding: '12px 0',
  },
}
