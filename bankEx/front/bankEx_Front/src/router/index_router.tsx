import {BrowserRouter, Routes, Route} from 'react-router-dom'
import Home from '../pages/Home'
import Login from '../pages/Login'
import MyPage from '../pages/MyPage'
import {path} from './path'

export default function AppRouter() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path={path.home} element={<Home/>} />
        <Route path={path.login} element={<Login />} />
        <Route path={path.mypage} element={<MyPage />} />
      </Routes>
    </BrowserRouter>
  )
}