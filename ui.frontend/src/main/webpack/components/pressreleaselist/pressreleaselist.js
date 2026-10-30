(function () {
  'use strict';

  var MONTHS = ['January', 'February', 'March', 'April', 'May', 'June',
                'July', 'August', 'September', 'October', 'November', 'December'];

  var DEFAULT_ENDPOINT = '/graphql/execute.json/multisite';
  var PAGE_SIZE = 4;          // hamesha 4 cards (dialog ki value ignore hoti hai)
  var MIN_LOADING_MS = 350;   // skeleton kam se kam itni der dikhe, taaki flicker na ho

  var CAL_ICON =
    '<svg class="cmp-prl__cal" width="16" height="16" viewBox="0 0 24 24" fill="none" ' +
    'stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">' +
    '<rect x="3" y="5" width="18" height="16" rx="3"></rect>' +
    '<path d="M3 10h18M8 3v4M16 3v4"></path></svg>';

  function esc(s) {
    var d = document.createElement('div');
    d.textContent = s == null ? '' : s;
    return d.innerHTML;
  }

  function pad(n) {
    return ('0' + n).slice(-2);
  }

  function fmt(dt) {
    return dt.getUTCFullYear() + '-' + pad(dt.getUTCMonth() + 1) + '-' + pad(dt.getUTCDate());
  }

  function PressList(root) {
    this.root = root;
    this.reqId = 0;
    this.cfg = {
      endpoint: DEFAULT_ENDPOINT,
      rootPath: root.dataset.rootPath,
      pageSize: PAGE_SIZE,
      startYear: parseInt(root.dataset.startYear, 10) || 2020,
      heading: root.dataset.heading || 'All Press Releases',
      categories: (root.dataset.categories || '').split(',').filter(Boolean)
    };
    this.state = {
      year: '',
      month: '',
      category: '',
      sort: root.dataset.defaultSort || 'publishDate DESC',
      page: 1
    };
    if (!this.cfg.rootPath) { return; }
    this.build();
    this.load();
  }

  // Ek dropdown-style filter group (Year - All v)
  function group(key, label, listHtml) {
    return '<div class="cmp-prl__group" data-group="' + key + '">' +
      '<button type="button" class="cmp-prl__toggle" aria-expanded="false">' +
      '<span class="cmp-prl__label">' + label + '</span>' +
      '<span class="cmp-prl__sep"> - </span>' +
      '<span class="cmp-prl__value">All</span>' +
      '<span class="cmp-prl__chev" aria-hidden="true"></span></button>' +
      '<ul>' + listHtml + '</ul></div>';
  }

  PressList.prototype.build = function () {
    var c = this.cfg, now = new Date().getFullYear();
    var yrs = '', mon = '', cats = '', i;

    yrs += '<li><button type="button" data-f="year" data-v="" class="is-active">All</button></li>';
    for (i = now; i >= c.startYear; i--) {
      yrs += '<li><button type="button" data-f="year" data-v="' + i + '">' + i + '</button></li>';
    }

    mon += '<li><button type="button" data-f="month" data-v="" class="is-active">All</button></li>';
    MONTHS.forEach(function (m, idx) {
      mon += '<li><button type="button" data-f="month" data-v="' + (idx + 1) + '">' + m + '</button></li>';
    });

    cats += '<li><button type="button" data-f="category" data-v="" class="is-active">All</button></li>';
    c.categories.forEach(function (x) {
      cats += '<li><button type="button" data-f="category" data-v="' + esc(x) + '">' + esc(x) + '</button></li>';
    });

    this.root.innerHTML =
      '<aside class="cmp-prl__filters"><h3>Filter By</h3>' +
      group('year', 'Year', yrs) +
      group('month', 'Month', mon) +
      group('category', 'Category', cats) +
      '</aside>' +
      '<div class="cmp-prl__main">' +
      '<div class="cmp-prl__bar" aria-hidden="true"></div>' +
      '<div class="cmp-prl__head"><div><h2>' + esc(c.heading) + '</h2>' +
      '<p class="cmp-prl__count">&nbsp;</p></div>' +
      '<select class="cmp-prl__sort" aria-label="Sort by">' +
      '<option value="publishDate DESC">Newest First</option>' +
      '<option value="publishDate ASC">Oldest First</option></select></div>' +
      '<div class="cmp-prl__grid" aria-live="polite"></div><nav class="cmp-prl__pager"></nav></div>';

    this.grid = this.root.querySelector('.cmp-prl__grid');
    this.root.querySelector('.cmp-prl__sort').value = this.state.sort;
    this.toggleMonth();
    this.bind();
  };

  PressList.prototype.closeGroups = function (except) {
    this.root.querySelectorAll('.cmp-prl__group.is-open').forEach(function (g) {
      if (g !== except) {
        g.classList.remove('is-open');
        g.querySelector('.cmp-prl__toggle').setAttribute('aria-expanded', 'false');
      }
    });
  };

  // "Year - All" me selected value ko update karta hai
  PressList.prototype.syncLabels = function () {
    this.root.querySelectorAll('.cmp-prl__group').forEach(function (g) {
      var a = g.querySelector('button[data-f].is-active');
      g.querySelector('.cmp-prl__value').textContent = a ? a.textContent : 'All';
    });
  };

  PressList.prototype.bind = function () {
    this.root.querySelector('.cmp-prl__filters').addEventListener('click', (e) => {
      // Dropdown open / close
      var t = e.target.closest('.cmp-prl__toggle');
      if (t) {
        if (t.disabled) { return; }
        var g = t.parentNode, open = g.classList.contains('is-open');
        this.closeGroups(g);
        g.classList.toggle('is-open', !open);
        t.setAttribute('aria-expanded', String(!open));
        return;
      }

      // Option select
      var b = e.target.closest('button[data-f]');
      if (!b || b.disabled) { return; }
      var f = b.dataset.f;
      if (this.state[f] === b.dataset.v) { this.closeGroups(); return; }   // same value, reload nahi
      this.state[f] = b.dataset.v;
      if (f === 'year') {
        this.state.month = '';
        this.toggleMonth();
        this.mark('month', '');
      }
      this.mark(f, b.dataset.v);
      this.syncLabels();
      this.closeGroups();
      this.state.page = 1;
      this.load();
    });

    // Bahar click karne par dropdown band
    document.addEventListener('click', (e) => {
      if (!this.root.contains(e.target)) { this.closeGroups(); }
    });

    this.root.querySelector('.cmp-prl__sort').addEventListener('change', (e) => {
      this.state.sort = e.target.value;
      this.state.page = 1;
      this.load();
    });

    this.root.querySelector('.cmp-prl__pager').addEventListener('click', (e) => {
      var b = e.target.closest('button[data-p]');
      if (!b || b.disabled) { return; }
      this.state.page = parseInt(b.dataset.p, 10);
      this.load();
      this.root.scrollIntoView({ behavior: 'smooth', block: 'start' });
    });
  };

  PressList.prototype.mark = function (f, v) {
    this.root.querySelectorAll('button[data-f="' + f + '"]').forEach(function (b) {
      b.classList.toggle('is-active', b.dataset.v === String(v));
    });
  };

  PressList.prototype.toggleMonth = function () {
    var g = this.root.querySelector('[data-group="month"]');
    var disabled = !this.state.year;
    g.classList.toggle('is-disabled', disabled);
    g.querySelector('.cmp-prl__toggle').disabled = disabled;
    if (disabled) {
      g.classList.remove('is-open');
      g.querySelector('.cmp-prl__toggle').setAttribute('aria-expanded', 'false');
    }
  };

  // Persisted query URL banata hai.
  // AEM 6.5.22 me AFTER / BEFORE boundary exclusive hote hain.
  // Sort query ke andar hardcoded hai, isliye query name me asc/desc jaata hai
  // (URL me space / %20 ki problem avoid karne ke liye).
  PressList.prototype.buildUrl = function () {
    var s = this.state, c = this.cfg, size = c.pageSize;
    var after = '1900-01-01', before = '2100-01-01';

    if (s.year) {
      var y = parseInt(s.year, 10);
      if (s.month) {
        var m = parseInt(s.month, 10);
        after = fmt(new Date(Date.UTC(y, m - 1, 0)));   // pichle mahine ka last day
        before = fmt(new Date(Date.UTC(y, m, 1)));      // agle mahine ka first day
      } else {
        after = (y - 1) + '-12-31';
        before = (y + 1) + '-01-01';
      }
    }

    var dir = s.sort.indexOf('ASC') > -1 ? 'asc' : 'desc';
    var name = s.category ? 'press-releases-by-category-' + dir : 'press-releases-' + dir;

    var params = ['path=' + c.rootPath, 'after=' + after, 'before=' + before];
    if (s.category) {
      // "Airports & Aero Services" -> "Airports" (query me CONTAINS operator hai)
      var catKey = s.category.split(/[\s&]+/)[0];
      params.push('category=' + encodeURIComponent(catKey));
    }
    params.push('limit=' + size, 'offset=' + ((s.page - 1) * size));

    var base = c.endpoint.replace(/\/$/, '');
    return base + '/' + name + ';' + params.join(';');
  };

  // Loading ke time 4 shimmer (skeleton) cards dikhte hain
  PressList.prototype.showSkeleton = function () {
    var html = '', i;
    for (i = 0; i < this.cfg.pageSize; i++) {
      html += '<article class="cmp-prl__card is-skeleton" aria-hidden="true">' +
        '<div class="cmp-prl__img"></div>' +
        '<div class="cmp-prl__body">' +
        '<div class="cmp-prl__sk cmp-prl__sk--line"></div>' +
        '<div class="cmp-prl__sk cmp-prl__sk--line cmp-prl__sk--short"></div>' +
        '<div class="cmp-prl__sk cmp-prl__sk--meta"></div>' +
        '</div></article>';
    }
    // Height lock: skeleton aur result ke beech page jump na kare
    this.grid.style.minHeight = this.grid.offsetHeight + 'px';
    this.grid.innerHTML = html;
  };

  PressList.prototype.load = function () {
    var id = ++this.reqId, started = Date.now();

    this.root.classList.add('is-loading');
    this.showSkeleton();

    fetch(this.buildUrl())
      .then((r) => r.json())
      .then((res) => new Promise((ok) => {
        var wait = Math.max(0, MIN_LOADING_MS - (Date.now() - started));
        setTimeout(function () { ok(res); }, wait);
      }))
      .then((res) => {
        if (id !== this.reqId) { return; }   // purani request ka response ignore
        if (res.errors) { console.error('GraphQL errors', res.errors); }
        var d = res.data || {};
        this.render((d.page && d.page.items) || [],
                    (d.total && d.total.items.length) || 0);
        this.root.classList.remove('is-loading');
      })
      .catch((err) => {
        if (id !== this.reqId) { return; }
        console.error('Press list error', err);
        this.grid.innerHTML = '<p class="cmp-prl__msg">Something went wrong. Please try again.</p>';
        this.root.classList.remove('is-loading');
      });
  };

  PressList.prototype.render = function (items, total) {
    var s = this.state, size = this.cfg.pageSize;
    var from = total ? (s.page - 1) * size + 1 : 0;
    var to = Math.min(s.page * size, total);

    this.root.querySelector('.cmp-prl__count').textContent =
      'Displaying ' + from + ' - ' + to + ' of ' + total;

    var html = items.map(function (it, idx) {
      var img = it.image && it.image._path
        ? '<img src="' + esc(it.image._path) + '" alt="' + esc(it.title) + '"/>'
        : '';
      var dt = it.publishDate
        ? new Date(it.publishDate).toLocaleDateString('en-GB',
            { day: '2-digit', month: 'short', year: 'numeric', timeZone: 'UTC' })
        : '';
      var title = it.link
        ? '<a href="' + esc(it.link) + '">' + esc(it.title) + '</a>'
        : esc(it.title);

      return '<article class="cmp-prl__card is-in" style="--i:' + idx + '">' +
        '<div class="cmp-prl__img">' + img + '</div>' +
        '<div class="cmp-prl__body"><h3>' + title + '</h3>' +
        '<div class="cmp-prl__meta"><span class="cmp-prl__tag">' + esc(it.category) + '</span>' +
        '<span class="cmp-prl__date">' + CAL_ICON + '<span>' + esc(dt) + '</span></span></div></div></article>';
    }).join('');

    this.grid.innerHTML = html || '<p class="cmp-prl__msg">No press releases found.</p>';
    this.grid.style.minHeight = '';

    // Image load hone par fade-in
    this.grid.querySelectorAll('.cmp-prl__img img').forEach(function (img) {
      function done() { img.classList.add('is-loaded'); }
      if (img.complete) { done(); }
      else {
        img.addEventListener('load', done);
        img.addEventListener('error', done);
      }
    });

    this.pager(Math.ceil(total / size));
  };

  PressList.prototype.pager = function (pages) {
    var cur = this.state.page, h = '', start, end, i;
    var nav = this.root.querySelector('.cmp-prl__pager');

    if (pages <= 1) { nav.innerHTML = ''; return; }

    start = Math.max(1, cur - 2);
    end = Math.min(pages, start + 4);
    start = Math.max(1, end - 4);

    h += '<button data-p="' + (cur - 1) + '"' + (cur === 1 ? ' disabled' : '') + '>&laquo; Prev</button>';
    for (i = start; i <= end; i++) {
      h += '<button data-p="' + i + '"' + (i === cur ? ' class="is-active"' : '') + '>' + i + '</button>';
    }
    h += '<button data-p="' + (cur + 1) + '"' + (cur === pages ? ' disabled' : '') + '>Next &raquo;</button>';
    nav.innerHTML = h;
  };

  function initAll() {
    document.querySelectorAll('[data-cmp-prl]').forEach(function (el) {
      if (!el.__prl) { el.__prl = new PressList(el); }
    });
  }

  if (document.readyState !== 'loading') {
    initAll();
  } else {
    document.addEventListener('DOMContentLoaded', initAll);
  }

  // Author mode me component drop/edit ke baad dobara init
  if (window.Granite && window.Granite.author && window.jQuery) {
    window.jQuery(document).on('cq-content-frame-loaded cq-overlay-rendered', initAll);
  }
})();