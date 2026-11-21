(async () => {
  Menu.pintar();
  const contenido = document.getElementById("contenido");
  const id = Number(parametro("id"));
  const NOMBRES_ESTRELLAS = ["", "Mala", "Regular", "Buena", "Muy buena", "¡Excelente!"];
  let estado = null;
  let perfilId = 0;

  if (!id) {
    contenido.innerHTML = `<div class="vacio"><b>🎬</b>Falta indicar qué película abrir.<br><a href="inicio.html">Volver al inicio</a></div>`;
    return;
  }

  try {
    const r = await api("/peliculas/detalle?id=" + id);
    estado = r.estado;
    perfilId = r.perfilId;
    pintar(r.pelicula, r.comentarios);
  } catch (e) {
    mostrarError(e);
    contenido.innerHTML = `<div class="vacio"><b>😕</b>${escapar(e.message)}<br><a href="inicio.html">Volver al inicio</a></div>`;
  }

  function pintar(p, comentarios) {
    document.title = p.titulo + " · StreamFlix";
    const duracion = p.tipo === "Serie" ? `capítulos de ${p.duracionMin} min` : `${Math.floor(p.duracionMin / 60)} h ${p.duracionMin % 60} min`;
    contenido.innerHTML = `
      <div class="detalle aparece">
        <div class="portada-trailer">${tarjeta(p, { grande: true }).replace('class="tarjeta"', 'class="tarjeta" tabindex="-1"').replace(/href="[^"]*"/, "")}
          ${p.trailer ? `<button class="play-portada" data-trailer aria-label="Ver tráiler de ${escapar(p.titulo)}"><span>▶</span>Ver tráiler</button>` : ""}</div>
        <div>
          <h1>${escapar(p.titulo)}</h1>
          <div class="datos"><span>${p.anio}</span><span>${escapar(p.tipo)}</span><span>${duracion}</span>
            <span>${escapar(p.clasificacion)}</span><span>⭐ ${p.votos ? p.promedio.toFixed(1) + " (" + p.votos + " votos)" : "sin votos aún"}</span></div>
          ${p.generos.map((g, i) => `<span class="etiqueta" style="--g:${COLORES_ACENTO[i % 5]}">${escapar(g)}</span>`).join("")}
          <p style="margin-top:10px;max-width:720px">${escapar(p.descripcion)}</p>

          <h2 class="titulo-seccion" style="--c:var(--c4)">¿Dónde puedes verla?<small>abre la plataforma en otra pestaña</small></h2>
          <div class="donde">${p.plataformas.map(pl => `<a class="plataforma" href="${escapar(pl.enlace)}" target="_blank" rel="noopener" style="background:${pl.color}">▶ ${escapar(pl.nombre)}</a>`).join("")}
            ${p.trailer ? `<button type="button" class="plataforma boton-trailer" data-trailer>🎬 Ver tráiler</button>` : ""}</div>

          <div class="fila-botones izq">
            <button class="btn" id="visto"></button>
            <button class="btn" id="lista"></button>
          </div>

          <h2 class="titulo-seccion" style="--c:var(--c3)">Tu calificación<small>con tus estrellas la IA aprende tus gustos</small></h2>
          <div class="estrellas" id="estrellas">${[1, 2, 3, 4, 5].map(n => `<button class="estrella" data-n="${n}" aria-label="${n} estrellas">★</button>`).join("")}</div>
          <div class="texto-estrellas" id="texto-estrellas"></div>

          <h2 class="titulo-seccion" style="--c:var(--c1)">Comentarios<small id="cuantos"></small></h2>
          <form id="formulario" novalidate>
            <div class="campo">
              <label for="texto">Tu comentario <span class="marca obligatorio">10 A 300 LETRAS</span></label>
              <textarea class="entrada" id="texto" name="texto" required maxlength="300" data-min="10" placeholder="Ejemplo: Me gustó la música y el final, la volvería a ver."></textarea>
              <div class="ayuda"><span>Cuenta qué te pareció (máximo 3 comentarios por título). La IA lee si es positivo o negativo.</span><span class="contador"></span></div>
            </div>
            <div class="fila-botones izq"><button class="btn btn-1" type="submit">Publicar comentario</button></div>
          </form>
          <div id="comentarios"></div>
        </div>
      </div>`;
    pintarBotones();
    pintarEstrellas(estado.estrellas);
    pintarComentarios(comentarios);
    activar(p);
  }

  function pintarBotones() {
    const v = document.getElementById("visto");
    const l = document.getElementById("lista");
    v.className = "btn " + (estado.visto ? "btn-3" : "btn-1");
    v.textContent = estado.visto ? "✔ Ya la vi" : "👁 Marcar como vista";
    l.className = "btn " + (estado.enLista ? "btn-3" : "btn-2");
    l.textContent = estado.enLista ? "✔ En mi lista" : "＋ Mi lista";
  }

  function pintarEstrellas(n, vista = false) {
    document.querySelectorAll(".estrella").forEach(b => b.classList.toggle("on", Number(b.dataset.n) <= n));
    document.getElementById("texto-estrellas").textContent = n
      ? `${n} de 5 · ${NOMBRES_ESTRELLAS[n]}${vista ? "" : " (tu calificación)"}`
      : "Aún no la calificas. Toca una estrella.";
  }

  function pintarComentarios(lista) {
    document.getElementById("cuantos").textContent = lista.length === 1 ? "1 comentario" : `${lista.length} comentarios`;
    document.getElementById("comentarios").innerHTML = lista.length ? lista.map((c, i) => `
      <div class="comentario" style="--c:${COLORES_ACENTO[i % 5]}">
        <span class="avatar-mini" style="background:${c.color}">${c.avatar}</span>
        <div style="flex:1">
          <div class="cabeza"><b>${escapar(c.perfil)}</b><span style="color:#ffc53d">${estrellasTexto(c.estrellas)}</span><span>${c.fecha}</span>
          ${c.perfilId === perfilId ? `<button class="borrar-comentario" data-borrar="${c.id}">🗑 Borrar</button>` : ""}</div>
          <p>${escapar(c.texto)}</p>
        </div>
      </div>`).join("") : `<p class="vacio"><b>💬</b>Nadie comentó todavía. ¡Sé el primero!</p>`;
  }

  function abrirTrailer(p, origen) {
    const fondo = document.createElement("div");
    fondo.className = "fondo-modal fondo-trailer";
    fondo.innerHTML = `
      <div class="caja-trailer" role="dialog" aria-modal="true" aria-label="Tráiler de ${escapar(p.titulo)}">
        <div class="cab-trailer"><b>🎬 Tráiler · ${escapar(p.titulo)}</b>
          ${p.trailerIdioma === "es" ? '<span class="idioma-trailer es">Español latino</span>' : '<span class="idioma-trailer">Solo en inglés</span>'}
          <button type="button" class="cerrar-trailer" aria-label="Cerrar tráiler">✕</button></div>
        <div class="marco-trailer">
          <iframe src="https://www.youtube-nocookie.com/embed/${encodeURIComponent(p.trailer)}?autoplay=1&rel=0&playsinline=1"
            title="Tráiler de ${escapar(p.titulo)}" allow="autoplay; encrypted-media; picture-in-picture; fullscreen" allowfullscreen></iframe>
        </div>
        <p class="nota-trailer">${p.trailerIdioma === "es" ? "" : "Aún no hay tráiler oficial en español; se muestra el original. "}Se cierra con ✕, con la tecla Esc o tocando fuera. Si no se escucha, toca el video para activar el sonido.</p>
      </div>`;
    const cerrar = () => {
      fondo.remove();
      document.removeEventListener("keydown", tecla);
      document.body.style.overflow = "";
      if (origen) origen.focus();
    };
    const tecla = e => { if (e.key === "Escape") cerrar(); };
    fondo.addEventListener("click", e => { if (e.target === fondo) cerrar(); });
    fondo.querySelector(".cerrar-trailer").addEventListener("click", cerrar);
    document.addEventListener("keydown", tecla);
    document.body.style.overflow = "hidden";
    document.body.appendChild(fondo);
    fondo.querySelector(".cerrar-trailer").focus();
  }

  function activar(p) {
    document.querySelectorAll("[data-trailer]").forEach(b => b.addEventListener("click", () => abrirTrailer(p, b)));
    document.getElementById("visto").addEventListener("click", async () => {
      if (estado.visto) {
        const ok = await Avisos.confirmarDoble({ accion: "quitar", que: "esta película de «Ya la vi»", consecuencia: "Dejará de contar como vista para tus recomendaciones." });
        if (!ok) return;
      }
      try {
        const r = await api("/interaccion/visto", { peliculaId: p.id, visto: !estado.visto });
        estado.visto = r.visto;
        pintarBotones();
        Avisos.mostrar(r.mensaje);
      } catch (e) { mostrarError(e); }
    });

    document.getElementById("lista").addEventListener("click", async () => {
      if (estado.enLista) {
        const ok = await Avisos.confirmarDoble({ accion: "quitar", que: "esta película de Mi lista", consecuencia: "Podrás volver a agregarla cuando quieras." });
        if (!ok) return;
      }
      try {
        const r = await api("/interaccion/lista", { peliculaId: p.id, agregar: !estado.enLista });
        estado.enLista = r.enLista;
        pintarBotones();
        Avisos.mostrar(r.mensaje);
      } catch (e) { mostrarError(e); }
    });

    const caja = document.getElementById("estrellas");
    caja.addEventListener("mouseover", e => { const b = e.target.closest(".estrella"); if (b) pintarEstrellas(Number(b.dataset.n), true); });
    caja.addEventListener("mouseleave", () => pintarEstrellas(estado.estrellas));
    caja.addEventListener("click", async e => {
      const b = e.target.closest(".estrella");
      if (!b) return;
      const n = Number(b.dataset.n);
      try {
        const r = await api("/interaccion/calificar", { peliculaId: p.id, estrellas: n });
        estado.estrellas = n;
        estado.visto = true;
        pintarEstrellas(n);
        pintarBotones();
        Sonido.estrella(n);
        Avisos.mostrar(r.mensaje);
      } catch (err) { mostrarError(err); }
    });

    const formulario = document.getElementById("formulario");
    Campos.preparar(formulario);
    formulario.addEventListener("submit", async e => {
      e.preventDefault();
      if (!Campos.validarTodo(formulario)) return;
      try {
        const r = await api("/interaccion/comentar", { peliculaId: p.id, texto: document.getElementById("texto").value.trim() });
        document.getElementById("texto").value = "";
        document.getElementById("texto").dispatchEvent(new Event("input"));
        pintarComentarios(r.comentarios);
        Avisos.mostrar(r.mensaje);
      } catch (err) { mostrarError(err, formulario); }
    });

    document.getElementById("comentarios").addEventListener("click", async e => {
      const b = e.target.closest("[data-borrar]");
      if (!b) return;
      const ok = await Avisos.confirmarDoble({ accion: "borrar", que: "tu comentario", consecuencia: "El comentario desaparecerá para todos. No se puede deshacer." });
      if (!ok) return;
      try {
        const r = await api("/interaccion/borrar-comentario", { id: Number(b.dataset.borrar), peliculaId: p.id });
        pintarComentarios(r.comentarios);
        Avisos.mostrar(r.mensaje);
      } catch (err) { mostrarError(err); }
    });
  }
})();
