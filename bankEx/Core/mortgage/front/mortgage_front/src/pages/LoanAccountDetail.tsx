import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { getLoanAccount, getRepaymentHistory, getSchedule, errorMessage, won } from '../api/mortgage_api'
import type { Installment, LoanAccountResponse, RepaymentHistory } from '../api/mortgage_api'
import { Card, ErrorText, Rows } from '../components/ui'
import { tableStyle, tdStyle, thStyle } from '../components/styles'

export default function LoanAccountDetail() {
  const { loanAccountId } = useParams<{ loanAccountId: string }>()
  const [loan, setLoan] = useState<LoanAccountResponse | null>(null)
  const [schedule, setSchedule] = useState<Installment[]>([])
  const [history, setHistory] = useState<RepaymentHistory[]>([])
  const [error, setError] = useState('')

  useEffect(() => {
    const id = Number(loanAccountId)
    Promise.all([getLoanAccount(id), getSchedule(id), getRepaymentHistory(id)])
      .then(([l, s, h]) => { setLoan(l.data); setSchedule(s.data); setHistory(h.data) })
      .catch(e => setError(errorMessage(e)))
  }, [loanAccountId])

  if (!loan) return error ? <ErrorText message={error} /> : <p>불러오는 중...</p>
  const a = loan.account

  return (
    <>
      <h2>대출 계좌 {a.loanAccountNo}</h2>
      <Card>
        <Rows rows={[
          ['고객 / 상품', `${a.customerId} / ${a.productCode}`],
          ['상태', loan.statusLabel],
          ['실행 원금 / 잔액', `${won(a.principal)} / ${won(a.outstandingPrincipal)}`],
          ['금리 / 기간 / 상환 방식', `연 ${a.interestRate}% / ${a.termMonths}개월 / ${loan.repaymentMethodLabel}`],
          ['실행일 / 만기일', `${a.executionDate} / ${a.maturityDate}`],
        ]} />
      </Card>

      {history.length > 0 && (
        <Card title="상환 거래">
          <table style={tableStyle}>
            <thead><tr><th style={thStyle}>회차</th><th style={{ ...thStyle, textAlign: 'right' }}>납부액</th><th style={thStyle}>출금 계좌 / bankEx 거래</th><th style={thStyle}>일시</th></tr></thead>
            <tbody>
              {history.map(h => (
                <tr key={h.repaymentId}>
                  <td style={tdStyle}>{h.installmentNo}</td>
                  <td style={{ ...tdStyle, textAlign: 'right' }}>{won(h.totalPaid)}</td>
                  <td style={tdStyle}>{h.sourceAccountNo} / {h.idempotencyKey}</td>
                  <td style={tdStyle}>{new Date(h.paidAt).toLocaleString()}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </Card>
      )}

      <Card title={`상환 스케줄 (${schedule.length}회차)`}>
        <div style={{ maxHeight: 480, overflowY: 'auto' }}>
          <table style={tableStyle}>
            <thead>
              <tr>
                <th style={thStyle}>회차</th><th style={thStyle}>납기일</th><th style={{ ...thStyle, textAlign: 'right' }}>원금</th>
                <th style={{ ...thStyle, textAlign: 'right' }}>이자</th><th style={{ ...thStyle, textAlign: 'right' }}>합계</th>
                <th style={{ ...thStyle, textAlign: 'right' }}>잔액</th><th style={thStyle}>상태</th>
              </tr>
            </thead>
            <tbody>
              {schedule.map(s => (
                <tr key={s.installmentNo} style={{ color: s.status === 'PAID' ? '#9aa0a6' : undefined }}>
                  <td style={tdStyle}>{s.installmentNo}</td>
                  <td style={tdStyle}>{s.dueDate}</td>
                  <td style={{ ...tdStyle, textAlign: 'right' }}>{s.principal.toLocaleString()}</td>
                  <td style={{ ...tdStyle, textAlign: 'right' }}>{s.interest.toLocaleString()}</td>
                  <td style={{ ...tdStyle, textAlign: 'right' }}>{s.totalPayment.toLocaleString()}</td>
                  <td style={{ ...tdStyle, textAlign: 'right' }}>{s.balanceAfter.toLocaleString()}</td>
                  <td style={tdStyle}>{s.status === 'PAID' ? '납부' : '예정'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>
    </>
  )
}
