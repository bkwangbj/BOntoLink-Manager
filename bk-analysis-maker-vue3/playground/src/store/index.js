import { defineStore } from 'pinia'

export const useComponentsStore = defineStore('components', {
  state () {
    return {
      components: []
    }
  },
  actions: {
    setComponents (list) {
      this.components = list
    }
  }
})
