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
let radioActual = 3;

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
    // Centro inicial: Cartagena como fallback
    const cartagenaLat = 10.3910;
    const cartagenaLon = -75.4794;

    mapa = L.map('mapaLeaflet').setView([cartagenaLat, cartagenaLon], 14);

    // Tile layer de OpenStreetMap
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
    sliderRadio.addEventListener('input', (e) => {
        const nuevoRadio = parseInt(e.target.value);
        document.getElementById('labelRadio').textContent = nuevoRadio + ' km';
        radioActual = nuevoRadio;

        // Solo recargar si ya tenemos ubicación
        if (latGuardada !== null && lonGuardada !== null) {
            cargarActividades(latGuardada, lonGuardada, radioActual);
        }
    });
}

// ========================================
// Geolocalización
// ========================================

/**
 * Solicita la ubicación del usuario usando Geolocation API
 */
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

            // Guardar coordenadas
            latGuardada = lat;
            lonGuardada = lon;

            // Centrar mapa en la ubicación del usuario
            mapa.setView([lat, lon], 15);

            // Pintar marcador del usuario
            pintarUserMarker(lat, lon);

            // Habilitar slider
            document.getElementById('sliderRadio').disabled = false;

            // Cambiar texto del botón
            btnTexto.textContent = 'Actualizar ubicación';

            // Cargar actividades cercanas
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

/**
 * Pinta el marcador azul del usuario en el mapa
 */
function pintarUserMarker(lat, lon) {
    // Remover marcador anterior si existe
    if (userMarker) {
        mapa.removeLayer(userMarker);
    }

    // Crear icono personalizado para el usuario
    const iconoUsuario = L.divIcon({
        className: 'marker-usuario',
        html: '<div class="marker-usuario"><svg width="24" height="24" fill="white" viewBox="0 0 24 24"><circle cx="12" cy="12" r="8"/></svg></div>',
        iconSize: [28, 28],
        iconAnchor: [14, 14]
    });

    userMarker = L.marker([lat, lon], { icon: iconoUsuario }).addTo(mapa);
    userMarker.bindPopup('Tu ubicación').openPopup();
}

// ========================================
// Carga de Actividades (AJAX)
// ========================================

/**
 * Llama al endpoint JSON para obtener actividades cercanas
 */
function cargarActividades(lat, lon, radio) {
    mostrarEstado('cargando');

    fetch(`/actividades/cercanas/json?lat=${lat}&lon=${lon}&radio=${radio}`)
        .then(response => {
            if (!response.ok) {
                throw new Error('Error al cargar actividades');
            }
            return response.json();
        })
        .then(data => {
            if (data.length === 0) {
                mostrarEstado('vacio');
                document.getElementById('contadorResultados').textContent = '0 actividades encontradas';
            } else {
                mostrarEstado('resultados');
                document.getElementById('contadorResultados').textContent =
                    `${data.length} ${data.length === 1 ? 'actividad encontrada' : 'actividades encontradas'}`;

                limpiarMarcadores();
                pintarMarcadores(data);
                renderizarTarjetas(data);
                pintarCirculoRadio(lat, lon, radio);
            }
        })
        .catch(error => {
            console.error('Error al cargar actividades:', error);
            mostrarEstado('inicial');
            mostrarAlerta('Ocurrió un error al buscar actividades. Intenta de nuevo.', 'error');
        });
}

// ========================================
// Renderizado de Marcadores en el Mapa
// ========================================

/**
 * Limpia todos los marcadores de actividades del mapa
 */
function limpiarMarcadores() {
    marcadoresActividades.forEach(marker => mapa.removeLayer(marker));
    marcadoresActividades = [];

    if (circuloRadio) {
        mapa.removeLayer(circuloRadio);
        circuloRadio = null;
    }
}

/**
 * Pinta marcadores numerados para cada actividad
 */
function pintarMarcadores(actividades) {
    actividades.forEach((actividad, index) => {
        const numero = index + 1;

        // Crear icono numerado
        const iconoNumero = L.divIcon({
            className: 'marker-numero',
            html: `<div class="marker-numero">${numero}</div>`,
            iconSize: [28, 28],
            iconAnchor: [14, 28]
        });

        const marker = L.marker([actividad.latitud, actividad.longitud], {
            icon: iconoNumero
        }).addTo(mapa);

        // Popup con info básica
        marker.bindPopup(`
            <div class="text-center">
                <strong>${actividad.titulo}</strong><br>
                <span class="text-sm text-gray-600">${actividad.distanciaKm} km</span>
            </div>
        `);

        // Click en marcador → resaltar tarjeta
        marker.on('click', () => {
            highlightTarjeta(actividad.idActividad);
            scrollToTarjeta(actividad.idActividad);
        });

        marcadoresActividades.push(marker);
    });
}

/**
 * Pinta un círculo semitransparente mostrando el radio activo
 */
function pintarCirculoRadio(lat, lon, radioKm) {
    if (circuloRadio) {
        mapa.removeLayer(circuloRadio);
    }

    circuloRadio = L.circle([lat, lon], {
        color: '#1D9E75',
        fillColor: '#1D9E75',
        fillOpacity: 0.1,
        radius: radioKm * 1000 // convertir km a metros
    }).addTo(mapa);
}

// ========================================
// Renderizado de Tarjetas en el Panel
// ========================================

