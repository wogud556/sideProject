import { useNavigate } from 'react-router-dom'
import { useOperatorStore, type OperatorRole } from '../stores/operatorStore'
import { PATH } from '../router/path'

// 경량 권한모델 데모용 — 백엔드 DataInitializer가 시딩하는 직원 5명과 1:1로 대응한다. 실제 로그인이 아니다.
const DEMO_OPERATORS: { operatorId: string; name: string; role: OperatorRole; roleLabel: string }[] = [
  { operatorId: 'teller01', name: '창구직원', role: 'ROLE_TELLER', roleLabel: '일반업무' },
  { operatorId: 'reviewer01', name: '심사역', role: 'ROLE_REVIEWER', roleLabel: '심사' },
  { operatorId: 'approver01', name: '승인권자', role: 'ROLE_APPROVER', roleLabel: '승인' },
  { operatorId: 'operator01', name: '실행운영자', role: 'ROLE_OPERATOR', roleLabel: '실행/재처리' },
  { operatorId: 'admin01', name: '관리자', role: 'ROLE_ADMIN', roleLabel: '관리자' },
]

export default function OperatorSelect() {
  const navigate = useNavigate()
  const setOperator = useOperatorStore((s) => s.setOperator)

  const select = (op: (typeof DEMO_OPERATORS)[number]) => {
    setOperator(op.operatorId, op.name, op.role)
    navigate(PATH.DASHBOARD)
  }

  return (
    <div style={{ maxWidth: 480, margin: '80px auto', padding: 24 }}>
      <h1 style={{ fontSize: 20, marginBottom: 4 }}>가계대출 대환 업무 시스템</h1>
      <p style={{ color: '#666', marginBottom: 24 }}>업무를 진행할 직원을 선택하세요. (데모용 — 실제 로그인 아님)</p>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
        {DEMO_OPERATORS.map((op) => (
          <button
            key={op.operatorId}
            onClick={() => select(op)}
            style={{
              textAlign: 'left',
              padding: '12px 16px',
              border: '1px solid #d8dce3',
              borderRadius: 6,
              background: '#fff',
              cursor: 'pointer',
              fontSize: 14,
            }}
          >
            <strong>{op.name}</strong>
            <span style={{ color: '#888', marginLeft: 8 }}>{op.operatorId} · {op.roleLabel}</span>
          </button>
        ))}
      </div>
    </div>
  )
}
