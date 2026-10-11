import { BrowserRouter, Routes, Route } from 'react-router-dom'
import { PATH } from './path'
import ScreeningQueue from '../pages/ScreeningQueue'
import ScreeningDetail from '../pages/ScreeningDetail'
import NewScreening from '../pages/NewScreening'

export default function AppRouter() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path={PATH.QUEUE} element={<ScreeningQueue />} />
        <Route path={PATH.NEW} element={<NewScreening />} />
        <Route path={PATH.DETAIL} element={<ScreeningDetail />} />
      </Routes>
    </BrowserRouter>
  )
}