/**
 * Genera HTML de tarjetas en el panel lateral
 */
function renderizarTarjetas(actividades) {
    const listaTarjetas = document.getElementById('listaTarjetas');
    listaTarjetas.innerHTML = '';

    actividades.forEach((actividad, index) => {
        const numero = index + 1;
        const urlDetalle = construirUrlDetalle(actividad.slug, actividad.idActividad);

        const tarjetaHTML = `
            <div class="tarjeta-cercana" data-id="${actividad.idActividad}">
                <div class="flex gap-3 cursor-pointer" onclick="navegarADetalle('${urlDetalle}')">
                    <!-- Número -->
                    <div class="flex-shrink-0">
                        <div class="marker-numero-tarjeta">${numero}</div>
                    </div>
                    <!-- src="/images/${actividad.imagen || 'placeholder.jpg'}"  -->
                    <!-- Imagen -->
                    <div class="flex-shrink-0 w-24 h-24 rounded-lg overflow-hidden bg-gray-200">
                        <img 
                            th:if="${actividad.imagen != null}"
                            th:src="@{'/uploads/' + ${actividad.imagen}}"
                            alt="${actividad.titulo}"
                            class="w-full h-full object-cover">
                        <img 
                            th:if="${actividad.imagen == null}"
                            src="/images/placeholder.jpg"
                            alt="${actividad.titulo}"
                            class="w-full h-full object-cover">
                    </div>
                    
                    <!-- Info -->
                    <div class="flex-1 min-w-0">
                        <h3 class="font-semibold text-gray-800 text-sm mb-1 truncate">
                            ${actividad.titulo}
                        </h3>
                        <p class="text-xs text-gray-600 mb-1">
                            ${actividad.categoriaNombre || 'Sin categoría'}
                        </p>
                        <div class="flex items-center gap-2 text-xs text-gray-600 mb-2">
                            <svg class="w-3 h-3" fill="currentColor" viewBox="0 0 20 20">
                                <path fill-rule="evenodd" d="M5.05 4.05a7 7 0 119.9 9.9L10 18.9l-4.95-4.95a7 7 0 010-9.9zM10 11a2 2 0 100-4 2 2 0 000 4z" clip-rule="evenodd"/>
                            </svg>
                            <span>${actividad.distanciaKm} km</span>
                        </div>
                        <div class="flex items-center justify-between">
                            <span class="font-bold text-primary">
                                ${formatearPrecio(actividad.precioConsumidor)}
                            </span>
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

/**
 * Resalta visualmente una tarjeta específica
 */
function highlightTarjeta(idActividad) {
    // Remover resaltado de todas
    document.querySelectorAll('.tarjeta-cercana').forEach(tarjeta => {
        tarjeta.classList.remove('activa');
    });

    // Añadir resaltado a la seleccionada
    const tarjeta = document.querySelector(`.tarjeta-cercana[data-id="${idActividad}"]`);
    if (tarjeta) {
        tarjeta.classList.add('activa');
    }
}

/**
 * Hace scroll hacia una tarjeta en el panel
 */
function scrollToTarjeta(idActividad) {
    const tarjeta = document.querySelector(`.tarjeta-cercana[data-id="${idActividad}"]`);
    if (tarjeta) {
        tarjeta.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }
}

/**
 * Navega a la página de detalle de la actividad
 */
function navegarADetalle(url) {
    window.location.href = url;
}

/**
 * Construye la URL de detalle con slug e id
 */
function construirUrlDetalle(slug, id) {
    return `/actividad/${slug}-${id}`;
}

// ========================================
// Manejo de Estados de la Vista
// ========================================

/**
 * Cambia el estado visible del panel de resultados
 */
function mostrarEstado(estado) {
    const estadoInicial = document.getElementById('estadoInicial');
    const estadoCargando = document.getElementById('estadoCargando');
    const estadoVacio = document.getElementById('estadoVacio');
    const listaTarjetas = document.getElementById('listaTarjetas');

    // Ocultar todos
    estadoInicial.classList.add('hidden');
    estadoCargando.classList.add('hidden');
    estadoVacio.classList.add('hidden');
    listaTarjetas.classList.add('hidden');

    // Mostrar el estado correspondiente
    switch (estado) {
        case 'inicial':
            estadoInicial.classList.remove('hidden');
            break;
        case 'cargando':
            estadoCargando.classList.remove('hidden');
            break;
        case 'vacio':
            estadoVacio.classList.remove('hidden');
            document.getElementById('mensajeVacio').innerHTML =
                `No hay actividades en ${radioActual} km.<br>Intenta ampliar el radio.`;
            break;
        case 'resultados':
            listaTarjetas.classList.remove('hidden');
            break;
    }
}

// ========================================
// Utilidades
// ========================================

/**
 * Formatea un número BigDecimal a formato moneda colombiana
 */
function formatearPrecio(precio) {
    return new Intl.NumberFormat('es-CO', {
        style: 'currency',
        currency: 'COP',
        minimumFractionDigits: 0
    }).format(precio);
}

/**
 * Muestra una alerta usando el sistema global de alertas
 */
function mostrarAlerta(mensaje, tipo) {
    // Asumiendo que existe un sistema de alertas global (alert.js)
    if (typeof showAlert === 'function') {
        showAlert(mensaje, tipo);
    } else {
        alert(mensaje);
    }
}
