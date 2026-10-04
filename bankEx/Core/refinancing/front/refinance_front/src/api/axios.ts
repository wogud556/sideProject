import axios from 'axios'
import { useOperatorStore } from '../stores/operatorStore'

const api = axios.create({
  baseURL: 'http://localhost:8082/api',
})

api.interceptors.request.use((config) => {
  const operatorId = useOperatorStore.getState().operatorId
  if (operatorId) {
    config.headers['X-Operator-Id'] = operatorId
  }
  return config
})

export default api
