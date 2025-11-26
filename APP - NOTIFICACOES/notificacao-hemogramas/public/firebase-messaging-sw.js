importScripts('https://www.gstatic.com/firebasejs/9.23.0/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/9.23.0/firebase-messaging-compat.js');

firebase.initializeApp({
  apiKey: "AIzaSyBUzx--pX3Y7A-XNhznwUl-YScXJPhT1X0",
  authDomain: "notificacao-hemograma.firebaseapp.com",
  projectId: "notificacao-hemograma",
  storageBucket: "notificacao-hemograma.firebasestorage.app",
  messagingSenderId: "634442050780",
  appId: "1:634442050780:web:ab3c33430f6a848ced5819",
  measurementId: "G-6JWLTP4NCE"
});

const messaging = firebase.messaging();

messaging.onBackgroundMessage(function(payload) {
  console.log('Recebido em background: ', payload);
  const notificationTitle = payload.notification.title;
  const notificationOptions = {
    body: payload.notification.body,
    icon: '/assets/icons/icon-192x192.png' // Caminho do seu ícone
  };

  self.registration.showNotification(notificationTitle, notificationOptions);
});
