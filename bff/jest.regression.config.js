// Regressão funcional contra o ambiente Docker Compose em execução (Angular/Nginx -> BFF -> Spring Boot).
// Uso: REGRESSION_BASE_URL=http://localhost:4200 npm run test:regression
module.exports = {
  testEnvironment: 'node',
  rootDir: '.',
  roots: ['<rootDir>/test/regression'],
  testRegex: '\\.regression\\.ts$',
  transform: { '^.+\\.ts$': 'ts-jest' },
  moduleFileExtensions: ['ts', 'js', 'json'],
  testTimeout: 120000,
};
