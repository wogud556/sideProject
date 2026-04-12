import { useState } from 'react'
import Input from '../components/Input'

export default function Login() {
  const [id, setId] = useState('')
  const [pw, setPw] = useState('')
  const [error, setError] = useState('')

  const handleLogin = () => {
    // 간단한 검증
    if (!id || !pw) {
      setError('아이디와 비밀번호를 입력해주세요')
      return
    }

    // 임시 로그인 로직
    if (id === 'admin' && pw === '1234') {
      alert('로그인 성공!')
      setError('')
    } else {
      setError('아이디 또는 비밀번호가 틀렸습니다')
    }
  }

  return (
    <div style={styles.container}>
      <div style={styles.card}>
        <h2 style={styles.title}>🏦 BankEx</h2>

        <Input
          placeholder="아이디"
          value={id}
          onChange={(e) => setId(e.target.value)}
        />

        <Input
          type="password"
          placeholder="비밀번호"
          value={pw}
          onChange={(e) => setPw(e.target.value)}
        />

        {error && <p style={styles.error}>{error}</p>}

        <button style={styles.button} onClick={handleLogin}>
          로그인
        </button>
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
  },
  title: {
    textAlign: 'center' as const,
    marginBottom: '20px',
  },
  button: {
    width: '100%',
    padding: '12px',
    borderRadius: '8px',
    border: 'none',
    background: '#2c7be5',
    color: '#fff',
    fontWeight: 'bold',
    cursor: 'pointer',
  },
  error: {
    color: 'red',
    fontSize: '14px',
    marginBottom: '10px',
  },
}