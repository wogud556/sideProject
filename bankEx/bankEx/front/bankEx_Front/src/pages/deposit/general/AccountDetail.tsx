import { useState, useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import {
  getAccountApi,
  getTransactionsApi,
  depositApi,
  withdrawApi,
  closeAccountApi,
  extractErrorMessage,
  type AccountResponse,
  type TransactionResponse,
} from '../../../api/bank_api'
import { path } from '../../../router/path'

const TYPE_LABEL: Record<string, string> = {
  DEPOSIT: '입금',
  WITHDRAW: '출금',
  LOAN_DISBURSEMENT: '대출 실행금',
  LOAN_REPAYMENT: '대출 상환',
}

const CREDIT_TYPES = new Set(['DEPOSIT', 'LOAN_DISBURSEMENT'])

export default function AccountDetail() {
  const navigate = useNavigate()
  const { accountNumber } = useParams<{ accountNumber: string }>()
  const user = JSON.parse(localStorage.getItem('user') || 'null')

  const [account, setAccount] = useState<AccountResponse | null>(null)
  const [transactions, setTransactions] = useState<TransactionResponse[]>([])
  const [tab, setTab] = useState<'deposit' | 'withdraw'>('deposit')
  const [amount, setAmount] = useState('')
  const [description, setDescription] = useState('')
  const [loading, setLoading] = useState(false)
  const [closing, setClosing] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

  const loadData = () => {
    if (!accountNumber) return
    getAccountApi(accountNumber).then(setAccount).catch(() => setError('계좌 정보를 불러오지 못했습니다'))
    getTransactionsApi(accountNumber).then(setTransactions).catch(() => setError('거래내역을 불러오지 못했습니다'))
  }

  useEffect(() => {
    if (!user) {
      navigate(path.login)
      return
    }
    loadData()
  }, [accountNumber])

  const handleSubmit = async () => {
    if (!accountNumber) return
    const amountNumber = Number(amount)
    if (!amountNumber || amountNumber <= 0) {
      setError('올바른 금액을 입력해주세요')
      return
    }

    setLoading(true)
    setError('')
    setMessage('')
    try {
      if (tab === 'deposit') {
        await depositApi(accountNumber, amountNumber, description || '입금')
        setMessage('입금이 완료되었습니다')
      } else {
        await withdrawApi(accountNumber, amountNumber, description || '출금')
        setMessage('출금이 완료되었습니다')
      }
      setAmount('')
      setDescription('')
      loadData()
    } catch (err) {
      setError(extractErrorMessage(err, '처리에 실패했습니다'))
    } finally {
      setLoading(false)
    }
  }

  const handleClose = async () => {
    if (!accountNumber || !user) return
    if (!window.confirm('계좌를 해지하시겠습니까?')) return

    setClosing(true)
    setError('')
    setMessage('')
    try {
      await closeAccountApi(accountNumber, user.userId)
      navigate(path.mypage)
    } catch (err) {
      setError(extractErrorMessage(err, '계좌 해지에 실패했습니다'))
    } finally {
      setClosing(false)
    }
  }

  return (
    <div style={styles.container}>
      <div style={styles.card}>
        <button onClick={() => navigate(path.mypage)} style={styles.backButton}>← 마이페이지</button>
        <h2 style={styles.title}>계좌 상세</h2>

        {account && (
          <div style={styles.balanceBox}>
            <p style={styles.accountNumber}>{account.accountNumber}</p>
            <p style={styles.balance}>{account.balance.toLocaleString()}원</p>
          </div>
        )}

        {account && account.accountStatus === 'CLOSED' ? (
          <p style={styles.closedNotice}>해지된 계좌입니다</p>
        ) : (
          <>
            <div style={styles.tabRow}>
              <button
                onClick={() => { setTab('deposit'); setError(''); setMessage('') }}
                style={{ ...styles.tabButton, ...(tab === 'deposit' ? styles.tabButtonActive : {}) }}
              >
                입금
              </button>
              <button
                onClick={() => { setTab('withdraw'); setError(''); setMessage('') }}
                style={{ ...styles.tabButton, ...(tab === 'withdraw' ? styles.tabButtonActive : {}) }}
              >
                출금
              </button>
            </div>

            <div style={styles.fieldGroup}>
              <input
                type="number"
                placeholder="금액"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                style={styles.input}
              />
            </div>
            <div style={styles.fieldGroup}>
              <input
                type="text"
                placeholder="설명 (선택)"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                style={styles.input}
              />
            </div>

            {error && <p style={styles.error}>{error}</p>}
            {message && <p style={styles.success}>{message}</p>}

            <button
              onClick={handleSubmit}
              disabled={loading}
              style={{ ...styles.button, opacity: loading ? 0.7 : 1 }}
            >
              {loading ? '처리 중...' : tab === 'deposit' ? '입금하기' : '출금하기'}
            </button>

            {account && account.balance !== 0 && (
              <p style={styles.closeHint}>잔액을 모두 출금한 후 해지할 수 있습니다</p>
            )}
            <button
              onClick={handleClose}
              disabled={!account || account.balance !== 0 || closing}
              style={{ ...styles.closeButton, opacity: !account || account.balance !== 0 || closing ? 0.5 : 1 }}
            >
              {closing ? '해지 처리 중...' : '계좌 해지'}
            </button>
          </>
        )}

        <h4 style={styles.sectionTitle}>거래내역</h4>
        {transactions.length === 0 ? (
          <p style={styles.empty}>거래내역이 없습니다.</p>
        ) : (
          <div style={styles.list}>
            {transactions.map((t, idx) => {
              const isCredit = CREDIT_TYPES.has(t.transactionType)
              return (
                <div key={idx} style={styles.item}>
                  <div style={styles.itemRow}>
                    <span style={styles.itemType}>{TYPE_LABEL[t.transactionType] || t.transactionType}</span>
                    <span style={{ ...styles.itemAmount, color: isCredit ? '#2e7d32' : '#c0392b' }}>
                      {isCredit ? '+' : '-'}{t.amount.toLocaleString()}원
                    </span>
                  </div>
                  <div style={styles.itemRow}>
                    <span style={styles.itemDesc}>{t.description}</span>
                    <span style={styles.itemDate}>{t.transactionAt.slice(0, 16).replace('T', ' ')}</span>
                  </div>
                  <p style={styles.itemBalance}>잔액 {t.balanceAfter.toLocaleString()}원</p>
                </div>
              )
            })}
          </div>
        )}
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
    margin: '0 0 16px 0',
    fontSize: '20px',
  },
  balanceBox: {
    background: '#f0f5ff',
    borderRadius: '10px',
    padding: '16px',
    textAlign: 'center' as const,
    marginBottom: '20px',
  },
  accountNumber: {
    fontSize: '13px',
    color: '#666',
    letterSpacing: '1px',
    margin: '0 0 6px 0',
  },
  balance: {
    fontSize: '24px',
    fontWeight: 'bold',
    color: '#2c7be5',
    margin: 0,
  },
  tabRow: {
    display: 'flex',
    gap: '8px',
    marginBottom: '12px',
  },
  tabButton: {
    flex: 1,
    padding: '10px',
    borderRadius: '8px',
    border: '1px solid #e8e8e8',
    background: '#fff',
    color: '#888',
    cursor: 'pointer',
    fontWeight: 'bold',
  },
  tabButtonActive: {
    border: '1px solid #2c7be5',
    background: '#e8f0fb',
    color: '#2c7be5',
  },
  fieldGroup: {
    marginBottom: '10px',
  },
  input: {
    width: '100%',
    padding: '10px 12px',
    borderRadius: '8px',
    border: '1px solid #ddd',
    fontSize: '14px',
    boxSizing: 'border-box' as const,
  },
  button: {
    width: '100%',
    padding: '12px',
    borderRadius: '8px',
    border: 'none',
    background: '#2c7be5',
    color: '#fff',
    fontWeight: 'bold',
    cursor: 'pointer',
    fontSize: '15px',
    marginBottom: '20px',
  },
  sectionTitle: {
    fontSize: '14px',
    color: '#888',
    marginBottom: '8px',
    fontWeight: '600',
  },
  list: {
    display: 'flex',
    flexDirection: 'column' as const,
    gap: '10px',
  },
  item: {
    border: '1px solid #e8e8e8',
    borderRadius: '10px',
    padding: '12px',
  },
  itemRow: {
    display: 'flex',
    justifyContent: 'space-between',
    marginBottom: '4px',
  },
  itemType: {
    fontSize: '14px',
    fontWeight: 'bold',
  },
  itemAmount: {
    fontSize: '14px',
    fontWeight: 'bold',
  },
  itemDesc: {
    fontSize: '12px',
    color: '#888',
  },
  itemDate: {
    fontSize: '12px',
    color: '#bbb',
  },
  itemBalance: {
    fontSize: '12px',
    color: '#aaa',
    margin: '4px 0 0 0',
  },
  empty: {
    color: '#aaa',
    fontSize: '14px',
    textAlign: 'center' as const,
    padding: '12px 0',
  },
  error: {
    color: 'red',
    fontSize: '14px',
    marginBottom: '10px',
  },
  success: {
    color: '#2e7d32',
    fontSize: '14px',
    marginBottom: '10px',
  },
  closedNotice: {
    color: '#888',
    fontSize: '14px',
    textAlign: 'center' as const,
    padding: '16px 0',
    marginBottom: '20px',
  },
  closeHint: {
    color: '#c0392b',
    fontSize: '12px',
    marginBottom: '8px',
  },
  closeButton: {
    width: '100%',
    padding: '12px',
    borderRadius: '8px',
    border: '1px solid #c0392b',
    background: '#fff',
    color: '#c0392b',
    fontWeight: 'bold',
    cursor: 'pointer',
    fontSize: '15px',
    marginBottom: '20px',
  },
}
