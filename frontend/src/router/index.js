import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'

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
      component: () => import('../views/AboutView.vue'),
    },
    {
      path: '/gdmypage',
      name: 'gdmypage',
      component: () => import('../views/GdMypageView.vue'),
    },
    {
      path: '/dpmypage',
      name: 'dpmypage',
      component: () => import('../views/DpMypageView.vue'),
    },
  ],
})

export default router
