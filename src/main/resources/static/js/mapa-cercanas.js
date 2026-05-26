// ========================================
// Módulo de Mapa de Actividades Cercanas
// ========================================
// Estado global
let mapa = null;
let userMarker = null;
let marcadoresActividades = [];
let circuloRadio = null;
let latGuardada = null;
let lonGuardada = null;
let radioActual = 3000;

// ========================================
// Inicialización
// ========================================
document.addEventListener('DOMContentLoaded', () => {
    inicializarMapa();
    configurarEventos();
});

/**
 * Crea el mapa Leaflet con tile layer de OpenStreetMap
 */
function inicializarMapa() {
    const cartagenaLat = 10.3910;
    const cartagenaLon = -75.4794;
    mapa = L.map('mapaLeaflet').setView([cartagenaLat, cartagenaLon], 14);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
        maxZoom: 19
    }).addTo(mapa);
}

/**
 * Configura listeners de eventos
 */
function configurarEventos() {
    const btnUbicacion = document.getElementById('btnUbicacion');
    const sliderRadio = document.getElementById('sliderRadio');

    btnUbicacion.addEventListener('click', solicitarUbicacion);

    // Slider: actualizar label + preview del círculo en tiempo real, sin recargar
    sliderRadio.addEventListener('input', (e) => {
        radioActual = parseInt(e.target.value);
        actualizarLabelRadio(radioActual);

        // Actualizar círculo en tiempo real si hay ubicación
        if (latGuardada !== null && lonGuardada !== null) {
            pintarCirculoRadio(latGuardada, lonGuardada, radioActual);
            mostrarBotonBuscarCerca(true);
        }
    });

    const btnBuscarCerca = document.getElementById('btnBuscarCerca');
    if (btnBuscarCerca) {
        btnBuscarCerca.addEventListener('click', () => {
            if (latGuardada !== null && lonGuardada !== null) {
                cargarActividades(latGuardada, lonGuardada, radioActual);
                // El botón se oculta dentro de cargarActividades
            }
        });
    }

    const btnBuscarZona = document.getElementById('btnBuscarZona');
    const inputBuscarZona = document.getElementById('inputBuscarZona');
    if (btnBuscarZona && inputBuscarZona) {
        btnBuscarZona.addEventListener('click', () => buscarZona(inputBuscarZona.value));
        inputBuscarZona.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                e.preventDefault();
                buscarZona(inputBuscarZona.value);
            }
        });
    }

    // Botón centrar mapa en la ubicación guardada
    const btnCentrar = document.getElementById('btnCentrarMapa');
    if (btnCentrar) {
        btnCentrar.addEventListener('click', () => {
            if (latGuardada !== null && lonGuardada !== null) {
                mapa.setView([latGuardada, lonGuardada], 15, { animate: true });
            } else {
                mostrarAlerta('Aún no tienes una ubicación activa.', 'error');
            }
        });
    }
}

// ========================================
// Helpers de UI
// ========================================

/**
 * Muestra u oculta el botón "Buscar aquí"
 */
function mostrarBotonBuscarCerca(mostrar) {
    const btn = document.getElementById('btnBuscarCerca');
    if (!btn) return;
    if (mostrar) {
        btn.classList.remove('hidden');
        btn.classList.add('inline-flex');
    } else {
        btn.classList.add('hidden');
        btn.classList.remove('inline-flex');
    }
}

/**
 * Actualiza el texto del label del radio
 */
function actualizarLabelRadio(radioMts) {
    const label = document.getElementById('labelRadio');
    if (!label) return;
    if (radioMts >= 1000) {
        label.textContent = (radioMts / 1000).toFixed(1).replace('.0', '') + ' km';
    } else {
        label.textContent = radioMts + ' mts';
    }
}

// ========================================
// Geolocalización
// ========================================

