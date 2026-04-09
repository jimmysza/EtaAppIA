// Lógica del mapa interactivo para la vista de planes

document.addEventListener('DOMContentLoaded', function() {
    // Inicializar mapa centrado en Cartagena
    const map = L.map('map').setView([10.391049, -75.479426], 13);

    // Añadir capa de OpenStreetMap
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
        maxZoom: 18,
    }).addTo(map);

    // Paleta de colores para diferentes planes
    const colores = ['#7c3aed', '#3b82f6', '#10b981', '#f59e0b', '#ef4444'];

    // Añadir marcadores por cada plan
    if (planesData && planesData.length > 0) {
        planesData.forEach((plan, planIndex) => {
            const color = colores[planIndex % colores.length];
            
            if (plan.actividades && plan.actividades.length > 0) {
                // Crear polilínea para conectar las actividades del plan
                const puntos = [];
                
                plan.actividades.forEach((actividad, actIndex) => {
                    if (actividad.latitud && actividad.longitud) {
                        const latlng = [actividad.latitud, actividad.longitud];
                        puntos.push(latlng);
                        
                        // Crear marcador personalizado
                        const markerHtml = `
                            <div class="custom-marker" style="border-color: ${color}; color: ${color};">
                                ${actividad.orden}
                            </div>
                        `;
                        
                        const customIcon = L.divIcon({
                            html: markerHtml,
                            className: 'custom-div-icon',
                            iconSize: [32, 32],
                            iconAnchor: [16, 16]
                        });
                        
                        // Añadir marcador
                        const marker = L.marker(latlng, { icon: customIcon }).addTo(map);
                        
                        // Popup con info de la actividad
                        const popupContent = `
                            <div class="map-popup">
                                <h4>${actividad.nombreActividad}</h4>
                                <p><strong>Plan:</strong> ${plan.titulo}</p>
                                <p><strong>Hora sugerida:</strong> ${actividad.horaSugerida || 'No especificada'}</p>
                                ${actividad.notaPersonalizada ? `<p class="text-xs italic">"${actividad.notaPersonalizada}"</p>` : ''}
                                <a href="/actividad/${actividad.slugActividad}-${actividad.idActividad}">Ver actividad →</a>
                            </div>
                        `;
                        
                        marker.bindPopup(popupContent);
                        
                        // Al hacer click en marcador, resaltar card del plan
                        marker.on('click', function() {
                            resaltarPlan(plan.id);
                        });
                    }
                });
                
                // Dibujar línea conectando las actividades del plan
                if (puntos.length > 1) {
                    L.polyline(puntos, {
                        color: color,
                        weight: 3,
                        opacity: 0.6,
                        dashArray: '10, 10'
                    }).addTo(map);
                }
            }
        });
        
        // Ajustar zoom para mostrar todos los marcadores
        if (planesData.length > 0) {
            const bounds = [];
            planesData.forEach(plan => {
                plan.actividades.forEach(act => {
                    if (act.latitud && act.longitud) {
                        bounds.push([act.latitud, act.longitud]);
                    }
                });
            });
            
            if (bounds.length > 0) {
                map.fitBounds(bounds, { padding: [50, 50] });
            }
        }
    }

    // Función para resaltar un plan en la lista
    function resaltarPlan(planId) {
        // Quitar highlight de todos los planes
        document.querySelectorAll('.plan-card').forEach(card => {
            card.classList.remove('highlighted');
        });
        
        // Añadir highlight al plan seleccionado
        const planCard = document.getElementById('plan-' + planId);
        if (planCard) {
            planCard.classList.add('highlighted');
            
            // Scroll suave hacia el plan
            planCard.scrollIntoView({ 
                behavior: 'smooth', 
                block: 'center' 
            });
            
            // Quitar highlight después de 3 segundos
            setTimeout(() => {
                planCard.classList.remove('highlighted');
            }, 3000);
        }
    }
    
    // Permitir click en cards de planes para resaltar en el mapa
    document.querySelectorAll('.plan-card').forEach(card => {
        card.addEventListener('mouseenter', function() {
            // Opcional: añadir efecto visual al pasar el mouse
            this.style.borderColor = '#7c3aed';
        });
        
        card.addEventListener('mouseleave', function() {
            if (!this.classList.contains('highlighted')) {
                this.style.borderColor = '';
            }
        });
    });
});
