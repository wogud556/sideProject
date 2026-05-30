import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Input from '../components/Input'
import { loginApi } from '../api/bank_api'
import { path } from '../router/path'

export default function Login() {
  const navigate = useNavigate()
  const [id, setId] = useState('')
  const [pw, setPw] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const handleLogin = async () => {
    if (!id || !pw) {
      setError('아이디와 비밀번호를 입력해주세요')
      return
    }

    setLoading(true)
    setError('')

    try {
      const data = await loginApi(id, pw)
      localStorage.setItem('user', JSON.stringify({ userId: data.userId, userName: data.userName }))
      navigate(path.home)
    } catch (err: any) {
      setError(err?.response?.data || '로그인에 실패했습니다')
    } finally {
      setLoading(false)
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

        <button
          onClick={handleLogin}
          disabled={loading}
          style={{ ...styles.button, opacity: loading ? 0.7 : 1 }}
        >
          {loading ? '로그인 중...' : '로그인'}
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
