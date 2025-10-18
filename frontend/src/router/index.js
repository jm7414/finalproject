import { createRouter, createWebHistory } from 'vue-router'
import { isAuthenticated, getCurrentUser, getDefaultRouteByRole } from '@/utils/auth'
import DP_main from '@/views/DP_main.vue'
import GD_main from '@/views/GD_main.vue'
import GeoFencingView from '@/views/GeoFencingView.vue'
import SearchRouteView from '@/views/SearchRouteView.vue'
import Signup from '@/views/SignUp.vue'
import Login from '@/views/Login.vue'
import CommunityView from '@/views/CommunityView.vue'
import PredictLocation from '@/views/PredictLocation.vue'
import CommunityMissing from '@/components/CommunityMissing.vue'
import CommunityPostWrite from '@/components/CommunityPostWrite.vue'
import CommunityPost from '@/components/CommunityPost.vue'
import CommunityEvent from '@/components/CommunityEvent.vue'
import TotalSupport from '@/views/TotalSupport.vue'
import MoneySupport from '@/views/MoneySupport.vue'
import Record from '@/views/Record.vue'
import Report from '@/views/Report.vue'
import GdMypageView from '@/views/GdMypageView.vue'
import Basicplan from '@/views/Basicplan.vue'
import Plusplan from '@/views/Plusplan.vue'
import Calender from '@/views/Calender.vue'
import AddSchedule from '@/views/AddSchedule.vue'
import DP_Connect from '@/views/DP_Connect.vue'
import GD_Connect from '@/views/GD_Connect.vue'
import BasicSafeZoneLocationView from '@/views/BasicSafeZoneLocationView.vue'
import BasicSafeZoneRadiusView from '@/views/BasicSafeZoneRadiusView.vue'
import MapMain from '@/views/MapMain.vue'
import Pr from '@/views/Pr.vue'
import Game from '@/views/Game.vue'
zz
import Loan from '@/views/Loan.vue'
import Insurance from '@/views/Insurance.vue'
import HeartCare from '@/views/heartCare.vue'
import DP_schedule from '@/views/DP_schedule.vue'
import Benefit from '@/views/Benefit.vue'
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      redirect: '/login',
    },
    {
      path: '/gdc',
      name: 'gdc',
      component: GD_Connect,
    },
     {
      path: '/pr',
      name: 'pr',
      component: Pr,
    },
    {
      path: '/game',
      name: 'game',
      component: Game,
    },
     {
      path: '/dpc',
      name: 'dpc',
      component: DP_Connect,
    },
    {
      path: '/DP',
      name: 'DP',
      component: DP_main,
      meta: { requiresAuth: true, roles: [2] } // 환자 전용 (roleNo: 2)
    },
    {
      path: '/GD',
      name: 'GD',
      component: GD_main,
      meta: { requiresAuth: true, roles: [1, 3] } // 보호자(1), 구독자(3) 전용
    },
    {
      path: '/SignUp',
      name: 'SignUp',
      component: Signup,
      meta: { requiresGuest: true }
    },
    {
      path: '/login',
      name: 'login',
      component: Login,
      meta: { requiresGuest: true }
    },

    // 병욱 게시판 시작
    {
      path: '/CommunityView',
      name: 'CommunityView',
      component: CommunityView,
      meta: { requiresAuth: true, roles: [1, 3] } // 보호자, 구독자 전용
    },
    {
      path: '/predict-location',
      name: 'predict-location',
      component: PredictLocation,
      meta: { requiresAuth: true, roles: [1, 3] } // 보호자, 구독자 전용
    },
    {
      path: '/CommunityMissing',
      name: 'CommunityMissing',
      component: CommunityMissing,
      meta: { requiresAuth: true, roles: [1, 3] } // 보호자, 구독자 전용
    },
    {
      path: '/post/:id',
      name: 'CommunityPost',
      component: CommunityPost,
      meta: { requiresAuth: true, roles: [1, 3] } // 보호자, 구독자 전용
    },
    {
      path: '/CommunityPostWrite',
      name: 'CommunityPostWrite',
      component: CommunityPostWrite,
      meta: { requiresAuth: true, roles: [1, 3] } // 보호자, 구독자 전용
    },    
    {
      path: '/CommunityEvent',
      name: 'CommunityEvent',
      component: CommunityEvent,
    },
    {
      path: '/post/edit/:id', // '/post/edit/1' 과 같은 개념
      name: 'PostEdit',
      component: CommunityPostWrite // PostWrite 재활용 가능
    },    
    // 병욱 게시판 끝

    {
      path: '/geo-fencing',
      name: 'geo-fencing',
      component: GeoFencingView,
      meta: { requiresAuth: true, roles: [1, 3] } // 보호자, 구독자 전용
    },
    {
      path: '/search-route',
      name: 'search-route',
      component: SearchRouteView,
      meta: { requiresAuth: true, roles: [1, 3] } // 보호자, 구독자 전용
    },     

    // 주형 종합지원, 지원금안내페이지, 기록, 리포트 수정 시작
    {
      path: '/total-support',
      name: 'totalSupport',
      component: TotalSupport,
      meta: { requiresAuth: true, roles: [1, 3] } // 보호자, 구독자 전용
    },        
    {
      path: '/money-support',
      name: 'moneySupport',
      component: MoneySupport,
      meta: { requiresAuth: true, roles: [1, 3] } // 보호자, 구독자 전용
    },        
    {
      path: '/record',
      name: 'record',
      component: Record,
      meta: { requiresAuth: true, roles: [1, 3] } // 보호자, 구독자 전용
    },
    {
      path: '/report',
      name: 'report',
      component: Report,
      meta: { requiresAuth: true, roles: [1, 3] } // 보호자, 구독자 전용
    },        



    {
      path: '/loan',
      name: 'loan',
      component: Loan,
      meta: { requiresAuth: true, roles: [1, 3] } // 보호자, 구독자 전용
    },        
    {
      path: '/benefit',
      name: 'benefit',
      component: Benefit,
      meta: { requiresAuth: true, roles: [1, 3] } // 보호자, 구독자 전용
    },        
    {
      path: '/insurance',
      name: 'insurance',
      component: Insurance,
      meta: { requiresAuth: true, roles: [1, 3] } // 보호자, 구독자 전용
    },        
    {
      path: '/heartCare',
      name: 'heartCare',
      component: HeartCare,
      meta: { requiresAuth: true, roles: [1, 3] } // 보호자, 구독자 전용
    },        
    {
      path: '/GD_shcedule',
      name: 'GD_shcedule',
      component: DP_schedule,
      meta: { requiresAuth: true, roles: [2] } // 환자 전용 (roleNo: 2)
    },        

    // 주형 종합지원, 지원금안내페이지 수정 끝
        {
      path: '/gdmypage',
      name: 'gdmypage',
      component: GdMypageView,
      meta: { requiresAuth: true, roles: [1, 3] } // 보호자, 구독자 전용
    },
    {
      path: '/basicplan',
      name: 'basicplan',
      component: Basicplan,
      meta: { requiresAuth: true, roles: [1, 3] } // 보호자, 구독자 전용
    },
    {
      path: '/plusplan',
      name: 'plusplan',
      component: Plusplan,
      meta: { requiresAuth: true, roles: [1, 3] } // 보호자, 구독자 전용
    },
    {
      path: '/calendar',
      name: 'calendar',
      component: Calender,
      meta: { requiresAuth: true, roles: [1, 3] } // 보호자, 구독자 전용
    },
    {
      path: '/add-schedule',
      name: 'add-schedule',
      component: AddSchedule,
      meta: { requiresAuth: true, roles: [1, 3] } // 보호자, 구독자 전용
    },
    {
      path: '/basic-safe-zone/location',
      name: 'basic-safe-zone-location',
      component: BasicSafeZoneLocationView,
      meta: { requiresAuth: true, roles: [1, 3] } // 보호자, 구독자 전용
    },
    {
      path: '/basic-safe-zone/radius',
      name: 'basic-safe-zone-radius',
      component: BasicSafeZoneRadiusView,
      meta: { requiresAuth: true, roles: [1, 3] } // 보호자, 구독자 전용
    },
    {
      path: '/map-main',
      name: 'map-main',
      component: MapMain,
      meta: { requiresAuth: true, roles: [1, 3] } // 보호자, 구독자 전용
    }
  ],
})

