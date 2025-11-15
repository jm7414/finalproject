import { createRouter, createWebHistory } from 'vue-router'
import { isAuthenticated, getCurrentUser, getDefaultRouteByRole } from '@/utils/auth'
import DP_main from '@/views/DP_main.vue'
import GD_main from '@/views/GD_main.vue'
import GeoFencingView from '@/views/GeoFencingView.vue'
import SearchRouteView from '@/views/SearchRouteView.vue'
import Signup from '@/views/SignUp.vue'
import Login from '@/views/Login.vue'
import MissingReport from '@/components/MissingReport.vue'
import CommunityView from '@/views/CommunityView.vue'
import PredictLocation from '@/views/PredictLocation.vue'
import Simulation from '@/views/NoDataSimulation.vue'
import CommunityMissing from '@/components/CommunityMissing.vue'
import CommunityPostWrite from '@/components/CommunityPostWrite.vue'
import SightingReportBoard from '@/views/SightingReportBoard.vue'
import SightingReport from '@/components/SightingReport.vue'
import SightingReportWrite from '@/components/SightingReportWrite.vue'
import CommunityPost from '@/components/CommunityPost.vue'
import CommunityEvent from '@/components/CommunityEvent.vue'
import TotalSupport from '@/views/TotalSupport.vue'
import MoneySupport from '@/views/MoneySupport.vue'
import Record from '@/views/Record.vue'
import Report from '@/views/Report.vue'
import DpMypageView from '@/views/DpMypageView.vue'
import GdMypageView from '@/views/GdMypageView.vue'
import Basicplan from '@/views/Basicplan.vue'
import Plusplan from '@/views/Plusplan.vue'
import Payment from '@/views/Payment.vue'
import Calender from '@/views/Calender.vue'
import AddSchedule from '@/views/AddSchedule.vue'
import DP_Connect from '@/views/DP_Connect.vue'
import GD_Connect from '@/views/GD_Connect.vue'
import BasicSafeZoneLocationView from '@/views/BasicSafeZoneLocationView.vue'
import BasicSafeZoneRadiusView from '@/views/BasicSafeZoneRadiusView.vue'
import MapMain from '@/views/MapMain.vue'
import Pr from '@/views/Pr.vue'
import Game from '@/views/Game.vue'
import Loan from '@/views/Loan.vue'
import Insurance from '@/views/Insurance.vue'
import HeartCare from '@/views/heartCare.vue'
import DP_schedule from '@/views/DP_schedule.vue'
import Benefit from '@/views/Benefit.vue'
import DP_ModifyInfo from '@/views/DP_ModifyInfo.vue'
import HospitalCare from '@/views/HospitalCare.vue'
import GD_ModifyInfo from '@/views/GD_ModifyInfo.vue'
import GD_AdminDP from '@/views/GD_AdminDP.vue'
import NH_main from '@/views/NH_main.vue'
import NH_Calender from '@/views/NH_Calender.vue'
import NH_AddSchedule from '@/views/NH_AddSchedule.vue'
import NH_Notice from '@/views/NH_Notice.vue'
import DesktopMain from '@/views/desktop/DesktopMain.vue'
import DesktopLogin from '@/views/desktop/DesktopLogin.vue'
import DesktopSchedule from '@/views/desktop/DesktopSchedule.vue'
import DesktopCommunityView from '@/views/desktop/DesktopCommunityView.vue'
import DesktopCommunityPost from '@/components/desktop/DesktopCommunityPost.vue'
import DesktopCommunityBoard from '@/components/desktop/DesktopCommunityBoard.vue'
import DesktopCommunityEvent from '@/components/desktop/DesktopCommunityEvent.vue'
import DesktopCommunityMissing from '@/components/desktop/DesktopCommunityMissing.vue'
import DesktopCommunityPostWrite from '@/components/desktop/DesktopCommunityPostWrite.vue'
import DesktopPredict from '@/views/desktop/DesktopPredict.vue'
import DesktopMypage from '@/views/desktop/DesktopMypage.vue'
import DesktopMypageModify from '@/views/desktop/DesktopMypageModify.vue'
import DesktopMypageBilling from '@/views/desktop/DesktopMypageBilling.vue'
import DesktopMypagePlusplan from '@/views/desktop/DesktopMypagePlusplan.vue'
import DesktopMypageConnect from '@/views/desktop/DesktopMypageConnect.vue'
import DesktopMypageAdmin from '@/views/desktop/DesktopMypageAdmin.vue'
import DesktopMypageSafezone from '@/views/desktop/DesktopMypageSafezone.vue'
import DesktopMypageSafezoneRadius from '@/views/desktop/DesktopMypageSafezoneRadius.vue'
import DesktopReport from '@/views/desktop/DesktopReport.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: Login,
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
      meta: { requiresAuth: true, roles: [2, 3] } // 환자(2), 구독자(3) 전용
    },
    {
      path: '/GD',
      name: 'GD',
      component: GD_main,
      meta: { requiresAuth: true, roles: [1] } // 보호자(1) 전용
    },
    {
      path: '/desktop/main',
      name: 'desktop-main',
      component: DesktopMain,
      meta: { requiresAuth: true, roles: [1], layout: 'desktop' }
    },
    {
      path: '/desktop/schedule',
      name: 'desktop-schedule',
      component: DesktopSchedule,
      meta: { requiresAuth: true, roles: [1], layout: 'desktop' }
    },
    {
      path: '/desktop/predict/:id?',
      name: 'desktop-predict',
      component: DesktopPredict,
      meta: { requiresAuth: true, roles: [1], layout: 'desktop' } // '?''를 추가해서 id를 선택적으로 받음
    },
    {
      path: '/desktop/mypage',
      name: 'desktop-mypage',
      component: DesktopMypage,
      meta: { requiresAuth: true, roles: [1], layout: 'desktop' }
    },
    {
      path: '/desktop/mypage/modify',
      name: 'desktop-mypage-modify',
      component: DesktopMypageModify,
      meta: { requiresAuth: true, roles: [1], layout: 'desktop' }
    },
    {
      path: '/desktop/mypage/connect',
      name: 'desktop-mypage-connect',
      component: DesktopMypageConnect,
      meta: { requiresAuth: true, roles: [1], layout: 'desktop' }
    },
    {
      path: '/desktop/mypage/admin',
      name: 'desktop-mypage-admin',
      component: DesktopMypageAdmin,
      meta: { requiresAuth: true, roles: [1], layout: 'desktop' }
    },
    {
      path: '/desktop/mypage/safezone',
      name: 'desktop-mypage-safezone',
      component: DesktopMypageSafezone,
      meta: { requiresAuth: true, roles: [1], layout: 'desktop' }
    },
    {
      path: '/desktop/mypage/safezone/radius',
      name: 'desktop-mypage-safezone-radius',
      component: DesktopMypageSafezoneRadius,
      meta: { requiresAuth: true, roles: [1], layout: 'desktop' }
    },
    {
      path: '/desktop/mypage/billing',
      name: 'desktop-mypage-billing',
      component: DesktopMypageBilling,
      meta: { requiresAuth: true, roles: [1], layout: 'desktop' }
    },
    {
      path: '/desktop/mypage/plusplan',
      name: 'desktop-mypage-plusplan',
      component: DesktopMypagePlusplan,
      meta: { requiresAuth: true, roles: [1], layout: 'desktop' }
    },
    {
      path: '/desktop/report',
      name: 'desktop-report',
      component: DesktopReport,
      meta: { requiresAuth: true, roles: [1], layout: 'desktop' }
    },
    {
      path: '/NH',
      name: 'NH',
      component: NH_main,
      meta: { requiresAuth: true, roles: [1, 4] } // 보호자(1), 이웃(4) 접근 가능
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
    {
      path: '/desktop/login',
      name: 'desktop-login',
      component: DesktopLogin,
      meta: { requiresGuest: true, layout: 'desktop' }
    },

    // 병욱 게시판 시작
    {
      path: '/MissingReport/:id',
      name: 'MissingReport',

      component: MissingReport,
      meta: { requiresAuth: true, roles: [1] } // 보호자 전용
    },
    {
      path: '/CommunityView',
      name: 'CommunityView',

      component: CommunityView,
      meta: { requiresAuth: true, roles: [1] } // 보호자 전용
    },
    {
      path: '/predict-location/:id?',   // 메인이랑 게시판에서 보내는걸 중복으로 처리함
      name: 'predict-location',         // 원래는 /:id 있는거 없는거 2개 있었는데 하나로 합침
      component: PredictLocation,
      meta: { requiresAuth: true, roles: [1] } // 보호자 전용
    },
    {
      path: '/simulation',   // 메인이랑 게시판에서 보내는걸 중복으로 처리함
      name: 'simulation',         // 원래는 /:id 있는거 없는거 2개 있었는데 하나로 합침
      component: Simulation,
      meta: { requiresAuth: true, roles: [1] } // 보호자 전용
    },
    {
      path: '/CommunityMissing',
      name: 'CommunityMissing',
      component: CommunityMissing,
      meta: { requiresAuth: true, roles: [1] } // 보호자 전용
    },
    {
      path: '/post/:id',
      name: 'CommunityPost',
      component: CommunityPost,
      meta: { requiresAuth: true, roles: [1] } // 보호자 전용
    },
    {
      path: '/CommunityPostWrite',
      name: 'CommunityPostWrite',
      component: CommunityPostWrite,
      meta: { requiresAuth: true, roles: [1] } // 보호자 전용
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
    {
      path: '/SightingReportBoard/:id',
      name: 'SightingReportBoard',
      component: SightingReportBoard,
      meta: { requiresAuth: true, roles: [1, 4] }
    },
    {
      path: '/SightingReportWrite/:id',
      name: 'SightingReportWrite',
      component: SightingReportWrite,
      meta: { requiresAuth: true, roles: [1, 4] }
    },
    {
      path: '/report-edit/:id',
      name: 'ReportEdit',
      component: SightingReportWrite,
      meta: { requiresAuth: true, roles: [1, 4] }
    },    
    {
      path: '/SightingReport/:id',
      name: 'SightingReport',
      component: SightingReport,
      meta: { requiresAuth: true, roles: [1, 4] }
    },
    {
        path: '/desktop/communityView',
        name: 'desktop-communityView',
        component: DesktopCommunityView,
        meta: { requiresAuth: true, layout: 'desktop' }
    },    
    {
        path: '/desktop/communityBoard',
        name: 'desktop-communityBoard',
        component: DesktopCommunityBoard,
        meta: { requiresAuth: true, layout: 'desktop' }
    },
    {
        path: '/desktop/communityPost/:id',
        name: 'desktop-communityPost',
        component: DesktopCommunityPost,
        meta: { requiresAuth: true, layout: 'desktop' }
    },
    {
        path: '/desktop/communityPostWrite',
        name: 'desktop-communityPostWrite',
        component: DesktopCommunityPostWrite,
        meta: { requiresAuth: true, layout: 'desktop' }
    },   
    {
        path: '/desktop/post/edit/:id',
        name: 'desktop-PostEdit',
        component: DesktopCommunityPostWrite,
        meta: { requiresAuth: true, layout: 'desktop' }
    },           
    {
        path: '/desktop/communityMissing',
        name: 'desktop-communityMissing',
        component: DesktopCommunityMissing,
        meta: { requiresAuth: true, layout: 'desktop' }
    },
    {
        path: '/desktop/communityEvent',
        name: 'desktop-communityEvent',
        component: DesktopCommunityEvent,
        meta: { requiresAuth: true, layout: 'desktop' }
    },        
    // 병욱 게시판 끝
    {
      path: '/geo-fencing',
      name: 'geo-fencing',
      component: GeoFencingView,
      meta: { requiresAuth: true, roles: [1] } // 보호자 전용
    },
    {
      path: '/search-route',
      name: 'search-route',
      component: SearchRouteView,
      meta: { requiresAuth: true, roles: [1] } // 보호자 전용
    },

    // 주형 종합지원, 지원금안내페이지, 기록, 리포트 수정 시작
    {
      path: '/total-support',
      name: 'totalSupport',
      component: TotalSupport,
      meta: { requiresAuth: true, roles: [1] } // 보호자 전용
    },
    {
      path: '/money-support',
      name: 'moneySupport',
      component: MoneySupport,
      meta: { requiresAuth: true, roles: [1] } // 보호자 전용
    },
    {
      path: '/record',
      name: 'record',
      component: Record,
      meta: { requiresAuth: true, roles: [1] } // 보호자 전용
    },
    {
      path: '/report',
      name: 'report',
      component: Report,
      meta: { requiresAuth: true, roles: [1] } // 보호자 전용
    },



    {
      path: '/loan',
      name: 'loan',
      component: Loan,
      meta: { requiresAuth: true, roles: [1] } // 보호자 전용
    },
    {
      path: '/benefit',
      name: 'benefit',
      component: Benefit,
      meta: { requiresAuth: true, roles: [1] } // 보호자 전용
    },
    {
      path: '/insurance',
      name: 'insurance',
      component: Insurance,
      meta: { requiresAuth: true, roles: [1] } // 보호자 전용
    },
    {
      path: '/heartCare',
      name: 'heartCare',
      component: HeartCare,
      meta: { requiresAuth: true, roles: [1] } // 보호자 전용
    },
    {
      path: '/hospitalCare',
      name: 'hospitalCare',
      component: HospitalCare,
      meta: { requiresAuth: true, roles: [1] } // 보호자 전용
    },
    {
      path: '/DP_schedule',
      name: 'DP_schedule',
      component: DP_schedule,
      meta: { requiresAuth: true, roles: [2, 3] } // 환자(2), 구독자(3) 전용
    },
    // 지현
    {
      path: '/dpmypage',
      name: 'dpmypage',
      component: DpMypageView,
      meta: { requiresAuth: true, roles: [2, 3] } // 환자(2), 구독자(3) 전용
    },
    {
      path: '/dpmodifyinfo',
      name: 'dpmodifyinfo',
      component: DP_ModifyInfo,
      meta: { requiresAuth: true, roles: [2, 3] } // 환자(2), 구독자(3) 전용
    },
    // 주형 종합지원, 지원금안내페이지 수정 끝
    {
      path: '/gdmypage',
      name: 'gdmypage',
      component: GdMypageView,
      meta: { requiresAuth: true, roles: [1] } // 보호자 전용
    },
    {
      path: '/gdmodifyinfo',
      name: 'gdmodifyinfo',
      component: GD_ModifyInfo,
      meta: { requiresAuth: true, roles: [1] } // 보호자 전용
    },
    {
      path: '/gdadmindp',
      name: 'gdadmindp',
      component: GD_AdminDP,
      meta: { requiresAuth: true, roles: [1] } // 보호자 전용
    },
    {
      path: '/basicplan',
      name: 'basicplan',
      component: Basicplan,
      meta: { requiresAuth: true, roles: [1] } // 보호자 전용
    },
    {
      path: '/plusplan',
      name: 'plusplan',
      component: Plusplan,
      meta: { requiresAuth: true, roles: [1] } // 보호자 전용
    },
    {
      path: '/payment',
      name: 'payment',
      component: Payment,
      meta: { requiresAuth: true, roles: [1] } // 보호자 전용
    },
    {
      path: '/calendar',
      name: 'calendar',
      component: Calender,
      meta: { requiresAuth: true, roles: [1] } // 보호자 전용
    },
    {
      path: '/add-schedule',
      name: 'add-schedule',
      component: AddSchedule,
      meta: { requiresAuth: true, roles: [1] } // 보호자 전용
    },
    {
      path: '/basic-safe-zone/location',
      name: 'basic-safe-zone-location',
      component: BasicSafeZoneLocationView,
      meta: { requiresAuth: true, roles: [1] } // 보호자 전용
    },
    {
      path: '/basic-safe-zone/radius',
      name: 'basic-safe-zone-radius',
      component: BasicSafeZoneRadiusView,
      meta: { requiresAuth: true, roles: [1] } // 보호자 전용
    },
    {
      path: '/map-main',
      name: 'map-main',
      component: MapMain,
      meta: { requiresAuth: true, roles: [1] } // 보호자 전용
    },
    // 지겸
    {
      path: '/nhCalender',
      name: 'NH_Calender',
      component: NH_Calender,
      meta: { requiresAuth: true, roles: [1, 4] } // 보호자(1), 이웃(4) 접근 가능
    },
    {
      path: '/nhAddSchedule',
      name: 'NH_AddSchedule',
      component: NH_AddSchedule,
      meta: { requiresAuth: true, roles: [1, 4] } // 보호자(1), 이웃(4) 접근 가능
    },
    {
      path: '/nhNotice',
      name: 'NH_Notice',
      component: NH_Notice,
      meta: { requiresAuth: true, roles: [1, 4] } // 보호자(1), 이웃(4) 접근 가능
    }
  ],
})

// 라우터 가드 설정
router.beforeEach(async (to, from, next) => {
  // 게스트 페이지(로그인, 회원가입)는 인증 체크 없이 통과
  if (to.meta.requiresGuest) {
    next()
    return
  }

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