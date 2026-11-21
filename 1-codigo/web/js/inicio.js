(async () => {
  Menu.pintar();
  const zonaFilas = document.getElementById("filas");
  const zonaDestacada = document.getElementById("destacada");
  const zonaMosaicos = document.getElementById("mosaicos");
  const TIPOS = [["Todo", "🏠 Todo"], ["Película", "🎬 Películas"], ["Serie", "📺 Series"], ["Anime", "🍥 Anime"]];
  let tipo = "Todo", genero = null, inicio = null, categorias = [], masAbierto = false;
  const PRIMEROS = 8;

  function pintarFilas(d, filas) {
    zonaFilas.innerHTML = filas.filter(f => f.peliculas.length).map((f, i) => `
      <section id="${f.id}" class="aparece" style="animation-delay:${i * 0.06}s">
        <h2 class="titulo-seccion" style="--c:${COLORES_ACENTO[i % 5]}">${escapar(f.titulo)}<small>${escapar(f.detalle)}</small></h2>
        <div class="carrusel-caja">
          <button class="flecha izq" aria-label="Anterior">‹</button>
          <div class="carrusel">${f.peliculas.map(x => tarjeta(x, { vistas: d.vistas, conMatch: f.id === "para-ti" })).join("")}</div>
          <button class="flecha der" aria-label="Siguiente">›</button>
        </div>
      </section>`).join("");
    activarFlechas();
  }

  function pintarDestacada(d) {
    const p = d.destacada;
    if (!p) { zonaDestacada.innerHTML = ""; return; }
    zonaDestacada.innerHTML = `
      <div class="destacada aparece">
        <div>
          <span class="etiqueta" style="--g:var(--c3)">⭐ ${p.puntaje}% para ti</span>
          <h1>${escapar(p.titulo)}</h1>
          <div class="datos">${p.anio} · ${escapar(p.tipo)} · ${escapar(p.generos.join(", "))} · ${escapar(p.clasificacion)}</div>
          <div class="motivo">💡 ${escapar(p.motivo)}</div>
          <div class="fila-botones izq">
            <a class="btn btn-1" href="pelicula.html?id=${p.id}">▶ Ver detalles</a>
            <button class="btn btn-3" id="agregar-destacada">＋ Mi lista</button>
          </div>
        </div>
        ${tarjeta(p, { grande: true })}
      </div>`;
    const b = document.getElementById("agregar-destacada");
    b.textContent = d.lista.includes(p.id) ? "✔ En mi lista" : "＋ Mi lista";
    b.addEventListener("click", async () => {
      try {
        const r = await api("/interaccion/lista", { peliculaId: p.id, agregar: !b.textContent.includes("✔") });
        b.textContent = r.enLista ? "✔ En mi lista" : "＋ Mi lista";
        Avisos.mostrar(r.mensaje);
      } catch (e) { mostrarError(e); }
    });
  }

  function pintarBotones() {
    document.getElementById("tipos").innerHTML = TIPOS.map(([v, t]) =>
      `<button type="button" class="boton-tipo ${tipo === v ? "si" : ""}" data-tipo="${v}" aria-pressed="${tipo === v}">${t}</button>`).join("");
    const orden = categorias.filter(c => c.nombre !== "Anime").sort((a, b) => b.cantidad - a.cantidad);
    if (orden.slice(PRIMEROS).some(c => c.nombre === genero)) masAbierto = true;
    const boton = c => `<button type="button" class="boton-genero ${genero === c.nombre ? "si" : ""}" data-genero="${escapar(c.nombre)}" aria-pressed="${genero === c.nombre}">${escapar(c.nombre)}</button>`;
    const resto = orden.slice(PRIMEROS);
    document.getElementById("generos").innerHTML = orden.slice(0, PRIMEROS).map(boton).join("")
      + (resto.length ? `<button type="button" class="boton-mas" id="ver-mas" aria-expanded="${masAbierto}">${masAbierto ? "▲ Menos géneros" : `▼ Más géneros (${resto.length})`}</button>` : "")
      + (masAbierto && resto.length ? `<div class="mas-generos">${resto.map(boton).join("")}</div>` : "");
    document.getElementById("ver-mas")?.addEventListener("click", () => { masAbierto = !masAbierto; pintarBotones(); });
    document.querySelectorAll("[data-tipo]").forEach(b => b.addEventListener("click", () => { tipo = b.dataset.tipo; filtrar(); }));
    document.querySelectorAll("[data-genero]").forEach(b => b.addEventListener("click", () => {
      genero = genero === b.dataset.genero ? null : b.dataset.genero; filtrar();
    }));
  }

  function pintarMosaicos() {
    const lista = categorias.filter(c => c.portadas.length >= 3);
    zonaMosaicos.innerHTML = lista.length ? `
      <h2 class="titulo-seccion" style="--c:var(--c3)">Explora por categoría<small>toca una para ver todo lo de ese género</small></h2>
      <div class="mosaicos">${lista.map((c, i) => `
        <a class="mosaico" href="categoria.html?genero=${encodeURIComponent(c.nombre)}" style="--m:${COLORES_ACENTO[i % 5]}">
          <div class="mosaico-fotos">${c.portadas.slice(0, 3).map(f => imagenDiferida(f)).join("")}</div>
          <div class="mosaico-velo"></div><small>${c.cantidad} títulos</small><b>${escapar(c.nombre)}</b>
        </a>`).join("")}</div>` : "";
  }

  async function filtrar() {
    pintarBotones();
    const estado = document.getElementById("estado-filtro");
    if (tipo === "Todo" && !genero) {
      estado.innerHTML = "Toca un tipo o un género para ver solo eso.";
      pintarDestacada(inicio);
      pintarFilas(inicio, inicio.filas);
      zonaMosaicos.hidden = false;
      return;
    }
    zonaDestacada.innerHTML = "";
    zonaMosaicos.hidden = true;
    zonaFilas.innerHTML = '<p class="vacio"><b>🍿</b>Buscando…</p>';
    try {
      const q = new URLSearchParams({ tipo, genero: genero || "" });
      const d = (await api("/recomendaciones/explorar?" + q)).explorar;
      estado.innerHTML = `Mostrando <b>${escapar(d.nombre)}</b> · ${d.total} títulos <button type="button" class="quitar-filtros" id="quitar">✕ Quitar filtros</button>`;
      document.getElementById("quitar").addEventListener("click", () => { tipo = "Todo"; genero = null; filtrar(); });
      if (!d.total) {
        zonaFilas.innerHTML = `<div class="vacio"><b>😕</b>No hay ${escapar(d.nombre.toLowerCase())} en el catálogo por ahora.<br>Prueba con otro género o quita los filtros.</div>`;
        return;
      }
      pintarFilas(d, d.filas);
    } catch (e) {
      mostrarError(e);
      zonaFilas.innerHTML = `<div class="vacio"><b>😕</b>${escapar(e.message)}</div>`;
    }
  }

  try {
    const [d, c] = await Promise.all([api("/recomendaciones/inicio"), api("/recomendaciones/categorias")]);
    inicio = d;
    categorias = c.categorias;
    document.getElementById("filtros").hidden = false;
    pintarMosaicos();
    await filtrar();
    if (location.hash) document.querySelector(location.hash)?.scrollIntoView();
  } catch (e) {
    mostrarError(e);
    zonaFilas.innerHTML = `<div class="vacio"><b>😕</b>${escapar(e.message)}<br><small>${escapar(e.detalle || "")}</small></div>`;
  }
})();
