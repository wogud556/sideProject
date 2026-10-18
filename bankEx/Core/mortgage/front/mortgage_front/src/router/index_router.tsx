import { BrowserRouter, Routes, Route } from 'react-router-dom'
import { PATH } from './path'
import Layout from '../components/Layout'
import ApplicationQueue from '../pages/ApplicationQueue'
import ApplicationDetail from '../pages/ApplicationDetail'
import PolicyList from '../pages/PolicyList'
import LoanAccountDetail from '../pages/LoanAccountDetail'

export default function AppRouter() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<Layout />}>
          <Route path={PATH.QUEUE} element={<ApplicationQueue />} />
          <Route path={PATH.DETAIL} element={<ApplicationDetail />} />
          <Route path={PATH.POLICIES} element={<PolicyList />} />
          <Route path={PATH.LOAN_ACCOUNT} element={<LoanAccountDetail />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}
