class ApiError extends Error {
    constructor(message, status, fieldErrors = null) {
        super(message);
        this.status = status;
        this.fieldErrors = fieldErrors;
    }
}

const VetApi = (() => {
    const devPreviewPorts = ['5500', '5501', '5502', '5503', '3000', '4200', '5173', '63342'];
    const isStaticPreview = window.location.protocol === 'file:'
        || window.location.pathname.includes('/src/main/resources/static')
        || devPreviewPorts.includes(window.location.port);
    const API_BASE = isStaticPreview ? 'http://localhost:8080' : '';

    async function request(url, options = {}) {
        const config = {
            headers: {
                'Content-Type': 'application/json',
                ...(options.headers || {})
            },
            ...options
        };

        let response;
        try {
            response = await fetch(`${API_BASE}${url}`, config);
        } catch (networkErr) {
            throw new ApiError(
                'No se pudo conectar con el servidor Spring Boot en http://localhost:8080. Asegúrate de iniciar el backend primero.',
                0
            );
        }
        if (response.status === 204) {
            return null;
        }

        const contentType = response.headers.get('content-type') || '';
        const data = contentType.includes('application/json') ? await response.json() : null;

        if (!response.ok) {
            const message = (data && data.message) || `Error HTTP ${response.status}`;
            const fieldErrors = (data && data.fieldErrors) || null;
            throw new ApiError(message, response.status, fieldErrors);
        }

        return data;
    }

    function toQuery(params = {}) {
        const search = new URLSearchParams();
        Object.entries(params).forEach(([k, v]) => {
            if (v !== null && v !== undefined && String(v).trim() !== '') {
                search.append(k, String(v).trim());
            }
        });
        const str = search.toString();
        return str ? `?${str}` : '';
    }

    return {
        baseUrl: API_BASE,
        dashboard: () => request('/api/dashboard'),

        // Propietarios
        getPropietarios: (q = '') => request(`/api/propietarios${toQuery({ q })}`),
        createPropietario: (payload) => request('/api/propietarios', { method: 'POST', body: JSON.stringify(payload) }),
        updatePropietario: (id, payload) => request(`/api/propietarios/${id}`, { method: 'PUT', body: JSON.stringify(payload) }),
        deletePropietario: (id) => request(`/api/propietarios/${id}`, { method: 'DELETE' }),

        // Mascotas
        getMascotas: (params = {}) => request(`/api/mascotas${toQuery(params)}`),
        getExpediente: (id) => request(`/api/mascotas/${id}/expediente`),
        createMascota: (payload) => request('/api/mascotas', { method: 'POST', body: JSON.stringify(payload) }),
        updateMascota: (id, payload) => request(`/api/mascotas/${id}`, { method: 'PUT', body: JSON.stringify(payload) }),
        deleteMascota: (id) => request(`/api/mascotas/${id}`, { method: 'DELETE' }),

        // Citas
        getCitas: (params = {}) => request(`/api/citas${toQuery(params)}`),
        createCita: (payload) => request('/api/citas', { method: 'POST', body: JSON.stringify(payload) }),
        updateCita: (id, payload) => request(`/api/citas/${id}`, { method: 'PUT', body: JSON.stringify(payload) }),
        cambiarEstadoCita: (id, estado) => request(`/api/citas/${id}/estado`, { method: 'PATCH', body: JSON.stringify({ estado }) }),
        deleteCita: (id) => request(`/api/citas/${id}`, { method: 'DELETE' }),

        // Consultas
        getConsultas: (params = {}) => request(`/api/consultas${toQuery(params)}`),
        createConsulta: (payload) => request('/api/consultas', { method: 'POST', body: JSON.stringify(payload) }),
        updateConsulta: (id, payload) => request(`/api/consultas/${id}`, { method: 'PUT', body: JSON.stringify(payload) }),
        deleteConsulta: (id) => request(`/api/consultas/${id}`, { method: 'DELETE' }),

        // Vacunas
        getVacunas: (params = {}) => request(`/api/vacunas${toQuery(params)}`),
        createVacuna: (payload) => request('/api/vacunas', { method: 'POST', body: JSON.stringify(payload) }),
        updateVacuna: (id, payload) => request(`/api/vacunas/${id}`, { method: 'PUT', body: JSON.stringify(payload) }),
        deleteVacuna: (id) => request(`/api/vacunas/${id}`, { method: 'DELETE' })
    };
})();
