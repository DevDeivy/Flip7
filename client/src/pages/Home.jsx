import React from 'react'
import mesabg from '../assets/mesa.png'
const Home = () => {
  return (
    <div className='flex h-screen w-screen'>
      <img className='scale-80 rotate-180' src={mesabg} alt="mesabg" />
    </div>
  )
}

export default Home