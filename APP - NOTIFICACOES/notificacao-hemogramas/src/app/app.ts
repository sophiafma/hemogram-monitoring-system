import { Component, inject, NgZone, OnInit } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Messaging, getToken, onMessage } from '@angular/fire/messaging';
import { DengueAlert } from './models/alert.model';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, CommonModule],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements OnInit {

  private messaging = inject(Messaging);
  private zone = inject(NgZone);

  vapidKey = "BDsM-mal2e8sAcNjyAx3e0WCkpSaez62YwQTwEhJy9pqBJHojjZuqd5uqXaX2aolDSdSMsKOr2SHoY7fSvkRfOE";

  alerts: DengueAlert[] = [];

  showToast = false;
  latestAlert: DengueAlert | null = null;

  ngOnInit() {
    this.requestPermission();
    this.listen();
  }

requestPermission() {
    getToken(this.messaging, { vapidKey: this.vapidKey })
      .then((token) => {
        if (token) {
          console.log("Device Token:", token);
          // DICA: No seu Backend Java, você pode associar este token
          // a uma região (Ex: Topic Subscription 'zona-sul')
        }
      });
  }

  listen() {
    onMessage(this.messaging, (payload) => {
      console.log('Alerta recebido:', payload);

      const novoAlerta: DengueAlert = {
        titulo: payload.notification?.title || 'Alerta de Dengue',
        mensagem: payload.notification?.body || 'Cuidado com água parada.',
        // O Java manda a região dentro de "data"
        regiao: payload.data?.['regiao'] || 'Geral',
        severidade: (payload.data?.['nivel'] === 'alto') ? 'perigo' : 'aviso',
        data: new Date()
      };

      this.zone.run(() => {
        this.alerts.unshift(novoAlerta); // Adiciona o mais recente no topo
      });
    });
  }
}
