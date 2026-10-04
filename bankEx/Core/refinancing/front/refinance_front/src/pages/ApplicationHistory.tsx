import { useEffect, useState } from 'react'
import {
  listRefinanceApplicationsApi,
  getApplicationHistoryApi,
  type RefinanceApplicationResponse,
  type RefinanceHistoryResponse,
} from '../api/refinance_api'

export default function ApplicationHistory() {
  const [applications, setApplications] = useState<RefinanceApplicationResponse[]>([])
  const [selected, setSelected] = useState<RefinanceApplicationResponse | null>(null)
  const [history, setHistory] = useState<RefinanceHistoryResponse[]>([])

  useEffect(() => {
    listRefinanceApplicationsApi().then(setApplications)
  }, [])

  const select = async (app: RefinanceApplicationResponse) => {
    setSelected(app)
    const h = await getApplicationHistoryApi(app.applicationId)
    setHistory(h)
  }

  return (
    <div style={{ maxWidth: 1000, margin: '40px auto', padding: 24, display: 'flex', gap: 24 }}>
      <div style={{ flex: '0 0 320px' }}>
        <h1 style={{ fontSize: 18, marginBottom: 12 }}>업무이력</h1>
        <ul style={{ listStyle: 'none', padding: 0 }}>
          {applications.map((a) => (
            <li
              key={a.applicationId}
              onClick={() => select(a)}
              style={{
                padding: 10, border: '1px solid #d8dce3', borderRadius: 6, marginBottom: 8, cursor: 'pointer',
                background: selected?.applicationId === a.applicationId ? '#eef2fb' : '#fff',
              }}
            >
              <div style={{ fontSize: 13, fontWeight: 'bold' }}>{a.applicationNo}</div>
              <div style={{ fontSize: 12, color: '#666' }}>{a.status}</div>
            </li>
          ))}
        </ul>
      </div>

      <div style={{ flex: 1 }}>
        {!selected ? (
          <p style={{ color: '#888' }}>좌측 목록에서 신청 건을 선택하세요.</p>
        ) : (
          <div>
            <h2 style={{ fontSize: 18, marginBottom: 16 }}>{selected.applicationNo} 업무이력</h2>
            <div style={{ borderLeft: '2px solid #d8dce3', paddingLeft: 20 }}>
              {history.map((h) => (
                <div key={h.historyId} style={{ marginBottom: 16, position: 'relative' }}>
                  <div style={{
                    position: 'absolute', left: -25, top: 4, width: 8, height: 8, borderRadius: '50%', background: '#1a3d8f',
                  }} />
                  <div style={{ fontSize: 12, color: '#888' }}>{h.processedAt}</div>
                  <div style={{ fontWeight: 'bold', fontSize: 14 }}>{h.actionType}</div>
                  <div style={{ fontSize: 13, color: '#555' }}>{h.description}</div>
                  <div style={{ fontSize: 12, color: '#888' }}>처리자: {h.processedBy}</div>
                </div>
              ))}
              {history.length === 0 && <p style={{ color: '#888' }}>이력이 없습니다.</p>}
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
