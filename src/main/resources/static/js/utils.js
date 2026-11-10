const VetUtils = (() => {
    function escapeHtml(value) {
        if (value === null || value === undefined) return '';
        return String(value)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    function formatDate(dateStr) {
        if (!dateStr) return '—';
        const parts = String(dateStr).split('T')[0].split('-');
        if (parts.length !== 3) return escapeHtml(dateStr);
        return `${parts[2]}/${parts[1]}/${parts[0]}`;
    }

    function formatDateTime(dateTimeStr) {
        if (!dateTimeStr) return '—';
        const d = new Date(dateTimeStr);
        if (Number.isNaN(d.getTime())) return escapeHtml(dateTimeStr);
        return d.toLocaleString('es-CO', {
            day: '2-digit',
            month: 'short',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
            hour12: false
        });
    }

    function formatTime(dateTimeStr) {
        if (!dateTimeStr) return '—';
        const d = new Date(dateTimeStr);
        if (Number.isNaN(d.getTime())) return '—';
        return d.toLocaleTimeString('es-CO', {
            hour: '2-digit',
            minute: '2-digit',
            hour12: false
        });
    }

    function toLocalDateInput(date = new Date()) {
        const d = date instanceof Date ? date : new Date(date);
        const pad = n => String(n).padStart(2, '0');
        return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
    }

    function toLocalDateTimeInput(date = new Date()) {
        const d = date instanceof Date ? date : new Date(date);
        const pad = n => String(n).padStart(2, '0');
        return `${toLocalDateInput(d)}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
    }

    function addDaysToDateInput(baseDateStr, days) {
        const base = baseDateStr ? new Date(`${baseDateStr}T12:00:00`) : new Date();
        base.setDate(base.getDate() + days);
        return toLocalDateInput(base);
    }

    function buildWhatsAppUrl(phone, message) {
        if (!phone) return null;
        const digits = String(phone).replace(/\D/g, '');
        if (!digits) return null;
        const text = message ? `?text=${encodeURIComponent(message)}` : '';
        return `https://wa.me/${digits}${text}`;
    }

    function speciesMeta(especie) {
        const key = (especie || '').trim().toLowerCase();
        if (key.includes('perro') || key.includes('canin')) return { icon: '🐶', badge: 'badge-teal' };
        if (key.includes('gato') || key.includes('felin')) return { icon: '🐱', badge: 'badge-indigo' };
        if (key.includes('ave') || key.includes('loro') || key.includes('periq')) return { icon: '🦜', badge: 'badge-warning' };
        if (key.includes('conejo')) return { icon: '🐰', badge: 'badge-info' };
        if (key.includes('roedor') || key.includes('hamster') || key.includes('cobay')) return { icon: '🐹', badge: 'badge-warning' };
        if (key.includes('reptil') || key.includes('tortuga')) return { icon: '🐢', badge: 'badge-success' };
        return { icon: '🐾', badge: 'badge-neutral' };
    }

    function tipoCitaLabel(tipo) {
        const map = {
            CONSULTA_GENERAL: 'Consulta General',
            VACUNACION: 'Vacunación',
            URGENCIA: 'Urgencia',
            CIRUGIA: 'Cirugía',
            CONTROL: 'Control',
            DESPARASITACION: 'Desparasitación',
            ESTETICA: 'Estética / Baño'
        };
        return map[tipo] || tipo || 'Consulta';
    }

    function prioridadCitaMeta(prioridad) {
        const map = {
            URGENTE: { label: 'Urgente', cls: 'badge-danger' },
            ALTA: { label: 'Alta', cls: 'badge-warning' },
            NORMAL: { label: 'Normal', cls: 'badge-neutral' }
        };
        return map[prioridad] || { label: prioridad || 'Normal', cls: 'badge-neutral' };
    }

    function estadoCitaMeta(estado) {
        const map = {
            PROGRAMADA: { label: 'Programada', cls: 'badge-info' },
            EN_ATENCION: { label: 'En atención', cls: 'badge-warning' },
            COMPLETADA: { label: 'Completada', cls: 'badge-success' },
            CANCELADA: { label: 'Cancelada', cls: 'badge-neutral' }
        };
        return map[estado] || { label: estado || '—', cls: 'badge-neutral' };
    }

    function estadoVacunaMeta(estado, dias) {
        if (estado === 'VENCIDA') {
            const atraso = dias !== null && dias !== undefined ? `Hace ${Math.abs(dias)} d` : 'Vencida';
            return { label: `Vencida (${atraso})`, cls: 'badge-danger' };
        }
        if (estado === 'PROXIMA') {
            const texto = dias === 0 ? 'Vence hoy' : `En ${dias} día${dias === 1 ? '' : 's'}`;
            return { label: `Próxima (${texto})`, cls: 'badge-warning' };
        }
        if (estado === 'AL_DIA') {
            return { label: 'Al día', cls: 'badge-success' };
        }
        return { label: 'Completada', cls: 'badge-neutral' };
    }

    function initials(name) {
        if (!name) return '?';
        return name.trim().split(/\s+/).slice(0, 2).map(p => p[0]).join('').toUpperCase();
    }

    function debounce(fn, wait = 220) {
        let timer;
        return (...args) => {
            clearTimeout(timer);
            timer = setTimeout(() => fn(...args), wait);
        };
    }

    function showToast(message, type = 'success') {
        const container = document.getElementById('toast-container');
        if (!container) return;
        const toast = document.createElement('div');
        toast.className = `toast ${type}`;
        toast.innerHTML = `
            <span>${escapeHtml(message)}</span>
            <button class="icon-btn" style="width:26px;height:26px;border:none;" aria-label="Cerrar">✕</button>
        `;
        const closeBtn = toast.querySelector('button');
        const remove = () => toast.remove();
        closeBtn.addEventListener('click', remove);
        container.appendChild(toast);
        setTimeout(remove, 4200);
    }

    return {
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
    };
})();
