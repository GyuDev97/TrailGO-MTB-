(function () {
  var body = document.body;
  var sidebarToggle = document.querySelector('.sidebar-toggle');
  if (sidebarToggle) {
    sidebarToggle.addEventListener('click', function () { body.classList.toggle('sidebar-open'); });
  }

  // ---- shared helpers ----------------------------------------------------------------------------------

  /** JSON call with the CSRF header. Rejects with an Error (message from the server) and .data (its body). */
  function api(url, method, payload) {
    var token = document.querySelector('meta[name="_csrf"]');
    var header = document.querySelector('meta[name="_csrf_header"]');
    var headers = { 'Content-Type': 'application/json' };
    if (token && header) headers[header.content] = token.content;
    var options = { method: method, headers: headers };
    if (payload !== undefined && method !== 'GET') options.body = JSON.stringify(payload);
    return fetch(url, options).then(function (response) {
      return response.text().then(function (text) {
        var data = {};
        try { data = text ? JSON.parse(text) : {}; } catch (e) { data = {}; }
        if (!response.ok) {
          var error = new Error(data.message || '요청을 처리하지 못했습니다. (' + response.status + ')');
          error.data = data;
          error.status = response.status;
          throw error;
        }
        return data;
      });
    });
  }

  var kakaoLoading = null;
  /** Loads the Kakao map script once; the callback gets window.kakao.maps, or is not called without a key. */
  function withKakao(callback) {
    var key = body.dataset.kakaoKey;
    if (!key) return;
    if (window.kakao && window.kakao.maps && window.kakao.maps.Map) { callback(window.kakao.maps); return; }
    if (!kakaoLoading) {
      kakaoLoading = new Promise(function (resolve, reject) {
        var script = document.createElement('script');
        script.src = 'https://dapi.kakao.com/v2/maps/sdk.js?autoload=false&appkey=' + encodeURIComponent(key);
        script.onload = function () { window.kakao.maps.load(function () { resolve(window.kakao.maps); }); };
        script.onerror = reject;
        document.head.appendChild(script);
      });
    }
    kakaoLoading.then(callback, function () { /* the page works without the map */ });
  }

  function setBusy(root, busy) {
    root.querySelectorAll('button').forEach(function (button) {
      if (busy) { button.dataset.wasDisabled = button.disabled ? '1' : ''; button.disabled = true; }
      else { button.disabled = button.dataset.wasDisabled === '1'; }
    });
  }

  function text(value) { return value === null || value === undefined ? '' : String(value); }

  function el(tag, className, content) {
    var node = document.createElement(tag);
    if (className) node.className = className;
    if (content !== undefined) node.textContent = content;
    return node;
  }

  var COLORS = {
    unverified: { stroke: '#e65100', fill: '#ff8f00' },
    verified: { stroke: '#2e7d32', fill: '#43a047' }
  };

  function drawZone(maps, map, lat, lng, radius, verified, title) {
    var position = new maps.LatLng(lat, lng);
    var color = verified ? COLORS.verified : COLORS.unverified;
    var circle = new maps.Circle({
      map: map, center: position, radius: radius,
      strokeWeight: 2, strokeColor: color.stroke, strokeOpacity: 0.9,
      fillColor: color.fill, fillOpacity: 0.25
    });
    var content = document.createElement('div');
    content.className = 'kakao-hazard-marker ' + (verified ? 'is-verified' : 'is-unverified');
    content.title = title || '위험지역';
    content.innerHTML = '<span>!</span>';
    var overlay = new maps.CustomOverlay({ map: map, position: position, content: content, yAnchor: 1 });
    return { circle: circle, overlay: overlay, position: position };
  }

  // ---- list / detail map ------------------------------------------------------------------------------

  var mapElement = document.getElementById('hazard-map');
  if (mapElement) initializeMap(mapElement);

  function initializeMap(element) {
    var latitude = parseFloat(element.dataset.lat);
    var longitude = parseFloat(element.dataset.lng);
    if (!isFinite(latitude) || !isFinite(longitude)) return;
    withKakao(function (maps) {
      var map = new maps.Map(element, { center: new maps.LatLng(latitude, longitude), level: 5 });
      var items = Array.prototype.slice.call(element.querySelectorAll('.hazard-marker-data'));
      items.forEach(function (item) {
        var lat = Number(item.dataset.lat), lng = Number(item.dataset.lng);
        if (!isFinite(lat) || !isFinite(lng)) return;
        drawZone(maps, map, lat, lng, Number(item.dataset.radius) || 30, item.dataset.verified === 'true', item.dataset.title);
      });
    });
  }

  // ---- rows that link to a detail page ---------------------------------------------------------------

  document.querySelectorAll('tr[data-href]').forEach(function (row) {
    row.addEventListener('click', function (event) {
      if (event.target.closest('.select-cell, a, input, button')) return;
      window.location.href = row.dataset.href;
    });
  });

  // ---- list: select several zones to merge ---------------------------------------------------------------

  var mergeSelected = document.getElementById('merge-selected');
  if (mergeSelected) {
    var boxes = function () { return Array.prototype.slice.call(document.querySelectorAll('.row-select:checked')); };
    var refresh = function () {
      var count = boxes().length;
      document.getElementById('selection-count').textContent = count + '건 선택';
      mergeSelected.disabled = count < 2;
    };
    document.querySelectorAll('.row-select').forEach(function (box) { box.addEventListener('change', refresh); });
    mergeSelected.addEventListener('click', function () {
      window.location.href = '/admin/hazards/merge?ids=' + boxes().map(function (b) { return b.value; }).join(',');
    });
    refresh();
  }

  // ---- detail: review buttons and editing ------------------------------------------------------------------

  var form = document.getElementById('hazard-edit-form');
  if (form) {
    form.querySelectorAll('[data-action]').forEach(function (button) {
      button.addEventListener('click', function () { handleDetailAction(form, button.dataset.action); });
    });
  }

  function handleDetailAction(formElement, action) {
    var id = formElement.dataset.hazardId;
    var base = '/admin/api/hazards/' + id;
    if (action === 'approve') {
      if (window.confirm('이 위험지역을 승인할까요? 승인하면 앱에 \'운영자 확인\' 위험지역으로 표시됩니다.')) run(formElement, base + '/approve', 'POST', {});
    } else if (action === 'reject') {
      var reason = window.prompt('거절 사유를 입력해 주세요.');
      if (reason !== null) run(formElement, base + '/reject', 'POST', { reason: reason });
    } else if (action === 'hide') {
      if (window.confirm('이 위험지역을 숨길까요? 앱에는 더 이상 표시되지 않습니다.')) run(formElement, base + '/hide', 'POST', {});
    } else if (action === 'restore') {
      if (window.confirm('숨김(거절)을 해제하고 다시 검토 대기 상태로 돌릴까요?')) run(formElement, base + '/restore', 'POST', {});
    } else if (action === 'save') {
      var payload = {};
      new FormData(formElement).forEach(function (value, key) { payload[key] = value; });
      if (payload.radiusM !== undefined && payload.radiusM !== '') payload.radiusM = Number(payload.radiusM); else delete payload.radiusM;
      run(formElement, base, 'PATCH', payload);
    } else if (action === 'duplicates') {
      loadDuplicates(formElement, id);
    }
  }

  /** One request at a time: buttons stay disabled until the server answered, then the page is reloaded from the DB. */
  function run(root, url, method, payload) {
    setBusy(root, true);
    api(url, method, payload)
      .then(function () { window.location.reload(); })
      .catch(function (error) {
        setBusy(root, false);
        window.alert(error.message + (error.status === 409 ? '\n(화면을 새로고침하면 최신 상태를 볼 수 있습니다.)' : ''));
      });
  }

  function loadDuplicates(formElement, id) {
    var box = document.getElementById('duplicate-list');
    box.hidden = false;
    box.textContent = '중복 후보를 찾는 중입니다…';
    api('/admin/api/hazards/' + id + '/duplicates', 'GET').then(function (data) {
      renderCandidates(box, data.candidates || [], { selectable: true, baseId: id });
    }).catch(function (error) { box.textContent = error.message; });
  }

  /** Candidate list: why each one looks like a duplicate. Nothing is merged automatically. */
  function renderCandidates(box, candidates, options) {
    box.innerHTML = '';
    if (!candidates.length) { box.appendChild(el('p', 'muted', '중복 가능성이 있는 위험지역이 없습니다.')); return; }
    box.appendChild(el('p', 'muted', '가까이 있다는 이유만으로 자동 병합하지 않습니다. 같은 위험인지 직접 확인해 주세요.'));
    candidates.forEach(function (candidate) {
      var row = el('label', 'candidate');
      if (options.selectable) {
        var check = document.createElement('input');
        check.type = 'checkbox'; check.value = candidate.id; check.className = 'candidate-check';
        row.appendChild(check);
      }
      var info = el('div', 'candidate-info');
      var head = el('strong', null, '#' + candidate.id + ' ' + text(candidate.title));
      info.appendChild(head);
      info.appendChild(el('span', 'candidate-meta', text(candidate.hazardTypeLabel) + ' · ' + text(candidate.statusLabel)
        + ' · 반경 ' + text(candidate.radiusM) + 'm · 거리 ' + text(candidate.distanceM) + 'm · 일치도 ' + text(candidate.score)));
      var reasons = el('ul', 'candidate-reasons');
      (candidate.reasons || []).forEach(function (reason) { reasons.appendChild(el('li', null, reason)); });
      info.appendChild(reasons);
      var link = el('a', 'candidate-link', '상세 보기');
      link.href = '/admin/hazards/' + candidate.id; link.target = '_blank';
      info.appendChild(link);
      row.appendChild(info);
      box.appendChild(row);
    });
    if (options.selectable) {
      var go = el('button', 'primary-button compact', '선택한 항목과 병합 화면으로');
      go.type = 'button';
      go.addEventListener('click', function () {
        var ids = [options.baseId];
        box.querySelectorAll('.candidate-check:checked').forEach(function (c) { ids.push(c.value); });
        if (ids.length < 2) { window.alert('병합할 후보를 선택해 주세요.'); return; }
        window.location.href = '/admin/hazards/merge?ids=' + ids.join(',');
      });
      box.appendChild(go);
    }
  }

  // ---- register a zone directly -----------------------------------------------------------------------------

  var createForm = document.getElementById('hazard-create-form');
  if (createForm) initCreate(createForm);

  function initCreate(root) {
    var latInput = root.querySelector('#latitude');
    var lngInput = root.querySelector('#longitude');
    var radiusInput = root.querySelector('#radiusM');
    var message = document.getElementById('create-message');
    var duplicateBox = document.getElementById('duplicate-list');
    var mapState = null;

    function showMessage(textValue) { message.hidden = !textValue; message.textContent = textValue || ''; }

    function currentPoint() {
      var lat = parseFloat(latInput.value), lng = parseFloat(lngInput.value);
      return (isFinite(lat) && isFinite(lng)) ? { lat: lat, lng: lng } : null;
    }
    function currentRadius() {
      var r = parseInt(radiusInput.value, 10);
      return isFinite(r) ? r : 30;
    }
    function redraw() {
      if (!mapState) return;
      var point = currentPoint();
      if (mapState.shape) { mapState.shape.circle.setMap(null); mapState.shape.overlay.setMap(null); mapState.shape = null; }
      if (!point) return;
      mapState.shape = drawZone(mapState.maps, mapState.map, point.lat, point.lng, Math.max(10, Math.min(200, currentRadius())), false, '선택한 위치');
    }

    withKakao(function (maps) {
      var element = document.getElementById('create-map');
      element.innerHTML = '';
      var map = new maps.Map(element, { center: new maps.LatLng(35.89617906, 128.62171907), level: 5 });
      mapState = { maps: maps, map: map, shape: null };
      maps.event.addListener(map, 'click', function (event) {
        var position = event.latLng;
        latInput.value = position.getLat().toFixed(7);
        lngInput.value = position.getLng().toFixed(7);
        redraw();
      });
      redraw();
    });
    [latInput, lngInput, radiusInput].forEach(function (input) { input.addEventListener('input', redraw); });

    function payload(acknowledge) {
      return {
        latitude: parseFloat(latInput.value), longitude: parseFloat(lngInput.value),
        radiusM: currentRadius(), type: root.querySelector('#hazardType').value,
        title: root.querySelector('#title').value, description: root.querySelector('#description').value,
        address: root.querySelector('#address').value, acknowledgeDuplicates: !!acknowledge
      };
    }

    root.querySelector('[data-action="check-duplicates"]').addEventListener('click', function () {
      showMessage('');
      setBusy(root, true);
      api('/admin/api/hazards/duplicates', 'POST', payload(false)).then(function (data) {
        duplicateBox.hidden = false;
        renderCandidates(duplicateBox, data.candidates || [], { selectable: false });
      }).catch(function (error) { showMessage(error.message); }).then(function () { setBusy(root, false); });
    });

    function create(acknowledge, thenMerge) {
      showMessage('');
      setBusy(root, true);
      api('/admin/api/hazards', 'POST', payload(acknowledge)).then(function (data) {
        var id = data.hazard.id;
        if (thenMerge && thenMerge.length) {
          window.location.href = '/admin/hazards/merge?ids=' + [id].concat(thenMerge).join(',');
        } else {
          window.location.href = '/admin/hazards/' + id;
        }
      }).catch(function (error) {
        // the form keeps everything that was typed
        setBusy(root, false);
        if (error.data && error.data.code === 'DUPLICATE_CANDIDATES') {
          showMessage(error.message);
          duplicateBox.hidden = false;
          renderCandidates(duplicateBox, error.data.candidates || [], { selectable: false });
          var ids = (error.data.candidates || []).map(function (c) { return c.id; });
          var actions = el('div', 'duplicate-actions');
          var anyway = el('button', 'secondary-button compact', '중복이 아니므로 그대로 등록');
          anyway.type = 'button';
          anyway.addEventListener('click', function () { create(true, null); });
          var merge = el('button', 'primary-button compact', '등록 후 병합 화면으로');
          merge.type = 'button';
          merge.addEventListener('click', function () { create(true, ids); });
          actions.appendChild(anyway); actions.appendChild(merge);
          duplicateBox.appendChild(actions);
        } else {
          showMessage(error.message);
        }
      });
    }
    root.querySelector('[data-action="create"]').addEventListener('click', function () {
      if (!currentPoint()) { showMessage('지도에서 위치를 선택하거나 위도·경도를 입력해 주세요.'); return; }
      if (!window.confirm('이 위험지역을 등록할까요? 등록하면 바로 활성화되고 앱에 \'운영자 확인\' 위험지역으로 전달됩니다.')) return;
      create(false, null);
    });
  }

  // ---- merge ------------------------------------------------------------------------------------------------------

  var mergeForm = document.getElementById('hazard-merge-form');
  if (mergeForm) initMerge(mergeForm);

  function initMerge(root) {
    var rows = Array.prototype.slice.call(document.querySelectorAll('#merge-table tbody tr'));
    var messageBox = document.getElementById('merge-message');
    var previewBox = document.getElementById('merge-preview');
    var confirmButton = root.querySelector('[data-action="merge-confirm"]');
    var typeConfirm = document.getElementById('type-confirm');
    var typeCheck = document.getElementById('confirm-type');
    var fields = ['title', 'hazardType', 'description', 'latitude', 'longitude', 'radiusM'];
    var originals = {};
    var lastPreview = null;
    var timer = null;

    function zoneOf(row) {
      return { id: Number(row.dataset.id), title: row.dataset.title, hazardType: row.dataset.type, description: row.dataset.description,
               latitude: row.dataset.lat, longitude: row.dataset.lng, radiusM: row.dataset.radius };
    }
    function canonicalRow() { var r = document.querySelector('.canonical-radio:checked'); return r ? r.closest('tr') : null; }
    function includedRows() { return rows.filter(function (r) { return r.querySelector('.include-check').checked && !r.querySelector('.include-check').disabled; }); }
    function show(textValue) { messageBox.hidden = !textValue; messageBox.textContent = textValue || ''; }

    function fillFromCanonical() {
      var row = canonicalRow();
      if (!row) return;
      originals = zoneOf(row);
      fields.forEach(function (name) { var input = root.querySelector('#' + name); if (input) input.value = text(originals[name]); });
    }

    /** Only what the operator changed is sent as an edit of the representative. */
    function request() {
      var row = canonicalRow();
      var req = { canonicalId: row ? Number(row.dataset.id) : null, sourceIds: [], confirmTypeMismatch: typeCheck.checked };
      includedRows().forEach(function (r) { if (r !== row) req.sourceIds.push(Number(r.dataset.id)); });
      fields.forEach(function (name) {
        var input = root.querySelector('#' + name);
        if (!input) return;
        var value = input.value;
        if (text(value) !== text(originals[name])) req[name] = name === 'radiusM' ? Number(value) : value;
      });
      return req;
    }

    function renderPreview(data) {
      previewBox.innerHTML = '';
      (data.errors || []).forEach(function (e) { previewBox.appendChild(el('p', 'preview-error', '⛔ ' + e.message)); });
      (data.warnings || []).forEach(function (w) { previewBox.appendChild(el('p', 'preview-warning', '⚠ ' + w.message)); });
      var after = data.after || {};
      var list = el('dl', 'preview-after');
      [['대표 위험지역', '#' + after.id + ' ' + text(after.title)], ['유형', after.hazardTypeLabel], ['반경', text(after.radiusM) + 'm'],
       ['위치', text(after.latitude) + ', ' + text(after.longitude)], ['상태', after.statusLabel]].forEach(function (pair) {
        var wrap = el('div'); wrap.appendChild(el('dt', null, pair[0])); wrap.appendChild(el('dd', null, text(pair[1]))); list.appendChild(wrap);
      });
      previewBox.appendChild(list);
      previewBox.appendChild(el('p', 'muted', '병합되는 ' + (data.sources || []).length + '건은 삭제되지 않고 원본 제보·제보자·이력이 보존됩니다. 라이더 앱에는 대표 위험지역만 전달됩니다.'));
      typeConfirm.hidden = !data.needsTypeConfirmation;
      if (!data.needsTypeConfirmation) typeCheck.checked = false;
      confirmButton.disabled = !data.canMerge || (data.needsTypeConfirmation && !typeCheck.checked);
    }

    function preview() {
      var req = request();
      lastPreview = null;
      if (!req.canonicalId || !req.sourceIds.length) {
        previewBox.innerHTML = '<p>대표 위험지역과 병합할 위험지역을 1건 이상 선택해 주세요.</p>';
        confirmButton.disabled = true; return;
      }
      api('/admin/api/hazards/merge/preview', 'POST', req).then(function (data) { lastPreview = data; show(''); renderPreview(data); })
        .catch(function (error) { confirmButton.disabled = true; show(error.message); });
    }
    function previewSoon() { clearTimeout(timer); timer = setTimeout(preview, 250); }

    document.querySelectorAll('.canonical-radio').forEach(function (r) {
      r.addEventListener('change', function () { fillFromCanonical(); preview(); drawMap(); });
    });
    document.querySelectorAll('.include-check').forEach(function (c) { c.addEventListener('change', function () { preview(); drawMap(); }); });
    fields.forEach(function (name) { var input = root.querySelector('#' + name); if (input) input.addEventListener('input', previewSoon); });
    typeCheck.addEventListener('change', function () {
      confirmButton.disabled = !(lastPreview && lastPreview.canMerge && (!lastPreview.needsTypeConfirmation || typeCheck.checked));
    });

    confirmButton.addEventListener('click', function () {
      var req = request();
      if (!window.confirm('선택한 ' + req.sourceIds.length + '건을 대표 위험지역 #' + req.canonicalId + '에 병합할까요?\n병합된 항목은 앱에 더 이상 표시되지 않습니다. (원본은 삭제되지 않습니다)')) return;
      setBusy(root, true);
      api('/admin/api/hazards/merge', 'POST', req).then(function () {
        window.location.href = '/admin/hazards/' + req.canonicalId;
      }).catch(function (error) {
        // the edits stay in the form
        setBusy(root, false);
        show(error.message);
        preview();
      });
    });

    var mapState = null;
    function drawMap() {
      if (!mapState) return;
      mapState.shapes.forEach(function (s) { s.circle.setMap(null); s.overlay.setMap(null); });
      mapState.shapes = [];
      var canonical = canonicalRow();
      var bounds = new mapState.maps.LatLngBounds();
      rows.forEach(function (r) {
        if (r.dataset.merged === 'true') return;
        var lat = Number(r.dataset.lat), lng = Number(r.dataset.lng);
        if (!isFinite(lat) || !isFinite(lng)) return;
        var shape = drawZone(mapState.maps, mapState.map, lat, lng, Number(r.dataset.radius) || 30, r === canonical,
          (r === canonical ? '대표 ' : '') + '#' + r.dataset.id + ' ' + r.dataset.title);
        if (!r.querySelector('.include-check').checked && r !== canonical) { shape.circle.setOptions({ fillOpacity: 0.05 }); }
        mapState.shapes.push(shape);
        bounds.extend(shape.position);
      });
      if (mapState.shapes.length) mapState.map.setBounds(bounds);
    }
    withKakao(function (maps) {
      var element = document.getElementById('merge-map');
      element.innerHTML = '';
      mapState = { maps: maps, map: new maps.Map(element, { center: new maps.LatLng(35.89617906, 128.62171907), level: 5 }), shapes: [] };
      drawMap();
    });

    fillFromCanonical();
    preview();
  }
}());
