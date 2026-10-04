import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  getMyApplicationsApi,
  approveLoanApi,
  disburseLoanApi,
  repayLoanApi,
  extractErrorMessage,
  type LoanApplicationResponse,
} from '../../../api/bank_api'
import { path } from '../../../router/path'

const STATUS_STYLE: Record<string, { background: string; color: string }> = {
  심사중: { background: '#fff3cd', color: '#856404' },
  승인: { background: '#d1ecf1', color: '#0c5460' },
  실행완료: { background: '#d4edda', color: '#155724' },
}

export default function MyLoans() {
  const navigate = useNavigate()
  const user = JSON.parse(localStorage.getItem('user') || 'null')
  const [applications, setApplications] = useState<LoanApplicationResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const loadApplications = () => {
    if (!user) return
    getMyApplicationsApi(user.userId)
      .then(setApplications)
      .catch(() => setError('대출 내역을 불러오지 못했습니다'))
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    if (!user) {
      navigate(path.login)
      return
    }
    loadApplications()
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
            <ApplicationCard key={a.applicationId} application={a} onChanged={loadApplications} />
          ))}
        </div>
      </div>
    </div>
  )
}

function ApplicationCard({
  application,
  onChanged,
}: {
  application: LoanApplicationResponse
  onChanged: () => void
}) {
  const [repayAmount, setRepayAmount] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  const badge = STATUS_STYLE[application.status] || { background: '#eee', color: '#555' }

  const handleApprove = async () => {
    setBusy(true)
    setError('')
    try {
      await approveLoanApi(application.applicationId)
      onChanged()
    } catch (err) {
      setError(extractErrorMessage(err, '승인 처리에 실패했습니다'))
    } finally {
      setBusy(false)
    }
  }

  const handleDisburse = async () => {
    setBusy(true)
    setError('')
    try {
      await disburseLoanApi(application.applicationId)
      onChanged()
    } catch (err) {
      setError(extractErrorMessage(err, '실행 처리에 실패했습니다'))
    } finally {
      setBusy(false)
    }
  }

  const handleRepay = async () => {
    const amountNumber = Number(repayAmount)
    if (!amountNumber || amountNumber <= 0) {
      setError('올바른 상환 금액을 입력해주세요')
      return
    }
    setBusy(true)
    setError('')
    try {
      await repayLoanApi(application.applicationId, amountNumber)
      setRepayAmount('')
      onChanged()
    } catch (err) {
      setError(extractErrorMessage(err, '상환에 실패했습니다'))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div style={styles.item}>
      <div style={styles.itemHeader}>
        <span style={styles.productName}>{application.productName}</span>
        <span style={{ ...styles.statusBadge, background: badge.background, color: badge.color }}>
          {application.status}
        </span>
      </div>
      <div style={styles.itemRow}>
        <span style={styles.itemLabel}>신청 금액</span>
        <span style={styles.itemValue}>{application.requestAmount.toLocaleString()}원</span>
      </div>
      <div style={styles.itemRow}>
        <span style={styles.itemLabel}>대출 기간</span>
        <span style={styles.itemValue}>{application.loanPeriod}개월</span>
      </div>
      <div style={styles.itemRow}>
        <span style={styles.itemLabel}>신청일</span>
        <span style={styles.itemValue}>{application.createdAt.slice(0, 10)}</span>
      </div>

      {application.status === '실행완료' && (
        <div style={styles.itemRow}>
          <span style={styles.itemLabel}>남은 대출 잔액</span>
          <span style={styles.itemValue}>{(application.remainingBalance ?? 0).toLocaleString()}원</span>
        </div>
      )}

      {error && <p style={styles.error}>{error}</p>}

      {application.status === '심사중' && (
        <button onClick={handleApprove} disabled={busy} style={styles.actionButton}>
          {busy ? '처리 중...' : '승인 처리 (테스트)'}
        </button>
      )}

      {application.status === '승인' && (
        <button onClick={handleDisburse} disabled={busy} style={styles.actionButton}>
          {busy ? '처리 중...' : '대출 실행하기'}
        </button>
      )}

      {application.status === '실행완료' && (application.remainingBalance ?? 0) > 0 && (
        <div style={styles.repayRow}>
          <input
            type="number"
            placeholder="상환 금액"
            value={repayAmount}
            onChange={(e) => setRepayAmount(e.target.value)}
            style={styles.repayInput}
          />
          <button onClick={handleRepay} disabled={busy} style={styles.repayButton}>
            {busy ? '처리 중...' : '상환하기'}
          </button>
        </div>
      )}

      {application.status === '실행완료' && (application.remainingBalance ?? 0) <= 0 && (
        <p style={styles.paidOff}>상환 완료</p>
      )}
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
    fontSize: '13px',
    margin: '6px 0',
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
  actionButton: {
    width: '100%',
    marginTop: '10px',
    padding: '10px',
    borderRadius: '8px',
    border: 'none',
    background: '#2c7be5',
    color: '#fff',
    cursor: 'pointer',
    fontWeight: 'bold',
    fontSize: '13px',
  },
  repayRow: {
    display: 'flex',
    gap: '8px',
    marginTop: '10px',
  },
  repayInput: {
    flex: 1,
    padding: '10px 12px',
    borderRadius: '8px',
    border: '1px solid #ddd',
    fontSize: '13px',
    boxSizing: 'border-box' as const,
  },
  repayButton: {
    padding: '10px 14px',
    borderRadius: '8px',
    border: 'none',
    background: '#2c7be5',
    color: '#fff',
    cursor: 'pointer',
    fontSize: '13px',
    fontWeight: 'bold',
    whiteSpace: 'nowrap' as const,
  },
  paidOff: {
    marginTop: '10px',
    textAlign: 'center' as const,
    color: '#2e7d32',
    fontWeight: 'bold',
    fontSize: '13px',
  },
}
