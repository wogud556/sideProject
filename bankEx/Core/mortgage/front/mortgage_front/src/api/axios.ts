import axios from 'axios'
import { useOperatorStore } from '../stores/operatorStore'

const api = axios.create({
  baseURL: 'http://localhost:8084/api/v1/mortgages',
})

// 인증은 없다. 담당자 업무 API는 선택한 담당자 ID를 헤더로 보내고, Core가 역할을 확인한다
api.interceptors.request.use((config) => {
  const operatorId = useOperatorStore.getState().operatorId
  if (operatorId) config.headers['X-Operator-Id'] = operatorId
  return config
})

export default api
