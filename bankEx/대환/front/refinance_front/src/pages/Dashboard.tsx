import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useOperatorStore } from '../stores/operatorStore'
import { getDashboardApi, type DashboardResponse } from '../api/refinance_api'
import { PATH } from '../router/path'

export default function Dashboard() {
  const { name, role } = useOperatorStore()
  const [stats, setStats] = useState<DashboardResponse | null>(null)

  useEffect(() => {
    getDashboardApi().then(setStats)
  }, [])

  return (
    <div style={{ maxWidth: 960, margin: '40px auto', padding: 24 }}>
      <h1 style={{ fontSize: 20 }}>대환업무 Dashboard</h1>
      <p style={{ color: '#666', marginBottom: 20 }}>{name} ({role}) 님, 환영합니다.</p>

      {stats && (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(6, 1fr)', gap: 12, marginBottom: 24 }}>
          <StatTile label="금일 신청건수" value={stats.todayApplicationCount} />
          <StatTile label="심사대기" value={stats.reviewingCount} />
          <StatTile label="승인완료" value={stats.approvedCount} />
          <StatTile label="실행대기" value={stats.executionPendingCount} />
          <StatTile label="완료" value={stats.completedCount} color="#1a7d3a" />
          <StatTile label="실패거래" value={stats.failedCount} color="#c0392b" />
        </div>
      )}

      <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
        <Link to={PATH.CUSTOMER_SEARCH} style={linkButtonStyle}>고객 검색 →</Link>
        <Link to={PATH.REFINANCE_REVIEW} style={linkButtonStyle}>심사 →</Link>
        <Link to={PATH.REFINANCE_EXECUTION} style={linkButtonStyle}>실행 →</Link>
        <Link to={PATH.FAILURE_RETRY} style={linkButtonStyle}>실패거래/재처리 →</Link>
        <Link to={PATH.APPLICATION_HISTORY} style={linkButtonStyle}>업무이력 →</Link>
      </div>
    </div>
  )
}

function StatTile({ label, value, color }: { label: string; value: number; color?: string }) {
  return (
    <div style={{ background: '#fff', border: '1px solid #d8dce3', borderRadius: 6, padding: 16 }}>
      <div style={{ fontSize: 12, color: '#888', marginBottom: 6 }}>{label}</div>
      <div style={{ fontSize: 24, fontWeight: 'bold', color: color ?? '#1a1a2e' }}>{value}</div>
    </div>
  )
}

const linkButtonStyle: React.CSSProperties = {
  display: 'inline-block',
  padding: '10px 20px',
  background: '#1a3d8f',
  color: '#fff',
  borderRadius: 4,
  fontSize: 14,
}
