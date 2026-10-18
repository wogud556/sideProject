import type { ApplicationDetail } from '../../api/mortgage_api'

export interface PanelProps {
  detail: ApplicationDetail
  /** 처리 결과로 받은 최신 상세를 화면에 반영한다 */
  onUpdated: (detail: ApplicationDetail) => void
}
