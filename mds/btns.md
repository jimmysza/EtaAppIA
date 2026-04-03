Aquí te muestro cómo quedaría el flujo con el modal de pantalla completa. Primero el HTML del modal que reemplaza el bloque actual, y luego te explico qué cambiar exactamente.Aquí tienes el flujo interactivo — puedes hacer clic en **Siguiente →** para ver el modal en acción, navegar entre pasos y ver la pantalla de confirmación final.

---

Ahora te doy el código para reemplazar en tu proyecto. En tu HTML debes hacer **dos cambios**:

**1. Elimina el bloque `.onboarding-section` y el botón `div9`** que ya tenías dentro del `<form>`, y reemplaza el botón de submit por uno que abre el modal:

```html
<!-- Reemplaza el botón submit original por este -->
<button type="button" class="login-button div9" onclick="abrirModalOnboarding()">
  Siguiente
</button>
```

**2. Agrega el modal justo antes de cerrar el `<body>`**, fuera del `<form>`:

```html
<!-- MODAL ONBOARDING - fuera del form, antes de </body> -->
<div id="modalOnboarding" style="display:none; position:fixed; inset:0; z-index:9999;
  background:rgba(0,0,0,0.6); align-items:center; justify-content:center;">
  
  <div style="background:var(--color-background-primary, #fff); border-radius:16px;
    padding:2.5rem; width:90%; max-width:520px; position:relative;">

    <button onclick="cerrarModal()" style="position:absolute; top:1rem; right:1rem;
      background:none; border:none; font-size:1.25rem; cursor:pointer; color:#9ca3af;">✕</button>

    <p id="stepLabel" style="font-size:13px; color:#6b7280; margin-bottom:4px;">Paso 1 de 4</p>
    <div style="height:5px; background:#e5e7eb; border-radius:3px; margin-bottom:1.5rem; overflow:hidden;">
      <div id="progressFill" style="height:100%; background:#23aae2; transition:width 0.4s; width:25%;"></div>
    </div>

    <div id="modalStepContent"></div>

    <div style="display:flex; justify-content:space-between; align-items:center; margin-top:1.5rem;">
      <button id="btnAnteriorModal" onclick="pasoAnterior()" class="btn-onboard btn-anterior"
        style="display:none;">Anterior</button>
      <button id="btnSiguienteModal" onclick="pasoSiguiente()" class="btn-onboard btn-siguiente ml-auto">
        Siguiente
      </button>
      <button id="btnUneteFinal" onclick="submitFormFinal()" class="login-button"
        style="display:none;">Únete</button>
    </div>
  </div>
</div>
```

**3. El script** que maneja todo esto (reemplaza el script de onboarding que ya tienes):

