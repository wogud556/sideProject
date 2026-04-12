import {useState} from 'react'
import {postUserApi} from '../api/bank_api'

export default function FetchUSerButton() {
    const [loading, setLoading] = useState(false);
    const [user, setUser] = useState<any>(null)
    const [error, setError] = useState('')

    const handleClick = async () => {
        setLoading(true)
        setError('')

        try {
            const data = await postUserApi()
            setUser(data)
        }catch (err : any) {
            setError(
                err?. response?.data?. message || '사용자 정보를 가져오는데 실패했습니다.',
            )
        } finally {
            setLoading(false);
        }
        
    }

    return (
        <div>
            <button
                onClick = {handleClick}
                style={{
                    padding: '10px 16px',
                    background: '#1d4ed8',
                    color: '#fff',
                    border: 'none',
                    borderRadius : '8px',
                    cursor : 'pointer',
                }}
            >
                {loading ? '불러오는 중...' : '사용자 정보 조회'}

                </button>
                {error && (
                    <p style={{color:'red', marginTop: '10px'} }>{error}</p>
                )}

                {user && (
                    <div style={{marginTop: '15px' }}>
                        <p>아이디: {user.userId}</p>
                        <p>이름: {user.userName}</p>
                        <p>잔액: {user.balance.toLocalString()}원</p>
                    </div>
                )

                }
        </div>
    )
}