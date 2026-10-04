import { useNavigate } from 'react-router-dom'
import { path } from '../../router/path'

export default function Home() {
  const navigate = useNavigate()
  const user = JSON.parse(localStorage.getItem('user') || 'null')

  const handleLogout = () => {
    localStorage.removeItem('user')
    navigate(path.login)
  }

  if (!user) {
    navigate(path.login)
    return null
  }

  return (
    <div style={styles.container}>
      <div style={styles.card}>
        <h2 style={styles.title}>BankEx</h2>
        <p style={styles.welcome}>안녕하세요, <strong>{user.userName}</strong>님!</p>
        <p style={styles.sub}>{user.userId}</p>

        <div style={styles.menuGrid}>
          <MenuButton label="대출 상품" emoji="💰" onClick={() => navigate(path.loan)} />
          <MenuButton label="전세대출" emoji="🏠" onClick={() => navigate(path.jeonseLoan)} />
          <MenuButton label="내 대출 내역" emoji="📋" onClick={() => navigate(path.myLoans)} />
          <MenuButton label="마이페이지" emoji="👤" onClick={() => navigate(path.mypage)} />
        </div>

        <button onClick={handleLogout} style={styles.logoutButton}>로그아웃</button>
      </div>
    </div>
  )
}

function MenuButton({ label, emoji, onClick }: { label: string; emoji: string; onClick: () => void }) {
  return (
    <button onClick={onClick} style={styles.menuItem}>
      <span style={styles.menuEmoji}>{emoji}</span>
      <span style={styles.menuLabel}>{label}</span>
    </button>
  )
}

const styles = {
  container: {
    minHeight: '100vh',
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    background: '#f5f6f8',
  },
  card: {
    width: '320px',
    padding: '28px 24px',
    borderRadius: '16px',
    background: '#fff',
    boxShadow: '0 4px 12px rgba(0,0,0,0.1)',
    textAlign: 'center' as const,
  },
  title: {
    margin: '0 0 16px 0',
    color: '#2c7be5',
  },
  welcome: {
    fontSize: '18px',
    marginBottom: '4px',
  },
  sub: {
    color: '#aaa',
    fontSize: '13px',
    marginBottom: '24px',
  },
  menuGrid: {
    display: 'flex',
    flexDirection: 'column' as const,
    gap: '10px',
    marginBottom: '20px',
  },
  menuItem: {
    display: 'flex',
    alignItems: 'center',
    gap: '12px',
    padding: '14px 16px',
    borderRadius: '10px',
    border: '1px solid #e8e8e8',
    background: '#fff',
    cursor: 'pointer',
    fontSize: '15px',
    textAlign: 'left' as const,
  },
  menuEmoji: {
    fontSize: '20px',
  },
  menuLabel: {
    fontWeight: '500',
  },
  logoutButton: {
    padding: '10px 20px',
    borderRadius: '8px',
    border: 'none',
    background: '#f5f6f8',
    color: '#e53e3e',
    fontWeight: 'bold',
    cursor: 'pointer',
    fontSize: '14px',
  },
}
