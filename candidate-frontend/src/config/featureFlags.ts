// src/config/featureFlags.ts
// Feature flag registry for candidate-frontend
// Controls deployment-level features such as Practice & Learning Mode,
// allowing actual exam questions to be tested interactively during development/staging
// while strictly disabling practice solutions in production CBT environments.

export const FEATURE_FLAGS = {
  /**
   * Enables interactive practice & learning mode (step-by-step solutions, instant feedback).
   * In production CBT delivery environments, this is disabled to enforce strict exam security.
   * Configurable via VITE_ENABLE_PRACTICE_MODE environment variable.
   */
  ENABLE_PRACTICE_MODE:
    import.meta.env.VITE_ENABLE_PRACTICE_MODE !== undefined
      ? import.meta.env.VITE_ENABLE_PRACTICE_MODE === 'true'
      : true, // Enabled by default for testing & learning

  /**
   * Allow skipping fullscreen requirement in development/testing mode
   */
  ALLOW_EXIT_FULLSCREEN:
    import.meta.env.VITE_ALLOW_EXIT_FULLSCREEN !== undefined
      ? import.meta.env.VITE_ALLOW_EXIT_FULLSCREEN === 'true'
      : false,
};