// 라우터 가드 설정
router.beforeEach(async (to, from, next) => {
  const isLoggedIn = await isAuthenticated()
  
  // 로그인이 필요한 페이지에 접근하는 경우
  if (to.meta.requiresAuth && !isLoggedIn) {
    next('/login')
    return
  }
  
  // 역할 기반 접근 제어
  if (to.meta.requiresAuth && to.meta.roles && isLoggedIn) {
    const user = await getCurrentUser()
    if (user && !to.meta.roles.includes(user.roleNo)) {
      // 접근 권한이 없으면 해당 역할의 기본 페이지로 리다이렉트
      const defaultRoute = getDefaultRouteByRole(user.roleNo)
      alert('접근 권한이 없습니다.')
      next(defaultRoute)
      return
    }
  }
  
  // 게스트만 접근 가능한 페이지(로그인, 회원가입)에 로그인된 상태로 접근하는 경우
  if (to.meta.requiresGuest && isLoggedIn) {
    // 사용자 정보를 가져와서 역할에 따른 기본 페이지로 리다이렉트
    const user = await getCurrentUser()
    if (user) {
      const defaultRoute = getDefaultRouteByRole(user.roleNo)
      next(defaultRoute)
      return
    }
  }
  
  // 루트 경로 접근 시 로그인 상태에 따라 리다이렉트
  if (to.path === '/') {
    if (isLoggedIn) {
      const user = await getCurrentUser()
      if (user) {
        const defaultRoute = getDefaultRouteByRole(user.roleNo)
        next(defaultRoute)
        return
      }
    }
    next('/login')
    return
  }
  
  next()
})

export default router
