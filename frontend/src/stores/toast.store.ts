import { defineStore } from 'pinia';

export type ToastTone = 'success' | 'error' | 'info';

export interface ToastMessage {
  id: number;
  tone: ToastTone;
  message: string;
}

let nextToastId = 1;
const TOAST_DURATION = 4200;

export const useToastStore = defineStore('toast', {
  state: () => ({
    messages: [] as ToastMessage[],
  }),
  actions: {
    notify(tone: ToastTone, message: string): void {
      const id = nextToastId++;
      this.messages.push({ id, tone, message });

      window.setTimeout(() => {
        this.dismiss(id);
      }, TOAST_DURATION);
    },
    success(message: string): void {
      this.notify('success', message);
    },
    error(message: string): void {
      this.notify('error', message);
    },
    info(message: string): void {
      this.notify('info', message);
    },
    dismiss(id: number): void {
      this.messages = this.messages.filter((toast) => toast.id !== id);
    },
  },
});