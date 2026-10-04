import { useEffect, useState } from 'react'
import {
  listRefinanceApplicationsApi,
  reviewApplicationApi,
  approveApplicationApi,
  rejectApplicationApi,
  type RefinanceApplicationResponse,
} from '../api/refinance_api'
import { useOperatorStore } from '../stores/operatorStore'

const REVIEWABLE = new Set(['REQUESTED', 'REVIEWING'])

export default function ApplicationReview() {
  const { role } = useOperatorStore()
  const [applications, setApplications] = useState<RefinanceApplicationResponse[]>([])
  const [selected, setSelected] = useState<RefinanceApplicationResponse | null>(null)
  const [opinion, setOpinion] = useState('')
  const [rejectReason, setRejectReason] = useState('')
  const [busy, setBusy] = useState(false)

  const load = async () => {
    const all = await listRefinanceApplicationsApi()
    setApplications(all.filter((a) => REVIEWABLE.has(a.status)))
  }

  useEffect(() => {
    listRefinanceApplicationsApi().then((all) => {
      setApplications(all.filter((a) => REVIEWABLE.has(a.status)))
    })
  }, [])

  const select = (app: RefinanceApplicationResponse) => {
    setSelected(app)
    setOpinion('')
    setRejectReason('')
  }

  const doReview = async () => {
    if (!selected) return
    setBusy(true)
    try {
      const updated = await reviewApplicationApi(selected.applicationId, opinion)
      setSelected(updated)
      await load()
    } catch {
      alert('심사역(ROLE_REVIEWER) 권한이 필요합니다.')
    } finally {
      setBusy(false)
    }
  }

  const doApprove = async () => {
    if (!selected) return
    if (!confirm(`신청번호 ${selected.applicationNo}\n승인금액 ${selected.requestedAmount.toLocaleString()}원을 승인합니다.\n\n계속 진행하시겠습니까?`)) return
    setBusy(true)
    try {
      const updated = await approveApplicationApi(selected.applicationId, {})
      setSelected(updated)
      await load()
    } catch {
      alert('승인권자(ROLE_APPROVER) 권한이 필요하거나 처리 중 오류가 발생했습니다.')
    } finally {
      setBusy(false)
    }
  }

  const doReject = async () => {
    if (!selected) return
    if (!rejectReason.trim()) {
      alert('거절 사유를 입력해 주세요.')
      return
    }
    if (!confirm(`신청번호 ${selected.applicationNo}을(를) 거절합니다.\n\n계속 진행하시겠습니까?`)) return
    setBusy(true)
    try {
      const updated = await rejectApplicationApi(selected.applicationId, rejectReason)
      setSelected(updated)
      await load()
    } catch {
      alert('승인권자(ROLE_APPROVER) 권한이 필요하거나 처리 중 오류가 발생했습니다.')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div style={{ maxWidth: 1100, margin: '40px auto', padding: 24, display: 'flex', gap: 24 }}>
      <div style={{ flex: '0 0 320px' }}>
        <h1 style={{ fontSize: 18, marginBottom: 12 }}>심사 대기 목록</h1>
        <p style={{ fontSize: 12, color: '#888', marginBottom: 8 }}>현재 직원 role: {role}</p>
        {applications.length === 0 && <p style={{ color: '#888' }}>심사할 신청이 없습니다.</p>}
        <ul style={{ listStyle: 'none', padding: 0 }}>
          {applications.map((a) => (
            <li
              key={a.applicationId}
              onClick={() => select(a)}
              style={{
                padding: 10,
                border: '1px solid #d8dce3',
                borderRadius: 6,
                marginBottom: 8,
                cursor: 'pointer',
                background: selected?.applicationId === a.applicationId ? '#eef2fb' : '#fff',
              }}
            >
              <div style={{ fontSize: 13, fontWeight: 'bold' }}>{a.applicationNo}</div>
              <div style={{ fontSize: 12, color: '#666' }}>{a.status} · {a.requestedAmount.toLocaleString()}원</div>
            </li>
          ))}
        </ul>
      </div>

      <div style={{ flex: 1 }}>
        {!selected ? (
          <p style={{ color: '#888' }}>좌측 목록에서 신청 건을 선택하세요.</p>
        ) : (
          <div>
            <h2 style={{ fontSize: 18, marginBottom: 12 }}>{selected.applicationNo} 심사</h2>

            <section style={sectionBox}>
              <h3 style={sectionHeading}>기존대출</h3>
              {selected.targets.map((t) => (
                <p key={t.targetId} style={{ fontSize: 13 }}>
                  {t.financialInstitutionCode} · {t.maskedLoanAccountNo} · 잔액 {t.loanBalance.toLocaleString()}원 · 상환예정 {t.repaymentAmount.toLocaleString()}원
                </p>
              ))}
            </section>

            <section style={sectionBox}>
              <h3 style={sectionHeading}>신규대출</h3>
              <p style={{ fontSize: 13 }}>{selected.newLoanProductName} · {selected.newLoanAmount.toLocaleString()}원 · {selected.newLoanRate}% · {selected.newLoanPeriodMonths}개월</p>
            </section>

            <section style={sectionBox}>
              <h3 style={sectionHeading}>상태</h3>
              <p style={{ fontSize: 13, fontWeight: 'bold' }}>{selected.status}</p>
            </section>

            {selected.status === 'REQUESTED' && (
              <section style={sectionBox}>
                <h3 style={sectionHeading}>심사의견</h3>
                <textarea value={opinion} onChange={(e) => setOpinion(e.target.value)} rows={3} style={{ width: '100%', padding: 8 }} />
                <button disabled={busy} onClick={doReview} style={buttonStyle}>심사 처리</button>
              </section>
            )}

            {selected.status === 'REVIEWING' && (
              <section style={sectionBox}>
                <h3 style={sectionHeading}>승인 / 거절</h3>
                <button disabled={busy} onClick={doApprove} style={buttonStyle}>승인</button>
                <div style={{ marginTop: 8 }}>
                  <textarea placeholder="거절 사유" value={rejectReason} onChange={(e) => setRejectReason(e.target.value)} rows={2} style={{ width: '100%', padding: 8 }} />
                  <button disabled={busy} onClick={doReject} style={dangerButtonStyle}>거절</button>
                </div>
              </section>
            )}
          </div>
        )}
      </div>
    </div>
  )
}

const sectionBox: React.CSSProperties = { background: '#fff', border: '1px solid #d8dce3', borderRadius: 6, padding: 14, marginBottom: 12 }
const sectionHeading: React.CSSProperties = { fontSize: 13, color: '#555', marginBottom: 6 }
const buttonStyle: React.CSSProperties = { marginTop: 8, padding: '8px 20px', background: '#1a3d8f', color: '#fff', border: 'none', borderRadius: 4, cursor: 'pointer', fontSize: 14 }
const dangerButtonStyle: React.CSSProperties = { ...buttonStyle, marginTop: 8, background: '#c0392b' }