function solicitarUbicacion() {
    if (!navigator.geolocation) {
        mostrarAlerta('Tu navegador no soporta geolocalización', 'error');
        return;
    }
    const btnTexto = document.getElementById('btnTexto');
    btnTexto.textContent = 'Detectando ubicación...';
    mostrarEstado('cargando');

    navigator.geolocation.getCurrentPosition(
        (position) => {
            const lat = position.coords.latitude;
            const lon = position.coords.longitude;
            latGuardada = lat;
            lonGuardada = lon;

            mapa.setView([lat, lon], 15, { animate: true });
            pintarUserMarker(lat, lon);

            document.getElementById('sliderRadio').disabled = false;
            btnTexto.textContent = 'Actualizar ubicación';
            document.dispatchEvent(new CustomEvent('ubicacionActiva'));

            cargarActividades(lat, lon, radioActual);
        },
        (error) => {
            console.error('Error de geolocalización:', error);
            btnTexto.textContent = 'Usar mi ubicación';
            mostrarEstado('inicial');
            let mensaje = 'No pudimos obtener tu ubicación.';
            switch (error.code) {
                case error.PERMISSION_DENIED:
                    mensaje = 'Debes permitir el acceso a tu ubicación en el navegador.';
                    break;
                case error.POSITION_UNAVAILABLE:
                    mensaje = 'Tu ubicación no está disponible en este momento.';
                    break;
                case error.TIMEOUT:
                    mensaje = 'La solicitud de ubicación expiró. Intenta de nuevo.';
                    break;
            }
            mostrarAlerta(mensaje, 'error');
        }
    );
}

function pintarUserMarker(lat, lon) {
    if (userMarker) mapa.removeLayer(userMarker);

    const iconoUsuario = L.divIcon({
        className: 'bg-transparent border-0',
        html: '<div class="w-7 h-7 bg-blue-600 rounded-full flex items-center justify-center shadow-[0_0_15px_rgba(37,99,235,0.5)] border-[3px] border-white"><div class="w-2.5 h-2.5 bg-white rounded-full"></div></div>',
        iconSize: [28, 28],
        iconAnchor: [14, 14]
    });

    userMarker = L.marker([lat, lon], { icon: iconoUsuario }).addTo(mapa);
    userMarker.bindPopup('Tu ubicación / Zona seleccionada').openPopup();
}

function buscarZona(zona) {
    if (!zona || zona.trim() === '') {
        mostrarAlerta('Por favor, escribe una zona a buscar.', 'error');
        return;
    }
    const btnTexto = document.getElementById('btnTexto');
    btnTexto.textContent = 'Usar mi ubicación';
    mostrarEstado('cargando');

    const query = encodeURIComponent(zona.trim() + ', Cartagena, Colombia');
    fetch(`https://nominatim.openstreetmap.org/search?format=json&q=${query}`)
        .then(r => r.json())
        .then(data => {
            if (data && data.length > 0) {
                const lat = parseFloat(data[0].lat);
                const lon = parseFloat(data[0].lon);
                latGuardada = lat;
                lonGuardada = lon;
                mapa.setView([lat, lon], 15, { animate: true });
                pintarUserMarker(lat, lon);
                document.getElementById('sliderRadio').disabled = false;
                document.dispatchEvent(new CustomEvent('ubicacionActiva'));
                cargarActividades(lat, lon, radioActual);
            } else {
                mostrarEstado('inicial');
                mostrarAlerta('No se encontró la zona especificada.', 'error');
            }
        })
        .catch(error => {
            console.error('Error al buscar la zona:', error);
            mostrarEstado('inicial');
            mostrarAlerta('Ocurrió un error al buscar la zona. Intenta nuevamente.', 'error');
        });
}

// ========================================
// Carga de Actividades (AJAX)
// ========================================

