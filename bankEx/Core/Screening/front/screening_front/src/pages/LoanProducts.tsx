import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { getProducts } from '../api/screening_api'
import { useAuthStore } from '../stores/authStore'
import { useLoanProductStore } from '../stores/loanProductStore'

export default function LoanProducts() {
  const navigate = useNavigate()
  const { isLoggedIn } = useAuthStore()
  const { products, setProducts, setSelectedProduct } = useLoanProductStore()

  useEffect(() => {
    if (products.length === 0) {
      getProducts().then(res => setProducts(res.data))
    }
  }, [products.length, setProducts])

  const handleApply = (productId: number) => {
    if (!isLoggedIn) {
      alert('로그인이 필요합니다.')
      navigate('/login')
      return
    }
    const product = products.find(p => p.productId === productId)
    if (product) setSelectedProduct(product)
    navigate(`/apply/${productId}`)
  }

  if (products.length === 0) return <p style={{ textAlign: 'center', marginTop: 80 }}>불러오는 중...</p>

  return (
    <div style={{ maxWidth: 800, margin: '40px auto', padding: '0 20px' }}>
      <h2>대출 상품 목록</h2>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
        {products.map(p => (
          <div key={p.productId} style={cardStyle}>
            <div>
              <span style={typeBadge(p.productType)}>{p.productType}</span>
              <h3 style={{ margin: '8px 0 4px' }}>{p.productName}</h3>
              <p style={{ color: '#666', margin: 0 }}>
                금리: {p.minInterestRate}% ~ {p.maxInterestRate}% &nbsp;|&nbsp;
                한도: {(p.maxLimitAmount / 10000).toLocaleString()}만원 &nbsp;|&nbsp;
                기간: {p.loanPeriodMonths}개월
              </p>
            </div>
            <button style={applyBtn} onClick={() => handleApply(p.productId)}>신청하기</button>
          </div>
        ))}
      </div>
    </div>
  )
}

const cardStyle: React.CSSProperties = {
  background: '#fff', borderRadius: 12, padding: '20px 24px',
  boxShadow: '0 2px 8px rgba(0,0,0,0.08)', display: 'flex',
  justifyContent: 'space-between', alignItems: 'center',
}
const applyBtn: React.CSSProperties = {
  padding: '10px 20px', background: '#1a73e8', color: '#fff',
  border: 'none', borderRadius: 8, cursor: 'pointer', whiteSpace: 'nowrap',
}
function typeBadge(type: string): React.CSSProperties {
  const bg = type === '전세대출' ? '#e8f0fe' : '#fce8e6'
  const color = type === '전세대출' ? '#1a73e8' : '#ea4335'
  return { padding: '2px 10px', background: bg, color, borderRadius: 20, fontSize: 12, fontWeight: 600 }
}
