import { BrowserRouter, Routes, Route } from 'react-router-dom'
import Home from '../pages/Home'
import Login from '../pages/Login'
import Signup from '../pages/Signup'
import MyPage from '../pages/MyPage'
import Loan from '../pages/loan'
import MyLoans from '../pages/MyLoans'
import AccountDetail from '../pages/AccountDetail'
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
        <Route path={path.myLoans} element={<MyLoans />} />
        <Route path={path.accountDetail} element={<AccountDetail />} />
      </Routes>
    </BrowserRouter>
  )
}
