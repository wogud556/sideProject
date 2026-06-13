import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { signup } from '../api/screening_api'
import { PATH } from '../router/path'

export default function Signup() {
  const navigate = useNavigate()
  const [form, setForm] = useState({ userId: '', password: '', userName: '', phoneNumber: '' })
  const [error, setError] = useState('')

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    try {
      await signup(form)
      alert('회원가입이 완료되었습니다. 로그인해 주세요.')
      navigate(PATH.LOGIN)
    } catch {
      setError('이미 사용 중인 아이디이거나 오류가 발생했습니다.')
    }
  }

  const handleChange = (field: string) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setForm(p => ({ ...p, [field]: e.target.value }))

  return (
    <div style={containerStyle}>
      <h2>회원가입</h2>
      <form onSubmit={handleSubmit} style={formStyle}>
        <input style={inputStyle} placeholder="아이디" value={form.userId} onChange={handleChange('userId')} required />
        <input style={inputStyle} type="password" placeholder="비밀번호" value={form.password} onChange={handleChange('password')} required />
        <input style={inputStyle} placeholder="이름" value={form.userName} onChange={handleChange('userName')} required />
        <input style={inputStyle} placeholder="휴대폰 번호" value={form.phoneNumber} onChange={handleChange('phoneNumber')} />
        {error && <p style={{ color: 'red', margin: 0 }}>{error}</p>}
        <button style={submitStyle} type="submit">가입하기</button>
      </form>
      <p style={{ marginTop: 16, color: '#666' }}>
        이미 계정이 있으신가요? <Link to={PATH.LOGIN} style={{ color: '#1a73e8' }}>로그인</Link>
      </p>
    </div>
  )
}

const containerStyle: React.CSSProperties = { maxWidth: 400, margin: '80px auto', padding: '0 20px', textAlign: 'center' }
const formStyle: React.CSSProperties = { display: 'flex', flexDirection: 'column', gap: 12 }
const inputStyle: React.CSSProperties = { padding: '10px 14px', border: '1px solid #ddd', borderRadius: 8, fontSize: 15 }
const submitStyle: React.CSSProperties = { padding: '12px', background: '#34a853', color: '#fff', border: 'none', borderRadius: 8, fontSize: 16, cursor: 'pointer' }
