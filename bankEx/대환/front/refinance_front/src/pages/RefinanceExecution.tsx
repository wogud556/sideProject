import { useEffect, useState } from 'react'
import {
  listRefinanceApplicationsApi,
  executeApplicationApi,
  type RefinanceApplicationResponse,
  type RefinanceStatus,
} from '../api/refinance_api'

const EXECUTION_RELATED = new Set<RefinanceStatus>([
  'APPROVED', 'EXECUTING', 'NEW_LOAN_EXECUTED', 'REPAYING', 'COMPLETED', 'FAILED',
])

const STEPS: { key: RefinanceStatus[]; label: string }[] = [
  { key: ['APPROVED'], label: '신청검증' },
  { key: ['EXECUTING'], label: '상환금액 재조회' },
  { key: ['EXECUTING', 'NEW_LOAN_EXECUTED', 'REPAYING', 'COMPLETED'], label: '신규대출 실행' },
  { key: ['REPAYING', 'COMPLETED'], label: '기존대출 상환' },
  { key: ['COMPLETED'], label: '최종확정' },
]

function stepMark(status: RefinanceStatus, stepIndex: number): string {
  const order: RefinanceStatus[] = ['APPROVED', 'EXECUTING', 'NEW_LOAN_EXECUTED', 'REPAYING', 'COMPLETED']
  const currentIdx = status === 'FAILED' ? -1 : order.indexOf(status)
  if (status === 'FAILED') return stepIndex === 0 ? '✓' : '✗'
  if (currentIdx > stepIndex) return '✓'
  if (currentIdx === stepIndex) return '→'
  return '○'
}

export default function RefinanceExecution() {
  const [applications, setApplications] = useState<RefinanceApplicationResponse[]>([])
  const [selected, setSelected] = useState<RefinanceApplicationResponse | null>(null)
  const [busy, setBusy] = useState(false)

  const load = async () => {
    const all = await listRefinanceApplicationsApi()
    const filtered = all.filter((a) => EXECUTION_RELATED.has(a.status))
    setApplications(filtered)
    if (selected) {
      const refreshed = filtered.find((a) => a.applicationId === selected.applicationId)
      if (refreshed) setSelected(refreshed)
    }
  }

  useEffect(() => {
    listRefinanceApplicationsApi().then((all) => {
      setApplications(all.filter((a) => EXECUTION_RELATED.has(a.status)))
    })
  }, [])

  const doExecute = async () => {
    if (!selected) return
    if (!confirm(`신규대출 ${selected.newLoanAmount.toLocaleString()}원을 실행하고\n기존 대출을 상환합니다.\n\n계속 진행하시겠습니까?`)) return
    setBusy(true)
    try {
      const updated = await executeApplicationApi(selected.applicationId)
      setSelected(updated)
      await load()
    } catch {
      alert('실행운영자(ROLE_OPERATOR) 권한이 필요하거나 처리 중 오류가 발생했습니다.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div style={{ maxWidth: 1100, margin: '40px auto', padding: 24, display: 'flex', gap: 24 }}>
      <div style={{ flex: '0 0 320px' }}>
        <h1 style={{ fontSize: 18, marginBottom: 12 }}>대환 실행</h1>
        {applications.length === 0 && <p style={{ color: '#888' }}>실행 대상 신청이 없습니다.</p>}
        <ul style={{ listStyle: 'none', padding: 0 }}>
          {applications.map((a) => (
            <li
              key={a.applicationId}
              onClick={() => setSelected(a)}
              style={{
                padding: 10, border: '1px solid #d8dce3', borderRadius: 6, marginBottom: 8, cursor: 'pointer',
                background: selected?.applicationId === a.applicationId ? '#eef2fb' : '#fff',
              }}
            >
              <div style={{ fontSize: 13, fontWeight: 'bold' }}>{a.applicationNo}</div>
              <div style={{ fontSize: 12, color: a.status === 'FAILED' ? '#c0392b' : '#666' }}>{a.status}</div>
            </li>
          ))}
        </ul>
      </div>

      <div style={{ flex: 1 }}>
        {!selected ? (
          <p style={{ color: '#888' }}>좌측 목록에서 신청 건을 선택하세요.</p>
        ) : (
          <div>
            <h2 style={{ fontSize: 18, marginBottom: 4 }}>{selected.applicationNo}</h2>
            <p style={{ color: '#666', marginBottom: 16 }}>신규대출 {selected.newLoanAmount.toLocaleString()}원</p>

            <div style={{ background: '#fff', border: '1px solid #d8dce3', borderRadius: 6, padding: 16, marginBottom: 16 }}>
              <p style={{ fontWeight: 'bold', marginBottom: 8 }}>대환실행 진행 상황</p>
              {STEPS.map((step, i) => (
                <p key={step.label} style={{ margin: '4px 0', fontSize: 13 }}>
                  {stepMark(selected.status, i)} {step.label}
                </p>
              ))}
              {selected.status === 'FAILED' && (
                <p style={{ color: '#c0392b', marginTop: 8, fontSize: 13 }}>
                  처리 실패 — "실패거래/재처리" 화면에서 재처리할 수 있습니다.
                </p>
              )}
            </div>

            {selected.status === 'APPROVED' && (
              <button disabled={busy} onClick={doExecute} style={buttonStyle}>대환 실행</button>
            )}
          </div>
        )}
      </div>
    </div>
  )
}

const buttonStyle: React.CSSProperties = { padding: '10px 24px', background: '#1a3d8f', color: '#fff', border: 'none', borderRadius: 4, cursor: 'pointer', fontSize: 14 }