```javascript
const pasosData = [
  { titulo: '¿Qué actividades te gustan?', subtitulo: 'Selecciona una o más', tipo: 'checkbox', nombre: 'categoriasIds' },
  { titulo: '¿Con quién sueles salir?', subtitulo: 'Selecciona una opción', tipo: 'radio', nombre: 'grupoViaje',
    opciones: [{v:'SOLO',l:'Solo'},{v:'PAREJA',l:'En pareja'},{v:'FAMILIA',l:'Con familia'},{v:'AMIGOS',l:'Con amigos'},{v:'VARIOS',l:'Varía'}] },
  { titulo: '¿Qué presupuesto manejas?', subtitulo: 'Por actividad', tipo: 'radio', nombre: 'rangoPrecio',
    opciones: [{v:'ECONOMICO',l:'Económico (< $50k)'},{v:'MODERADO',l:'Moderado ($50k–$150k)'},{v:'PREMIUM',l:'Premium (> $150k)'}] },
  { titulo: '¿Cuándo haces más actividades?', subtitulo: 'Selecciona una opción', tipo: 'radio', nombre: 'disponibilidadSemana',
    opciones: [{v:'FINDE',l:'Fines de semana'},{v:'ENTRE_SEMANA',l:'Entre semana'},{v:'AMBOS',l:'Ambos'}] }
];

let pasoActual = 0;

function abrirModalOnboarding() {
  // Validar que el form principal esté completo
  const form = document.querySelector('form');
  const inputs = form.querySelectorAll('input[required]');
  for (const inp of inputs) {
    if (!inp.value.trim()) { inp.focus(); alert('Por favor completa todos los campos'); return; }
  }
  pasoActual = 0;
  document.getElementById('modalOnboarding').style.display = 'flex';
  renderPaso();
}

function cerrarModal() {
  document.getElementById('modalOnboarding').style.display = 'none';
}

function renderPaso() {
  const paso = pasosData[pasoActual];
  document.getElementById('stepLabel').textContent = `Paso ${pasoActual + 1} de ${pasosData.length}`;
  document.getElementById('progressFill').style.width = `${((pasoActual + 1) / pasosData.length) * 100}%`;

  let html = `<h3 style="font-size:20px;font-weight:600;margin-bottom:6px;">${paso.titulo}</h3>
              <p style="font-size:14px;color:#6b7280;margin-bottom:1.25rem;">${paso.subtitulo}</p>`;

  if (paso.tipo === 'checkbox') {
    // Las categorías vienen del servidor via Thymeleaf — aquí se renderizan igual que antes
    // solo cambia el estilo visual a pills
    html += `<div style="display:flex;flex-wrap:wrap;gap:8px;">`;
    // Tomamos los checkboxes existentes del DOM (ya renderizados por Thymeleaf antes del modal)
    document.querySelectorAll('input[name="categoriasIds"]').forEach(cb => {
      const label = cb.nextElementSibling?.textContent || cb.value;
      const checked = cb.checked ? 'background:#23aae2;color:white;border-color:#23aae2;' : '';
      html += `<div onclick="toggleCat('${cb.id}')" id="pill_${cb.id}"
        style="padding:8px 16px;border:1.5px solid #e5e7eb;border-radius:999px;cursor:pointer;
        font-weight:500;font-size:14px;transition:all 0.2s;${checked}">${label}</div>`;
    });
    html += `</div>`;
  } else {
    html += `<div style="display:flex;flex-direction:column;gap:8px;">`;
    paso.opciones.forEach(op => {
      const existing = document.querySelector(`input[name="${paso.nombre}"][value="${op.v}"]`);
      const checked = existing?.checked;
      html += `<button type="button" onclick="selectOpcion('${paso.nombre}','${op.v}',this)"
        style="padding:11px 16px;text-align:left;border:1.5px solid ${checked?'#23aae2':'#e5e7eb'};
        border-radius:8px;font-size:14px;font-weight:500;cursor:pointer;
        background:${checked?'#23aae2':'white'};color:${checked?'white':'#374151'};transition:all 0.2s;">
        ${op.l}</button>`;
    });
    html += `</div>`;
  }

  document.getElementById('modalStepContent').innerHTML = html;
  document.getElementById('btnAnteriorModal').style.display = pasoActual > 0 ? 'inline-block' : 'none';
  document.getElementById('btnSiguienteModal').style.display = pasoActual < pasosData.length - 1 ? 'inline-block' : 'none';
  document.getElementById('btnUneteFinal').style.display = pasoActual === pasosData.length - 1 ? 'inline-block' : 'none';
}

function toggleCat(id) {
  const cb = document.getElementById(id);
  cb.checked = !cb.checked;
  const pill = document.getElementById('pill_' + id);
  if (cb.checked) {
    pill.style.background = '#23aae2'; pill.style.color = 'white'; pill.style.borderColor = '#23aae2';
  } else {
    pill.style.background = ''; pill.style.color = ''; pill.style.borderColor = '#e5e7eb';
  }
}

function selectOpcion(nombre, valor, btn) {
  // Actualizar input hidden/radio real en el form
  const radio = document.querySelector(`input[name="${nombre}"][value="${valor}"]`);
  if (radio) radio.checked = true;
  // Actualizar visual
  btn.parentElement.querySelectorAll('button').forEach(b => {
    b.style.background = 'white'; b.style.color = '#374151'; b.style.borderColor = '#e5e7eb';
  });
  btn.style.background = '#23aae2'; btn.style.color = 'white'; btn.style.borderColor = '#23aae2';
}

function validarPasoModal() {
  if (pasoActual === 0) {
    const alguno = Array.from(document.querySelectorAll('input[name="categoriasIds"]')).some(c => c.checked);
    if (!alguno) { alert('Por favor selecciona al menos una categoría'); return false; }
  } else {
    const nombre = pasosData[pasoActual].nombre;
    const alguno = document.querySelector(`input[name="${nombre}"]:checked`);
    if (!alguno) { alert('Por favor selecciona una opción'); return false; }
  }
  return true;
}

function pasoSiguiente() {
  if (validarPasoModal() && pasoActual < pasosData.length - 1) {
    pasoActual++; renderPaso();
  }
}

function pasoAnterior() {
  if (pasoActual > 0) { pasoActual--; renderPaso(); }
}

function submitFormFinal() {
  if (!validarPasoModal()) return;
  cerrarModal();
  document.querySelector('form').submit();
}
```

El truco clave es que los `input[name="categoriasIds"]` y los `radio` del Thymeleaf siguen existiendo en el DOM (puedes dejarlos ocultos con `display:none`), el modal solo actúa como interfaz visual encima de ellos, y al final hace `form.submit()` normalmente. No necesitas cambiar nada en el backend.