//  @ts-check

import { tanstackConfig } from "@tanstack/eslint-config"

export default [
  {
    ignores: [
      ".output/**",
      ".tanstack/**",
      ".vinxi/**",
      "dist/**",
      "dist-ssr/**",
      "node_modules/**",
    ],
  },
  ...tanstackConfig,
]
