import { useState } from 'react'
import { Link } from 'react-router-dom'
import { execute, getApplication, errorMessage, won } from '../../api/mortgage_api'
import { loanAccountPath } from '../../router/path'
import { Badge, Card, ErrorText } from '../ui'
import { buttonStyle, tableStyle, tdStyle, thStyle } from '../styles'
import type { PanelProps } from './types'

export default function ExecutionPanel({ detail, onUpdated }: PanelProps) {
  // 실행 버튼 한 번에 키 하나. 응답을 못 받아 다시 눌러도 같은 키라 중복 실행되지 않는다.
  // 실패(FAILED) 응답을 받으면 새 키로 바꿔 재시도할 수 있게 한다.
  const [idempotencyKey, setIdempotencyKey] = useState(() => crypto.randomUUID())
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const id = detail.application.applicationId

  const submit = async () => {
    setBusy(true)
    setError('')
    try {
      const res = await execute(id, idempotencyKey)
      if (res.data.status === 'FAILED') setIdempotencyKey(crypto.randomUUID())
      onUpdated((await getApplication(id)).data)
    } catch (e) {
      setError(errorMessage(e))
    } finally {
      setBusy(false)
    }
  }

  return (
    <Card title="5. 대출 실행">
      {detail.executions.length > 0 && (
        <table style={tableStyle}>
          <thead><tr><th style={thStyle}>실행번호</th><th style={thStyle}>결과</th><th style={{ ...thStyle, textAlign: 'right' }}>금액</th><th style={thStyle}>bankEx 거래 / 사유</th><th style={thStyle}>담당</th></tr></thead>
          <tbody>
            {detail.executions.map(x => (
              <tr key={x.executionId}>
                <td style={tdStyle}>{x.executionNo}</td>
                <td style={tdStyle}><Badge color={x.status === 'COMPLETED' ? '#188038' : '#d93025'}>{x.status === 'COMPLETED' ? '완료' : '실패'}</Badge></td>
                <td style={{ ...tdStyle, textAlign: 'right' }}>{won(x.amount)}</td>
                <td style={{ ...tdStyle, fontSize: 12 }}>{x.bankTransactionNo ?? x.failureReason}</td>
                <td style={tdStyle}>{x.executedBy}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
      {detail.application.status === 'EXECUTION_READY' && (
        <div>
          <p style={{ fontSize: 13, color: '#5f6368' }}>
            여신 실무 담당. {won(detail.agreement?.agreedAmount)}을 bankEx 계좌 {detail.agreement?.disbursementAccountNo}로 입금하고
            대출 계좌와 상환 스케줄을 만듭니다.
          </p>
          <button disabled={busy} onClick={submit} style={buttonStyle('#9334e6', busy)}>대출 실행</button>
        </div>
      )}
      {detail.loanAccountId && (
        <p><Link to={loanAccountPath(detail.loanAccountId)} style={{ color: '#1a73e8' }}>대출 계좌 · 상환 스케줄 보기 →</Link></p>
      )}
      <ErrorText message={error} />
    </Card>
  )
}
