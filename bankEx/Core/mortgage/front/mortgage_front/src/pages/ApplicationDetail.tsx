import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getApplication, errorMessage, won, STATUS_COLOR } from '../api/mortgage_api'
import type { ApplicationDetail as Detail } from '../api/mortgage_api'
import { PATH } from '../router/path'
import { Badge, Card, ErrorText, Rows } from '../components/ui'
import CollateralPanel from '../components/panels/CollateralPanel'
import LimitPanel from '../components/panels/LimitPanel'
import ScreeningPanel from '../components/panels/ScreeningPanel'
import ApprovalPanel from '../components/panels/ApprovalPanel'
import ExecutionPanel from '../components/panels/ExecutionPanel'

export default function ApplicationDetail() {
  const { applicationId } = useParams<{ applicationId: string }>()
  const [detail, setDetail] = useState<Detail | null>(null)
  const [error, setError] = useState('')

  useEffect(() => {
    getApplication(Number(applicationId))
      .then(res => setDetail(res.data))
      .catch(e => setError(errorMessage(e)))
  }, [applicationId])

  if (!detail) return error ? <ErrorText message={error} /> : <p>불러오는 중...</p>

  const a = detail.application
  const panelProps = { detail, onUpdated: setDetail }

  return (
    <>
      <p><Link to={PATH.QUEUE} style={{ color: '#1a73e8', fontSize: 14 }}>← 대기열</Link></p>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
        <h2 style={{ margin: 0 }}>{a.applicationNo}</h2>
        <Badge color={STATUS_COLOR[a.status]}>{a.statusLabel}</Badge>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
        <Card title="신청 정보">
          <Rows rows={[
            ['고객', `${a.customerName} (${a.customerId})`],
            ['상품', a.productCode],
            ['신청 금액', won(a.requestedAmount)],
            ['기간 / 상환 방식', `${detail.termMonths}개월 / ${detail.repaymentMethod}`],
            ['대출 목적', detail.loanPurpose],
            ['보유 주택 / 차주 유형', `${detail.houseCount}채 / ${detail.borrowerType}`],
          ]} />
        </Card>
        <Card title="담보 주택 · 소득">
          <Rows rows={[
            ['주소', detail.property.address],
            ['지역 / 유형', `${detail.property.regionCode} / ${detail.property.propertyType}`],
            ['고객 제시 가격', won(detail.property.declaredPrice)],
            ['연소득', `${won(detail.income.annualIncome)} (${detail.income.incomeType})`],
            ['기존 부채', detail.debts.length === 0 ? '없음'
              : detail.debts.map(d => `${d.debtType} 잔액 ${won(d.balance)}, 연 원리금 ${won(d.annualPrincipal + d.annualInterest)}`).join(' / ')],
          ]} />
        </Card>
      </div>

      <CollateralPanel {...panelProps} />
      {(detail.limit || detail.valuations.length > 0) && <LimitPanel {...panelProps} />}
      {(detail.limit || detail.screenings.length > 0) && <ScreeningPanel {...panelProps} />}
      {(detail.approval || ['SCREENING_APPROVED', 'CONDITIONALLY_APPROVED'].includes(a.status)) && <ApprovalPanel {...panelProps} />}
      {(detail.executions.length > 0 || a.status === 'EXECUTION_READY') && <ExecutionPanel {...panelProps} />}
    </>
  )
}
