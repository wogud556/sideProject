import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { applyLoan, runScreening } from '../api/screening_api'
import { useAuthStore } from '../stores/authStore'
import { useLoanProductStore } from '../stores/loanProductStore'
import { useLoanApplicationStore } from '../stores/loanApplicationStore'

export default function LoanApplication() {
  const { productId } = useParams<{ productId: string }>()
  const navigate = useNavigate()
  const { userId, isLoggedIn } = useAuthStore()
  const { selectedProduct } = useLoanProductStore()
  const { setCurrentApplication, setScreeningResult } = useLoanApplicationStore()
  const [amount, setAmount] = useState('')
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (!isLoggedIn) { navigate('/login'); return }
    if (!selectedProduct) { navigate('/products'); return }
  }, [isLoggedIn, selectedProduct, navigate])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true)
    try {
      const applyRes = await applyLoan({
        userId: userId!,
        productId: Number(productId),
        requestAmount: Number(amount),
      })
      setCurrentApplication(applyRes.data)

      const screenRes = await runScreening(applyRes.data.applicationId)
      setScreeningResult(screenRes.data)

      navigate(`/result/${applyRes.data.applicationId}`)
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: string } })?.response?.data
      alert(msg ?? '신청 중 오류가 발생했습니다.')
    } finally {
      setLoading(false)
    }
  }

  if (!selectedProduct) return null

  return (
    <div style={{ maxWidth: 480, margin: '60px auto', padding: '0 20px' }}>
      <h2>대출 신청</h2>
      <div style={infoBox}>
        <p><strong>{selectedProduct.productName}</strong> ({selectedProduct.productType})</p>
        <p>금리 {selectedProduct.minInterestRate}% ~ {selectedProduct.maxInterestRate}%</p>
        <p>최대 {(selectedProduct.maxLimitAmount / 10000).toLocaleString()}만원 / {selectedProduct.loanPeriodMonths}개월</p>
      </div>
      <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 12, marginTop: 24 }}>
        <label style={{ fontWeight: 600 }}>신청 금액 (원)</label>
        <input
          style={inputStyle}
          type="number"
          placeholder={`최대 ${selectedProduct.maxLimitAmount.toLocaleString()}원`}
          value={amount}
          onChange={e => setAmount(e.target.value)}
          min={100000}
          max={selectedProduct.maxLimitAmount}
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
