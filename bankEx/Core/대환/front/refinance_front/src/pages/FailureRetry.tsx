import { useState } from 'react'
import { searchErrorsApi, retryApplicationApi, type ErrorSearchResult } from '../api/refinance_api'

const STATUS_OPTIONS = ['', 'FAILED', 'RETRYING', 'SUCCESS', 'MANUAL_CHECK_REQUIRED']
const STEP_OPTIONS = ['', 'NEW_LOAN_EXECUTION', 'EXISTING_LOAN_REPAYMENT']

export default function FailureRetry() {
  const [applicationNo, setApplicationNo] = useState('')
  const [customerId, setCustomerId] = useState('')
  const [failedStep, setFailedStep] = useState('')
  const [status, setStatus] = useState('')
  const [errorCode, setErrorCode] = useState('')
  const [results, setResults] = useState<ErrorSearchResult[]>([])
  const [busyId, setBusyId] = useState<number | null>(null)

  const search = async () => {
    const data = await searchErrorsApi({
      applicationNo: applicationNo || undefined,
      customerId: customerId ? Number(customerId) : undefined,
      failedStep: failedStep || undefined,
      status: status || undefined,
      errorCode: errorCode || undefined,
    })
    setResults(data)
  }

  const retry = async (row: ErrorSearchResult) => {
    if (!confirm(`${row.applicationNo} 신청 건을 재처리합니다.\n(실패한 단계부터만 재개하며, 이미 성공한 신규대출 실행은 다시 수행하지 않습니다.)\n\n계속 진행하시겠습니까?`)) return
    setBusyId(row.errorId)
    try {
      await retryApplicationApi(row.applicationId)
      alert('재처리가 완료되었습니다.')
      await search()
    } catch {
      alert('재처리 중 오류가 발생했습니다. (실행운영자 권한 필요 / 재처리 불가 상태일 수 있습니다)')
    } finally {
      setBusyId(null)
    }
  }

  return (
    <div style={{ maxWidth: 1000, margin: '40px auto', padding: 24 }}>
      <h1 style={{ fontSize: 20, marginBottom: 16 }}>실패거래 / 재처리</h1>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(5, 1fr)', gap: 8, marginBottom: 12 }}>
        <input placeholder="신청번호" value={applicationNo} onChange={(e) => setApplicationNo(e.target.value)} style={inputStyle} />
        <input placeholder="고객ID" value={customerId} onChange={(e) => setCustomerId(e.target.value)} style={inputStyle} />
        <select value={failedStep} onChange={(e) => setFailedStep(e.target.value)} style={inputStyle}>
          {STEP_OPTIONS.map((s) => <option key={s} value={s}>{s || '거래유형 전체'}</option>)}
        </select>
        <input placeholder="오류코드" value={errorCode} onChange={(e) => setErrorCode(e.target.value)} style={inputStyle} />
        <select value={status} onChange={(e) => setStatus(e.target.value)} style={inputStyle}>
          {STATUS_OPTIONS.map((s) => <option key={s} value={s}>{s || '처리상태 전체'}</option>)}
        </select>
      </div>
      <button onClick={search} style={buttonStyle}>조회</button>

      <table style={{ width: '100%', borderCollapse: 'collapse', marginTop: 20 }}>
        <thead>
          <tr style={{ borderBottom: '2px solid #d8dce3', textAlign: 'left' }}>
            <th style={thStyle}>신청번호</th>
            <th style={thStyle}>거래유형</th>
            <th style={thStyle}>오류코드</th>
            <th style={thStyle}>오류메시지</th>
            <th style={thStyle}>처리상태</th>
            <th style={thStyle}>재시도횟수</th>
            <th style={thStyle}>발생일시</th>
            <th style={thStyle}></th>
          </tr>
        </thead>
        <tbody>
          {results.map((r) => (
            <tr key={r.errorId} style={{ borderBottom: '1px solid #eee' }}>
              <td style={tdStyle}>{r.applicationNo}</td>
              <td style={tdStyle}>{r.failedStep}</td>
              <td style={tdStyle}>{r.errorCode}</td>
              <td style={tdStyle}>{r.errorMessage}</td>
              <td style={{ ...tdStyle, color: r.status === 'SUCCESS' ? '#1a7d3a' : r.status === 'MANUAL_CHECK_REQUIRED' ? '#c0392b' : '#666' }}>{r.status}</td>
              <td style={tdStyle}>{r.retryCount}</td>
              <td style={tdStyle}>{r.createdAt}</td>
              <td style={tdStyle}>
                {(r.status === 'FAILED') && (
                  <button disabled={busyId === r.errorId} onClick={() => retry(r)} style={smallButtonStyle}>재처리</button>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      {results.length === 0 && <p style={{ color: '#888', marginTop: 12 }}>조회 결과가 없습니다.</p>}
    </div>
  )
}

const inputStyle: React.CSSProperties = { padding: '8px 10px', border: '1px solid #d8dce3', borderRadius: 4, fontSize: 13 }
const buttonStyle: React.CSSProperties = { padding: '8px 20px', background: '#1a3d8f', color: '#fff', border: 'none', borderRadius: 4, cursor: 'pointer', fontSize: 14 }
const smallButtonStyle: React.CSSProperties = { padding: '4px 12px', background: '#1a3d8f', color: '#fff', border: 'none', borderRadius: 4, cursor: 'pointer', fontSize: 12 }
const thStyle: React.CSSProperties = { padding: '8px 6px', fontSize: 13, color: '#555' }
const tdStyle: React.CSSProperties = { padding: '8px 6px', fontSize: 13 }
