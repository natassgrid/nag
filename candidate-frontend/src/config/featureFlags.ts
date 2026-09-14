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

  /**
   * Enables multi-language examination delivery.
   * When enabled, candidates can select their preferred regional medium before starting
   * the exam and toggle between English (master reference) and their chosen language
   * on a per-question basis with zero latency (purely client-side switching).
   *
   * In accordance with NTA/SSC/RRB standards: the English version prevails in case
   * of any discrepancy with a regional translation.
   *
   * Configurable via VITE_ENABLE_MULTILINGUAL environment variable.
   * Defaults to true so regional language delivery is available in all environments.
   */
  ENABLE_MULTILINGUAL:
    import.meta.env.VITE_ENABLE_MULTILINGUAL !== undefined
      ? import.meta.env.VITE_ENABLE_MULTILINGUAL === 'true'
      : true,
};
