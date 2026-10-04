import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { getCustomerApi, getCustomerLoansApi, type CustomerResponse, type ExistingLoanResponse } from '../api/refinance_api'
import { useRefinanceWizardStore } from '../stores/refinanceWizardStore'
import { PATH } from '../router/path'

export default function CustomerLoans() {
  const { customerId } = useParams<{ customerId: string }>()
  const navigate = useNavigate()
  const setCustomer = useRefinanceWizardStore((s) => s.setCustomer)
  const setSelectedLoanIds = useRefinanceWizardStore((s) => s.setSelectedLoanIds)

  const [customer, setCustomerInfo] = useState<CustomerResponse | null>(null)
  const [loans, setLoans] = useState<ExistingLoanResponse[]>([])
  const [checked, setChecked] = useState<Set<number>>(new Set())
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!customerId) return
    const id = Number(customerId)
    Promise.all([getCustomerApi(id), getCustomerLoansApi(id)])
      .then(([c, l]) => {
        setCustomerInfo(c)
        setLoans(l)
      })
      .finally(() => setLoading(false))
  }, [customerId])

  const toggle = (loanId: number, eligible: boolean) => {
    if (!eligible) return
    setChecked((prev) => {
      const next = new Set(prev)
      if (next.has(loanId)) next.delete(loanId)
      else next.add(loanId)
      return next
    })
  }

  const proceed = () => {
    if (!customerId || checked.size === 0) return
    setCustomer(Number(customerId))
    setSelectedLoanIds(Array.from(checked))
    navigate(PATH.REFINANCE_WIZARD)
  }

  if (loading) return <div style={{ padding: 24 }}>불러오는 중...</div>
  if (!customer) return <div style={{ padding: 24 }}>고객 정보를 찾을 수 없습니다.</div>

  return (
    <div style={{ maxWidth: 900, margin: '40px auto', padding: 24 }}>
      <h1 style={{ fontSize: 20, marginBottom: 4 }}>고객 대출현황</h1>
      <p style={{ color: '#666', marginBottom: 20 }}>
        {customer.maskedName} ({customer.customerNo}) · {customer.maskedPhone}
      </p>

      {loans.length === 0 ? (
        <p style={{ color: '#888' }}>보유 대출이 없습니다.</p>
      ) : (
        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
          <thead>
            <tr style={{ borderBottom: '2px solid #d8dce3', textAlign: 'left' }}>
              <th style={thStyle}></th>
              <th style={thStyle}>금융기관</th>
              <th style={thStyle}>계좌번호</th>
              <th style={thStyle}>상품명</th>
              <th style={thStyle}>대출잔액</th>
              <th style={thStyle}>금리</th>
              <th style={thStyle}>상환방식</th>
              <th style={thStyle}>연체</th>
              <th style={thStyle}>대환가능</th>
            </tr>
          </thead>
          <tbody>
            {loans.map((loan) => (
              <tr key={loan.loanId} style={{ borderBottom: '1px solid #eee', opacity: loan.refinanceEligible ? 1 : 0.5 }}>
                <td style={tdStyle}>
                  <input
                    type="checkbox"
                    checked={checked.has(loan.loanId)}
                    disabled={!loan.refinanceEligible}
                    onChange={() => toggle(loan.loanId, loan.refinanceEligible)}
                  />
                </td>
                <td style={tdStyle}>{loan.financialInstitutionName}</td>
                <td style={tdStyle}>{loan.maskedLoanAccountNo}</td>
                <td style={tdStyle}>{loan.loanProductName}</td>
                <td style={tdStyle}>{loan.currentBalance.toLocaleString()}원</td>
                <td style={tdStyle}>{loan.interestRate}%</td>
                <td style={tdStyle}>{loan.repaymentMethod}</td>
                <td style={tdStyle}>{loan.overdue ? 'Y' : 'N'}</td>
                <td style={tdStyle}>{loan.refinanceEligible ? '가능' : '불가'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      <button onClick={proceed} disabled={checked.size === 0} style={buttonStyle}>
        선택한 대출 대환 검토 ({checked.size}건)
      </button>
    </div>
  )
}

const thStyle: React.CSSProperties = { padding: '8px 6px', fontSize: 13, color: '#555' }
const tdStyle: React.CSSProperties = { padding: '8px 6px', fontSize: 14 }
const buttonStyle: React.CSSProperties = {
  marginTop: 20,
  padding: '10px 24px',
  background: '#1a3d8f',
  color: '#fff',
  border: 'none',
  borderRadius: 4,
  cursor: 'pointer',
  fontSize: 14,
}
