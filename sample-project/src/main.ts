import { createApp } from 'vue'
import App from './App.vue'
import AppButton from './components/AppButton.vue'
import UserCard from './components/UserCard.vue'
import ToastNotification from './components/ToastNotification.vue'

const app = createApp(App)

// تسجيل عام لمكوّن
app.component('AppButton', AppButton)

// تسجيل عام عبر destructuring
app.component('UserCard', UserCard)

app.use({
  install(app: any) {
    app.component('ToastNotification', ToastNotification)
  },
})

app.mount('#app')
