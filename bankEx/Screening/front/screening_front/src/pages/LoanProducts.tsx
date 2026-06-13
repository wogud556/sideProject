import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getProducts, LoanProduct } from '../api/screening_api'

export default function LoanProducts() {
  const navigate = useNavigate()
  const [products, setProducts] = useState<LoanProduct[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    getProducts().then(res => {
      setProducts(res.data)
      setLoading(false)
    })
  }, [])

  const handleApply = (productId: number) => {
    const userId = sessionStorage.getItem('userId')
    if (!userId) {
      alert('로그인이 필요합니다.')
      navigate('/login')
      return
    }
    navigate(`/apply/${productId}`)
  }

  if (loading) return <p style={{ textAlign: 'center', marginTop: 80 }}>불러오는 중...</p>

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
