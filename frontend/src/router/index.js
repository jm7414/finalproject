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
      path: '/post',
      name: 'post',
      component: () => import('../views/Post.vue'),
    },
  ],
})

export default router
