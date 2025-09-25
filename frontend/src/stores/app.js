import { ref } from "vue"
import { defineStore } from 'pinia'

export const useAppStore = defineStore('app', () => {
    const main = ref(false)

    return {
        main
    }
})