function cargarActividades(lat, lon, radioMts) {
    mostrarEstado('cargando');
    mostrarBotonBuscarCerca(false); // ocultar mientras carga

    const radioKm = radioMts / 1000.0;
    fetch(`/actividades/cercanas/json?lat=${lat}&lon=${lon}&radio=${radioKm}`)
        .then(response => {
            if (!response.ok) throw new Error('Error al cargar actividades');
            return response.json();
        })
        .then(data => {
            limpiarMarcadores();
            pintarCirculoRadio(lat, lon, radioMts); // siempre pintar el círculo actual

            if (data.length === 0) {
                mostrarEstado('vacio');
                document.getElementById('contadorResultados').textContent = '0 actividades encontradas';
            } else {
                mostrarEstado('resultados');
                document.getElementById('contadorResultados').textContent =
                    `${data.length} ${data.length === 1 ? 'actividad encontrada' : 'actividades encontradas'}`;
                pintarMarcadores(data);
                renderizarTarjetas(data);
            }
        })
        .catch(error => {
            console.error('Error al cargar actividades:', error);
            mostrarEstado('inicial');
            mostrarAlerta('Ocurrió un error al buscar actividades. Intenta de nuevo.', 'error');
        });
}

// ========================================
// Marcadores y Círculo
// ========================================

function limpiarMarcadores() {
    marcadoresActividades.forEach(marker => mapa.removeLayer(marker));
    marcadoresActividades = [];
    if (circuloRadio) {
        mapa.removeLayer(circuloRadio);
        circuloRadio = null;
    }
}

function pintarMarcadores(actividades) {
    actividades.forEach((actividad, index) => {
        const numero = index + 1;
        const iconoNumero = L.divIcon({
            className: 'bg-transparent border-0',
            html: `<div class="w-8 h-8 bg-gray-900 rounded-full flex items-center justify-center text-white font-bold text-sm shadow-lg border-[3px] border-white transition-transform hover:scale-110 hover:bg-orange-600 cursor-pointer">${numero}</div>`,
            iconSize: [32, 32],
            iconAnchor: [16, 32]
        });

        const marker = L.marker([actividad.latitud, actividad.longitud], { icon: iconoNumero }).addTo(mapa);
        marker.bindPopup(`
            <div class="text-center">
                <strong>${actividad.titulo}</strong><br>
                <span class="text-sm text-gray-600">${actividad.distanciaKm} km</span>
            </div>
        `);
        marker.on('click', () => {
            highlightTarjeta(actividad.idActividad);
            scrollToTarjeta(actividad.idActividad);
        });
        marcadoresActividades.push(marker);
    });
}

function pintarCirculoRadio(lat, lon, radioMts) {
    if (circuloRadio) mapa.removeLayer(circuloRadio);
    circuloRadio = L.circle([lat, lon], {
        color: '#1D9E75',
        fillColor: '#1D9E75',
        fillOpacity: 0.1,
        radius: radioMts
    }).addTo(mapa);
}

// ========================================
// Tarjetas
// ========================================

