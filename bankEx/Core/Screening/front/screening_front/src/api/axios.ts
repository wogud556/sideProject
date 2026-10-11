import axios from 'axios'

const api = axios.create({
  baseURL: 'http://localhost:8081/api/screenings',
  withCredentials: true,
})

export default api
