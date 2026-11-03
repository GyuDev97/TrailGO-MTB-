(function () {
  var androidButton = document.getElementById('android-download');
  if (!androidButton) return;

  fetch('/api/public/config')
    .then(function (response) { return response.ok ? response.json() : null; })
    .then(function (config) {
      if (!config || !config.androidDownloadUrl) return;
      androidButton.href = config.androidDownloadUrl;
      androidButton.classList.remove('is-disabled');
      androidButton.removeAttribute('aria-disabled');
      androidButton.querySelector('strong').textContent = 'Google Play에서 다운로드';
    })
    .catch(function () {
      // The built-in Google Play URL remains available when public config is unavailable.
    });
}());
