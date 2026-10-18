import { useEffect, useState } from 'react'
import { Link, Outlet } from 'react-router-dom'
import { getOperators, ROLE_LABEL } from '../api/mortgage_api'
import type { Operator } from '../api/mortgage_api'
import { useOperatorStore } from '../stores/operatorStore'
import { PATH } from '../router/path'

export default function Layout() {
  const { operatorId, setOperatorId } = useOperatorStore()
  const [operators, setOperators] = useState<Operator[]>([])

  useEffect(() => {
    getOperators().then(res => setOperators(res.data)).catch(() => setOperators([]))
  }, [])

  return (
    <>
      <header style={{ background: '#174ea6', color: '#fff', padding: '12px 24px', display: 'flex', alignItems: 'center', gap: 24 }}>
        <strong style={{ fontSize: 17 }}>주택담보대출 여신 업무</strong>
        <nav style={{ display: 'flex', gap: 16, fontSize: 14 }}>
          <Link to={PATH.QUEUE}>업무 대기열</Link>
          <Link to={PATH.POLICIES}>규제 정책</Link>
        </nav>
        <label style={{ marginLeft: 'auto', fontSize: 14 }}>
          담당자{' '}
          <select value={operatorId} onChange={e => setOperatorId(e.target.value)} style={{ padding: 4, borderRadius: 4 }}>
            {operators.map(o => (
              <option key={o.operatorId} value={o.operatorId}>
                {o.operatorId} {o.operatorName} ({ROLE_LABEL[o.role]})
              </option>
            ))}
          </select>
        </label>
      </header>
      <main style={{ maxWidth: 1000, margin: '0 auto', padding: '8px 20px 60px' }}>
        <Outlet />
      </main>
    </>
  )
}
