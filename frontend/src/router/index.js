import { createRouter, createWebHistory } from 'vue-router'
import DP_main from '@/views/DP_main.vue'
import GD_main from '@/views/GD_main.vue'
import GeoFencingView from '@/views/GeoFencingView.vue'
import SearchRouteView from '@/views/SearchRouteView.vue'
import Signup from '@/views/SignUp.vue'
import Login from '@/views/Login.vue'
import CommunityView from '@/views/CommunityView.vue'
import PredictLocation from '@/views/PredictLocation.vue'
import CommunityMissing from '@/components/CommunityMissing.vue'
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

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      redirect: '/login',
    },
    {
      path: '/DP',
      name: 'DP',
      component: DP_main,
    },
    {
      path: '/GD',
      name: 'GD',
      component: GD_main,
    },
    {
      path: '/SignUp',
      name: 'SignUp',
      component: Signup,
    },
    {
      path: '/login',
      name: 'login',
      component: Login,
    },
    {
      path: '/CommunityView',
      name: 'CommunityView',
      component: CommunityView,
    },
    {
      path: '/predict-location',
      name: 'predict-location',
      component: PredictLocation,
    },
    {
      path: '/CommunityMissing',
      name: 'CommunityMissing',
      component: CommunityMissing,
    },
    {
      path: '/CommunityPost',
      name: 'CommunityPost',
      component: CommunityPost,
    },
    {
      path: '/CommunityEvent',
      name: 'CommunityEvent',
      component: CommunityEvent,
    },
        {
      path: '/geo-fencing',
      name: 'geo-fencing',
      component: GeoFencingView,
    },
    {
      path: '/search-route',
      name: 'search-route',
      component: SearchRouteView,
    },        
    // 주형 종합지원, 지원금안내페이지, 기록, 리포트 수정 시작
    {
      path: '/total-support',
      name: 'totalSupport',
      component: TotalSupport,
    },        
    {
      path: '/money-support',
      name: 'moneySupport',
      component: MoneySupport,
    },        
    {
      path: '/record',
      name: 'record',
      component: Record,
    },        
    {
      path: '/report',
      name: 'report',
      component: Report,
    },        
    // 주형 종합지원, 지원금안내페이지 수정 끝
        {
      path: '/gdmypage',
      name: 'gdmypage',
      component: GdMypageView,
    },
    {
      path: '/basicplan',
      name: 'basicplan',
      component: Basicplan,
    },
    {
      path: '/plusplan',
      name: 'plusplan',
      component: Plusplan,
    },
    {
      path: '/calendar',
      name: 'calendar',
      component: Calender,
    },
    {
      path: '/add-schedule',
      name: 'add-schedule',
      component: AddSchedule,
    }
  ],
})

export default router
