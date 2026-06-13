import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { applyLoan, getProduct, LoanProduct, runScreening } from '../api/screening_api'

export default function LoanApplication() {
  const { productId } = useParams<{ productId: string }>()
  const navigate = useNavigate()
  const [product, setProduct] = useState<LoanProduct | null>(null)
  const [amount, setAmount] = useState('')
  const [loading, setLoading] = useState(false)
  const userId = sessionStorage.getItem('userId') ?? ''

  useEffect(() => {
    if (productId) {
      getProduct(Number(productId)).then(res => setProduct(res.data))
    }
  }, [productId])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!userId) { alert('로그인이 필요합니다.'); navigate('/login'); return }
    setLoading(true)
    try {
      const applyRes = await applyLoan({
        userId,
        productId: Number(productId),
        requestAmount: Number(amount),
      })
      const applicationId = applyRes.data.applicationId
      await runScreening(applicationId)
      navigate(`/result/${applicationId}`)
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: string } })?.response?.data
      alert(msg ?? '신청 중 오류가 발생했습니다.')
    } finally {
      setLoading(false)
    }
  }

  if (!product) return <p style={{ textAlign: 'center', marginTop: 80 }}>불러오는 중...</p>

  return (
    <div style={{ maxWidth: 480, margin: '60px auto', padding: '0 20px' }}>
      <h2>대출 신청</h2>
      <div style={infoBox}>
        <p><strong>{product.productName}</strong> ({product.productType})</p>
        <p>금리 {product.minInterestRate}% ~ {product.maxInterestRate}%</p>
        <p>최대 {(product.maxLimitAmount / 10000).toLocaleString()}만원 / {product.loanPeriodMonths}개월</p>
      </div>
      <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 12, marginTop: 24 }}>
        <label style={{ fontWeight: 600 }}>신청 금액 (원)</label>
        <input
          style={inputStyle}
          type="number"
          placeholder={`최대 ${product.maxLimitAmount.toLocaleString()}원`}
          value={amount}
          onChange={e => setAmount(e.target.value)}
          min={100000}
          max={product.maxLimitAmount}
          required
        />
        <button style={submitStyle} type="submit" disabled={loading}>
          {loading ? '심사 중...' : '신청 및 심사 실행'}
        </button>
      </form>
    </div>
  )
}

const infoBox: React.CSSProperties = {
  background: '#f0f4ff', borderRadius: 10, padding: '16px 20px', lineHeight: 1.8,
}
const inputStyle: React.CSSProperties = {
  padding: '10px 14px', border: '1px solid #ddd', borderRadius: 8, fontSize: 15,
}
const submitStyle: React.CSSProperties = {
  padding: '13px', background: '#1a73e8', color: '#fff', border: 'none',
  borderRadius: 8, fontSize: 16, cursor: 'pointer',
}
