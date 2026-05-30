import { useNavigate } from 'react-router-dom'
import { path } from '../router/path'

export default function Home() {
  const navigate = useNavigate()
  const user = JSON.parse(localStorage.getItem('user') || 'null')

  const handleLogout = () => {
    localStorage.removeItem('user')
    navigate(path.login)
  }

  return (
    <div style={styles.container}>
      <div style={styles.card}>
        <h2 style={styles.title}>🏦 BankEx</h2>
        {user ? (
          <>
            <p style={styles.welcome}>안녕하세요, <strong>{user.userName}</strong>님!</p>
            <p style={styles.sub}>아이디: {user.userId}</p>
            <button onClick={handleLogout} style={styles.button}>로그아웃</button>
          </>
        ) : (
          <p>로그인이 필요합니다.</p>
        )}
      </div>
    </div>
  )
}

const styles = {
  container: {
    height: '100vh',
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    background: '#f5f6f8',
  },
  card: {
    width: '320px',
    padding: '24px',
    borderRadius: '16px',
    background: '#fff',
    boxShadow: '0 4px 12px rgba(0,0,0,0.1)',
    textAlign: 'center' as const,
  },
  title: {
    marginBottom: '20px',
  },
  welcome: {
    fontSize: '18px',
    marginBottom: '8px',
  },
  sub: {
    color: '#888',
    marginBottom: '20px',
  },
  button: {
    padding: '10px 20px',
    borderRadius: '8px',
    border: 'none',
    background: '#e53e3e',
    color: '#fff',
    fontWeight: 'bold',
    cursor: 'pointer',
  },
}