function renderizarTarjetas(actividades) {
    const listaTarjetas = document.getElementById('listaTarjetas');
    listaTarjetas.innerHTML = '';
    actividades.forEach((actividad, index) => {
        const numero = index + 1;
        const urlDetalle = construirUrlDetalle(actividad.slug, actividad.idActividad);
        const tarjetaHTML = `
            <div class="tarjeta-cercana" data-id="${actividad.idActividad}">
                <div class="flex gap-3 cursor-pointer" onclick="navegarADetalle('${urlDetalle}')">
                    <div class="flex-shrink-0">
                        <div class="w-7 h-7 bg-gray-900 rounded-full flex items-center justify-center text-white font-bold text-xs shadow-md border-2 border-white">${numero}</div>
                    </div>
                    <div class="flex-shrink-0 w-24 h-24 rounded-lg overflow-hidden bg-gray-200">
                        <img 
                            src="${actividad.imagen ? '/uploads/' + actividad.imagen : '/images/placeholder.webp'}"
                            alt="${actividad.titulo}"
                            class="w-full h-full object-cover">
                    </div>
                    <div class="flex-1 min-w-0">
                        <h3 class="font-semibold text-gray-800 text-sm mb-1 truncate">${actividad.titulo}</h3>
                        <p class="text-xs text-gray-600 mb-1">${actividad.categoriaNombre || 'Sin categoría'}</p>
                        <div class="flex items-center gap-2 text-xs text-gray-600 mb-2">
                            <svg class="w-3 h-3" fill="currentColor" viewBox="0 0 20 20">
                                <path fill-rule="evenodd" d="M5.05 4.05a7 7 0 119.9 9.9L10 18.9l-4.95-4.95a7 7 0 010-9.9zM10 11a2 2 0 100-4 2 2 0 000 4z" clip-rule="evenodd"/>
                            </svg>
                            <span>${actividad.distanciaKm} km</span>
                        </div>
                        <div class="flex items-center justify-between">
                            <span class="font-bold text-primary">${formatearPrecio(actividad.precioConsumidor)}</span>
                            <div class="flex items-center gap-1">
                                <svg class="w-4 h-4 text-yellow-400" fill="currentColor" viewBox="0 0 20 20">
                                    <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.07 3.292a1 1 0 00.95.69h3.462c.969 0 1.371 1.24.588 1.81l-2.8 2.034a1 1 0 00-.364 1.118l1.07 3.292c.3.921-.755 1.688-1.54 1.118l-2.8-2.034a1 1 0 00-1.175 0l-2.8 2.034c-.784.57-1.838-.197-1.539-1.118l1.07-3.292a1 1 0 00-.364-1.118L2.98 8.72c-.783-.57-.38-1.81.588-1.81h3.461a1 1 0 00.951-.69l1.07-3.292z"/>
                                </svg>
                                <span class="text-sm text-gray-600">${actividad.calificacion}/5</span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        `;
        listaTarjetas.insertAdjacentHTML('beforeend', tarjetaHTML);
    });
}

function highlightTarjeta(idActividad) {
    document.querySelectorAll('.tarjeta-cercana').forEach(t => t.classList.remove('activa'));
    const tarjeta = document.querySelector(`.tarjeta-cercana[data-id="${idActividad}"]`);
    if (tarjeta) tarjeta.classList.add('activa');
}

function scrollToTarjeta(idActividad) {
    const tarjeta = document.querySelector(`.tarjeta-cercana[data-id="${idActividad}"]`);
    if (tarjeta) tarjeta.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
}

function navegarADetalle(url) {
    window.location.href = url;
}

function construirUrlDetalle(slug, id) {
    return `/actividad/${slug}-${id}`;
}

// ========================================
// Estados
// ========================================

function mostrarEstado(estado) {
    const estadoInicial = document.getElementById('estadoInicial');
    const estadoCargando = document.getElementById('estadoCargando');
    const estadoVacio = document.getElementById('estadoVacio');
    const listaTarjetas = document.getElementById('listaTarjetas');

    estadoInicial.classList.add('hidden');
    estadoCargando.classList.add('hidden');
    estadoVacio.classList.add('hidden');
    listaTarjetas.classList.add('hidden');

    switch (estado) {
        case 'inicial':
            estadoInicial.classList.remove('hidden');
            break;
        case 'cargando':
            estadoCargando.classList.remove('hidden');
            break;
        case 'vacio':
            estadoVacio.classList.remove('hidden');
            const textoMts = radioActual >= 1000 ? (radioActual / 1000) + ' km' : radioActual + ' mts';
            document.getElementById('mensajeVacio').innerHTML =
                `No hay actividades en ${textoMts}.<br>Intenta ampliar el radio.`;
            break;
        case 'resultados':
            listaTarjetas.classList.remove('hidden');
            break;
    }
}

// ========================================
// Utilidades
// ========================================

function formatearPrecio(precio) {
    return new Intl.NumberFormat('es-CO', {
        style: 'currency',
        currency: 'COP',
        minimumFractionDigits: 0
    }).format(precio);
}

function mostrarAlerta(mensaje, tipo) {
    if (typeof showAlert === 'function') {
        showAlert(mensaje, tipo);
    } else {
        alert(mensaje);
    }
}