import { BrowserRouter, Routes, Route } from 'react-router-dom'
import Home from '../pages/common/Home'
import Login from '../pages/login/Login'
import Signup from '../pages/login/Signup'
import MyPage from '../pages/common/MyPage'
import Loan from '../pages/loan/general/loan'
import JeonseLoan from '../pages/loan/jeonse/JeonseLoan'
import MyLoans from '../pages/loan/general/MyLoans'
import AccountDetail from '../pages/deposit/general/AccountDetail'
import { path } from './path'

export default function AppRouter() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path={path.login} element={<Login />} />
        <Route path={path.signup} element={<Signup />} />
        <Route path={path.home} element={<Home />} />
        <Route path={path.mypage} element={<MyPage />} />
        <Route path={path.loan} element={<Loan />} />
        <Route path={path.jeonseLoan} element={<JeonseLoan />} />
        <Route path={path.myLoans} element={<MyLoans />} />
        <Route path={path.accountDetail} element={<AccountDetail />} />
      </Routes>
    </BrowserRouter>
  )
}
