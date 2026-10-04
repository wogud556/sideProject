import { Link } from 'react-router-dom'
import { PATH } from '../router/path'

export default function Home() {
  return (
    <div style={{ maxWidth: 600, margin: '80px auto', textAlign: 'center', padding: '0 20px' }}>
      <h1 style={{ fontSize: 32, marginBottom: 12 }}>Loan Screening System</h1>
      <p style={{ color: '#666', marginBottom: 40 }}>
        신용대출 / 전세대출 자동 심사 서비스
      </p>
      <div style={{ display: 'flex', gap: 16, justifyContent: 'center' }}>
        <Link to={PATH.LOGIN}>
          <button style={btnStyle('#1a73e8')}>로그인</button>
        </Link>
        <Link to={PATH.SIGNUP}>
          <button style={btnStyle('#34a853')}>회원가입</button>
        </Link>
        <Link to={PATH.PRODUCTS}>
          <button style={btnStyle('#ea4335')}>대출상품 조회</button>
        </Link>
      </div>
    </div>
  )
}

function btnStyle(bg: string): React.CSSProperties {
  return {
    padding: '12px 24px',
    background: bg,
    color: '#fff',
    border: 'none',
    borderRadius: 8,
    fontSize: 16,
    cursor: 'pointer',
  }
}
