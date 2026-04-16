/**
 * Módulo de Predicción de Ocupación
 * 
 * Funciones auxiliares para integrar las predicciones del modelo ML
 * en la interfaz de usuario.
 */

/**
 * Obtiene la predicción de ocupación para una disponibilidad específica.
 * 
 * @param {number} idDisponibilidad - ID de la disponibilidad
 * @returns {Promise<Object>} Datos de la predicción
 */
async function obtenerPrediccionDisponibilidad(idDisponibilidad) {
    try {
        const response = await fetch(`/api/prediccion/disponibilidad/${idDisponibilidad}`);
        
        if (!response.ok) {
            throw new Error('Error al obtener predicción');
        }
        
        const prediccion = await response.json();
        return prediccion;
    } catch (error) {
        console.error('Error en predicción:', error);
        throw error;
    }
}

/**
 * Obtiene la predicción de ocupación con parámetros manuales.
 * 
 * @param {Object} params - Parámetros de la predicción
 * @param {string} params.categoria - aventura, cultura, gastronomia, etc.
 * @param {string} params.rangoPrecio - bajo, medio, alto
 * @param {string} params.rangoHora - mañana, tarde, noche
 * @param {string} params.tipoDia - entre_semana, fin_semana
 * @param {string} params.rangoCupos - bajo, medio, alto
 * @returns {Promise<Object>} Datos de la predicción
 */
async function obtenerPrediccionManual(params) {
    try {
        const queryParams = new URLSearchParams(params);
        const response = await fetch(`/api/prediccion/manual?${queryParams}`);
        
        if (!response.ok) {
            throw new Error('Error al obtener predicción');
        }
        
        const prediccion = await response.json();
        return prediccion;
    } catch (error) {
        console.error('Error en predicción manual:', error);
        throw error;
    }
}

/**
 * Renderiza un badge visual con el nivel de ocupación predicho.
 * 
 * @param {string} nivelOcupacion - baja, media, alta, agotado
 * @param {number} confianza - Confianza de la predicción (0.0 a 1.0)
 * @returns {string} HTML del badge
 */
function renderizarBadgeOcupacion(nivelOcupacion, confianza) {
    const config = {
        baja: {
            color: 'bg-green-100 text-green-800 border-green-300',
            icono: '🟢',
            texto: 'Baja demanda'
        },
        media: {
            color: 'bg-yellow-100 text-yellow-800 border-yellow-300',
            icono: '🟡',
            texto: 'Demanda moderada'
        },
        alta: {
            color: 'bg-orange-100 text-orange-800 border-orange-300',
            icono: '🟠',
            texto: 'Alta demanda'
        },
        agotado: {
            color: 'bg-red-100 text-red-800 border-red-300',
            icono: '🔴',
            texto: 'Se agotará'
        }
    };

    const style = config[nivelOcupacion] || config.media;
    const confianzaPorcentaje = (confianza * 100).toFixed(0);

    return `
        <div class="inline-flex items-center gap-2 px-3 py-1 rounded-full border ${style.color}">
            <span>${style.icono}</span>
            <span class="font-medium">${style.texto}</span>
            <span class="text-xs opacity-75">(${confianzaPorcentaje}% confianza)</span>
        </div>
    `;
}

/**
 * Muestra la predicción en el calendario de disponibilidades.
 * Agrega un indicador visual a cada fecha.
 * 
 * @param {HTMLElement} elementoFecha - Elemento DOM de la fecha
 * @param {number} idDisponibilidad - ID de la disponibilidad
 */
async function mostrarPrediccionEnCalendario(elementoFecha, idDisponibilidad) {
    try {
        const prediccion = await obtenerPrediccionDisponibilidad(idDisponibilidad);
        
        // Crear badge pequeño para el calendario
        const badge = document.createElement('div');
        badge.className = 'prediccion-badge';
        badge.innerHTML = obtenerIconoOcupacion(prediccion.nivelOcupacion);
        badge.title = `Predicción: ${prediccion.nivelOcupacion} (${(prediccion.confianza * 100).toFixed(0)}% confianza)`;
        
        elementoFecha.appendChild(badge);
    } catch (error) {
        console.error('Error al mostrar predicción:', error);
    }
}

