// Lógica para el formulario de crear plan

// Estado de actividades seleccionadas
let actividadesSeleccionadas = [];
let ordenActual = 1;
let timeoutBusqueda = null;

// Inicializar cuando el DOM esté listo
document.addEventListener('DOMContentLoaded', function () {
    const buscador = document.getElementById('buscadorActividades');
    const resultadosDiv = document.getElementById('resultadosBusqueda');

    // Búsqueda con debounce
    buscador.addEventListener('input', function (e) {
        clearTimeout(timeoutBusqueda);
        const termino = e.target.value.trim();

        if (termino.length < 2) {
            resultadosDiv.classList.add('hidden');
            return;
        }

        timeoutBusqueda = setTimeout(() => {
            buscarActividades(termino);
        }, 300);
    });
});

// Buscar actividades en el backend
async function buscarActividades(termino) {
    const resultadosDiv = document.getElementById('resultadosBusqueda');

    try {
        // Mostrar spinner
        resultadosDiv.innerHTML = `
            <div class="flex items-center justify-center py-4">
                <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-purple-600 mx-auto"></div>
                <span class="ml-2 text-gray-600">Buscando...</span>
            </div>
        `;
        resultadosDiv.classList.remove('hidden');

        // Llamar al endpoint JSON
        const response = await fetch(`/api/actividades/buscar?nombre=${encodeURIComponent(termino)}`);

        if (!response.ok) {
            throw new Error('Error al buscar actividades');
        }

        const actividades = await response.json();

        if (actividades.length === 0) {
            resultadosDiv.innerHTML = `
                <p class="text-center text-gray-500 py-4">
                    No se encontraron actividades
                </p>
            `;
            return;
        }

        // Renderizar resultados
        resultadosDiv.innerHTML = actividades.map((act, idx) => `
            <div class="actividad-resultado flex items-center gap-3 p-3 border-2 border-gray-200 rounded-xl hover:border-purple-500" data-idx="${idx}">
                <img src="/uploads/${act.imagen || 'default.jpg'}" 
                     alt="${act.titulo}"
                     class="w-16 h-16 object-cover rounded-lg">
                <div class="flex-1">
                    <h4 class="font-semibold text-gray-900">${act.titulo}</h4>
                    <p class="text-xs text-gray-600">${act.nombreCategoria || 'Sin categoría'}</p>
                </div>
                <button type="button" 
                        class="btn-agregar-actividad px-3 py-1 bg-purple-600 text-white text-sm font-semibold rounded-lg hover:bg-purple-700"
                        data-idx="${idx}">
                    Añadir
                </button>
            </div>
        `).join('');

        // Enlazar eventos a los botones
        document.querySelectorAll('.btn-agregar-actividad').forEach(btn => {
            btn.addEventListener('click', function(e) {
                e.stopPropagation();
                const idx = this.getAttribute('data-idx');
                agregarActividad(actividades[idx]);
            });
        });
    } catch (error) {
        console.error('Error al buscar actividades:', error);
        resultadosDiv.innerHTML = `
            <p class="text-center text-red-500 py-4">
                Error al buscar actividades
            </p>
        `;
    }
}

// Agregar actividad a la lista
function agregarActividad(actividad) {
    // Verificar si ya está seleccionada
    if (actividadesSeleccionadas.find(a => a.idActividad === actividad.idActividad)) {
        alert('Esta actividad ya está en tu plan');
        return;
    }

    // Añadir al array con orden
    const actividadConOrden = {
        ...actividad,
        orden: ordenActual++,
        horaSugerida: '',
        notaPersonalizada: ''
    };

    actividadesSeleccionadas.push(actividadConOrden);

    // Actualizar UI
    renderizarActividadesSeleccionadas();

    // Limpiar buscador
    document.getElementById('buscadorActividades').value = '';
    document.getElementById('resultadosBusqueda').classList.add('hidden');
}

