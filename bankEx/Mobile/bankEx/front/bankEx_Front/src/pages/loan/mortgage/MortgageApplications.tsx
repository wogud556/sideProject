import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { extractErrorMessage, getAccountsApi, type AccountResponse } from '../../../api/bank_api'
import {
  agreeMortgageApi,
  getMortgageAccountsApi,
  getMortgageApplicationApi,
  getMortgageApplicationsApi,
  withdrawMortgageApi,
  REPAYMENT_METHOD_LABEL,
  type MortgageApplication,
  type MortgageLoanAccount,
} from '../../../api/mortgage_api'
import { path } from '../../../router/path'
import { styles } from './mortgageStyles'
import MortgageRow from './MortgageRow'

/** 실행 전 단계에서는 고객이 신청을 철회할 수 있다 */
const WITHDRAWABLE = [
  'APPLIED', 'COLLATERAL_EVALUATED', 'LIMIT_CALCULATED', 'DOCUMENT_REQUIRED',
  'SCREENING_APPROVED', 'CONDITIONALLY_APPROVED', 'APPROVED', 'AGREED', 'EXECUTION_READY',
]

export default function MortgageApplications() {
  const navigate = useNavigate()
  const user = JSON.parse(localStorage.getItem('user') || 'null')

  const [applications, setApplications] = useState<MortgageApplication[]>([])
  const [loans, setLoans] = useState<MortgageLoanAccount[]>([])
  const [selected, setSelected] = useState<MortgageApplication | null>(null)
  const [accounts, setAccounts] = useState<AccountResponse[]>([])
  const [disbursementAccount, setDisbursementAccount] = useState('')
  const [consent, setConsent] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const load = () => {
    getMortgageApplicationsApi(user.userId).then(setApplications)
      .catch((err) => setError(extractErrorMessage(err, '신청 내역을 불러오지 못했습니다')))
    getMortgageAccountsApi(user.userId).then(setLoans).catch(() => setLoans([]))
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
      setDisbursementAccount(active[0]?.accountNumber ?? '')
    })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const open = async (applicationId: number) => {
    setError('')
    try {
      setSelected(await getMortgageApplicationApi(applicationId, user.userId))
      setConsent(false)
    } catch (err) {
      setError(extractErrorMessage(err, '신청 정보를 불러오지 못했습니다'))
    }
  }

  const run = async (action: () => Promise<MortgageApplication>, fallback: string) => {
    setLoading(true)
    setError('')
    try {
      setSelected(await action())
      load()
    } catch (err) {
      setError(extractErrorMessage(err, fallback))
    } finally {
      setLoading(false)
    }
  }

  if (selected) {
    const { approval, agreement } = selected
    return (
      <div style={styles.container}>
        <div style={styles.card}>
          <button onClick={() => setSelected(null)} style={styles.backButton}>← 목록</button>
          <h2 style={styles.cardTitle}>{selected.applicationNo}</h2>
          <p style={styles.cardSub}>{selected.propertyAddress}</p>
          <div style={styles.notice}>{selected.notice}</div>

          <div style={styles.resultBox}>
            <MortgageRow label="진행 상태" value={selected.statusLabel} highlight />
            <MortgageRow label="신청 금액" value={`${selected.requestedAmount.toLocaleString()}원`} />
            {selected.termMonths && selected.repaymentMethod && (
              <MortgageRow label="기간 / 상환" value={`${selected.termMonths / 12}년 / ${REPAYMENT_METHOD_LABEL[selected.repaymentMethod]}`} />
            )}
          </div>

          {approval && (
            <>
              <p style={styles.sectionTitle}>승인 조건</p>
              <div style={styles.resultBox}>
                <MortgageRow label="승인 금액" value={`${approval.approvedAmount.toLocaleString()}원`} highlight />
                <MortgageRow label="금리" value={`연 ${approval.interestRate.toFixed(2)}%`} />
                <MortgageRow label="기간" value={`${approval.termMonths / 12}년 · ${REPAYMENT_METHOD_LABEL[approval.repaymentMethod]}`} />
                {approval.conditions && <MortgageRow label="조건" value={approval.conditions} />}
                <MortgageRow label="약정 기한" value={approval.expiresOn} />
              </div>
            </>
          )}

          {agreement && (
            <>
              <p style={styles.sectionTitle}>약정</p>
              <div style={styles.resultBox}>
                <MortgageRow label="약정번호" value={agreement.agreementNo} />
                <MortgageRow label="입금 계좌" value={agreement.disbursementAccountNo} />
              </div>
            </>
          )}

          {selected.status === 'APPROVED' && (
            <>
              <p style={styles.sectionTitle}>약정하기</p>
              <label style={styles.label}>대출금 받을 계좌</label>
              <select value={disbursementAccount} onChange={(e) => setDisbursementAccount(e.target.value)} style={styles.input}>
                {accounts.map((a) => (
                  <option key={a.accountNumber} value={a.accountNumber}>{a.accountNumber}</option>
                ))}
              </select>
              <label style={{ ...styles.checkboxLabel, marginTop: '10px' }}>
                <input type="checkbox" checked={consent} onChange={(e) => setConsent(e.target.checked)} />
                승인 조건과 대출 약관에 동의합니다
              </label>
              <button
                disabled={loading || !consent || !disbursementAccount}
                onClick={() => run(() => agreeMortgageApi(selected.applicationId, user.userId, disbursementAccount), '약정에 실패했습니다')}
                style={{ ...styles.button, opacity: loading || !consent ? 0.6 : 1 }}
              >
                약정하기
              </button>
            </>
          )}

          {selected.loanAccountId && (
            <button onClick={() => navigate(path.mortgageAccount.replace(':loanAccountId', String(selected.loanAccountId)))} style={styles.button}>
              대출 계좌 · 상환 보기
            </button>
          )}

          {error && <p style={{ ...styles.error, marginTop: '12px' }}>{error}</p>}

          {WITHDRAWABLE.includes(selected.status) && (
            <button
              disabled={loading}
              onClick={() => {
                if (window.confirm('신청을 철회할까요? 철회한 신청은 되돌릴 수 없습니다.')) {
                  run(() => withdrawMortgageApi(selected.applicationId, user.userId), '철회에 실패했습니다')
                }
              }}
              style={{ ...styles.subButton, color: '#e53e3e' }}
            >
              신청 철회
            </button>
          )}
        </div>
      </div>
    )
  }

  return (
    <div style={styles.container}>
      <div style={styles.card}>
        <button onClick={() => navigate(path.home)} style={styles.backButton}>← 홈</button>
        <h2 style={styles.cardTitle}>내 주담대</h2>
        <p style={styles.cardSub}>신청 진행 상황과 실행된 대출을 확인합니다.</p>
        {error && <p style={styles.error}>{error}</p>}

        {loans.length > 0 && (
          <>
            <p style={styles.sectionTitle}>대출 계좌</p>
            {loans.map((l) => (
              <button
                key={l.account.loanAccountId}
                onClick={() => navigate(path.mortgageAccount.replace(':loanAccountId', String(l.account.loanAccountId)))}
                style={styles.listItem}
              >
                <div style={{ fontWeight: 'bold' }}>{l.account.loanAccountNo} · {l.statusLabel}</div>
                <div style={{ fontSize: '13px', color: '#555', marginTop: '4px' }}>
                  잔액 {l.account.outstandingPrincipal.toLocaleString()}원
                  {l.nextInstallment && ` · 다음 납부 ${l.nextInstallment.dueDate}`}
                </div>
              </button>
            ))}
          </>
        )}

        <p style={styles.sectionTitle}>신청 내역</p>
        {applications.length === 0 && <p style={{ fontSize: '13px', color: '#888' }}>신청 내역이 없습니다.</p>}
        {applications.map((a) => (
          <button key={a.applicationId} onClick={() => open(a.applicationId)} style={styles.listItem}>
            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
              <span style={{ fontWeight: 'bold' }}>{a.applicationNo}</span>
              <span style={styles.chip}>{a.statusLabel}</span>
            </div>
            <div style={{ fontSize: '13px', color: '#555', marginTop: '4px' }}>
              {a.requestedAmount.toLocaleString()}원 · {a.appliedAt.slice(0, 10)}
            </div>
          </button>
        ))}

        <button onClick={() => navigate(path.mortgage)} style={styles.button}>새로 신청하기</button>
      </div>
    </div>
  )
}
