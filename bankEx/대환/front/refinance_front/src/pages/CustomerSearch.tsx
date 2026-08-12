import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { searchCustomersApi, type CustomerResponse } from '../api/refinance_api'
import { useCustomerSearchStore } from '../stores/customerSearchStore'
import { PATH } from '../router/path'

export default function CustomerSearch() {
  const navigate = useNavigate()
  const setSelectedCustomer = useCustomerSearchStore((s) => s.setSelectedCustomer)

  const [customerNo, setCustomerNo] = useState('')
  const [name, setName] = useState('')
  const [birthDate, setBirthDate] = useState('')
  const [phone, setPhone] = useState('')
  const [results, setResults] = useState<CustomerResponse[]>([])
  const [searched, setSearched] = useState(false)
  const [loading, setLoading] = useState(false)

  const search = async () => {
    setLoading(true)
    try {
      const data = await searchCustomersApi({
        customerNo: customerNo || undefined,
        name: name || undefined,
        birthDate: birthDate || undefined,
        phone: phone || undefined,
      })
      setResults(data)
      setSearched(true)
    } catch {
      alert('고객 조회 중 오류가 발생했습니다.')
    } finally {
      setLoading(false)
    }
  }

  const select = (customer: CustomerResponse) => {
    setSelectedCustomer(customer)
    navigate(PATH.CUSTOMER_LOANS.replace(':customerId', String(customer.customerId)))
  }

  return (
    <div style={{ maxWidth: 720, margin: '40px auto', padding: 24 }}>
      <h1 style={{ fontSize: 20, marginBottom: 16 }}>고객 검색</h1>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8, marginBottom: 12 }}>
        <input placeholder="고객번호" value={customerNo} onChange={(e) => setCustomerNo(e.target.value)} style={inputStyle} />
        <input placeholder="고객명" value={name} onChange={(e) => setName(e.target.value)} style={inputStyle} />
        <input type="date" placeholder="생년월일" value={birthDate} onChange={(e) => setBirthDate(e.target.value)} style={inputStyle} />
        <input placeholder="휴대전화번호 (010-0000-0000)" value={phone} onChange={(e) => setPhone(e.target.value)} style={inputStyle} />
      </div>
      <button onClick={search} disabled={loading} style={buttonStyle}>
        {loading ? '조회 중...' : '조회'}
      </button>

      {searched && (
        <div style={{ marginTop: 24 }}>
          {results.length === 0 ? (
            <p style={{ color: '#888' }}>조회된 고객이 없습니다.</p>
          ) : (
            <table style={{ width: '100%', borderCollapse: 'collapse' }}>
              <thead>
                <tr style={{ borderBottom: '2px solid #d8dce3', textAlign: 'left' }}>
                  <th style={thStyle}>고객번호</th>
                  <th style={thStyle}>성명</th>
                  <th style={thStyle}>생년월</th>
                  <th style={thStyle}>전화번호</th>
                  <th style={thStyle}>상태</th>
                </tr>
              </thead>
              <tbody>
                {results.map((c) => (
                  <tr
                    key={c.customerId}
                    onClick={() => select(c)}
                    style={{ borderBottom: '1px solid #eee', cursor: 'pointer' }}
                  >
                    <td style={tdStyle}>{c.customerNo}</td>
                    <td style={tdStyle}>{c.maskedName}</td>
                    <td style={tdStyle}>{c.maskedBirthDate}</td>
                    <td style={tdStyle}>{c.maskedPhone}</td>
                    <td style={tdStyle}>{c.status}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}
    </div>
  )
}

const inputStyle: React.CSSProperties = {
  padding: '8px 10px',
  border: '1px solid #d8dce3',
  borderRadius: 4,
  fontSize: 14,
}

const buttonStyle: React.CSSProperties = {
  padding: '8px 20px',
  background: '#1a3d8f',
  color: '#fff',
  border: 'none',
  borderRadius: 4,
  cursor: 'pointer',
  fontSize: 14,
}

const thStyle: React.CSSProperties = { padding: '8px 6px', fontSize: 13, color: '#555' }
const tdStyle: React.CSSProperties = { padding: '8px 6px', fontSize: 14 }
