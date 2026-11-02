// Configuration Karma : https://karma-runner.github.io/1.0/config/configuration-file.html
// - en local : navigateur Chrome (npm test) ;
// - en CI (Jenkins) : ChromeHeadlessCI, sans sandbox car le conteneur tourne en root (npm run test:ci).

module.exports = function (config) {
  config.set({
    basePath: '',
    frameworks: ['jasmine', '@angular-devkit/build-angular'],
    plugins: [
      require('karma-jasmine'),
      require('karma-chrome-launcher'),
      require('karma-jasmine-html-reporter'),
      require('karma-coverage'),
      require('@angular-devkit/build-angular/plugins/karma')
    ],
    client: {
      jasmine: {},
    },
    jasmineHtmlReporter: {
      suppressAll: true
    },
    coverageReporter: {
      dir: require('path').join(__dirname, './coverage/frontend'),
      subdir: '.',
      reporters: [
        { type: 'html' },
        { type: 'text-summary' },
        // lcov.info est lu par SonarQube (sonar.javascript.lcov.reportPaths)
        { type: 'lcovonly', file: 'lcov.info' }
      ]
    },
    reporters: ['progress', 'kjhtml'],
    browsers: ['Chrome'],
    customLaunchers: {
      ChromeHeadlessCI: {
        base: 'ChromeHeadless',
        flags: ['--no-sandbox', '--disable-gpu', '--disable-dev-shm-usage']
      }
    },
    // Le premier demarrage de Chrome dans un conteneur peut etre lent
    captureTimeout: 120000,
    browserNoActivityTimeout: 120000,
    restartOnFileChange: true
  });
};
