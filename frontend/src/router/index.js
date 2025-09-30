import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import AboutView from '@/views/AboutView.vue'
import DP_main from '@/views/DP_main.vue'
import GD_main from '@/views/GD_main.vue'
import GeoFencingView from '@/views/GeoFencingView.vue'
import SearchRouteView from '@/views/SearchRouteView.vue'
import Signup from '@/views/SignUp.vue'
import Login from '@/views/Login.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/about',
      name: 'about',
      component: AboutView,
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
      component: () => import('../views/CommunityView.vue'),
    },
    {
      path: '/predict-location',
      name: 'predict-location',
      component: () => import('../views/PredictLocation.vue'),
    },
    {
      path: '/CommunityMissing',
      name: 'CommunityMissing',
      component: () => import('../components/CommunityMissing.vue'),
    },
    {
      path: '/CommunityPost',
      name: 'CommunityPost',
      component: () => import('../components/CommunityPost.vue'),
    },
    {
      path: '/CommunityEvent',
      name: 'CommunityEvent',
      component: () => import('../components/CommunityEvent.vue'),
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
    // 주형 종합지원, 지원금안내페이지 수정 시작
    {
      path: '/total-support',
      name: 'totalSupport',
      component: () => import('../views/TotalSupport.vue'),
    },        
    {
      path: '/money-support',
      name: 'moneySupport',
      component: () => import('../views/MoneySupport.vue'),
    },        
    // 주형 종합지원, 지원금안내페이지 수정 끝
  ],
})

export default router
