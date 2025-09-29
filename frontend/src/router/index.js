import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import AboutView from '@/views/AboutView.vue'
import DP_main from '@/views/DP_main.vue'
import GD_main from '@/views/GD_main.vue'

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
      path: '/CommunityBoard',
      name: 'CommunityBoard',
      component: () => import('../components/CommunityBoard.vue'),
    },
    {
      path: '/CommunityPost',
      name: 'CommunityPost',
      component: () => import('../components/CommunityPost.vue'),
    },
    {
      path: '/CommunityPostWrite',
      name: 'CommunityPostWrite',
      component: () => import('../components/CommunityPostWrite.vue'),
    },
    {
      path: '/CommunityEvent',
      name: 'CommunityEvent',
      component: () => import('../components/CommunityEvent.vue'),
    },         
  ],
})

export default router
