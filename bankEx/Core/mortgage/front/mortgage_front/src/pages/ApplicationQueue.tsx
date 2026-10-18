import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getApplications, errorMessage, won, QUEUE_TABS, STATUS_COLOR, ROLE_LABEL } from '../api/mortgage_api'
import type { ApplicationSummary } from '../api/mortgage_api'
import { detailPath } from '../router/path'
import { Badge, Card, ErrorText } from '../components/ui'
import { tableStyle, tdStyle, thStyle } from '../components/styles'

export default function ApplicationQueue() {
  const navigate = useNavigate()
  const [tab, setTab] = useState(0)
  const [rows, setRows] = useState<ApplicationSummary[]>([])
  const [error, setError] = useState('')

  useEffect(() => {
    getApplications(QUEUE_TABS[tab].status)
      .then(res => { setRows(res.data); setError('') })
      .catch(e => setError(errorMessage(e)))
  }, [tab])

  const current = QUEUE_TABS[tab]

  return (
    <>
      <h2>업무 대기열</h2>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
        {QUEUE_TABS.map((t, i) => (
          <button key={t.label} onClick={() => setTab(i)} style={{
            padding: '6px 12px', borderRadius: 16, border: '1px solid #dadce0', cursor: 'pointer', fontSize: 13,
            background: i === tab ? '#174ea6' : '#fff', color: i === tab ? '#fff' : '#202124',
          }}>
            {t.label}
          </button>
        ))}
      </div>
      {current.role && <p style={{ color: '#5f6368', fontSize: 13 }}>처리 담당: {ROLE_LABEL[current.role]}</p>}
      <ErrorText message={error} />

      <Card>
        {rows.length === 0 ? (
          <p style={{ color: '#5f6368', margin: 0 }}>대기 중인 신청이 없습니다.</p>
        ) : (
          <table style={tableStyle}>
            <thead>
              <tr>
                <th style={thStyle}>신청번호</th>
                <th style={thStyle}>고객</th>
                <th style={thStyle}>상품</th>
                <th style={{ ...thStyle, textAlign: 'right' }}>신청 금액</th>
                <th style={thStyle}>상태</th>
                <th style={thStyle}>최종 변경</th>
              </tr>
            </thead>
            <tbody>
              {rows.map(r => (
                <tr key={r.applicationId} onClick={() => navigate(detailPath(r.applicationId))} style={{ cursor: 'pointer' }}>
                  <td style={tdStyle}>{r.applicationNo}</td>
                  <td style={tdStyle}>{r.customerName} ({r.customerId})</td>
                  <td style={tdStyle}>{r.productCode}</td>
                  <td style={{ ...tdStyle, textAlign: 'right' }}>{won(r.requestedAmount)}</td>
                  <td style={tdStyle}><Badge color={STATUS_COLOR[r.status]}>{r.statusLabel}</Badge></td>
                  <td style={tdStyle}>{new Date(r.updatedAt).toLocaleString()}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Card>
    </>
  )
}
