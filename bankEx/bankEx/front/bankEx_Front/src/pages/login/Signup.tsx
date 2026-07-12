import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Input from '../../components/Input'
import { signupApi } from '../../api/bank_api'
import { path } from '../../router/path'

export default function Signup() {
  const navigate = useNavigate()
  const [userId, setUserId] = useState('')
  const [password, setPassword] = useState('')
  const [userName, setUserName] = useState('')
  const [phone, setPhone] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const handleSignup = async () => {
    if (!userId || !password || !userName || !phone) {
      setError('모든 항목을 입력해주세요')
      return
    }

    setLoading(true)
    setError('')

    try {
      const data = await signupApi(userId, password, userName, phone)
      alert(`회원가입 완료!\n계좌번호: ${data.accountNumber}\n로그인 후 이용해주세요.`)
      navigate(path.login)
    } catch (err: any) {
      setError(err?.response?.data || '회원가입에 실패했습니다')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={styles.container}>
      <div style={styles.card}>
        <h2 style={styles.title}>회원가입</h2>
        <p style={styles.subtitle}>BankEx 계정을 만들면 계좌가 자동 생성됩니다</p>

        <Input
          placeholder="아이디"
          value={userId}
          onChange={(e) => setUserId(e.target.value)}
        />
        <Input
          type="password"
          placeholder="비밀번호"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
        <Input
          placeholder="이름"
          value={userName}
          onChange={(e) => setUserName(e.target.value)}
        />
        <Input
          placeholder="연락처 (010-1234-5678)"
          value={phone}
          onChange={(e) => setPhone(e.target.value)}
        />

        {error && <p style={styles.error}>{error}</p>}

        <button
          onClick={handleSignup}
          disabled={loading}
          style={{ ...styles.button, opacity: loading ? 0.7 : 1 }}
        >
          {loading ? '처리 중...' : '가입하기'}
        </button>

        <button onClick={() => navigate(path.login)} style={styles.linkButton}>
          이미 계정이 있으신가요? 로그인
        </button>
      </div>
    </div>
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
    padding: '24px',
    borderRadius: '16px',
    background: '#fff',
    boxShadow: '0 4px 12px rgba(0,0,0,0.1)',
  },
  title: {
    textAlign: 'center' as const,
    marginBottom: '4px',
  },
  subtitle: {
    textAlign: 'center' as const,
    fontSize: '13px',
    color: '#888',
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
    marginTop: '8px',
  },
  linkButton: {
    width: '100%',
    padding: '10px',
    marginTop: '8px',
    border: 'none',
    background: 'none',
    color: '#2c7be5',
    fontSize: '14px',
    cursor: 'pointer',
  },
  error: {
    color: 'red',
    fontSize: '14px',
    marginBottom: '10px',
  },
}
