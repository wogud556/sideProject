import { BrowserRouter, Routes, Route } from 'react-router-dom'
import { PATH } from './path'
import Home from '../pages/Home'
import Login from '../pages/Login'
import Signup from '../pages/Signup'
import LoanProducts from '../pages/LoanProducts'
import LoanApplication from '../pages/LoanApplication'
import ScreeningResult from '../pages/ScreeningResult'
import MyApplications from '../pages/MyApplications'
import MyPage from '../pages/MyPage'

export default function AppRouter() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path={PATH.HOME} element={<Home />} />
        <Route path={PATH.LOGIN} element={<Login />} />
        <Route path={PATH.SIGNUP} element={<Signup />} />
        <Route path={PATH.PRODUCTS} element={<LoanProducts />} />
        <Route path={PATH.APPLY} element={<LoanApplication />} />
        <Route path={PATH.RESULT} element={<ScreeningResult />} />
        <Route path={PATH.MY_APPLICATIONS} element={<MyApplications />} />
        <Route path={PATH.MY_PAGE} element={<MyPage />} />
      </Routes>
    </BrowserRouter>
  )
}