/**
 * Obtiene el icono correspondiente al nivel de ocupación.
 * 
 * @param {string} nivel - baja, media, alta, agotado
 * @returns {string} Emoji del icono
 */
function obtenerIconoOcupacion(nivel) {
    const iconos = {
        baja: '🟢',
        media: '🟡',
        alta: '🟠',
        agotado: '🔴'
    };
    return iconos[nivel] || '⚪';
}

/**
 * Ejemplo de uso en el dashboard del colaborador:
 * Mostrar predicción al crear una nueva disponibilidad.
 */
function ejemploUsoCrearDisponibilidad() {
    const formCrearDisponibilidad = document.getElementById('formCrearDisponibilidad');
    
    if (formCrearDisponibilidad) {
        formCrearDisponibilidad.addEventListener('submit', async (e) => {
            // ... código existente de validación ...
            
            // Después de crear la disponibilidad, obtener predicción
            const idDisponibilidadCreada = 123; // ID devuelto por el backend
            
            try {
                const prediccion = await obtenerPrediccionDisponibilidad(idDisponibilidadCreada);
                
                // Mostrar notificación con la predicción
                mostrarNotificacion(
                    `Disponibilidad creada. Predicción: ${prediccion.nivelOcupacion}`,
                    'success'
                );
                
                // Actualizar UI con indicador visual
                const contenedorPrediccion = document.getElementById('prediccionContainer');
                contenedorPrediccion.innerHTML = renderizarBadgeOcupacion(
                    prediccion.nivelOcupacion,
                    prediccion.confianza
                );
            } catch (error) {
                console.error('Error al obtener predicción:', error);
            }
        });
    }
}

/**
 * Ejemplo de uso en el calendario de actividades:
 * Cargar predicciones para todas las fechas del mes.
 */
async function cargarPrediccionesCalendario(disponibilidades) {
    const predicciones = await Promise.all(
        disponibilidades.map(async (disp) => {
            try {
                const pred = await obtenerPrediccionDisponibilidad(disp.idDisponibilidad);
                return { ...disp, prediccion: pred };
            } catch (error) {
                console.error(`Error en predicción para disp ${disp.idDisponibilidad}:`, error);
                return { ...disp, prediccion: null };
            }
        })
    );
    
    return predicciones;
}

/**
 * Renderiza un tooltip con información detallada de la predicción.
 * 
 * @param {Object} prediccion - Datos de la predicción
 * @returns {string} HTML del tooltip
 */
function renderizarTooltipPrediccion(prediccion) {
    return `
        <div class="bg-white shadow-lg rounded-lg p-4 max-w-xs">
            <h4 class="font-bold text-gray-800 mb-2">Predicción de Demanda</h4>
            <div class="space-y-2 text-sm">
                <div class="flex justify-between">
                    <span class="text-gray-600">Nivel:</span>
                    <span class="font-semibold">${prediccion.nivelOcupacion}</span>
                </div>
                <div class="flex justify-between">
                    <span class="text-gray-600">Confianza:</span>
                    <span class="font-semibold">${(prediccion.confianza * 100).toFixed(1)}%</span>
                </div>
                <div class="flex justify-between">
                    <span class="text-gray-600">Categoría:</span>
                    <span>${prediccion.categoria}</span>
                </div>
                <div class="flex justify-between">
                    <span class="text-gray-600">Horario:</span>
                    <span>${prediccion.rangoHora}</span>
                </div>
                <div class="flex justify-between">
                    <span class="text-gray-600">Tipo día:</span>
                    <span>${prediccion.tipoDia === 'fin_semana' ? 'Fin de semana' : 'Entre semana'}</span>
                </div>
            </div>
        </div>
    `;
}

// Exportar funciones (si se usa módulos ES6)
if (typeof module !== 'undefined' && module.exports) {
    module.exports = {
        obtenerPrediccionDisponibilidad,
        obtenerPrediccionManual,
        renderizarBadgeOcupacion,
        mostrarPrediccionEnCalendario,
        cargarPrediccionesCalendario,
        renderizarTooltipPrediccion
    };
}
