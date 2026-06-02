import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { getMyApplicationsApi, type LoanApplicationResponse } from '../api/bank_api'
import { path } from '../router/path'

export default function MyLoans() {
  const navigate = useNavigate()
  const user = JSON.parse(localStorage.getItem('user') || 'null')
  const [applications, setApplications] = useState<LoanApplicationResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!user) {
      navigate(path.login)
      return
    }
    getMyApplicationsApi(user.userId)
      .then(setApplications)
      .catch(() => setError('대출 내역을 불러오지 못했습니다'))
      .finally(() => setLoading(false))
  }, [])

  return (
    <div style={styles.container}>
      <div style={styles.card}>
        <button onClick={() => navigate(path.home)} style={styles.backButton}>← 홈</button>
        <h2 style={styles.title}>내 대출 내역</h2>

        {loading && <p style={styles.center}>불러오는 중...</p>}
        {error && <p style={styles.error}>{error}</p>}

        {!loading && applications.length === 0 && (
          <div style={styles.empty}>
            <p>신청한 대출이 없습니다.</p>
            <button onClick={() => navigate(path.loan)} style={styles.button}>대출 상품 보기</button>
          </div>
        )}

        <div style={styles.list}>
          {applications.map((a) => (
            <div key={a.applicationId} style={styles.item}>
              <div style={styles.itemHeader}>
                <span style={styles.productName}>{a.productName}</span>
                <span style={{ ...styles.statusBadge, background: a.status === '심사중' ? '#fff3cd' : '#d4edda', color: a.status === '심사중' ? '#856404' : '#155724' }}>
                  {a.status}
                </span>
              </div>
              <div style={styles.itemRow}>
                <span style={styles.itemLabel}>신청 금액</span>
                <span style={styles.itemValue}>{a.requestAmount.toLocaleString()}원</span>
              </div>
              <div style={styles.itemRow}>
                <span style={styles.itemLabel}>대출 기간</span>
                <span style={styles.itemValue}>{a.loanPeriod}개월</span>
              </div>
              <div style={styles.itemRow}>
                <span style={styles.itemLabel}>신청일</span>
                <span style={styles.itemValue}>{a.createdAt.slice(0, 10)}</span>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
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
    width: '380px',
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
  title: {
    margin: '0 0 20px 0',
    fontSize: '20px',
  },
  list: {
    display: 'flex',
    flexDirection: 'column' as const,
    gap: '12px',
  },
  item: {
    border: '1px solid #e8e8e8',
    borderRadius: '12px',
    padding: '16px',
  },
  itemHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '12px',
  },
  productName: {
    fontSize: '16px',
    fontWeight: 'bold',
  },
  statusBadge: {
    fontSize: '12px',
    padding: '3px 8px',
    borderRadius: '20px',
  },
  itemRow: {
    display: 'flex',
    justifyContent: 'space-between',
    marginBottom: '6px',
  },
  itemLabel: {
    fontSize: '13px',
    color: '#888',
  },
  itemValue: {
    fontSize: '13px',
    color: '#333',
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
    textAlign: 'center' as const,
    color: '#888',
    padding: '20px 0',
  },
  button: {
    marginTop: '12px',
    padding: '10px 20px',
    borderRadius: '8px',
    border: 'none',
    background: '#2c7be5',
    color: '#fff',
    cursor: 'pointer',
    fontWeight: 'bold',
  },
}
