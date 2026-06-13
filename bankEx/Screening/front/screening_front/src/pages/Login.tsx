import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { login } from '../api/screening_api'
import { PATH } from '../router/path'
import { useAuthStore } from '../stores/authStore'

export default function Login() {
  const navigate = useNavigate()
  const setAuth = useAuthStore(s => s.setAuth)
  const [form, setForm] = useState({ userId: '', password: '' })
  const [error, setError] = useState('')

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    try {
      const res = await login(form)
      setAuth(res.data.userId, res.data.userName)
      navigate(PATH.PRODUCTS)
    } catch {
      setError('아이디 또는 비밀번호가 올바르지 않습니다.')
    }
  }

  return (
    <div style={containerStyle}>
      <h2>로그인</h2>
      <form onSubmit={handleSubmit} style={formStyle}>
        <input
          style={inputStyle}
          placeholder="아이디"
          value={form.userId}
          onChange={e => setForm(p => ({ ...p, userId: e.target.value }))}
          required
        />
        <input
          style={inputStyle}
          type="password"
          placeholder="비밀번호"
          value={form.password}
          onChange={e => setForm(p => ({ ...p, password: e.target.value }))}
          required
        />
        {error && <p style={{ color: 'red', margin: 0 }}>{error}</p>}
        <button style={submitStyle} type="submit">로그인</button>
      </form>
      <p style={{ marginTop: 16, color: '#666' }}>
        계정이 없으신가요? <Link to={PATH.SIGNUP} style={{ color: '#1a73e8' }}>회원가입</Link>
      </p>
      <p style={{ color: '#999', fontSize: 13 }}>테스트 계정: test01 / 1234</p>
    </div>
  )
}

const containerStyle: React.CSSProperties = { maxWidth: 400, margin: '80px auto', padding: '0 20px', textAlign: 'center' }
const formStyle: React.CSSProperties = { display: 'flex', flexDirection: 'column', gap: 12 }
const inputStyle: React.CSSProperties = { padding: '10px 14px', border: '1px solid #ddd', borderRadius: 8, fontSize: 15 }
const submitStyle: React.CSSProperties = { padding: '12px', background: '#1a73e8', color: '#fff', border: 'none', borderRadius: 8, fontSize: 16, cursor: 'pointer' }
