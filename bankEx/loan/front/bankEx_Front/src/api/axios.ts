import axios from 'axios'

export const api = axios.create({
    baseURL: `http://${window.location.hostname}:8080/api/bank/user`,
    timeout: 5000,
    headers : {
        'Content-Type' : 'application/json'
    },
})