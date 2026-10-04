import { BrowserRouter, Routes, Route } from 'react-router-dom'
import OperatorSelect from '../pages/OperatorSelect'
import Dashboard from '../pages/Dashboard'
import CustomerSearch from '../pages/CustomerSearch'
import CustomerLoans from '../pages/CustomerLoans'
import RefinanceWizard from '../pages/RefinanceWizard'
import ApplicationReview from '../pages/ApplicationReview'
import RefinanceExecution from '../pages/RefinanceExecution'
import FailureRetry from '../pages/FailureRetry'
import ApplicationHistory from '../pages/ApplicationHistory'
import { PATH } from './path'

export default function AppRouter() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path={PATH.OPERATOR_SELECT} element={<OperatorSelect />} />
        <Route path={PATH.DASHBOARD} element={<Dashboard />} />
        <Route path={PATH.CUSTOMER_SEARCH} element={<CustomerSearch />} />
        <Route path={PATH.CUSTOMER_LOANS} element={<CustomerLoans />} />
        <Route path={PATH.REFINANCE_WIZARD} element={<RefinanceWizard />} />
        <Route path={PATH.REFINANCE_REVIEW} element={<ApplicationReview />} />
        <Route path={PATH.REFINANCE_EXECUTION} element={<RefinanceExecution />} />
        <Route path={PATH.FAILURE_RETRY} element={<FailureRetry />} />
        <Route path={PATH.APPLICATION_HISTORY} element={<ApplicationHistory />} />
      </Routes>
    </BrowserRouter>
  )
}
