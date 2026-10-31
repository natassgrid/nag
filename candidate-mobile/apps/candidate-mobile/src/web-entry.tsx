import React from 'react';
import { App } from './App';

export { App };

/**
 * Initializes and mounts the NAG Candidate Mobile Application
 * inside the Android WebView shell.
 */
export function mountMobileApp(): void {
  if (typeof document === 'undefined') return;
  const root = document.getElementById('root');
  if (root && typeof (window as any).NagReactMount === 'function') {
    (window as any).NagReactMount(React.createElement(App), root);
  }
}

if (typeof window !== 'undefined' && typeof document !== 'undefined') {
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', mountMobileApp);
  } else {
    mountMobileApp();
  }
}