// Renderizar lista de actividades seleccionadas
function renderizarActividadesSeleccionadas() {
    const container = document.getElementById('actividadesSeleccionadas');

    if (actividadesSeleccionadas.length === 0) {
        container.innerHTML = `
            <p class="text-gray-500 text-center py-8 border-2 border-dashed border-gray-300 rounded-xl">
                No has seleccionado actividades todavía. Busca y añade actividades arriba.
            </p>
        `;
        return;
    }

    container.innerHTML = actividadesSeleccionadas.map((act, index) => `
        <div class="actividad-seleccionada bg-white border-2 border-gray-300 rounded-xl p-4"
             data-index="${index}">
            <div class="flex items-start gap-4">
                <!-- Orden y controles -->
                <div class="flex flex-col items-center gap-2">
                    <span class="flex-shrink-0 w-10 h-10 rounded-full bg-purple-100 text-purple-700 flex items-center justify-center font-black">
                        ${act.orden}
                    </span>
                    <button type="button" 
                            onclick="moverActividad(${index}, 'arriba')"
                            ${index === 0 ? 'disabled' : ''}
                            class="p-1 text-gray-400 hover:text-purple-600 disabled:opacity-30">
                        <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <polyline points="18 15 12 9 6 15"></polyline>
                        </svg>
                    </button>
                    <button type="button"
                            onclick="moverActividad(${index}, 'abajo')"
                            ${index === actividadesSeleccionadas.length - 1 ? 'disabled' : ''}
                            class="p-1 text-gray-400 hover:text-purple-600 disabled:opacity-30">
                        <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <polyline points="6 9 12 15 18 9"></polyline>
                        </svg>
                    </button>
                </div>
                
                <!-- Info de actividad -->
                <div class="flex-1 space-y-3">
                    <div>
                        <h4 class="font-bold text-gray-900">${act.titulo}</h4>
                        <p class="text-sm text-gray-600">${act.nombreCategoria || 'Sin categoría'}</p>
                    </div>
                    
                    <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
                        <div>
                            <label class="block text-xs font-semibold text-gray-700 mb-1">Hora sugerida</label>
                            <input type="time"
                                   value="${act.horaSugerida || ''}"
                                   onchange="actualizarActividad(${index}, 'horaSugerida', this.value)"
                                   class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:border-purple-500 focus:ring-1 focus:ring-purple-200 text-sm">
                        </div>
                        <div class="col-span-2">
                            <label class="block text-xs font-semibold text-gray-700 mb-1">Nota para viajeros</label>
                            <textarea rows="2"
                                      placeholder="Ej: No olvides llevar protector solar"
                                      onchange="actualizarActividad(${index}, 'notaPersonalizada', this.value)"
                                      class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:border-purple-500 focus:ring-1 focus:ring-purple-200 text-sm resize-none">${act.notaPersonalizada || ''}</textarea>
                        </div>
                    </div>
                </div>
                
                <!-- Botón eliminar -->
                <button type="button"
                        onclick="eliminarActividad(${index})"
                        class="flex-shrink-0 p-2 text-red-500 hover:bg-red-50 rounded-lg transition-all">
                    <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <polyline points="3 6 5 6 21 6"></polyline>
                        <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                    </svg>
                </button>
            </div>
        </div>
    `).join('');
}

// Mover actividad en el orden
function moverActividad(index, direccion) {
    const nuevaPosicion = direccion === 'arriba' ? index - 1 : index + 1;

    if (nuevaPosicion < 0 || nuevaPosicion >= actividadesSeleccionadas.length) {
        return;
    }

    // Intercambiar posiciones
    [actividadesSeleccionadas[index], actividadesSeleccionadas[nuevaPosicion]] =
        [actividadesSeleccionadas[nuevaPosicion], actividadesSeleccionadas[index]];

    // Actualizar órdenes
    actividadesSeleccionadas.forEach((act, i) => {
        act.orden = i + 1;
    });

    renderizarActividadesSeleccionadas();
}

// Actualizar datos de una actividad
function actualizarActividad(index, campo, valor) {
    actividadesSeleccionadas[index][campo] = valor;
}

// Eliminar actividad
function eliminarActividad(index) {
    if (confirm('¿Seguro que quieres eliminar esta actividad del plan?')) {
        actividadesSeleccionadas.splice(index, 1);

        // Reordenar
        actividadesSeleccionadas.forEach((act, i) => {
            act.orden = i + 1;
        });

        ordenActual = actividadesSeleccionadas.length + 1;
        renderizarActividadesSeleccionadas();
    }
}

// Preparar datos antes de enviar el formulario
function prepararEnvio() {
    if (actividadesSeleccionadas.length === 0) {
        alert('Debes añadir al menos una actividad a tu plan');
        return false;
    }

    // Convertir a JSON y poner en campo oculto
    const datosActividades = actividadesSeleccionadas.map(act => ({
        idActividad: act.idActividad,
        orden: act.orden,
        horaSugerida: act.horaSugerida || null,
        notaPersonalizada: act.notaPersonalizada || null
    }));

    document.getElementById('actividadesJson').value = JSON.stringify(datosActividades);

    return true;
}

// Mostrar nombre del archivo seleccionado
function mostrarNombreArchivo(input) {
    const nombreArchivo = document.getElementById('nombreArchivo');
    if (input.files && input.files[0]) {
        nombreArchivo.textContent = `Archivo seleccionado: ${input.files[0].name}`;
        nombreArchivo.classList.add('text-purple-600', 'font-semibold');
    }
}
