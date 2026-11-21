(() => {
  Menu.pintar();
  const caja = document.getElementById("metricas");
  const NUEVOS = ["Colaborativo por películas", "Factorización de matrices", "Híbrido mejorado (pesos aprendidos)", "Híbrido mejorado solo con estrellas"];
  const GANADOR = "Híbrido mejorado (pesos aprendidos)";
  const ACTUAL = "Sistema actual (más populares)";
  const mil = n => String(n).replace(/\B(?=(\d{3})+(?!\d))/g, " ");

  function buscar(metodos, nombre) {
    return metodos.find(m => m.nombre === nombre) || metodos[metodos.length - 1];
  }

  function barras(metodos, campo, unidad, max) {
    return metodos.map((m, i) => `
      <div class="barra-gusto"><span style="width:210px;font-size:12.5px">${escapar(m.nombre)}</span>
        <div class="pista"><div class="relleno" data-v="${(100 * m[campo] / max).toFixed(1)}" style="background:${COLORES_ACENTO[i % 5]}"></div></div>
        <span style="width:70px;text-align:right">${m[campo]}${unidad}</span></div>`).join("");
  }

  function tablaCorta(metodos) {
    return `<div style="overflow-x:auto"><table class="tabla tabla-corta">
      <thead><tr><th>Método</th><th class="num">MAE</th><th class="num">Prec@3</th></tr></thead>
      <tbody>${metodos.map(x => `<tr class="${x.nombre === GANADOR ? "ganador" : NUEVOS.includes(x.nombre) ? "nuevo" : ""}">
        <td>${escapar(x.nombre)}${NUEVOS.includes(x.nombre) ? ' <span class="chip-nuevo">NUEVO</span>' : ""}</td>
        <td class="num">${x.mae}</td><td class="num">${x.precision}%</td></tr>`).join("")}</tbody></table></div>`;
  }

  function conjunto(icono, titulo, detalle, metodos) {
    const g = buscar(metodos, GANADOR), a = buscar(metodos, ACTUAL);
    return `<section class="panel conjunto aparece">
      <h3>${icono} ${escapar(titulo)}</h3>
      <p class="sub">${detalle}</p>
      ${tablaCorta(metodos)}
      <p class="sub" style="margin-top:10px">El híbrido mejorado acierta <b>${g.precision}%</b> frente a <b>${a.precision}%</b> del sistema actual.</p>
    </section>`;
  }

  function curva(c) {
    const puntos = c.curva;
    if (!puntos || !puntos.length) return "";
    const W = 640, H = 220, x0 = 46, y0 = 16, x1 = W - 14, y1 = H - 34;
    const valores = puntos.flatMap(p => [p.entrenamiento, p.validacion]);
    const min = Math.min(...valores), max = Math.max(...valores);
    const ancho = Math.max(puntos.length - 1, 1);
    const X = i => x0 + (x1 - x0) * i / ancho;
    const Y = v => y1 - (y1 - y0) * (v - min) / ((max - min) || 1);
    const linea = campo => puntos.map((p, i) => `${X(i).toFixed(1)},${Y(p[campo]).toFixed(1)}`).join(" ");
    const mejor = Math.min(c.mejorEpoca - 1, puntos.length - 1);
    return `<section class="panel aparece">
      <h3>📉 ${escapar(c.nombre)}</h3>
      <p class="sub">${c.factores} rasgos ocultos · regularización ${c.regularizacion} (elegidos solos) · mejor vuelta: ${c.mejorEpoca}</p>
      <svg class="curva-svg" viewBox="0 0 ${W} ${H}" role="img" aria-label="Error en cada vuelta de entrenamiento">
        <g stroke="#36367a"><line x1="${x0}" y1="${y0}" x2="${x0}" y2="${y1}"/><line x1="${x0}" y1="${y1}" x2="${x1}" y2="${y1}"/></g>
        <g fill="#b3b0d9" font-size="11"><text x="4" y="${y0 + 8}">${max.toFixed(2)}</text><text x="4" y="${y1}">${min.toFixed(2)}</text>
          <text x="${x0}" y="${H - 12}">1</text><text x="${x1 - 20}" y="${H - 12}">${puntos.length}</text>
          <text x="${(x0 + x1) / 2 - 80}" y="${H - 12}">vueltas de entrenamiento (épocas)</text></g>
        <line x1="${X(mejor)}" y1="${y0}" x2="${X(mejor)}" y2="${y1}" stroke="#ffd84d" stroke-dasharray="4 4"/>
        <polyline fill="none" stroke="#ff6fa5" stroke-width="3" points="${linea("entrenamiento")}"/>
        <polyline fill="none" stroke="#5fe0b7" stroke-width="3" stroke-dasharray="7 5" points="${linea("validacion")}"/>
      </svg>
      <div class="leyenda-curva"><span><i style="background:#ff6fa5"></i>error al entrenar</span><span><i style="background:#5fe0b7"></i>error en la prueba</span><span><i style="background:#ffd84d"></i>donde se detuvo</span></div>
    </section>`;
  }

  function historial(d) {
    const h = d.historial || [];
    const caja = document.getElementById("historial");
    const coma = v => String(v).replace(".", ",");
    if (!h.length) {
      caja.innerHTML = `<section class="panel aparece" style="margin-top:20px"><h2>📈 La IA aprende con el tiempo</h2>
        <p class="sub">Todavía no hay mediciones. La primera se hace sola en unos segundos; vuelve a abrir esta página.</p></section>`;
      return;
    }
    const inicio = h[0], ahora = h[h.length - 1];
    const subio = Math.round((ahora.precision - inicio.precision) * 10) / 10;
    const faltan = Math.max(1, d.cadaCuanto - Math.abs(d.calificaciones - ahora.calificaciones));
    caja.innerHTML = `<section class="panel aparece" style="margin-top:20px">
      <h2>📈 La IA aprende con el tiempo</h2>
      <p class="sub">Cada ${d.cadaCuanto} calificaciones nuevas vuelve a medirse sola, sin hacer esperar a nadie.</p>
      <div class="antes-ahora">
        <div class="tarjeta-tiempo" style="--c:var(--c1)"><span>Al empezar (${mil(inicio.calificaciones)} calificaciones)</span><b>${coma(inicio.precision)}%</b><span>de aciertos</span></div>
        <div class="flecha-tiempo" aria-hidden="true">➜</div>
        <div class="tarjeta-tiempo" style="--c:var(--c4)"><span>Ahora (${mil(ahora.calificaciones)} calificaciones)</span><b>${coma(ahora.precision)}%</b><span>de aciertos · ${subio >= 0 ? "+" : ""}${coma(subio)} puntos</span></div>
      </div>
      <p class="sub" style="margin-top:12px">${h.length} mediciones guardadas. La última (${escapar(ahora.fecha)}): «${escapar(ahora.nota)}».</p>
      <span class="proxima-medicion">⏳ Próxima medición en ${faltan} ${faltan === 1 ? "calificación nueva" : "calificaciones nuevas"}</span>
    </section>`;
  }

  async function cargar(recalcular = false) {
    caja.innerHTML = '<p class="vacio"><b>🧪</b>Ejecutando la validación cruzada…</p>';
    try {
      const d = await api("/recomendaciones/metricas" + (recalcular ? "?recalcular=1" : ""));
      const m = d.metodos;
      const actual = buscar(m, ACTUAL);
      const propuesto = buscar(m, GANADOR);
      const mejoraPrecision = ((propuesto.precision - actual.precision) / actual.precision * 100).toFixed(0);
      const mejoraError = ((actual.mae - propuesto.mae) / actual.mae * 100).toFixed(0);
      const mejoraMinutos = ((propuesto.minutos - actual.minutos) / actual.minutos * 100).toFixed(0);
      const e = d.encuesta;
      const reales = d.datosReales || [];
      const icono = n => n.startsWith("Anime") ? "🍥" : "🌍";
      caja.innerHTML = `
        <div class="rejilla-4 aparece" style="margin-top:20px">
          <div class="dato-grande" style="--c:var(--c4)"><b>+${mejoraPrecision}%</b><span>más aciertos que el sistema actual</span></div>
          <div class="dato-grande" style="--c:var(--c3)"><b>−${mejoraError}%</b><span>menos error al adivinar estrellas</span></div>
          <div class="dato-grande" style="--c:var(--c2)"><b>+${mejoraMinutos}%</b><span>tiempo de visualización</span></div>
          <div class="dato-grande" style="--c:var(--c1)"><b>${e.satisfaccion}</b><span>satisfacción en encuesta (de 5, ${e.respuestas} respuestas)</span></div>
        </div>

        <h2 class="titulo-seccion" style="--c:var(--c5);margin-top:26px">Probado con datos de ejemplo y con datos reales<small>la misma validación cruzada en los tres</small></h2>
        <div class="rejilla-3">
          ${conjunto("📦", "Datos de ejemplo", `${mil(d.calificaciones)} calificaciones · ${d.perfiles} perfiles · ${d.peliculas} títulos`, m)}
          ${reales.map(c => conjunto(icono(c.nombre), c.nombre, `${mil(c.calificaciones)} calificaciones · ${mil(c.usuarios)} personas · ${mil(c.titulos)} títulos · ${escapar(c.fuente)}`, c.metodos)).join("")}
        </div>
        ${reales.length ? "" : '<p class="sub">Para ver MovieLens y anime, ejecuta datos-reales/descargar.bat y luego probar-datos-reales.bat.</p>'}

        ${reales.length ? `<h2 class="titulo-seccion" style="--c:var(--c2);margin-top:26px">Así aprende la factorización<small>error en cada vuelta: cuando la prueba deja de bajar, para sola para no «memorizar»</small></h2>
        <div class="rejilla-2">${reales.map(curva).join("")}</div>` : ""}

        <div id="historial"></div>

        <section class="panel aparece" style="margin-top:20px">
          <h2>🧪 Detalle de los datos de ejemplo: ${d.partes} partes × ${d.repeticiones} repeticiones</h2>
          <p class="sub">En cada vuelta se esconde 1/5 de las calificaciones y los modelos tratan de adivinarlas. Se repite con ${d.repeticiones} repartos al azar distintos y se promedia, para que no dependa de la suerte. Tardó ${d.segundos} s.</p>
          <div style="overflow-x:auto"><table class="tabla tabla-apilada">
            <thead><tr><th>Método</th><th class="num">Error medio (MAE)</th><th class="num">RMSE</th><th class="num">Precisión@${d.top}</th><th class="num">Satisfacción</th><th class="num">Minutos vistos</th></tr></thead>
            <tbody>${m.map(x => `<tr class="${x.nombre === GANADOR ? "ganador" : ""}"><td>${escapar(x.nombre)}</td><td class="num" data-titulo="MAE">${x.mae}</td><td class="num" data-titulo="RMSE">${x.rmse}</td><td class="num" data-titulo="Prec@${d.top}">${x.precision}%</td><td class="num" data-titulo="Satisf.">${x.satisfaccion} ★</td><td class="num" data-titulo="Minutos">${x.minutos}</td></tr>`).join("")}</tbody>
          </table></div>
          <p class="sub" style="margin-top:10px">MAE y RMSE: cuánto se equivoca al adivinar las estrellas (menos es mejor). Precisión@${d.top}: de las ${d.top} primeras que recomienda, cuántas le gustaron de verdad. «Al azar» sirve de referencia: cualquier método útil debe superarlo.</p>
        </section>

        <div class="rejilla-2" style="margin-top:20px">
          <section class="panel aparece"><h2>🎯 Precisión@${d.top}</h2>${barras(m, "precision", "%", 100)}</section>
          <section class="panel aparece"><h2>📉 Error medio (menos es mejor)</h2>${barras(m, "mae", "", Math.max(...m.map(x => x.mae)))}</section>
          <section class="panel aparece"><h2>⭐ Satisfacción estimada</h2>${barras(m, "satisfaccion", " ★", 5)}</section>
          <section class="panel aparece"><h2>⏱ Minutos vistos de lo recomendado</h2>${barras(m, "minutos", "", Math.max(...m.map(x => x.minutos)))}</section>
        </div>

        <section class="panel aparece" style="margin-top:20px">
          <h2>📊 Precisión del híbrido mejorado en cada repetición</h2>
          <p class="sub">Cada repetición reparte las calificaciones de otra forma. Si los resultados se parecen, el modelo es estable.</p>
          ${propuesto.precisionPorParte.map((v, i) => `<div class="barra-gusto"><span>Reparto ${i + 1}</span><div class="pista"><div class="relleno" data-v="${v}" style="background:${COLORES_ACENTO[i % 5]}"></div></div><span>${v}%</span></div>`).join("")}
          <div class="fila-botones"><button class="btn btn-3" id="recalcular">🔄 Volver a calcular</button><a class="btn btn-1" href="gustos.html#encuesta">📝 Responder encuesta</a></div>
        </section>`;
      historial(d);
      setTimeout(() => caja.querySelectorAll(".relleno").forEach(b => { b.style.width = b.dataset.v + "%"; }), 80);
      document.getElementById("recalcular").addEventListener("click", () => cargar(true));
      if (recalcular) Avisos.mostrar("Métricas recalculadas con los datos de ahora.");
    } catch (e) {
      mostrarError(e);
      caja.innerHTML = `<div class="vacio"><b>😕</b>${escapar(e.message)}</div>`;
    }
  }
  cargar();
})();
