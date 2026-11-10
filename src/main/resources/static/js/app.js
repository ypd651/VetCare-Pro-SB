(() => {
    const {
        escapeHtml,
        formatDate,
        formatDateTime,
        formatTime,
        toLocalDateInput,
        toLocalDateTimeInput,
        addDaysToDateInput,
        buildWhatsAppUrl,
        speciesMeta,
        tipoCitaLabel,
        prioridadCitaMeta,
        estadoCitaMeta,
        estadoVacunaMeta,
        initials,
        debounce,
        showToast
    } = VetUtils;

    const state = {
        view: 'dashboard',
        dashboard: null,
        propietarios: [],
        mascotas: [],
        citas: [],
        consultas: [],
        vacunas: [],
        filters: {
            propietarioQ: '',
            mascotaQ: '',
            mascotaEspecie: '',
            mascotaPropietarioId: null,
            citaScope: 'proximas',
            citaQ: '',
            consultaQ: '',
            vacunaEstado: 'ALERTAS',
            vacunaTipo: '',
            vacunaQ: ''
        },
        expediente: {
            mascotaId: null,
            tab: 'consultas',
            data: null
        }
    };

    const $view = document.getElementById('view-container');
    const $sidebar = document.getElementById('sidebar');
    const $mobileBackdrop = document.getElementById('mobile-backdrop');
    const $globalSearch = document.getElementById('global-search-input');
    const $globalResults = document.getElementById('global-search-results');

    const $drawer = document.getElementById('expediente-drawer');
    const $drawerBackdrop = document.getElementById('drawer-backdrop');
    const $drawerTitle = document.getElementById('drawer-title');
    const $drawerBody = document.getElementById('expediente-body');

    const $formBackdrop = document.getElementById('form-modal-backdrop');
    const $formTitle = document.getElementById('form-modal-title');
    const $formBody = document.getElementById('form-modal-body');

    const $confirmBackdrop = document.getElementById('confirm-modal-backdrop');
    const $confirmTitle = document.getElementById('confirm-modal-title');
    const $confirmMsg = document.getElementById('confirm-modal-message');
    const $confirmAccept = document.getElementById('confirm-accept-btn');
    let confirmCallback = null;

    // ---------- Initialization & Theme ----------
    function initTheme() {
        const saved = localStorage.getItem('vetcare-theme');
        const prefersDark = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
        const theme = saved || (prefersDark ? 'dark' : 'light');
        applyTheme(theme);
    }

    function applyTheme(theme) {
        document.documentElement.setAttribute('data-theme', theme);
        localStorage.setItem('vetcare-theme', theme);
        const moon = document.getElementById('theme-icon-moon');
        const sun = document.getElementById('theme-icon-sun');
        if (theme === 'dark') {
            moon.classList.add('hidden');
            sun.classList.remove('hidden');
        } else {
            sun.classList.add('hidden');
            moon.classList.remove('hidden');
        }
    }

    function toggleSidebar(open) {
        $sidebar.classList.toggle('open', open);
        $mobileBackdrop.classList.toggle('hidden', !open);
    }

    // ---------- Data Synchronization ----------
    async function refreshSidebarAndLookups() {
        const [dash, props, pets] = await Promise.all([
            VetApi.dashboard(),
            VetApi.getPropietarios(),
            VetApi.getMascotas()
        ]);
        state.dashboard = dash;
        state.propietarios = props;
        state.mascotas = pets;
        updateNavBadges(dash);
    }

    function updateNavBadges(dash) {
        if (!dash) return;
        setBadge('badge-citas', dash.citasPendientes);
        setBadge('badge-mascotas', dash.totalMascotas);
        setBadge('badge-propietarios', dash.totalPropietarios);
        setBadge('badge-vacunas', dash.vacunasVencidas + dash.vacunasProximas);
    }

    function setBadge(id, value) {
        const el = document.getElementById(id);
        if (!el) return;
        if (value > 0) {
            el.textContent = value;
            el.classList.remove('hidden');
        } else {
            el.classList.add('hidden');
        }
    }

    async function navigateTo(view) {
        state.view = view;
        document.querySelectorAll('.nav-item').forEach(btn => {
            btn.classList.toggle('active', btn.dataset.view === view);
        });
        toggleSidebar(false);
        await loadAndRenderView();
    }

    async function loadAndRenderView() {
        $view.innerHTML = `<div class="loading-state"><div class="spinner"></div><p>Cargando información...</p></div>`;
        const connPill = document.getElementById('api-connection-pill');
        try {
            await refreshSidebarAndLookups();
            if (connPill) {
                connPill.className = 'badge badge-success';
                connPill.textContent = '● Backend Spring Boot conectado';
            }
            if (state.view === 'dashboard') {
                renderDashboard();
            } else if (state.view === 'propietarios') {
                state.propietarios = await VetApi.getPropietarios(state.filters.propietarioQ);
                renderPropietarios();
            } else if (state.view === 'mascotas') {
                state.mascotas = await VetApi.getMascotas({
                    q: state.filters.mascotaQ,
                    especie: state.filters.mascotaEspecie,
                    propietarioId: state.filters.mascotaPropietarioId
                });
                renderMascotas();
            } else if (state.view === 'citas') {
                await loadAndRenderCitas();
            } else if (state.view === 'consultas') {
                state.consultas = await VetApi.getConsultas({ q: state.filters.consultaQ });
                renderConsultas();
            } else if (state.view === 'vacunas') {
                await loadAndRenderVacunas();
            }

            if (state.expediente.mascotaId && !$drawer.classList.contains('hidden')) {
                await refreshOpenExpediente();
            }
        } catch (err) {
            if (connPill) {
                connPill.className = 'badge badge-danger';
                connPill.textContent = '● Backend sin conexión';
            }
            $view.innerHTML = `
                <div class="card"><div class="empty-state">
                    <div class="empty-state-icon">⚠️</div>
                    <h4>No se pudo cargar la vista</h4>
                    <p>${escapeHtml(err.message)}</p>
                    <button class="btn btn-primary btn-sm" style="margin-top:14px;" id="retry-load-btn">Reintentar conexión</button>
                </div></div>
            `;
            const retryBtn = document.getElementById('retry-load-btn');
            if (retryBtn) retryBtn.addEventListener('click', loadAndRenderView);
        }
    }

    async function loadAndRenderCitas() {
        const params = { q: state.filters.citaQ };
        const scope = state.filters.citaScope;
        if (scope === 'hoy') params.fecha = 'hoy';
        else if (scope === 'proximas') params.fecha = 'proximas';
        else if (scope === 'URGENTE') params.prioridad = 'URGENTE';
        else if (scope === 'COMPLETADA' || scope === 'CANCELADA') params.estado = scope;
        state.citas = await VetApi.getCitas(params);
        renderCitas();
    }

    async function loadAndRenderVacunas() {
        const estado = state.filters.vacunaEstado === 'TODAS' ? '' : state.filters.vacunaEstado;
        state.vacunas = await VetApi.getVacunas({
            estado,
            tipo: state.filters.vacunaTipo,
            q: state.filters.vacunaQ
        });
        renderVacunas();
    }

    // ---------- Dashboard View ----------
    function renderDashboard() {
        const d = state.dashboard;
        const alertasTotal = d.vacunasVencidas + d.vacunasProximas;

        $view.innerHTML = `
            <div class="page-header">
                <div>
                    <h1>Panel Principal</h1>
                    <p class="page-subtitle">Resumen operativo de la clínica, agenda de hoy y alertas sanitarias.</p>
                </div>
                <div class="row-actions">
                    <button class="btn btn-outline btn-sm" data-action="new-propietario">+ Propietario</button>
                    <button class="btn btn-primary btn-sm" data-action="new-consulta">+ Registrar Consulta</button>
                </div>
            </div>

            <section class="kpi-grid">
                <div class="kpi-card" data-nav="citas" data-cita-scope="hoy">
                    <div>
                        <div class="kpi-label">Citas para Hoy</div>
                        <div class="kpi-value">${d.citasHoy}</div>
                        <div class="kpi-note ${d.urgenciasActivas > 0 ? 'urgent' : ''}">
                            ${d.urgenciasActivas > 0 ? `🚨 ${d.urgenciasActivas} urgencia(s) activa(s)` : `${d.citasPendientes} pendientes en total`}
                        </div>
                    </div>
                    <div class="kpi-icon teal">📅</div>
                </div>

                <div class="kpi-card" data-nav="vacunas" data-vacuna-estado="ALERTAS">
                    <div>
                        <div class="kpi-label">Alertas de Vacunas</div>
                        <div class="kpi-value">${alertasTotal}</div>
                        <div class="kpi-note ${d.vacunasVencidas > 0 ? 'urgent' : ''}">
                            ${d.vacunasVencidas} vencida(s) · ${d.vacunasProximas} próxima(s)
                        </div>
                    </div>
                    <div class="kpi-icon red">💉</div>
                </div>

                <div class="kpi-card" data-nav="mascotas">
                    <div>
                        <div class="kpi-label">Pacientes Activos</div>
                        <div class="kpi-value">${d.totalMascotas}</div>
                        <div class="kpi-note">${d.consultasMes} consulta(s) este mes</div>
                    </div>
                    <div class="kpi-icon indigo">🐾</div>
                </div>

                <div class="kpi-card" data-nav="propietarios">
                    <div>
                        <div class="kpi-label">Propietarios</div>
                        <div class="kpi-value">${d.totalPropietarios}</div>
                        <div class="kpi-note">Base de clientes registrada</div>
                    </div>
                    <div class="kpi-icon amber">👥</div>
                </div>
            </section>

            <section class="dashboard-grid">
                <div class="card">
                    <div class="card-header">
                        <div class="card-title-wrap">
                            <h3>🗓️ Agenda de Hoy</h3>
                            <span class="badge badge-teal">${d.citasHoyLista.length}</span>
                        </div>
                        <button class="btn btn-outline btn-xs" data-nav="citas" data-cita-scope="todas">Ver toda la agenda</button>
                    </div>
                    <div class="card-body">
                        ${d.citasHoyLista.length === 0 ? `
                            <div class="empty-state">
                                <div class="empty-state-icon">☀️</div>
                                <h4>Sin citas programadas para hoy</h4>
                                <p>Puedes agendar una nueva cita desde el botón superior.</p>
                            </div>
                        ` : `
                            <div class="agenda-list">
                                ${d.citasHoyLista.map(renderDashboardCitaItem).join('')}
                            </div>
                        `}
                    </div>
                </div>

                <div class="card">
                    <div class="card-header">
                        <div class="card-title-wrap">
                            <h3>🚨 Alertas de Vacunación</h3>
                            <span class="badge ${alertasTotal > 0 ? 'badge-danger' : 'badge-success'}">${alertasTotal}</span>
                        </div>
                        <button class="btn btn-outline btn-xs" data-nav="vacunas" data-vacuna-estado="ALERTAS">Gestionar</button>
                    </div>
                    <div class="card-body">
                        ${d.alertasVacunas.length === 0 ? `
                            <div class="empty-state">
                                <div class="empty-state-icon">✅</div>
                                <h4>Todo al día</h4>
                                <p>No hay vacunas vencidas ni próximas en los siguientes 15 días.</p>
                            </div>
                        ` : `
                            <div class="alert-list">
                                ${d.alertasVacunas.slice(0, 6).map(renderVacunaAlertItem).join('')}
                            </div>
                        `}
                    </div>
                </div>
            </section>

            <section class="dashboard-grid">
                <div class="card">
                    <div class="card-header">
                        <div class="card-title-wrap">
                            <h3>🩺 Próximas Revisiones Médicas (14 días)</h3>
                        </div>
                        <button class="btn btn-outline btn-xs" data-nav="consultas">Historial</button>
                    </div>
                    <div class="card-body">
                        ${d.proximasRevisiones.length === 0 ? `
                            <div class="empty-state">
                                <h4>Sin revisiones pendientes próximas</h4>
                                <p>Las revisiones programadas en consultas aparecerán aquí.</p>
                            </div>
                        ` : `
                            <div class="agenda-list">
                                ${d.proximasRevisiones.map(cm => `
                                    <div class="agenda-item">
                                        <div>
                                            <div class="cell-title">
                                                ${escapeHtml(cm.nombreMascota)} · <span class="badge badge-info">${formatDate(cm.proximaRevision)}</span>
                                            </div>
                                            <div class="cell-sub">${escapeHtml(cm.diagnostico)} · Dueño: ${escapeHtml(cm.nombrePropietario)}</div>
                                        </div>
                                        <div class="row-actions">
                                            <button class="btn btn-soft-primary btn-xs" data-open-expediente="${cm.mascotaId}">Ver Expediente</button>
                                        </div>
                                    </div>
                                `).join('')}
                            </div>
                        `}
                    </div>
                </div>

                <div class="card">
                    <div class="card-header">
                        <div class="card-title-wrap">
                            <h3>📊 Pacientes por Especie</h3>
                        </div>
                    </div>
                    <div class="card-body">
                        ${renderSpeciesChart(d.distribucionEspecies, d.totalMascotas)}
                    </div>
                </div>
            </section>
        `;
    }

    function renderDashboardCitaItem(c) {
        const pMeta = prioridadCitaMeta(c.prioridad);
        const eMeta = estadoCitaMeta(c.estado);
        const sMeta = speciesMeta(c.especieMascota);
        const isUrgent = c.prioridad === 'URGENTE' && c.estado !== 'COMPLETADA' && c.estado !== 'CANCELADA';

        return `
            <div class="agenda-item ${isUrgent ? 'urgente' : ''}">
                <div style="display:flex;align-items:center;gap:12px;">
                    <div class="agenda-time">${formatTime(c.fechaHora)}</div>
                    <div>
                        <div class="cell-title">
                            ${sMeta.icon} ${escapeHtml(c.nombreMascota)}
                            <span class="badge ${pMeta.cls}">${pMeta.label}</span>
                            <span class="badge ${eMeta.cls}">${eMeta.label}</span>
                        </div>
                        <div class="cell-sub">
                            ${escapeHtml(tipoCitaLabel(c.tipo))} · ${escapeHtml(c.veterinario)} · Dueño: ${escapeHtml(c.nombrePropietario)}
                        </div>
                        <div class="cell-sub">${escapeHtml(c.motivo)}</div>
                    </div>
                </div>
                <div class="row-actions">
                    <button class="btn btn-outline btn-xs" data-open-expediente="${c.mascotaId}">Expediente</button>
                    ${c.estado === 'PROGRAMADA' ? `<button class="btn btn-soft-warning btn-xs" data-cita-estado="${c.id}" data-nuevo-estado="EN_ATENCION">Iniciar</button>` : ''}
                    ${(c.estado === 'PROGRAMADA' || c.estado === 'EN_ATENCION') ? `<button class="btn btn-soft-success btn-xs" data-atender-cita="${c.id}">Atender</button>` : ''}
                </div>
            </div>
        `;
    }

    function renderVacunaAlertItem(v) {
        const meta = estadoVacunaMeta(v.estadoAlerta, v.diasParaVencimiento);
        const waUrl = buildWhatsAppUrl(v.telefonoPropietario, `Hola ${v.nombrePropietario}, te recordamos desde VetCare Pro que ${v.nombreMascota} tiene pendiente su dosis de "${v.nombre}".`);
        return `
            <div class="alert-item ${v.estadoAlerta === 'VENCIDA' ? 'vencida' : 'proxima'}">
                <div>
                    <div class="cell-title">
                        ${escapeHtml(v.nombreMascota)} · ${escapeHtml(v.nombre)}
                        <span class="badge ${meta.cls}">${meta.label}</span>
                    </div>
                    <div class="cell-sub">
                        Dueño: ${escapeHtml(v.nombrePropietario)} · Tel: ${escapeHtml(v.telefonoPropietario || '—')}
                    </div>
                </div>
                <div class="row-actions">
                    ${waUrl ? `<a class="btn btn-outline btn-xs" href="${waUrl}" target="_blank" rel="noopener">WhatsApp</a>` : ''}
                    <button class="btn btn-soft-primary btn-xs" data-refuerzo-vacuna="${v.id}">Refuerzo</button>
                </div>
            </div>
        `;
    }

    function renderSpeciesChart(especies, total) {
        if (!especies || especies.length === 0) {
            return `<div class="empty-state"><p>Sin pacientes registrados aún.</p></div>`;
        }
        return `
            <div class="species-bars">
                ${especies.map(item => {
                    const pct = total > 0 ? Math.round((item.cantidad / total) * 100) : 0;
                    const meta = speciesMeta(item.especie);
                    return `
                        <div class="species-row">
                            <div class="species-row-top">
                                <span>${meta.icon} ${escapeHtml(item.especie)}</span>
                                <span>${item.cantidad} (${pct}%)</span>
                            </div>
                            <div class="progress-track">
                                <div class="progress-fill" style="width:${pct}%"></div>
                            </div>
                        </div>
                    `;
                }).join('')}
            </div>
        `;
    }

    // ---------- Propietarios View ----------
    function renderPropietarios() {
        $view.innerHTML = `
            <div class="page-header">
                <div>
                    <h1>Propietarios</h1>
                    <p class="page-subtitle">Directorio de clientes, contacto rápido y mascotas asociadas.</p>
                </div>
                <button class="btn btn-primary" data-action="new-propietario">+ Nuevo Propietario</button>
            </div>

            <div class="toolbar">
                <div class="toolbar-controls">
                    <input id="propietario-search" class="input-search" type="search" placeholder="Buscar por nombre, documento, teléfono o correo..." value="${escapeHtml(state.filters.propietarioQ)}">
                </div>
                <span class="badge badge-neutral">${state.propietarios.length} propietario(s)</span>
            </div>

            <div class="card">
                <div class="card-body no-pad">
                    ${state.propietarios.length === 0 ? `
                        <div class="empty-state">
                            <div class="empty-state-icon">👥</div>
                            <h4>No se encontraron propietarios</h4>
                            <p>Registra un nuevo propietario o ajusta tu búsqueda.</p>
                        </div>
                    ` : `
                        <div class="table-wrap">
                            <table>
                                <thead>
                                    <tr>
                                        <th>Propietario</th>
                                        <th>Contacto</th>
                                        <th>Dirección</th>
                                        <th>Mascotas</th>
                                        <th style="text-align:right;">Acciones</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    ${state.propietarios.map(p => {
                                        const waUrl = buildWhatsAppUrl(p.telefono, `Hola ${p.nombre}, te saludamos de Clínica Veterinaria VetCare Pro.`);
                                        return `
                                            <tr>
                                                <td>
                                                    <div class="entity-cell">
                                                        <div class="avatar owner">${escapeHtml(initials(p.nombre))}</div>
                                                        <div>
                                                            <div class="cell-title">${escapeHtml(p.nombre)}</div>
                                                            <div class="cell-sub">${p.documento ? `Doc: ${escapeHtml(p.documento)}` : `ID #${p.id}`}</div>
                                                        </div>
                                                    </div>
                                                </td>
                                                <td>
                                                    <div class="contact-links">
                                                        <a class="contact-chip" href="tel:${escapeHtml(p.telefono)}">📞 ${escapeHtml(p.telefono)}</a>
                                                        ${waUrl ? `<a class="contact-chip" href="${waUrl}" target="_blank" rel="noopener">💬 WhatsApp</a>` : ''}
                                                    </div>
                                                    ${p.email ? `<div class="cell-sub" style="margin-top:4px;">✉️ ${escapeHtml(p.email)}</div>` : ''}
                                                </td>
                                                <td>
                                                    <div>${escapeHtml(p.direccion || '—')}</div>
                                                    ${p.notas ? `<div class="cell-sub">${escapeHtml(p.notas)}</div>` : ''}
                                                </td>
                                                <td>
                                                    <button class="btn btn-soft-primary btn-xs" data-ver-mascotas-prop="${p.id}">
                                                        🐾 ${p.cantidadMascotas} mascota${p.cantidadMascotas === 1 ? '' : 's'}
                                                    </button>
                                                </td>
                                                <td>
                                                    <div class="row-actions">
                                                        <button class="btn btn-outline btn-xs" data-new-mascota-prop="${p.id}">+ Mascota</button>
                                                        <button class="btn btn-outline btn-xs" data-edit-propietario="${p.id}">Editar</button>
                                                        <button class="btn btn-soft-danger btn-xs" data-delete-propietario="${p.id}">Eliminar</button>
                                                    </div>
                                                </td>
                                            </tr>
                                        `;
                                    }).join('')}
                                </tbody>
                            </table>
                        </div>
                    `}
                </div>
            </div>
        `;

        const searchEl = document.getElementById('propietario-search');
        searchEl.addEventListener('input', debounce(async (e) => {
            state.filters.propietarioQ = e.target.value;
            state.propietarios = await VetApi.getPropietarios(state.filters.propietarioQ);
            renderPropietarios();
            const restored = document.getElementById('propietario-search');
            if (restored) {
                restored.focus();
                restored.setSelectionRange(restored.value.length, restored.value.length);
            }
        }, 250));
    }

    // ---------- Mascotas View ----------
    function renderMascotas() {
        const speciesOptions = ['', 'Perro', 'Gato', 'Ave', 'Conejo', 'Otro'];
        const activeOwner = state.filters.mascotaPropietarioId
            ? state.propietarios.find(p => p.id === Number(state.filters.mascotaPropietarioId))
            : null;

        $view.innerHTML = `
            <div class="page-header">
                <div>
                    <h1>Pacientes (Mascotas)</h1>
                    <p class="page-subtitle">Accede al expediente clínico completo, alergias, peso y vacunas de cada paciente.</p>
                </div>
                <button class="btn btn-primary" data-action="new-mascota">+ Nuevo Paciente</button>
            </div>

            <div class="toolbar">
                <div class="filter-pills">
                    ${speciesOptions.map(sp => `
                        <button class="pill-btn ${state.filters.mascotaEspecie === sp ? 'active' : ''}" data-filter-especie="${escapeHtml(sp)}">
                            ${sp === '' ? 'Todas las especies' : `${speciesMeta(sp).icon} ${sp}`}
                        </button>
                    `).join('')}
                    ${activeOwner ? `
                        <button class="pill-btn active" data-clear-owner-filter="1">
                            Dueño: ${escapeHtml(activeOwner.nombre)} ✕
                        </button>
                    ` : ''}
                </div>
                <div class="toolbar-controls">
                    <input id="mascota-search" class="input-search" type="search" placeholder="Buscar mascota, raza, microchip o dueño..." value="${escapeHtml(state.filters.mascotaQ)}">
                </div>
            </div>

            <div class="card">
                <div class="card-body no-pad">
                    ${state.mascotas.length === 0 ? `
                        <div class="empty-state">
                            <div class="empty-state-icon">🐾</div>
                            <h4>No hay pacientes que coincidan</h4>
                            <p>Registra una nueva mascota o limpia los filtros aplicados.</p>
                        </div>
                    ` : `
                        <div class="table-wrap">
                            <table>
                                <thead>
                                    <tr>
                                        <th>Paciente</th>
                                        <th>Especie / Características</th>
                                        <th>Propietario</th>
                                        <th>Estado Clínico</th>
                                        <th style="text-align:right;">Acciones</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    ${state.mascotas.map(m => {
                                        const sm = speciesMeta(m.especie);
                                        return `
                                            <tr>
                                                <td>
                                                    <div class="entity-cell">
                                                        <div class="avatar pet">${sm.icon}</div>
                                                        <div>
                                                            <div class="cell-title">${escapeHtml(m.nombre)}</div>
                                                            <div class="cell-sub">
                                                                ${m.microchip ? `Chip: ${escapeHtml(m.microchip)}` : `Ficha #${m.id}`}
                                                                ${m.esterilizado ? ' · Esterilizado' : ''}
                                                            </div>
                                                        </div>
                                                    </div>
                                                </td>
                                                <td>
                                                    <div><span class="badge ${sm.badge}">${escapeHtml(m.especie)}</span> ${escapeHtml(m.raza || '')}</div>
                                                    <div class="cell-sub">
                                                        ${m.sexo ? `${escapeHtml(m.sexo)} · ` : ''}
                                                        ${m.edadAnios !== null && m.edadAnios !== undefined ? `${m.edadAnios} año(s)` : 'Edad N/D'}
                                                        ${m.pesoKg ? ` · ${m.pesoKg} kg` : ''}
                                                    </div>
                                                </td>
                                                <td>
                                                    <div class="cell-title">${escapeHtml(m.nombrePropietario)}</div>
                                                    <div class="cell-sub">📞 ${escapeHtml(m.telefonoPropietario || '—')}</div>
                                                </td>
                                                <td>
                                                    <div style="display:flex;flex-wrap:wrap;gap:5px;">
                                                        <span class="badge badge-neutral">🩺 ${m.totalConsultas} consulta(s)</span>
                                                        ${m.alertasVacunas > 0 ? `<span class="badge badge-danger">💉 ${m.alertasVacunas} alerta(s)</span>` : `<span class="badge badge-success">Vacunas OK</span>`}
                                                        ${m.alergias ? `<span class="badge badge-warning" title="${escapeHtml(m.alergias)}">⚠️ Alergia</span>` : ''}
                                                    </div>
                                                </td>
                                                <td>
                                                    <div class="row-actions">
                                                        <button class="btn btn-soft-primary btn-xs" data-open-expediente="${m.id}">Expediente</button>
                                                        <button class="btn btn-outline btn-xs" data-cita-mascota="${m.id}">+ Cita</button>
                                                        <button class="btn btn-outline btn-xs" data-edit-mascota="${m.id}">Editar</button>
                                                        <button class="btn btn-soft-danger btn-xs" data-delete-mascota="${m.id}">Eliminar</button>
                                                    </div>
                                                </td>
                                            </tr>
                                        `;
                                    }).join('')}
                                </tbody>
                            </table>
                        </div>
                    `}
                </div>
            </div>
        `;

        const searchEl = document.getElementById('mascota-search');
        searchEl.addEventListener('input', debounce(async (e) => {
            state.filters.mascotaQ = e.target.value;
            state.mascotas = await VetApi.getMascotas({
                q: state.filters.mascotaQ,
                especie: state.filters.mascotaEspecie,
                propietarioId: state.filters.mascotaPropietarioId
            });
            renderMascotas();
            const restored = document.getElementById('mascota-search');
            if (restored) {
                restored.focus();
                restored.setSelectionRange(restored.value.length, restored.value.length);
            }
        }, 250));
    }

    // ---------- Citas View ----------
    function renderCitas() {
        const scopes = [
            { id: 'proximas', label: 'Próximas' },
            { id: 'hoy', label: 'Hoy' },
            { id: 'URGENTE', label: '🚨 Urgencias' },
            { id: 'todas', label: 'Todas' },
            { id: 'COMPLETADA', label: 'Completadas' },
            { id: 'CANCELADA', label: 'Canceladas' }
        ];

        $view.innerHTML = `
            <div class="page-header">
                <div>
                    <h1>Agenda de Citas</h1>
                    <p class="page-subtitle">Control de citas con validación automática de conflictos de horario por veterinario.</p>
                </div>
                <button class="btn btn-primary" data-action="new-cita">+ Agendar Cita</button>
            </div>

            <div class="toolbar">
                <div class="filter-pills">
                    ${scopes.map(s => `
                        <button class="pill-btn ${state.filters.citaScope === s.id ? 'active' : ''}" data-cita-scope-btn="${s.id}">
                            ${s.label}
                        </button>
                    `).join('')}
                </div>
                <div class="toolbar-controls">
                    <input id="cita-search" class="input-search" type="search" placeholder="Buscar mascota, dueño, veterinario o motivo..." value="${escapeHtml(state.filters.citaQ)}">
                </div>
            </div>

            <div class="card">
                <div class="card-body no-pad">
                    ${state.citas.length === 0 ? `
                        <div class="empty-state">
                            <div class="empty-state-icon">📅</div>
                            <h4>No hay citas en este filtro</h4>
                            <p>Cambia el filtro superior o programa una nueva cita.</p>
                        </div>
                    ` : `
                        <div class="table-wrap">
                            <table>
                                <thead>
                                    <tr>
                                        <th>Fecha y Hora</th>
                                        <th>Paciente / Propietario</th>
                                        <th>Tipo y Motivo</th>
                                        <th>Veterinario</th>
                                        <th>Prioridad / Estado</th>
                                        <th style="text-align:right;">Acciones</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    ${state.citas.map(c => {
                                        const pMeta = prioridadCitaMeta(c.prioridad);
                                        const eMeta = estadoCitaMeta(c.estado);
                                        const sm = speciesMeta(c.especieMascota);
                                        return `
                                            <tr>
                                                <td>
                                                    <div class="cell-title">${formatDateTime(c.fechaHora)}</div>
                                                </td>
                                                <td>
                                                    <div class="cell-title">${sm.icon} ${escapeHtml(c.nombreMascota)}</div>
                                                    <div class="cell-sub">${escapeHtml(c.nombrePropietario)} · 📞 ${escapeHtml(c.telefonoPropietario || '—')}</div>
                                                </td>
                                                <td>
                                                    <div class="cell-title">${escapeHtml(tipoCitaLabel(c.tipo))}</div>
                                                    <div class="cell-sub">${escapeHtml(c.motivo)}</div>
                                                </td>
                                                <td>${escapeHtml(c.veterinario)}</td>
                                                <td>
                                                    <div style="display:flex;gap:5px;flex-wrap:wrap;">
                                                        <span class="badge ${pMeta.cls}">${pMeta.label}</span>
                                                        <span class="badge ${eMeta.cls}">${eMeta.label}</span>
                                                    </div>
                                                </td>
                                                <td>
                                                    <div class="row-actions">
                                                        ${c.estado === 'PROGRAMADA' ? `<button class="btn btn-soft-warning btn-xs" data-cita-estado="${c.id}" data-nuevo-estado="EN_ATENCION">Iniciar</button>` : ''}
                                                        ${(c.estado === 'PROGRAMADA' || c.estado === 'EN_ATENCION') ? `
                                                            <button class="btn btn-soft-success btn-xs" data-atender-cita="${c.id}">Atender</button>
                                                            <button class="btn btn-outline btn-xs" data-cita-estado="${c.id}" data-nuevo-estado="CANCELADA">Cancelar</button>
                                                        ` : ''}
                                                        <button class="btn btn-outline btn-xs" data-edit-cita="${c.id}">Editar</button>
                                                        <button class="btn btn-soft-danger btn-xs" data-delete-cita="${c.id}">Eliminar</button>
                                                    </div>
                                                </td>
                                            </tr>
                                        `;
                                    }).join('')}
                                </tbody>
                            </table>
                        </div>
                    `}
                </div>
            </div>
        `;

        const searchEl = document.getElementById('cita-search');
        searchEl.addEventListener('input', debounce(async (e) => {
            state.filters.citaQ = e.target.value;
            await loadAndRenderCitas();
            const restored = document.getElementById('cita-search');
            if (restored) {
                restored.focus();
                restored.setSelectionRange(restored.value.length, restored.value.length);
            }
        }, 250));
    }

    // ---------- Consultas Médicas View ----------
    function renderConsultas() {
        $view.innerHTML = `
            <div class="page-header">
                <div>
                    <h1>Consultas Médicas</h1>
                    <p class="page-subtitle">Historial clínico, signos vitales, diagnósticos y tratamientos formulados.</p>
                </div>
                <button class="btn btn-primary" data-action="new-consulta">+ Nueva Consulta</button>
            </div>

            <div class="toolbar">
                <div class="toolbar-controls">
                    <input id="consulta-search" class="input-search" type="search" placeholder="Buscar por paciente, diagnóstico, tratamiento o veterinario..." value="${escapeHtml(state.filters.consultaQ)}">
                </div>
                <span class="badge badge-neutral">${state.consultas.length} registro(s)</span>
            </div>

            <div class="card">
                <div class="card-body no-pad">
                    ${state.consultas.length === 0 ? `
                        <div class="empty-state">
                            <div class="empty-state-icon">🩺</div>
                            <h4>Sin consultas médicas registradas</h4>
                            <p>Registra una atención clínica para alimentar el historial del paciente.</p>
                        </div>
                    ` : `
                        <div class="table-wrap">
                            <table>
                                <thead>
                                    <tr>
                                        <th>Fecha</th>
                                        <th>Paciente</th>
                                        <th>Diagnóstico y Tratamiento</th>
                                        <th>Signos Vitales</th>
                                        <th>Veterinario</th>
                                        <th style="text-align:right;">Acciones</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    ${state.consultas.map(cm => `
                                        <tr>
                                            <td>
                                                <div class="cell-title">${formatDateTime(cm.fechaConsulta)}</div>
                                                ${cm.proximaRevision ? `<div class="cell-sub">Rev: ${formatDate(cm.proximaRevision)}</div>` : ''}
                                            </td>
                                            <td>
                                                <div class="cell-title">${speciesMeta(cm.especieMascota).icon} ${escapeHtml(cm.nombreMascota)}</div>
                                                <div class="cell-sub">${escapeHtml(cm.nombrePropietario)}</div>
                                            </td>
                                            <td>
                                                <div class="cell-title">${escapeHtml(cm.diagnostico)}</div>
                                                <div class="cell-sub">Rx: ${escapeHtml(cm.tratamiento)}</div>
                                            </td>
                                            <td>
                                                <div style="display:flex;gap:5px;flex-wrap:wrap;">
                                                    ${cm.pesoKg ? `<span class="badge badge-neutral">⚖️ ${cm.pesoKg} kg</span>` : ''}
                                                    ${cm.temperaturaC ? `<span class="badge badge-info">🌡️ ${cm.temperaturaC} °C</span>` : ''}
                                                    ${cm.frecuenciaCardiaca ? `<span class="badge badge-teal">❤️ ${cm.frecuenciaCardiaca} lpm</span>` : ''}
                                                </div>
                                            </td>
                                            <td>${escapeHtml(cm.veterinario)}</td>
                                            <td>
                                                <div class="row-actions">
                                                    <button class="btn btn-soft-primary btn-xs" data-open-expediente="${cm.mascotaId}">Expediente</button>
                                                    <button class="btn btn-outline btn-xs" data-edit-consulta="${cm.id}">Editar</button>
                                                    <button class="btn btn-soft-danger btn-xs" data-delete-consulta="${cm.id}">Eliminar</button>
                                                </div>
                                            </td>
                                        </tr>
                                    `).join('')}
                                </tbody>
                            </table>
                        </div>
                    `}
                </div>
            </div>
        `;

        const searchEl = document.getElementById('consulta-search');
        searchEl.addEventListener('input', debounce(async (e) => {
            state.filters.consultaQ = e.target.value;
            state.consultas = await VetApi.getConsultas({ q: state.filters.consultaQ });
            renderConsultas();
            const restored = document.getElementById('consulta-search');
            if (restored) {
                restored.focus();
                restored.setSelectionRange(restored.value.length, restored.value.length);
            }
        }, 250));
    }

    // ---------- Vacunas y Alertas View ----------
    function renderVacunas() {
        const estados = [
            { id: 'ALERTAS', label: '🚨 Alertas (Vencidas + Próximas)' },
            { id: 'VENCIDA', label: 'Vencidas' },
            { id: 'PROXIMA', label: 'Próximas (15 días)' },
            { id: 'AL_DIA', label: 'Al día' },
            { id: 'TODAS', label: 'Todas' }
        ];

        $view.innerHTML = `
            <div class="page-header">
                <div>
                    <h1>Vacunas y Desparasitación</h1>
                    <p class="page-subtitle">Monitoreo automático de esquemas preventivos, vencimientos y recordatorios a propietarios.</p>
                </div>
                <button class="btn btn-primary" data-action="new-vacuna">+ Registrar Vacuna / Dosis</button>
            </div>

            <div class="toolbar">
                <div class="filter-pills">
                    ${estados.map(e => `
                        <button class="pill-btn ${state.filters.vacunaEstado === e.id ? 'active' : ''}" data-vacuna-estado-btn="${e.id}">
                            ${e.label}
                        </button>
                    `).join('')}
                </div>
                <div class="toolbar-controls">
                    <select id="vacuna-tipo-filter" class="select-filter">
                        <option value="" ${state.filters.vacunaTipo === '' ? 'selected' : ''}>Vacunas y Desparasitantes</option>
                        <option value="VACUNA" ${state.filters.vacunaTipo === 'VACUNA' ? 'selected' : ''}>Solo Vacunas</option>
                        <option value="DESPARASITACION" ${state.filters.vacunaTipo === 'DESPARASITACION' ? 'selected' : ''}>Solo Desparasitación</option>
                    </select>
                    <input id="vacuna-search" class="input-search" type="search" placeholder="Buscar biológico, mascota o dueño..." value="${escapeHtml(state.filters.vacunaQ)}">
                </div>
            </div>

            <div class="card">
                <div class="card-body no-pad">
                    ${state.vacunas.length === 0 ? `
                        <div class="empty-state">
                            <div class="empty-state-icon">💉</div>
                            <h4>No hay registros para este filtro</h4>
                            <p>Registra una aplicación o selecciona "Todas" para ver el historial completo.</p>
                        </div>
                    ` : `
                        <div class="table-wrap">
                            <table>
                                <thead>
                                    <tr>
                                        <th>Paciente / Propietario</th>
                                        <th>Biológico / Tipo</th>
                                        <th>Aplicación</th>
                                        <th>Próxima Dosis</th>
                                        <th>Estado</th>
                                        <th style="text-align:right;">Acciones</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    ${state.vacunas.map(v => {
                                        const meta = estadoVacunaMeta(v.estadoAlerta, v.diasParaVencimiento);
                                        const waUrl = buildWhatsAppUrl(v.telefonoPropietario, `Hola ${v.nombrePropietario}, recordatorio de VetCare Pro: ${v.nombreMascota} tiene programada su dosis de ${v.nombre} (${formatDate(v.proximaDosis)}).`);
                                        return `
                                            <tr>
                                                <td>
                                                    <div class="cell-title">${speciesMeta(v.especieMascota).icon} ${escapeHtml(v.nombreMascota)}</div>
                                                    <div class="cell-sub">${escapeHtml(v.nombrePropietario)} · 📞 ${escapeHtml(v.telefonoPropietario || '—')}</div>
                                                </td>
                                                <td>
                                                    <div class="cell-title">${escapeHtml(v.nombre)}</div>
                                                    <div class="cell-sub">
                                                        <span class="badge ${v.tipo === 'VACUNA' ? 'badge-indigo' : 'badge-teal'}">
                                                            ${v.tipo === 'VACUNA' ? 'Vacuna' : 'Desparasitación'}
                                                        </span>
                                                        ${v.lote ? ` · Lote: ${escapeHtml(v.lote)}` : ''}
                                                    </div>
                                                </td>
                                                <td>${formatDate(v.fechaAplicacion)}</td>
                                                <td>${formatDate(v.proximaDosis)}</td>
                                                <td><span class="badge ${meta.cls}">${meta.label}</span></td>
                                                <td>
                                                    <div class="row-actions">
                                                        ${waUrl ? `<a class="btn btn-outline btn-xs" href="${waUrl}" target="_blank" rel="noopener">WhatsApp</a>` : ''}
                                                        <button class="btn btn-soft-primary btn-xs" data-refuerzo-vacuna="${v.id}">Refuerzo</button>
                                                        <button class="btn btn-outline btn-xs" data-edit-vacuna="${v.id}">Editar</button>
                                                        <button class="btn btn-soft-danger btn-xs" data-delete-vacuna="${v.id}">Eliminar</button>
                                                    </div>
                                                </td>
                                            </tr>
                                        `;
                                    }).join('')}
                                </tbody>
                            </table>
                        </div>
                    `}
                </div>
            </div>
        `;

        document.getElementById('vacuna-tipo-filter').addEventListener('change', async (e) => {
            state.filters.vacunaTipo = e.target.value;
            await loadAndRenderVacunas();
        });

        const searchEl = document.getElementById('vacuna-search');
        searchEl.addEventListener('input', debounce(async (e) => {
            state.filters.vacunaQ = e.target.value;
            await loadAndRenderVacunas();
            const restored = document.getElementById('vacuna-search');
            if (restored) {
                restored.focus();
                restored.setSelectionRange(restored.value.length, restored.value.length);
            }
        }, 250));
    }

    // ---------- Expediente Clínico Drawer ----------
    async function openExpediente(mascotaId, tab = 'consultas') {
        state.expediente.mascotaId = Number(mascotaId);
        state.expediente.tab = tab;
        $drawerBackdrop.classList.remove('hidden');
        $drawer.classList.remove('hidden');
        $drawerTitle.textContent = 'Cargando expediente...';
        $drawerBody.innerHTML = `<div class="loading-state"><div class="spinner"></div><p>Cargando historia clínica...</p></div>`;
        await refreshOpenExpediente();
    }

    async function refreshOpenExpediente() {
        if (!state.expediente.mascotaId) return;
        try {
            const exp = await VetApi.getExpediente(state.expediente.mascotaId);
            state.expediente.data = exp;
            renderExpediente(exp);
        } catch (err) {
            $drawerBody.innerHTML = `<div class="empty-state"><h4>Error cargando expediente</h4><p>${escapeHtml(err.message)}</p></div>`;
        }
    }

    function closeExpediente() {
        $drawer.classList.add('hidden');
        $drawerBackdrop.classList.add('hidden');
        state.expediente.mascotaId = null;
    }

    function renderExpediente(exp) {
        const m = exp.mascota;
        const p = exp.propietario;
        const sm = speciesMeta(m.especie);
        const waUrl = buildWhatsAppUrl(p.telefono, `Hola ${p.nombre}, te escribimos de VetCare Pro respecto a ${m.nombre}.`);

        $drawerTitle.textContent = `${sm.icon} ${m.nombre} — ${m.especie}`;

        $drawerBody.innerHTML = `
            <div class="expediente-banner">
                <div>
                    <div style="display:flex;align-items:center;gap:8px;flex-wrap:wrap;">
                        <strong style="font-size:1.1rem;">${escapeHtml(m.nombre)}</strong>
                        <span class="badge ${sm.badge}">${escapeHtml(m.especie)}</span>
                        ${m.raza ? `<span class="badge badge-neutral">${escapeHtml(m.raza)}</span>` : ''}
                        ${m.esterilizado ? `<span class="badge badge-success">Esterilizado</span>` : ''}
                    </div>
                    <div class="cell-sub" style="margin-top:6px;">
                        Propietario: <strong>${escapeHtml(p.nombre)}</strong> · 📞 ${escapeHtml(p.telefono)}
                        ${p.email ? ` · ✉️ ${escapeHtml(p.email)}` : ''}
                    </div>
                </div>
                <div class="row-actions no-print">
                    ${waUrl ? `<a class="btn btn-outline btn-xs" href="${waUrl}" target="_blank" rel="noopener">💬 WhatsApp</a>` : ''}
                    <button class="btn btn-outline btn-xs" data-edit-mascota="${m.id}">Editar Ficha</button>
                </div>
            </div>

            ${m.alergias ? `
                <div class="allergy-alert">
                    ⚠️ <strong>Alerta de Alergias / Sensibilidades:</strong> ${escapeHtml(m.alergias)}
                </div>
            ` : ''}

            <div class="meta-grid">
                <div class="meta-box"><span>Edad</span><strong>${m.edadAnios !== null && m.edadAnios !== undefined ? `${m.edadAnios} año(s)` : 'N/D'}</strong></div>
                <div class="meta-box"><span>Peso Actual</span><strong>${m.pesoKg ? `${m.pesoKg} kg` : 'N/D'}</strong></div>
                <div class="meta-box"><span>Sexo</span><strong>${escapeHtml(m.sexo || 'N/D')}</strong></div>
                <div class="meta-box"><span>Microchip</span><strong>${escapeHtml(m.microchip || 'Sin chip')}</strong></div>
            </div>

            <div class="row-actions no-print" style="justify-content:flex-start;">
                <button class="btn btn-primary btn-sm" data-consulta-mascota="${m.id}">+ Registrar Consulta</button>
                <button class="btn btn-soft-primary btn-sm" data-vacuna-mascota="${m.id}">+ Registrar Vacuna</button>
                <button class="btn btn-outline btn-sm" data-cita-mascota="${m.id}">+ Agendar Cita</button>
            </div>

            <div class="filter-pills no-print">
                <button class="pill-btn ${state.expediente.tab === 'consultas' ? 'active' : ''}" data-exp-tab="consultas">
                    🩺 Consultas (${exp.consultas.length})
                </button>
                <button class="pill-btn ${state.expediente.tab === 'vacunas' ? 'active' : ''}" data-exp-tab="vacunas">
                    💉 Vacunas (${exp.vacunas.length})
                </button>
                <button class="pill-btn ${state.expediente.tab === 'citas' ? 'active' : ''}" data-exp-tab="citas">
                    📅 Citas (${exp.citas.length})
                </button>
                <button class="pill-btn ${state.expediente.tab === 'datos' ? 'active' : ''}" data-exp-tab="datos">
                    📋 Notas y Contacto
                </button>
            </div>

            <div>
                ${renderExpedienteTabContent(exp)}
            </div>
        `;
    }

    function renderExpedienteTabContent(exp) {
        const tab = state.expediente.tab;
        if (tab === 'consultas') {
            if (exp.consultas.length === 0) {
                return `<div class="empty-state"><h4>Sin consultas registradas</h4><p>Usa "+ Registrar Consulta" para iniciar la historia clínica.</p></div>`;
            }
            return `<div class="agenda-list">${exp.consultas.map(cm => `
                <div class="timeline-card">
                    <div class="timeline-card-top">
                        <strong>${formatDateTime(cm.fechaConsulta)} · ${escapeHtml(cm.veterinario)}</strong>
                        <div style="display:flex;gap:6px;flex-wrap:wrap;">
                            ${cm.pesoKg ? `<span class="badge badge-neutral">${cm.pesoKg} kg</span>` : ''}
                            ${cm.temperaturaC ? `<span class="badge badge-info">${cm.temperaturaC} °C</span>` : ''}
                            ${cm.frecuenciaCardiaca ? `<span class="badge badge-teal">${cm.frecuenciaCardiaca} lpm</span>` : ''}
                        </div>
                    </div>
                    <div><strong>Motivo:</strong> ${escapeHtml(cm.motivoConsulta)}</div>
                    ${cm.sintomas ? `<div><strong>Síntomas:</strong> ${escapeHtml(cm.sintomas)}</div>` : ''}
                    <div><strong>Diagnóstico:</strong> ${escapeHtml(cm.diagnostico)}</div>
                    <div><strong>Tratamiento:</strong> ${escapeHtml(cm.tratamiento)}</div>
                    ${cm.proximaRevision ? `<div class="cell-sub">📅 Próxima revisión sugerida: ${formatDate(cm.proximaRevision)}</div>` : ''}
                </div>
            `).join('')}</div>`;
        }

        if (tab === 'vacunas') {
            if (exp.vacunas.length === 0) {
                return `<div class="empty-state"><h4>Sin vacunas ni desparasitaciones</h4><p>Agrega el primer registro preventivo.</p></div>`;
            }
            return `<div class="agenda-list">${exp.vacunas.map(v => {
                const meta = estadoVacunaMeta(v.estadoAlerta, v.diasParaVencimiento);
                return `
                    <div class="timeline-card">
                        <div class="timeline-card-top">
                            <strong>${escapeHtml(v.nombre)}</strong>
                            <span class="badge ${meta.cls}">${meta.label}</span>
                        </div>
                        <div class="cell-sub">
                            Aplicada: ${formatDate(v.fechaAplicacion)}
                            ${v.proximaDosis ? ` · Próxima dosis: ${formatDate(v.proximaDosis)}` : ''}
                            ${v.lote ? ` · Lote: ${escapeHtml(v.lote)}` : ''}
                        </div>
                        ${v.notas ? `<div>${escapeHtml(v.notas)}</div>` : ''}
                    </div>
                `;
            }).join('')}</div>`;
        }

        if (tab === 'citas') {
            if (exp.citas.length === 0) {
                return `<div class="empty-state"><h4>Sin citas registradas</h4></div>`;
            }
            return `<div class="agenda-list">${exp.citas.map(c => {
                const eMeta = estadoCitaMeta(c.estado);
                return `
                    <div class="timeline-card">
                        <div class="timeline-card-top">
                            <strong>${formatDateTime(c.fechaHora)} · ${escapeHtml(tipoCitaLabel(c.tipo))}</strong>
                            <span class="badge ${eMeta.cls}">${eMeta.label}</span>
                        </div>
                        <div>${escapeHtml(c.motivo)}</div>
                        <div class="cell-sub">Veterinario: ${escapeHtml(c.veterinario)}</div>
                    </div>
                `;
            }).join('')}</div>`;
        }

        return `
            <div class="timeline-card">
                <div><strong>Color / Señas:</strong> ${escapeHtml(exp.mascota.color || 'No especificado')}</div>
                <div><strong>Notas médicas generales:</strong> ${escapeHtml(exp.mascota.notasMedicas || 'Sin observaciones adicionales.')}</div>
                <hr style="border:none;border-top:1px solid var(--border);margin:6px 0;">
                <div><strong>Dirección del Propietario:</strong> ${escapeHtml(exp.propietario.direccion || 'No registrada')}</div>
                <div><strong>Documento:</strong> ${escapeHtml(exp.propietario.documento || 'No registrado')}</div>
                <div><strong>Notas del Propietario:</strong> ${escapeHtml(exp.propietario.notas || 'Sin notas.')}</div>
            </div>
        `;
    }

    // ---------- Modals (Create / Edit Forms) ----------
    function openFormModal(title, html, onSubmit) {
        $formTitle.textContent = title;
        $formBody.innerHTML = `<div id="modal-form-alert" class="form-alert hidden"></div>${html}`;
        $formBackdrop.classList.remove('hidden');

        const form = $formBody.querySelector('form');
        if (!form) return;

        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            clearFormErrors(form);
            const submitBtn = form.querySelector('button[type="submit"]');
            if (submitBtn) submitBtn.disabled = true;

            try {
                await onSubmit(form);
                closeFormModal();
                await loadAndRenderView();
            } catch (err) {
                showFormError(form, err);
            } finally {
                if (submitBtn) submitBtn.disabled = false;
            }
        });
    }

    function closeFormModal() {
        $formBackdrop.classList.add('hidden');
        $formBody.innerHTML = '';
    }

    function clearFormErrors(form) {
        const alertEl = document.getElementById('modal-form-alert');
        if (alertEl) {
            alertEl.textContent = '';
            alertEl.classList.add('hidden');
        }
        form.querySelectorAll('.form-group.has-error').forEach(g => g.classList.remove('has-error'));
        form.querySelectorAll('.field-error').forEach(el => el.remove());
    }

    function showFormError(form, err) {
        const alertEl = document.getElementById('modal-form-alert');
        if (alertEl) {
            alertEl.textContent = err.message || 'Error al guardar los datos.';
            alertEl.classList.remove('hidden');
        }
        if (err.fieldErrors) {
            Object.entries(err.fieldErrors).forEach(([field, msg]) => {
                const input = form.querySelector(`[name="${field}"]`);
                if (input) {
                    const group = input.closest('.form-group');
                    if (group) {
                        group.classList.add('has-error');
                        const errSpan = document.createElement('span');
                        errSpan.className = 'field-error';
                        errSpan.textContent = msg;
                        group.appendChild(errSpan);
                    }
                }
            });
        }
    }

    function openConfirmModal(title, message, onConfirm) {
        $confirmTitle.textContent = title;
        $confirmMsg.textContent = message;
        confirmCallback = onConfirm;
        $confirmBackdrop.classList.remove('hidden');
    }

    function closeConfirmModal() {
        $confirmBackdrop.classList.add('hidden');
        confirmCallback = null;
    }

    // Form Builders
    function openPropietarioModal(existing = null) {
        const isEdit = Boolean(existing);
        const p = existing || {};
        openFormModal(isEdit ? 'Editar Propietario' : 'Nuevo Propietario', `
            <form class="form-grid">
                <div class="form-group full">
                    <label>Nombre completo *</label>
                    <input name="nombre" required value="${escapeHtml(p.nombre || '')}" placeholder="Ej. Camila Mendoza">
                </div>
                <div class="form-group">
                    <label>Teléfono / WhatsApp *</label>
                    <input name="telefono" required value="${escapeHtml(p.telefono || '')}" placeholder="Ej. +57 310 458 9210">
                </div>
                <div class="form-group">
                    <label>Documento de identidad</label>
                    <input name="documento" value="${escapeHtml(p.documento || '')}" placeholder="Cédula / DNI / Pasaporte">
                </div>
                <div class="form-group">
                    <label>Correo electrónico</label>
                    <input name="email" type="email" value="${escapeHtml(p.email || '')}" placeholder="cliente@correo.com">
                </div>
                <div class="form-group">
                    <label>Dirección</label>
                    <input name="direccion" value="${escapeHtml(p.direccion || '')}" placeholder="Calle, número, barrio">
                </div>
                <div class="form-group full">
                    <label>Notas de atención</label>
                    <textarea name="notas" rows="2" placeholder="Preferencias de contacto u observaciones">${escapeHtml(p.notas || '')}</textarea>
                </div>
                <div class="modal-actions full" style="grid-column:1/-1;">
                    <button type="button" class="btn btn-outline" data-close-form="1">Cancelar</button>
                    <button type="submit" class="btn btn-primary">${isEdit ? 'Guardar cambios' : 'Registrar propietario'}</button>
                </div>
            </form>
        `, async (form) => {
            const fd = new FormData(form);
            const payload = Object.fromEntries(fd.entries());
            if (isEdit) {
                await VetApi.updatePropietario(p.id, payload);
                showToast('Propietario actualizado correctamente.');
            } else {
                await VetApi.createPropietario(payload);
                showToast('Propietario registrado correctamente.');
            }
        });
    }

    async function ensurePropietariosLoaded() {
        if (state.propietarios.length === 0) {
            state.propietarios = await VetApi.getPropietarios();
        }
    }

    async function ensureMascotasLoaded() {
        if (state.mascotas.length === 0) {
            state.mascotas = await VetApi.getMascotas();
        }
    }

    async function openMascotaModal(existing = null, preselectedOwnerId = null) {
        await ensurePropietariosLoaded();
        if (state.propietarios.length === 0) {
            showToast('Primero registra al menos un propietario para asociar la mascota.', 'warning');
            openPropietarioModal();
            return;
        }

        const isEdit = Boolean(existing);
        const m = existing || {};
        const selectedOwner = m.propietarioId || preselectedOwnerId || state.propietarios[0].id;
        const especies = ['Perro', 'Gato', 'Ave', 'Conejo', 'Roedor', 'Reptil', 'Otro'];
        if (m.especie && !especies.includes(m.especie)) {
            especies.unshift(m.especie);
        }

        openFormModal(isEdit ? 'Editar Paciente' : 'Nuevo Paciente (Mascota)', `
            <form class="form-grid">
                <div class="form-group">
                    <label>Nombre de la mascota *</label>
                    <input name="nombre" required value="${escapeHtml(m.nombre || '')}" placeholder="Ej. Max">
                </div>
                <div class="form-group">
                    <label>Propietario *</label>
                    <select name="propietarioId" required>
                        ${state.propietarios.map(p => `
                            <option value="${p.id}" ${Number(selectedOwner) === p.id ? 'selected' : ''}>
                                ${escapeHtml(p.nombre)} (${escapeHtml(p.telefono)})
                            </option>
                        `).join('')}
                    </select>
                </div>
                <div class="form-group">
                    <label>Especie *</label>
                    <select name="especie" required>
                        ${especies.map(sp => `<option value="${sp}" ${(m.especie || 'Perro') === sp ? 'selected' : ''}>${sp}</option>`).join('')}
                    </select>
                </div>
                <div class="form-group">
                    <label>Raza</label>
                    <input name="raza" value="${escapeHtml(m.raza || '')}" placeholder="Ej. Golden Retriever, Mestizo...">
                </div>
                <div class="form-group">
                    <label>Sexo</label>
                    <select name="sexo">
                        <option value="">No especificado</option>
                        <option value="Macho" ${m.sexo === 'Macho' ? 'selected' : ''}>Macho</option>
                        <option value="Hembra" ${m.sexo === 'Hembra' ? 'selected' : ''}>Hembra</option>
                    </select>
                </div>
                <div class="form-group">
                    <label>Edad (años)</label>
                    <input name="edadAnios" type="number" min="0" max="60" value="${m.edadAnios ?? ''}" placeholder="Ej. 3">
                </div>
                <div class="form-group">
                    <label>Peso (kg)</label>
                    <input name="pesoKg" type="number" step="0.01" min="0" value="${m.pesoKg ?? ''}" placeholder="Ej. 12.5">
                </div>
                <div class="form-group">
                    <label>Color / Señas</label>
                    <input name="color" value="${escapeHtml(m.color || '')}" placeholder="Ej. Dorado y blanco">
                </div>
                <div class="form-group">
                    <label>Microchip</label>
                    <input name="microchip" value="${escapeHtml(m.microchip || '')}" placeholder="Opcional">
                </div>
                <div class="form-group">
                    <label>Estado reproductivo</label>
                    <label class="checkbox-row">
                        <input name="esterilizado" type="checkbox" ${m.esterilizado ? 'checked' : ''}>
                        <span>Paciente esterilizado / castrado</span>
                    </label>
                </div>
                <div class="form-group full">
                    <label>Alergias conocidas</label>
                    <input name="alergias" value="${escapeHtml(m.alergias || '')}" placeholder="Ej. Penicilina, proteína de pollo...">
                </div>
                <div class="form-group full">
                    <label>Notas médicas o comportamiento</label>
                    <textarea name="notasMedicas" rows="2" placeholder="Observaciones relevantes para el equipo médico">${escapeHtml(m.notasMedicas || '')}</textarea>
                </div>
                <div class="modal-actions full" style="grid-column:1/-1;">
                    <button type="button" class="btn btn-outline" data-close-form="1">Cancelar</button>
                    <button type="submit" class="btn btn-primary">${isEdit ? 'Guardar cambios' : 'Registrar paciente'}</button>
                </div>
            </form>
        `, async (form) => {
            const fd = new FormData(form);
            const payload = {
                nombre: fd.get('nombre'),
                propietarioId: Number(fd.get('propietarioId')),
                especie: fd.get('especie'),
                raza: fd.get('raza'),
                sexo: fd.get('sexo'),
                edadAnios: fd.get('edadAnios') !== '' ? Number(fd.get('edadAnios')) : null,
                pesoKg: fd.get('pesoKg') !== '' ? Number(fd.get('pesoKg')) : null,
                color: fd.get('color'),
                microchip: fd.get('microchip'),
                esterilizado: form.querySelector('[name="esterilizado"]').checked,
                alergias: fd.get('alergias'),
                notasMedicas: fd.get('notasMedicas')
            };
            if (isEdit) {
                await VetApi.updateMascota(m.id, payload);
                showToast('Ficha del paciente actualizada.');
            } else {
                await VetApi.createMascota(payload);
                showToast('Paciente registrado correctamente.');
            }
        });
    }

    async function openCitaModal(existing = null, preselectedMascotaId = null) {
        await ensureMascotasLoaded();
        if (state.mascotas.length === 0) {
            showToast('Primero registra al menos una mascota.', 'warning');
            return;
        }
        const isEdit = Boolean(existing);
        const c = existing || {};
        const selectedPet = c.mascotaId || preselectedMascotaId || state.mascotas[0].id;
        const defaultDateTime = c.fechaHora ? String(c.fechaHora).slice(0, 16) : toLocalDateTimeInput(new Date(Date.now() + 3600000));

        openFormModal(isEdit ? 'Editar / Reprogramar Cita' : 'Agendar Nueva Cita', `
            <form class="form-grid">
                <div class="form-group">
                    <label>Paciente *</label>
                    <select name="mascotaId" required>
                        ${state.mascotas.map(m => `
                            <option value="${m.id}" ${Number(selectedPet) === m.id ? 'selected' : ''}>
                                ${escapeHtml(m.nombre)} (${escapeHtml(m.especie)} · ${escapeHtml(m.nombrePropietario)})
                            </option>
                        `).join('')}
                    </select>
                </div>
                <div class="form-group">
                    <label>Fecha y Hora *</label>
                    <input name="fechaHora" type="datetime-local" required value="${defaultDateTime}">
                </div>
                <div class="form-group">
                    <label>Tipo de Cita *</label>
                    <select name="tipo" required>
                        <option value="CONSULTA_GENERAL" ${c.tipo === 'CONSULTA_GENERAL' ? 'selected' : ''}>Consulta General</option>
                        <option value="VACUNACION" ${c.tipo === 'VACUNACION' ? 'selected' : ''}>Vacunación</option>
                        <option value="URGENCIA" ${c.tipo === 'URGENCIA' ? 'selected' : ''}>Urgencia</option>
                        <option value="CONTROL" ${c.tipo === 'CONTROL' ? 'selected' : ''}>Control</option>
                        <option value="CIRUGIA" ${c.tipo === 'CIRUGIA' ? 'selected' : ''}>Cirugía</option>
                        <option value="DESPARASITACION" ${c.tipo === 'DESPARASITACION' ? 'selected' : ''}>Desparasitación</option>
                        <option value="ESTETICA" ${c.tipo === 'ESTETICA' ? 'selected' : ''}>Estética / Baño</option>
                    </select>
                </div>
                <div class="form-group">
                    <label>Prioridad</label>
                    <select name="prioridad">
                        <option value="NORMAL" ${(c.prioridad || 'NORMAL') === 'NORMAL' ? 'selected' : ''}>Normal</option>
                        <option value="ALTA" ${c.prioridad === 'ALTA' ? 'selected' : ''}>Alta</option>
                        <option value="URGENTE" ${c.prioridad === 'URGENTE' ? 'selected' : ''}>Urgente</option>
                    </select>
                </div>
                <div class="form-group full">
                    <label>Veterinario Asignado *</label>
                    <input name="veterinario" required value="${escapeHtml(c.veterinario || 'Dra. Laura Restrepo')}" list="vets-datalist">
                    <datalist id="vets-datalist">
                        <option value="Dra. Laura Restrepo">
                        <option value="Dr. Carlos Ramírez">
                    </datalist>
                </div>
                <div class="form-group full">
                    <label>Motivo de la cita *</label>
                    <input name="motivo" required value="${escapeHtml(c.motivo || '')}" placeholder="Ej. Vacuna anual, decaimiento, revisión de herida...">
                </div>
                <div class="form-group full">
                    <label>Notas adicionales</label>
                    <textarea name="notas" rows="2">${escapeHtml(c.notas || '')}</textarea>
                </div>
                <div class="modal-actions full" style="grid-column:1/-1;">
                    <button type="button" class="btn btn-outline" data-close-form="1">Cancelar</button>
                    <button type="submit" class="btn btn-primary">${isEdit ? 'Guardar cita' : 'Confirmar cita'}</button>
                </div>
            </form>
        `, async (form) => {
            const fd = new FormData(form);
            const payload = {
                mascotaId: Number(fd.get('mascotaId')),
                fechaHora: fd.get('fechaHora'),
                tipo: fd.get('tipo'),
                prioridad: fd.get('prioridad'),
                veterinario: fd.get('veterinario'),
                motivo: fd.get('motivo'),
                notas: fd.get('notas')
            };
            if (isEdit) {
                await VetApi.updateCita(c.id, payload);
                showToast('Cita reprogramada correctamente.');
            } else {
                await VetApi.createCita(payload);
                showToast('Cita programada sin conflictos.');
            }
        });
    }

    async function openConsultaModal(existing = null, prefill = {}) {
        await ensureMascotasLoaded();
        if (state.mascotas.length === 0) {
            showToast('Primero registra al menos una mascota.', 'warning');
            return;
        }
        const isEdit = Boolean(existing);
        const cm = existing || {};
        const selectedPetId = cm.mascotaId || prefill.mascotaId || state.mascotas[0].id;
        const selectedPet = state.mascotas.find(m => m.id === Number(selectedPetId));
        const fechaDef = cm.fechaConsulta ? String(cm.fechaConsulta).slice(0, 16) : toLocalDateTimeInput();

        openFormModal(isEdit ? 'Editar Consulta Médica' : 'Registrar Consulta Médica', `
            <form class="form-grid">
                <input type="hidden" name="citaId" value="${cm.citaId || prefill.citaId || ''}">
                <div class="form-group">
                    <label>Paciente *</label>
                    <select name="mascotaId" required>
                        ${state.mascotas.map(m => `
                            <option value="${m.id}" ${Number(selectedPetId) === m.id ? 'selected' : ''}>
                                ${escapeHtml(m.nombre)} (${escapeHtml(m.especie)} · ${escapeHtml(m.nombrePropietario)})
                            </option>
                        `).join('')}
                    </select>
                </div>
                <div class="form-group">
                    <label>Fecha y Hora</label>
                    <input name="fechaConsulta" type="datetime-local" value="${fechaDef}">
                </div>
                <div class="form-group">
                    <label>Veterinario *</label>
                    <input name="veterinario" required value="${escapeHtml(cm.veterinario || prefill.veterinario || 'Dra. Laura Restrepo')}">
                </div>
                <div class="form-group">
                    <label>Motivo de Consulta *</label>
                    <input name="motivoConsulta" required value="${escapeHtml(cm.motivoConsulta || prefill.motivoConsulta || '')}" placeholder="Motivo de ingreso">
                </div>
                <div class="form-group full">
                    <label>Síntomas y hallazgos</label>
                    <textarea name="sintomas" rows="2" placeholder="Signos observados durante el examen físico">${escapeHtml(cm.sintomas || '')}</textarea>
                </div>
                <div class="form-group full">
                    <label>Diagnóstico *</label>
                    <textarea name="diagnostico" rows="2" required placeholder="Diagnóstico clínico presuntivo o definitivo">${escapeHtml(cm.diagnostico || '')}</textarea>
                </div>
                <div class="form-group full">
                    <label>Tratamiento / Prescripción *</label>
                    <textarea name="tratamiento" rows="2" required placeholder="Medicamentos, dosis, duración e indicaciones">${escapeHtml(cm.tratamiento || '')}</textarea>
                </div>
                <div class="form-group">
                    <label>Peso actual (kg)</label>
                    <input name="pesoKg" type="number" step="0.01" min="0" value="${cm.pesoKg ?? (selectedPet && selectedPet.pesoKg ? selectedPet.pesoKg : '')}" placeholder="Actualiza el peso de la ficha">
                </div>
                <div class="form-group">
                    <label>Temperatura (°C)</label>
                    <input name="temperaturaC" type="number" step="0.1" min="25" max="45" value="${cm.temperaturaC ?? ''}" placeholder="Ej. 38.5">
                </div>
                <div class="form-group">
                    <label>Frecuencia Cardíaca (lpm)</label>
                    <input name="frecuenciaCardiaca" type="number" min="10" max="400" value="${cm.frecuenciaCardiaca ?? ''}" placeholder="Ej. 100">
                </div>
                <div class="form-group">
                    <label>Próxima revisión sugerida</label>
                    <input name="proximaRevision" type="date" value="${cm.proximaRevision || ''}">
                </div>
                <div class="form-group full">
                    <label>Observaciones adicionales</label>
                    <textarea name="observaciones" rows="2">${escapeHtml(cm.observaciones || '')}</textarea>
                </div>
                <div class="modal-actions full" style="grid-column:1/-1;">
                    <button type="button" class="btn btn-outline" data-close-form="1">Cancelar</button>
                    <button type="submit" class="btn btn-primary">${isEdit ? 'Actualizar consulta' : 'Guardar consulta'}</button>
                </div>
            </form>
        `, async (form) => {
            const fd = new FormData(form);
            const citaIdRaw = fd.get('citaId');
            const payload = {
                mascotaId: Number(fd.get('mascotaId')),
                citaId: citaIdRaw ? Number(citaIdRaw) : null,
                fechaConsulta: fd.get('fechaConsulta') || null,
                veterinario: fd.get('veterinario'),
                motivoConsulta: fd.get('motivoConsulta'),
                sintomas: fd.get('sintomas'),
                diagnostico: fd.get('diagnostico'),
                tratamiento: fd.get('tratamiento'),
                pesoKg: fd.get('pesoKg') !== '' ? Number(fd.get('pesoKg')) : null,
                temperaturaC: fd.get('temperaturaC') !== '' ? Number(fd.get('temperaturaC')) : null,
                frecuenciaCardiaca: fd.get('frecuenciaCardiaca') !== '' ? Number(fd.get('frecuenciaCardiaca')) : null,
                proximaRevision: fd.get('proximaRevision') || null,
                observaciones: fd.get('observaciones')
            };
            if (isEdit) {
                await VetApi.updateConsulta(cm.id, payload);
                showToast('Consulta médica actualizada.');
            } else {
                await VetApi.createConsulta(payload);
                showToast(payload.citaId ? 'Consulta guardada y cita marcada como completada.' : 'Consulta médica registrada.');
            }
        });
    }

    async function openVacunaModal(existing = null, prefill = {}) {
        await ensureMascotasLoaded();
        if (state.mascotas.length === 0) {
            showToast('Primero registra al menos una mascota.', 'warning');
            return;
        }
        const isEdit = Boolean(existing);
        const v = existing || {};
        const selectedPet = v.mascotaId || prefill.mascotaId || state.mascotas[0].id;
        const todayStr = toLocalDateInput();
        const tipoDefault = v.tipo || prefill.tipo || 'VACUNA';
        const fechaApp = v.fechaAplicacion || prefill.fechaAplicacion || todayStr;
        const proximaDef = v.proximaDosis !== undefined
            ? (v.proximaDosis || '')
            : (prefill.proximaDosis || addDaysToDateInput(fechaApp, tipoDefault === 'DESPARASITACION' ? 90 : 365));

        openFormModal(isEdit ? 'Editar Vacuna / Desparasitación' : (prefill.isRefuerzo ? 'Registrar Refuerzo' : 'Registrar Vacuna o Desparasitación'), `
            <form class="form-grid">
                <div class="form-group">
                    <label>Paciente *</label>
                    <select name="mascotaId" required>
                        ${state.mascotas.map(m => `
                            <option value="${m.id}" ${Number(selectedPet) === m.id ? 'selected' : ''}>
                                ${escapeHtml(m.nombre)} (${escapeHtml(m.especie)} · ${escapeHtml(m.nombrePropietario)})
                            </option>
                        `).join('')}
                    </select>
                </div>
                <div class="form-group">
                    <label>Tipo *</label>
                    <select name="tipo" required>
                        <option value="VACUNA" ${tipoDefault === 'VACUNA' ? 'selected' : ''}>Vacuna</option>
                        <option value="DESPARASITACION" ${tipoDefault === 'DESPARASITACION' ? 'selected' : ''}>Desparasitación</option>
                    </select>
                </div>
                <div class="form-group full">
                    <label>Nombre del biológico / antiparasitario *</label>
                    <input name="nombre" required value="${escapeHtml(v.nombre || prefill.nombre || '')}" placeholder="Ej. Antirrábica, Séxtuple Canina, Triple Felina...">
                </div>
                <div class="form-group">
                    <label>Fecha de aplicación *</label>
                    <input name="fechaAplicacion" type="date" required value="${fechaApp}">
                </div>
                <div class="form-group">
                    <label>Próxima dosis (para alertas)</label>
                    <input name="proximaDosis" type="date" value="${proximaDef}">
                </div>
                <div class="form-group">
                    <label>Número de lote</label>
                    <input name="lote" value="${escapeHtml(v.lote || '')}" placeholder="Ej. RAB-2026-01">
                </div>
                <div class="form-group">
                    <label>Veterinario</label>
                    <input name="veterinario" value="${escapeHtml(v.veterinario || prefill.veterinario || 'Dr. Carlos Ramírez')}">
                </div>
                <div class="form-group full">
                    <label>Notas</label>
                    <textarea name="notas" rows="2">${escapeHtml(v.notas || '')}</textarea>
                </div>
                <div class="modal-actions full" style="grid-column:1/-1;">
                    <button type="button" class="btn btn-outline" data-close-form="1">Cancelar</button>
                    <button type="submit" class="btn btn-primary">${isEdit ? 'Guardar cambios' : 'Guardar aplicación'}</button>
                </div>
            </form>
        `, async (form) => {
            const fd = new FormData(form);
            const payload = {
                mascotaId: Number(fd.get('mascotaId')),
                tipo: fd.get('tipo'),
                nombre: fd.get('nombre'),
                fechaAplicacion: fd.get('fechaAplicacion'),
                proximaDosis: fd.get('proximaDosis') || null,
                lote: fd.get('lote'),
                veterinario: fd.get('veterinario'),
                notas: fd.get('notas')
            };
            if (isEdit) {
                await VetApi.updateVacuna(v.id, payload);
                showToast('Registro de inmunización actualizado.');
            } else {
                await VetApi.createVacuna(payload);
                showToast('Aplicación registrada y próxima alerta calculada.');
            }
        });
    }

    // ---------- Global Search ----------
    function renderGlobalSearch(query) {
        const q = (query || '').trim().toLowerCase();
        if (!q) {
            $globalResults.classList.add('hidden');
            $globalResults.innerHTML = '';
            return;
        }

        const matchedPets = state.mascotas.filter(m =>
            (m.nombre || '').toLowerCase().includes(q)
            || (m.raza || '').toLowerCase().includes(q)
            || (m.nombrePropietario || '').toLowerCase().includes(q)
        ).slice(0, 5);

        const matchedOwners = state.propietarios.filter(p =>
            (p.nombre || '').toLowerCase().includes(q)
            || (p.telefono || '').toLowerCase().includes(q)
            || (p.email || '').toLowerCase().includes(q)
        ).slice(0, 4);

        if (matchedPets.length === 0 && matchedOwners.length === 0) {
            $globalResults.innerHTML = `<div class="search-result-item"><span>Sin coincidencias para "${escapeHtml(query)}"</span></div>`;
            $globalResults.classList.remove('hidden');
            return;
        }

        let html = '';
        if (matchedPets.length > 0) {
            html += `<div class="search-result-group">Pacientes</div>`;
            html += matchedPets.map(m => `
                <button class="search-result-item" data-search-pet="${m.id}">
                    <span>${speciesMeta(m.especie).icon} <strong>${escapeHtml(m.nombre)}</strong> (${escapeHtml(m.especie)})</span>
                    <span class="search-result-meta">Dueño: ${escapeHtml(m.nombrePropietario)}</span>
                </button>
            `).join('');
        }
        if (matchedOwners.length > 0) {
            html += `<div class="search-result-group">Propietarios</div>`;
            html += matchedOwners.map(p => `
                <button class="search-result-item" data-search-owner="${p.id}">
                    <span>👤 <strong>${escapeHtml(p.nombre)}</strong></span>
                    <span class="search-result-meta">📞 ${escapeHtml(p.telefono)} · ${p.cantidadMascotas} mascota(s)</span>
                </button>
            `).join('');
        }

        $globalResults.innerHTML = html;
        $globalResults.classList.remove('hidden');
    }

    // ---------- Event Wiring ----------
    function setupEventListeners() {
        document.querySelectorAll('.nav-item').forEach(btn => {
            btn.addEventListener('click', () => navigateTo(btn.dataset.view));
        });

        document.getElementById('open-sidebar-btn').addEventListener('click', () => toggleSidebar(true));
        document.getElementById('close-sidebar-btn').addEventListener('click', () => toggleSidebar(false));
        $mobileBackdrop.addEventListener('click', () => toggleSidebar(false));

        document.getElementById('theme-toggle-btn').addEventListener('click', () => {
            const current = document.documentElement.getAttribute('data-theme');
            applyTheme(current === 'dark' ? 'light' : 'dark');
        });

        document.getElementById('refresh-btn').addEventListener('click', async () => {
            await loadAndRenderView();
            showToast('Datos actualizados.');
        });

        document.getElementById('quick-new-cita-btn').addEventListener('click', () => openCitaModal());
        document.getElementById('quick-new-mascota-btn').addEventListener('click', () => openMascotaModal());

        document.getElementById('close-drawer-btn').addEventListener('click', closeExpediente);
        $drawerBackdrop.addEventListener('click', closeExpediente);
        document.getElementById('print-expediente-btn').addEventListener('click', () => window.print());

        document.getElementById('close-form-modal-btn').addEventListener('click', closeFormModal);
        $formBackdrop.addEventListener('click', (e) => {
            if (e.target === $formBackdrop || e.target.closest('[data-close-form]')) {
                closeFormModal();
            }
        });

        document.getElementById('close-confirm-modal-btn').addEventListener('click', closeConfirmModal);
        document.getElementById('confirm-cancel-btn').addEventListener('click', closeConfirmModal);
        $confirmBackdrop.addEventListener('click', (e) => {
            if (e.target === $confirmBackdrop) closeConfirmModal();
        });
        $confirmAccept.addEventListener('click', async () => {
            if (!confirmCallback) return;
            $confirmAccept.disabled = true;
            try {
                await confirmCallback();
                closeConfirmModal();
                await loadAndRenderView();
            } catch (err) {
                showToast(err.message, 'error');
            } finally {
                $confirmAccept.disabled = false;
            }
        });

        // Global search & keyboard shortcuts
        $globalSearch.addEventListener('input', (e) => renderGlobalSearch(e.target.value));
        $globalSearch.addEventListener('focus', (e) => {
            if (e.target.value.trim()) renderGlobalSearch(e.target.value);
        });
        document.addEventListener('click', (e) => {
            if (!e.target.closest('.global-search-wrap')) {
                $globalResults.classList.add('hidden');
            }
        });

        $globalResults.addEventListener('click', (e) => {
            const petBtn = e.target.closest('[data-search-pet]');
            const ownerBtn = e.target.closest('[data-search-owner]');
            if (petBtn) {
                $globalResults.classList.add('hidden');
                $globalSearch.value = '';
                openExpediente(petBtn.dataset.searchPet);
            } else if (ownerBtn) {
                $globalResults.classList.add('hidden');
                $globalSearch.value = '';
                state.filters.mascotaPropietarioId = Number(ownerBtn.dataset.searchOwner);
                navigateTo('mascotas');
            }
        });

        document.addEventListener('keydown', (e) => {
            if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
                e.preventDefault();
                $globalSearch.focus();
                $globalSearch.select();
            } else if (e.key === '/' && document.activeElement.tagName !== 'INPUT' && document.activeElement.tagName !== 'TEXTAREA') {
                e.preventDefault();
                $globalSearch.focus();
            } else if (e.key === 'Escape') {
                $globalResults.classList.add('hidden');
                if (!$confirmBackdrop.classList.contains('hidden')) closeConfirmModal();
                else if (!$formBackdrop.classList.contains('hidden')) closeFormModal();
                else if (!$drawer.classList.contains('hidden')) closeExpediente();
            }
        });

        // Delegated action clicks for view & drawer
        document.addEventListener('click', handleDelegatedClicks);
    }

    async function handleDelegatedClicks(e) {
        const target = e.target.closest('button, .kpi-card');
        if (!target) return;

        // Navigation shortcuts
        if (target.dataset.nav) {
            if (target.dataset.citaScope) state.filters.citaScope = target.dataset.citaScope;
            if (target.dataset.vacunaEstado) state.filters.vacunaEstado = target.dataset.vacunaEstado;
            await navigateTo(target.dataset.nav);
            return;
        }

        // Quick Create Actions
        if (target.dataset.action === 'new-propietario') return openPropietarioModal();
        if (target.dataset.action === 'new-mascota') return openMascotaModal();
        if (target.dataset.action === 'new-cita') return openCitaModal();
        if (target.dataset.action === 'new-consulta') return openConsultaModal();
        if (target.dataset.action === 'new-vacuna') return openVacunaModal();

        // Expediente
        if (target.dataset.openExpediente) return openExpediente(target.dataset.openExpediente);
        if (target.dataset.expTab) {
            state.expediente.tab = target.dataset.expTab;
            if (state.expediente.data) renderExpediente(state.expediente.data);
            return;
        }

        // Propietario row actions
        if (target.dataset.verMascotasProp) {
            state.filters.mascotaPropietarioId = Number(target.dataset.verMascotasProp);
            return navigateTo('mascotas');
        }
        if (target.dataset.newMascotaProp) return openMascotaModal(null, Number(target.dataset.newMascotaProp));
        if (target.dataset.editPropietario) {
            const p = state.propietarios.find(item => item.id === Number(target.dataset.editPropietario));
            if (p) openPropietarioModal(p);
            return;
        }
        if (target.dataset.deletePropietario) {
            const p = state.propietarios.find(item => item.id === Number(target.dataset.deletePropietario));
            if (!p) return;
            openConfirmModal(
                'Eliminar propietario',
                `¿Seguro que deseas eliminar a "${p.nombre}"? Se eliminarán también sus ${p.cantidadMascotas} mascota(s) e historiales médicos.`,
                async () => {
                    await VetApi.deletePropietario(p.id);
                    showToast('Propietario eliminado.');
                }
            );
            return;
        }

        // Mascota filters & row actions
        if (target.dataset.filterEspecie !== undefined) {
            state.filters.mascotaEspecie = target.dataset.filterEspecie;
            return loadAndRenderView();
        }
        if (target.dataset.clearOwnerFilter) {
            state.filters.mascotaPropietarioId = null;
            return loadAndRenderView();
        }
        if (target.dataset.editMascota) {
            const id = Number(target.dataset.editMascota);
            const m = state.mascotas.find(item => item.id === id)
                || (state.expediente.data && state.expediente.data.mascota.id === id ? state.expediente.data.mascota : null);
            if (m) openMascotaModal(m);
            return;
        }
        if (target.dataset.deleteMascota) {
            const m = state.mascotas.find(item => item.id === Number(target.dataset.deleteMascota));
            if (!m) return;
            openConfirmModal(
                'Eliminar paciente',
                `¿Eliminar a "${m.nombre}" junto con su historial clínico, citas y vacunas?`,
                async () => {
                    await VetApi.deleteMascota(m.id);
                    if (state.expediente.mascotaId === m.id) closeExpediente();
                    showToast('Paciente eliminado.');
                }
            );
            return;
        }
        if (target.dataset.citaMascota) return openCitaModal(null, Number(target.dataset.citaMascota));
        if (target.dataset.consultaMascota) return openConsultaModal(null, { mascotaId: Number(target.dataset.consultaMascota) });
        if (target.dataset.vacunaMascota) return openVacunaModal(null, { mascotaId: Number(target.dataset.vacunaMascota) });

        // Citas filters & actions
        if (target.dataset.citaScopeBtn) {
            state.filters.citaScope = target.dataset.citaScopeBtn;
            return loadAndRenderCitas();
        }
        if (target.dataset.citaEstado) {
            try {
                await VetApi.cambiarEstadoCita(Number(target.dataset.citaEstado), target.dataset.nuevoEstado);
                showToast('Estado de la cita actualizado.');
                await loadAndRenderView();
            } catch (err) {
                showToast(err.message, 'error');
            }
            return;
        }
        if (target.dataset.atenderCita) {
            const citaId = Number(target.dataset.atenderCita);
            const cita = state.citas.find(c => c.id === citaId) || (state.dashboard && state.dashboard.citasHoyLista.find(c => c.id === citaId));
            if (cita) {
                openConsultaModal(null, {
                    mascotaId: cita.mascotaId,
                    citaId: cita.id,
                    veterinario: cita.veterinario,
                    motivoConsulta: cita.motivo
                });
            }
            return;
        }
        if (target.dataset.editCita) {
            const c = state.citas.find(item => item.id === Number(target.dataset.editCita));
            if (c) openCitaModal(c);
            return;
        }
        if (target.dataset.deleteCita) {
            const cId = Number(target.dataset.deleteCita);
            openConfirmModal(
                'Eliminar cita',
                '¿Deseas eliminar esta cita de la agenda?',
                async () => {
                    await VetApi.deleteCita(cId);
                    showToast('Cita eliminada.');
                }
            );
            return;
        }

        // Consultas actions
        if (target.dataset.editConsulta) {
            const cm = state.consultas.find(item => item.id === Number(target.dataset.editConsulta));
            if (cm) openConsultaModal(cm);
            return;
        }
        if (target.dataset.deleteConsulta) {
            const cmId = Number(target.dataset.deleteConsulta);
            openConfirmModal(
                'Eliminar consulta médica',
                '¿Seguro que deseas eliminar este registro del historial clínico?',
                async () => {
                    await VetApi.deleteConsulta(cmId);
                    showToast('Consulta médica eliminada.');
                }
            );
            return;
        }

        // Vacunas filters & actions
        if (target.dataset.vacunaEstadoBtn) {
            state.filters.vacunaEstado = target.dataset.vacunaEstadoBtn;
            return loadAndRenderVacunas();
        }
        if (target.dataset.refuerzoVacuna) {
            const vId = Number(target.dataset.refuerzoVacuna);
            const v = state.vacunas.find(item => item.id === vId) || (state.dashboard && state.dashboard.alertasVacunas.find(item => item.id === vId));
            if (v) {
                openVacunaModal(null, {
                    mascotaId: v.mascotaId,
                    nombre: v.nombre,
                    tipo: v.tipo,
                    veterinario: v.veterinario,
                    isRefuerzo: true
                });
            }
            return;
        }
        if (target.dataset.editVacuna) {
            const v = state.vacunas.find(item => item.id === Number(target.dataset.editVacuna));
            if (v) openVacunaModal(v);
            return;
        }
        if (target.dataset.deleteVacuna) {
            const vId = Number(target.dataset.deleteVacuna);
            openConfirmModal(
                'Eliminar registro de vacuna',
                '¿Deseas eliminar este registro de inmunización?',
                async () => {
                    await VetApi.deleteVacuna(vId);
                    showToast('Registro eliminado.');
                }
            );
        }
    }

    // Boot
    if (VetApi.baseUrl) {
        document.querySelectorAll('.sidebar-links a[href^="/"]').forEach(link => {
            link.href = `${VetApi.baseUrl}${link.getAttribute('href')}`;
        });
    }
    initTheme();
    setupEventListeners();
    loadAndRenderView();
})();
