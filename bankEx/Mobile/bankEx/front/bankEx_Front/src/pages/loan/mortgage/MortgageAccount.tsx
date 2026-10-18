import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { extractErrorMessage, getAccountsApi, type AccountResponse } from '../../../api/bank_api'
import {
  getMortgageAccountApi,
  getMortgageScheduleApi,
  repayMortgageApi,
  type MortgageInstallment,
  type MortgageLoanAccount,
  type MortgageRepayResponse,
} from '../../../api/mortgage_api'
import { path } from '../../../router/path'
import { styles } from './mortgageStyles'
import MortgageRow from './MortgageRow'

export default function MortgageAccount() {
  const navigate = useNavigate()
  const { loanAccountId } = useParams<{ loanAccountId: string }>()
  const user = JSON.parse(localStorage.getItem('user') || 'null')

  const [loan, setLoan] = useState<MortgageLoanAccount | null>(null)
  const [schedule, setSchedule] = useState<MortgageInstallment[]>([])
  const [accounts, setAccounts] = useState<AccountResponse[]>([])
  const [sourceAccount, setSourceAccount] = useState('')
  const [showAll, setShowAll] = useState(false)
  const [repaid, setRepaid] = useState<MortgageRepayResponse | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const id = Number(loanAccountId)

  const load = async () => {
    try {
      const [l, s] = await Promise.all([getMortgageAccountApi(id, user.userId), getMortgageScheduleApi(id, user.userId)])
      setLoan(l)
      setSchedule(s)
    } catch (err) {
      setError(extractErrorMessage(err, '대출 정보를 불러오지 못했습니다'))
    }
  }

  useEffect(() => {
    if (!user) {
      navigate(path.login)
      return
    }
    load()
    getAccountsApi(user.userId).then((list) => {
      const active = list.filter((a) => a.accountStatus === 'ACTIVE')
      setAccounts(active)
      setSourceAccount(active[0]?.accountNumber ?? '')
    })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [loanAccountId])

  const handleRepay = async () => {
    setLoading(true)
    setError('')
    try {
      setRepaid(await repayMortgageApi(id, user.userId, sourceAccount))
      await load()
    } catch (err) {
      setError(extractErrorMessage(err, '상환에 실패했습니다'))
    } finally {
      setLoading(false)
    }
  }

  if (!loan) {
    return (
      <div style={styles.container}>
        <div style={styles.card}>{error ? <p style={styles.error}>{error}</p> : '불러오는 중...'}</div>
      </div>
    )
  }

  const { account, nextInstallment } = loan
  const visible = showAll ? schedule : schedule.filter((s) => s.status === 'SCHEDULED').slice(0, 6)

  return (
    <div style={styles.container}>
      <div style={styles.card}>
        <button onClick={() => navigate(path.mortgageApplications)} style={styles.backButton}>← 내 주담대</button>
        <h2 style={styles.cardTitle}>{account.loanAccountNo}</h2>
        <p style={styles.cardSub}>{loan.repaymentMethodLabel} · 연 {account.interestRate.toFixed(2)}% · {loan.statusLabel}</p>

        <div style={styles.resultBox}>
          <MortgageRow label="대출 잔액" value={`${account.outstandingPrincipal.toLocaleString()}원`} highlight />
          <MortgageRow label="실행 금액" value={`${account.principal.toLocaleString()}원`} />
          <MortgageRow label="실행일 / 만기일" value={`${account.executionDate} / ${account.maturityDate}`} />
        </div>

        {repaid && (
          <div style={styles.notice}>
            {repaid.installmentNo}회차 {repaid.totalPaid.toLocaleString()}원을 상환했습니다.
            출금 후 잔액 {repaid.balanceAfter.toLocaleString()}원
          </div>
        )}

        {nextInstallment ? (
          <>
            <p style={styles.sectionTitle}>이번 회차 ({nextInstallment.installmentNo}회차)</p>
            <div style={styles.resultBox}>
              <MortgageRow label="납기일" value={nextInstallment.dueDate} />
              <MortgageRow label="원금 + 이자" value={`${nextInstallment.principal.toLocaleString()} + ${nextInstallment.interest.toLocaleString()}`} />
              <MortgageRow label="납부 금액" value={`${nextInstallment.totalPayment.toLocaleString()}원`} highlight />
            </div>
            <label style={styles.label}>출금 계좌</label>
            <select value={sourceAccount} onChange={(e) => setSourceAccount(e.target.value)} style={styles.input}>
              {accounts.map((a) => (
                <option key={a.accountNumber} value={a.accountNumber}>
                  {a.accountNumber} (잔액 {a.balance.toLocaleString()}원)
                </option>
              ))}
            </select>
            {error && <p style={{ ...styles.error, marginTop: '10px' }}>{error}</p>}
            <button onClick={handleRepay} disabled={loading || !sourceAccount} style={{ ...styles.button, opacity: loading ? 0.7 : 1 }}>
              {loading ? '처리 중...' : `${nextInstallment.totalPayment.toLocaleString()}원 상환하기`}
            </button>
          </>
        ) : (
          <div style={styles.notice}>모든 회차를 상환했습니다.</div>
        )}

        <p style={styles.sectionTitle}>상환 스케줄 ({schedule.length}회차)</p>
        {visible.map((s) => (
          <MortgageRow
            key={s.installmentNo}
            label={`${s.installmentNo}회 · ${s.dueDate}`}
            value={`${s.totalPayment.toLocaleString()}원${s.status === 'PAID' ? ' (납부)' : ''}`}
          />
        ))}
        <button onClick={() => setShowAll((v) => !v)} style={styles.subButton}>
          {showAll ? '예정 회차만 보기' : '전체 회차 보기'}
        </button>
      </div>
    </div>
  )
}
